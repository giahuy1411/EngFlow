import api from './api'

const fetchPrompts = (page = 0, size = 100, admin = false, q = '') => {
  const prefix = admin ? '/api/v1/admin' : '/api/v1'
  const params = { page, size }
  if (q) params.q = q
  return api.get(`${prefix}/speaking-prompts`, { params })
    .then(response => response.data)
}

const createPrompt = data => api.post('/api/v1/admin/speaking-prompts', data).then(response => response.data)
const updatePrompt = (id, data) => api.put(`/api/v1/admin/speaking-prompts/${id}`, data).then(response => response.data)
const deletePrompt = id => api.delete(`/api/v1/admin/speaking-prompts/${id}`).then(response => response.data)

export default {
  getPrompts(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, false, q)
  },
  getAdminPrompts(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, true, q)
  },
  getAll(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, false, q)
  },
  getAllAdmin(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, true, q)
  },
  getById(id) {
    return api.get(`/api/v1/speaking-prompts/${id}`).then(response => response.data)
  },
  createPrompt,
  updatePrompt,
  deletePrompt,
  create: createPrompt,
  update: updatePrompt,
  delete: deletePrompt,
  uploadSubmission(promptId, file) {
    const form = new FormData()
    form.append('promptId', promptId)
    form.append('file', file)
    return api.post('/api/v1/speaking-submissions/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }).then(response => response.data)
  },
  getSubmissions(page = 0, size = 100) {
    return api.get('/api/v1/speaking-submissions', { params: { page, size } })
      .then(response => response.data)
  },
  getPromptSubmissions(promptId, page = 0, size = 20) {
    return api.get(`/api/v1/speaking-prompts/${promptId}/submissions`, { params: { page, size } })
      .then(response => response.data)
  },
  getAdminSubmissions(page = 0, size = 100, status = '') {
    const params = { page, size }
    if (status) params.status = status
    return api.get('/api/v1/admin/speaking-submissions', { params })
      .then(response => response.data)
  },
  getSubmission(id) {
    return api.get(`/api/v1/speaking-submissions/${id}`).then(response => response.data)
  },
  gradeSubmission(id, data) {
    return api.patch(`/api/v1/admin/speaking-submissions/${id}/grade`, data)
      .then(response => response.data)
  }
}
