import api from './api'

/**
 * Normalise the backend's vocabulary rows into the shape this service returns.
 * Shared by the local exact fast path and the proxy's DB fallback so both stay in sync.
 */
function mapBackendRows(data) {
  const rows = Array.isArray(data) ? data : []
  return rows.map((row, idx) => ({
    id: row.id || (idx + 1),
    word: row.word,
    phonetic: row.pronunciation || '',
    audioUrl: row.audioUrl || '',
    meanings: [{
      partOfSpeech: row.wordType || '',
      definitions: [{
        definition: row.definitionEn || row.meaning || '',
        example: row.exampleSentence || '',
        synonyms: [],
        antonyms: []
      }],
      synonyms: [],
      antonyms: []
    }],
    partOfSpeech: row.wordType || '',
    definition: row.definitionEn || row.meaning || '',
    example: row.exampleSentence || '',
    syllables: undefined,
    pronunciation: row.pronunciation,
    origin: undefined,
    cefrLevel: row.cefrLevel,
    source: row.source
  }))
}

/**
 * Tra từ - Dictionary lookup with rich metadata.
 * Uses free dictionaryapi.dev (no API key required).
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

  search: async (keyword) => {
    const trimmed = (keyword || '').trim()
    if (!trimmed) return []

    const url = `https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(trimmed.toLowerCase())}`
    // dictionaryapi.dev đôi khi chậm/từ chối kết nối từ VN — thử direct 15s,
    // thất bại thì retry 1 lần (thường DNS/TCP cache sẽ hoàn thành), sau đó
    // fallback proxy qua backend /api/vocabulary/search (permitAll, Oxford3000 DB).
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
      // 1) Proxy qua backend tới dictionaryapi.dev (container có mạng tới API
      //    mà browser VN đôi khi không vào được). Trả array cùng shape direct.
      //    Timeout 32s riêng: upstream thực tế ~20s khi cache lạnh, axios
      //    default 10s sẽ cắt sớm → báo "Không tìm thấy từ" sai.
      try {
        const proxy = await api.get(`/api/vocabulary/dictionary/${encodeURIComponent(trimmed)}`, { timeout: 32000 })
        const raw = typeof proxy.data === 'string' ? JSON.parse(proxy.data) : proxy.data
        if (Array.isArray(raw) && raw.length > 0) {
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
      } catch (proxyErr) { /* tiếp tục DB fallback */ }
      // 2) DB Oxford3000 trong backend (shape khác — normalize)
      const backend = await api.get(`/api/vocabulary/search?keyword=${encodeURIComponent(trimmed)}`)
      return mapBackendRows(backend.data)
    }

    // audit-v17 F-17-05 — resolve order, with a fast local fallback:
    //   The dictionary gives the RICH entry (phonetics, audio, several meanings, synonyms); the
    //   local table (118 rows) has no audio and one meaning, so it must NOT win by default.
    //   But the dictionary can take ~20 s on a cold upstream and its failure is not cached.
    //   So: start the dictionary immediately, and if the word is in the local table and the
    //   dictionary has not answered within LOCAL_GRACE_MS, return the local row NOW (the
    //   dictionary request keeps going and will populate the cache for the next lookup).
    //   -> rich entry when the dictionary is fast (warm cache); instant answer when it is cold.
    // NOTE: the proxy is called ONCE. It used to be reachable from two places, so a slow upstream
    // could be hit twice (~40 s worst case).
    const LOCAL_GRACE_MS = 1500;

    async function localExact() {
      const res = await api.get(`/api/vocabulary/search?keyword=${encodeURIComponent(trimmed)}&exact=true`)
      const rows = mapBackendRows(res.data)
      return rows.length === 0 ? null : rows
    }

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

    let local = null
    try { local = await localExact() } catch (localErr) { /* best-effort — never block the dictionary */ }

    if (local) {
      // Race the dictionary against a short grace period. Never leave dictPromise unhandled.
      const raced = await Promise.race([
        dictPromise.then(r => ({ kind: 'dict', r }), e => ({ kind: 'err', e })),
        new Promise(res => setTimeout(() => res({ kind: 'slow' }), LOCAL_GRACE_MS)),
      ]);
      if (raced.kind === 'dict') return raced.r;
      if (raced.kind === 'err') return local;      // dictionary failed -> local is the best answer
      dictPromise.catch(() => {});                 // dictionary still in flight -> let it warm the cache
      return local;                                // slow upstream -> answer instantly from local
    }

    return await dictPromise;
  }
}
