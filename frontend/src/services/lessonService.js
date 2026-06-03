import api from './api'

export default {
  getAll: () => api.get('/api/lessons').then(r => r.data),
  getById: (id) => api.get(`/api/lessons/${id}`).then(r => r.data),
  create: (lesson) => api.post('/api/lessons', lesson).then(r => r.data),
  update: (id, lesson) => api.put(`/api/lessons/${id}`, lesson).then(r => r.data),
  remove: (id) => api.delete(`/api/lessons/${id}`).then(r => r.data)
}
