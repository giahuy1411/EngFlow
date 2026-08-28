import api from './api'

/**
 * Game service — khởi tạo session và nộp kết quả lên backend.
 * Backend lưu answer map trong Redis, submit sẽ server-side validate.
 */
export default {
  startQuiz: (deckId) => api.get(`/api/games/quiz/${deckId}`).then(r => r.data),
  startMemory: (deckId) => api.get(`/api/games/memory/${deckId}`).then(r => r.data),
  startTyping: (deckId) => api.get(`/api/games/typing/${deckId}`).then(r => r.data),
  startListening: (deckId) => api.get(`/api/games/listening/${deckId}`).then(r => r.data),
  startMixed: (deckId) => api.get(`/api/games/mixed/${deckId}`).then(r => r.data),

  submit: (sessionId, answers, correctAnswers) =>
    api.post('/api/games/submit', { sessionId, answers, correctAnswers }).then(r => r.data),
}
