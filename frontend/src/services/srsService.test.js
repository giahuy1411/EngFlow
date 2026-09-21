import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

import api from './api'
import srsService from './srsService'

/**
 * audit-v12: `GET /api/srs/due/{deckId}` had no frontend caller. The endpoint returns
 * vocabId / definitionVi / pronunciation / exampleSentence, while the flashcard card renders
 * id / meaning / phonetic / example. The mapping lives in the service (same place
 * deckService.normalizeDeck maps the deck payload) — these tests pin it.
 */
const API_ROW = {
  vocabId: 10020,
  word: 'determine',
  pronunciation: '/dɪˈtɜː.mɪn/',
  definitionVi: 'xác định',
  definitionEn: 'To control or influence something directly.',
  exampleSentence: 'Your health is determined in part by what you eat.',
  audioUrl: null,
  wordType: 'verb',
  level: 0
}

describe('srsService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('maps the API field names to the card field names', async () => {
    api.get.mockResolvedValueOnce({ data: [API_ROW] })

    const [w] = await srsService.getDueWords(10006)

    expect(api.get).toHaveBeenCalledWith('/api/srs/due/10006')
    expect(w.id).toBe(10020)                       // from vocabId
    expect(w.meaning).toBe('xác định')              // from definitionVi
    expect(w.phonetic).toBe('/dɪˈtɜː.mɪn/')         // from pronunciation
    expect(w.example).toContain('Your health')      // from exampleSentence
    expect(w.wordType).toBe('verb')
    expect(w.audioUrl).toBe('')                     // null -> '' so v-if stays falsy
  })

  it('returns [] when the API answers with a non-array', async () => {
    api.get.mockResolvedValueOnce({ data: null })
    expect(await srsService.getDueWords(1)).toEqual([])
  })

  it('sends the SM-2 quality to POST /api/srs/review', async () => {
    api.post.mockResolvedValueOnce({ data: {} })
    await srsService.review(10020, 4)
    expect(api.post).toHaveBeenCalledWith('/api/srs/review', { vocabId: 10020, quality: 4 })
  })
})
