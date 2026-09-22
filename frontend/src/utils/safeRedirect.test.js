import { describe, it, expect } from 'vitest'
import { safeRedirect, DEFAULT_REDIRECT } from '@/utils/safeRedirect'

// Regression test for audit-v13 F-13-20.
//
// `route.query.redirect` is attacker-controllable. Feeding it straight to router.replace()
// would be an open redirect, so every hostile shape below must fall back to the default.
describe('safeRedirect — F-13-20 open-redirect guard', () => {
  describe('accepts real internal paths', () => {
    it.each([
      '/speaking',
      '/videos/1',
      '/decks/10006?tab=quiz',
      '/admin/dashboard',
      '/lessons/445#exercises',
    ])('keeps %s', (path) => {
      expect(safeRedirect(path)).toBe(path)
    })

    // Review finding: returning the DECODED value corrupted a literal `%` in a query.
    // The helper must validate the decoded form but hand back the original string.
    it.each([
      '/search?q=100%25off',
      '/search?q=50%25',
      '/lessons?q=a%20b',
    ])('returns %s unchanged (not decoded)', (path) => {
      expect(safeRedirect(path)).toBe(path)
    })
  })

  describe('rejects anything that could leave the site', () => {
    it.each([
      ['protocol-relative', '//evil.com'],
      ['absolute http', 'http://evil.com'],
      ['absolute https', 'https://evil.com'],
      ['javascript scheme', 'javascript:alert(1)'],
      ['data scheme', 'data:text/html,<script>alert(1)</script>'],
      ['backslash trick', '/\\evil.com'],
      ['double backslash', '\\\\evil.com'],
      ['bare host', 'evil.com'],
      ['encoded protocol-relative', '%2F%2Fevil.com'],
      ['leading whitespace', ' /speaking'],
      ['newline injection', '/speaking\n/evil'],
      ['tab injection', '/speak\ting'],
      ['null byte', '/speak\u0000ing'],
    ])('rejects %s', (_label, value) => {
      expect(safeRedirect(value)).toBe(DEFAULT_REDIRECT)
    })
  })

  describe('handles absent / malformed input', () => {
    it.each([
      ['undefined', undefined],
      ['null', null],
      ['empty string', ''],
      ['number', 42],
      ['array', ['/speaking']],
      ['object', { path: '/speaking' }],
    ])('falls back for %s', (_label, value) => {
      expect(safeRedirect(value)).toBe(DEFAULT_REDIRECT)
    })
  })
})
