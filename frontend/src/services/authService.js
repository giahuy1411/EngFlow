import api from './api'

export default {
  login: (credentials) => api.post('/api/auth/login', credentials).then(r => r.data),
  register: (userData) => api.post('/api/auth/register', userData).then(r => r.data),
  getMe: () => api.get('/api/auth/me').then(r => r.data),
  updateAvatar: (avatarUrl) => api.put('/api/auth/avatar', { avatarUrl }).then(r => r.data),
  uploadAvatarFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/auth/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  }
}
