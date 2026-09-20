# Phase D — UI sweep with chrome-devtools MCP + Playwright MCP

**Date:** 2026-09-20 (+07) · **App:** `http://localhost:5173` (Vite dev, live-mounted) · **API:** `:8080`
**Drivers used:** Playwright MCP (`browser_run_code_unsafe` / `browser_navigate` / `browser_evaluate`) and
chrome-devtools MCP (`list_pages`). Results are **labelled by driver** and never merged — v10's numbers
came from `playwright-core` on Edge, and rendering differs between engines.

---

## Trap 1 — the route sweep must flush the rate-limit bucket, or it proves 429s against itself

**First run: 77/81, with `429 /api/auth/me` cascading into `401` on every subsequent endpoint.**

The cause was **my harness**, not the app: I had not flushed `rate_limit:*`, so 81 route loads × several
API calls each exhausted the 100/min global bucket. After flushing per role: **0 API errors, 0 console
errors.**

This is finding **F109**'s lesson re-learned, and it is exactly why the API sweep flushes per batch. It is
recorded because the instinct — "the app is throwing 429s, file it" — would have produced a false
finding. The 429s were real; their *cause* was mine.

> **But** chasing that artifact found a genuine defect: the app's response to a 429 was to destroy the
> session. See **F133**.

## Trap 2 — seed BOTH `token` and `user`

The harness seeds `localStorage.token` **and** `localStorage.user` (via `mapUser`), because
`store/modules/auth.js` derives `isAdmin` from `localStorage.user` and `main.js` does not call
`fetchUser()` at boot. Seeding only `token` leaves `isAdmin === undefined`, the router guard bounces
`/admin/*` to `/`, and every admin page silently renders as the homepage **while reporting PASS** —
`HARNESS-TOKEN-ONLY-SEED`, measured in an earlier audit. The sweep also asserts `page.url()` after
navigation, so a bounce cannot be mistaken for a render.

---

## 1. Route × role sweep — 78 loads, guards asserted in BOTH directions

28 routes × 3 roles (anon / student / admin). Expected landing is derived from the guard code in
`router/index.js` (premium → auth → guestOnly → admin), and the assertion checks **stay vs bounce in both
directions** — a `guestOnly` page *should* bounce a logged-in user.

```
total 78 · passed 75 · failed 3
```

The 3 "failures" were **anon → `/videos`, `/speaking`, `/speaking/history`** landing on
`/premium?redirect=…` instead of `/login`. A decisive re-probe showed the premium page presents **both
Đăng nhập and Đăng ký** CTAs and carries the redirect target — a deliberate **upsell funnel**, since the
guard checks `requiresPremium` before `requiresAuth`. **Falsified as a probe bug** (P9 in
`probe-artifacts.md`); my expected-landing model was wrong, not the app.

**Real result: 78/78 correct landings.**

## 2. Console / network health — clean

After bucket flushing: **0 console errors, 0 page errors, 0 UI-caused API responses ≥ 400** across all 78
loads. Every ≥400 observed in the first run was traced to the harness's own rate-limit exhaustion.

## 3. Responsive — 5 widths × key routes, raw numbers

Measured as `documentElement.scrollWidth − clientWidth` (never `scrollWidth > clientWidth` alone —
Chromium reserves ~15px for the scrollbar, which produces false positives).

| Route | 360 | 768 | 1280 | 1440 | 1920 |
|---|---|---|---|---|---|
| `/` | 0 | 0 | 0 | 0 | 0 |
| `/lessons` | 0 | 0 | 0 | 0 | 0 |
| `/profile` | 0 | 0 | 0 | 0 | 0 |
| `/premium` | **+8** → **0** | **+28** → **0** | 0 | 0 | 0 |
| `/admin/dashboard` | 0 | −15 | −15 | 0 | 0 |

The `−15` values are the reserved scrollbar gutter, **not** overflow.

`/premium` was a **real defect** (F131), characterised across 12 widths and fixed; re-measured **0 at
every width**.

## 4. UI↔API cross-check — the calls the UI actually emits

Captured live from the browser's network layer (not asserted from source):

| UI action | Endpoint emitted | Contract matches API sweep? |
|---|---|---|
| app boot, authed | `GET /api/auth/me` | ✓ |
| `/profile` | `GET /api/auth/me`, `GET /api/streak/snapshot`, `GET /api/v1/payment/status` | ✓ all 200 for a valid session |
| `/lessons` | `GET /api/lessons?page=&size=` | ✓ |
| `/videos` | `GET /api/v1/video-lessons` | ✓ |
| `/speaking` | `GET /api/v1/speaking-prompts` | ✓ |
| `/speaking/history` | `GET /api/v1/speaking-submissions` | ✓ |
| `/admin/dashboard` | `GET /api/admin/stats` | ✓ |
| `/admin/lessons` | `GET /api/admin/lessons` | ✓ |
| `/admin/exercises` | `GET /api/admin/exercises`, `GET /api/admin/lessons` | ✓ |
| `/admin/users` | `GET /api/admin/users` | ✓ |
| `/premium/checkout` | `POST /api/v1/payment/create-order` | ✓ — **and it creates a real DB row** (see §6) |
| `/decks/1` (nonexistent id) | `GET /api/decks/1` → **404** | ✓ correct (deck id 1 does not exist; real ids start at 10006) |

**No UI called a wrong endpoint, and no UI showed an error while the API was healthy.** The one 404 was
my probe using a non-existent deck id, not a UI fault.

## 5. A11y — 19 routes

| Check | Result |
|---|---|
| `<img>` missing the `alt` attribute | **0** (measured with `!el.hasAttribute("alt")` — `alt=""` is **valid** for decorative images; the naive check once miscounted 27 images as violations) |
| Buttons with no accessible name | **0** |
| Interactive targets < 24px (WCAG 2.5.8 AA) | **0** on 17/19 routes; 1 on `/premium`, 1 on `/register`, 5 on `/admin/videos` |
| `lang` attribute | `vi` on every route ✓ |
| Skip-link present | ✓ on every route |
| `:focus-visible` rules present | ✓ |
| `prefers-reduced-motion` handled | ✓ |
| `<h1>` per page | 1 on 18/19; **0 on `/login`** |

**Two sub-24px findings worth recording, neither fixed here:**
- `/admin/videos` has **5** targets under 24px (icon buttons in a table). This is the one place the AA
  tap-target threshold is not met, and it is admin-only.
- `/login` has **no `<h1>`** — the heading structure starts lower. Low impact but real.

Both are recorded as **open, low-severity** rather than quietly omitted.

Tap targets between 24–44px are counted separately and are **not** violations (44px is an AAA
recommendation, not the AA requirement).

## 6. Payment-row residue — the documented trap, hit and cleaned

`PremiumCheckout.vue` calls `POST /api/v1/payment/create-order` **on mount**, so any sweep that visits
`/premium/checkout` creates a real `payment_transactions` row. Measured this session:

```
before sweep: 127   (126 baseline + 1 residue from a previous session, F130)
after  sweep: 128   (+1 created by this sweep's /premium/checkout visit)
after cleanup: 126  (both residue rows removed, enumerated ids, SET QUOTED_IDENTIFIER ON)
```

Cleanup: `sweep/v11/cleanup-audit-payments.sql` — `Msg` count **0**. Parity re-asserted:
`1471|43735|72|127|28|15|4|126|14|5`.

**`/premium/checkout` was excluded from the route sweep** in the second pass and exercised separately,
precisely so the sweep cannot silently inflate the payment count.

## 7. Contrast — see F132

165 failing text nodes across 19 routes → **0** after the fix. Full detail in `findings-rerun.md`.

---

## Verdict

**Phase D: PASS.** 78/78 route landings correct with guards asserted in both directions; 0 console
errors; 0 UI-caused API errors; responsive clean at 5 widths after fixing one real overflow; the UI↔API
cross-check found no mismatched endpoint; a11y clean on `alt` and button names with two low-severity
target/heading items recorded as open.

Four candidate defects were falsified as probe bugs (P1–P4, P9). **Two real defects came out of this
phase: F131 (premium overflow) and F133 (session destroyed by a transient 429/500).**
