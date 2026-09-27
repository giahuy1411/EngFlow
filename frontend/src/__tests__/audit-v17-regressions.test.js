import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { useToast } from '@/composables/useToast'

// Regression tests for audit-v17 findings.
//
// F-17-01 — toast success used `bg-accent text-white`. Measured with the WCAG
//   relative-luminance formula: #FFFFFF on #8B5CF6 = 4.23:1, below the 4.5:1
//   WCAG 2.2 AA floor for body text (constitution P7). It also bypassed the
//   audit-v11 F132 ink/strong layer every other call site goes through.
//   Fix: `bg-accent-strong` (#7C3AED) = 5.70:1 under white text.
//
// F-17-02 — the documented rule "gõ dưới 2 ký tự thì không tìm" (demo doc §4.2)
//   was enforced only on the backend /api/vocabulary/search path, not in
//   SearchVocabulary.vue, which uses /api/vocabulary/dictionary/{word}. A 1-char
//   query fired a real lookup (measured ~20 s when the upstream cache is cold).
//
// F-17-03 — stale CSS-var fallbacks carrying values a prior audit removed: the
//   warm-cream #E5DECF (audit-v5) and the low-contrast #64748B (audit-v11 F138,
//   which raised --geo-muted-fg to #556070 precisely because #64748B is 4.34:1 on
//   the muted panel). A fallback that resolves to a value the audit declared
//   non-compliant is a latent violation the moment the var is unresolved.
//
// NOTE on test strength (audit-v17 cross-review): F-17-02 is asserted
// BEHAVIOURALLY (the guard is exercised through the component), not by grepping
// source — a source regex matches a commented-out guard and would false-pass on a
// reverted fix. The F-17-03 assertions read source but check the value is absent
// everywhere in the file, and that the correct token is present.

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '../..')
const read = (p) => readFileSync(resolve(root, p), 'utf8')

// ── F-17-01: behavioral, mutation-sensitive ──────────────────────────────────
describe('audit-v17 F-17-01 — toast success must not put white text on vivid accent', () => {
  it('uses accent-strong (5.70:1), never the vivid accent (4.23:1)', () => {
    const { toastBackground } = useToast()
    const classes = toastBackground('success').split(/\s+/)
    expect(classes).not.toContain('bg-accent') // vivid #8B5CF6 under white = 4.23:1 FAIL
    expect(classes).toContain('bg-accent-strong') // #7C3AED under white = 5.70:1 PASS
  })
})

// ── F-17-02: behavioral — drives the real component ──────────────────────────
vi.mock('@/services/vocabularyService', () => ({
  default: { search: vi.fn(() => Promise.resolve([])) },
}))

describe('audit-v17 F-17-02 — vocabulary search does not fire a lookup for < 2 characters', () => {
  let vocabularyService
  let SearchVocabulary

  beforeEach(async () => {
    vi.clearAllMocks()
    vocabularyService = (await import('@/services/vocabularyService')).default
    SearchVocabulary = (await import('@/views/SearchVocabulary.vue')).default
  })

  async function mountAndSearch(term) {
    const wrapper = mount(SearchVocabulary, {
      global: { stubs: { RouterLink: true, 'router-link': true } },
    })
    await wrapper.find('input').setValue(term)
    // The submit control is icon-only; it is named by aria-label ("Tra từ").
    const btn = wrapper.find('[aria-label="Tra từ"]')
    expect(btn.exists(), 'the "Tra từ" control must exist').toBe(true)
    await btn.trigger('click')
    await flushPromises()
    return wrapper
  }

  it('1 character -> service is NOT called', async () => {
    await mountAndSearch('h')
    expect(vocabularyService.search).not.toHaveBeenCalled()
  })

  it('2 characters -> service IS called (boundary is "< 2", not "< 3")', async () => {
    await mountAndSearch('hi')
    expect(vocabularyService.search).toHaveBeenCalledTimes(1)
    // audit-v17 L1-A: the view now passes an options bag (onSlow) as the 2nd arg.
    expect(vocabularyService.search.mock.calls[0][0]).toBe('hi')
    expect(vocabularyService.search.mock.calls[0][1]).toHaveProperty('onSlow')
  })
})

// ── F-17-03: no stale removed-legacy fallback values ─────────────────────────
describe('audit-v17 F-17-03 — no stale CSS-var fallback carries a removed value', () => {
  // Values a prior audit removed/replaced. Kept in one place so a new sweep is a
  // one-line change. #64748B is included because audit-v11 F138 replaced it.
  const REMOVED = ['#E5DECF', '#F5F0E6', '#EAE4D6', '#64748B']

  it('Lessons.vue carries no removed value and uses the real --geo-border token', () => {
    const src = read('src/views/Lessons.vue')
    for (const v of REMOVED) expect(src.toUpperCase()).not.toContain(v.toUpperCase())
    expect(src).toMatch(/--geo-border,\s*#E2E8F0/i)
  })

  it('lessonLevels.js fallbacks use the current --geo-muted-fg (#556070)', () => {
    const src = read('src/utils/lessonLevels.js')
    for (const v of REMOVED) expect(src.toUpperCase()).not.toContain(v.toUpperCase())
    expect(src).toMatch(/--geo-muted-fg,\s*#556070/i)
  })
})
