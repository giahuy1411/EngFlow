# audit-v13 — Prompt holes found and fixed

The design-system prompt handed to this audit is used as a **spec to test the code against**. It also has to be internally consistent. Below are the holes found, each with a measurement.

## H1 — The prompt claims AAA, but mandates colours that fail AA — **REAL, FIXED**

**The prompt says:**
> "**Contrast**: The text is slate-800 on off-white/white, which is AAA."

That is true *only* for `foreground` on `background`/`card`. But the same prompt also says:
> "Use `secondary`, `tertiary`, and `quaternary` rotationally for decorative shapes, icons, or **emphasized words**."

Applied literally to words, those hues measure (WCAG relative luminance, computed this session):

| Foreground | Surface | Ratio | AA (4.5:1) |
|---|---|---|---|
| #1E293B foreground | #FFFDF5 cream | 14.36 | PASS |
| #1E293B foreground | #FFFFFF white | 14.63 | PASS |
| #FFFFFF on #8B5CF6 accent | — | **4.23** | **FAIL** (normal text) |
| #FFFFFF on #F472B6 secondary | — | **2.65** | **FAIL** |
| #FFFFFF on #FBBF24 tertiary | — | **1.67** | **FAIL** |
| #FFFFFF on #34D399 quaternary | — | **1.92** | **FAIL** |
| #FBBF24 tertiary as text | #FFFFFF | **1.67** | **FAIL** |
| #34D399 quaternary as text | #FFFFFF | **1.92** | **FAIL** |
| #8B5CF6 accent as text | #FFFDF5 | **4.16** | **FAIL** |

So the "AAA" claim is a **partial truth that becomes false the moment the palette is used as the prompt instructs**. Constitution P7 makes WCAG 2.2 AA mandatory, so P7 resolves the conflict.

**Fix — the rule the prompt was missing (now recorded, and applied in code):**

- **vivid** `accent`/`secondary`/`tertiary`/`quaternary` → **fills, borders, decorative shapes, /10–/30 tints only.** Correct as-is (dark ink on amber = 8.76:1, on emerald = 7.61:1).
- **`*-ink`** (`accent-ink` #6D28D9, `secondary-ink` #BE185D, `tertiary-ink` #B45309, `quaternary-ink` #047857) → **text on a light surface** (cream/white/muted).
- **`*-strong`** (`accent-strong` #7C3AED, `secondary-strong` #DB2777) → **a vivid surface under white text.**

This layer already existed in `design-system.css` (added in audit-v11 F132/F138) but **4 call-sites were missed** — see F-13-03/04/05 in `findings.md`. v13 closed them and **measured one suspected site (admin sidebar) as already correct (8.76:1) and left it unchanged.**

## H2 — The prompt specifies fonts that are wrong for this product — **RESOLVED BY SUBSTITUTION**

The prompt names **Outfit** (headings) + **Plus Jakarta Sans** (body). The user's instruction replaced all fonts with **Be Vietnam Pro**, and constitution P6 mandates it as the single family. Verified: `index.html:54`, `tailwind.config.js:76-80`, `design-system.css:86`; grep for Outfit/Plus Jakarta = **0 hits**. Be Vietnam Pro is also the correct choice because it covers Vietnamese diacritics.

## H3 — "Lucide React" is the wrong library for this stack — **documented, not a defect**

The prompt says "Lucide React settings". This is a **Vue 3** app; the correct package is `lucide-vue-next` (present in `package.json`, stroke-width 2.5 enforced globally via `.lucide { stroke-width: 2.5 }`). The *intent* (chunky 2.5px icons in shapes) is implemented; only the library name in the prompt is wrong.

## H4 — Layout claims that do not match the real product — **documented, not a defect**

- "Hero: Text left, Image right" — the real hero has no photographic image; it has a blob shape and a sun circle. The prompt itself hedges ("The image itself has a blob mask"), so the decoration is honoured.
- "Pricing: the middle card is scaled up (1.1) and has a massive yellow star badge 'MOST POPULAR' rotated 15deg" — the real product has **2** plans, not 3, so there is no "middle card"; `PremiumPage.vue` scales the featured card and renders a star badge. Intent honoured, literal wording impossible for a 2-plan product.

## Verdict

| Hole | Real product impact | Action taken |
|---|---|---|
| H1 contrast / "AAA" | **Yes — 3 real WCAG failures** | **Fixed in code (F-13-03/04/05) + rule recorded here** |
| H2 fonts | No — already resolved by user instruction | Verified, recorded |
| H3 Lucide React | No — library name only | Recorded |
| H4 layout literals | No — product has 2 plans / no hero photo | Recorded |
