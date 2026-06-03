import api from './api'

export default {
  getProgress: () => api.get('/api/users/progress').then(r => r.data)
}
