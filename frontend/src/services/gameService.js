import api from './api'

export default {
  getQuizData: (deckId) => api.get(`/api/games/quiz/${deckId}`).then(r => r.data),
  getMemoryData: (deckId) => api.get(`/api/games/memory/${deckId}`).then(r => r.data),
  getTypingData: (deckId) => api.get(`/api/games/typing/${deckId}`).then(r => r.data),
  getListeningData: (deckId) => api.get(`/api/games/listening/${deckId}`).then(r => r.data),
  getMixedData: (deckId) => api.get(`/api/games/mixed/${deckId}`).then(r => r.data),
  submitResult: (sessionId, correctAnswers) => 
    api.post('/api/games/submit', { sessionId, correctAnswers }).then(r => r.data)
}
