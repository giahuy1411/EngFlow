/**
 * audit-v13 F-13-20: the router puts a `?redirect=` parameter in the URL when it bounces a
 * user off a protected page, but nothing ever read it — so after logging in the user was not
 * returned to where they came from.
 *
 * This helper decides where a post-login redirect may go. It MUST only ever return an
 * internal path: a raw `route.query.redirect` is attacker-controllable, and passing it
 * straight to `router.replace()` is an open-redirect (e.g. `//evil.com`, `https://evil.com`,
 * `javascript:...`).
 */

/** Where to send a user when no usable redirect was supplied. */
export const DEFAULT_REDIRECT = '/lessons'

/**
 * Returns `candidate` when it is a safe internal path, otherwise {@link DEFAULT_REDIRECT}.
 *
 * Rejected:
 *   - anything not starting with a single `/` (`http://…`, `javascript:…`, `evil.com`)
 *   - protocol-relative URLs (`//evil.com`) and backslash variants (`/\evil.com`, `\\evil.com`)
 *   - control characters and whitespace-padded tricks
 *
 * @param {unknown} candidate value from `route.query.redirect`
 * @returns {string} a safe internal path
 */
export function safeRedirect(candidate) {
  if (typeof candidate !== 'string') return DEFAULT_REDIRECT

  // Reject RAW control characters or whitespace: a browser may strip them and change the
  // target (e.g. "/speak\ting" -> "/speaking"). Checked on the original, not the decoded
  // form — an encoded space (`%20`) in a query is legitimate.
  if (/[\u0000-\u001F\u007F\s]/.test(candidate)) return DEFAULT_REDIRECT

  // Decode once so `%2F%2Fevil.com` cannot slip through as a protocol-relative URL.
  // Validate the DECODED form, but return the ORIGINAL: returning the decoded value would
  // corrupt a legitimate destination that contains a literal `%` (e.g. `/search?q=100%25off`
  // would come back as `/search?q=100%off` and then fail vue-router's own decoding).
  let decoded = candidate
  try {
    decoded = decodeURIComponent(candidate)
  } catch {
    return DEFAULT_REDIRECT
  }

  // Must be a rooted path...
  if (!decoded.startsWith('/')) return DEFAULT_REDIRECT
  // ...but not protocol-relative (`//host`) or a backslash trick (`/\host`, `/\/host`).
  if (decoded.startsWith('//') || decoded.includes('\\')) return DEFAULT_REDIRECT

  return candidate
}
