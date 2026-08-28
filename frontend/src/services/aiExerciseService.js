import api from './api'

export default {
  generateExercises: (lessonId, exerciseType, count) =>
    api.post('/api/admin/exercises/ai/generate', { lessonId, exerciseType, count }).then(r => r.data),

  // Option 2 — async generation (202 + batchId)
  generateExercisesAsync: (lessonId, exerciseType, count) =>
    api.post('/api/admin/exercises/ai/generate-async', { lessonId, exerciseType, count }).then(r => r.data),

  generateAll: (lessonId, count) =>
    api.post('/api/admin/exercises/ai/generate-all', { lessonId, count }).then(r => r.data),

  generateBatch: (force = false) =>
    api.post(`/api/admin/exercises/ai/generate-batch?force=${force}`).then(r => r.data),

  saveExercises: (exercises) =>
    api.post('/api/admin/exercises/ai/save', exercises).then(r => r.data),

  validateExercises: (exercises, useAiReview = false) =>
    api.post('/api/admin/exercises/ai/validate', { exercises, useAiReview }).then(r => r.data),

  getBatchStatus: () =>
    api.get('/api/admin/exercises/ai/status').then(r => r.data),

  // Option 2 — poll progress per batchId
  getStatus: (batchId) =>
    api.get(`/api/admin/exercises/ai/status?batchId=${batchId}`).then(r => r.data),
}
