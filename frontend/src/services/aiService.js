/**
 * aiService — sinh từ vựng bằng AI (`/api/ai/**`).
 *
 * Dùng instance axios dùng chung (`./api`), trả `r.data`. JWT TTL 900s, không có
 * refresh-token. Các endpoint dưới đây bị giới hạn quota (mỗi lần gọi tốn một credit)
 * và backend có CJK guard: từ tiếng Trung/Nhật/Hàn bị từ chối.
 *
 * Độ trễ: AI chạy trên Ollama local nên chậm thất thường, vì vậy mỗi hàm đặt timeout
 * riêng — đừng hạ xuống thấp hơn giá trị hiện có.
 */
import api from './api'

export default {
  /**
   * Sinh danh sách từ vựng theo chủ đề. Tốn quota; timeout 120s vì AI local chậm.
   * @param {string} topic - chủ đề
   * @param {string} level - trình độ CEFR (mặc định 'B2')
   * @param {number} count - số từ cần sinh (mặc định 10)
   * @returns {Promise<object>} danh sách từ đã sinh (CHƯA lưu vào DB)
   */
  generateVocab: (topic, level = 'B2', count = 10) =>
    api.post('/api/ai/generate-vocab', { topic, level, count }, { timeout: 120000 }).then(r => r.data),
  /**
   * Làm giàu một từ đơn (nghĩa, phiên âm, ví dụ...). Timeout 60s.
   * @param {string} word
   * @returns {Promise<object>} dữ liệu chi tiết của từ
   */
  enrichWord: (word) =>
    api.post('/api/ai/enrich-word', { word }, { timeout: 60000 }).then(r => r.data),
  /**
   * audit-v11 F145: `deckId` is optional but strongly recommended. Without it the words are
   * written to the global `vocabulary` table with no deck and no owner, so the user pays a
   * quota credit, sees "saved!", and can never reach them again. With it, the backend adds
   * each word to that deck (ownership-checked) so they land somewhere the user can open.
   *
   * Lưu danh sách từ đã sinh vào DB. `deckId` là tuỳ chọn nhưng RẤT NÊN truyền: không có
   * nó thì từ rơi vào bảng `vocabulary` toàn cục, không deck, không chủ — người dùng mất
   * một credit quota, thấy báo "đã lưu!", rồi không bao giờ mở lại được. Có `deckId` thì
   * backend thêm từng từ vào deck đó (có kiểm tra sở hữu) nên từ nằm ở chỗ mở được.
   *
   * @param {Array<object>} words - kết quả từ `generateVocab`
   * @param {number|string|null} deckId - deck đích; null = lưu kiểu toàn cục (không nên)
   * @returns {Promise<object>}
   */
  saveVocab: (words, deckId = null) =>
    api.post('/api/ai/save-vocab', words, {
      params: deckId ? { deckId } : {},
      timeout: 30000,
    }).then(r => r.data)
}
