import { defineStore } from 'pinia'
import { ref } from 'vue'
import lessonService from '@/services/lessonService'

export const useLessonStore = defineStore('lesson', () => {
  // State
  const lessons = ref([])
  const currentLesson = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const totalElements = ref(0)
  const totalPages = ref(1)

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

  // Paginated + filtered listing used by Lessons.vue
  // params: { page, size, level?, q? }
  async function fetchLessonPage(params = {}) {
    loading.value = true
    error.value = null
    try {
      const data = await lessonService.getPage(params)
      lessons.value = data.content || []
      totalElements.value = data.totalElements ?? lessons.value.length
      totalPages.value = data.totalPages ?? 1
    } catch (e) {
      error.value = e.response?.data?.error || e.response?.data?.message || 'Lỗi tải danh sách bài học'
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

  return { lessons, currentLesson, loading, error, totalElements, totalPages, fetchLessons, fetchLessonPage, fetchLessonById, clearCurrentLesson }
})
