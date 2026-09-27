# audit-v19-full — W4: SePay chữ ký THẬT (chuyển khoản thật) — **PASS** ✅

**Ngày:** 2026-09-27 · **Quyết định người dùng:** đi đường **chuyển khoản thật** (như v17). **Người dùng đã chuyển.**

## KẾT QUẢ: PASS — chữ ký SePay THẬT đã được verify

| | |
|---|---|
| `orderCode` | **`ENG73E2D3AA2DF6`** |
| DB row | **`SUCCESS \| 85111759 \| MBBank \| 10000`** |
| `transaction_id` | **`85111759`** — do **SePay sinh** (không phải synthetic) |
| `gateway` | **`MBBank`** — ngân hàng thật |
| Premium | `user@gmail.com`: `2026-10-03` → **`2026-10-27`** (+1 tháng) |
| Verify script | `W4 RESULT: PASS` |

## Bằng chứng đường đi = WEBHOOK (không phải polling)

Điểm mấu chốt: settle qua **polling** KHÔNG chứng minh chữ ký. Chứng minh đây là **webhook**:

1. **Dấu hiệu polling VẮNG MẶT:** `pollAndSettle` log `"SePay API polling detected payment for order"`
   (`PaymentService.java`) — grep 40' log = **0 lần** → không phải polling.
2. **Thread = HTTP:** log `Premium activated` ở `[nio-8080-exec-6]` (Tomcat HTTP thread), không phải scheduler.
3. **Chỉ 2 đường settle:** `processSePayTransaction` gọi từ `processWebhook` (`:108`, đòi HMAC) và `pollAndSettle`
   (`:293`). Polling bị loại → **webhook**.
4. Webhook `POST /api/webhook/sepay` bắt buộc **HMAC sha256 trên `"<ts>.<rawBody>"`** bằng `SEPAY_WEBHOOK_SECRET`
   + replay window ±5'. Chỉ SePay (giữ secret) ký được → **chữ ký THẬT**.

**Log:** `2026-09-27T22:06:28.203+07:00 INFO 1 --- [nio-8080-exec-6] c.datn.engflow.service.PaymentService : Premium activated for user 2: MONTH until 2026-10-27`

## Xử lý row thật (ràng buộc V9)

- **KHÔNG xoá row** (tiền thật) — khác probe g6 (probe tự xoá). Row `id=100411` **giữ lại**.
- Parity: `payments` 12→13, `PENDING` vẫn 0 — **giao dịch thật hợp lệ**, KHÔNG phải residue.
- `user@gmail.com` gia hạn premium +1 tháng (2026-10-27).
- Order cũ `ENG2143E44D4DEC` vẫn PENDING (bỏ dở, không phải tiền thật) — dọn riêng sau.

## Bối cảnh (đính chính v18)

v18 báo *"Webhook SePay chữ ký THẬT — BLOCKED (biên real-money)"*. Khảo sát phát hiện khung này **không chính xác**:
SePay có **Test-mode simulator MIỄN PHÍ**. Người dùng chọn **chuyển khoản thật** → đã chuyển → **PASS**.

## Fallback (ghi tài liệu)

Nếu cần lặp lại không tốn tiền: SePay Test-mode simulator (miễn phí, 0 code, đổi `SEPAY_WEBHOOK_SECRET`).
