import api from './api'

export default {
  getLeaderboard: (params = {}) => {
    if (typeof params === 'number') {
      return api.get('/api/leaderboard', { params: { page: 0, size: params } }).then(r => r.data)
    }
    return api.get('/api/leaderboard', { params: { page: 0, size: 20, ...params } }).then(r => r.data)
  }
}