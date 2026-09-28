import api from './api'

/**
 * Service thống kê trang chủ/dashboard.
 * Response trả về dạng phẳng (body là object stats trực tiếp), KHÔNG bọc `.data`.
 */
export default {
  /**
   * Lấy số liệu tổng quan của người dùng hiện tại (cần JWT):
   * `GET /api/dashboard/stats` → object thống kê (điểm, streak, tiến độ...).
   */
  getStats: () => api.get('/api/dashboard/stats').then(r => r.data)
}
