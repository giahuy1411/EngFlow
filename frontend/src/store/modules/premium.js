/**
 * Store premium (Pinia setup-style).
 *
 * Trạng thái premium lấy từ `paymentService.getStatus()` (backend quyết định bằng
 * `hasPremiumAccess()`). Đây chỉ là bản cache cho UI — hàng rào thật nằm ở route meta
 * `requiresPremium` phía frontend và ở kiểm tra `userService.hasPremiumAccess()` phía
 * backend; đừng tin mỗi `isPremium` ở client.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import paymentService from '@/services/paymentService'

export const usePremiumStore = defineStore('premium', () => {
  const isPremium = ref(false)
  const premiumExpiry = ref(null)
  const premiumStatus = ref(null)
  const loading = ref(false)

  /**
   * Đồng bộ trạng thái premium từ server. Gọi lúc khởi động app và sau khi thanh toán.
   * Không ném lỗi ra ngoài — xem chú thích trong catch.
   * @returns {Promise<void>}
   */
  async function checkStatus() {
    try {
      const data = await paymentService.getStatus()
      isPremium.value = data.isPremium
      premiumExpiry.value = data.premiumExpiry
      premiumStatus.value = data
    } catch (e) {
      // audit-v5 fix: a network blip must NOT flip a paying user to the
      // free-tier UI. Keep the last known state; only an authoritative
      // response (above) changes it.
      // Chỉ phản hồi hợp lệ từ server mới được đổi trạng thái; lỗi mạng thì giữ
      // nguyên trạng thái đã biết để người đang trả phí không bị đẩy về UI miễn phí.
      console.warn('payment/status failed, keeping last premium state', e)
    }
  }

  /**
   * Tạo đơn thanh toán cho một gói; bật/tắt cờ `loading` để UI khoá nút.
   * @param {string} planType - loại gói (backend định nghĩa)
   * @returns {Promise<object>} dữ liệu đơn hàng từ paymentService
   */
  async function createOrder(planType) {
    loading.value = true
    try {
      return await paymentService.createOrder(planType)
    } finally {
      loading.value = false
    }
  }

  return { isPremium, premiumExpiry, premiumStatus, loading, checkStatus, createOrder }
})
