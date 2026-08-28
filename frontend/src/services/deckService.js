import api from './api'

function normalizeDeck(deck) {
  if (!deck || !Array.isArray(deck.words)) return deck

  return {
    ...deck,
    words: deck.words.map(entry => {
      const vocabulary = entry?.vocabulary || entry
      return {
        ...vocabulary,
        id: vocabulary?.id ?? entry?.id,
        deckWordId: entry?.id,
        orderIndex: entry?.orderIndex
      }
    })
  }
}

export default {
  getPublicDecks: (params = {}) => api.get('/api/decks', { params: { page: 0, size: 9, ...params } }).then(r => r.data),
  getMyDecks: (params = {}) => api.get('/api/decks/my', { params: { page: 0, size: 9, ...params } }).then(r => r.data),
  getDeckById: (id) => api.get(`/api/decks/${id}`).then(r => normalizeDeck(r.data)),
  createDeck: (deckData) => api.post('/api/decks', deckData).then(r => r.data),
  updateDeck: (id, deckData) => api.put(`/api/decks/${id}`, deckData).then(r => r.data),
  deleteDeck: (id) => api.delete(`/api/decks/${id}`).then(r => r.data),
  addWordToDeck: (deckId, vocabId) => api.post(`/api/decks/${deckId}/words`, { vocabId }).then(r => r.data)
}
