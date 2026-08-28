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
  create: async (vocabData) => api.post('/api/vocabulary', vocabData).then(r => r.data),

  search: async (keyword) => {
    const trimmed = (keyword || '').trim()
    if (!trimmed) return []

    const url = `https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(trimmed.toLowerCase())}`
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 8000)
    let response
    try {
      response = await fetch(url, { signal: controller.signal })
    } catch (e) {
      if (e && e.name === 'AbortError') {
        throw new Error('TIMEOUT')
      }
      throw new Error('NETWORK_ERROR')
    } finally {
      clearTimeout(timeoutId)
    }
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

      // Map all meanings + all definitions (not just the first one)
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
        // Convenience flat field for the first definition (legacy UI fallback)
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
}
