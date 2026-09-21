import api from './api'

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
      const rows = Array.isArray(backend.data) ? backend.data : []
      if (rows.length === 0) return []
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

    let response
    try {
      // Proxy backend là đường chính: Redis cache 1h dùng chung mọi user +
      // timeout 3s/30s phía server. Chỉ khi proxy lỗi mới thử direct từ
      // browser (4s×2). Tiết kiệm ~8s chờ vô ích cho mỗi từ mới.
      try {
        return await backendFallback()
      } catch (proxyErr) {
        try {
          response = await directFetch(4000)
        } catch (firstErr) {
          response = await directFetch(4000)
        }
        return await mapDirectResponse(response)
      }
    } catch (e) {
      // mapDirectResponse lỗi HTTP (404 trả [] nên không tới đây) — thử backend lần cuối
      try {
        return await backendFallback()
      } catch (backendErr) {
        if (e && e.name === 'AbortError') {
          throw new Error('TIMEOUT')
        }
        throw new Error('NETWORK_ERROR')
      }
    }
  }
}
