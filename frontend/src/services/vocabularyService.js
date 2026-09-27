import api from './api'

/**
 * Tra từ - Dictionary lookup with rich metadata.
 * Uses free dictionaryapi.dev (no API key required).
 *
 * audit-v17 closing round (C2): the local `vocabulary` table is NO LONGER on this path at all.
 * The helper that normalised its rows (`mapBackendRows`) was removed with it. The table remains
 * the DECK store (deck_words / user_vocabulary_progress) and is untouched by this service.
 *
 * Returned shape per result (kept consistent with UI expectations):
 *   { id, word, phonetic, audioUrl, meanings: [{ partOfSpeech, definition, example, synonyms[], antonyms[] }],
 *     origin, syllables, pronunciation }
 */
export default {
  // audit-v12 F147: `deckId` is sent so the server links the word to the deck in the SAME
  // transaction. Previously the caller had to make a second call to link it, which could
  // fail and strand the word in the shared dictionary with no owner.
  create: async (vocabData, deckId) => api.post(
    deckId ? `/api/vocabulary?deckId=${encodeURIComponent(deckId)}` : '/api/vocabulary',
    vocabData
  ).then(r => r.data),

  search: async (keyword, options = {}) => {
    const trimmed = (keyword || '').trim()
    if (!trimmed) return []

    const url = `https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(trimmed.toLowerCase())}`
    // dictionaryapi.dev đôi khi chậm/từ chối kết nối từ VN. Đường đi: proxy backend (container
    // có mạng tới API) → nếu proxy lỗi, gọi thẳng từ browser, retry 1 lần (DNS/TCP cache thường
    // giúp lần hai thành công). KHÔNG còn nhánh local DB nào (audit-v17 closing round C2).
    async function directFetch(timeoutMs) {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
      try {
        return await fetch(url, { signal: controller.signal })
      } finally {
        clearTimeout(timeoutId)
      }
    }
    async function mapDirectResponse(response) {
      if (!response.ok) {
        if (response.status === 404) return []
        throw new Error(`Lỗi tra cứu từ điển (HTTP ${response.status})`)
      }
      const data = await response.json()
      const results = []
      let idCounter = 1
      data.forEach(entry => {
        const word = entry.word || trimmed
        const phonetics = Array.isArray(entry.phonetics) ? entry.phonetics : []
        const phoneticText = phonetics.find(p => p && p.text)?.text || ''
        const audioEntry = phonetics.find(p => p && p.audio && p.audio.length > 0)
        const audioUrl = audioEntry ? audioEntry.audio : ''
        const meanings = (entry.meanings || []).map(m => {
          const partOfSpeech = m.partOfSpeech || ''
          const definitions = (m.definitions || []).map(d => ({
            definition: d.definition || '',
            example: d.example || '',
            synonyms: Array.isArray(d.synonyms) ? d.synonyms : [],
            antonyms: Array.isArray(d.antonyms) ? d.antonyms : []
          }))
          return {
            partOfSpeech,
            definitions,
            synonyms: Array.isArray(m.synonyms) ? m.synonyms : [],
            antonyms: Array.isArray(m.antonyms) ? m.antonyms : []
          }
        })
        results.push({
          id: idCounter++,
          word,
          phonetic: phoneticText,
          audioUrl,
          meanings,
          partOfSpeech: meanings[0]?.partOfSpeech || '',
          definition: meanings[0]?.definitions[0]?.definition || '',
          example: meanings[0]?.definitions[0]?.example || '',
          syllables: entry.syllables,
          pronunciation: entry.pronunciation,
          origin: entry.origin
        })
      })
      return results
    }
    async function backendFallback() {
      // Proxy qua backend tới dictionaryapi.dev (container có mạng tới API mà browser VN đôi
      // khi không vào được). Timeout 32s riêng: upstream thực tế ~20s khi cache lạnh, axios
      // default 10s sẽ cắt sớm → báo "Không tìm thấy từ" sai.
      //
      // audit-v17 closing round (C2): the local-table step that used to follow this proxy call
      // was REMOVED — the dictionary is the ONLY lookup source now. A proxy SUCCESS is final,
      // even an empty array (upstream 404 → "[]" = the word does not exist); only a proxy
      // FAILURE throws, so the caller can fall through to the direct browser call. A cold
      // upstream that times out inside the backend also yields "[]", but the caller's ~6 s
      // budget has already surfaced TIMEOUT by then, so the user sees "quá lâu", never a false
      // "not found".
      const proxy = await api.get(`/api/vocabulary/dictionary/${encodeURIComponent(trimmed)}`, { timeout: 32000 })
      const raw = typeof proxy.data === 'string' ? JSON.parse(proxy.data) : proxy.data
      if (!Array.isArray(raw)) return []
      return raw.map((entry, idx) => ({
        id: idx + 1,
        word: entry.word || trimmed,
        phonetic: (entry.phonetics || []).find(p => p && p.text)?.text || '',
        audioUrl: (entry.phonetics || []).find(p => p && p.audio)?.audio || '',
        meanings: (entry.meanings || []).map(m => ({
          partOfSpeech: m.partOfSpeech || '',
          definitions: (m.definitions || []).map(d => ({
            definition: d.definition || '',
            example: d.example || '',
            synonyms: Array.isArray(d.synonyms) ? d.synonyms : [],
            antonyms: Array.isArray(d.antonyms) ? d.antonyms : []
          })),
          synonyms: Array.isArray(m.synonyms) ? m.synonyms : [],
          antonyms: Array.isArray(m.antonyms) ? m.antonyms : []
        })),
        partOfSpeech: entry.meanings?.[0]?.partOfSpeech || '',
        definition: entry.meanings?.[0]?.definitions?.[0]?.definition || '',
        example: entry.meanings?.[0]?.definitions?.[0]?.example || '',
        syllables: entry.syllables,
        pronunciation: entry.pronunciation,
        origin: entry.origin
      }))
    }

    // audit-v17 remove-limits round (L1-A) — do NOT fail at 6 s; keep waiting.
    //
    //   Measured: the upstream's OWN TTFB is ~19.5 s (DNS + connect + TLS are only 0.17 s), so a
    //   cold lookup of ANY word — found or not — costs ~20 s the first time. The previous version
    //   threw TIMEOUT after 6 s, so the user saw "Tra cứu quá lâu" for a word that WOULD have
    //   resolved at ~20 s. That is a false failure.
    //
    //   Fix: one request, waited on for as long as it can legitimately take. DICT_BUDGET_MS is now a
    //   SOFT threshold — at 6 s we tell the caller "still working" (so the view can show a calmer
    //   message) and KEEP waiting for the SAME request. DICT_TOTAL_MS is the hard cap; it sits above
    //   the whole fallback chain (32 s backend-proxy timeout + 4 s + 4 s direct retries), so the real
    //   outcome (a word, an empty result, or a genuine network error) is what the user sees.
    //
    //   We deliberately do NOT re-issue the request on the slow path: a second in-flight call would
    //   hit the upstream while the first is still running, and the upstream throttles concurrency
    //   (measured: 6 parallel lookups -> 5 timeouts). One request, waited on, is both faster and
    //   kinder to the upstream. The request also warms the Redis cache, so the next lookup is ~65 ms.
    const DICT_BUDGET_MS = 6000;   // soft: "still working" signal
    const DICT_TOTAL_MS = 45000;   // hard: above 32 s proxy + 4 s + 4 s direct retries

    // Kick off the dictionary path immediately (not awaited yet).
    const dictPromise = (async () => {
      let response
      try {
        return await backendFallback()
      } catch (proxyErr) {
        // The proxy is unreachable — fall back to a direct browser call (retried once, since a
        // transient DNS/TCP failure often succeeds on the second attempt).
        let directErr = null
        try {
          response = await directFetch(4000)
        } catch (firstErr) {
          try {
            response = await directFetch(4000)
          } catch (secondErr) {
            directErr = secondErr
          }
        }
        if (directErr) {
          // audit-v17 round 2 (F-17-19, cross-review): an AbortError means the 4s timeout fired
          // (slow upstream); anything else (e.g. `TypeError: Failed to fetch` when the browser is
          // OFFLINE) is a network failure. The UI shows different guidance for each
          // (SearchVocabulary.vue:136-139), so collapsing both to TIMEOUT would tell an offline
          // user "the dictionary is slow" — wrong advice.
          throw new Error(directErr.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR')
        }
        try {
          return await mapDirectResponse(response)
        } catch (mapErr) {
          // Direct returned a non-ok, non-404 status. Preserve the UI's error contract.
          throw new Error('NETWORK_ERROR')
        }
      }
    })();

    // Soft signal at DICT_BUDGET_MS: tell the caller the lookup is slow but still running. Never let
    // a callback error affect the lookup.
    let budgetId
    if (typeof options.onSlow === 'function') {
      budgetId = setTimeout(() => { try { options.onSlow() } catch (e) { /* ignore */ } }, DICT_BUDGET_MS)
    }
    // Hard cap. The request is never abandoned — it is left to settle so the Redis cache warms.
    let totalId
    const total = new Promise((_, reject) => {
      totalId = setTimeout(() => reject(new Error('TIMEOUT')), DICT_TOTAL_MS)
    })
    try {
      return await Promise.race([dictPromise, total])
    } finally {
      clearTimeout(budgetId)
      clearTimeout(totalId)
      dictPromise.catch(() => {}) // swallow if it settles after we already gave up
    }
  }
}
