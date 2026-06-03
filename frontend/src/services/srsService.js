import api from './api'

export default {
  reviewWord: (vocabId, quality) => api.post('/api/srs/review', { vocabId, quality }).then(r => r.data),
  getDueWords: (deckId) => api.get(`/api/srs/due/${deckId}`).then(r => r.data),
  getStats: () => api.get('/api/srs/stats').then(r => r.data)
}
