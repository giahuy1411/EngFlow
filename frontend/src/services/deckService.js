import api from './api'

/**
 * Chuẩn hoá response chi tiết một deck về dạng phẳng dễ dùng ở UI.
 *
 * Backend trả mỗi word dưới dạng `{ id (deckWordId), orderIndex, vocabulary: {...} }`
 * (quan hệ bảng nối). Hàm này "trải" `vocabulary` lên trên và giữ lại
 * `deckWordId` + `orderIndex` để component không phải đọc nested.
 * Nếu deck không có mảng `words` (hoặc đã phẳng sẵn) thì trả nguyên trạng.
 */
function normalizeDeck(deck) {
  if (!deck || !Array.isArray(deck.words)) return deck

  return {
    ...deck,
    words: deck.words.map(entry => {
      const vocabulary = entry?.vocabulary || entry
      return {
        ...vocabulary,
        id: vocabulary?.id ?? entry?.id,
        deckWordId: entry?.id,
        orderIndex: entry?.orderIndex
      }
    })
  }
}

/**
 * Service bộ từ vựng (deck): xem/CRUD deck và thêm từ vào deck.
 * Response trả phẳng (body trực tiếp) — KHÔNG bọc `.data`.
 * Riêng `getDeckById` đã đi qua `normalizeDeck` nên `words[]` phẳng như mô tả trên.
 */
export default {
  /**
   * Danh sách deck CÔNG KHAI (phân trang).
   * `GET /api/decks` — mặc định `{ page: 0, size: 9 }`, `params` truyền vào sẽ ghi đè.
   * Trả `{ content, totalPages, totalElements, ... }` (Page của Spring).
   */
  getPublicDecks: (params = {}) => api.get('/api/decks', { params: { page: 0, size: 9, ...params } }).then(r => r.data),
  /**
   * Danh sách deck CỦA TÔI. Cùng shape Page như `getPublicDecks`.
   * Lưu ý: nhóm `/api/decks/**` là `permitAll` ở SecurityConfig, nhưng endpoint
   * này tự kiểm tra principal và trả 401 khi chưa đăng nhập → vẫn phải có JWT.
   */
  getMyDecks: (params = {}) => api.get('/api/decks/my', { params: { page: 0, size: 9, ...params } }).then(r => r.data),
  /**
   * Chi tiết 1 deck theo id; `words[]` đã được `normalizeDeck` làm phẳng.
   * Là endpoint công khai: khách xem được deck public, còn deck riêng tư bị
   * backend lọc theo quyền của người gọi.
   */
  getDeckById: (id) => api.get(`/api/decks/${id}`).then(r => normalizeDeck(r.data)),
  /** Tạo deck mới từ `deckData` (name/description/...). Trả deck vừa tạo. */
  createDeck: (deckData) => api.post('/api/decks', deckData).then(r => r.data),
  /** Cập nhật deck theo id. Trả deck sau khi sửa. */
  updateDeck: (id, deckData) => api.put(`/api/decks/${id}`, deckData).then(r => r.data),
  /** Xoá deck theo id. Trả `{ message: "Deck deleted successfully" }`. */
  deleteDeck: (id) => api.delete(`/api/decks/${id}`).then(r => r.data),
  /**
   * Thêm 1 từ (theo `vocabId`) vào deck. Body `{ vocabId }`.
   * Trả `{ message: "Word added to deck successfully" }`; 400 nếu thiếu `vocabId`.
   */
  addWordToDeck: (deckId, vocabId) => api.post(`/api/decks/${deckId}/words`, { vocabId }).then(r => r.data)
}
