import api from './api'

/**
 * Service xác thực người dùng (đăng nhập/đăng ký/hồ sơ/avatar).
 *
 * LƯU Ý VỀ HÌNH DẠNG RESPONSE (gotcha quan trọng):
 * - `login`/`register`/`getMe`/`updateAvatar`/`uploadAvatarFile` trả về
 *   UserResponse **PHẲNG** — các field như `token`, `isAdmin`, `isPremium` nằm
 *   ngay ở tầng trên cùng của body, KHÔNG bọc trong `.data`. Vì vậy store
 *   `store/modules/auth.js` đọc trực tiếp `data.token`, `mapUser(data)`.
 *   (Một số API khác của backend lại trả `{ data: {...} }` — đọc service tương
 *   ứng trước khi dùng, đừng mặc định.)
 * - `requestPasswordReset`/`resetPassword` trả `{ message: "..." }`.
 * - Mọi hàm ở đây đã `.then(r => r.data)`: trả về BODY của response, không phải
 *   đối tượng axios (không còn `.status`/`.headers`).
 */
export default {
  /** Đăng nhập bằng `{ email, password }`. Trả UserResponse phẳng (có `token`). */
  login: (credentials) => api.post('/api/auth/login', credentials).then(r => r.data),
  /**
   * Đăng ký tài khoản mới. Trả UserResponse; `token` CHỈ có khi backend tự đăng
   * nhập ngay sau đăng ký — store phải kiểm tra `if (data.token)` trước khi lưu.
   */
  register: (userData) => api.post('/api/auth/register', userData).then(r => r.data),
  /** Lấy hồ sơ người đang đăng nhập (cần JWT). Dùng để đồng bộ lại `user` khi boot. */
  getMe: () => api.get('/api/auth/me').then(r => r.data),
  /** Gán avatar bằng URL có sẵn (không upload file). Trả UserResponse đã cập nhật. */
  updateAvatar: (avatarUrl) => api.put('/api/auth/avatar', { avatarUrl }).then(r => r.data),
  /**
   * Gửi yêu cầu quên mật khẩu (OTP qua email).
   * Backend hiện chưa có endpoint này → 404; store `forgotPassword` fail-soft coi
   * 404 là thành công để không lộ email có tồn tại hay không.
   */
  requestPasswordReset: (email) => api.post('/api/auth/forgot-password', { email }).then(r => r.data),
  /** Đặt lại mật khẩu bằng OTP đã nhận qua email. */
  resetPassword: (email, otp, newPassword) => api.post('/api/auth/reset-password', { email, otp, newPassword }).then(r => r.data),
  /**
   * Upload file ảnh avatar lên Cloudinary (multipart/form-data), backend gắn URL
   * trả về vào hồ sơ. Trả UserResponse đã cập nhật `avatarUrl`.
   * Lỗi: 400 file không hợp lệ (rỗng/không phải ảnh), 500 lỗi hạ tầng Cloudinary.
   */
  uploadAvatarFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/auth/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  }
}
