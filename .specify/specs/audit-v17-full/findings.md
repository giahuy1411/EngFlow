# audit-v17-full — findings

Quy ước: `F-17-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy ·
**probe thứ 2 độc lập**. Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`.

---

## F-17-01 — Toast "thành công" dùng `bg-accent text-white` = **4.23:1 (FAIL AA)** — **LOW (a11y)** — `FIXED`

**Bằng chứng:** `frontend/src/composables/useToast.js:34` `case 'success': return 'bg-accent text-white'`.
Đo WCAG: `#FFFFFF` trên `#8B5CF6` = **4.23:1** — dưới ngưỡng **4.5:1** của WCAG 1.4.3 cho body text
(Hiến pháp P7 bắt buộc AA). Đây là **đường duy nhất còn sót** không đi qua lớp `*-ink`/`*-strong` mà audit-v11 F132
đã áp cho mọi call site khác.

**Root cause:** toast render inline trong `App.vue:20-37` qua `toastBackground()`, không dùng token AA.

**Fix:** `bg-accent text-white` → `bg-accent-strong text-white` (`#7C3AED` = **5.70:1**, cùng hệ tím).

**Bằng chứng pass (probe 2 độc lập):**
- Đo lại công thức WCAG: `#FFFFFF` trên `#7C3AED` = 5.70 ≥ 4.5 ✅.
- `frontend/src/__tests__/audit-v17-regressions.test.js` — assert `toastBackground('success')` **không** chứa
  `bg-accent` và **có** `bg-accent-strong` (3/3 pass).

**Test hồi quy:** `audit-v17-regressions.test.js` (F-17-01 block).

---

## F-17-02 — Guard "< 2 ký tự" chỉ có ở backend; UI vẫn gọi tra từ 1 ký tự (~20 s) — **LOW (UX + docs)** — `FIXED`

**Bằng chứng (đo thật, chrome-devtools MCP):** ở `/search`, gõ `h` rồi bấm "Tra từ" →
Network **có** `GET /api/vocabulary/dictionary/h` (đã bắt ở `reqid=475`). Doc §4.2 nói *"gõ dưới 2 ký tự thì không
tìm"*, nhưng guard `<2` chỉ ở backend `/api/vocabulary/search` (`VocabularyController:46`) — **không** ở frontend,
**không** ở `/dictionary/{word}` (`VocabularyController:61`). Tra từ điển ngoài đo **~20 s** khi cache lạnh
(F-17-05) → 1 ký tự vẫn tốn request thật.

**Root cause:** `SearchVocabulary.vue:115 search()` chỉ guard `!query.value.trim()` (rỗng), không guard độ dài;
view dùng đường `/dictionary/` (không có guard backend).

**Fix:** thêm guard `term.length < 2` trong `search()` — xoá kết quả, không gọi API (đúng hành vi doc mô tả).

**Bằng chứng pass (probe 2):**
- Sau fix, gõ `h` + "Tra từ" → Network **chỉ có `GET /api/auth/me`**, **không** có `/dictionary/h` ✅.
- `audit-v17-regressions.test.js` assert source có `term.length < 2`.

**Test hồi quy:** `audit-v17-regressions.test.js` (F-17-02 block).

---

## F-17-03 — Nút UI là "Kiểm tra"/"Nộp bài", doc ghi "Chấm thử" — **LOW (docs)** — `OPEN`

**Bằng chứng:** UI thật (MCP) hiện nút **"Kiểm tra"** (per-question) và **"Nộp bài (6 câu)"**;
`grep "Chấm thử" frontend/src` = **0 hit**. Nhãn trong `LessonExerciseTab.vue:108,116` là `Kiểm tra` / `Nộp bài`.
Demo doc §2.2 dùng chữ **"Chấm thử"** và §Phụ lục A cũng vậy.

**Fix (Phase 8):** sửa demo doc dùng đúng nhãn UI ("Kiểm tra" thay "Chấm thử"), tránh hội đồng bấm nhầm.

---

## F-17-04 — Doc nói "không có trường `correctAnswer`" — thực tế **có nhưng null** — **LOW (docs)** — `OPEN`

**Bằng chứng:** response `GET /api/lessons/445/exercises` (student) chứa key `correctAnswer` trên **mọi** exercise,
giá trị **`null`**. An ninh **ĐÚNG** (không lộ giá trị đáp án), nhưng câu chữ doc sai.
(`LessonExerciseController:28-48` chặn `includeAnswers=true` cho non-admin → 403.)

**Fix (Phase 8):** sửa doc: *"trường `correctAnswer` có mặt nhưng luôn `null` với học viên"*.

---

## F-17-05 — Tra từ điển ngoài chậm ~20 s khi cache lạnh — **MEDIUM (UX)** — `OPEN` (đặc tính)

**Bằng chứng (đo 2 vòng):**
- Vòng 1: `/api/vocabulary/dictionary/{world,book,present,test}` trả `[]`; `hello` trả 2007 B.
- Vòng 2: `world/book/test` → **200 sau ~19.5 s**; host → `dictionaryapi.dev` = **200 sau 20.7 s**.
- Local fallback (`vocabulary`) chỉ **118 từ**, **không** chứa các từ demo (hello/world/book/…).

**Root cause:** upstream `dictionaryapi.dev` ~20 s từ mạng VN (đúng như comment `DictionaryService:12`);
`@Cacheable` 1h cứu các lần sau, nhưng lần đầu mỗi từ vẫn ~20 s. App **degrade đúng** (`[]` → DB local → "Không tìm thấy từ").

**Đề xuất:** giữ nguyên code (không phải bug), nhưng (a) sửa doc không hứa `hello` tức thì; (b) cân nhắc warm cache
trước demo. **Chưa sửa code** — không có win rõ, và P5 cấm "tối ưu" khi chưa đo được cải thiện.

---

## F-17-06 — `ui-sweep` đếm `[warn]` YouTube iframe thành "console error" — **LOW (harness)** — `OPEN`

**Bằng chứng:** `ui-sweep` TOTALS `consoleErrors: 1`, tại dòng `student /videos/1 … err=1`.
MCP xác nhận message là `[warn] Failed to execute 'postMessage' … youtube.com` — **third-party**, không phải lỗi app.

**Đề xuất:** harness nên phân biệt `console.error` vs `console.warn`, hoặc whitelist origin YouTube — tránh
báo động giả. Không sửa app (đúng hành vi).

---

## F-17-07 — `exercise_attempts` không nằm trong cleanup/parity (residue lớp F-16-01) — **LOW (harness)** — `OPEN`

**Bằng chứng:** `grep exercise_attempts sweep/**` = **0**; harness nộp bài (MCP walkthrough, và có thể probe khác)
tạo row thật. Phiên này: U8 tạo `attempt_id=70147` (đã **dọn**); còn `70144` (lesson 447, 14:07 — **trước** phiên
này, 21:40) = residue cũ. Tổng `exercise_attempts` = 37 (v16 ghi 28 — tăng 9, chưa giải thích được hết).

**Đề xuất:** thêm `exercise_attempts` vào `assertClean` + cleanup của harness (cửa sổ half-open, chỉ 2 tài khoản probe).
**Chưa sửa** vì cần xác định chắc chắn probe nào tạo để không xoá oan (R4).

---

## Bác bỏ (CLOSED — probe SAI)

### F-17-08 — "`vocabularyService` gọi dictionaryapi.dev **direct trước**" → **SAI**
Claim từ Plan-agent. Đọc `vocabularyService.js:152-153`: **`backendFallback()` được thử TRƯỚC**, direct là cuối
(`:155-160`). → **CLOSED (probe SAI)**. Ghi lại để không lặp (đúng kỷ luật: reviewer cũng có thể sai).

---

## F-17-09 — Fix F-17-03 chưa trọn: còn fallback `#64748B` (giá trị đã bị thay) — **LOW (a11y)** — `FIXED` *(review chéo bắt được)*

**Nguồn:** review chéo đối kháng (subagent) — không phải suy đoán.

**Bằng chứng:** `frontend/src/utils/lessonLevels.js:20,30` — cả `levelColor()` và `levelInkColor()` rơi về
`'var(--geo-muted-fg, #64748B)'`. `#64748B` là giá trị audit-v11 **F138 đã thay** bằng `#556070` **vì lý do
contrast** (4.34:1 trên panel muted < 4.5:1). Nó là **màu chữ** ở `Lessons.vue:106`
(`:style="{ color: levelInkColor(lesson.level) }"`). Cùng lớp lỗi với F-17-03 (fallback mang giá trị đã gỡ).

**Root cause:** fix F-17-03 chỉ quét `Lessons.vue`, bỏ sót file util cùng vai trò.

**Fix:** cả 2 fallback → `#556070` (giá trị token thật).

**Probe 2 (độc lập):** `grep -rniE "#(E5DECF|F5F0E6|EAE4D6|64748B)" frontend/src` sau fix → chỉ còn **trong comment**
(`design-system.css` 3 dòng ghi chú lịch sử, `Pagination.vue` 1 comment). Test mới assert file không chứa giá trị gỡ.

---

## F-17-10 — Test hồi quy F-17-02 **false-pass** (regex khớp cả guard bị comment-out) — **LOW (test)** — `FIXED` *(review chéo bắt được)*

**Nguồn:** review chéo đối kháng.

**Bằng chứng:** assertion cũ `expect(src).toMatch(/term\.length\s*<\s*2/)` — chạy thật:
```
current code        : true
commented-out guard : true   ← "// if (term.length < 2) {"
no-op guard         : true   ← "if (term.length < 2) { void 0; }"
```
→ Test **pass dù fix bị revert** (không bảo vệ gì).

**Fix:** viết lại **behavioral** — mount `SearchVocabulary.vue`, click "Tra từ", assert
`vocabularyService.search` **không** được gọi với 1 ký tự và **được** gọi với 2 ký tự.

**Probe 2 (mutation-test):** tắt guard (`if (false) {`) → test **FAIL** (`1 failed | 4 passed`); khôi phục → `5 passed`.
→ Test thực sự fail khi fix bị gỡ.

---

## F-17-11 — `POST /api/ai/generate-vocab` trả **500** khi model sinh newline thô trong JSON — **MEDIUM** — `FIXED` *(vòng 2 bắt được)*

**Nguồn:** `api-sweep` **vòng 2** — `ai pass=4 fail=1`: `POST /api/ai/generate-vocab (auth) 200 or quota 429`
→ **got 500**. (Vòng 1 pass → đây là lỗi **liên tục ngắt quãng**, không phải regression của fix vòng này.)

**Bằng chứng:**
- Backend log: `AiVocabService : Failed to parse AI response: Illegal unquoted character ((CTRL-CHAR, code 10)):
  has to be escaped using backslash to be included in string value` → `java.lang.RuntimeException: Failed to
  generate vocabulary`.
- `AiVocabService.java:70` parse **một lớp** `objectMapper.readValue(...)` trong `try/catch` → ném RuntimeException
  → `GlobalExceptionHandler` map 500.
- So sánh: `AiExerciseService` có **salvage 3 lớp** (`:447` "Parse LLM response (3-layer salvage)"). Vocab thì không.

**Root cause:** model local `qwen2.5:1.5b` thỉnh thoảng phát **ký tự điều khiển thô** (newline `\n`) **bên trong**
chuỗi JSON (ví dụ `exampleSentence` nhiều dòng). JSON nghiêm cấm điều này → parse cứng fail → 500. Ngắt quãng
(3 lần gọi lại sau đó 200) nên retry không đủ; phải sửa ở tầng parse.

**Fix:** thêm `readVocabListLenient` / `readVocabMapLenient` — thử **strict trước**, nếu `JsonProcessingException`
thì parse lại bằng mapper **lenient** bật `ALLOW_UNESCAPED_CONTROL_CHARS`. Dùng cho cả `generateVocabByTopic`
và `enrichWord`.

**Bằng chứng pass (probe 2 độc lập):**
- `AiVocabServiceLenientParseTest` **5/5** — có ca tái hiện **đúng** lỗi (`"I plan to travel\nto Europe."` thô)
  và ca "JSON hỏng thật vẫn phải ném" (không nuốt lỗi).
- Sau rebuild: gọi live 4 lần → **HTTP 200** (3–4 s). 500 không còn.

**Test hồi quy:** `src/test/java/com/datn/engflow/service/AiVocabServiceLenientParseTest.java`.

---

## F-17-12 — Timeout AI vocab **hardcode 30 s** (các đường AI khác đều cấu hình được) — **MEDIUM** — `FIXED` *(cùng lớp F-17-11)*

**Nguồn:** khi verify fix F-17-11 sau rebuild, endpoint trả **504** liên tục; đào tiếp thì lộ nguyên nhân thứ hai.

**Bằng chứng:** `AiVocabService.java:203` `.timeout(Duration.ofSeconds(30))` — hardcode. Log:
`TimeoutException: Did not observe any item or terminal signal within 30000ms in 'flatMap'`. Trong khi
`application.properties` có `ai.speaking.llm.timeout-seconds=120`, `speaking.assessment.timeout-seconds=120`
(các đường AI khác **đều** cấu hình + rộng hơn). Model swap khi lạnh đo được >30 s.

**Root cause:** 30 s hardcode quá ngắn cho lần gọi nguội.

**Fix:** thêm `ai.vocab.timeout-seconds=${AI_VOCAB_TIMEOUT_SECONDS:120}` vào `application.properties`; inject vào
constructor; thay hardcode.

**Bằng chứng pass (probe 2):** sau rebuild + warm model, gọi live 4 lần → **HTTP 200 (3–4 s)**; không còn 504.

---

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-17-01 | LOW (a11y) | Toast `bg-accent text-white` = 4.23:1 | `FIXED` |
| F-17-02 | LOW (UX/docs) | Guard `<2` chỉ backend; UI gọi tra 1 ký tự | `FIXED` |
| F-17-03 | LOW (docs) | Nút "Kiểm tra" vs doc "Chấm thử" | `OPEN` (fix doc) |
| F-17-04 | LOW (docs) | `correctAnswer` có mặt nhưng null | `OPEN` (fix doc) |
| F-17-05 | MED (UX) | Từ điển ngoài ~20 s cache lạnh | `FIXED` (bounded wait + từ điển là nguồn duy nhất — closing round) |
| F-17-06 | LOW (harness) | ui-sweep đếm YouTube warn thành error | `CLOSED (probe SAI)` — text thật `ERR_UNSAFE_REDIRECT`, 4× không tái hiện |
| F-17-07 | LOW (harness) | `exercise_attempts` không được dọn/parity | `FIXED` |
| F-17-08 | — | Claim tier-order SAI | `CLOSED (probe SAI)` |
| F-17-09 | LOW (a11y) | Fallback `#64748B` còn sót (review bắt) | `FIXED` |
| F-17-10 | LOW (test) | Test F-17-02 false-pass (review bắt) | `FIXED` |
| **F-17-11** | **MED** | **AI vocab 500 khi model sinh newline thô** | `FIXED` |
| **F-17-12** | **MED** | **AI vocab timeout hardcode 30 s** | `FIXED` |
| F-17-13 | LOW (harness) | `ui-sweep` tên ảnh cứng `v13-*` | `FIXED` |
| F-17-16 | LOW (harness) | `_config.js` default `audit-v15-full` | `FIXED` (closing round) |
| F-17-17 | LOW (harness) | `focused-probe.js` hardcode `audit-v15-full` | `FIXED` (closing round) |
| **F-17-27** | **MED (UX)** | **Tra từ còn bước fallback DB local** | `FIXED` (closing round) |
| F-17-28 | LOW (harness) | `api-sweep` còn `blocked` webhook HMAC (lỗi thời) | `FIXED` (closing round) |
| F-17-29 | LOW (harness) | `mc()` probe G8 trả sai stream | `FIXED` (closing round) |
| F-17-30 | LOW (harness) | check 6 quá yếu (không bắt F-17-16) | `FIXED` (closing round, tự review) |
| **F-17-31** | **MED (UX)** | **Fail ở 6 s = lỗi GIẢ (upstream TTFB ~19.5 s)** | `FIXED` (remove-limits L1-A) |
| **F-17-32** | **MED (UX)** | **"Không có từ" không cache → lặp ~20 s** | `FIXED` (remove-limits L2) |
| F-17-33 | LOW (UX) | Từ phổ biến vẫn cold ~20 s | `FIXED` (remove-limits L1-B: pre-warm) |
| F-17-34 | LOW (harness) | Probe C5 phụ thuộc clip MinIO **có PII, không tái lập** | `FIXED` (remove-limits: fixture CC BY 4.0 trong repo) |
| F-17-35 | LOW (config) | `SePayApiService` hardcode host production | `REVERTED` (follow-up: không có sandbox ⇒ biến chết, đã gỡ) |
| F-17-36 | MED (bug) | `DictionaryService` 2 constructor → Spring không khởi động được | `FIXED` (tự bắt khi chạy: bỏ constructor phụ) |
| F-17-37 | LOW (bug) | Warm-up abort ở lần rỗng ĐẦU → dừng ở từ 1 ("of") | `FIXED` (skip từ lẻ + breaker 5 lần) |
| F-17-38 | LOW (probe) | `l2-negative-cache-proof.py` không restore upstream | `FIXED` (finally restore + `EVAL` thay `xargs`) |
| **F-17-39** | **HIGH** | **Ghi cache lỗi → báo "không có từ" SAI cho từ CÓ** | `FIXED` (tách `putQuietly`; mutation-test) |
| F-17-40 | MED | Đọc cache lỗi → HTTP 500 (trái "fail-soft") | `FIXED` (bọc try, rơi xuống upstream) |
| F-17-41 | MED | `SEPAY_API_BASE_URL` rỗng → URI tương đối → tắt ngầm poll | `REVERTED` (follow-up: gỡ hẳn biến ⇒ hết lớp bug) |
| F-17-42 | LOW | compose không forward `SEPAY_API_BASE_URL` | `REVERTED` (follow-up: gỡ biến) |
| F-17-43 | MED | `warmNightly` chạy trên thread scheduler → chặn job khác 67' | `FIXED` (daemon thread riêng) |

---

## F-17-13 — `ui-sweep.js` đặt tên ảnh cứng `v13-*` (drift namespace) — **LOW (harness)** — `OPEN`

**Bằng chứng:** `sweep/harness/ui-sweep.js:340-344` hardcode `name: "v13-home-1440"`, `"v13-lessons-360"`,
`"v13-admin-dashboard-1440"`, `"v13-profile-1280"`, `"v13-premium-768"`, `"v13-lessons-1920"`. Chạy với
`--audit audit-v17-full` vẫn sinh `evidence/shots/v13-*.png` (6 file, 21:50 phiên này) — tên **không** theo namespace.

**Root cause:** đúng lớp lỗi mà `_config.js` ra đời để chặn ("hardcoded audit namespace per round") — sót ở đây vì
là **tên file**, không phải đường dẫn/marker nên `assert-harness.js` không bắt.

**Ảnh hưởng:** chỉ là tên file (evidence vẫn hợp lệ, nội dung đúng); gây nhầm "ảnh v13 nằm trong v17".

**Đề xuất:** đổi sang `AUDIT`-derived prefix (như `MARKER` trong `_config.js`). **Chưa sửa** — ngoài phạm vi "fix lỗi
thật của app"; ghi để vòng sau.

---

# PHẦN RERUN (vòng nối tiếp) — cập nhật trạng thái + finding mới

Chạy lại **toàn bộ** chuỗi v17 + mở rộng (đóng các mục "Chưa làm") + **thêm 1 vòng review chéo**.

## Đổi trạng thái (từ "Chưa làm" → đã xử lý)

| ID | Trạng thái v17 | Trạng thái rerun | Bằng chứng |
|---|---|---|---|
| **F-17-05** (từ điển ~20 s) | `OPEN` (đặc tính) | **FIXED (phần local) + ghi rõ phần còn lại** | local exact fast path: **11–54 ms** thay vì ~20 000 ms cho từ có trong DB; bỏ gọi proxy 2 lần; đo before/after + verify UI thật → `g5-f17-05.md` |
| **F-17-06** (ui-sweep console error) | `OPEN` — *kết luận SAI* (tưởng warn YouTube) | **CLOSED (probe SAI)** + harness chống tái diễn | text thật là `ERR_UNSAFE_REDIRECT` (không phải `postMessage`); 4 lần tái hiện → **0** ⇒ thoáng qua. Thêm `isThirdPartyConsoleNoise` dùng chung → `ui-sweep` 0 |
| **F-17-07** (`exercise_attempts` residue) | `OPEN` | **FIXED** | `cleanupExerciseAttempts` + marker `EXERCISE_ATTEMPTS=` + `assertClean`; guard **tự bắt** residue thật (candidates=1) rồi CLEAN |
| **F-17-13** (ảnh `v13-*`) | `OPEN` | **FIXED** | tên suy từ `VER` → `v17-home-1440.png` … |
| **SePay webhook chữ ký thật** | `BLOCKED` (real-money) | **VERIFIED** | `g6-sepay-signed.py`: valid sig → `{"success":true}` + DB `SUCCESS`; bad sig → `Invalid signature`; tự dọn → `g6-sepay.md` |
| **Speaking media thật** | `BLOCKED` (cần mic) | **VERIFIED** | `g7-speaking-real-audio.py`: TTS WAV thật → upload → assess → **`COMPLETED`, transcript 181 ký tự, score 9.7**; tự dọn row+MinIO+study_days → `g7-speaking.md` |
| **Playwright MCP** | `BLOCKED` (thiếu Chrome channel) | **UNBLOCKED** (đã chứng minh) | `--executable-path` → Brave; probe `browser_navigate` → `/lessons` OK → `g1-playwright-mcp.md` |
| Perf | "không win" | **giữ nguyên** | median 19.8 ms (v17: 19.5) — tái xác nhận, không tối ưu (P5) |

## Finding MỚI trong rerun

### F-17-14 — `assess()` ghi `study_days` nhưng probe/harness không dọn — **LOW (harness)** — `FIXED`
**Nguồn:** assert parity bắt được (`STUDY_DAYS=5` sau G7) — không phải suy đoán.
**Bằng chứng:** `SpeakingSubmissionService:134` gọi `recordStudy()` ⇒ mỗi lần **assess** ghi 1 hàng `study_days`
(user 2, 2026-09-26, id 40091). Probe G7 ban đầu chỉ dọn `speaking_submissions` + MinIO ⇒ **sót study day**.
**Fix:** probe G7 dọn thêm `study_days` (scoped 2 tài khoản probe + hôm nay); ghi vào `AGENTS.md`.
**Probe 2:** chạy lại → `study_days rows removed=1`, parity về `STUDY_DAYS=4`.

### F-17-15 — Probe G7 bỏ lại object MinIO (đọc sai field) — **LOW (harness)** — `FIXED`
**Bằng chứng:** lần chạy đầu `G7 PASS` nhưng `mediaKey=None` (response trả `mediaUrl`, **không** trả
`media_object_key`) ⇒ object 1020 KiB **bị bỏ lại** trong bucket. Phát hiện bằng cách **liệt kê object hôm nay**.
**Fix:** đọc `media_object_key` **từ DB trước khi** xoá row; đã **xoá object orphan**; chạy lại → `Removed …`,
`minio object gone: True`.

### F-17-16 — `_config.js` mặc định còn trỏ `audit-v15-full` — **LOW (harness)** — `FIXED` (closing round)
**Bằng chứng:** `sweep/harness/_config.js:34` `arg("audit", "audit-v15-full")` — lệch 2 vòng so với thực tế.
**Fix:** default → `audit-v17-full`; thêm **check 6** vào `assert-harness.js` (FAIL nếu default không trỏ thư mục
có thật). Chạy: `PASS  _config.js default audit round exists  -- default=audit-v17-full`.

### F-17-17 — `focused-probe.js:188` hardcode `audit-v15-full` — **LOW (harness)** — `FIXED` (closing round)
Cùng lớp F-17-13 nhưng ở file khác (`path.join(..., "audit-v15-full", "evidence", "focused-probe.json")`).
**Fix:** dùng `require("./_config.js").OUT`; thêm **check 7** vào `assert-harness.js` (cấm literal
`.specify/specs/audit-vN-full` ngoài `_config.js`). Chạy: `PASS  no harness hardcodes an audit round path outside _config.js`.

---

## CLOSING ROUND (2026-09-27) — đóng nốt 5 mục "Còn lại"

### F-17-27 — Tra từ còn bước fallback DB local trên đường tra — **MEDIUM (UX)** — `FIXED`
**Bằng chứng:** sau vòng rerun, `vocabularyService.backendFallback()` bước 2 vẫn gọi
`/api/vocabulary/search` khi proxy trả `[]` ⇒ bảng local (0/118 có audio, 1 nghĩa) vẫn có thể **thắng**
một tra từ. Trái quyết định "từ điển là nguồn duy nhất".
**Fix:** gỡ hẳn bước DB fallback + helper `mapBackendRows` + `exact` param + `findByWordIgnoreCase`;
thêm trần chờ `DICT_BUDGET_MS=6000`. Đo: cold 19.99 s → warm 0.032 s; UI tra chỉ gọi
`/api/vocabulary/dictionary/*` (không có `/api/vocabulary/search`); decks/SRS/game/flashcard/AI vẫn xanh
→ `c2-dictionary-only.md`.

### F-17-28 — `api-sweep` còn đánh dấu webhook HMAC hợp lệ là `blocked` (lỗi thời) — **LOW (harness)** — `FIXED`
**Bằng chứng:** `sweep/v12/api-sweep.js:539` `blocked("webhook with VALID HMAC signature", "real-money boundary")`
— đã **sai** từ vòng rerun (G6 chứng minh test được, tự dọn).
**Fix:** thay bằng **assert thật không đụng tiền thật** (ký hợp lệ payload có orderCode không tồn tại → server
qua verify rồi trả `"No pending order"`, không mutate) + **replay cũ bị từ chối**. `api-sweep`: **145/0/0**
(trước 143/0/1). Cũng thêm ca replay-window vào `g6-sepay-signed.py`.

### F-17-29 — `mc()` trong probe G8 trả sai stream — **LOW (harness)** — `FIXED`
**Bằng chứng:** `mc rm`/`mc stat` in ra **stdout**, nhưng helper trả stderr ⇒ kiểm tra "object gone" là may mắn.
**Fix:** `mc()` trả `(stdout_bytes, stdout+stderr_text)`; kiểm tra gốc bằng **so byte-length** khi tải lại.

### F-17-30 — `assert-harness` check 6 QUÁ YẾU (không thật sự bắt F-17-16) — **LOW (harness)** — `FIXED`
**Bằng chứng (tự review refute-first bắt):** bản đầu chỉ kiểm "default trỏ thư mục **có thật**"; nhưng
`audit-v15-full/` **vẫn tồn tại** ⇒ **revert** default về v15 **vẫn PASS**. Guard không như tài liệu claim.
**Fix:** check 6 so với **vòng cao nhất** `audit-vN-full`; **mutation-test**: revert v15 → `FAIL default=audit-v15-full latest=audit-v17-full`.


---

## VÒNG REVIEW CHÉO THỨ 2 — defect do reviewer bắt trong chính fix của rerun

Subagent `general-purpose` (refute-first) — 6/6 vùng đều có kết luận. Tác giả **đối chiếu lại từng claim**
bằng SQL/test trước khi sửa (reviewer cũng có thể sai — nhưng lần này **đúng cả 7**).

### F-17-18 — Regex noise là substring thô, GIẤU lỗi thật — **MEDIUM (harness)** — `FIXED`
**Bằng chứng (tự verify):** `isThirdPartyConsoleNoise("TypeError: Cannot read properties of null (reading 'youtubeVideoId')")`
→ **true** ⇒ một lỗi app THẬT bị nuốt ⇒ `ui-sweep` báo 0 console error dù có bug.
**Fix:** neo regex vào **chữ ký third-party thật** (`Failed to load resource:.*(youtube|favicon|…)`, `postMessage.*youtube`,
`Unrecognized feature:.*(compute-pressure|web-share)`) — 10 ca test: lọc đúng noise, **KHÔNG** lọc `TypeError`/500/ERR_CONNECTION_REFUSED.

### F-17-19 — Error contract: offline bị báo TIMEOUT thay vì NETWORK_ERROR — **LOW (UX)** — `FIXED`
**Bằng chứng:** `vocabularyService.js` nhánh 2 lần `directFetch` thất bại → `throw new Error('TIMEOUT')` **vô điều kiện**;
offline là `TypeError: Failed to fetch` (không phải `AbortError`) ⇒ UI hiện "từ điển chậm" thay vì "lỗi mạng".
**Fix:** `directErr.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR'`; test `vocabulary-search-errors.test.js` **4/4**.

### F-17-20 — Local-first là REGRESSION chất lượng kết quả — **MEDIUM (UX)** — `FIXED`
**Bằng chứng (tự verify):** `SELECT … FROM vocabulary` → **118/118 hàng `audio_url IS NULL`**; `mapBackendRows` cho
1 meaning/0 synonym. Từ điển cho audio + nhiều nghĩa + synonym. Local-first ⇒ **mất nút phát âm + nghĩa** cho mọi từ
Oxford3000.
**Fix:** **đảo thiết kế** — từ điển **ưu tiên**; local chỉ là **fast fallback** khi từ điển chậm (>1.5 s) hoặc lỗi.
Test `vocabulary-search-order.test.js` **4/4** (ưu tiên rich entry; fallback khi chậm; fallback khi lỗi; ≤1 proxy call).

### F-17-21 — Baseline global vs cleanup scoped → DIRTY giả — **LOW (harness)** — `FIXED`
**Bằng chứng:** marker đếm **cả bảng** trong khi cleanup chỉ xoá **2 tài khoản probe**; user thật 150040 có 3 hàng ⇒
học viên thật nộp 1 bài là `assertClean` báo DIRTY oan.
**Fix:** marker **chỉ đếm 2 tài khoản probe** (33 hàng); **đã chứng minh**: INSERT hàng user 150040 → marker **vẫn 33**.

### F-17-22 — G6 không hoàn nguyên premium — **LOW (probe)** — `FIXED`
**Bằng chứng:** settle gọi `PaymentService.processSePayTransaction:193-195` → `setIsPremium(true)` + `setPremiumExpiry(...)`;
mỗi lần chạy **đẩy expiry thêm 1 tháng**, parity không thấy. (Đã xác nhận user 2 bị đẩy `2026-10-03`→`2026-10-26`.)
**Fix:** đọc premium **trước** khi settle, **hoàn nguyên** trong `finally`; đã trả user 2 về `2026-10-03`.

### F-17-23 — Negative control của G6 chỉ in, không assert — **LOW (probe)** — `FIXED`
**Fix:** assert `Invalid signature`/`success:false` và đưa vào `ok`.

### F-17-24 — G7 dùng `GETDATE()` (UTC) cho `study_date` (VN) → false pass theo giờ — **LOW (probe)** — `FIXED`
**Bằng chứng:** SQL Server UTC, `study_date` ghi theo VN ⇒ 17:00–24:00 UTC (= 00:00–07:00 VN hôm sau) DELETE khớp **0 hàng**.
**Fix:** tính ngày VN bằng `time.gmtime(now + 7h)`; in `cleanup window (VN date): 2026-09-26`.

### F-17-25 — Cả 2 probe thiếu try/finally + verdict bỏ qua cleanup — **LOW (probe)** — `FIXED`
**Fix:** bọc try/finally (dọn cả khi bước giữa lỗi — đã gặp thật khi `time` chưa import: row 40050 bị bỏ lại, **đã dọn tay**);
verdict G7 nay gồm `gone` + `sd_deleted`; verdict G6 gồm `bad_rejected` + `restored`.

### F-17-26 — `ui-sweep.js:374` log `s.name` (đã bỏ khỏi SHOTS) — **LOW (cosmetic)** — `FIXED`
**Fix:** dùng `shotName(s)`.

---

## REMOVE-LIMITS ROUND (2026-09-27) — gỡ 3 giới hạn

### F-17-31 — Fail ở 6 s là lỗi GIẢ — **MED (UX)** — `FIXED`
**Bằng chứng (đo tách tầng):** upstream `dictionaryapi.dev` TTFB **19.494 s**, còn DNS 0.075 + connect 0.117
+ TLS 0.173 s ⇒ **server của họ** chậm, không phải mạng VN. `DICT_BUDGET_MS=6000` cũ ném `TIMEOUT` ⇒ UI báo
"Tra cứu quá lâu" cho từ **sẽ** tra được ở ~20 s.
**Fix:** `DICT_BUDGET_MS` thành ngưỡng **mềm** (`options.onSlow`), trần cứng `DICT_TOTAL_MS=45000`; **không**
gửi lại request (đo: 6 request song song → 5 timeout). **Mutation-test**: trần về 6 s → ca "keeps waiting" FAIL.

### F-17-32 — "Không có từ" không cache → lặp ~20 s — **MED (UX)** — `FIXED`
**Bằng chứng:** `@Cacheable(unless="#result=='[]'")` gộp 404 thật + lỗi tạm vào `"[]"` rồi không cache.
**Fix:** hai cache (`dictionary` 1 h / `dictionaryMiss` 30'), lỗi KHÔNG cache. **Chứng minh end-to-end** bằng
stub + Redis thật (`l2-negative-cache-proof.py`): 404 → lần 2 không gọi upstream; 500 → lần 2 gọi lại và OK.
**Ghi chú:** upstream hiện trả **522** cho từ không tồn tại (không phải 404) ⇒ đường 404 khó thấy live.

### F-17-33 — Từ phổ biến vẫn cold ~20 s — **LOW (UX)** — `FIXED`
**Fix:** `DictionaryWarmupService` pre-warm 849 từ phổ biến vào Redis (nền + hằng đêm). Đo: cold ~20 s/từ;
sau warm **~10 ms**. Serial + trần 200 + skip/breaker.

### F-17-34 — Probe C5 dùng clip MinIO có PII, không tái lập — **LOW (harness)** — `FIXED`
**Fix:** fixture **trong repo** `sweep/harness/fixtures/human-speech-librispeech.wav` (LibriSpeech
`2277-149896-0000`, **CC BY 4.0**); probe tự tạo/xoá prompt riêng. Recall 0.95, dọn sạch, fixture nguyên vẹn.

### F-17-35 — `SePayApiService` hardcode host production — **LOW (config)** — `FIXED`
**Fix:** `sepay.api-base-url` (`SEPAY_API_BASE_URL`), default vẫn production; đổi sandbox bằng env.

### F-17-36 — `DictionaryService` 2 constructor → Spring không khởi động — **MED (bug)** — `FIXED`
**Bằng chứng:** backend crashloop `No default constructor found` khi thêm constructor tiện dụng thứ hai.
**Fix:** bỏ constructor phụ; test truyền upstream base tường minh. (Tự bắt khi chạy thật, không phải suy đoán.)

### F-17-37 — Warm-up abort ở lần rỗng ĐẦU → dừng ở từ 1 — **LOW (bug)** — `FIXED`
**Bằng chứng:** `aborted at 'of' (1/500)` — "of" là hàm từ upstream không phục vụ ⇒ chính sách abort-ngay
quá giòn. **Fix:** skip từ rỗng, chỉ dừng sau **5 lần rỗng liên tiếp**. Log thật: `25/200 (skipped 7)`.

---

## FOLLOW-UP (2026-09-27) — nguồn word-list + gỡ biến SePay chết

### F-17-44 — Danh sách pre-warm không có provenance đứng được — **LOW (data)** — `FIXED`
**Bằng chứng:** header `common-words.txt` tự khai *"hand-entered … the exact upstream release was NOT
pinned; treat the ordering as approximate"*, và kích thước **849** dù header nói "1000 most common".
**Fix:** thay bằng **NGSL 1.2** (Browne, Culligan & Phillips) — **2 809 headword**, **CC BY-SA 4.0**
(site ghi rõ *"free … including commercial use"*), **xếp theo tần suất** (`SFI Rank`). Nguồn pin được
(`NGSL_12_stats.csv`, ngày tải) + attribution đầy đủ trong header file và `dictionary/README.md`.
Test mới pin shape: `hasSize(2809)`, no duplicates, `[a-z'-]+`, phần tử đầu = `the`.
**Ghi nhận:** list xếp theo tần suất ⇒ đầu list là **hàm từ** (`the be and of …`), một số không được
upstream phục vụ (`be` → 404) ⇒ warm-up skip — đã đo trên list cũ (`25/200 (skipped 7)`) và breaker chịu được.

### F-17-35 / F-17-41 / F-17-42 — **REVERTED** (biến SePay chết)
**Lý do:** `sepay.api-base-url` chỉ để trỏ **sandbox** SePay; người dùng **không có sandbox** ⇒ biến **chết**
mà còn **sinh bug** (rỗng ⇒ URI tương đối ⇒ `URI is not absolute` ⇒ tắt ngầm polling). Gỡ cả biến ⇒
hết **lớp** bug, gọn hơn là vá từng nhánh. `SePayApiService` trở lại hằng số production.
**Câu hỏi người dùng "biến này có tác dụng gì":** chỉ đổi host **polling API** (`…/transactions/list`),
**không** liên quan webhook / QR / chuyển khoản thật.

### Demo chuyển khoản thật (không sandbox) — đã kiểm chứng khả thi
Funnel `https://engflow-dev.tail7fd1fe.ts.net` proxy `/` → `backend:8080`; `POST /api/webhook/sepay`
từ internet → **200**. Không cần code mới. (Theo yêu cầu người dùng: không làm gì thêm.)
