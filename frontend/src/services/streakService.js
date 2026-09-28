import api from './api'

/**
 * Service chuỗi ngày học (streak).
 *
 * Lưu ý: streak được backend tính theo giờ VN (+07); các endpoint chỉ ĐỌC
 * trạng thái — chúng KHÔNG ghi `study_days` (việc ghi do nộp bài/checkin lo).
 * Response trả phẳng (body trực tiếp), KHÔNG bọc `.data`.
 */
export default {
  /**
   * Ảnh chụp nhanh trạng thái streak: `GET /api/streak/snapshot`.
   * Trả `StudySnapshot` (giờ VN): `{ today, currentStreak, studiedToday,
   * effectiveFrom, studiedDays[], legacyAccessDays[], legacyHistoryAvailable }`
   * — đủ dữ liệu để render StreakCalendar/header trong một lần gọi.
   */
  getSnapshot: () => api.get('/api/streak/snapshot').then(r => r.data),
  /**
   * Danh sách ngày ĐÃ HỌC trong `days` ngày gần nhất (mặc định 30).
   * `GET /api/streak/history?days=<days>` — `days` nhét thẳng vào query string.
   * Trả mảng chuỗi ngày ISO (`['2026-09-27', ...]`), tăng dần — KHÔNG phải object.
   */
  getHistory: (days = 30) => api.get(`/api/streak/history?days=${days}`).then(r => r.data),
  /** Streak hiện tại — `GET /api/streak/current` → `{ currentStreak, today }`.
   * `today` là ngày server (ISO `yyyy-MM-dd`, giờ VN) để lịch trong Profile đóng
   * khung đúng múi giờ backend thay vì suy từ đồng hồ máy khách. */
  getCurrentStreak: () => api.get('/api/streak/current').then(r => r.data)
}
