# Skeptic #2 — REFUTATION ATTEMPT: "Premium redirect query parameter is generated but never consumed"

**Verdict: NOT REFUTED (claim holds). Claimed severity LOW is justified.**

## Method
Read source exhaustively + reproduced the full guest→premium→login flow live with
Chrome DevTools MCP against the running stack (frontend :5173, backend :8080).

## Source evidence (exhaustive)

`grep -rn "redirect" frontend --exclude-dir=node_modules --exclude-dir=dist` returns
exactly 5 hits, and none is a consumer:

| file:line | content | role |
|---|---|---|
| router/index.js:178 | `{ path: '', redirect: '/admin/dashboard' }` | unrelated route-level redirect |
| router/index.js:193 | `redirect: '/'` | unrelated catch-all route |
| router/index.js:206 | `next('/premium?redirect=' + encodeURIComponent(to.fullPath))` | **generator** |
| components/home/LearningPath.vue:95 | `to: '/premium?redirect=%2Fspeaking'` | **generator** |
| components/home/LearningPath.test.js:38 | `expect(links).toContain('/premium?redirect=%2Fspeaking')` | asserts href produced only |

`grep -rn "\.query" frontend/src` returns exactly one consumer of any query param:
`views/premium/PremiumCheckout.vue:57  const planType = ref(route.query.plan || 'MONTH')`.
**`query.redirect` is read nowhere.**

Router has exactly one hook — `router.beforeEach` at router/index.js:202 (no
afterEach/beforeResolve). `main.js` and `App.vue` contain no redirect handler.
No `sessionStorage`/`localStorage` persistence of the redirect in router, Login, or main.

## Live reproduction (Chrome DevTools MCP, isolatedContext `refute-guest`, no auth)

1. Navigate `http://localhost:5173/speaking` (guest, meta requiresPremium:true) →
   landed at `http://localhost:5173/premium?redirect=/speaking` — **param generated**. ✔
2. On that page, both login links resolve to plain `/login`
   (`document.querySelectorAll('a[href="/login"]')` → `"/login"`; the param is already dropped).
3. Navigate `http://localhost:5173/login?redirect=%2Fspeaking`, log in as
   `user@gmail.com / 123456` (login succeeded, token issued) → final URL
   `http://localhost:5173/lessons`. **The user is NOT returned to /speaking.** ✔

## Answers to the skeptic questions
- **Probe artefact?** No. The guard redirect is deterministic and source-visible; the live
  login flow reproduced the dead-end. Login returned a token (no 429 self-inflicted);
  the final URL was read directly (not inferred from a scrollbar/overflow reading).
- **Severity justified?** Yes, LOW. Pure UX dead-end; no data/security impact. The
  un-consumed param is also why it is not an open-redirect vector (nothing reads it).
- **Correct behaviour?** No. The guard and the CTA deliberately encode a return path and
  the unit test asserts the href is produced — clear intent of post-login return. With no
  consumer the intent is unfulfilled: dead code, not intended behaviour.

## Root cause (confirmed)
router/index.js:206 (and LearningPath.vue:95) emit `redirect=<encoded fullPath>`, but no
component reads `route.query.redirect`; Login.vue:66 unconditionally `router.push('/lessons')`.
