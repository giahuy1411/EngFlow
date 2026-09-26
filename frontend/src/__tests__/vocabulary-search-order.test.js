import { describe, it, expect, vi, beforeEach } from 'vitest'

// Regression tests for audit-v17 F-17-05 / F-17-20 (cross-review correction).
//
// The FIRST version of the F-17-05 fix tried a local exact match BEFORE the dictionary. Round 2
// of cross-review showed that is a QUALITY regression: the local table (118 rows) has no audio
// (`audio_url IS NULL` for all 118, verified) and one meaning, while the dictionary returns
// phonetics, an mp3, several meanings and synonyms. So the design is now:
//
//   - the dictionary is PREFERRED (rich entry),
//   - the local exact row is a FAST FALLBACK used when the dictionary is slow (> grace) or fails.
//
// These tests assert that preference, the fallback, and the call count — not a source string.

const apiGet = vi.fn()
vi.mock('@/services/api', () => ({
  default: { get: (...a) => apiGet(...a), post: vi.fn() },
}))

const fetchMock = vi.fn()
global.fetch = fetchMock

let vocabularyService

beforeEach(async () => {
  vi.clearAllMocks()
  vi.resetModules()
  vocabularyService = (await import('@/services/vocabularyService')).default
})

const localRow = { id: 1, word: 'ambitious', pronunciation: '/æmˈbɪʃ.əs/', meaning: 'có tham vọng', wordType: 'adjective' }
const dictRow = {
  word: 'ambitious',
  phonetics: [{ text: '/æmˈbɪʃ.əs/', audio: 'https://api.dictionaryapi.dev/media/ambitious.mp3' }],
  meanings: [
    { partOfSpeech: 'adjective', definitions: [{ definition: 'Having a strong desire for success.', example: 'She is ambitious.' }], synonyms: ['driven'] },
    { partOfSpeech: 'noun', definitions: [{ definition: 'A person who is ambitious.' }], synonyms: [] },
  ],
}

describe('audit-v17 F-17-20 — the dictionary entry is PREFERRED over the local row', () => {
  it('when the dictionary answers fast, its RICH entry wins (audio + meanings preserved)', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [localRow] })
      if (url.includes('/api/vocabulary/dictionary/')) return Promise.resolve({ data: [dictRow] })
      return Promise.resolve({ data: [] })
    })
    const out = await vocabularyService.search('ambitious')
    expect(out.length).toBeGreaterThanOrEqual(1)
    // The dictionary's audio + multi-meaning shape must be what the user gets.
    expect(out[0].audioUrl).toContain('.mp3')
    expect(out[0].meanings.length).toBeGreaterThan(1)
  })

  it('when the dictionary is SLOW, the local row is returned (no ~20s wait)', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [localRow] })
      if (url.includes('/api/vocabulary/dictionary/')) {
        // never resolves within the grace window
        return new Promise(() => {})
      }
      return Promise.resolve({ data: [] })
    })
    const t0 = Date.now()
    const out = await vocabularyService.search('ambitious')
    const elapsed = Date.now() - t0
    expect(out.length).toBe(1)
    expect(out[0].word).toBe('ambitious')
    expect(elapsed).toBeLessThan(6000) // answered from local, not by waiting ~20s
  })

  it('when the dictionary FAILS, the local row is the answer', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [localRow] })
      return Promise.reject(new Error('dictionary down'))
    })
    fetchMock.mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }))
    const out = await vocabularyService.search('ambitious')
    expect(out.length).toBe(1)
    expect(out[0].word).toBe('ambitious')
  })

  it('the dictionary proxy is called AT MOST ONCE', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [] })
      if (url.includes('/api/vocabulary/dictionary/')) return Promise.resolve({ data: [dictRow] })
      return Promise.resolve({ data: [] })
    })
    await vocabularyService.search('world')
    const proxyCalls = apiGet.mock.calls.filter(c => String(c[0]).includes('/api/vocabulary/dictionary/'))
    expect(proxyCalls.length).toBeLessThanOrEqual(1)
  })
})
