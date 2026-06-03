import api from './api'

export default {
  submit: (payload) => api.post('/api/exercises/submit', payload).then(r => r.data),
  getSubmissions: () => api.get('/api/exercises/submissions').then(r => r.data)
}
