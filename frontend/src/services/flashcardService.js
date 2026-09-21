import api from './api'

export default {
  // audit-v12 F148: the second argument is now the SM-2 quality (0-5), not a boolean.
  // The three flashcard buttons map to 1 ("Lại") / 4 ("Tiếp theo") / 5 ("Dễ").
  reviewFlashcard: (vocabularyId, quality) => api.post('/api/flashcards/review', { vocabularyId, quality }).then(r => r.data),
  getStatus: (vocabularyId) => api.get(`/api/flashcards/status/${vocabularyId}`).then(r => r.data)
}
