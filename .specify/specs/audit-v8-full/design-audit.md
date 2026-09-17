# Design Audit — Playful Geometric + Be Vietnam Pro

**Task:** comprehensive-audit-redesign Task 11 (Plan B convergence) · **Date:** 2026-09-16
**Harnesses:** `sweep/v8/ui/design-v2.js` → `design-v2.txt` / `.json`;
`sweep/v8/ui/design.js` → `design.txt`; `sweep/v8/ui/routes-all.js` → `routes-all.txt`
**Constraint (constitution P6):** Be Vietnam Pro is the **only** application font.

## 1. Font — verified at three independent levels

A font claim is only worth what its weakest check proves. Three layers were
measured, because each catches what the others miss:

| Level | Method | Result |
|---|---|---|
| **Declaration** | grep every `font-family` in `frontend/src` | 19 declarations, **all** `var(--geo-font)`; 0 hardcoded families |
| **Token** | value of `--geo-font` in `design-system.css:39` | `'Be Vietnam Pro', system-ui, sans-serif` |
| **Runtime** | `document.fonts.check()` per weight, in Chromium | 400/700/900 = **true / true / true**, **15 faces** loaded |
| **Computed** | `getComputedStyle` first family of every visible text element | **0 non-BVP** out of **7 685** sampled elements |

The 7 685-element computed check is the decisive one: it catches a font applied
via an inline style, a third-party component, or a utility class that no grep
would find. It ran across 12 routes × 5 viewports = 60 combinations.

### Legacy families are absent

| Family | Occurrences in `frontend/src` + `index.html` |
|---|---:|
| Outfit | 0 |
| Plus Jakarta Sans | 0 (one comment recording its removal) |
| Inter | 0 |
| Roboto | 0 |
| Poppins | 0 |

`tailwind.config.js` maps `sans`, `heading` **and** `mono` all to
`'"Be Vietnam Pro"', 'system-ui', 'sans-serif'` — `mono` deliberately resolves to
Be Vietnam Pro rather than a real monospace face, so a stray `font-mono` class
cannot introduce a second family. `index.html` loads exactly one stylesheet:
`family=Be+Vietnam+Pro:wght@400;500;600;700;900`.

> **Note on a false positive I had to rule out.** A naive grep for these names
> matches `Inter` inside *Intermediate*, *interceptor*, *pointer-events* and
> *cursor: pointer*, producing ~55 hits that look alarming. Only a
> word-boundary / quoted search is meaningful; with it, the count is 0.

## 2. Design tokens

`frontend/src/assets/design-system.css` defines **43** `--geo-*` tokens; the
runtime probe confirms `--geo-font` resolves. No second token system was created
during this audit (constitution: tokens live only in `design-system.css` +
`tailwind.config.js`).

Level colours are consumed through the token layer with a literal fallback, e.g.
`PRE_INTERMEDIATE: 'var(--geo-tertiary, #FBBF24)'` in `lessonLevels.js` and
`Lessons.vue` — so a missing token degrades to the documented palette value
rather than to `transparent`.

## 3. Visual language adherence

Sampled per route (13 routes, desktop):

| Signal | Total | Assessment |
|---|---:|---|
| Elements with `badFont` (non-BVP) | **0** | pass |
| Images missing an `alt` attribute | **0** | pass — see §4 |
| Buttons with no accessible name | **0** | pass |
| Focus-visible styling missing | **0** | pass |
| Tap targets < 24 px | **0** | pass (WCAG 2.5.8 AA) |
| Tap targets 24–44 px | 138 | AAA recommendation only — see §4 |
| Elements with `thinBorder` (≠ 2 px) | 5 | informational |
| Elements without a hard shadow | 46 | informational |
| `h1` weight | **900** on every route | pass |

`noShadow = 46` and `thinBorder = 5` are **not** defects: the Playful Geometric
system uses hard `shadow-pop-*` shadows and 2 px borders on *cards, buttons and
containers*, not on every element in the subtree. `h1` at weight 900 is uniform
across all 13 routes, which is the actual typographic contract.

## 4. Accessibility (WCAG 2.2)

| Criterion | Check | Result |
|---|---|---|
| 1.1.1 Non-text Content | `!img.hasAttribute("alt")` | **0 violations** |
| 2.4.1 Bypass Blocks | skip link present | **present** |
| 2.4.7 Focus Visible | `:focus-visible` styling | **0 missing** |
| 2.5.8 Target Size (AA, ≥24 px) | computed box < 24 px | **0 violations** |
| 2.5.5 Target Size (AAA, ≥44 px) | computed box < 44 px | 138 (recommendation) |
| 2.3.3 Animation from Interactions | `prefers-reduced-motion: reduce` | **honoured** — transition duration `0s`, transform `none` |

**On `alt=""`:** the harness counts `!el.hasAttribute("alt")`, not
`!el.getAttribute("alt")`. An empty `alt` is the *correct* marking for a
decorative image; using `getAttribute` previously counted 12 images on
`/lessons`, 5 on `/videos` and 10 on `/admin/users` as WCAG 1.1.1 violations.
Those were false positives and the distinction is now encoded in the harness.

**On the 138 sub-44px targets:** these are 24–44 px, which **satisfies** WCAG 2.2
AA (2.5.8 requires ≥24 px). 44 px is the AAA recommendation. Most sit in dense
admin tables and lesson lists where 44 px rows would be a genuine usability
regression. Reported as a recommendation, **not** a finding, and no code was
changed for it.

**On reduced motion:** measured by emulating the media feature, not by reading
the CSS — the computed transition duration is `0s` and transform is `none` under
`reduce`, so animations genuinely stop rather than merely being declared to.

## 5. Responsive layout — 0 overflow

| Harness | Coverage | Overflow beyond the 16 px scrollbar allowance |
|---|---|---|
| `design-v2.js` | 12 routes × 5 viewports (360/768/1280/1440/1920) = **60** | **0** |
| `routes-all.js` | 38 visited routes × 2 viewports × 3 roles = **228 visits** (`/admin` is the one inventory entry not visited directly — it only redirects) | **0** |

Every route reports `overflow = -15`, i.e. `scrollWidth − clientWidth = −15`.
That is the healthy value: Chromium reserves ~15 px for the scrollbar, so a
naive `scrollWidth > clientWidth` assertion would flag **every** page as broken.
The harness therefore records raw `scrollWidth`/`clientWidth` in
`routes-all.json` and only flags a genuine excess beyond the noise threshold.
This is the documented false-positive source in `AGENTS.md`.

The viewport set matches the audit prompt's requirement (360/768/1280/1440/1920).

## 6. What changed, and what did not

**No design-system code was changed in this round.** The migration to Be Vietnam
Pro was completed in the earlier Plan B round; this round's job was to verify it
survives on the current build rather than to re-do it. Verified fresh:

- 7 685 computed elements → 0 non-BVP
- 15 font faces loaded; weights 400/700/900 all report available
- 0 legacy families
- 0 overflow across **288 browser visits** (228 route×viewport×role from `routes-all.js` + 60 route×viewport from `design-v2.js`)
- 0 a11y violations at the AA level

## Summary of dispositions

| Item | Disposition |
|---|---|
| Be Vietnam Pro as sole font | **PASS** — declaration + token + runtime + computed, 0/7 685 deviations |
| Legacy families (Outfit/PJS/Inter/Roboto/Poppins) | **PASS** — 0 occurrences |
| Design tokens (`--geo-*`) | **PASS** — 43 tokens, single system |
| WCAG 1.1.1 / 2.4.1 / 2.4.7 / 2.5.8 / 2.3.3 | **PASS** — 0 violations |
| WCAG 2.5.5 (AAA, 44 px) | **Recommendation only** — 138 targets 24–44 px; AA is met |
| `noShadow` 46 / `thinBorder` 5 | **Not applicable** — system applies these to containers, not every node |
| Responsive overflow | **PASS** — 0/288 visits beyond the scrollbar allowance |
