# F143 — the hero circle covering the headline (owner-reported)

**Date:** 2026-09-21 (+07) · **Reported by:** the project owner
**Report:** *"cái vòng tròn màu vàng ở trang chủ đã che mất chữ học tiếng anh vui vẻ"*

---

## 1. The report was precise, and the measurement agrees with it

`.app-hero__sun` is documented in its own comment as *"A massive yellow circle **behind** the text"*.
It is not behind anything.

| Viewport | Sun box | Headline box | Overlap |
|---|---|---|---|
| 1280 × 900 | 640×640 at (−96, −58) | 528×205 at (81, 166) | **464 × 205 px** |
| 390 × 844 | 544×544 at (−160, −90) | 287×137 at (44, 134) | **100% of the headline** |

`sunZ=0`, `position: absolute`, `opacity: 0.35`.

## 2. Root cause — CSS painting order

The CSS painting order within a stacking context is:

| Step | What paints |
|---|---|
| 2 | negative z-index descendants |
| 3 | block-level in-flow backgrounds |
| 4 | **block-level in-flow backgrounds (ancestor `div.bg-background`, opaque cream)** |
| 5 | **inline in-flow content — the text glyphs** |
| 6 | **`z-index: 0` positioned descendants — the circle** |

So `z-index: 0` paints the circle in **step 6**, *after* the text in **step 5**. The comment says
"behind"; the property says "in front". That is the whole bug.

## 3. Composited, the reader sees different colours than the stylesheet declares

Amber at 35 % over the glyphs, composited with the cream backdrop:

| Text | Specified | **Actually rendered** | Needs | Verdict |
|---|---|---|---|---|
| HỌC TIẾNG ANH | 12.01:1 | 5.27:1 | 3 | passes, badly degraded |
| **VUI VẺ** | 3.48:1 | **2.64:1** | 3 | **FAIL** |
| subtitle | 5.23:1 | **3.17:1** | 4.5 | **FAIL** |

"Vui Vẻ" is the worst hit — which is exactly the word the owner named.

## 4. The two obvious fixes, and why each fails

| Attempt | Result |
|---|---|
| `z-index: 0` (original) | circle paints over the text — **the bug** |
| `z-index: -1` | paints in step 2, i.e. **behind step 4** — the ancestor `div.bg-background` has `background: rgb(255,253,245)`, opaque. **The circle vanished entirely** (screenshot: `sweep/v11/hero-after-1280.png` — no circle at all) |
| **raise the text** ✅ | circle keeps `z-index: 0` (visible, above the ancestor background); `.app-hero__text` gets `position: relative; z-index: 1` |

The ancestor-background check is what made the difference — I walked the ancestor chain and found two
elements reporting an opaque cream background, which is why the naive `z-index: -1` "fix" would have
silently deleted a design element.

## 5. Re-measured

| Text | 390 | 768 | 1280 | 1920 | Needs |
|---|---|---|---|---|---|
| HỌC TIẾNG ANH | 12.01 | 12.01 | 12.01 | 12.01 | 3 ✓ |
| **VUI VẺ** | 5.83 | 5.83 | 5.83 | 5.83 | 3 ✓ *(was 2.64)* |
| subtitle | 5.23 | 5.23 | 5.23 | 5.23 | 4.5 ✓ *(was 3.17)* |

- `sunZ=0`, `textZ=1` at every width; the circle is visible in every screenshot.
- Lighthouse `/` Accessibility **100**.
- Full sweep, 19 routes × 3 roles: **0 contrast failures, 0 console errors, 0 UI-caused API errors.**
- Frontend suite **109 passed / 1 skipped**; build **177.40 kB** (unchanged — token swaps only).

Visual evidence: `hero-before-1280.png` / `hero-after2-1280.png`, `hero-before-390.png` /
`hero-after2-390.png`, `mobile-menu-after.png`.

## 6. A gap in my own F132 work, exposed by this report

`.app-hero__title-accent` was still `color: var(--geo-accent)` — the **F132 codemod never touched it**,
because that codemod scanned `*.vue` files and this rule lives in `assets/app-layout.css`.

That prompted a grep of the shared CSS for the same patterns, which found **9 more violations**:

| Rule | Was | Now |
|---|---|---|
| `.app-admin__topbar-label` | accent text | `--geo-accent-ink` |
| `.app-marquee__item::after` | accent text glyph | `--geo-accent-ink` |
| `.app-auth__link` | accent text | `--geo-accent-ink` |
| `.app-navbar__mobile-link--active` | white on accent (4.23:1) | white on `--geo-accent-strong` (5.70:1) |
| `.app-navbar__mobile-cta--solid` | white on accent | `--geo-accent-strong` |
| `.app-admin__nav-link--active` | white on accent | `--geo-accent-strong` |
| `.app-features__icon--accent` | white icon on accent | `--geo-accent-strong` |
| `.app-features__icon--secondary` | white icon on secondary (**2.65:1**) | `--geo-secondary-strong` (4.60:1) |
| `.lesson-html em` / `summary` / `a` / `a:hover` | accent / secondary text | `*-ink` |
| `.app-tabs__tab--active` | accent text | `--geo-accent-ink` |

**Why F132 missed them, stated plainly:** I reported "0 contrast failures across 19 routes", and that
was true *for the routes and states the probe entered*. The sweep rendered components through their
Tailwind classes and never triggered CSS-only rules — the **mobile menu needs a click** to render,
`.lesson-html` prose needs lesson content, the marquee needs its animation state. **A route sweep cannot
see a rule whose state it never enters.**

This is the third time in this audit that a "verified clean" result had a boundary the probe could not
see (F138 via a compositing bug, then this). The pattern is worth naming: *the probe's coverage is not
the app's surface.*
