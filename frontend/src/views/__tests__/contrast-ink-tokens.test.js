import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for audit-v13 F-13-03/04/05.
//
// The vivid design-system hues (amber #FBBF24, emerald #34D399, pink #F472B6,
// violet #8B5CF6) measure 1.64–4.23:1 as TEXT on the light cream/white surfaces
// this app uses — below the 4.5:1 WCAG 2.2 AA requires (constitution P7).
//
// Measured live this session:
//   Profile.vue:44  text-tertiary  #FBBF24 on #FFFFFF = 1.67:1  -> FAIL
//   AdminDashboard  text-quaternary #34D399 on light    = 1.92:1 -> FAIL
//
// The design system provides *-ink tokens for text on light surfaces. This test
// stops those two call sites from regressing to the vivid token.
//
// NOTE: AdminLayout.vue deliberately keeps `text-tertiary` — it sits on the DARK
// sidebar (#1E293B) where amber measures 8.76:1 and passes. That is why this test
// names files explicitly instead of banning the class repo-wide.

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '../../..')

const read = (p) => readFileSync(resolve(root, p), 'utf8')

describe('audit-v13 F-13-03/04/05 — vivid hues must not be used as text on light surfaces', () => {
  it('Profile.vue does not use text-tertiary for the streak icon', () => {
    const src = read('src/views/Profile.vue')
    expect(src).not.toMatch(/class="[^"]*\btext-tertiary\b(?![-\w])/)
    expect(src).toMatch(/text-tertiary-ink/)
  })

  it('AdminDashboard.vue does not use vivid tokens as StatCard icon colours', () => {
    const src = read('src/views/admin/AdminDashboard.vue')
    expect(src).not.toMatch(/iconColor="text-tertiary"/)
    expect(src).not.toMatch(/iconColor="text-quaternary"/)
    expect(src).toMatch(/iconColor="text-tertiary-ink"/)
    expect(src).toMatch(/iconColor="text-quaternary-ink"/)
  })

  it('every *-ink token used in a view exists in the Tailwind config', () => {
    const cfg = read('tailwind.config.js')
    for (const token of ['tertiary-ink', 'quaternary-ink', 'secondary-ink', 'accent-ink']) {
      expect(cfg).toContain(`'${token}'`)
    }
  })
})

describe('audit-v13 F-13-14 — danger hue must not be used as text on light surfaces', () => {
  // #E11D48 measures 4.00:1 on bg-danger/10 over white and 3.95:1 over cream — below
  // the 4.5:1 AA threshold. #BE123C (danger-ink) is 5.29-6.29:1 on every surface.
  const files = [
    'src/views/Leaderboard.vue',
    'src/views/admin/AdminDashboard.vue',
    'src/views/luyentu/AiVocabGenerator.vue',
    'src/views/speaking/SpeakingRecord.vue',
    'src/components/admin/AiGeneratePanel.vue',
  ]

  it('does not use bare text-danger in the checked views', () => {
    for (const f of files) {
      const src = read(f)
      expect(src, f).not.toMatch(/\btext-danger\b(?!-)/)
    }
  })

  it('defines the danger-ink token in the Tailwind config', () => {
    expect(read('tailwind.config.js')).toContain("'danger-ink'")
  })

  it('defines --geo-danger-ink in the design system', () => {
    expect(read('src/assets/design-system.css')).toContain('--geo-danger-ink')
  })
})
