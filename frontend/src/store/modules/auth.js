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

  // Actions
  async function login(credentials) {
    loading.value = true
    error.value = null
    try {
      const data = await authService.login(credentials)
      token.value = data.token
      user.value = {
        id: data.id,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        avatarUrl: data.avatarUrl,
        isAdmin: data.isAdmin,
        currentLevel: data.currentLevel,
        totalPoints: data.totalPoints,
        currentStreak: data.currentStreak
      }
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
        user.value = {
          id: data.id,
          username: data.username,
          email: data.email,
          fullName: data.fullName,
          avatarUrl: data.avatarUrl,
          isAdmin: data.isAdmin,
          currentLevel: data.currentLevel,
          totalPoints: data.totalPoints,
          currentStreak: data.currentStreak
        }
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
      user.value = {
        id: data.id,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        avatarUrl: data.avatarUrl,
        isAdmin: data.isAdmin,
        currentLevel: data.currentLevel,
        totalPoints: data.totalPoints,
        currentStreak: data.currentStreak
      }
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch (e) {
      logout()
    }
  }

  return { token, user, loading, error, isLoggedIn, isAdmin, login, register, logout, fetchUser }
})
