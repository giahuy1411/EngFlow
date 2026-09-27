# audit-v17-full — REMOVE-LIMITS ROUND: gỡ 3 giới hạn còn lại

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` · **Tiền đề:** `2d6be94` (closing round)
**Yêu cầu:** gỡ các giới hạn đã nói thật ở báo cáo closing. **Người dùng chốt:** L1-A + L1-B + L2 +
C5 (fixture public-domain) + C6-a (configurable; **không có sandbox**).

---

## L1-A — hết treo/báo lỗi sai khi tra từ lạnh (frontend)

**Vấn đề (đo được):** `DICT_BUDGET_MS=6000` hết hạn → ném `TIMEOUT` ⇒ UI báo "Tra cứu quá lâu" cho một
từ **sẽ** tra được ở ~20 s. Nguyên nhân gốc: **TTFB của upstream là ~19.5 s ở phía họ** — đo tách tầng:

| Tầng | Thời gian |
|---|---|
| DNS | 0.075 s |
| connect | 0.117 s |
| TLS | 0.173 s |
| **TTFB** | **19.494 s** |

⇒ Không phải mạng VN chậm; **server của họ** chậm. Fail ở 6 s là **lỗi giả**.

**Cách gỡ:** `DICT_BUDGET_MS` thành **ngưỡng mềm** (chỉ gọi `options.onSlow()` để UI hiện "đang tra…");
request **vẫn chờ** tới trần cứng `DICT_TOTAL_MS = 45000` (trên tổng chuỗi fallback 32 s proxy + 4 + 4).
**KHÔNG** gửi lại request trên đường chậm — đo được upstream **chặn song song**:

```
6 request SONG SONG → 1 trả sau 19.7 s, 5 còn lại timeout 30 s (trả [])
4 request TUẦN TỰ    → mỗi cái ~19.5–21 s, đều nhận dữ liệu
```

**Bằng chứng:** `frontend/src/services/vocabularyService.js` (DICT_BUDGET_MS mềm + DICT_TOTAL_MS),
`SearchVocabulary.vue` (trạng thái `slow`), test `vocabulary-search-order.test.js` ca mới
"keeps waiting past 6 s and returns the real result" — **mutation-test**: đổi trần cứng về 6 s → ca đó **FAIL**.

---

## L1-B — pre-warm từ phổ biến (backend)

**Cách gỡ:** `DictionaryWarmupService` nạp `resources/dictionary/common-words.txt` (**849 từ**, nguồn
public-domain, xem header file) vào Redis lúc khởi động (`ApplicationReadyEvent`, chạy nền) + hằng đêm.

**Ba tính chất bắt buộc (rút ra từ ĐO, không phải suy đoán):**
1. **SERIAL** — upstream chặn song song (đo trên), nên warm song song là tự bắn vào chân.
2. **Có trần** — `dictionary.warmup.limit` (default 500).
3. **Skip từ lẻ + breaker** — lần chạy ĐẦU với chính sách "abort ở lần rỗng đầu tiên" **dừng ở từ 1**
   (`aborted at 'of' (1/500)`) vì "of" là hàm từ upstream không phục vụ. Đã sửa: bỏ qua từ rỗng, chỉ
   **dừng sau 5 lần rỗng LIÊN TIẾP** (dấu hiệu upstream sập). Test phủ cả 3 nhánh.

**Mặc định TẮT** (`dictionary.warmup.enabled=false`), compose bật `true` — để JVM test (nạp
`application.properties`, bắn `ApplicationReadyEvent`) **không** đụng mạng.

**Đo thực (trung thực):** cold **vẫn ~20 s/từ** (đo `have`=30.2 s, `the`=20.1 s, `run`=19.4 s sau khi xoá
cache). Warm-up **trả giá đó MỘT LẦN ở nền** rồi mọi lần tra sau **~10 ms** (`have` 0.015 s).
⇒ Nó gỡ **chi phí cho người dùng**, không gỡ **độ trễ upstream**.

**Tốc độ warm-up đo được (log thật, 2026-09-27):** `~21 s/từ` (25 từ trong ~9 phút) ⇒ **200 từ ≈ 67 phút**
— vừa cửa sổ đêm, KHÔNG vừa lúc khởi động. Vì thế `dictionary.warmup.limit` default = **200** (không 500),
và `enabled` mặc định **false** (compose bật `true`). Log sống: `Dictionary warm-up progress: 25/200 (skipped 7)`
⇒ **skip hoạt động thật** (7 hàm từ như "of" bị bỏ qua, không abort).

**Lưu ý nhiễm bẩn (đã tự bắt):** lần đầu tôi thấy log "500 từ trong 0.1 s" — đó là do lần chạy stub L2
trước đã cache mọi từ. Đã xoá cache + đo lại trên upstream thật để có số đúng ở trên.

---

## L2 — negative cache cho "không có từ" (backend) — **GỠ HOÀN TOÀN, chứng minh end-to-end**

**Vấn đề:** `@Cacheable(unless="#result=='[]'")` gộp **404 thật** và **lỗi tạm** vào cùng `"[]"` rồi
không cache cái nào ⇒ tra lại từ không tồn tại vẫn tốn ~20 s.

**Cách gỡ:** `DictionaryService` tách hai nguyên nhân, dùng **hai cache TTL khác nhau**:
- `dictionary` — payload thật, TTL `cache.ttl-hours` (1 h);
- `dictionaryMiss` — **404 đã xác nhận**, TTL `dictionary.miss-ttl-minutes` (30');
- **lỗi** (timeout/5xx/522/null) → **KHÔNG** cache.

`RedisConfig` thêm `withInitialCacheConfigurations("dictionaryMiss", 30')`.

**Kiểm chứng TTL sống trên Redis thật** (chứng minh per-cache TTL MERGE, không thay thế default):
```
dictionary::have      ttl=3253   (~1 h  = cache.ttl-hours, đúng)
dictionaryMiss::was   ttl=1212   (giảm từ 1800 = 30 phút, đúng)
dictionaryMiss::be    ttl=1388
```
Và `was`/`be`/`for` được ghi vào miss-cache vì upstream **thật sự trả 404** cho chúng (kiểm chứng độc lập:
`curl https://api.dictionaryapi.dev/api/v2/entries/en/was` → **404**). ⇒ negative cache + skip của warm-up
đều đúng: hàm từ upstream không phục vụ thì được nhớ là "không có" (30'), warm-up bỏ qua.

**Chứng minh end-to-end** (`sweep/harness/l2-negative-cache-proof.py`, stub upstream + **Redis thật**):
```
missing:  1st 0.03s body=[] | 2nd 0.04s body=[] | stub calls=1
  negative-cached (2nd does not re-hit upstream): True
flaky:    1st body=[] | 2nd body=[{"word":"flaky",...}] | stub calls=2
  failure NOT cached (2nd re-hits upstream and succeeds): True
realword: 1st [...] | 2nd [...] | stub calls=1
  positive payload cached (2nd does not re-hit upstream): True
L2 RESULT: PASS
```

**Phát hiện trung thực:** `dictionaryapi.dev` **hiện trả HTTP 522** cho **mọi** từ không tồn tại
(Cloudflare origin-timeout), **không phải 404**. Nghĩa là trên upstream thật, "không có từ" bị coi là
**lỗi tạm** (đúng: không cache). Đường 404 vì thế khó thấy live — nên tôi (a) chứng minh bằng stub, và
(b) làm `dictionary.upstream-url` **configurable** để có thể trỏ dictionary self-host.

---

## L3-C5 — fixture giọng NGƯỜI có license (thay clip PII)

**Vấn đề:** probe cũ đọc một clip học viên nằm trong MinIO local — **không commit, không tái lập, có PII**.

**Cách gỡ:** fixture **trong repo**: `sweep/harness/fixtures/human-speech-librispeech.wav`
(LibriSpeech `2277-149896-0000`, **CC BY 4.0**, 16 kHz mono, 210 958 B, sha256 `73c1489a…`,
provenance đầy đủ ở `fixtures/README.md`). Probe tự **tạo prompt riêng** (referenceText = đúng câu của
fixture) rồi **tự xoá** prompt + row + object + study_days.

**Kết quả (`python sweep/harness/g8-speaking-human-audio.py`):**
```
fixture: human-speech-librispeech.wav bytes=210958
prompt created: 201 id=91906
upload: 200 id=50055
assess: 200 status=COMPLETED transcript_len=116 scoreTotal=6.0
transcript head: He was in a fevered state of mind, owing to the blight his wife's action …
cleanup: row deleted=1 ; minio: Removed `…_audit-v17-human.wav`
cleanup: study_days rows removed=1 ; prompt deleted=yes
minio copy gone: True | repo fixture intact: True (bytes 210958 -> 210958)
reference/transcript word-set recall: 0.95
G8 RESULT: PASS
```
Transcript khớp reference (recall **0.95**); dọn sạch; fixture trong repo nguyên vẹn.

---

## L3-C6-a — SePay configurable (KHÔNG có sandbox)

- `sepay.api-base-url` (`SEPAY_API_BASE_URL`) — `SePayApiService` không còn hardcode host; default vẫn
  là production `https://my.sepay.vn/userapi`. Đổi sang sandbox `https://userapi-sandbox.sepay.vn` **chỉ
  bằng env**, không sửa code.
- `.env.example` thêm `SEPAY_API_BASE_URL` kèm ghi chú.

**Giới hạn còn lại (nói thật):** người dùng **không có tài khoản Test mode** ⇒ test `g6` vẫn dùng **secret
local**; nó chứng minh **logic** HMAC/replay (đã verify + negative + replay + tự dọn), **không** chứng
minh chữ ký do SePay sinh. Muốn end-to-end thật: tạo tài khoản Test mode, trỏ webhook tới Funnel URL
(`https://engflow-dev.tail7fd1fe.ts.net`) — nay chỉ cần đổi env.

---

## Kết quả tổng

| Suite | Kết quả | Log |
|---|---|---|
| Backend (JUnit) | **536 / 0 / 0 / 11** (was 520; +16 test mới) | `backend-removelimits.log` |
| Frontend (Vitest) | **194 pass / 1 skip** | `frontend-removelimits.log` |
| Frontend build | ✓ built | `build-removelimits.log` |
| L2 end-to-end (stub) | **PASS** (kèm restore) | `l2-negative-cache-proof.py` |
| G8 (speaking, fixture người) | **PASS** (recall 0.95) | mục trên |
| G6 (SePay) | **PASS** | — |
| assert-harness | **ALL CLEAN** | — |
| api-sweep | **145 / 0 / 0** | `api-sweep.json` |
| deep-probe | **58 / 0 / 0** | `deep-probe.json` |
| ui-sweep | exit 0 (`consoleErrors:0, guardFails:0, overflowRoutes:0`) | `ui-sweep.json` |
| parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + `STUDY_DAYS=4 PENDING_PAYMENTS=0 EXERCISE_ATTEMPTS=33` | — |

**Mutation-test:** L2 (bỏ ghi miss-cache → 2 test FAIL) · L1-A (trần cứng về 6 s → ca "keeps waiting" FAIL).

## Review chéo (refute-first) — defect TỰ BẮT + REVIEWER BẮT trong chính việc này

**Tự bắt (chạy thật, không suy đoán):**

1. **F-17-36 (MED)** — thêm constructor tiện dụng thứ hai cho `DictionaryService` ⇒ Spring **không khởi
   động được** (`No default constructor found`), backend crashloop. Bắt được **vì chạy thật** (rebuild +
   restart), không phải chỉ compile. Sửa: 1 constructor duy nhất.
2. **F-17-37 (LOW)** — warm-up abort ở **lần rỗng đầu tiên** ⇒ dừng ở **từ 1** (`aborted at 'of' (1/500)`)
   vì "of" là hàm từ upstream không phục vụ. Sửa: skip từ rỗng, chỉ dừng sau **5 lần rỗng liên tiếp**.
   Log sống sau sửa: `25/200 (skipped 7)`.
3. **F-17-38 (LOW, probe)** — `l2-negative-cache-proof.py` docstring ghi "restores the backend env" nhưng
   code **không** restore ⇒ sau khi chạy, backend bị bỏ trỏ vào stub. Sửa: `finally` restore về upstream
   thật + xoá cache stub; chạy lại xác nhận `restored backend upstream -> https://api.dictionaryapi.dev/...`.
   (Cũng sửa `redis_del` dùng `EVAL` thay `xargs` vì form cũ lỗi `unmatched single quote`.)

**Reviewer bắt (subagent `general-purpose` refute-first) — 5 defect, tất cả ĐÚNG, đã sửa:**

4. **F-17-39 (HIGH)** — `hit.put(...)` nằm TRONG `try` ⇒ lỗi **ghi cache** bị catch chung nuốt và trả `"[]"`
   cho một từ **đã lấy được** ⇒ báo "không có từ" SAI. Sửa: tách ghi cache ra `putQuietly()` (best-effort).
   **Mutation-test:** đưa `put` trở lại trong `try` → `cacheWriteFailure_doesNotFabricateNotFound` **FAIL**.
5. **F-17-40 (MED)** — đọc cache nằm NGOÀI `try` ⇒ Redis chết là **HTTP 500** (controller không có handler),
   trái chú thích "fail-soft". Sửa: bọc đọc cache, lỗi → rơi xuống upstream. Test mới phủ.
6. **F-17-41 (MED)** — `SEPAY_API_BASE_URL` trong `.env.example` để **rỗng**; `${X:default}` **KHÔNG** áp
   default khi biến có-nhưng-rỗng ⇒ base="" → URI tương đối `/transactions/list` → `URI is not absolute` ⇒
   **tắt ngầm mọi poll SePay**. Sửa: `SePayApiService` coi blank = production host + test mới.
7. **F-17-42 (LOW)** — compose **không forward** `SEPAY_API_BASE_URL` (`.env` không mount vào container) ⇒
   "đổi sandbox bằng env" bất khả thi trong deployment thật. Sửa: forward trong service `backend`.
8. **F-17-43 (MED)** — `warmNightly()` chạy vòng serial trên **thread scheduler** (pool mặc định = 1) ⇒ với
   `limit=200` (~67 phút), `SePayPollingScheduler` (fixedDelay 60 s) bị **chặn** suốt cửa sổ ⇒ ngừng tự phát
   hiện chuyển khoản. Sửa: chạy trên **daemon thread riêng** (giống nhánh startup).

**Reviewer xác nhận SẠCH:** `RedisConfig` (merge TTL đúng, prefix `name::` không đụng nhau) · `SePayApiService`
constructor (chỉ 3 test site đổi, default host giữ nguyên) · `vocabularyService.js` (timer clear mọi nhánh,
`onSlow` không thể nổ sau khi resolve) · `SearchVocabulary.vue` · `docker-compose.yml` (`:-` fallback đúng) ·
g8/l2 harness (prompt xoá trong finally; l2 restore) · warm-up breaker (bounded, `enabled=false` chặn cả 2 lối).

## Giới hạn CÒN LẠI (nói thật)

- **L1:** dependency upstream **vẫn còn** (theo thiết kế của bạn). L1-A gỡ *triệu chứng treo*; L1-B gỡ
  *chi phí cho từ phổ biến* (nay ~10 ms sau warm). Từ **lạ ngoài 849 từ** vẫn ~20 s lần đầu (nay có
  trạng thái rõ, không báo lỗi sai).
- **L2:** gỡ hoàn toàn cho **404 thật**; nhưng upstream hiện trả **522** cho từ không tồn tại nên trên
  upstream thật đường 404 ít gặp (đúng đắn là không cache 522).
- **L3-C5:** fixture là bản ghi **có license, tái lập từ repo** — không phải "phiên production".
- **L3-C6:** **không có sandbox** ⇒ vẫn secret local; C6-a chỉ làm **sẵn sàng** đổi sandbox/prod.
