import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/store/modules/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

// Longer timeout for admin endpoints that return large datasets
api.interceptors.request.use(config => {
  if (config.url?.startsWith('/api/admin')) {
    config.timeout = 60000
  }
  return config
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  // F7-BUG02 FIX: Proactively check token expiry before sending request
  if (token) {
    try {
      const parts = token.split('.')[1]
      if (parts) {
        const payload = JSON.parse(atob(parts))
        const now = Math.floor(Date.now() / 1000)
        if (payload.exp && payload.exp < now) {
          useAuthStore().logout()
          router.push('/login')
          return Promise.reject(new Error('Token expired'))
        }
      }
    } catch (e) {
      useAuthStore().logout()
      router.push('/login')
      return Promise.reject(new Error('Invalid token'))
    }
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

api.interceptors.response.use(
  res => res,
  async err => {
    const data = err.response?.data
    // Backend trả lỗi theo RFC 7807: thông báo tiếng Việt nằm ở `detail`, còn
    // hầu hết component đọc `message`. Sao chép một lần tại đây để mọi call-site
    // hiển thị đúng thông báo mà không phải sửa từng nơi.
    if (data && typeof data === 'object' && data.detail && !data.message) {
      data.message = data.detail
    }
    // audit-v8 F94: 20 endpoint (AiVocab/Game/Deck/Srs/LessonStructure/
    // SpeakingPrompt controller) vẫn trả shape cũ `{"error": "..."}`. Không có
    // nhánh này thì call-site đọc `.detail`/`.message` hiện chuỗi generic
    // ("Sinh từ thất bại") thay vì thông báo thật của backend.
    if (data && typeof data === 'object' && typeof data.error === 'string' && data.error) {
      if (!data.detail) data.detail = data.error
      if (!data.message) data.message = data.error
    }
    // F7-BUG02 FIX: Handle both 401 (expired) and 403 (fallback) for auth failures
    if (err.response?.status === 401 || err.response?.status === 403) {
      const msg = err.response?.data?.message || ''
      if (msg.includes('hết hạn') || msg.includes('không hợp lệ') || msg.includes('xác thực') || msg.includes('truy cập')) {
        useAuthStore().logout()
        router.push('/login')
      }
    }
    return Promise.reject(err)
  }
)

export default api
