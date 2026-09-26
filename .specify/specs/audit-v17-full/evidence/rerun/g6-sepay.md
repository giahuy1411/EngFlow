# audit-v17-full (rerun) — G6: SePay webhook với chữ ký HMAC THẬT

**Trạng thái v17:** BLOCKED ("biên real-money"). **Rerun:** **VERIFIED** — hoá ra **test được**, và đã test.

## Vì sao v17 tưởng blocked

Sợ rằng ký webhook hợp lệ = giao dịch tiền thật. **Không phải**: ta **tự tạo order PENDING**, **tự ký** bằng secret
**local** trong `.env`, rồi **tự xoá row**. Không có tiền thật, không có production.

## Contract (đọc từ source)

`PaymentService.isSignatureValid` (`:397-433`):
- Header `X-Sepay-Signature: sha256=<hex>` + `X-Sepay-Timestamp: <epoch-giây>`.
- HMAC-SHA256(secret, `"<ts>.<rawBody>"`); timestamp trong `REPLAY_WINDOW_MS`.
- Body cần `id` (SePay gửi dạng số; `PaymentService:93` đọc `body.get("id")`), `content` (chứa `ENG…`),
  `transferAmount`/`amount_in`/`amount`, `gateway`.
- Secret: `sepay.webhook.secret` ← `SEPAY_WEBHOOK_SECRET` (có trong `.env`).

## Probe: `sweep/harness/g6-sepay-signed.py` (tự dọn)

```
secret loaded: length=38 (value not printed)
login: 200
create-order: 200 {"amount":10000,"orderCode":"ENG5847DA2962FE", ...}
orderCode: ENG5847DA2962FE
webhook(valid sig): 200 {"success":true}          ← chữ ký ĐÚNG được chấp nhận
db status: SUCCESS                                 ← row settle thật
webhook(bad sig):  200 {"error":"Invalid signature","success":false}   ← negative control
cleanup: deleted=1 sqlError=False                  ← dọn theo orderCode
G6 RESULT: PASS
```

## Probe 2 (độc lập)

- **Negative control trong cùng probe:** chữ ký sai (`sha256=` + 64 số 0) → **`Invalid signature`** — chứng minh
  đường verify không "chấp nhận tất".
- **Parity sau dọn:** `PENDING_PAYMENTS=0` (không còn row), `EXERCISE_ATTEMPTS=33`, `STUDY_DAYS=4`.

## Ghi chú trung thực

- Đây là test **end-to-end trên môi trường LOCAL với secret LOCAL** — **không phải** giao dịch tiền thật và
  **không phải** bằng chứng production. Secret **không** được in ra log (chỉ độ dài).
- Script **tự dọn** (xoá đúng row theo `orderCode`) và **tự assert** parity ⇒ có thể chạy lại nhiều lần.

---

## Vòng review chéo 2 — defect trong chính probe này (đã sửa)

- **F-17-22 (premium không hoàn nguyên):** settle gọi `PaymentService.processSePayTransaction:193-195` →
  `setIsPremium(true)` + `setPremiumExpiry(...)`. Probe ban đầu chỉ xoá row ⇒ **mỗi lần chạy đẩy expiry thêm 1 tháng**
  (đã đo: user 2 `2026-10-03` → `2026-10-26`), **parity không thấy**. → **Sửa:** đọc premium **trước**, hoàn nguyên
  trong `finally`; đã trả user 2 về `2026-10-03`. Chạy lại: `premium restored=1`.
- **F-17-23 (negative control chỉ in):** chữ ký sai chỉ được **in**, không assert ⇒ server chấp nhận chữ ký sai vẫn PASS.
  → **Sửa:** assert `Invalid signature`/`success:false` và đưa vào `ok`. Chạy lại: `rejected: True`.
- **F-17-25 (thiếu try/finally):** → **Sửa:** bọc `finally`, dọn cả khi bước giữa lỗi.

**Kết quả sau fix:** `premium before/after` giống nhau, `bad sig rejected: True`, `deleted=1`, `restored=1`,
`G6 RESULT: PASS`.
