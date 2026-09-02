# Tasks: Comprehensive Audit + Playful Geometric Redesign

## Phase 1: Reconnaissance

- [ ] **T1.1** List all backend `@RequestMapping` paths → map per controller (Auth, Lesson, Exercise, Admin, Game, Deck, Flashcard, SRS, Vocab, Speaking, Video, Payment, Leaderboard, Progress, Streak, Dashboard, AI, MediaProxy)
- [ ] **T1.2** List all frontend `.vue` files + routes → map per view category
- [ ] **T1.3** Count DB tables, FK, row counts
- [ ] **T1.4** Font scan: `grep -r "Outfit" frontend/` và `grep -r "Plus Jakarta Sans" frontend/`
- [ ] **T1.5** Hardcoded hex scan: `grep -rnE "#[0-9A-Fa-f]{3,6}" frontend/src` — phân loại: token hợp lệ vs hardcode
- [ ] **T1.6** Check `frontend/index.html` fonts loaded, `tailwind.config.js` theme, `design-system.css` CSS vars hiện tại

## Phase 2: Backend API Audit

- [ ] **T2.1** Auth: login, register (409 conflict đã fix), me, logout → verify token flow
- [ ] **T2.2** Lessons: list, get, structure, content, attempts
- [ ] **T2.3** Exercises: grade (ungradeable đã fix), submit, admin CRUD
- [ ] **T2.4** Admin: stats, lessons CRUD, exercises, vocabulary, users, speaking-prompts
- [ ] **T2.5** Game: quiz (Redis session)
- [ ] **T2.6** Deck: list, get, words
- [ ] **T2.7** Flashcard: status, SRS: due
- [ ] **T2.8** Speaking: prompts list, submit audio
- [ ] **T2.9** Video: list, detail, YouTube embed
- [ ] **T2.10** Payment: create-order, webhook, status (latency đã fix)
- [ ] **T2.11** Leaderboard, Progress, Streak, Dashboard
- [ ] **T2.12** AI: vocab gen, exercise gen, backfill status/answers
- [ ] **T2.13** Bug fix loop: mỗi bug → root cause → fix → regression test
- [ ] **T2.14** Run `mvnw.cmd test` → expect ≥ 223/223

## Phase 3: Frontend Font & Design Audit

- [ ] **T3.1** Thay tất cả `Outfit` → `Be Vietnam Pro` trong Vue components, CSS, Tailwind config
- [ ] **T3.2** Thay tất cả `Plus Jakarta Sans` → `Be Vietnam Pro`
- [ ] **T3.3** Verify `frontend/index.html` chỉ load Be Vietnam Pro
- [ ] **T3.4** Verify `design-system.css` CSS vars match spec Section 3.1
- [ ] **T3.5** Find & replace hardcoded hex (#7C3AED, #DB2777, #FBBF24) trong Vue files → Tailwind classes hoặc CSS vars
- [ ] **T3.6** Verify `box-shadow` hard trong CSS files match `4px 4px 0px #1E293B`
- [ ] **T3.7** Verify `border-2 border-foreground` pattern trong templates
- [ ] **T3.8** Verify `prefers-reduced-motion` wrap cho bouncy/wiggle keyframes
- [ ] **T3.9** Mobile shadow scale: verify 4px → 2px media query

## Phase 4: Frontend View Navigation Audit (Chrome DevTools MCP + Playwright)

- [ ] **T4.1** Login flow → Home page (verify design tokens, font, shadows)
- [ ] **T4.2** Lessons list → Lesson detail → Lesson Exercise (CRUD API interaction)
- [ ] **T4.3** Speaking page → record → submit
- [ ] **T4.4** Video lessons → YouTube embed
- [ ] **T4.5** Premium page → Payment flow
- [ ] **T4.6** Leaderboard, Streak
- [ ] **T4.7** Admin: dashboard, lessons CRUD, exercises, vocabulary, users, speaking submissions
- [ ] **T4.8** Console error check mỗi view

## Phase 5: Database Performance Audit

- [ ] **T5.1** Verify FK columns đều có indexes: `SELECT fk.name, c.name FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc ON ...`
- [ ] **T5.2** Orphan record scan: `SELECT` with `NOT IN` cho mỗi parent-child relationship
- [ ] **T5.3** Slow query detection: `sys.dm_exec_query_stats` top elapsed queries
- [ ] **T5.4** Fix: CREATE INDEX cho missing FK indexes (nếu có)
- [ ] **T5.5** Verify Redis usage: keys pattern `engflow:*`

## Phase 6: Performance Optimization

- [ ] **P6.1** Measure P95 latency: `/api/lessons`, `/api/lessons/{id}/exercises`, `/api/leaderboard`
- [ ] **P6.2** Identify N+1: check repository methods for loop `.stream().map(ex -> repo.find...)` → replace with batch `findAllById`
- [ ] **P6.3** Fix N+1 nếu có
- [ ] **P6.4** Hikari pool: check `spring.datasource.hikari` settings (maxPoolSize, connectionTimeout)

## Phase 7: Apply Playful Geometric Design System

- [ ] **P7.1** Update `tailwind.config.js`: ensure theme extends colors/shadows/radius correctly
- [ ] **P7.2** Update `design-system.css`: finalize CSS variables (accent #7C3AED, secondary #DB2777, tertiary #B45309, quaternary #34D399, foreground #1E293B, background #FFFDF5)
- [ ] **P7.3** Update `frontend/index.html`: only Be Vietnam Pro, weights 400/500/700/800
- [ ] **P7.4** Component audit: 18 UI components — each must use design tokens
- [ ] **P7.5** Button variant: verify "Candy Button" pattern (bg-accent, rounded-full, border-2 border-foreground, shadow-pop-sm)
- [ ] **P7.6** Card style: verify "Sticker Card" pattern (bg-card, border-2 border-foreground, shadow-pop-lg)
- [ ] **P7.7** Focus states: verify hard ring (4px outline) not buried by shadow

## Phase 8: Verify & Report

- [ ] **P8.1** `cmd /c "mvnw.cmd test"` → ≥ 223/223
- [ ] **P8.2** `cd frontend && npx vitest run` → ≥ 73/73
- [ ] **P8.3** `cd frontend && npx vite build` → clean
- [ ] **P8.4** `docker compose up -d --build backend` → verify healthy
- [ ] **P8.5** Live API smoke test (login → lessons → grade)
- [ ] **P8.6** Negative test: `grep -r "Outfit\|Plus Jakarta Sans" frontend/src` → 0 results
- [ ] **P8.7** Write `REPORT.md` tại `.specify/specs/audit-redesign-v1/REPORT.md`
