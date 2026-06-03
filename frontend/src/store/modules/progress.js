import { defineStore } from 'pinia'
import { ref } from 'vue'
import progressService from '@/services/progressService'

export const useProgressStore = defineStore('progress', () => {
  // State
  const progressSummary = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // Actions
  async function fetchProgressSummary() {
    loading.value = true
    error.value = null
    try {
      progressSummary.value = await progressService.getProgress()
    } catch (e) {
      error.value = e.response?.data?.error || 'Lỗi tải tiến độ học tập'
    } finally {
      loading.value = false
    }
  }

  return { progressSummary, loading, error, fetchProgressSummary }
})
