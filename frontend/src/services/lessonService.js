/**
 * lessonService — API bài học và bài tập gắn với bài học (`/api/lessons/**`).
 *
 * Dùng instance axios dùng chung (`./api`), trả `r.data`. JWT TTL 900s, không có
 * refresh-token — 401 thì phải đăng nhập lại.
 *
 * Gotcha hình dạng response: endpoint danh sách `/api/lessons` trả về kiểu Spring
 * `Page` (có `content`) nhưng có thể trả thẳng mảng — nên `getAll` phải chấp nhận cả
 * hai (xem chú thích trong hàm).
 */
import api from './api'

export default {
  /**
   * Lấy tối đa 100 bài học (dùng cho các dropdown/select cần đủ danh sách).
   *
   * Chấp nhận cả hai hình dạng response: mảng thuần, hoặc object phân trang có
   * `.content` (Spring Page). Nếu không khớp cả hai thì trả mảng rỗng.
   * @returns {Promise<Array<object>>} danh sách bài học
   */
  getAll: async () => {
    const data = await api.get('/api/lessons', { params: { page: 0, size: 100 } }).then(r => r.data)
    return Array.isArray(data) ? data : data.content || []
  },
  /**
   * Lấy một trang bài học theo tham số truyền vào.
   * @param {object} params - query (page, size, ...)
   * @returns {Promise<object>} trang bài học
   */
  getPage: (params = {}) => api.get('/api/lessons', { params }).then(r => r.data),
  /** Lấy bài học theo id (endpoint public — chỉ thấy bài đã publish). @param {number|string} id @returns {Promise<object>} */
  getById: (id) => api.get(`/api/lessons/${id}`).then(r => r.data),
  /** Tạo bài học. @param {object} lesson @returns {Promise<object>} */
  create: (lesson) => api.post('/api/lessons', lesson).then(r => r.data),
  /** Cập nhật bài học. @param {number|string} id @param {object} lesson @returns {Promise<object>} */
  update: (id, lesson) => api.put(`/api/lessons/${id}`, lesson).then(r => r.data),
  /** Xoá bài học. @param {number|string} id @returns {Promise<object>} */
  remove: (id) => api.delete(`/api/lessons/${id}`).then(r => r.data),
  // Exercises
  /** Bài tập của một lesson, KHÔNG kèm đáp án (an toàn cho client). @param {number|string} lessonId @returns {Promise<object>} */
  getExercises: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises`).then(r => r.data),
  /** Bài tập KÈM đáp án — chỉ dùng ở màn hình admin/sửa bài, đừng gọi ở luồng học viên. @param {number|string} lessonId @returns {Promise<object>} */
  getExercisesWithAnswers: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises?includeAnswers=true`).then(r => r.data),
  /** Chấm thử một lượt trả lời (không lưu attempt). @param {number|string} lessonId @param {object} answers @returns {Promise<object>} */
  gradeExercises: (lessonId, answers) => api.post(`/api/lessons/${lessonId}/exercises/grade`, { answers }).then(r => r.data),
  /** Nộp bài và LƯU attempt. @param {number|string} lessonId @param {object} answers @returns {Promise<object>} */
  submitExercises: (lessonId, answers) => api.post(`/api/lessons/${lessonId}/exercises/submit`, { answers }).then(r => r.data),
  /** Lịch sử các lượt làm bài của lesson. @param {number|string} lessonId @returns {Promise<object>} */
  getAttempts: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises/attempts`).then(r => r.data),
  /** Chi tiết một lượt làm bài. @param {number|string} lessonId @param {number|string} attemptId @returns {Promise<object>} */
  getAttemptDetail: (lessonId, attemptId) => api.get(`/api/lessons/${lessonId}/exercises/attempts/${attemptId}`).then(r => r.data),
  // Clean content (answers stripped, per-exercise HTML fragments)
  /** Nội dung bài tập đã lọc đáp án, trả về từng mảnh HTML theo exercise. @param {number|string} lessonId @returns {Promise<object>} */
  getCleanContent: (lessonId) => api.get(`/api/lessons/${lessonId}/exercises/content`).then(r => r.data),
}
