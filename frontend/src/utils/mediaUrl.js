/**
 * Resolve a backend media URL against the API base.
 * audit-v6 F28: backend now returns relative paths (/api/v1/media/...) so
 * deploys behind another host/port keep working; absolute URLs (Cloudinary,
 * legacy rows) pass through untouched.
 */
export function resolveMediaUrl(url) {
  if (!url) return url
  if (url.startsWith('/')) {
    const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    return base.replace(/\/$/, '') + url
  }
  return url
}
