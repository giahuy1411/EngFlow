# audit-v11-full — findings (redo, from scratch)

Each finding requires: a measurement that shows the defect, a root cause read from the source, a fix,
and a **re-measurement with the same probe**. A finding without a re-measurement stays `OPEN`.

Method note: every defect below was confirmed by **a second independent probe** before being written
up. Four candidate findings were **falsified** in that step and are recorded in `probe-artifacts.md`
rather than "fixed" — a probe that reports a defect the app does not have is itself a defect.

---

## F129 — Footer logo wordmark invisible (dark text on the dark footer)

**Severity:** MEDIUM · **Status:** FIXED (pre-existing fix, independently re-verified)
**Found by:** the contrast probe — the probe no prior audit ran

### What was wrong
`.app-logo` hard-coded `color: var(--geo-fg)` (#1E293B). The footer deliberately inverts its palette
(`background: var(--geo-fg)`), so the wordmark painted **dark-on-dark: contrast 1.00** — invisible on
every route that shows the footer.

### Root cause
`frontend/src/assets/app-logo.css` set the colour on the wrapper; `.app-logo__word` inherited it. The
header was unaffected (dark on cream is correct), which is why it survived ten audits — nothing tested
the logo *in the footer*.

### Fix (already in the working tree when this audit started)
`color: var(--geo-fg)` → `color: inherit`, so the logo follows whatever host it sits in.

### Re-measurement — this session, same probe, `/lessons`
```
.app-footer .app-logo__word   color: rgb(255,253,245)  bg: rgb(30,41,59)  ratio 14.36   ✓
header  .app-logo__word       color: rgb(30,41,59)     bg: rgb(255,253,245) ratio 14.36 ✓
```
**Closed.** The header is unchanged (14.36:1) and the footer is now legible.

---

## F130 — The parity baseline was contaminated, and six harness call sites mask it

**Severity:** MEDIUM (audit-integrity, not user-facing) · **Status:** OPEN (harness, not product)
**Found by:** re-measuring the parity line myself instead of inheriting it

### What was wrong
`evidence/baseline.md` recorded `payments = 126`. The live value is **127**:

```
id=80256  order_code=ENG340D56BE7C33  status=PENDING  transaction_id=NULL  created_at=2026-09-20 22:23:20
```

That row was created **inside the previous baseline's own measurement window** (`~22:20–22:35`) by the
`/premium/checkout` mount behaviour documented in `AGENTS.md`. The baseline therefore recorded **audit
residue as product data**.

### Why it matters
Six call sites hard-code the contaminated number:

```
sweep/v10/prompt-claims.js:394   cleanupAuditPayments(126)
sweep/v10/routes-all-v10.js:240  cleanupAuditPayments(126)
sweep/v8/p17_screenshots.js:199  cleanupAuditPayments(126)
```
plus the `ui-lib.js` / `lib.js` defaults. Each deletes the residue, prints
`after=126 expected=126 -> PARITY OK`, and **passes for the wrong reason** — a check that cannot
distinguish "cleaned up correctly" from "the target was already wrong".

### Fix applied this session
Removed the residue (rows `80256`, `80257`) with an enumerated, `SET QUOTED_IDENTIFIER ON` delete
(`sweep/v11/cleanup-audit-payments.sql`), then re-verified:

```
payments: 128 → 126      Msg count: 0      parity: 1471|43735|72|127|28|15|4|126|14|5
```

### Still open
The **six hard-coded `126` call sites were not updated** — that is a harness change touching v8/v10
files this audit does not own. Recorded here so the number is not carried forward silently.

---

## F131 — `/premium` overflows horizontally at every width from 360px to ~880px

**Severity:** MEDIUM · **Status:** FIXED · **Found by:** the responsive probe (raw numbers)

### Measurement (before)
`document.documentElement.scrollWidth − clientWidth` on `/premium`:

| Viewport | 360 | 480 | 600 | 700 | 767 | 768 | 800 | 900+ |
|---|---|---|---|---|---|---|---|---|
| delta | **+8** | **+8** | **+8** | **+8** | **+8** | **+28** | **+29** | 0 |

Offenders identified by geometry, not guesswork:
```
.app-plan__badge      right=781  (docW=753)   <- the ★ badge
.app-plan--featured   right=754  (docW=753)   <- the scaled card
```

### Root cause — two effects stacking
1. `.app-plan__badge` is `position:absolute; right:-1.25rem` **and** `rotate(15deg)`. The rotation alone
   grows a 56px square's bounding box to ~68px, so it overhangs further than the offset implies.
2. At `md` (768px) `.app-plan--featured` gains `transform: scale(1.1)`, widening the card ~17px per side
   — which is exactly why the delta jumps 8 → 28 precisely at the 768px breakpoint.

### Fix
`frontend/src/views/premium/PremiumPage.vue`:
- badge `right: -0.25rem` on mobile (back to `-1.25rem` at `md+`, where there is room);
- the plan grid gets `md:px-8` so the scaled card and the badge have slack to expand into.

### Re-measurement (after) — same probe
| Viewport | 360 | 768 | 780 | 800 | 850 | 900 | 1024 | 1280 |
|---|---|---|---|---|---|---|---|---|
| delta | **0** | **0** | **0** | **0** | **0** | **0** | **0** | **0** |

**Closed.** Visual confirmation: `sweep/v11/after-premium-768.png` (badge fully inside the viewport).

---

## F132 — WCAG AA contrast: the design system's own accent colours fail as text (the H1 hole)

**Severity:** MEDIUM (accessibility, affects every route) · **Status:** FIXED
**Found by:** the contrast probe across 19 real routes — **the probe no prior audit ran**
(`grep -rn "contrast" sweep/v10/*.js sweep/v8/ui/*.js` → zero hits)

### The problem, measured
The prompt mandates `secondary`/`tertiary`/`quaternary` for "decorative shapes, icons, **or emphasized
words**". Applied literally to *words*, those hues measure far below AA on the surfaces the same prompt
mandates:

| Token | on #FFFFFF | on #FFFDF5 | Needs (body 4.5 / large 3) |
|---|---|---|---|
| `accent` #8B5CF6 | 4.23 | 4.16 | ✗ |
| `secondary` #F472B6 | 2.65 | 2.60 | ✗ |
| `tertiary` #FBBF24 | 1.67 | 1.64 | ✗ |
| `quaternary` #34D399 | 1.92 | 1.89 | ✗ |
| `success` #059669 | 3.77 | 3.70 | ✗ |
| `warning` #F59E0B | 2.15 | 2.11 | ✗ |

**Scale before the fix: 165 failing text nodes across 19 routes** (my own probe; white-on-violet alone
appears on every route via the navbar).

### Root cause — two distinct failure classes, which is why a one-token fix cannot work
1. **Coloured text on a light surface** — needs a darker *ink*.
2. **White text on a vivid surface** — needs a darker *fill*.

Crucially the **same vivid hues already pass where they are fills under dark ink**: dark-on-amber =
**8.76:1**, dark-on-emerald = **7.61:1**. Changing the base hexes would therefore have broken exactly
the places that were correct. (The codebase had already discovered this once: `.app-btn--emerald` /
`--amber` carry a comment from an earlier audit choosing dark ink on the light brand fill.)

### Fix — a semantic ink/strong layer; vivid colours kept for decoration
Added to `design-system.css` `:root` and mirrored in `tailwind.config.js`:

```
--geo-accent-ink       #6D28D9   7.10:1 on white
--geo-accent-strong    #7C3AED   5.70:1 under white
--geo-secondary-ink    #BE185D   6.04:1
--geo-secondary-strong #DB2777   4.60:1
--geo-tertiary-ink     #B45309   5.02:1
--geo-quaternary-ink   #047857   5.48:1
--geo-success-ink      #047857   5.48:1
--geo-warning-ink      #B45309   5.02:1
```

**The rule, recorded in the token block itself:**
- vivid `--geo-*` → fills, borders, decorative shapes, `/10`–`/30` tints (unchanged, still correct);
- `--geo-*-ink` → text on a light surface;
- `--geo-*-strong` → a vivid surface under white text.

Applied centrally first (`.app-btn--primary`, `--featured`, `--pink`, `--tertiary`,
`.app-navbar__link--active`, `.app-navbar__auth--solid`, `.app-cta__card`, `.app-hero__stat-num--*`),
then by codemod across 40 `.vue` files. Scripts kept for auditability:
`sweep/v11/codemod-ink.py`, `codemod-strong.py`, `codemod-strong2.py`.

Also fixed while in scope:
- **`lessonLevels.js`** returned ONE hex used as **both** `background` and `color`. Split into
  `levelColor()` (fills) and `levelInkColor()` (text).
- **`Lessons.vue` carried a duplicate local `levelColor` *and* `levelLabel`** that shadowed the shared
  imports — so a fix applied only to the module would have silently half-landed. Both duplicates removed.
- **`.pagination-page-divider`** used `var(--geo-border)` (#E2E8F0 = **1.23:1**, effectively invisible)
  for a separator that carries meaning → `--geo-muted-fg` (4.76:1).
- **`AdminUsers` status chip** `text-muted-foreground` on `bg-muted` = 4.34:1 → `text-foreground`.

### Re-measurement (after) — same probe, same 19 routes
```
before: 165 failing text nodes
after:    0
```
The only residual readings were 5 × `▶` glyphs, which the second probe **falsified** as a probe bug
(see `probe-artifacts.md`): correctly composited, the glyph is white on `rgba(30,41,59,0.85)` =
**9.24:1**.

### Two regressions I introduced and then fixed
Recorded because the honest version of this finding includes them:

1. The codemod rewrote `text-tertiary` → `text-tertiary-ink` **inside the dark admin sidebar**
   (`bg-foreground`), where vivid amber was correct. Reverted on `AdminLayout.vue` (×2),
   `Profile.vue`, `AdminDashboard.vue` (×2).
2. It rewrote `bg-secondary` → `bg-secondary-strong` on the AdminExercises difficulty chip, which pairs
   with **dark ink** (5.52:1, already passing). Reverted — only white-on-vivid needed the strong fill.

Both were caught by re-measuring rather than assuming the codemod was correct.

---

## F133 — A transient 429/500 on `/api/auth/me` logs the user out and destroys the session

**Severity:** HIGH (silent, irreversible session loss) · **Status:** FIXED
**Found by:** the route×role sweep — but only after the 429s it produced were correctly attributed

### What was wrong
`frontend/src/store/modules/auth.js`:

```js
async function fetchUser() {
  if (!token.value) return
  try {
    const data = await authService.getMe()
    ...
  } catch (e) {
    logout()          // <-- ANY error, including 429 and 500
  }
}
```

`fetchUser()` is called on app boot (`App.vue:52`, `if (auth.isLoggedIn) auth.fetchUser()`). So **any**
failure of `/api/auth/me` — a rate-limit 429, a transient 500, a dropped connection — clears `token` and
`user` from state *and* `localStorage`, and the user is silently logged out with no explanation.

### The decisive experiment (controlled, three arms)
Session seeded with a valid token, then `/api/auth/me` intercepted:

| Arm | Intercepted response | `token` after | `user` after |
|---|---|---|---|
| A | **500** | **gone** | **gone** |
| B | **429** | **gone** | **gone** |
| C (control) | real (200) | present | present |

The control proves the experiment is sound: the session survives when `/me` succeeds, and is destroyed
when it fails for a reason that has nothing to do with the token being invalid.

### Why this is not just a harness artifact
This was discovered because the route sweep produced cascading `429 /api/auth/me` then `401` on every
subsequent endpoint. The instinct is to call that harness noise — and the 429s *were* partly
harness-induced (my sweep had not flushed the rate-limit bucket). **But the experiment above isolates
the app's response to that 429 and shows it is wrong regardless of who caused it.** A real user on a
shared IP, or behind a flaky connection, hits exactly this path.

### Fix
Distinguish "the token is rejected" (401/403 → log out) from "the server could not answer"
(429/5xx/network → keep the session, retry later):

```js
async function fetchUser() {
  if (!token.value) return
  try {
    const data = await authService.getMe()
    user.value = mapUser(data)
    localStorage.setItem('user', JSON.stringify(user.value))
  } catch (e) {
    const status = e?.response?.status
    // Only an actual auth rejection invalidates the session. A 429 (rate limit), a 5xx or a
    // network drop must NOT log the user out — the token may be perfectly valid.
    if (status === 401 || status === 403) {
      logout()
    }
    // otherwise: keep the cached session; the next successful /me refreshes it.
  }
}
```

### Re-measurement — same three-arm probe, after the fix
| Arm | Intercepted | `token` | `user` | Expected |
|---|---|---|---|---|
| A | **500** | **present** | **present** | keep ✓ |
| B | **429** | **present** | **present** | keep ✓ |
| C | **401** | gone | gone | log out ✓ (a genuinely rejected token must still sign the user out) |
| D | real (200) | present | present | control ✓ |

**Closed.** Arm C is the important control: the fix must not have made the app *incapable* of logging
out on a real auth failure — and it did not.

---

## F134 — A test asserted the exact token that F132 changed

**Severity:** LOW · **Status:** FIXED
`frontend/src/components/common/UserPageHeader.test.js` asserted `wrapper.find('h1 .text-accent')`.
The component's highlighted word is **text**, so F132 correctly moved it to `text-accent-ink`; the
assertion was pinned to the old token.

Fixed by asserting `text-accent-ink` **with a comment recording why** (4.23:1 → 7.10:1), so the next
person does not "fix" it back. Test suite re-run: **109 passed / 1 skipped**.

---

## F135 — Card headings skip a level (`<h3>` directly under `<h1>`)

**Severity:** LOW · **Status:** FIXED · **Found by:** Lighthouse via chrome-devtools MCP

### Measurement
Lighthouse `heading-order` = **0** on `/lessons`:
```
<h3 class="font-black text-lg uppercase leading-snug mt-1 text-foreground">   (lesson card title)
selector: article.group > div.p-5 > div > h3.font-black
```

### Root cause
The page has one `<h1>` (the page title) and the lesson cards jump straight to `<h3>`, skipping `<h2>`.
Assistive tech users navigating by heading level hear a broken outline (WCAG 1.3.1 / 2.4.6).

### Fix
`frontend/src/views/Lessons.vue`: the card title is the **second** level of the page's hierarchy, so it is
now `<h2>`.

### Re-measurement
Lighthouse `/lessons`: `heading-order` **passes**; Accessibility **98 → 100**.

---

## F136 — Pagination buttons violate WCAG 2.5.3 (Label in Name)

**Severity:** LOW · **Status:** FIXED · **Found by:** Lighthouse via chrome-devtools MCP

### Measurement
Lighthouse `label-content-name-mismatch` = **0** on `/lessons`:
```
<button class="pagination-control" disabled aria-label="Trang đầu tiên">1</button>
```
The button's **visible text is "1"** but its accessible name was **"Trang đầu tiên"**, which does not
contain the visible text.

### Root cause
WCAG 2.5.3 requires the accessible name to **contain** the visible label, because speech-input users
say what they see. A sighted user says "click 1"; the control was not addressable by that name. The same
pattern affected the last-page button (`{{ totalPages }}` labelled "Trang cuối cùng: N").

### Fix
`frontend/src/components/common/Pagination.vue` — labels now lead with the visible digit:
`"1 — trang đầu tiên"` and `` `${totalPages} — trang cuối cùng` ``.

### Re-measurement
Lighthouse `/lessons`: `label-content-name-mismatch` **passes**; Accessibility **100**.

The unit test pinning the old labels (`Pagination.test.js`) was updated to assert the new,
WCAG-compliant contract — with a comment recording the reason, so it is not reverted.

---

## F137 — Performance: admin exercise search is slow; the obvious fix does not help

**Severity:** LOW (admin-only, 169 ms) · **Status:** DECLINED with reason (P5), not a defect
**Found by:** the performance probe

`GET /api/admin/exercises?q=the` median **169.3 ms** vs **48.5 ms** without `q`, over 43 735 rows.
Root cause: the query wraps the column in `LOWER()` **and** uses a leading wildcard, so no index can be
used and the plan is a full scan (1294 logical reads).

I measured the obvious micro-optimisation rather than assuming it: the column collation is
`SQL_Latin1_General_CP1_CI_AS` (**CI**), so `LOWER()` is removable, and removing it does give identical
results (16 588 = 16 588 rows) at lower CPU (103 → 84 ms). **But logical reads are identical**, so the
plan does not change — the saving is ~19 ms of CPU against a 169 ms scan, and it would mean rewriting a
shared repository method used by the admin list. **Declined.** The real fix (prefix search or full-text)
changes search semantics and is a product decision, not an audit fix.

Full detail and the before/after table: `evidence/performance-rerun.md`.

---

## F138 — `--geo-muted-fg` passes on white but FAILS on the muted panel it is used on

**Severity:** MEDIUM · **Status:** FIXED · **Found by:** the SECOND pass (the first pass's probe had a
compositing bug that hid these)

### Measurement
The corrected probe found **13 failing text nodes across 19 routes**, all of the same kind:

| Route | Text | Ratio | Needs |
|---|---|---|---|
| `/leaderboard` | "Tổng người học" (12px) | **4.00** | 4.5 |
| `/speaking` | "Đề luyện có sẵn" (12px) | **4.13** | 4.5 |
| `/speaking/history` | "Điểm /10" (12px, ×8) | **4.27** | 4.5 |
| `/admin/lessons` | "Tổng số: 1471" (12px) | **4.21** | 4.5 |
| `/admin/exercises` | "Tổng số: 43735" (12px) | **4.21** | 4.5 |
| `/admin/users` | "Tổng số: 72" (12px) | **4.21** | 4.5 |

### Root cause
`--geo-muted-fg` was `#64748B`, which measures:
```
on #FFFFFF (white)  = 4.76  PASS
on #FFFDF5 (cream)  = 4.67  PASS
on #F1F5F9 (muted)  = 4.34  FAIL
```
**The same token passes on one surface and fails on another — and `muted-fg` is overwhelmingly used
*on* muted panels** (that is what the name means). The token was chosen against white and never checked
against its own background.

### Fix
`--geo-muted-fg` → **`#556070`** (mirrored in `tailwind.config.js` as both `geo.muted-fg` and the legacy
`muted-foreground`), which measures **6.38 / 6.26 / 5.82:1** on white / cream / muted — AA on every
surface the token is actually used against, while still reading as visually secondary.

### Re-measurement — same probe, same 19 routes
```
before: 13 failing nodes
after:   0
```

### Why the first pass missed it, and why that matters
My **first** contrast probe returned the first *fully opaque* ancestor background and discarded
partial-alpha layers. That made it over-report on translucent surfaces (the false `▶` failures, P4) and
**under-report** here — because on a `bg-muted` panel it resolved the parent's `rgba(...)` layer
incorrectly and landed on a lighter composite than reality.

The corrected probe composites **bottom-up** over white. **The second pass found a real defect the first
pass had cleared.** This is the concrete payoff of the user's instruction to run a second, wider round —
and it is why the pass exists rather than being a formality.


---

## F139 — `/login` (and 3 other auth routes) had no `<h1>`

**Severity:** LOW · **Status:** FIXED · **Found by:** Lighthouse on `/login`, which scored **94**

The auth card title was `<h2 class="app-auth__title">` on a **top-level route**, so the page had no
`<h1>` at all — breaking the heading outline for screen-reader navigation (WCAG 1.3.1 / 2.4.6).
`Register.vue` already had an `<h1>` (with different classes); Login/ForgotPassword/ResetPassword did not.

**Fix:** promoted the card title to `<h1>` on all three (Register left as-is).
**Re-measured:** Lighthouse `/login` Accessibility **94 → 100**.

---

## F140 — `/admin/videos` had 5 interactive targets under 24px (WCAG 2.5.8 AA)

**Severity:** LOW · **Status:** FIXED · **Found by:** the tap-target probe on the admin table

Five "Xem" links measured **30×15 px**, below the 24px AA minimum. The `<AppButton>`s beside them were
fine — only the inline links were undersized.

**Fix:** `inline-flex items-center min-h-6` — a real 24px target with the type size, colour and underline
unchanged, so the table looks identical.
**Re-measured:** targets under 24px on `/admin/videos`: **5 → 0** (28 targets total).

---

## F141 — `robots.txt` advertised a `sitemap.xml` that did not exist

**Severity:** LOW · **Status:** FIXED · **Found by:** cross-checking the Lighthouse `is-crawlable` result

`frontend/public/robots.txt` ends with `Sitemap: https://engflow.app/sitemap.xml`, but no such file
existed in `frontend/public/` — a 404 for every crawler that followed the reference.

**Fix:** generated `frontend/public/sitemap.xml` from the public routes in `frontend/src/router/index.js`,
including only routes with **no** `requiresAuth` / `requiresPremium` / `requiresAdmin` meta (`/`,
`/lessons`, `/search`, `/leaderboard`, `/decks`, `/premium`). Auth and admin routes are already
`Disallow`-ed in `robots.txt`, so they are correctly excluded from both.

**Note:** the same Lighthouse run reported `is-crawlable` as failing on `/login`. That is **not a
defect** — `robots.txt` deliberately disallows `/login`, `/register`, `/profile` and `/admin`. Blocking
auth pages from indexing is correct SEO hygiene, and the audit "failure" is that rule working.

---

## F142 — Auth links were distinguishable only by colour (WCAG 1.4.1)

**Severity:** LOW · **Status:** FIXED · **Found by:** Lighthouse `link-in-text-block` on `/login`

The auth links ("Đăng ký", "Quên mật khẩu?", "Đăng nhập") sat **inside a block of text** and differed
from it **only by colour** — measured at **1.11:1** against the surrounding text, with the underline
appearing on hover only. WCAG 1.4.1 requires a non-colour cue (or ≥3:1 contrast) for links inside text.

**Fix:** always-visible underline — `hover:underline` → `underline underline-offset-2 hover:no-underline`,
across 5 occurrences in Login / Register / ForgotPassword / ResetPassword.
**Re-measured:** `link-in-text-block` passes; `/login` Accessibility **100**.

---

## F130 — CLOSED (was OPEN): the harness assertion was the real defect

**Status:** FIXED · **Found by:** re-testing the deferral reason instead of restating it

The six call sites all share one implementation (`sweep/v8/ui/lib.js`; `sweep/v10/ui-lib.js` re-exports
it), so the fix is in one place. **The number 126 was never the bug — the assertion was.**

`after === expected` compares against a hard-coded baseline and therefore cannot distinguish "the sweep
cleaned up after itself" from "the baseline was already wrong". A sweep leaving residue while the
baseline is stale still prints `PARITY OK`.

**Fix:** assert what the function is responsible for — no row *its own date window* could have produced
survives it. A new `AUDIT_REMAINING` count is taken **after** the DELETE and must be 0; the baseline is
demoted to an informational cross-check. All six call sites annotated with the reason.

**Proven** by `sweep/v8/_f130-test.js`, which plants a residue row and passes a deliberately wrong
baseline so the two checks disagree:

```
candidates=1  remaining=0  after=126  baseline=999  -> SELF-CLEAN OK
NEW (remaining===0) -> true    [correct]
OLD (after===999)   -> false   [unusable]
```

**My first attempt at this fix was wrong and the test caught it:** I asserted `candidates === 0`, but
`candidates` is counted *before* the DELETE, so it is non-zero exactly when there was residue to clean —
the check failed on every honest run. A post-delete count was required.


---

## F143 — The yellow hero circle was painted OVER the headline (reported by the user)

**Severity:** MEDIUM (user-visible; the headline is the first thing on the site)
**Status:** FIXED · **Reported by:** the project owner — *"cái vòng tròn màu vàng ở trang chủ đã che mất
chữ học tiếng anh vui vẻ"*

### What was wrong
`.app-hero__sun` carries the comment *"A massive yellow circle **behind** the text"* and
`z-index: 0`. That is not what `z-index: 0` does. In the CSS painting order a **positioned element at
`z-index: 0` paints in step 6**, while **static in-flow inline text paints in step 5** — so the circle
painted *on top of* the headline, exactly the opposite of the comment.

Measured live on `/`:

| Viewport | Sun box | Headline box | Overlap |
|---|---|---|---|
| 1280px | 640×640 at (−96, −58) | 528×205 at (81, 166) | **464 × 205 px** |
| 390px | 544×544 at (−160, −90) | 287×137 at (44, 134) | **100% of the headline** |

At 35% opacity the amber washes over the glyphs. Composited, the text the user is reading is not the
text the stylesheet specifies:

| Text | Specified (text on cream) | **Actually rendered** (amber over glyph) | Needs |
|---|---|---|---|
| HỌC TIẾNG ANH | 12.01:1 | 5.27:1 | 3 (large) |
| **VUI VẺ** | 3.48:1 | **2.64:1** | 3 — **FAIL** |
| subtitle | 5.23:1 | **3.17:1** | 4.5 — **FAIL** |

So the report was precise: "Vui Vẻ" was the worst affected, and the subtitle was also below AA.

### Root cause — and why the two obvious fixes each fail
1. **`z-index: 0` (original)** → paints *after* static text → circle over the words. *(the bug)*
2. **`z-index: -1`** → paints in step 2, which is *behind* step 4, the **opaque cream background** of the
   ancestor `div.bg-background`. The circle **disappeared entirely** — I tried this and the screenshot
   showed no circle at all. Verified by walking the ancestor chain: two ancestors report
   `background: rgb(255,253,245)`.

### The fix
Raise the **text**, not lower the circle:
- `.app-hero__sun` keeps `z-index: 0` — above the ancestor's background, so it stays **visible**;
- `.app-hero__text` gets `position: relative; z-index: 1` — every word now paints above the circle.

The dot-pattern overlay inside the column keeps its own `-z-10`, which is now relative to the raised
column, so it still sits behind the text but above the circle — unchanged visually.

### Re-measured — 4 viewports, with paint order taken into account
| Text | 390 | 768 | 1280 | 1920 | Needs |
|---|---|---|---|---|---|
| HỌC TIẾNG ANH | 12.01 | 12.01 | 12.01 | 12.01 | 3 ✓ |
| **VUI VẺ** | 5.83 | 5.83 | 5.83 | 5.83 | 3 ✓ (was 2.64) |
| subtitle | 5.23 | 5.23 | 5.23 | 5.23 | 4.5 ✓ (was 3.17) |

`sunZ=0`, `textZ=1` at every width. Lighthouse `/` Accessibility **100**. Screenshots:
`sweep/v11/hero-before-1280.png` vs `hero-after2-1280.png`, and the 390px pair.

### The accent colour was ALSO still the unfixed vivid hue
`.app-hero__title-accent` was `color: var(--geo-accent)` (#8B5CF6) — **the F132 codemod never touched
it**, because that codemod scanned `*.vue` files and this rule lives in `assets/app-layout.css`. Over the
amber wash it measured 3.48:1: technically over the 3:1 large-text line, with zero headroom. It now uses
`--geo-accent-ink` (**5.83:1**).

### The same gap existed in 8 more places — found by grepping the CSS, not the components
Scanning the shared CSS for the patterns the codemod missed turned up **9 further violations**:

| File | Rule | Was | Now |
|---|---|---|---|
| `app-layout.css` | `.app-admin__topbar-label` | `--geo-accent` text | `--geo-accent-ink` |
| `app-layout.css` | `.app-marquee__item::after` | `--geo-accent` text glyph | `--geo-accent-ink` |
| `app-layout.css` | `.app-auth__link` | `--geo-accent` text | `--geo-accent-ink` |
| `app-layout.css` | `.app-navbar__mobile-link--active` | white on `--geo-accent` (4.23:1) | white on `--geo-accent-strong` (5.70:1) |
| `app-layout.css` | `.app-navbar__mobile-cta--solid` | white on `--geo-accent` | `--geo-accent-strong` |
| `app-layout.css` | `.app-admin__nav-link--active` | white on `--geo-accent` | `--geo-accent-strong` |
| `app-layout.css` | `.app-features__icon--accent` | white icon on `--geo-accent` | `--geo-accent-strong` |
| `app-layout.css` | `.app-features__icon--secondary` | white icon on `--geo-secondary` (2.65:1) | `--geo-secondary-strong` (4.60:1) |
| `design-system.css` | `.lesson-html em` | `--geo-accent` text | `--geo-accent-ink` |
| `design-system.css` | `.lesson-html summary` | `--geo-accent` on muted panel | `--geo-accent-ink` |
| `design-system.css` | `.lesson-html a` / `a:hover` | accent / secondary text | accent-ink / secondary-ink |
| `design-system.css` | `.app-tabs__tab--active` | `--geo-accent` text | `--geo-accent-ink` |

**The lesson:** F132 was described as "0 failures across 19 routes", and that was true *for the routes
and the DOM states the probe visited* — but the sweep rendered components through Tailwind classes, and
never exercised CSS-only rules like the mobile menu (needs a click), `.lesson-html` prose (needs lesson
content), or the marquee. A route sweep cannot see a rule that only applies to a state it never entered.

### Re-measured after the sweep
All 19 routes, 3 roles: **0 contrast failures, 0 console errors, 0 UI-caused API errors.** Frontend suite
**109 passed / 1 skipped**; build **177.40 kB** (unchanged — these were token swaps).


---

## F144 — Admin shadowing player AUDIBLY played recordings at 16x (owner-reported)

**Severity:** HIGH (user-visible, reported by the owner) · **Status:** FIXED
**Report:** *"khi tôi vào admin chấm shadowing thì nghe thấy âm thanh lạ chạy rất nhanh không nghe rõ"*

### Root cause — two independent mistakes in one 12-line helper
`AdminVideoAttempts.vue` had an inline `fixWebmDuration` whose job was cosmetic: MediaRecorder
WebM has no Duration element, so the native control shows "∞", and seeking past the end makes the
browser compute one.

```js
if (Number.isFinite(el.duration) && el.duration > 0) return   // (1)
const originalRate = el.playbackRate
el.onended = () => { ...; el.playbackRate = originalRate }
el.currentTime = 1e10
el.playbackRate = 16                                          // (2)
el.play().catch(() => {})                                     // (2)
```

**(1) The guard skips only FINITE durations.** A MediaRecorder WebM reports
`duration === Infinity` — the exact case the helper exists for — so the guard fell straight through.
Measured on the live page: a 358 KB real recording reported `Infinity`, and `Infinity`/`NaN` are the
only inputs that reach the workaround.

**(2) `playbackRate = 16` + `play()` is AUDIBLE.** The trick only needs the element to advance its
timeline; it never needed to be heard, and it never restored the rate unless `ended` fired.

That is the "âm thanh lạ chạy rất nhanh": the admin's click on the native play control triggered a
hidden 16× playback of the learner's recording.

### Fix
Extracted to a tested module `frontend/src/utils/webmDuration.js`:
- mutes the element **before** the accelerated seek and restores the previous mute state after;
- restores `playbackRate` and `currentTime` in a `restore()` that runs on `ended`, on a 1 s safety
  timer, and on a rejected `play()` — so it cannot get stuck at 16×;
- never leaves the element playing;
- if the browser cannot compute a duration this way, it is simply left as-is: a player showing "∞"
  is cosmetic, a player screaming at 16× is not.

### A SECOND copy of the same bug — on the student-facing page
`views/videos/VideoLesson.vue:552` had a byte-identical copy, so **students heard their own recording
at 16× too** when reviewing it. Both now import the shared module. Grep confirms no `playbackRate = 16`
remains outside the module's own comment and implementation.

### Verified three ways
1. **Unit test** (`utils/webmDuration.test.js`, 8 cases). Run against the **old** body it fails
   exactly as the user described: `expected 16 to be 1` for `Infinity` and for `NaN`. Run against the
   fix: 8/8 pass. The test is a genuine regression guard, not a restatement of the code.
2. **Live browser, forced `duration = Infinity` on a real page element**, event trace:
   ```
   rate → 16  →  play(rate=16, muted=TRUE)  →  pause  →  rate → 1
   audibleFastPlay: 0        leftAt16x: false
   ```
   Previously that same event played **unmuted at 16×**.
3. Full suite: **117 passed** (was 109; +8 new), build clean.

### Two more defects found while measuring, recorded but NOT fixed
| Observation | Evidence | Why not fixed here |
|---|---|---|
| Recordings are served as **`application/octet-stream`** | object keys have **no file extension** (`…/3790338b-248d-…`), so every `endsWith(".webm")` branch in `MediaProxyController` misses; response header confirmed `Content-Type: application/octet-stream` with `X-Content-Type-Options: nosniff` | **Not the cause of this bug** — I tested the same bytes as `audio/webm`, `application/octet-stream` and `""`: all three decoded identically (duration `Infinity`, correct playback). It is a latent fragility, not the reported defect, so it is recorded rather than bundled into this fix |
| Some attempts are **44-byte / 1644-byte stubs** reporting `duration = 0.1` | 6 of the 15 attempts; a real one is 358 KB | Test data left by earlier probes, not a product defect. The 0.1s files take the early-return path and are unaffected by F144 |

---

## F145 — AI-generated vocabulary is saved into a void (owner-reported "chưa có sự liên kết")

**Severity:** MEDIUM (a feature that appears to work and delivers nothing) · **Status:** FIXED
**Report:** *"có một số chức năng có vấn đề và chưa có sự liên kết với nhau"*

### What is wrong
`AiVocabService.saveVocabBatch(requests)` builds `Vocabulary` rows with `source = "AI_GENERATED"` and
**nothing else that ties them to a person or a container**:

```java
Vocabulary v = Vocabulary.builder()
        .word(req.getWord()).meaning(req.getMeaning())...
        .source("AI_GENERATED")
        .build();          // no lesson_id, no deck, no owner — and the method takes no userId
```

The endpoint is `POST /api/ai/save-vocab` and it charges the user **1 AI quota credit** (F60, capped at
50 words), then returns the rows. The UI reports success:

```
'Đã lưu ' + unsaved.length + ' từ vào DB!'
```

…and that is literally all that happens. Measured in the live DB:

```
ai_vocab_total          = 31
ai_vocab_in_a_deck      = 10
ai_vocab_orphan_no_deck = 21     <- 68% saved into a void
```

### Why it is unreachable
The saved rows have `lesson_id = NULL` and no `deck_words` entry, and there is **no owner column** on
`vocabulary`. So:
- `/api/decks/my` — the user's decks — cannot contain them (they are in no deck);
- `/api/vocabulary` and `/api/vocabulary/search` are **global** tables, not user-scoped, so the words
  are not "the user's" anywhere;
- no endpoint or view lists "words I generated".

**The user pays a quota credit, is told the save succeeded, and has no way to ever see the words
again.** The 10 that *are* in a deck are seeder-created (`VocabularyDataSeeder`), not user-saved.

### Fix
`save-vocab` now also accepts an optional `deckId`; when supplied, each saved word is added to that
deck (owner-checked via the existing `DeckService.addWordToDeck`), so the words land somewhere the
user can actually reach. When no deck is given, the response says so instead of implying the words are
now available.

*(Implementation and verification recorded in `evidence/ai-integration-fix.md`.)*

### Also recorded — one AI endpoint with no UI at all
`POST /api/admin/exercises/ai/backfill-answers` (+ `/status`) has **0 frontend callers** — it exists,
is documented in `AGENTS.md`, and no screen reaches it. Classified as an **orphaned admin tool**, not a
defect: it is documented as an operator-run backfill, not a user feature.
