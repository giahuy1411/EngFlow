import api from './api'

export default {
  getBalance: () => api.get('/api/coins/balance').then(r => r.data),
  earnCoins: (amount) => api.post('/api/coins/earn', { amount }).then(r => r.data)
}
