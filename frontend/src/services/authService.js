import api from './api'

export default {
  login: (credentials) => api.post('/api/auth/login', credentials).then(r => r.data),
  register: (userData) => api.post('/api/auth/register', userData).then(r => r.data),
  getMe: () => api.get('/api/auth/me').then(r => r.data)
}
