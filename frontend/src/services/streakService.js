import api from './api'

export default {
  checkin: (wordsStudied = 0, gamesPlayed = 0, coinsEarned = 0) => 
    api.post('/api/streak/checkin', { wordsStudied, gamesPlayed, coinsEarned }).then(r => r.data),
  getHistory: (days = 30) => api.get(`/api/streak/history?days=${days}`).then(r => r.data),
  getCurrentStreak: () => api.get('/api/streak/current').then(r => r.data)
}
