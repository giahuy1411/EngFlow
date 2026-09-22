# audit-v13-full — Phase 3: UI sweep

**Stack**: backend `http://localhost:8080` (Spring Boot), frontend `http://localhost:5173` (Vite dev),
SQL Server `engflow-sqlserver`, Redis `engflow-redis`.
**Drivers**: Playwright (chromium-1237, headless) for the full matrix; Chrome DevTools MCP as an
independent second browser for confirmation, `document.fonts`, and Lighthouse.
**Date**: 2026-09-22 (+07). **Login**: `user@gmail.com` / `admin@gmail.com`, password `123456`.
**Method**: real HTTP, real DB rows, real DOM reads. No mocks.

> **Concurrency note.** Another process was auditing the same tree at the same time and **fixed the
> contrast defects live while this sweep ran**. My first pass measured 7 contrast failures; the
> final re-run measured **0**. Both numbers are reported below with before/after ratios, and the
> three named "suspect" sites were re-checked and confirmed as false positives. That process also
> rewrote this file; its version is preserved at `ui-sweep.vi.md`.

## Summary (final re-run)

| Axis | Result |
|---|---|
| Route × role matrix | **117 cells** (39 routes × 3 roles), **0 guard failures** |
| Uncaught console errors | **0** |
| Unexpected API 4xx/5xx | **0** |
| Responsive overflow | **0** of 35 cells (360/768/1280/1440/1920) |
| Contrast failures (AA) | **0** (7 found on first pass; all fixed) |
| Missing `alt` attribute | **0** |
| Elements with no accessible name | **0** |
| Tap targets < 24px (AA 2.5.8) | **18 distinct** (mostly inline-text-link exceptions) |
| Design-system token conformance | PASS (live tokens match the spec) |
| Font `Be Vietnam Pro` | PASS — `document.fonts.check('16px "Be Vietnam Pro"') === true` |
| Heading order | **1 real issue** — `/lessons/:id` has no `h1` |

---

## 1. Route × role matrix

Enumerated from `frontend/src/router/index.js`. Each cell seeds **both** `localStorage.token`
**and** `localStorage.user` (`seedAuth`) before navigating, so the admin guard is actually
exercised rather than silently bouncing on a missing `isAdmin`.

**Guard semantics** (`router.beforeEach`, lines 202-226): the `requiresPremium` check runs
**before** the `requiresAuth` check. So an anonymous visitor to `/videos` or `/speaking` — routes
carrying *both* flags — lands on `/premium?redirect=…`, **not** `/login`. This is correct
behaviour; the 6 cells that initially looked like mismatches are exactly this ordering and are
recorded as PASS with a note.

Selected cells (full data in `ui-sweep.json`):

| route | anon | student | admin |
|---|---|---|---|
| `/` | `/` | `/` | `/` |
| `/login` (guestOnly) | `/login` | `/lessons` | `/lessons` |
| `/lessons/:id` | `/lessons/445` | `/lessons/445` | `/lessons/445` |
| `/videos` (premium+auth) | `/premium?redirect=/videos` | `/videos` | `/videos` |
| `/profile` (auth) | `/login` | `/profile` | `/profile` |
| `/decks/:id` (auth) | `/login` | `/decks/10006` | `/decks/10006` |
| `/speaking` (premium+auth) | `/premium?redirect=/speaking` | `/speaking` | `/speaking` |
| `/admin` | `/login` | `/` | `/admin/dashboard` |
| `/admin/users` | `/login` | `/` | `/admin/users` |
| `/admin/:id/build` | `/login` | `/` | `/admin/445/build` |
| `/definitely-not-a-route` | `/` | `/` | `/` |

**Result: 117/117 PASS.** No route leaks an admin or premium surface to the wrong role.
`/lessons` and `/lessons/:id` are reachable anonymously by design (`requiresAuth: false`).

---

## 2. Console + network health

**0 uncaught errors, 0 unhandled rejections, 0 unexpected API 4xx/5xx** across all 117 cells.

> **Artifact, not a defect.** A first pass recorded 15 console errors of the form
> `Failed to load resource: net::ERR_EMPTY_RESPONSE`. An external `docker compose up -d` replaced
> the `engflow-backend` container mid-sweep (docker events: `container kill … signal=15`,
> `signal=9`, `exitCode=137`, new image tag), so the backend was briefly unreachable. After the
> stack stabilised the clean re-run recorded **0**. Recorded so nobody "fixes" a non-existent bug.

One deterministic **request failure** does reproduce and is a real (minor) defect — Finding 4.

---

## 3. Responsive

35 cells (7 routes × 5 widths). Overflow measured as
`document.documentElement.scrollWidth - document.documentElement.clientWidth`.

**0 cells overflow.** Every cell reports `-15` — Chromium reserves ~15px for the scrollbar via
`scrollbar-gutter: stable`, which `design-system.css` sets on `html`. A secondary per-element scan
(`getBoundingClientRect().right > clientWidth + 1`) found no offending element either.

The Chrome DevTools MCP window has a ~500px floor and cannot render 360, so 360 evidence is from
Playwright (which honours the viewport exactly); MCP confirmed 1440 independently.

---

## 4. Accessibility (WCAG 2.2)

Method: `!el.hasAttribute('alt')` for images (`alt=""` is VALID and is not counted);
`min(width, height) < 24px` for the **AA** tap-target floor (2.5.8).

- **Missing `alt` attribute: 0.** Unnamed buttons/links: **0**.
- **`lang="vi"`** on every route. **Skip link** present on every route.
- **Heading order**: one real issue — `/lessons/:id` renders **no `h1`** and starts at `h3`.
  Every other audited route has exactly one `h1`.

### Tap targets < 24px — 18 distinct

The WCAG 2.5.8 exception for **inline text links** applies to most of these. *Effective* targets
were also measured: a checkbox's wrapping `<label>` is the real click target and is large
(`/login` label `80×16`, `/decks/create` label `764×20`), so the 16×16/20×20 inputs are covered.

| route | element | size | note |
|---|---|---|---|
| `/login` | checkbox `input` | 16×16 | wrapped by `80×16` label → not a violation |
| `/decks/create` | checkbox `input` | 20×20 | wrapped by `764×20` label → not a violation |
| `/login` | `QUÊN MẬT KHẨU?` | 120×16 | inline text link — exception |
| `/login` | `Đăng ký` | 58×18 | inline text link — exception |
| `/register` | `Đăng nhập` | 77×18 | inline text link — exception |
| `/forgot-password` | `Quay lại đăng nhập` | 136×18 | inline text link — exception |
| `/videos` | `đăng nhập` | 75×18 | inline text link — exception |
| `/decks/:id` | `QUAY LẠI` | 98×20 | standalone nav link — **below 24** |
| `/decks/:id/play/mixed` | `← QUAY LẠI` | 94×20 | standalone nav link — **below 24** |
| `/speaking/:id` | `← DANH SÁCH LUYỆN NÓI` | 171×22 | standalone nav link — **below 24** |
| `/speaking/:id/record` | `← QUAY LẠI ĐỀ BÀI` | 128×22 | standalone nav link — **below 24** |
| `/videos/:id` | transcript word buttons | 8×26 … 24×26 | inline word buttons in a `<p>` |

The four standalone "back" nav links (20-22px tall) are the least defensible: they are navigation
affordances, not inline text links. See Finding 2. The transcript word-buttons are inline within a
paragraph (exception) but 8px wide is very small for a pointer.

---

## 5. Design-system conformance + live contrast

Live tokens from `getComputedStyle(document.documentElement)` on three routes — all match
`design-system.css`:

| token | live value |
|---|---|
| `--geo-bg` | `#FFFDF5` |
| `--geo-fg` | `#1E293B` |
| `--geo-muted` | `#F1F5F9` |
| `--geo-muted-fg` | `#556070` |
| `--geo-accent` / `-strong` / `-ink` | `#8B5CF6` / `#7C3AED` / `#6D28D9` |
| `--geo-secondary-strong` | `#DB2777` |
| `--geo-tertiary-ink` | `#B45309` |
| `--geo-quaternary-ink` | `#047857` |
| `--geo-radius-md` / `--geo-border-width` | `16px` / `2px` |
| body font | `"Be Vietnam Pro", system-ui, sans-serif` |

- `document.fonts.check('16px "Be Vietnam Pro"')` → **true** on every audited route.
- Lucide icons carry `stroke-width: 2.5px` as the spec mandates.
- `scrollbar-gutter: stable` is applied (the CLS fix).

### Contrast (AA 1.4.3), alpha composited bottom-up

`effectiveBg()` walks ancestors collecting every background layer and composites them bottom-up
over white, so a `bg-danger/10` tint resolves to what the eye sees (`rgb(252,231,228)`), not to the
nearest opaque ancestor. Large text = `≥24px`, or `≥18.66px & weight ≥700`.

**Measured during this audit (first pass) — all now FIXED:**

| id | element | fg | effective bg | before | after | need |
|---|---|---|---|---|---|---|
| F13-01 | `text-danger` on `bg-danger/10` | `rgb(225,29,72)` | `rgb(252,231,228)` | **3.95** | 12.29 | 4.5 |
| F13-02 | logo `B` (`text-white` on `bg-accent`) | `rgb(255,255,255)` | `rgb(139,92,246)` | **4.23** | 5.70 | 4.5 |
| F13-02 | close btn `text-white/40` | `rgba(255,255,255,0.4)` | `rgb(30,41,59)` | **3.61** | — | 4.5 |
| F13-02 | close btn `text-foreground/40` | `rgba(30,41,59,0.4)` | `rgb(255,255,255)` | **2.35** | — | 4.5 |

F13-01 was reproduced live on three independent routes (API aborted via `page.route`):
`/admin/dashboard` ("Không tải được dữ liệu dashboard."), `/decks` ("Không tải được danh sách bộ
từ."), `/leaderboard` ("Không tải được bảng xếp hạng.") — all **3.95:1**. `#E11D48` is fine on its
own (4.61:1 on cream, 4.70:1 on white); only the `/10` tint drops it below AA, and the design system
had an ink layer for every vivid hue *except* danger. The concurrent process added
`--geo-danger-ink: #BE123C` and switched the text to `text-danger-ink` → **12.29:1**.

**The named suspects were checked and are FALSE POSITIVES — they already use the `-ink` layer:**

| site | token used | measured | verdict |
|---|---|---|---|
| `AdminDashboard.vue:19-20` | `text-tertiary-ink` / `text-quaternary-ink` | `#B45309` → **5.02:1** | PASS |
| `Profile.vue:44` | `text-tertiary-ink` | `#B45309` → **5.02:1** | PASS |
| `AdminLayout.vue:28,66` (`text-tertiary` on dark sidebar) | `#FBBF24` on `rgb(30,41,59)` | **8.76:1** | PASS |

The brief's warning was right: the dark sidebar makes amber pass at 8.76:1, and the
dashboard/profile had already migrated to `-ink`. No false positive was reported.

---

## Findings

**F13-01 (MEDIUM, FIXED during audit) — `text-danger` on `bg-danger/10` measured 3.95:1**
`--geo-danger` `#E11D48` is 4.61:1 on cream and 4.70:1 on white — both fine. But on its own `/10`
tint over cream (`rgb(252,231,228)`) it dropped to **3.95:1**, under AA. The design system had an
ink/strong layer for accent/secondary/tertiary/quaternary/success/warning but **no `danger-ink`**.
Reproduced live on `/admin/dashboard`, `/decks`, `/leaderboard` with the API aborted. Pattern used
in ~10 components (`Leaderboard.vue:23`, `Decks.vue:38`, `DeckDetail.vue:16`,
`AdminDashboard.vue:9`, `AdminVideoAttempts.vue:23`, `AdminVideoLessons.vue:83`,
`AdminSpeakingSubmissions.vue:189`, `AiGeneratePanel.vue:94`, …). Fix applied concurrently:
`--geo-danger-ink: #BE123C` + `text-danger-ink` → **12.29:1**.

**F13-02 (LOW, FIXED during audit) — `AdminLessonBuilder.vue` contrast, 3 elements**
`bg-accent` (`#8B5CF6`) + `text-white` = **4.23:1** (v11 F132 moved buttons to `--geo-accent-strong`
`#7C3AED`, 5.70:1, but this logo tile still used raw `bg-accent`); `text-white/40` on the dark
header = **3.61:1**; `text-foreground/40` on white = **2.35:1**. Fix applied concurrently:
`bg-accent` → `bg-accent-strong` (→ 5.70:1) and the `/40` opacities raised.

**F13-03 (LOW, OPEN) — `/lessons/:id` has no `h1`; heading order starts at `h3`**
`/lessons/445` renders headings `h3, h3, h3, h3, h3, h3` and no `h1`. `LessonContent.vue` renders
scraped lesson HTML via `v-html` and the only heading it owns is an `h2`
(`#lesson-speaking-title`). WCAG 2.4.6 / 1.3.1: no top-level heading, and a level is skipped.
Fix: give `LessonLayout.vue` an `<h1>` with the lesson title.

**F13-04 (LOW, OPEN) — leaderboard renders a broken avatar; no `onerror` fallback**
`users.avatar_url` for `testaudit2026` (id 150011) is `https://example.com/avatar.png` — a
placeholder left by a prior test. `Leaderboard.vue:45` renders
`<img :src="user.avatarUrl || dicebear…">`; because the value is non-empty the dicebear fallback is
never used and the request fails `net::ERR_BLOCKED_BY_ORB`. Confirmed in the live DOM:
`src="https://example.com/avatar.png" complete=true naturalWidth=0 naturalHeight=0` (1 of 21 images
broken). Fix: add `@error` to swap in the fallback, and/or clean the row.

**F13-05 (INFO, OPEN) — premium redirect parameter is generated but never consumed**
`router/index.js:206` sends `next('/premium?redirect=' + encodeURIComponent(to.fullPath))`, and
`LearningPath.vue:95` links to `/premium?redirect=%2Fspeaking`. **No file reads
`route.query.redirect`** (grep across `frontend/src` returns nothing; `main.js`/`App.vue` have no
global handler). A user bounced from `/speaking` who then logs in is not returned to `/speaking`.
`LearningPath.test.js:38` asserts the href is produced, so the param looks wired but has no
consumer. Behavioural gap, not a WCAG issue.

---

## Data hygiene

`/premium/checkout` calls `POST /api/v1/payment/create-order` on mount, so each student/admin visit
mints a real `payment_transactions` row. The sweep visited it twice (student + admin), creating
**2 PENDING rows** (`id` 80281 `ENG83F51E33D9AD`, 80282 `ENG51C765C5EFD9`, `created_at`
2026-09-22 03:49:30/33, `transaction_id IS NULL`). Both were **deleted by listed ID** (never
`LIKE`), scoped to `status <> 'SUCCESS' AND transaction_id IS NULL` so a genuine payment could
never match. Cleanup count = **2**, `TODAY_REMAINING=0`, output scanned for `Msg \d+` = 0 matches.
**Parity restored to baseline: `1470|43735|72|118|29|15|4|126|10|5`.**

## Evidence files

- `ui-sweep.json` — 117-cell matrix, 35 responsive cells, design tokens, per-route a11y.
- `a11y-audit.json` — a11y per route (alt, names, targets, headings, lang, skip link).
- `design-system.json` — token conformance + live contrast measurements.
- `focused-probe.json` — forced-error and suspect-site measurements.
- `ui-sweep.vi.md` — the concurrent process's version of this file, preserved.

## Screenshots (3-5+ as required)

- `shots/v13-home-1440.png`
- `shots/v13-lessons-360.png`
- `shots/v13-admin-builder-contrast.png` (the 4.23:1 logo, pre-fix)
- `shots/v13-mcp-leaderboard-broken-avatar.png` (broken avatar, MCP)
- `shots/v13-mcp-admin-dashboard-1440.png` (MCP, 1440)
- `shots/v13-mcp-admin-dashboard-360.png` (MCP, narrow)
