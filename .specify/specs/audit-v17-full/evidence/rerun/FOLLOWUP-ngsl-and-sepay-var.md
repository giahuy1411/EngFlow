# audit-v17-full — FOLLOW-UP: NGSL word list + gỡ biến SePay chết

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` · **Tiền đề:** `76f625f` (remove-limits round)
**Yêu cầu người dùng:** (1) xem lại nguồn danh sách từ pre-warm; (2) hỏi `SEPAY_API_BASE_URL` có tác dụng gì
→ chốt **gỡ bỏ**; (3) SePay **không cần sandbox** — demo bằng **chuyển khoản thật**.

---

## P1 — Thay word-list bằng NGSL 1.2 (provenance + license)

**Vấn đề:** `common-words.txt` (849 từ) tự khai trong header: *"hand-entered … the exact upstream release
was NOT pinned; treat the ordering as approximate"* — không có provenance đứng được.

**Nguồn mới (pin được):** **New General Service List 1.2** — Browne, C., Culligan, B., & Phillips, J.
File dùng: `NGSL_12_stats.csv` (cột `SFI Rank` cho thứ tự tần suất). Tải 2026-09-27 từ
`https://www.newgeneralservicelist.com/new-general-service-list`.

| Thuộc tính | Giá trị |
|---|---|
| Số từ | **2 809** headword (đã verify: `grep -vcE "^\s*(#|$)"` = 2809) |
| Thứ tự | **tần suất giảm dần** (`the`, `be`, `and`, `of`, …) |
| License | **CC BY-SA 4.0** — site ghi rõ *"free … including commercial use"* |
| Đối tượng | thiết kế cho **người học ESL**; phủ ~92% tiếng Anh thường |
| Attribution | header file + `dictionary/README.md` (điều kiện của license) |

**Đã cân nhắc và loại:**
- `google-10000-english`: license **LDC — cấm dùng thương mại** (đã đọc LICENSE.md) ⇒ không dùng.
- `wordfreq`: **CC BY-SA 4.0** nhưng tác giả **nói rõ** "đừng chuyển thành CSV vì CSV không giữ được
  attribution" ⇒ không hợp để ship dạng file.
- **Nội dung trong repo** (crawler `*.json`): Explore agent đo được 366 724 token / 10 519 type — **rộng**,
  nhưng **scrape từ `english-practice.net`** (`crawler/crawl.js:7`) ⇒ **không** sạch license hơn danh sách
  hiện tại. Bị loại.

**Đổi gì:** chỉ **dữ liệu** (`common-words.txt` + `README.md` mới). **Không** đổi `DictionaryWarmupService`
(parser/breaker/serial/cap giữ nguyên). `dictionary.warmup.limit` giữ **200** + comment giải thích list 2 809
nhưng upstream ~21 s/từ ⇒ chỉ warm `limit` từ **phổ biến nhất** mỗi lần.

**Test mới** (`DictionaryWarmupServiceTest.readWords_isTheNgslList_cleanAndDeduplicated`):
`hasSize(2809)` · không trùng · mọi từ khớp `[a-z'-]+` · phần tử đầu = `the`. → pin shape, chống regenerate hỏng.

**Ghi nhận đo được — đầu list là hàm từ:** NGSL xếp theo tần suất nên 30 từ đầu là
`the be and of to a in have it you he for they not that we on with this I do as at she but from by will or say`.
Một số không được upstream phục vụ (`be` → **404**; `the`/`and` → 200 — đo được, không nhất quán) ⇒ warm-up
**skip**. Trên list cũ cùng hình dạng, log thật đã cho `25/200 (skipped 7)` và **không** trip breaker ⇒ chịu được.

---

## P2 — Gỡ `SEPAY_API_BASE_URL`

### Trả lời câu hỏi: biến này có tác dụng gì?

**Đúng một tác dụng: đổi host của SePay User API** (`…/transactions/list` — endpoint app *dò* giao dịch,
tức **polling fallback**). Nó **không** liên quan webhook, **không** liên quan tạo QR, **không** nằm trên
đường chuyển khoản thật. Nơi đọc **duy nhất**: `SePayApiService.java:62` → dựng `transactionsListUrl`.

Mục đích gốc (audit-v17 L3-C6-a): cho phép trỏ sang **sandbox** `https://userapi-sandbox.sepay.vn` bằng env.
**Người dùng không có sandbox ⇒ biến chết**, mà còn **sinh bug** (F-17-41: rỗng ⇒ URI tương đối
`/transactions/list` ⇒ `URI is not absolute` ⇒ tắt ngầm polling).

### Đã gỡ

| File | Thay đổi |
|---|---|
| `SePayApiService.java` | bỏ tham số `apiBaseUrl` + `@Value`; trả lại **hằng số** `TRANSACTIONS_LIST_URL`; bỏ field + nhánh blank |
| `application.properties` | xoá `sepay.api-base-url=…` |
| `docker-compose.yml` | xoá dòng forward `SEPAY_API_BASE_URL` |
| `.env.example` | xoá `SEPAY_API_BASE_URL` + comment sandbox |
| `SePayApiServiceTest` | 3 chỗ dựng service → 3 tham số; xoá ca `blankApiBaseUrl_…`; thêm ca khẳng định host production là hằng số |

**Kiểm chứng:** `grep -rn "SEPAY_API_BASE_URL\|api-base-url\|transactionsListUrl" src/ docker-compose.yml
.env.example` → chỉ còn **comment lịch sử** (giải thích vì sao gỡ), **0** tham chiếu thật.

**findings.md:** F-17-35 / F-17-41 / F-17-42 đánh dấu **REVERTED** (trung thực: chúng chỉ tồn tại vì biến
này; gỡ biến là cách sửa gọn hơn vá từng nhánh).

---

## P3 — SePay chuyển khoản thật (kiểm chứng khả thi; KHÔNG làm thêm)

Người dùng xác nhận **không cần sandbox** — demo bằng **chuyển khoản thật**. Đã kiểm chứng đường ống sẵn sàng:

```
tailscale funnel status  ->  https://engflow-dev.tail7fd1fe.ts.net (Funnel on)
                             |-- / proxy http://backend:8080
POST https://engflow-dev.tail7fd1fe.ts.net/api/webhook/sepay  ->  http=200   (từ internet)
GET  https://engflow-dev.tail7fd1fe.ts.net/api/lessons?size=1  ->  http=200
```

Chuỗi thật (không cần code mới): quét QR (`https://qr.sepay.vn/img?acc=…&amount=…&des=ENG…&bank=…`)
→ chuyển tiền → SePay POST webhook tới `…/api/webhook/sepay` (HMAC `"<ts>.<rawBody>"`, replay ±5')
→ app kích premium. Webhook là **kênh chính**; `SEPAY_API_TOKEN` chỉ là **fallback polling** (không tới hạn).
Cần: `SEPAY_WEBHOOK_SECRET` khớp dashboard, `SEPAY_BANK_ACCOUNT`/`NAME` khớp tài khoản đã đăng ký,
URL webhook trong dashboard trỏ về host `ts.net`. **Theo yêu cầu: không làm gì thêm.**

---

## Kết quả tổng

| Suite | Kết quả |
|---|---|
| Backend (JUnit) | **537 / 0 / 0 / 11** (was 536; +1 test NGSL) — BUILD SUCCESS |
| Frontend (Vitest) | **194 pass / 1 skip** |
| Frontend build | ✓ built |
| assert-harness | **ALL CLEAN** |
| g6 (SePay, secret local) | **PASS** |
| parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + `STUDY_DAYS=4 PENDING_PAYMENTS=0 EXERCISE_ATTEMPTS=33` |
| Warm-up list | log thật: `Dictionary warm-up starting: 2809 words (cap 200)` rồi `Dictionary warm-up progress: 25/200 (skipped 4)` |

## Giới hạn còn lại (nói thật)

- **P1:** warm-up vẫn **~21 s/từ** ⇒ mỗi đêm chỉ warm `limit` (200) từ **phổ biến nhất**, **không** cả 2 809.
  NGSL là **CC BY-SA 4.0 (ShareAlike)** — file dữ liệu **phải** giữ attribution; là điều kiện license.
- **P2:** gỡ biến ⇒ **không còn** đường cấu hình sandbox; nếu sau cần Test mode phải thêm lại **có chủ đích**.
- **P3:** Funnel phụ thuộc `TS_AUTH_ONCE` + state đã lưu ở `./.tailscale`; nếu state mất thì Funnel ngừng.
