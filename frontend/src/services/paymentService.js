import api from './api'

export default {
  getStatus() {
    return api.get('/api/v1/payment/status').then(r => r.data)
  },
  createOrder(planType) {
    return api.post('/api/v1/payment/create-order', { planType }).then(r => r.data)
  }
}
