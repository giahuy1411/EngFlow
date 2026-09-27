import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Regression tests for audit-v17 — the dictionary is the ONLY lookup source.
//
// History: F-17-05 added a local exact-match fast path; F-17-20 (cross-review round 2) reversed
// it to "dictionary preferred, local row as a slow/failed fallback". The user then decided to
// remove the local DB from the lookup path entirely: the `vocabulary` table is the DECK store
// (100/118 rows are deck_words) and its rows carry no audio and one meaning, so serving them in
// tra-từ silently degraded the answer. Now:
//
//   - search() calls the dictionary proxy (→ direct fallback) and NOTHING else,
//   - a cold lookup WAITS (remove-limits round L1-A): a soft 6 s threshold only signals "still
//     working"; the request keeps going up to a hard 45 s cap. A cold word legitimately takes
//     ~20 s (measured upstream TTFB ~19.5 s), so failing at 6 s was a FALSE failure,
//   - a local row can never satisfy a lookup — no `exact=true` request exists.
//
// These tests assert behaviour, not source strings: the "no exact=true call" assertion fails the
// moment a local fast path is reintroduced (mutation-sensitive).

const apiGet = vi.fn()
vi.mock('@/services/api', () => ({
  default: { get: (...a) => apiGet(...a), post: vi.fn() },
}))

const fetchMock = vi.fn()
global.fetch = fetchMock

let vocabularyService

beforeEach(async () => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  vi.resetModules()
  vocabularyService = (await import('@/services/vocabularyService')).default
})

afterEach(() => {
  vi.useRealTimers()
})

const dictRow = {
  word: 'ambitious',
  phonetics: [{ text: '/æmˈbɪʃ.əs/', audio: 'https://api.dictionaryapi.dev/media/ambitious.mp3' }],
  meanings: [
    { partOfSpeech: 'adjective', definitions: [{ definition: 'Having a strong desire for success.', example: 'She is ambitious.' }], synonyms: ['driven'] },
    { partOfSpeech: 'noun', definitions: [{ definition: 'A person who is ambitious.' }], synonyms: [] },
  ],
}

describe('audit-v17 — the dictionary is the only lookup source', () => {
  it('returns the RICH dictionary entry (audio + several meanings)', async () => {
    apiGet.mockResolvedValue({ data: [dictRow] })
    const out = await vocabularyService.search('ambitious')
    expect(out.length).toBeGreaterThanOrEqual(1)
    expect(out[0].audioUrl).toContain('.mp3')
    expect(out[0].meanings.length).toBeGreaterThan(1)
  })

  it('NEVER requests the local exact fast path (no `exact=true` call)', async () => {
    apiGet.mockResolvedValue({ data: [dictRow] })
    await vocabularyService.search('ambitious')
    const exactCalls = apiGet.mock.calls.filter(c => String(c[0]).includes('exact=true'))
    expect(exactCalls.length).toBe(0)
  })

  it('a word the dictionary does not know returns [] — never a local row', async () => {
    // Proxy answers [] (upstream 404 → "[]"). The local table is NOT consulted at all, so the
    // result is [] — there is no second request that could return a stale deck word.
    apiGet.mockImplementation((url) => {
      if (url.includes('/api/vocabulary/dictionary/')) return Promise.resolve({ data: [] })
      return Promise.resolve({ data: [{ word: 'zzznotaword', meaning: 'should never be reached' }] })
    })
    const out = await vocabularyService.search('zzznotaword')
    expect(out).toEqual([])
    const localCalls = apiGet.mock.calls.filter(c => String(c[0]).includes('/api/vocabulary/search'))
    expect(localCalls.length).toBe(0) // the local table is off the lookup path entirely
  })

  it('the dictionary proxy is called AT MOST ONCE', async () => {
    apiGet.mockResolvedValue({ data: [dictRow] })
    await vocabularyService.search('world')
    const proxyCalls = apiGet.mock.calls.filter(c => String(c[0]).includes('/api/vocabulary/dictionary/'))
    expect(proxyCalls.length).toBeLessThanOrEqual(1)
  })

  // ── L1-A: a cold lookup waits; it does not fail at 6 s ─────────────────────
  it('a COLD lookup keeps waiting past 6 s and returns the real result (no false TIMEOUT)', async () => {
    let resolveDict
    apiGet.mockImplementation((url) => {
      if (url.includes('/api/vocabulary/dictionary/')) return new Promise((res) => { resolveDict = res })
      return Promise.resolve({ data: [] })
    })
    const onSlow = vi.fn()
    const p = vocabularyService.search('ambitious', { onSlow })

    // Past the SOFT threshold: onSlow fires, but the promise must still be pending (not rejected).
    await vi.advanceTimersByTimeAsync(6100)
    expect(onSlow).toHaveBeenCalledTimes(1)

    // The upstream answers at ~20 s — the same call must now resolve with the real entry.
    await vi.advanceTimersByTimeAsync(14000)
    resolveDict({ data: [dictRow] })
    const out = await p
    expect(out.length).toBeGreaterThanOrEqual(1)
    expect(out[0].audioUrl).toContain('.mp3')
  })

  it('a lookup that NEVER answers still gives up at the hard cap (45 s) with TIMEOUT', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('/api/vocabulary/dictionary/')) return new Promise(() => {}) // never resolves
      return Promise.resolve({ data: [] })
    })
    const p = vocabularyService.search('ambitious')
    const settled = expect(p).rejects.toThrow('TIMEOUT')
    await vi.advanceTimersByTimeAsync(45100)
    await settled
  })

  it('a cold lookup does NOT re-issue a second proxy call while the first is in flight', async () => {
    // The upstream throttles concurrency; re-firing on the slow path would hurt, not help.
    apiGet.mockImplementation((url) => {
      if (url.includes('/api/vocabulary/dictionary/')) return new Promise(() => {})
      return Promise.resolve({ data: [] })
    })
    const p = vocabularyService.search('ambitious')
    await vi.advanceTimersByTimeAsync(20000)
    const proxyCalls = apiGet.mock.calls.filter(c => String(c[0]).includes('/api/vocabulary/dictionary/'))
    expect(proxyCalls.length).toBe(1)
    p.catch(() => {})
  })
})
