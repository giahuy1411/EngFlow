import api from './api'

/**
 * Hàm nội bộ dùng chung cho cả 2 "không gian" prompt: public và admin.
 * Chỉ khác tiền tố URL (`/api/v1` vs `/api/v1/admin`) nên gộp một chỗ.
 * Trả `{ content, totalPages, totalElements, ... }` (Page của Spring).
 */
const fetchPrompts = (page = 0, size = 100, admin = false, q = '') => {
  const prefix = admin ? '/api/v1/admin' : '/api/v1'
  const params = { page, size }
  if (q) params.q = q
  return api.get(`${prefix}/speaking-prompts`, { params })
    .then(response => response.data)
}

const createPrompt = data => api.post('/api/v1/admin/speaking-prompts', data).then(response => response.data)
const updatePrompt = (id, data) => api.put(`/api/v1/admin/speaking-prompts/${id}`, data).then(response => response.data)
const deletePrompt = id => api.delete(`/api/v1/admin/speaking-prompts/${id}`).then(response => response.data)

/**
 * Service luyện nói: prompt (đề bài) + submission (bản ghi) + chấm điểm.
 *
 * Hình dạng response: trả phẳng (body trực tiếp), KHÔNG bọc `.data`.
 * - Danh sách prompt/submission: Page của Spring (`content`, `totalPages`...).
 * - `uploadSubmission`/`assessSubmission`/`getSubmission`: object submission.
 *
 * Tên hàm có bí danh (`getAll` = `getPrompts`, `create` = `createPrompt`...) để
 * tương thích code cũ — chúng trỏ về CÙNG một endpoint, đừng hiểu là 2 API khác.
 */
export default {
  /** Prompt công khai (học viên). Phân trang + tìm kiếm `q` tuỳ chọn. */
  getPrompts(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, false, q)
  },
  /** Prompt phía admin (cần quyền admin). Cùng shape Page. */
  getAdminPrompts(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, true, q)
  },
  /** Bí danh của `getPrompts`. */
  getAll(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, false, q)
  },
  /** Bí danh của `getAdminPrompts`. */
  getAllAdmin(page = 0, size = 100, q = '') {
    return fetchPrompts(page, size, true, q)
  },
  /** Chi tiết 1 prompt theo id. */
  getById(id) {
    return api.get(`/api/v1/speaking-prompts/${id}`).then(response => response.data)
  },
  createPrompt,
  updatePrompt,
  deletePrompt,
  /** Bí danh CRUD (tương thích code cũ). */
  create: createPrompt,
  update: updatePrompt,
  delete: deletePrompt,
  /**
   * Nộp bản ghi (audio/video) cho 1 prompt.
   * Multipart gồm `promptId` + `file`. Timeout riêng 120 s vì upload media lâu.
   * Backend lưu file lên MinIO rồi trả object submission (có `id`, `mediaUrl`).
   */
  uploadSubmission(promptId, file) {
    const form = new FormData()
    form.append('promptId', promptId)
    form.append('file', file)
    return api.post('/api/v1/speaking-submissions/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }).then(response => response.data)
  },
  /** Bản ghi của chính người dùng hiện tại (phân trang). */
  getSubmissions(page = 0, size = 100) {
    return api.get('/api/v1/speaking-submissions', { params: { page, size } })
      .then(response => response.data)
  },
  /** Các bản ghi thuộc 1 prompt (phân trang). */
  getPromptSubmissions(promptId, page = 0, size = 20) {
    return api.get(`/api/v1/speaking-prompts/${promptId}/submissions`, { params: { page, size } })
      .then(response => response.data)
  },
  /** Danh sách bản ghi phía admin; lọc theo `status` nếu truyền (vd 'PENDING'). */
  getAdminSubmissions(page = 0, size = 100, status = '') {
    const params = { page, size }
    if (status) params.status = status
    return api.get('/api/v1/admin/speaking-submissions', { params })
      .then(response => response.data)
  },
  /** Chi tiết 1 bản ghi (kèm kết quả chấm nếu có). */
  getSubmission(id) {
    return api.get(`/api/v1/speaking-submissions/${id}`).then(response => response.data)
  },
  /**
   * Kích hoạt chấm điểm tự động (Whisper sidecar + rubric Ollama).
   * Timeout riêng 180 s vì chờ transcribe + LLM. Trả object submission đã có điểm.
   */
  assessSubmission(id) {
    return api.post(`/api/v1/speaking-submissions/${id}/assess`, null, { timeout: 180000 })
      .then(response => response.data)
  },
  /** Admin chấm tay: `PATCH` với `data` (điểm/nhận xét). */
  gradeSubmission(id, data) {
    return api.patch(`/api/v1/admin/speaking-submissions/${id}/grade`, data)
      .then(response => response.data)
  }
}
