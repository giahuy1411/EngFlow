import api from './api'

export default {
  getBalance: () => api.get('/api/coins/balance').then(r => r.data),
  earnCoins: (amount) => api.post('/api/coins/earn', { amount }).then(r => r.data),
  getShopItems: () => api.get('/api/shop/items').then(r => r.data),
  buyItem: (itemId) => api.post(`/api/shop/buy/${itemId}`).then(r => r.data),
  getOwnedItems: () => api.get('/api/shop/owned').then(r => r.data),
  equipItem: (itemId) => api.post(`/api/shop/equip/${itemId}`).then(r => r.data)
}
