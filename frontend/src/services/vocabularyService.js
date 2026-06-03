import api from './api'

export default {
  create: async (vocabData) => api.post('/api/vocabulary', vocabData).then(r => r.data),
  search: async (keyword) => {
    try {
      const response = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(keyword)}`)
      if (!response.ok) {
        if (response.status === 404) return []
        throw new Error('Network response was not ok')
      }
      
      const data = await response.json()
      const results = []
      let idCounter = 1
      
      data.forEach(entry => {
        const word = entry.word
        const pronunciation = entry.phonetic || (entry.phonetics && entry.phonetics.find(p => p.text)?.text) || ''
        
        // Chỉ lấy nghĩa phổ biến nhất (đầu tiên) để hiển thị 1 flashcard duy nhất cho mỗi từ
        if (entry.meanings && entry.meanings.length > 0) {
          const firstMeaning = entry.meanings[0];
          if (firstMeaning.definitions && firstMeaning.definitions.length > 0) {
            const firstDef = firstMeaning.definitions[0];
            results.push({
              id: idCounter++,
              word: word,
              pronunciation: pronunciation,
              wordType: firstMeaning.partOfSpeech,
              meaning: firstDef.definition,
              exampleSentence: firstDef.example || '',
              meanings: entry.meanings,
              phonetics: entry.phonetics || []
            });
          }
        }
      })
      
      return results
    } catch (error) {
      console.error('Dictionary API error:', error)
      throw error
    }
  }
}
