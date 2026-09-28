/**
 * useToast — hàng đợi toast dùng chung cho toàn app.
 *
 * `toasts` là mảng `reactive` khai báo ở phạm vi module, nên mọi component gọi
 * `useToast()` đều nhìn thấy CÙNG một danh sách: toast bắn từ màn hình này sẽ hiện
 * trên component `<ToastContainer>` đang mount ở App.vue. Không dùng Pinia ở đây vì
 * toast không cần persist hay devtools.
 */
import { reactive } from 'vue'

const toasts = reactive([])
// Bộ đếm id tăng dần — đủ để phân biệt các toast trong một phiên.
let counter = 0

/**
 * Truy cập API toast. Trả về chính hàng đợi `toasts` cùng các helper bắn/xoá.
 * @returns {object} gồm toasts, add, remove, success, error, info, toastBackground
 *   (showError/showSuccess là alias của error/success)
 */
export function useToast() {
  /**
   * Bắn một toast mới và tự hẹn giờ xoá.
   * @param {string} message - nội dung hiển thị
   * @param {string} type - 'info' | 'success' | 'error' (mặc định 'info')
   * @param {number} duration - thời gian hiện, tính bằng ms; bị kẹp tối thiểu 1000ms
   *   để toast không biến mất trước khi người dùng kịp đọc
   * @returns {number} id của toast vừa tạo (dùng để remove thủ công nếu cần)
   */
  function add(message, type = 'info', duration = 4000) {
    const id = ++counter
    toasts.push({ id, message, type })
    const safeDuration = Math.max(1000, duration)
    setTimeout(() => remove(id), safeDuration)
    return id
  }

  /**
   * Xoá một toast khỏi hàng đợi theo id (no-op nếu toast đã biến mất).
   * @param {number} id
   */
  function remove(id) {
    const idx = toasts.findIndex(t => t.id === id)
    if (idx !== -1) toasts.splice(idx, 1)
  }

  /**
   * Toast thành công (biến thể màu xanh ở toastBackground).
   * @param {string} message
   * @param {number} duration
   * @returns {number} id toast
   */
  function success(message, duration = 4000) {
    return add(message, 'success', duration)
  }

  /**
   * Toast lỗi.
   * @param {string} message
   * @param {number} duration
   * @returns {number} id toast
   */
  function error(message, duration = 4000) {
    return add(message, 'error', duration)
  }

  /**
   * Toast thông tin trung tính.
   * @param {string} message
   * @param {number} duration
   * @returns {number} id toast
   */
  function info(message, duration = 4000) {
    return add(message, 'info', duration)
  }

  /**
   * Chọn class Tailwind nền/chữ cho từng loại toast.
   *
   * audit-v17 F-17-01: 'bg-accent text-white' đo được 4.23:1 — dưới ngưỡng 4.5:1 của
   * WCAG 1.4.3 cho body text, lại còn đi vòng qua lớp ink/strong của audit-v11 F132 mà
   * mọi call site khác đều dùng. --geo-accent-strong (#7C3AED) đạt 5.70:1 với chữ
   * trắng và vẫn giữ đúng sắc tím thương hiệu.
   *
   * @param {string} type - 'success' | 'error' | 'info'; giá trị khác rơi vào default
   * @returns {string} chuỗi class Tailwind
   */
  function toastBackground(type) {
    switch (type) {
      case 'success': return 'bg-accent-strong text-white'
      case 'error': return 'bg-danger text-danger-fg'
      case 'info': return 'bg-card text-foreground'
      default: return 'bg-card text-foreground'
    }
  }

  return { toasts, add, remove, success, error, info, toastBackground, showError: error, showSuccess: success }
}



