import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * audit-v12 F149 regression guard.
 *
 * `text-muted-foreground/60` composites to rgba(85,96,112,0.6) over white = #99A0A9,
 * measured 2.65:1 — under the WCAG 1.4.3 AA floor of 4.5:1 for 14px body text.
 * The replacement is the dedicated `text-placeholder` token (#6B7280 = 4.83:1).
 *
 * This is a SOURCE guard rather than a render assertion: the failure mode is a class
 * name, and a DOM test would still pass if the token's hex were quietly lowered.
 */
const here = dirname(fileURLToPath(import.meta.url))

// WCAG relative luminance + contrast ratio, so the token's hex is checked too.
function lum([r, g, b]) {
  const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4) }
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
}
function ratio(a, b) {
  const L1 = lum(a), L2 = lum(b)
  return (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05)
}
const hexToRgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)]

describe('F149 — empty-state placeholder text meets WCAG AA', () => {
  it('AdminLessons.vue no longer uses the low-contrast /60 opacity class', () => {
    const src = readFileSync(join(here, 'AdminLessons.vue'), 'utf8')
    expect(src).not.toMatch(/text-muted-foreground\/\d+/)
    expect(src).toMatch(/text-placeholder/)
  })

  it('the placeholder token itself clears 4.5:1 on white', () => {
    const cfg = readFileSync(join(here, '..', '..', '..', 'tailwind.config.js'), 'utf8')
    const m = /placeholder:\s*'(#[0-9A-Fa-f]{6})'/.exec(cfg)
    expect(m, 'placeholder token missing from tailwind.config.js').toBeTruthy()
    const r = ratio(hexToRgb(m[1]), [255, 255, 255])
    expect(r, `placeholder ${m[1]} is ${r.toFixed(2)}:1 on white, needs >= 4.5`).toBeGreaterThanOrEqual(4.5)
  })
})
