/**
 * Ghép URL media trả về từ backend với API base.
 * audit-v6 F28: backend giờ trả đường dẫn tương đối (/api/v1/media/...) để khi deploy
 * sau một host/port khác vẫn chạy được; các URL tuyệt đối (Cloudinary, dữ liệu cũ)
 * được giữ nguyên, không đụng tới.
 *
 * URL công khai của MinIO do biến môi trường quyết định, nên đừng hard-code host ở đây —
 * đường dẫn tương đối đi qua API base mới đúng cho cả môi trường dev lẫn deploy.
 *
 * @param {string} url - URL tuyệt đối hoặc đường dẫn tương đối bắt đầu bằng `/`
 * @returns {string} URL đầy đủ, hoặc nguyên giá trị đầu vào khi không cần ghép
 */
export function resolveMediaUrl(url) {
  if (!url) return url
  if (url.startsWith('/')) {
    const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    return base.replace(/\/$/, '') + url
  }
  return url
}
