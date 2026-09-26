import { describe, it, expect, vi, beforeEach } from 'vitest'

// audit-v17 F-17-05 — the refactor of search() must NOT lose the error contract the UI depends on.
// SearchVocabulary.vue branches on `e.message === 'TIMEOUT'` / 'NETWORK_ERROR'. These tests drive
// every failure path and assert the surfaced message is one of those two (never a raw AbortError
// or an axios error).

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

function abortError() {
  const e = new Error('aborted')
  e.name = 'AbortError'
  return e
}

describe('audit-v17 F-17-05 — error contract survives the refactor', () => {
  it('proxy fails + both direct attempts abort -> TIMEOUT', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [] })
      return Promise.reject(new Error('proxy down'))
    })
    fetchMock.mockRejectedValue(abortError())
    await expect(vocabularyService.search('hello')).rejects.toThrow('TIMEOUT')
  })

  it('proxy fails + direct returns a non-ok status -> NETWORK_ERROR', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [] })
      return Promise.reject(new Error('proxy down'))
    })
    fetchMock.mockResolvedValue({ ok: false, status: 503, json: () => Promise.resolve({}) })
    await expect(vocabularyService.search('hello')).rejects.toThrow('NETWORK_ERROR')
  })

  it('a localExact() failure does NOT abort the lookup (best-effort) — falls through to proxy', async () => {
    let sawProxy = false
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.reject(new Error('local table down'))
      if (url.includes('/api/vocabulary/dictionary/')) {
        sawProxy = true
        return Promise.resolve({ data: [{ word: 'hello', meanings: [{ definitions: [{ definition: 'a greeting' }] }] }] })
      }
      return Promise.resolve({ data: [] })
    })
    const out = await vocabularyService.search('hello')
    expect(sawProxy).toBe(true)
    expect(out.length).toBe(1)
  })

  it('a successful direct fetch (proxy down) still returns results, no error', async () => {
    apiGet.mockImplementation((url) => {
      if (url.includes('exact=true')) return Promise.resolve({ data: [] })
      return Promise.reject(new Error('proxy down'))
    })
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([{ word: 'hello', phonetics: [{ text: '/həˈləʊ/' }], meanings: [{ partOfSpeech: 'noun', definitions: [{ definition: 'a greeting' }] }] }]),
    })
    const out = await vocabularyService.search('hello')
    expect(out.length).toBe(1)
    expect(out[0].phonetic).toBe('/həˈləʊ/')
  })
})
