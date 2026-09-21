import api from './api'

/**
 * audit-v12: the SRS endpoints existed with no frontend caller. `GET /api/srs/due/{deckId}`
 * returns the words whose next review date has passed, and `GET /api/srs/stats` returns the
 * per-user progress counts.
 *
 * The API's field names differ from what the flashcard card renders (`vocabId` vs `id`,
 * `definitionVi` vs `meaning`, `pronunciation` vs `phonetic`, `exampleSentence` vs
 * `example`), so the mapping lives here — the same place `deckService.normalizeDeck` maps
 * the deck payload — so every consumer gets one consistent shape.
 */
function normalizeDueWord(row) {
  if (!row) return null
  return {
    id: row.vocabId ?? row.id,
    word: row.word,
    phonetic: row.pronunciation || '',
    meaning: row.definitionVi || row.meaning || '',
    definitionEn: row.definitionEn || '',
    example: row.exampleSentence || row.example || '',
    audioUrl: row.audioUrl || '',
    wordType: row.wordType || row.type || '',
    level: row.level ?? 0
  }
}

export default {
  getDueWords: (deckId) =>
    api.get(`/api/srs/due/${deckId}`).then(r => (Array.isArray(r.data) ? r.data.map(normalizeDueWord) : [])),

  getStats: () => api.get('/api/srs/stats').then(r => r.data),

  // quality 0-5, same scale the flashcard drill sends (audit-v12 F148 unified both paths).
  review: (vocabId, quality) => api.post('/api/srs/review', { vocabId, quality }).then(r => r.data)
}
