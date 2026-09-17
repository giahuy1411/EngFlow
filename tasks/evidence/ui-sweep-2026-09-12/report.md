# EngFlow UI Sweep — Evidence Report (2026-09-12)

- Runner: senior-QA subagent, dsh-builtin-browser (shared visible window), SPA client-side navigation (Vue Router handle), console-error collector + `performance.getEntriesByType('resource')` ≥400 scan on every sampled load.
- Roles: learner `user@gmail.com` (isPremium=true in seed) and admin `admin@gmail.com`.
- Real data used: lesson **41881** and **813** (id=1 does not exist — see notes), deck **10007** (AWL C1, 10 words), video **1**.
- Screenshot index: `NNN-<route>.png` in this directory (43 files + 3 `-b` variants).
- No mutations performed: no admin save (blocked client-side validation once, never POSTed), no account created, no payment, no AI generate/backfill, no delete clicked.

## Matrix

| route | role | tier | result | console-errors | failed-requests | screenshot | notes |
|---|---|---|---|---|---|---|---|
| / | anon | 1 | PASS | none | none | 001-home.png | title + hero render (body 1513 ch) |
| /login | anon | 1 | PASS | none | none | 002-login.png | form with email/pass/remember/forgot |
| /register | anon | 1 | PASS | none | none | 003-register.png | 4 fields + submit (validation: T2) |
| /forgot-password | anon | 1 | PASS | none | none | 004-forgot-password.png | renders email form |
| /reset-password?token=… | anon | 1 | PASS | none | none | 005-reset-password.png | render-only w/ dummy token; OTP+email+password form |
| /admin/dashboard (anon) | anon | 1 | PASS | none | none | 006-admin-anon-redirect-login.png | guard redirects to /login as expected |
| /lessons | learner | 1 | PASS | none | none | 007-lessons.png | level chips + 1471 lesson cards; login lands here |
| /lessons/:id (41881) | learner | 1 | PASS | none | none | 008-lessons-id-exercises.png | 3 tabs NỘI DUNG/BÀI TẬP/LỊCH SỬ + "In tài liệu". NOTE: brief's 6 skill tabs don't exist in LessonLayout (implemented: content/exercises/history). Probe /lessons/1 → handled API 404 (`/api/lessons/1/exercises/content`), shell renders, no crash (id=1 absent in DB) |
| /videos | learner | 1 | PASS | none | none | 009-videos.png | premium gate: user isPremium → allowed |
| /videos/:id (1) | learner | 1 | PASS | none | none | 010-videos-id.png, 010b-videos-id-quiz.png | YouTube iframe `youtube.com/embed/2VeQTuSSiI0` (title-verified, per AGENTS.md), word-button transcript, Quiz tab renders (T2) |
| /profile | learner | 1 | PASS | none | none | 011-profile.png | streak/points/premium expiry "3/10/2026" |
| /search | learner | 1 | PASS | none | none | 012-search.png, 012b-search-nonsense-empty.png | nonsense query → no results section, no error (T3) |
| /leaderboard | learner | 1 | PASS | none | none | 013-leaderboard.png | 76 learners, top list renders |
| /decks | learner | 1 | PASS | none | none | 014-decks.png | community + my decks grid |
| /decks/:id (10007) | learner | 1 | PASS | none | none | 015-decks-id.png | 6 game-mode tabs + 10-word list |
| /decks/10007/play/flashcard | learner | 1+2 | PASS | none | none | 016-deck-flashcard-flipped.png | card FLIPPED: ANALYZE→"PHÂN TÍCH" (T2 interaction) |
| /decks/10007/play/quiz | learner | 1+2 | PASS | none | none | 017-deck-quiz-answered.png | 1 answer clicked, advanced 0/1 (T2) |
| /decks/10007/play/memory | learner | 1+2 | PASS | none | none | 018-deck-memory-flipped.png | 2 cards flipped → 1 attempt counted (T2) |
| /decks/10007/play/typing | learner | 1+2 | PASS | none | none | 019-deck-typing-answered.png | typed "occur" + Enter → correct, 1/1 (T2) |
| /decks/10007/play/listening | learner | 1+2 | PASS | none | none | 020-deck-listening-answered.png | TTS fallback audio; answered → 1 ĐÚNG (T2) |
| /decks/10007/play/mixed | learner | 1+2 | PASS | none | none | 021-deck-mixed-answered.png | answer selected → 1/1 correct (T2) |
| /ai-vocab-generator | learner | 1 | PASS | none | none | 026-ai-vocab-generator.png | form renders; "Sinh từ vựng" NOT clicked — SKIP(slow+mutation) |
| /decks/create | learner | 1 | PASS | none | none | 027-decks-create.png | render-only; "Tạo bộ từ" NOT clicked (writes DB) |
| /speaking | learner | 1 | PASS | none | none | 022-speaking.png | 6 prompts listed |
| /speaking/history | learner | 1 | PASS | none | none | 025-speaking-history.png | graded submission (8.5/10) visible |
| /speaking/:id (3) | learner | 1 | PASS | none | none | 023-speaking-id.png | detail + "BẮT ĐẦU LUYỆN" |
| /speaking/:id/record (3) | learner | 1 | PASS(render) / BLOCKED-PERMISSION(record) | none | none | 024-speaking-id-record.png | "Bật micro" NOT clicked — automation cannot grant mic; UI states verified only |
| /premium | learner | 1 | PASS | none | none | 028-premium.png | plan cards render |
| /premium/checkout | learner | 1 | PASS | none | none | 029-premium-checkout-render-only.png | QR + CK content render ONLY; "Kiểm tra" NOT clicked, no payment |
| /login wrong-password | learner | 2 | PASS | none | POST login 4xx (expected, handled) | 031-login-wrong-password-error.png | visible inline error "Email hoặc mật khẩu không chính xác." |
| logout → /login | learner | 2 | PASS | none | none | 030-logout-state-login.png | token cleared (localStorage token gone) |
| /register validation | learner | 2 | PASS | none | none (no POST fired) | 032-register-validation-mismatch.png | mismatch pwd → role=alert "Mật khẩu KHÔNG KHỚP"; no account created |
| /lessons/:id exercises grade (813) | learner | 2 | PASS | none | none (inclAnswers probe 403 = by design) | 033-lesson-graded-correct-wrong.png | "Kiểm tra" ×2 via UI → POST /api/lessons/813/exercises/grade: "which"=✅ĐÚNG, "WRONGANSWERZZ"=❌SAI + key revealed. Ungradeable-empty-key rows = by design (backfill 2026-09-12) |
| /admin/dashboard | admin | 1 | PASS | none | none | 034-admin-dashboard.png | stats 76 users / 1471 lessons / 43737 exercises |
| /admin/lessons | admin | 1 | PASS | none | none | 035-admin-lessons.png | table + filters render |
| /admin/lessons create-modal | admin | 2 | PASS | none | none (save blocked by HTML5 validation, no POST) | 043-admin-lessons-create-modal.png | empty title invalid → submit blocked → **Hủy** (cancel), never "Lưu lại" |
| /admin/exercises | admin | 1 | PASS | none | none | 036-admin-exercises.png | 43737-row list paginated |
| /admin/users | admin | 1 | PASS | none | none | 037-admin-users.png | user table w/ Khóa/Cấp Premium buttons — NOT clicked (mutation) |
| /admin/speaking-prompts | admin | 1+2 | PASS | none | none | 038-admin-speaking-prompts.png, 038b-…-modal.png | "Thêm đề bài" modal opened → verified fields → ✕ closed. "Tạo đề bằng AI" NOT clicked (slow+mutation) |
| /admin/speaking-submissions | admin | 1 | PASS | none | none | 039-admin-speaking-submissions.png | grading list renders; no grade submitted |
| /admin/videos | admin | 1+2 | PASS | none | none | 040-admin-videos.png, 040b-…-modal.png | modal opened (5 fields incl. "AI tạo phụ đề" — NOT clicked), closed via ✕/Hủy without save; 5 video lessons listed; "Xóa" NOT clicked |
| /admin/video-attempts | admin | 1 | PASS | none | none | 041-admin-video-attempts.png | shadowing attempts list; "✨ AI chấm" NOT clicked (mutation+Ollama) |
| /admin/:id/build (41881) | admin | 1+2 | PASS | none | none | 042-admin-id-build.png | LessonBuilder renders (section + text block, textarea prefilled); "Lưu tất cả" disabled while clean — no edit made (would require save) |

Tier-1 counts: **37 router routes (5 public + 23 learner + 9 admin) all render-PASS / 0 FAIL**, plus the anon `/admin/dashboard` → /login redirect PASS (38 matrix entries). Mic recording sub-state on `/speaking/:id/record` = BLOCKED-PERMISSION; write buttons inside otherwise-passing pages = SKIP(slow+mutation) per rules.

## Tier 3 (cross-cutting, sampled)

- Viewport 375/1440: **SKIP(resize-unsupported)** — `window.resizeTo` does not change `innerWidth` in the shared browser (stuck at 1384; outerWidth changed but layout did not). Default 1384px captures reviewed visually.
- Nonsense search `/search` "zzqxv nonsense zzz": PASS — clean empty state, 0 console errors, 0 failed requests (012b).
- Console-clean on every sampled learner/admin load: PASS (collector + performance scan; see error note below).

## FAILS

None. All 36 routes render with correct role gating; no blank screens, no vite-error-overlay, no error boundaries during the whole sweep.

## Observations / minor notes (not failures)

1. **/lessons/1 probe** logs a console error "Failed to load lesson content: 404" (only console error in entire sweep). Cause is DATA (id 1 doesn't exist; real ids 41881/813/800-range), UI degrades gracefully. UX nit: failure is silent for the visitor (no visible "not found" message) — consider an empty-state.
2. **LessonLayout has 3 tabs (NỘI DUNG/BÀI TẬP/LỊCH SỬ)**, not the 6 skill tabs + exercises listed in the sweep brief — matches current router source (`LessonLayout.vue` tabs array), treated as spec drift, not a bug.
3. **Admin "Thêm video" modal ✕** needed a second attempt to close via synthetic clicks (Hủy works reliably); real-user clicks not repro-checked — cosmetic, no mutation risk.
4. `GET /api/lessons/{id}/exercises?includeAnswers=true` returns keys for admin token but learner sees `correctAnswer:null` and got **403 on /api/exercises** — matches audit-v5 design (server-side grading only).
5. Premium learner (`user@gmail.com`) has `isPremium:true` seed → speaking/videos gates pass; anon guard redirect verified once (item 006).

## BLOCKED / SKIP list

- BLOCKED-PERMISSION: `/speaking/3/record` actual recording (mic capture impossible in automation; "Bật micro" intentionally not clicked). Render + UI states = PASS.
- SKIP(slow+mutation): `/ai-vocab-generator` "Sinh từ vựng"; `/decks/create` "Tạo bộ từ"; `/admin/speaking-prompts` "Tạo đề bằng AI"; `/admin/videos` "AI tạo phụ đề"; `/admin/video-attempts` "✨ AI chấm"; all "Xóa"/"Khóa"/"Cấp Premium"/"Lưu lại"/grade-submit buttons; `/premium/checkout` payment check/completion (render only).
- SKIP(resize-unsupported): Tier 3 375px/1440px responsive sampling.

## Verdict

**PASS — 37/37 routes render correctly for their role (plus anon admin→login redirect), 6/6 core flows completed (one sub-state blocked on mic permission), zero application errors; only data-level 404 (lessons/1) and by-design gates observed.**
