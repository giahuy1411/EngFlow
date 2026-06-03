import { defineStore } from 'pinia'
import { ref } from 'vue'
import lessonService from '@/services/lessonService'

export const useLessonStore = defineStore('lesson', () => {
  // State
  const lessons = ref([])
  const currentLesson = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // Actions
  async function fetchLessons() {
    loading.value = true
    error.value = null
    try {
      lessons.value = await lessonService.getAll()
    } catch (e) {
      error.value = e.response?.data?.error || 'Lỗi tải danh sách bài học'
    } finally {
      loading.value = false
    }
  }

  async function fetchLessonById(id) {
    loading.value = true
    error.value = null
    try {
      currentLesson.value = await lessonService.getById(id)
    } catch (e) {
      error.value = e.response?.data?.error || 'Lỗi tải chi tiết bài học'
      throw e
    } finally {
      loading.value = false
    }
  }

  function clearCurrentLesson() {
    currentLesson.value = null
  }

  return { lessons, currentLesson, loading, error, fetchLessons, fetchLessonById, clearCurrentLesson }
})
