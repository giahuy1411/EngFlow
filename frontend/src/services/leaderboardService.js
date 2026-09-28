import api from './api'

/**
 * Service bảng xếp hạng.
 * Response trả phẳng: `{ content: [...], totalPages, totalElements, ... }` (Page của Spring).
 */
export default {
  /**
   * Lấy bảng xếp hạng (phân trang) — `GET /api/leaderboard`.
   *
   * Hỗ trợ 2 cách gọi để tương thích code cũ:
   * - `getLeaderboard(20)` → số = size, page mặc định 0.
   * - `getLeaderboard({ page, size })` → truyền thẳng params, mặc định `{ page: 0, size: 20 }`.
   */
  getLeaderboard: (params = {}) => {
    if (typeof params === 'number') {
      return api.get('/api/leaderboard', { params: { page: 0, size: params } }).then(r => r.data)
    }
    return api.get('/api/leaderboard', { params: { page: 0, size: 20, ...params } }).then(r => r.data)
  }
}