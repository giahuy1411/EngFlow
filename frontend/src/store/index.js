/**
 * Barrel export của tầng Pinia.
 *
 * Pinia instance được tạo ở `main.js` (`createPinia()`); file này chỉ gom lại đúng ba
 * store còn sống để component import một chỗ. Từ audit-v5, exercise/progress stores đã
 * bị xoá vì không component nào import (dead code) — đừng thêm lại nếu chưa có nhu cầu.
 */
export { useAuthStore } from './modules/auth'
export { useLessonStore } from './modules/lesson'
// audit-v5: exercise/progress stores removed — zero component importers (dead).
export { usePremiumStore } from './modules/premium'
