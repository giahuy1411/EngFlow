import api from './api'

export default {
  reviewFlashcard: (vocabularyId, isKnown) => api.post('/api/flashcards/review', { vocabularyId, isKnown }).then(r => r.data),
  getStatus: (vocabularyId) => api.get(`/api/flashcards/status/${vocabularyId}`).then(r => r.data)
}
