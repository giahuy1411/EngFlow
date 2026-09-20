# Design audit — audit-v9-full (Playful Geometric / Be Vietnam Pro), 2026-09-17

Font verification plus ONE code change (dead-CSS removal). No decoration was invented: the prompt's
diagonal-stripe/marquee ideas remain a deliberate divergence decided in the v8 round, and this audit keeps it.

All numbers below are parsed from `sweep/v8/r2-design-v2.log` (round 2, final build) unless a row says otherwise.

## Font — measured, not assumed

| Check | Result |
|---|---|
| webfont really loaded (400/700/900) | {"loaded":true,"bold":true,"black":true,"faces":15} on `document.fonts.check` at every viewport |
| computed `font-family` across sampled text nodes | 7685 elements sampled, 0 non-Be-Vietnam-Pro |
| legacy families in the cascade | 0 hits (Outfit / Plus Jakarta Sans / Inter / Roboto / Poppins) |
| routes with a bad font (228 role/route/viewport visits) | 0 (`routes-all.json`) |

## Design tokens — exact-value compare, 11 tokens x 60 route/viewport cells

`--geo-font --geo-bg --geo-fg --geo-accent --geo-secondary --geo-tertiary --geo-quaternary --geo-border --geo-radius-md --geo-shadow-md --geo-border-width`

| Check | Result |
|---|---|
| tokens missing | 0 |
| token value drift | 0 |
| lucide icons at stroke-width 2.5 (parsed numerically) | 750 checked, 0 violations |
| mobile (<640px) 2px shadow downgrade | 0 violations |

## Responsive / layout

| Check | Result |
|---|---|
| horizontal overflow (5 viewports x 12 routes) | 0 cells (raw scrollWidth-clientWidth per cell; the -15px on every cell is Chromium scrollbar reservation, per AGENTS.md) |
| overflow over 228 visits | 0 |
| console errors over 228 visits | 0 |
| API >= 400 caused by the UI | 0 |
| wrong landing (guard asserted in BOTH directions) | 0 |
| anon visits on guarded routes | 52, all redirected |

## Accessibility

| Check | Result | Source |
|---|---|---|
| `img` without an `alt` attribute (WCAG 1.1.1) | 0; decorative `alt=""` counted separately (12 /lessons, 5 /videos, 10 /admin/users) | `r2-a11y2.log`, `r2-altcheck.log` |
| tap targets below 24px (WCAG 2.5.8 AA) | 0 | `r2-a11y2.log` |
| reduced-motion | decorations become static under `prefers-reduced-motion` | `frontend/src/assets/design-system.css` |

## The one code change: dead `.geo-*` CSS (F113)

| Step | Result |
|---|---|
| `.geo-*` class selectors before -> after | ~50 -> 4 |
| selectors with 0 literal usage | 0 (`v9_deadcss_check.py` output: selectors=4 used=4 UNUSED=0) |
| kept groups | `.geo-card`, `.geo-markdown`, `.geo-audio`, `.geo-radio` (referenced by `Pagination.vue`, `MatchingExercise.vue`, `ListeningSkill.vue`, `GrammarSkill.vue`) |
| CSS entry | 92.42 kB -> 85.22 kB raw (gzip 15.91 -> 14.86) |
| JS entry | 176.80 kB unchanged |
| tests / build after removal | vitest 90/90, vite build success, design-v2 0 violations |

Checker caveat that mattered: a naive extractor reported a phantom `.geo-shadow-` selector that existed
only inside a CSS comment (`“.geo-shadow-* rules above”`). `v9_deadcss_check.py` strips comments first,

so the gate cannot be satisfied by prose.

## Decoration components (correcting v8)

`frontend/src/components/decor/` ships `DecoConfetti.vue`, `DecoShape.vue`, `DotBackground.vue`,
`SquiggleDivider.vue` and `index.js`. The v8 report's claim that confetti had no implementation was wrong;
this audit records the correction instead of adding new decoration.
