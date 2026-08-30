# EngFlow — Báo cáo triển khai hậu-audit (2026-08-29)

Mục tiêu: triển khai toàn bộ kế hoạch hậu-audit (TRỪ Flyway), verify bằng test/build/browser, commit theo nhóm logic và push lên `main`.

## Tóm tắt kết quả

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Phase A — Backup + restart stack | ✅ | branch `audit-2026-08-28-backup` (commit `7b139d6`); Redis `allkeys-lru` verify qua `CONFIG GET`; ngrok tunnel `sepay-webhook` hoạt động |
| Phase B — Migration AppButton | ✅ | ~110 nút thô → AppButton trên 32 views; build `✓ built`; Playwright verify Lessons filter (1463→498) |
| Phase C — ease-bounce + icon stroke 2.5 | ✅ | `.lucide { stroke-width: 2.5 }` trong design-system.css; AppButton mặc định ease-bounce |
| Phase D — Dọn legacy + tap target | ✅ | Xóa `bauhaus/` (StreakCalendar→common/, 5 file dead) + `geo/` (dead); `app-btn--sm` 36→44px; main.css bỏ dead helpers |
| Phase E — CSP + regression tests | ✅ | `script-src` bỏ `'unsafe-inline'` (verify header live); 56/56 frontend tests; 128/128 backend tests |
| Phase F — Verify + commit + push | ⏳ | đang chạy |

## Phát hiện then chốt trong phiên

1. **Nhánh phân kỳ**: `main` là ancestor của `feat/exercise-system` → push main là fast-forward an toàn.
2. **Working tree tích tụ**: 1415 thay đổi chưa commit từ nhiều phiên trước (design-system `components/ui/`, backend tests, MinIO deps). Đã backup sang `audit-2026-08-28-backup` rồi khôi phục nền tảng sang `feat/exercise-system`.
3. **`components/ui/` chưa từng được import** trên feat branch — toàn bộ app dùng `<button>` thô. Đã khôi phục bộ primitives + migration.
4. **Test infra thiếu**: 11 file `.test.js` tồn tại nhưng `package.json` không có vitest/@vue/test-utils/jsdom → đã thêm, chạy được 56 tests.
5. **Bootstrap dep khai tử**: `bootstrap` vẫn trong package.json nhưng 0 tham chiếu → đã xóa.
6. **CSP cho SPA**: backend chỉ serve JSON (`/api/**`), không serve HTML → `'unsafe-inline'` trong `script-src` là thừa (API response không có script; JSON-LD `type="application/ld+json"` không phải executable). Đã bỏ. `style-src 'unsafe-inline'` giữ lại (Vite/Tailwind inject inline style). Nonce-based CSP đầy đủ cần thực hiện ở tầng hosting HTML (nginx/CDN prod), không phải ở API header.

## Quyết định kiến trúc

- **Hướng design system**: chọn `components/ui/` (AppButton/StickerCard/AppInput/FormField) theo `design-system.md`, thay vì `components/geo/` (GeoButton) — vì `design-system.md` mô tả đúng bộ `ui/` và nó có sẵn tests.
- **Nút giữ nguyên `<button>` thô** (có chủ đích, kèm comment): answer-option buttons trong game (QuizGame, MemoryMatch, ListeningGame, MixedGame) vì cần dynamic per-state coloring (correct=green/wrong=pink) mà variants không cover; icon-only circular controls (audio play, record toggle); ARIA tab buttons (LessonLayout).

## Files thay đổi chính

- `frontend/src/components/ui/*` — khôi phục 18 primitives + tests
- `frontend/src/assets/design-system.css` — `.lucide` stroke, tap target 44px
- `frontend/src/views/**` — migration AppButton (32 views)
- `frontend/src/components/bauhaus/`, `geo/` — xóa (dead)
- `frontend/package.json`, `vite.config.js` — vitest infra, bỏ bootstrap
- `frontend/src/views/luyentu/QuizGame.test.js` — 4 regression tests
- `src/test/java/.../ExerciseServiceAdminAnswerRegressionTest.java` — 4 regression tests
- `src/main/java/.../config/SecurityConfig.java` — CSP hardening
- `ngrok.yml` — tạo lại file config tunnel (trước đó là thư mục rỗng gây lỗi mount)

## Cách verify

- Backend: `mvn test` → 128 pass / 0 fail / 0 error (24 classes)
- Frontend: `npx vitest run` → 56 pass / 0 fail (12 files); `npx vite build` → ✓ built
- Browser (Playwright/Brave): Lessons filter tương tác đúng, Decks render StickerCard, header/footer intact
- CSP: `curl -I` xác nhận header live không còn `unsafe-inline` ở script-src

## Việc còn lại / khuyến nghị

- Kích hoạt Flyway (đã loại trừ theo yêu cầu) — còn `ddl-auto=update`.
- Nonce-based CSP đầy đủ ở tầng hosting prod (nginx/CDN).
- Cân nhắc gộp `feat/exercise-system` → `main` qua PR thay vì push trực tiếp (đang push trực tiếp theo yêu cầu).

## Bug premium: "Chưa ghi nhận giao dịch" — nguyên nhân gốc & fix (2026-08-29)

**Triệu chứng:** Test thủ công gói MONTH → QR hiển thị → tiền đã trừ trong ngân hàng → trang nâng cấp vẫn báo "Chưa ghi nhận giao dịch."

**Chuỗi nguyên nhân (đã verify bằng evidence, không đoán):**

1. **Webhook SePay production dùng scheme chữ ký KHÁC với code.** SePay gửi `X-Sepay-Signature: sha256=<hex>` + `X-Sepay-Timestamp`, trong đó hex = `HMAC-SHA256(secret, "<timestamp>.<rawBody>")`. Code cũ chỉ kiểm tra `X-Signature` (header) hoặc field `signature` (body), tính HMAC trên body đã strip signature → **luôn fail** với payload thật. Bằng chứng: replay đúng payload SePay capture từ ngrok → trả `Invalid signature`; sau khi tính `HMAC(secret, ts + "." + body)` thì **match byte-for-byte** (`e588da7b…83a2`).
2. **Tunnel ngrok chết âm thầm.** `ngrok.yml` (v3) khai `authtoken` trong file config → ngrok báo `field authtoken not found in type config.v3yamlConfig` và crash-loop; container `Up` nhưng không có tunnel. Auth token phải truyền qua env `NGROK_AUTHTOKEN` (compose đã set) — file config không được chứa key đó. Đã sửa `ngrok.yml` bỏ `authtoken:`; tunnel `sagging-dyslexic-showoff.ngrok-free.dev` hoạt động, webhook POST tới backend trả 200.
3. **Fallback polling bị vô hiệu.** `SEPAY_API_TOKEN` rỗng (length=0) → `SePayApiService` log "token not configured" mỗi 5s (đúng nhịp frontend poll) và không thể tự phát hiện giao dịch. Đây là lý do UI kẹt ở PENDING vĩnh viễn khi webhook fail.

**Fix đã implement (commit trong HEAD):**
- `PaymentController.sepayWebhook`: nhận thêm `X-Sepay-Signature` + `X-Sepay-Timestamp`.
- `PaymentService.isSignatureValid`: chấp nhận CẢ HAI scheme — production (`sha256=` prefix, HMAC trên `"<ts>.<body>"`) và legacy (`X-Signature`/body field, HMAC trên body stripped). So sánh bằng `MessageDigest.isEqual` (constant-time, chống timing attack).
- `PaymentServiceTest`: +3 test regression (production valid / production sai chữ ký bị reject / không có prefix `sha256=`).
- `ngrok.yml`: bỏ `authtoken:` khỏi config v3.

**Verify end-to-end:** Replay webhook thật qua backend → `{"success":true}`; DB: order `ENG_0136FCCFC2B9` → `SUCCESS`, log `Premium activated for user 120010: MONTH until 2026-09-28`. Đã revert side-effect test (user về `is_premium=0`, order về `PENDING`) để không污染 dữ liệu. Full suite: **128 tests pass / 0 fail / 0 error**.

**Còn tồn (không phải bug code):**
- Giao dịch thật của user (`MOMO-CASHOUT-…-OQCOiruCxwac-…`) có `content` KHÔNG chứa order code `ENG_…` → regex `ENG_[A-Z0-9]{12}` không match → trả `Invalid content format`. Nghĩa là khi chuyển khoản, nội dung CK không được điền đúng mã đơn (user chuyển qua MOMO cash-out với content do MOMO sinh). Cần hướng dẫn user nhập đúng `content = orderCode` khi chuyển, HOẶC bổ sung matching theo amount+time window. **Cần quyết định product — chưa tự sửa.**
- `SEPAY_API_TOKEN` vẫn rỗng → fallback polling không hoạt động. Admin cần lấy token tại app.sepay.vn → Cài đặt → API rồi set `.env` + rebuild.
- Webhook URL đăng ký trên SePay phải trỏ đúng `<ngrok-url>/api/webhook/sepay`; URL ngrok free đổi mỗi lần restart → cần fixed domain hoặc cập nhật lại mỗi phiên dev.

## Nâng cấp QR tự điền nội dung CK (2026-08-29, source-driven + doubt-driven)

**Yêu cầu:** QR phải tự điền nội dung chuyển khoản khi quét để user không gõ tay.

**Điều tra (docs chính thức + giải mã payload QR thật):**
- `https://docs.sepay.vn/tao-qr-code-vietqr-dong.html`: tham số nội dung CK là **`des`** — code cũ dùng `content=` → **bị SePay bỏ qua hoàn toàn** (QR không có EMVCo field 62 → app ngân hàng không tự điền nội dung). Đây là nguyên nhân sâu xa khiến user chuyển thiếu/sai nội dung.
- Kiểm chứng thực nghiệm: giải mã QR sinh ra bởi `qr.sepay.vn/img` — payload `content=...` → **không có field 62**; `des=ENG_ABCDEF123456` → field `62/08 = ENGABCDEF123456` (**generator strip dấu `_`**); `des=ENGABCDEF123456` → giữ nguyên.
- `https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc`: xác nhận scheme HMAC production = `HMAC(secret, "{timestamp}.{raw_body}")`, header `X-SePay-Signature: sha256={hex}` — **khớp chính xác** với fix `isSignatureValid` đã deploy. Docs cũng khuyến nghị kiểm timestamp ±5 phút chống replay (xem Open questions).
- `https://docs.sepay.vn/api-giao-dich.html`: User API endpoint thật là `https://my.sepay.vn/userapi/transactions/list` (lọc `amount_in`, `reference_number`...) — **khác** endpoint `userapi.sepay.vn/v2/transactions` đang dùng trong `SePayApiService` (nghi vấn sai, cần verify khi set token).
- `https://docs.sepay.vn/api-va-theo-don-hang.html`: giải pháp "chính xác tuyệt đối" là **VA theo đơn hàng** (mỗi đơn một số TK ảo riêng, không phụ thuộc nội dung CK) — yêu cầu gói doanh nghiệp, vượt scope hiện tại.

**Thay đổi code:**
- `PaymentService.createOrder`: `content=` → `des=`; orderCode bỏ `_` (`ENG` + 12 hex hoa) vì generator strip `_` khỏi `des`.
- `PaymentService.processSePayTransaction`: regex `ENG_[A-Z0-9]{12}` → `ENG_?[A-Z0-9]{12}` + **legacy fallback**: nếu không tìm thấy PENDING theo code không `_`, thử lại với `ENG_...` — 71 đơn PENDING cũ (định dạng `ENG_`) vẫn fulfill được.
- `PaymentServiceTest`: +3 test (QR content không `_` match format mới / fallback legacy `ENG_` / content lowercase). **131 tests pass / 0 fail / 0 error.**

**Doubt-driven review:** fresh-context reviewer phát hiện **đúng bug chí tử** — bản `des=` đầu tiên vẫn sinh `ENG_...` trong khi regex webhook yêu cầu `_` bắt buộc → mọi đơn QR mới sẽ không bao giờ match. Đã sửa theo hướng reviewer đề xuất (bỏ `_` khỏi orderCode + regex optional + fallback). Reviewer cũng ghi nhận: round-trip test chưa có trong unit suite → đã bù bằng **E2E thủ công**: tạo đơn qua API thật → tải QR thật → giải mã field 62/08 → simulate webhook với content đúng như app ngân hàng sẽ gửi → `success=true`, `isPremium=true, expiry=2026-09-28`. Đã revert side-effect test.

**Hệ quả phụ cần biết:**
- Order code format mới `ENGXXXXXXXXXXXX` (15 ký tự, không `_`); format cũ `ENG_XXXXXXXXXXXX` vẫn resolve được qua fallback.
- `qrContent` (biến dead code cũ) đã xóa.
- **Cảnh báo còn nguyên**: QR tự điền không bắt buộc user chuyển đúng số tiền, và user vẫn có thể gõ tay nội dung khác → webhook vẫn có thể nhận content không match; lớp polling theo `q=orderCode` (cần `SEPAY_API_TOKEN`) vẫn là bảo hiểm chính.

## Củng cố luồng thanh toán (2026-08-29, đợt 2): User API endpoint + anti-replay + Test Mode

### 1. `SePayApiService` — đổi sang endpoint User API theo docs

- **Vấn đề:** code gọi `https://userapi.sepay.vn/v2/transactions?q=...&amount_in_min=...` — endpoint **không có trong docs**; docs chính thức (`docs.sepay.vn/api-giao-dich.html`) ghi `GET https://my.sepay.vn/userapi/transactions/list` với bộ lọc `amount_in` (khớp chính xác tiền vào), response `{ transactions: [ { id, transaction_content, amount_in, bank_brand_name, ... } ] }`.
- **Thay đổi (`SePayApiService.java`):**
  - URL → `https://my.sepay.vn/userapi/transactions/list?amount_in={amount}&limit=20`.
  - Parser đọc `transactions` (fallback legacy `data`), field `transaction_content` (fallback `content`), và **normalize** row về shape processor expect (`id`, `content`, `transferAmount`, `gateway`) để `PaymentService.checkPendingPayments` không phải biết shape API.
  - Bắt `HttpClientErrorException.TooManyRequests` (docs: rate limit 3 req/s, 429 kèm header `x-sepay-userapi-retry-after`) → log + trả empty, poll sau tự retry (frontend gọi status mỗi 5s).
  - Sửa hướng dẫn lấy token trong log warn: **my.sepay.vn → Cấu hình Công ty → API Access → + Thêm API** (không phải app.sepay.vn như cũ); đồng bộ `.env.example`.
- **Tests:** file mới `SePayApiServiceTest.java` — 10 case (shape docs, case-insensitive content, legacy shape, list rỗng, key lạ, không match content, HTTP error, 429, token rỗng skip HTTP, isTokenConfigured). **10/10 pass.**
- **Lưu ý:** chưa verify được với API thật vì `SEPAY_API_TOKEN` rỗng — parser defensive nên nếu response thật khác docs thì log debug hiện danh sách, không crash. Verify thật ở mục 4.

### 2. Anti-replay webhook theo docs

- **Vấn đề:** docs (`developer.sepay.vn/vi/sepay-webhooks/xac-thuc`) khuyến nghị `abs(time() - timestamp) > 300` → reject; code chỉ verify HMAC, không kiểm timestamp → payload bắt được có thể replay vô hạn (idempotency theo `transaction_id` chặn được phần lớn, nhưng không đủ vì id chỉ xuất hiện sau khi xử lý lần đầu thành công).
- **Thay đổi (`PaymentService.isSignatureValid`):** production scheme (`X-Sepay-Timestamp` + `X-Sepay-Signature`) giờ reject nếu skew > 5 phút (2 phía: quá khứ lẫn tương lai) hoặc timestamp không parse được; log `Webhook replay rejected: timestamp skew ...`. **Legacy/simulated scheme không kiểm timestamp** (giữ backward-compat cho test/simulation — có comment rõ lý do).
- **Tests:** +4 case (`staleTimestamp_rejectsReplay`, `futureTimestamp_rejectsReplay`, `nonNumericTimestamp_rejects`, `timestampJustInsideWindow_processesTransaction`); sửa 3 test production cũ dùng timestamp cố định 2026 → `Instant.now()` (chúng sẽ rơi vào ngoài window). **PaymentServiceTest: 33/33 pass.**

### 3. Quy trình test premium KHÔNG tốn tiền thật (docs: docs.sepay.vn/gia-lap-giao-dich.html)

1. Đăng nhập **my.sepay.vn** → bật công tắc **Test Mode** (góc trên phải).
2. **Cấu hình lại trong Test Mode**: webhook URL + secret, API token — môi trường Test **tách biệt hoàn toàn** với Live (webhook/token Live không dùng được ở Test).
3. Menu **Giao dịch** → **`+ Mô phỏng giao dịch`** → điền số tiền (10000) + nội dung = orderCode (`ENGXXXXXXXXXXXX`) → SePay bắn webhook payload thật → premium kích hoạt.
4. Verify UI premium + DB (`payment_transactions.status='SUCCESS'`), sau đó tắt Test Mode. Giao dịch giả không vào báo cáo Live.
- Webhook log còn có nút **"Gửi thử"** để test đường webhook không cần giao dịch.

### 4. Việc còn lại (chờ `SEPAY_API_TOKEN`) ✅ DONE

- Lấy token: my.sepay.vn → Cấu hình Công ty → API Access → + Thêm API → set `SEPAY_API_TOKEN` trong `.env` → `docker compose up -d --build backend` (cũng thêm biến này vào `docker-compose.yml`).
- Bật Test Mode → tạo giao dịch giả với orderCode thật → gọi `GET /api/v1/payment/status` (không cần webhook) → premium phải kích hoạt qua polling → xác nhận endpoint + response shape thật khớp docs.

### 5. Verify E2E thật với token + giao dịch thật (2026-08-30)

- **Token thật đã set**, container ghi `SePay API token configured. Webhook fallback polling is ENABLED` ở startup.
- **Endpoint xác nhận khớp docs**: `https://my.sepay.vn/userapi/transactions/list?amount_in=10000&limit=20` trả 200 + `transactions[]` đúng shape (`bank_brand_name`, `transaction_content`, `amount_in`, `id`, ...).
- **Phát hiện thêm trong khi verify (do dùng data thật)**:
  1. Matcher `contains(orderCode)` cũ không khớp khi content ngân hàng strip `_` (giống QR), ví dụ content `…-ENG0AC7BB0C1694-…` không match order `ENG_0AC7BB0C1694`. Đã sửa `SePayApiService` dùng regex `ENG_?[A-Z0-9]{12}` và **normalize cả hai phía** về dạng không `_` (12 hex) trước khi compare. +2 test (match legacy format, reject format khác).
  2. `findTop5ByUserIdAndStatusOrderByIdDesc` cắt mất order đã chuyển tiền cũ nằm ngoài top 5 mới nhất (user 120010 có 16 PENDING, order thật `ENG_0AC7BB0C1694` đứng thứ 6). Nâng lên **Top10** (1 dòng repo + 1 dòng service + update tests).
- **Kết quả thực** (user 120010 / giahuy5461@gmail.com poll `/api/v1/payment/status`):
  ```
  poll took 3717ms
  {"pollingEnabled": true, "premiumExpiry": "2026-09-30", "isPremium": true}
  ```
  Backend log: `SePay API found matching transaction for order code: ENG_0AC7BB0C1694` → `polling detected payment` → `Premium activated for user 120010: MONTH until 2026-09-30`.
  → **Đóng chuỗi bug gốc**: ca báo lỗi đầu tiên (user chuyển 10k nhưng "Chưa ghi nhận") — tới 8 ngày sau mới premium tự động kích hoạt khi user mở app lại nhờ polling. Full suite 148 tests pass.

## Tổng kết phạm vi QR tự điền nội dung

| Thành phần | Trạng thái | Bằng chứng |
|---|---|---|
| Backend: `des=` đúng param | ✅ | giải mã QR thật → `62/08 = ENGXXXXXXXXXXXX` |
| Backend: orderCode không `_` (match QR) | ✅ | regex `ENG_?[A-Z0-9]{12}` + legacy fallback `ENG_…` |
| Backend: webhook HMAC chuẩn prod | ✅ | replay window ±5 phút, 3 scheme hỗ trợ |
| Backend: User API polling fallback | ✅ | full E2E recovery user 120010 thật |
| Frontend: hiển thị QR + content + amount | ✅ | `PremiumCheckout.vue` đã có sẵn — user quét = app tự điền |
| Token SePay + docker-compose | ✅ | `.env` + `docker-compose.yml` line 68 |
| Test suite | ✅ | 148 / 0 / 0 |

**Không còn cần nhập tay nội dung CK khi quét QR.** Phòng hờ user tự sửa nội dung, polling fallback + legacy `ENG_` fallback vẫn map được.
