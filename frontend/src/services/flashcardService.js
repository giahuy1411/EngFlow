/**
 * flashcardService — điểm chạm API của chế độ luyện flashcard.
 *
 * Dùng instance axios dùng chung (`./api`), trả `r.data`. JWT TTL 900s, không có
 * refresh-token — 401 nghĩa là phải đăng nhập lại.
 */
import api from './api'

export default {
  // audit-v12 F153: the flashcard drill is a plain back/continue reader now — it no longer
  // rates the word, so it no longer sends an SM-2 quality. It still counts as a study day
  // (for the streak), recorded once per session on the first "continue".
  /**
   * Ghi nhận một ngày học (phục vụ streak). Chỉ gọi MỘT lần mỗi phiên, ở lần bấm
   * "continue" đầu tiên — không còn gửi chất lượng SM-2 vì drill giờ chỉ là đọc
   * lật mặt/chuyển tiếp (audit-v12 F153).
   * @returns {Promise<object>}
   */
  recordStudy: () => api.post('/api/flashcards/study').then(r => r.data),

  // Kept for the still-registered POST /api/flashcards/review. No UI calls it since F153;
  // it is removed together with SrsService in the Phase 12 cleanup.
  /**
   * Gửi đánh giá SM-2 cho một từ. Giữ lại vì route `POST /api/flashcards/review` vẫn
   * được đăng ký, nhưng từ F153 không UI nào gọi; sẽ xoá cùng SrsService ở đợt dọn Phase 12.
   * @param {number|string} vocabularyId
   * @param {number} quality - điểm chất lượng SM-2
   * @returns {Promise<object>}
   */
  reviewFlashcard: (vocabularyId, quality) => api.post('/api/flashcards/review', { vocabularyId, quality }).then(r => r.data),
  /**
   * Trạng thái SRS hiện tại của một từ.
   * @param {number|string} vocabularyId
   * @returns {Promise<object>}
   */
  getStatus: (vocabularyId) => api.get(`/api/flashcards/status/${vocabularyId}`).then(r => r.data)
}
