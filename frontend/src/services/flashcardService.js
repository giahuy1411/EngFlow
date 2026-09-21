import api from './api'

export default {
  // audit-v12 F153: the flashcard drill is a plain back/continue reader now — it no longer
  // rates the word, so it no longer sends an SM-2 quality. It still counts as a study day
  // (for the streak), recorded once per session on the first "continue".
  recordStudy: () => api.post('/api/flashcards/study').then(r => r.data),

  // Kept for the still-registered POST /api/flashcards/review. No UI calls it since F153;
  // it is removed together with SrsService in the Phase 12 cleanup.
  reviewFlashcard: (vocabularyId, quality) => api.post('/api/flashcards/review', { vocabularyId, quality }).then(r => r.data),
  getStatus: (vocabularyId) => api.get(`/api/flashcards/status/${vocabularyId}`).then(r => r.data)
}
