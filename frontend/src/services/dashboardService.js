import api from './api'

export default {
  getStats: () => api.get('/api/dashboard/stats').then(r => r.data)
}
