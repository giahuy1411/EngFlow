import api from './api'

export default {
  getPublicDecks: () => api.get('/api/decks').then(r => r.data),
  getMyDecks: () => api.get('/api/decks/my').then(r => r.data),
  getDeckById: (id) => api.get(`/api/decks/${id}`).then(r => r.data),
  createDeck: (deckData) => api.post('/api/decks', deckData).then(r => r.data),
  updateDeck: (id, deckData) => api.put(`/api/decks/${id}`, deckData).then(r => r.data),
  deleteDeck: (id) => api.delete(`/api/decks/${id}`).then(r => r.data),
  addWordToDeck: (deckId, vocabId) => api.post(`/api/decks/${deckId}/words`, { vocabId }).then(r => r.data)
}
