import api from './api'

export default {
  getHistory: (days = 30) => api.get(`/api/streak/history?days=${days}`).then(r => r.data),
  getCurrentStreak: () => api.get('/api/streak/current').then(r => r.data)
}
