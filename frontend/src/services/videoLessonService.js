import api from './api'

const list = (page = 0, size = 12, level = '') => {
  const params = { page, size }
  if (level) params.level = level
  return api.get('/api/v1/video-lessons', { params }).then(response => response.data)
}

export default {
  list,
  getById: id => api.get(`/api/v1/video-lessons/${id}`).then(response => response.data),
  submitAttempt(id, lineIndex, file) {
    const form = new FormData()
    form.append('lineIndex', lineIndex)
    form.append('file', file)
    return api.post(`/api/v1/video-lessons/${id}/attempts`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }).then(response => response.data)
  },
  myAttempts(page = 0, size = 50) {
    return api.get('/api/v1/video-attempts', { params: { page, size } }).then(response => response.data)
  },
  // Admin
  getAllAdmin: (page = 0, size = 50) =>
    api.get('/api/v1/admin/video-lessons', { params: { page, size } }).then(response => response.data),
  create: data => api.post('/api/v1/admin/video-lessons', data).then(response => response.data),
  createWithTranscript(meta, transcriptText) {
    const form = new FormData()
    form.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
    if (transcriptText) form.append('transcriptText', transcriptText)
    return api.post('/api/v1/admin/video-lessons/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    }).then(response => response.data)
  },
  update: (id, data) => api.put(`/api/v1/admin/video-lessons/${id}`, data).then(response => response.data),
  remove: id => api.delete(`/api/v1/admin/video-lessons/${id}`).then(response => response.data),
  getAdminAttempts(status = '', page = 0, size = 20) {
    const params = { page, size }
    if (status) params.status = status
    return api.get('/api/v1/admin/video-attempts', { params }).then(response => response.data)
  },
  gradeAttempt(id, score, feedback) {
    return api.patch(`/api/v1/admin/video-attempts/${id}/grade`, { score, feedback })
      .then(response => response.data)
  },
  // AI grading is synchronous (Whisper + LLM); allow up to 3 minutes.
  aiGradeAttempt(id) {
    return api.post(`/api/v1/admin/video-attempts/${id}/ai-grade`, null, { timeout: 180000 })
      .then(response => response.data)
  },
  // AI subtitle translation (fail-soft); returns lines with textVi filled.
  translateTranscript(lines) {
    return api.post('/api/v1/admin/video-lessons/translate-transcript', lines, { timeout: 180000 })
      .then(response => response.data)
  },
  // Fetches an existing YouTube transcript (auto or human captions) and has
  // the local LLM translate every cue to Vietnamese; also returns title and
  // description so the form can be pre-filled. Long local-LLM call → 3 min.
  fetchYoutubeTranscript(url) {
    return api.post('/api/v1/admin/video-lessons/fetch-youtube', { url }, { timeout: 180000 })
      .then(response => response.data)
  }
}
