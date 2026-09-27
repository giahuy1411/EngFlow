# audit-v19-full — W4: SePay chữ ký THẬT (chuyển khoản thật) — CHỜ NGƯỜI DÙNG CHUYỂN TIỀN

**Ngày:** 2026-09-27 · **Quyết định người dùng:** đi đường **chuyển khoản thật** (như v17).

## Đính chính khung "real-money boundary" của v18

v18 báo *"Webhook SePay chữ ký THẬT — BLOCKED (biên real-money)"*. Khảo sát phát hiện khung này **không chính xác**:
SePay có **Test-mode transaction simulator MIỄN PHÍ** (gửi webhook Live-shaped + ký thật tới URL công khai).
**Nhưng** người dùng chọn **chuyển khoản thật**, nên vẫn cần tiền thật — và điều đó là **lựa chọn của người dùng**,
không phải "bất khả thi kỹ thuật".

## Đã làm (phần tự động)

1. **Funnel reachable:** `POST https://engflow-dev.tail7fd1fe.ts.net/api/webhook/sepay` → **200** (đo phiên này).
2. **Config đủ:** `SEPAY_WEBHOOK_SECRET` (38), `SEPAY_BANK_ACCOUNT` (10), `SEPAY_BANK_NAME` (6), `SEPAY_API_TOKEN` (64) — đều SET.
3. **g6 plumbing PASS** (secret local): valid HMAC→SUCCESS; bad sig→rejected; **stale/replay→rejected**; tự dọn + hoàn nguyên premium.
4. **Tạo order THẬT** (`POST /api/v1/payment/create-order`):

   | | |
   |---|---|
   | `orderCode` | **`ENGF4E8A2FBEA40`** |
   | `amount` | **10.000đ** |
   | `qrUrl` | `https://qr.sepay.vn/img?acc=0706718329&amount=10000&des=ENGF4E8A2FBEA40&bank=MBBank` |
   | DB status | **PENDING** (chưa chuyển) |

5. **Script verify:** `sweep/harness/w4-sepay-real-verify.py <orderCode>` — kiểm row `SUCCESS` + `transaction_id`
   thật + `gateway` không phải `AUDIT-*` (phân biệt với g6 local).

## CẦN NGƯỜI DÙNG LÀM (không thể tự động)

> Quét QR trên (hoặc chuyển tới **MBBank `0706718329`**, nội dung **`ENGF4E8A2FBEA40`**), số tiền **đúng 10.000đ**.
> Sau khi chuyển, SePay POST webhook (chữ ký **của SePay**) tới Funnel → app kích premium.

**Bằng chứng khi xong:** log backend `Premium activated`, row `SUCCESS` với `id`/`gateway` **thật do SePay sinh**
(chỉ SePay ký được bằng secret của họ) → **đây là chữ ký THẬT**.

## Xử lý row thật (ràng buộc V9)

- **KHÔNG xoá row** (tiền thật) — khác với probe g6 (probe tự xoá).
- Row này sẽ làm parity lệch (`payments 12→13`, `PENDING→SUCCESS`). Ghi nhận là **giao dịch thật hợp lệ**;
  cập nhật baseline parity nếu người dùng đồng ý, hoặc giữ và ghi chú.
- `user@gmail.com` sẽ được gia hạn premium +1 tháng (từ 2026-10-03).

## Fallback (nếu không chuyển được)

SePay **Test-mode simulator** (miễn phí): tạo Test-mode webhook (HMAC secret riêng) trỏ về Funnel, đổi
`SEPAY_WEBHOOK_SECRET`, dùng "Mô phỏng giao dịch" → webhook Live-shaped + ký thật, **0 đồng, 0 code**.
Ghi ở đây để nếu người dùng đổi ý thì biết đường.

## Trạng thái

**PENDING — chờ người dùng chuyển khoản.** Sau khi chuyển, chạy `w4-sepay-real-verify.py ENGF4E8A2FBEA40`.
