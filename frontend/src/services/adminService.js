/**
 * adminService — toàn bộ endpoint `/api/admin/**` dùng ở khu vực quản trị.
 *
 * Mọi hàm đi qua instance axios dùng chung (`./api`): nó tự gắn Bearer token và
 * xử lý 401. Token JWT có TTL 900s và KHÔNG có endpoint refresh-token, nên khi hết
 * hạn người dùng phải đăng nhập lại — đừng cố tự refresh ở tầng service.
 *
 * Các hàm ở đây trả về `response.data` (payload đã bóc vỏ axios), KHÔNG phải
 * `response`. Riêng delete không trả gì. Backend cũng bọc thêm một lớp `data` cho
 * một số endpoint, nên nơi gọi phải kiểm tra hình dạng thực tế trước khi đọc field.
 *
 * Bảo vệ phía server: mọi route dưới đây yêu cầu role ADMIN; guard `requiresAdmin`
 * ở router chỉ là lớp UX, không thay thế kiểm tra quyền phía backend.
 */
import api from './api';

export const adminService = {
  // Stats
  /** Thống kê tổng quan cho dashboard admin. @returns {Promise<object>} */
  getDashboardStats: async () => {
    const response = await api.get('/api/admin/stats');
    return response.data;
  },

  // Users
  /**
   * Danh sách người dùng (có phân trang/lọc qua `params`).
   * @param {object} params - tham số query (page, size, keyword, ...)
   * @returns {Promise<object>} trang người dùng
   */
  getAllUsers: async (params = {}) => {
    const response = await api.get('/api/admin/users', { params });
    return response.data;
  },
  /** Bật/tắt trạng thái hoạt động của user. @param {number|string} id @returns {Promise<object>} */
  toggleUserActive: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-active`);
    return response.data;
  },
  /** Cấp/thu quyền admin của user. @param {number|string} id @returns {Promise<object>} */
  toggleUserAdmin: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-admin`);
    return response.data;
  },
  /** Bật/tắt premium thủ công cho user. @param {number|string} id @returns {Promise<object>} */
  toggleUserPremium: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-premium`);
    return response.data;
  },
  /** Thu hồi premium của user (khác toggle: luôn đưa về trạng thái không premium). @param {number|string} id @returns {Promise<object>} */
  revokeUserPremium: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/revoke-premium`);
    return response.data;
  },

  // Lessons
  /** Danh sách bài học cho admin (gồm cả bản nháp — khác endpoint public). @param {object} params @returns {Promise<object>} */
  getAllLessons: async (params = {}) => {
    const response = await api.get('/api/admin/lessons', { params });
    return response.data;
  },
  /** Lấy một bài học theo id (bản admin, thấy được cả nháp). @param {number|string} id @returns {Promise<object>} */
  getLesson: async (id) => {
    const response = await api.get(`/api/admin/lessons/${id}`);
    return response.data;
  },
  /** Tạo bài học mới. @param {object} lesson @returns {Promise<object>} bài học đã tạo */
  createLesson: async (lesson) => {
    const response = await api.post('/api/admin/lessons', lesson);
    return response.data;
  },
  /** Cập nhật bài học. @param {number|string} id @param {object} lesson @returns {Promise<object>} */
  updateLesson: async (id, lesson) => {
    const response = await api.put(`/api/admin/lessons/${id}`, lesson);
    return response.data;
  },
  /** Xoá bài học; không trả dữ liệu. @param {number|string} id @returns {Promise<void>} */
  deleteLesson: async (id) => {
    await api.delete(`/api/admin/lessons/${id}`);
  },
  /** Đổi trạng thái xuất bản (publish/draft) của bài học. @param {number|string} id @returns {Promise<object>} */
  toggleLessonPublish: async (id) => {
    const response = await api.put(`/api/admin/lessons/${id}/toggle-publish`);
    return response.data;
  },

  // Vocabulary
  /** Danh sách từ vựng cho admin. @param {object} params @returns {Promise<object>} */
  getAllVocabulary: async (params = {}) => {
    const response = await api.get('/api/admin/vocabulary', { params });
    return response.data;
  },
  /** Thêm một mục từ vựng. @param {object} vocabulary @returns {Promise<object>} */
  createVocabulary: async (vocabulary) => {
    const response = await api.post('/api/admin/vocabulary', vocabulary);
    return response.data;
  },
  /** Cập nhật từ vựng. @param {number|string} id @param {object} vocabulary @returns {Promise<object>} */
  updateVocabulary: async (id, vocabulary) => {
    const response = await api.put(`/api/admin/vocabulary/${id}`, vocabulary);
    return response.data;
  },
  /** Xoá từ vựng; không trả dữ liệu. @param {number|string} id @returns {Promise<void>} */
  deleteVocabulary: async (id) => {
    await api.delete(`/api/admin/vocabulary/${id}`);
  },

  // Exercises
  /** Danh sách bài tập cho admin. @param {object} params @returns {Promise<object>} */
  getAllExercises: async (params = {}) => {
    const response = await api.get('/api/admin/exercises', { params });
    return response.data;
  },
  /** Lấy một bài tập theo id. @param {number|string} id @returns {Promise<object>} */
  getExercise: async (id) => {
    const response = await api.get(`/api/admin/exercises/${id}`);
    return response.data;
  },
  /** Tạo bài tập mới. @param {object} exercise @returns {Promise<object>} */
  createExercise: async (exercise) => {
    const response = await api.post('/api/admin/exercises', exercise);
    return response.data;
  },
  /** Cập nhật bài tập. @param {number|string} id @param {object} exercise @returns {Promise<object>} */
  updateExercise: async (id, exercise) => {
    const response = await api.put(`/api/admin/exercises/${id}`, exercise);
    return response.data;
  },
  /** Xoá bài tập; không trả dữ liệu. @param {number|string} id @returns {Promise<void>} */
  deleteExercise: async (id) => {
    await api.delete(`/api/admin/exercises/${id}`);
  }
};
