/**
 * aiExerciseService — sinh bài tập bằng AI cho khu vực admin (`/api/admin/exercises/ai/**`).
 *
 * Tất cả hàm đi qua instance axios dùng chung (`./api`) và trả `r.data` (payload đã
 * bóc vỏ axios). JWT TTL 900s, không có refresh-token — hết hạn thì phải đăng nhập lại.
 *
 * Lưu ý về độ trễ: các đường AI chạy trên Ollama local nên chậm thất thường, có lúc
 * timeout. Vì vậy có hai chế độ sinh:
 *   - đồng bộ  (`generateExercises`): chờ ngay trong request, dễ timeout với lesson dài;
 *   - bất đồng bộ (`generateExercisesAsync`): backend trả 202 + `batchId`, client poll
 *     `getStatus(batchId)` để theo dõi tiến độ — nên dùng cho khối lượng lớn.
 */
import api from './api'

export default {
  /**
   * Sinh bài tập đồng bộ (chờ trong một request).
   * @param {number|string} lessonId
   * @param {string} exerciseType - loại bài tập backend hỗ trợ
   * @param {number} count - số lượng cần sinh
   * @returns {Promise<object>} danh sách bài tập đã sinh
   */
  generateExercises: (lessonId, exerciseType, count) =>
    api.post('/api/admin/exercises/ai/generate', { lessonId, exerciseType, count }).then(r => r.data),

  // Option 2 — async generation (202 + batchId)
  /**
   * Sinh bài tập bất đồng bộ. Backend trả 202 kèm `batchId`; theo dõi bằng `getStatus`.
   * @param {number|string} lessonId
   * @param {string} exerciseType
   * @param {number} count
   * @returns {Promise<object>} chứa batchId
   */
  generateExercisesAsync: (lessonId, exerciseType, count) =>
    api.post('/api/admin/exercises/ai/generate-async', { lessonId, exerciseType, count }).then(r => r.data),

  /**
   * Sinh đủ mọi loại bài tập cho một lesson.
   * @param {number|string} lessonId
   * @param {number} count - số lượng mỗi loại
   * @returns {Promise<object>}
   */
  generateAll: (lessonId, count) =>
    api.post('/api/admin/exercises/ai/generate-all', { lessonId, count }).then(r => r.data),

  /**
   * Sinh hàng loạt cho nhiều lesson (job nền phía server).
   * @param {boolean} force - true để sinh lại cả lesson đã có bài tập
   * @returns {Promise<object>}
   */
  generateBatch: (force = false) =>
    api.post(`/api/admin/exercises/ai/generate-batch?force=${force}`).then(r => r.data),

  /**
   * Kiểm tra chất lượng một tập bài tập (có thể nhờ AI review thêm).
   * @param {Array<object>} exercises
   * @param {boolean} useAiReview - bật lớp review bằng AI (chậm hơn)
   * @returns {Promise<object>} kết quả validate
   */
  validateExercises: (exercises, useAiReview = false) =>
    api.post('/api/admin/exercises/ai/validate', { exercises, useAiReview }).then(r => r.data),

  /**
   * Trạng thái các batch gần đây (không truyền batchId).
   * @returns {Promise<object>}
   */
  getBatchStatus: () =>
    api.get('/api/admin/exercises/ai/status').then(r => r.data),

  // Option 2 — poll progress per batchId
  /**
   * Poll tiến độ một batch sinh bài tập.
   * @param {string} batchId - id nhận từ `generateExercisesAsync`
   * @returns {Promise<object>} tiến độ + trạng thái batch
   */
  getStatus: (batchId) =>
    api.get(`/api/admin/exercises/ai/status?batchId=${batchId}`).then(r => r.data),
}
