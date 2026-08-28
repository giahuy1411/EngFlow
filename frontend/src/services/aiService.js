import api from './api'

export default {
  generateVocab: (topic, level = 'B2', count = 10) => 
    api.post('/api/ai/generate-vocab', { topic, level, count }, { timeout: 120000 }).then(r => r.data),
  enrichWord: (word) =>
    api.post('/api/ai/enrich-word', { word }, { timeout: 60000 }).then(r => r.data),
  saveVocab: (words) =>
    api.post('/api/ai/save-vocab', words, { timeout: 30000 }).then(r => r.data)
}
