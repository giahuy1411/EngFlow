import api from '@/services/api'

export default {
  getAnswers: (lessonId, skill) =>
    api.get(`/api/lessons/${lessonId}/answers`, { params: { skill } }).then(r => r.data),
}
