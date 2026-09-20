import api from './api'

export default {
  generateVocab: (topic, level = 'B2', count = 10) => 
    api.post('/api/ai/generate-vocab', { topic, level, count }, { timeout: 120000 }).then(r => r.data),
  enrichWord: (word) =>
    api.post('/api/ai/enrich-word', { word }, { timeout: 60000 }).then(r => r.data),
  /**
   * audit-v11 F145: `deckId` is optional but strongly recommended. Without it the words are
   * written to the global `vocabulary` table with no deck and no owner, so the user pays a
   * quota credit, sees "saved!", and can never reach them again. With it, the backend adds
   * each word to that deck (ownership-checked) so they land somewhere the user can open.
   */
  saveVocab: (words, deckId = null) =>
    api.post('/api/ai/save-vocab', words, {
      params: deckId ? { deckId } : {},
      timeout: 30000,
    }).then(r => r.data)
}
