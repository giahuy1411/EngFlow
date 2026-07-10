import api from './api'

export default {
  getAll: () => api.get('/api/lessons').then(r => r.data),
  getById: (id) => api.get(`/api/lessons/${id}`).then(r => r.data),
  create: (lesson) => api.post('/api/lessons', lesson).then(r => r.data),
  update: (id, lesson) => api.put(`/api/lessons/${id}`, lesson).then(r => r.data),
  remove: (id) => api.delete(`/api/lessons/${id}`).then(r => r.data),
  // Exercises
  getExercises: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises`).then(r => r.data),
  getExercisesWithAnswers: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises?includeAnswers=true`).then(r => r.data),
  gradeExercises: (lessonId, answers) => api.post(`/api/lessons/${lessonId}/exercises/grade`, { answers }).then(r => r.data),
  submitExercises: (lessonId, answers) => api.post(`/api/lessons/${lessonId}/exercises/submit`, { answers }).then(r => r.data),
  getAttempts: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises/attempts`).then(r => r.data),
  getAttemptDetail: (lessonId, attemptId) => api.get(`/api/lessons/${lessonId}/exercises/attempts/${attemptId}`).then(r => r.data),
}
