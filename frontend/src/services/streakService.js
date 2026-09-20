import api from './api'

export default {
  getSnapshot: () => api.get('/api/streak/snapshot').then(r => r.data),
  getHistory: (days = 30) => api.get(`/api/streak/history?days=${days}`).then(r => r.data),
  getCurrentStreak: () => api.get('/api/streak/current').then(r => r.data)
}
