/*
  audit-v11 F132 — hai hàm, vì CÙNG một sắc độ (hue) không thể gánh cả hai vai trò.

  `levelColor()` trả về sắc độ rực và dùng cho PHẦN TÔ (fill): thanh tiến độ, chấm tròn,
  nét SVG. Những chỗ đó đúng — fill rực là trang trí, không phải chữ.

  `levelInkColor()` trả về sắc độ tối đạt chuẩn AA và dùng cho CHỮ. Đo trên nền trắng:
  accent 4.23:1, tertiary 1.67:1, secondary 2.65:1, quaternary 1.92:1 — đều dưới mức
  4.5:1 mà WCAG 1.4.3 yêu cầu cho body text. Các biến thể ink đo được 7.10 / 5.02 / 6.04 / 5.48:1.

  Trước đây cả hai call site đều truyền CÙNG một giá trị cho `background` LẪN `color`, nên
  nếu chỉ sửa một hàm thì mới sửa được một nửa.
*/

/**
 * Sắc độ rực theo level bài học — CHỈ dùng cho fill/trang trí, không dùng cho chữ.
 *
 * Đây là bảng ánh xạ level của backend sang biến CSS. Thêm một level mới vào enum backend
 * mà quên cập nhật map này thì hàm rơi vào nhánh mặc định, badge sẽ ăn màu `--geo-muted-fg`
 * và trông như bị trống. Có test canh chừng ở `audit-v17-regressions.test.js`, test này CẤM
 * việc đưa các giá trị hex cũ trở lại.
 *
 * @param {string} level - giá trị level từ backend (ELEMENTARY, PRE_INTERMEDIATE, ...)
 * @returns {string} biến CSS kèm hex fallback, dùng cho phần tô màu
 */
export function levelColor(level) {
  return ({
    ELEMENTARY: 'var(--geo-accent, #8B5CF6)',
    PRE_INTERMEDIATE: 'var(--geo-tertiary, #FBBF24)',
    INTERMEDIATE: 'var(--geo-secondary, #F472B6)',
    UPPER_INTERMEDIATE: 'var(--geo-quaternary, #34D399)',
  })[level] || 'var(--geo-muted-fg, #556070)'
}

/**
 * Biến thể an toàn cho chữ của {@link levelColor} — dùng ở mọi nơi sắc độ mang chữ.
 *
 * Cùng bảng ánh xạ nhưng trỏ tới các biến `-ink` đã được đo đạt 4.5:1 trên nền trắng.
 * Nhầm hàm ở đây (lấy màu rực đem làm màu chữ) là lỗi tương phản WCAG 1.4.3.
 *
 * @param {string} level - giá trị level từ backend
 * @returns {string} biến CSS `-ink` kèm hex fallback, dùng cho chữ
 */
export function levelInkColor(level) {
  return ({
    ELEMENTARY: 'var(--geo-accent-ink, #6D28D9)',
    PRE_INTERMEDIATE: 'var(--geo-tertiary-ink, #B45309)',
    INTERMEDIATE: 'var(--geo-secondary-ink, #BE185D)',
    UPPER_INTERMEDIATE: 'var(--geo-quaternary-ink, #047857)',
  })[level] || 'var(--geo-muted-fg, #556070)'
}

/**
 * Nhãn hiển thị của level. Không có trong bảng thì trả lại chính chuỗi level thô
 * (thà hiện `SOME_NEW_LEVEL` còn hơn hiện rỗng).
 *
 * @param {string} level - giá trị level từ backend
 * @returns {string} nhãn để hiển thị
 */
export function levelLabel(level) {
  return ({
    ELEMENTARY: 'Elementary',
    PRE_INTERMEDIATE: 'Pre-Intermediate',
    INTERMEDIATE: 'Intermediate',
    UPPER_INTERMEDIATE: 'Upper-Intermediate',
  })[level] || level
}
