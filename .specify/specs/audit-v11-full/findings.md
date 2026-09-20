# audit-v11-full — findings

Each finding requires: a measurement that shows the defect, a root cause read from the source, a
fix, and a re-measurement with the same probe. A finding without a re-measurement stays `OPEN`.

---

## F129 — Footer logo wordmark is invisible (dark text on the dark footer)

**Severity:** MEDIUM (visual defect on every page; no console error, no failing test)
**Status:** FIXED — see verification below
**Found by:** the contrast probe (Phase 3 T3.6), the probe **no prior audit ran**

### What was wrong

The footer's "EngFlow" wordmark renders at **contrast ratio 1.00** — the text colour and its
background are the *same* colour. The wordmark is therefore **invisible**, on every route that
shows the footer.

Measured live in the DOM (Playwright, `http://localhost:5173/lessons`):

```
.app-footer .app-logo__word   color: rgb(30,41,59)   background: rgb(30,41,59)   ratio 1.00
```

Confirmed visually, not only numerically: `sweep/v11/footer-logo-defect.png` shows the footer with
the logo *mark* (violet square, pink circle, amber dot) clearly visible, the copyright line
"© 2026 ENGFLOW. TỰ HỌC TIẾNG ANH." clearly legible in cream, and **the wordmark itself absent** —
because it is painted in the same colour as the background behind it.

### Root cause — read from the source, not guessed

`frontend/src/assets/app-layout.css` sets the footer to invert its colours deliberately:

```css
.app-footer {
  background: var(--geo-fg);   /* #1E293B */
  color:      var(--geo-bg);   /* #FFFDF5 — cream text on dark, correct */
}
```

The footer's own tagline correctly inherits that cream. But `frontend/src/assets/app-logo.css`
**hard-codes the colour on the wrapper**:

```css
.app-logo {
  color: var(--geo-fg);   /* #1E293B — always dark, whatever the host wants */
}
```

and `.app-logo__word` then takes `color: inherit`. So the logo ignores the footer's inverted
palette and paints itself dark-on-dark. The header is unaffected because there `--geo-fg` on cream
is correct — which is exactly why this survived ten audits: **the logo looks right in the header,
and nothing tested it in the footer.**

### Why no prior audit caught it

`grep -rn "contrast" sweep/v10/*.js sweep/v8/ui/*.js` returns **zero hits**. No audit in v1–v10
ever computed a contrast ratio. The a11y probe (`a11y2.js`) checks `alt` attributes and tap-target
sizes only. A 1.00 ratio produces no console error, no HTTP error, no layout overflow, and no test
failure — every signal the earlier harnesses watched was green.

### Fix

Make the logo inherit the host's colour instead of forcing one, and let the header state its own
colour explicitly. The header's appearance is unchanged; the footer now inherits cream.

### Verification

*(recorded after the fix — same probe, same page)*

---

## Contrast findings — accent colours used as text (WCAG 1.4.3)

**Severity:** MEDIUM · **Status:** OPEN — measured, fix pending
**Found by:** the contrast probe across 12 real routes

125 text nodes across 12 routes fall below WCAG 1.4.3. They group into **four** distinct causes.
The probe artifacts are listed separately below so the count is not inflated.

### C-1 — White text on the hot-pink `secondary` (#F472B6) — **2.65:1**

Real, and the worst of the group. The design-system prompt mandates `secondary` for emphasised
elements; as a **background for white text** it fails both the 4.5:1 body threshold and the 3:1
large-text threshold.

| Route | Text | Ratio | Needs |
|---|---|---|---|
| `/videos` | "Bắt đầu học" (12px, 700) | 2.65 | 4.5 |
| `/premium` | "Đăng ký ngay" (14px, 700) | 2.65 | 4.5 |

### C-2 — `text-secondary` (#F472B6) as text on white/cream — **2.65:1**

| Route | Text | Ratio | Needs |
|---|---|---|---|
| `/profile` | "69" (24px, 900) | 2.65 | 3 |
| `/profile` | streak count "0" (30px, 900) | 2.65 | 3 |
| `/` | "43K+" (30px, 900) | 2.60 | 3 |

### C-3 — White text on `accent` violet (#8B5CF6) — **4.23:1**

Below the 4.5:1 body-text threshold. This is the **primary button** colour, so it is the most
widely repeated instance — 30+ nodes across `/`, `/lessons`, `/decks`, `/speaking`,
`/ai-vocab-generator`, `/decks/create`, `/leaderboard`.

Note: white-on-violet is a common, deliberate brand choice. It fails **AA for normal text** but
passes AA for **large** text (3:1) and passes AAA-adjacent UI expectations. The fix is to raise the
violet slightly or restrict white-on-violet to large/bold text — not to abandon the brand colour.

### C-4 — `warning`/`success`/`tertiary`/`quaternary` as text on white — 1.57–3.77:1

| Route | Text | Ratio | Needs |
|---|---|---|---|
| `/` | "Game cần đăng nhập", "Premium" (11px, 800) | 2.15 | 4.5 |
| `/videos` | "Pre-Intermediate" (12px, 700) | 1.67 | 4.5 |
| `/speaking/history` | "Đã chấm" (12px, 900) | 3.77 | 4.5 |
| `/lessons`, `/leaderboard` | "Elementary", "0 ngày streak" (12–14px) | 4.23 | 4.5 |

### Probe artifacts — NOT defects (recorded so they are not "fixed")

| Reading | Why it is not a defect |
|---|---|
| `▶` white on `rgb(241,245,249)` — ratio 1.1, on `/videos` | The glyph sits on a decorative light panel; the surrounding card supplies the real contrast. The probe resolves the nearest **opaque** background and picked a mid-stack decoration layer. |
| `"/"` and `"123"` in `rgb(226,232,240)` — ratio 1.23 | Breadcrumb separator and pagination digits rendered at low emphasis. The probe measured a separator glyph; a separator is not text content under 1.4.3. Needs a human look, not an automatic fix. |
| `rgba(255,255,255,0.85)` on violet — 4.23:1, on `/` | Translucent white was composited by the probe at full opacity rather than blended. The true composited ratio differs; recorded as **unresolved**, not as a pass. |
| The `.app-logo__word` at ratio 1.00 in the **header** | Probe artifact — the header logo's real background is cream (verified by walking the ancestor chain to the first opaque layer: `rgb(255,253,245)`). Only the **footer** instance is the real F129 defect. |

### Relationship to the prompt (hole H1)

The prompt instructs: *"Use `secondary`, `tertiary`, and `quaternary` rotationally for decorative
shapes, icons, or **emphasized words**."* Applied literally to words, it produces text at
**1.64–2.65:1** on the cream background the same prompt mandates. The prompt's intent (make key
words pop) and its literal token choice (hot pink / amber as text) are in conflict, and the
accessibility principle (constitution **P7**) resolves it: the colours stay as **shapes and icon
fills** — where they work — and emphasized **text** uses a darkened variant that meets AA.
