# Route Map — EngFlow Frontend

**Task:** comprehensive-audit-redesign Task 5/10 · **Date:** 2026-09-16
**Source of truth:** `frontend/src/router/index.js` (228 lines), parsed by
`sweep/v8/route_inventory.py` → `evidence/route-inventory.json`
**Cross-check:** `sweep/v8/route_verify.py` → **VERDICT: CONSISTENT** (exit 0)
**Runtime evidence:** `sweep/v8/ui/routes-all.js` → `sweep/v8/ui/routes-all.txt`

## Why this file is generated, not hand-written

A hand-maintained route list drifts: a route added to the router stays absent
from the audit, and a route deleted from the router keeps being "verified".
`route_inventory.py` reads the router source and replays `beforeEach`'s guard
precedence, and `routes-all.js` consumes the generated JSON, so the inventory and
the runtime sweep cannot disagree. If a route is added to the router, it is
swept automatically on the next run.

Two parser bugs were found and fixed while building this, both of which would
have produced a wrong number in the report:

- the catch-all `/:pathMatch(.*)*` was emitted by the parser *and* appended
  manually → reported **40** routes; the true count is **39**;
- the guard resolver originally treated a route with no `meta` block as public.
  `requiresAuth` is enforced by the global `beforeEach`, and `/admin`'s
  `requiresAdmin` is inherited by all 9 children, so those routes are not public.

The parser now refuses to emit a result at all on a duplicate path or on a named
route with neither component nor redirect (`sys.exit(2)`), and `route_verify.py`
reconciles the parsed set against the raw `path:` literals in the source.

## Totals

| Metric | Value |
|---|---|
| Routes parsed | **39** |
| Named routes | **37** |
| Path literals in source | 40 (incl. 10 relative child paths) |
| Duplicate paths | 0 |
| Parse ↔ source reconciliation | CONSISTENT (0 missing, 0 extra) |

### By guard

| Guard | Routes | Behaviour when not satisfied |
|---|---|---|
| `public` | 8 | none |
| `guestOnly` | 4 | logged-in → `/lessons` |
| `auth` | 11 | → `/login` |
| `auth+premium` | 6 | non-premium non-admin → `/premium?redirect=<fullPath>` |
| `admin` | 9 | non-admin → `/` |
| `admin-redirect` | 1 | `/admin` → `/admin/dashboard` |

Guard precedence in `router/index.js` is checked in this order, first match wins:
`requiresPremium` → `requiresAuth` → `guestOnly` → `requiresAdmin`. A premium
route therefore redirects to `/premium` even for a logged-out visitor, before the
`requiresAuth` check runs.

## Full route table

| # | Path | Name | Component | Guard |
|---|---|---|---|---|
| 1 | `/` | Home | `Home.vue` | public |
| 2 | `/login` | Login | `Login.vue` | guestOnly |
| 3 | `/register` | Register | `Register.vue` | guestOnly |
| 4 | `/forgot-password` | ForgotPassword | `ForgotPassword.vue` | guestOnly |
| 5 | `/reset-password` | ResetPassword | `ResetPassword.vue` | guestOnly |
| 6 | `/lessons` | Lessons | `Lessons.vue` | public |
| 7 | `/lessons/:id` | LessonDetail | `lessons/LessonLayout.vue` | public |
| 8 | `/videos` | VideoLibrary | `videos/VideoLibrary.vue` | auth+premium |
| 9 | `/videos/:id` | VideoLesson | `videos/VideoLesson.vue` | auth+premium |
| 10 | `/profile` | Profile | `Profile.vue` | auth |
| 11 | `/search` | Search | `SearchVocabulary.vue` | public |
| 12 | `/leaderboard` | Leaderboard | `Leaderboard.vue` | public |
| 13 | `/decks` | Decks | `luyentu/Decks.vue` | public |
| 14 | `/decks/:id` | DeckDetail | `luyentu/DeckDetail.vue` | auth |
| 15 | `/decks/:id/play/flashcard` | FlashcardGame | `luyentu/FlashcardGame.vue` | auth |
| 16 | `/decks/:id/play/quiz` | QuizGame | `luyentu/QuizGame.vue` | auth |
| 17 | `/decks/:id/play/memory` | MemoryMatchGame | `luyentu/MemoryMatchGame.vue` | auth |
| 18 | `/decks/:id/play/typing` | TypingGame | `luyentu/TypingGame.vue` | auth |
| 19 | `/decks/:id/play/listening` | ListeningGame | `luyentu/ListeningGame.vue` | auth |
| 20 | `/decks/:id/play/mixed` | MixedGame | `luyentu/MixedGame.vue` | auth |
| 21 | `/ai-vocab-generator` | AiVocabGenerator | `luyentu/AiVocabGenerator.vue` | auth |
| 22 | `/decks/create` | DeckCreate | `luyentu/DeckCreate.vue` | auth |
| 23 | `/speaking` | Speaking | `speaking/SpeakingList.vue` | auth+premium |
| 24 | `/speaking/history` | SpeakingHistory | `speaking/SubmissionHistory.vue` | auth+premium |
| 25 | `/speaking/:id` | SpeakingDetail | `speaking/SpeakingDetail.vue` | auth+premium |
| 26 | `/speaking/:id/record` | SpeakingRecord | `speaking/SpeakingRecord.vue` | auth+premium |
| 27 | `/premium` | PremiumPage | `premium/PremiumPage.vue` | public |
| 28 | `/premium/checkout` | PremiumCheckout | `premium/PremiumCheckout.vue` | auth |
| 29 | `/admin` | — | `layouts/AdminLayout.vue` | admin-redirect → `/admin/dashboard` |
| 30 | `/admin/dashboard` | AdminDashboard | `admin/AdminDashboard.vue` | admin |
| 31 | `/admin/lessons` | AdminLessons | `admin/AdminLessons.vue` | admin |
| 32 | `/admin/exercises` | AdminExercises | `admin/AdminExercises.vue` | admin |
| 33 | `/admin/users` | AdminUsers | `admin/AdminUsers.vue` | admin |
| 34 | `/admin/speaking-prompts` | AdminSpeakingPrompts | `admin/AdminSpeakingPrompts.vue` | admin |
| 35 | `/admin/speaking-submissions` | AdminSpeakingSubmissions | `admin/AdminSpeakingSubmissions.vue` | admin |
| 36 | `/admin/videos` | AdminVideoLessons | `admin/AdminVideoLessons.vue` | admin |
| 37 | `/admin/video-attempts` | AdminVideoAttempts | `admin/AdminVideoAttempts.vue` | admin |
| 38 | `/admin/:id/build` | AdminLessonBuilder | `admin/AdminLessonBuilder.vue` | admin |
| 39 | `/:pathMatch(.*)*` | — | — | public → `/` |

## Runtime verification (228 visits, 0 defects)

`ui/routes-all.js` visits every route at **1440×900** and **360×812**, as
**admin**, as a **premium user**, and **anonymously** (**38 visited routes** ×
2 viewports × 3 roles = **228 visits**). The inventory holds **39** entries; the
one not visited directly is `/admin`, which only redirects — its real
destinations are the `/admin/*` children, which are all covered. Verified against
`routes-all.json`: the set difference `inventory − visited` is exactly
`['/admin']`, and `visited − inventory` is empty.

Concrete ids used are verified against the live DB, and each route family gets
its own — substituting one lesson id into every `:id` produced 20 phantom 404s
in an earlier revision:

| Placeholder | Id used | Why |
|---|---|---|
| `/lessons/:id` | 445 | exists in `lessons` |
| `/decks/:id` | 10006 | `Oxford 3000`, `is_public=1` |
| `/speaking/:id` | 50007 | `Daily routine - read aloud`, `is_published=1` |
| `/videos/:id` | 1 | `video_lessons.id=1`, `is_published=1` |
| `/admin/:id/build` | 445 | exists in `lessons` |

| Check | Result |
|---|---|
| Visits | **228** (38 × 2 viewports × 3 roles) |
| Console errors | **0** |
| API responses ≥ 400 | **0** |
| Routes with horizontal overflow (> 16 px) | **0** |
| Routes that failed to mount `#app` | **0** |
| Routes rendering a non-Be Vietnam Pro family | **0** |
| Guarded routes visited anonymously | 52 |
| …of those, **not** redirected away | **0** |
| Visits landing on a page the guard did not intend | **0** |
| Admin visits rendering a real admin page | **18/18** |

The anonymous row is the meaningful one: all 52 visits to `auth`, `auth+premium`
and `admin` routes as an anonymous user ended somewhere other than the requested
URL, so the guard is effective at runtime and not merely declared in `meta`.

### The landing contract (both directions)

Recording *where a visit ended up* is what makes this an assertion instead of a
note. A privileged visit must **stay**; an under-privileged one must be
**bounced**. Encoded per guard × role:

| Guard | anon | user | admin |
|---|---|---|---|
| `public` | stay | stay | stay |
| `guestOnly` | stay | away | away |
| `auth` | away | stay | stay |
| `auth+premium` | away | stay | stay |
| `admin` | away | away | stay |

The catch-all `/:pathMatch(.*)*` is an unknown URL by construction, so it always
redirects to `/` regardless of role. `/login` bouncing a logged-in visitor to
`/lessons` is the `guestOnly` guard working, **not** a defect — an earlier
revision of this check reported 24 "wrong landings" that were all correct
behaviour, which is its own kind of false positive.

### Harness corrections that changed the numbers

All three were harness defects, not app defects, and are recorded because they
produced alarming intermediate results:

1. **Wrong id per route family** → 20 console errors + 20 API 404s across
   `/decks/445`, `/speaking/445`, `/videos/445`. Fixed by per-family ids above.
2. **Self-inflicted rate limiting / JWT expiry** → `429 /api/auth/me`,
   `429 /api/lessons`, then a cascading `401 /api/admin/lessons/445/structure`
   (which is genuinely `200` with a valid admin token, verified directly). The
   global bucket is 100/min/IP and the JWT TTL is 900 s, so a long sweep exhausts
   both. Fixed by re-seeding the session and clearing **only** its own
   `rate_limit:*` keys every 8 visits. This is the same class of false positive
   documented in `AGENTS.md`.
3. **Token-only session seed** → `seedToken()` wrote `localStorage.token` but not
   `localStorage.user`, and `auth.isAdmin` reads the latter, so the guard bounced
   admin routes to `/`. This walk happened to survive it — the app hydrates
   `user` from `/api/auth/me` during earlier routes, and an A/B of the old vs new
   seed (`p20_routes_all_old_seed_ab.js`) produced **identical** admin results,
   0/9 bounced either way. The fix here is therefore hardening, not a bug fix: it
   makes the sweep correct by construction rather than correct by accident of
   route order. The fresh-context harnesses were not so lucky — see
   `REPORT.md` §6.1b.

## Cross-references

- Backend endpoint inventory: `endpoint-map.md`, `evidence/endpoint-inventory.json`
- Guard ↔ backend authorization alignment: `security-audit.md`
- Design-system rendering per route: `design-audit.md`
