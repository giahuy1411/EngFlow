import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/authService'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  
  let initialUser = null
  try {
    initialUser = JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    localStorage.removeItem('user')
  }
  const user = ref(initialUser)
  
  const loading = ref(false)
  const error = ref(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.isAdmin)
  // Guard /speaking/* đọc auth.isPremium — phải expose đúng từ user payload (UserResponse.isPremium)
  const isPremium = computed(() => user.value?.isPremium === true)
  /**
   * Quyền premium do server tính (admin luôn có toàn quyền). Fallback isAdmin để
   * tài khoản còn cache trong localStorage từ trước khi có field này vẫn đúng.
   */
  const hasPremiumAccess = computed(
    () => user.value?.hasPremiumAccess === true || user.value?.isAdmin === true
  )

  /**
   * Chuẩn hoá payload UserResponse → state user. Tập trung ở một chỗ vì login,
   * register và /me trả cùng hình dạng; thêm field mới chỉ cần sửa một nơi.
   * Không nhét token vào đây: token là state riêng, lưu trùng dễ lộ JWT thêm chỗ.
   */
  function mapUser(data) {
    return {
      id: data.id,
      username: data.username,
      email: data.email,
      fullName: data.fullName,
      avatarUrl: data.avatarUrl,
      isAdmin: data.isAdmin,
      isPremium: data.isPremium === true,
      premiumExpiry: data.premiumExpiry,
      currentLevel: data.currentLevel,
      totalPoints: data.totalPoints,
      currentStreak: data.currentStreak,
      // Quyền lợi và hạn mức AI hiển thị trên Profile / Luyện từ.
      hasPremiumAccess: data.hasPremiumAccess === true,
      aiGenerationCount: data.aiGenerationCount ?? 0,
      aiGenerationsRemainingToday: data.aiGenerationsRemainingToday ?? null
    }
  }

  // Actions
  async function login(credentials) {
    loading.value = true
    error.value = null
    try {
      const data = await authService.login(credentials)
      token.value = data.token
      user.value = mapUser(data)
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch (e) {
      error.value = e.response?.data?.error || 'Đăng nhập thất bại'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(userData) {
    loading.value = true
    error.value = null
    try {
      const data = await authService.register(userData)
      if (data.token) {
        token.value = data.token
        user.value = mapUser(data)
        localStorage.setItem('token', token.value)
        localStorage.setItem('user', JSON.stringify(user.value))
      }
      return data
    } catch (e) {
      error.value = e.response?.data?.error || 'Đăng ký thất bại'
      throw e
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const data = await authService.getMe()
      user.value = mapUser(data)
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch (e) {
      logout()
    }
  }

  /**
   * Gửi email đặt lại mật khẩu. Backend hiện chưa có endpoint forgot-password,
   * nên fail-soft: hiển thị success chung chung để không leak email tồn tại.
   */
  async function forgotPassword(email) {
    loading.value = true
    error.value = null
    try {
      await authService.requestPasswordReset(email)
      return true
    } catch (e) {
      // 404 = backend chưa hỗ trợ — vẫn báo success để không lộ email tồn tại/không
      if (e.response && e.response.status === 404) return true
      error.value = e.response?.data?.message || 'Gửi yêu cầu thất bại'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { token, user, loading, error, isLoggedIn, isAdmin, isPremium, hasPremiumAccess, login, register, logout, fetchUser, forgotPassword }
})
