/**
 * audit-v13 F-13-20: router chèn tham số `?redirect=` vào URL khi đá người dùng ra khỏi
 * trang được bảo vệ, nhưng trước đây không ai đọc tham số này — nên sau khi đăng nhập
 * người dùng không được đưa về chỗ họ xuất phát.
 *
 * Helper này quyết định sau đăng nhập được phép chuyển hướng đi đâu. Nó BẮT BUỘC chỉ được
 * trả về đường dẫn nội bộ: `route.query.redirect` thô là dữ liệu kẻ tấn công kiểm soát được,
 * đưa thẳng vào `router.replace()` chính là lỗ hổng open-redirect (ví dụ `//evil.com`,
 * `https://evil.com`, `javascript:...`).
 */

/** Đích mặc định khi không có redirect dùng được. */
export const DEFAULT_REDIRECT = '/lessons'

/**
 * Trả về `candidate` khi nó là đường dẫn nội bộ an toàn, ngược lại trả {@link DEFAULT_REDIRECT}.
 *
 * Các trường hợp bị từ chối:
 *   - bất cứ thứ gì không bắt đầu bằng đúng một dấu `/` (`http://…`, `javascript:…`, `evil.com`)
 *   - URL protocol-relative (`//evil.com`) và biến thể dùng dấu chéo ngược (`/\evil.com`, `\\evil.com`)
 *   - ký tự điều khiển và chiêu chèn khoảng trắng để đánh lừa
 *
 * @param {unknown} candidate giá trị lấy từ `route.query.redirect`
 * @returns {string} một đường dẫn nội bộ an toàn
 */
export function safeRedirect(candidate) {
  if (typeof candidate !== 'string') return DEFAULT_REDIRECT

  // Từ chối ký tự điều khiển hoặc khoảng trắng THÔ: trình duyệt có thể cắt chúng và làm đổi
  // đích đến (ví dụ "/speak\ting" -> "/speaking"). Kiểm trên bản gốc, không phải bản đã
  // decode — dấu cách được mã hoá (`%20`) trong query là hợp lệ.
  if (/[\u0000-\u001F\u007F\s]/.test(candidate)) return DEFAULT_REDIRECT

  // Decode đúng một lần để `%2F%2Fevil.com` không lọt qua dưới dạng URL protocol-relative.
  // Kiểm tra trên bản ĐÃ DECODE, nhưng trả về bản GỐC: trả bản đã decode sẽ làm hỏng một
  // đích đến hợp lệ có chứa `%` theo nghĩa đen (ví dụ `/search?q=100%25off` sẽ biến thành
  // `/search?q=100%off` rồi fail ngay ở bước decode của vue-router).
  let decoded = candidate
  try {
    decoded = decodeURIComponent(candidate)
  } catch {
    return DEFAULT_REDIRECT
  }

  // Phải là đường dẫn bắt đầu từ gốc...
  if (!decoded.startsWith('/')) return DEFAULT_REDIRECT
  // ...nhưng không được là protocol-relative (`//host`) hay chiêu dấu chéo ngược (`/\host`, `/\/host`).
  if (decoded.startsWith('//') || decoded.includes('\\')) return DEFAULT_REDIRECT

  return candidate
}
