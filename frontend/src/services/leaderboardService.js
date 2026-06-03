import api from './api'

export default {
  getLeaderboard: (limit = 20) => api.get(`/api/leaderboard?limit=${limit}`).then(r => r.data)
}
