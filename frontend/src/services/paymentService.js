import api from './api'

/**
 * Service thanh toán Premium (SePay).
 * Cả 2 hàm trả về Map phẳng từ `PaymentController` — KHÔNG bọc `.data`:
 * đọc trực tiếp `data.isPremium`, `data.orderCode`, `data.qrUrl`...
 *
 * LƯU Ý: `createOrder` ghi một dòng THẬT vào bảng `payment_transactions`
 * (trạng thái PENDING) — mọi harness/UI chạm vào đây phải tự dọn dẹp.
 */
export default {
  /**
   * Kiểm tra trạng thái premium hiện tại của người dùng (cần JWT).
   * `GET /api/v1/payment/status` → `{ isPremium, premiumExpiry, ... }`.
   * Dùng để poll sau khi người dùng quét QR chuyển khoản.
   */
  getStatus() {
    return api.get('/api/v1/payment/status').then(r => r.data)
  },
  /**
   * Tạo đơn hàng premium và lấy thông tin dựng QR.
   * `POST /api/v1/payment/create-order` body `{ planType }` ('MONTH' | 'YEAR')
   * → `{ orderCode, qrUrl, amount }`. Gọi ngay khi mount trang checkout.
   */
  createOrder(planType) {
    return api.post('/api/v1/payment/create-order', { planType }).then(r => r.data)
  }
}
