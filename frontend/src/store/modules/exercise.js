import { defineStore } from 'pinia'
import { ref } from 'vue'
import exerciseService from '@/services/exerciseService'
import { useAuthStore } from './auth'

export const useExerciseStore = defineStore('exercise', () => {
  // State
  const submissions = ref([])
  const submitting = ref(false)
  const submitError = ref(null)

  // Actions
  async function submitExerciseAnswer(payload) {
    submitting.value = true
    submitError.value = null
    try {
      const result = await exerciseService.submit(payload)
      // Refresh user points if correct
      if (result.isCorrect) {
        const auth = useAuthStore()
        await auth.fetchUser()
      }
      return result
    } catch (e) {
      submitError.value = e.response?.data?.error || 'Lỗi nộp câu trả lời'
      throw e
    } finally {
      submitting.value = false
    }
  }

  async function fetchSubmissions() {
    try {
      submissions.value = await exerciseService.getSubmissions()
    } catch (e) {
      console.error('Lỗi tải lịch sử bài tập:', e)
      submitError.value = e.response?.data?.error || 'Không thể tải lịch sử bài tập'
    }
  }

  return { submissions, submitting, submitError, submitExerciseAnswer, fetchSubmissions }
})
