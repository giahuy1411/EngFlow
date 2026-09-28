/**
 * videoLessonService — API bài học video và lượt nộp (attempts) của học viên/admin.
 *
 * Dùng instance axios dùng chung (`./api`), trả `response.data`. JWT TTL 900s, không
 * có refresh-token — 401 thì phải đăng nhập lại.
 *
 * Các route AI (chấm điểm, dịch phụ đề, lấy transcript YouTube) gọi Whisper/LLM local
 * nên chạy đồng bộ và rất chậm; timeout được nới tương ứng (xem từng hàm).
 */
import api from './api'

/**
 * Liệt kê bài học video đã publish (endpoint public, có phân trang + lọc level).
 * @param {number} page - trang (0-based)
 * @param {number} size - số phần tử mỗi trang
 * @param {string} level - lọc theo level; chuỗi rỗng = không lọc
 * @returns {Promise<object>} trang bài học video
 */
const list = (page = 0, size = 12, level = '') => {
  const params = { page, size }
  if (level) params.level = level
  return api.get('/api/v1/video-lessons', { params }).then(response => response.data)
}

export default {
  list,
  /**
   * Đọc một bài học video theo id.
   *
   * Dùng endpoint PUBLIC `/api/v1/video-lessons/{id}` kể cả từ màn admin: admin KHÔNG
   * có route GET-by-id riêng cho video lesson, nên đây là cách duy nhất để đọc chi tiết.
   * @param {number|string} id
   * @returns {Promise<object>}
   */
  getById: id => api.get(`/api/v1/video-lessons/${id}`).then(response => response.data),
  /**
   * Nộp một lượt luyện nói cho một dòng phụ đề (multipart: lineIndex + file).
   * @param {number|string} id - id bài học video
   * @param {number} lineIndex - chỉ số dòng phụ đề đang luyện
   * @param {File} file - file audio/video người dùng ghi
   * @returns {Promise<object>} attempt vừa tạo (timeout 120s vì phải upload media)
   */
  submitAttempt(id, lineIndex, file) {
    const form = new FormData()
    form.append('lineIndex', lineIndex)
    form.append('file', file)
    return api.post(`/api/v1/video-lessons/${id}/attempts`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }).then(response => response.data)
  },
  /**
   * Lịch sử lượt nộp của chính người dùng hiện tại.
   * @param {number} page
   * @param {number} size
   * @returns {Promise<object>}
   */
  myAttempts(page = 0, size = 50) {
    return api.get('/api/v1/video-attempts', { params: { page, size } }).then(response => response.data)
  },
  // Admin
  /** Danh sách bài học video cho admin (gồm cả chưa publish). @param {number} page @param {number} size @returns {Promise<object>} */
  getAllAdmin: (page = 0, size = 50) =>
    api.get('/api/v1/admin/video-lessons', { params: { page, size } }).then(response => response.data),
  /** Tạo bài học video từ metadata JSON (không kèm file). @param {object} data @returns {Promise<object>} */
  create: data => api.post('/api/v1/admin/video-lessons', data).then(response => response.data),
  /**
   * Tạo bài học video kèm transcript dạng text (multipart).
   *
   * `meta` phải được đóng gói thành Blob JSON với `type: 'application/json'` để Spring
   * bind đúng part `meta`; nếu không, server trả 415/400.
   * @param {object} meta - metadata bài học (title, level, url, ...)
   * @param {string} transcriptText - transcript thô, mỗi dòng một cue
   * @returns {Promise<object>}
   */
  createWithTranscript(meta, transcriptText) {
    const form = new FormData()
    form.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
    if (transcriptText) form.append('transcriptText', transcriptText)
    return api.post('/api/v1/admin/video-lessons/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    }).then(response => response.data)
  },
  /** Cập nhật bài học video. @param {number|string} id @param {object} data @returns {Promise<object>} */
  update: (id, data) => api.put(`/api/v1/admin/video-lessons/${id}`, data).then(response => response.data),
  /** Xoá bài học video. @param {number|string} id @returns {Promise<object>} */
  remove: id => api.delete(`/api/v1/admin/video-lessons/${id}`).then(response => response.data),
  /**
   * Danh sách lượt nộp cho admin, lọc theo trạng thái chấm.
   * @param {string} status - trạng thái (chuỗi rỗng = tất cả)
   * @param {number} page
   * @param {number} size
   * @returns {Promise<object>}
   */
  getAdminAttempts(status = '', page = 0, size = 20) {
    const params = { page, size }
    if (status) params.status = status
    return api.get('/api/v1/admin/video-attempts', { params }).then(response => response.data)
  },
  /**
   * Chấm thủ công một lượt nộp.
   * @param {number|string} id - id attempt
   * @param {number} score - điểm
   * @param {string} feedback - nhận xét
   * @returns {Promise<object>}
   */
  gradeAttempt(id, score, feedback) {
    return api.patch(`/api/v1/admin/video-attempts/${id}/grade`, { score, feedback })
      .then(response => response.data)
  },
  // AI grading is synchronous (Whisper + LLM); allow up to 3 minutes.
  /**
   * Chấm bằng AI (Whisper + LLM) — chạy ĐỒNG BỘ, cho tới 3 phút. Body để null vì
   * không có tham số; nếu UI cho phép bấm nhiều lần, phải tự khoá nút để tránh
   * chồng request lên hàng đợi AI local.
   * @param {number|string} id - id attempt
   * @returns {Promise<object>}
   */
  aiGradeAttempt(id) {
    return api.post(`/api/v1/admin/video-attempts/${id}/ai-grade`, null, { timeout: 180000 })
      .then(response => response.data)
  },
  // AI subtitle translation (fail-soft); returns lines with textVi filled.
  /**
   * Dịch phụ đề sang tiếng Việt (fail-soft: lỗi AI không làm hỏng luồng lưu).
   * @param {Array<object>} lines - các dòng phụ đề
   * @returns {Promise<object>} các dòng đã điền `textVi`
   */
  translateTranscript(lines) {
    return api.post('/api/v1/admin/video-lessons/translate-transcript', lines, { timeout: 180000 })
      .then(response => response.data)
  },
  // Fetches an existing YouTube transcript (auto or human captions) and has
  // the local LLM translate every cue to Vietnamese; also returns title and
  // description so the form can be pre-filled. Long local-LLM call → 3 min.
  /**
   * Lấy transcript có sẵn của YouTube (caption tự động hoặc do người tạo), rồi cho
   * LLM local dịch từng cue sang tiếng Việt; đồng thời trả title/description để
   * pre-fill form. Gọi LLM local lâu → timeout 3 phút.
   * @param {string} url - URL video YouTube
   * @returns {Promise<object>}
   */
  fetchYoutubeTranscript(url) {
    return api.post('/api/v1/admin/video-lessons/fetch-youtube', { url }, { timeout: 180000 })
      .then(response => response.data)
  }
}
