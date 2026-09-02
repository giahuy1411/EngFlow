# Plan: Comprehensive Audit + Playful Geometric Redesign

## Architecture

### Backend
- **No structural change**. Audit phát hiện bug sẽ fix inline.
- Audit script: PowerShell `audit-redesign-v1/scripts/api-audit.ps1` gọi qua `Invoke-RestMethod`, log status + body excerpt.
- Performance audit: SQL `sys.dm_exec_query_stats` + Hikari pool metrics + Spring Boot Actuator `/actuator/metrics`.

### Frontend
- **Design system layer**:
  - `frontend/src/assets/design-system.css` — single source of truth cho CSS variables (giữ cấu trúc hiện tại).
  - `frontend/tailwind.config.js` — extend theme với color tokens, font family, shadow utilities.
  - `frontend/index.html` — chỉ load Be Vietnam Pro từ Google Fonts.
- **Component audit & refactor**: 18 UI components trong `frontend/src/components/ui/`. Mỗi component phải dùng design tokens, không hardcode hex.
- **View audit**: 89 Vue files. Mỗi view được navigate qua browser, screenshot, kiểm tra console errors, tương tác với API.

### Database
- **No schema change**. Performance audit đọc execution plans.
- Index audit: `sys.indexes` join `sys.foreign_keys` — verify mỗi FK có index.
- Orphan check: left join FKs.

## Phases

### Phase 1: Reconnaissance (parallel)
- **P1.1** Backend endpoint map — list all `@RequestMapping` paths.
- **P1.2** Frontend view map — list all `.vue` files + routes.
- **P1.3** DB table map — list tables + row counts + FK count.
- **P1.4** Font scan — grep `Outfit` / `Plus Jakarta Sans` trong `frontend/`.
- **P1.5** Hex color scan — find hardcoded colors (especially #7C3AED, #DB2777, #FBBF24) trong `.vue` files.

### Phase 2: Backend API Audit
- **P2.1** Reusable script: `scripts/api-audit.ps1`.
- **P2.2** Audit per controller: Auth, Lesson, Exercise, Admin, Game, Deck, Flashcard, SRS, Vocab, Speaking, Video, Payment, Leaderboard, Progress, Streak, Dashboard, AI Vocab, Media Proxy.
- **P2.3** Bug fix loop: mỗi bug phát hiện → repro → root cause → fix → regression test.

### Phase 3: Frontend Audit
- **P3.1** Font migration: tìm và thay tất cả `Outfit` / `Plus Jakarta Sans` → `Be Vietnam Pro`.
- **P3.2** Token compliance: tìm hardcoded hex trong `.vue` files → thay bằng class Tailwind hoặc CSS var.
- **P3.3** View-by-view navigation qua Chrome DevTools MCP, screenshot, console check.
- **P3.4** Design system enforcement: hard shadows, border-2, radius.

### Phase 4: DB Audit
- **P4.1** Index coverage check.
- **P4.2** Slow query log via SQL Server DMV.
- **P4.3** Orphan record scan.
- **P4.4** Fix nếu có: tạo index, drop unused.

### Phase 5: Performance
- **P5.1** N+1 detection qua repository method analysis.
- **P5.2** Endpoint latency measurement.
- **P5.3** Fix nếu có: batch fetch, index, Redis cache.

### Phase 6: Design System Apply
- **P6.1** Update `tailwind.config.js`: thay đổi theme (nếu cần).
- **P6.2** Update `design-system.css`: chuẩn hóa tokens.
- **P6.3** Update `index.html`: chỉ Be Vietnam Pro.
- **P6.4** Component refactor cho consistency.

### Phase 7: Verify & Report
- **P7.1** Backend test suite: `mvnw.cmd test` ≥ 223/223.
- **P7.2** Frontend test suite: `npx vitest run` ≥ 73/73.
- **P7.3** Frontend build: `npx vite build` clean.
- **P7.4** Rebuild Docker backend, live API smoke test.
- **P7.5** Write `REPORT.md`.

## Data Model

Không thay đổi schema. Database hiện tại đã có:
- 19 tables, 24 FK, 0 orphan.
- `users`, `lessons`, `exercises`, `exercise_attempts`, `lesson_blocks`, `lesson_sections`, `lesson_snapshots`, `lesson_submissions`, `decks`, `deck_words`, `flashcards`, `flashcard_reviews`, `vocabulary`, `user_vocabulary_progress`, `speaking_prompts`, `speaking_submissions`, `video_lessons`, `video_attempts`, `payment_transactions`, `streaks`, `leaderboard_entries`.

## Contracts

- Mỗi backend endpoint trả về `application/json`.
- Lỗi trả `application/problem+json` (RFC 7807) qua `GlobalExceptionHandler`.
- Frontend gọi qua `api` instance (axios) với bearer token tự động từ auth store.

## Quickstart

1. `docker compose ps` — verify 8 services healthy.
2. Backend: `cmd /c "mvnw.cmd test"` từ repo root.
3. Frontend: `cd frontend && npx vitest run && npx vite build`.
4. Login admin: `POST /api/auth/login` → `data.token`.
5. Audit script: `pwsh scripts/api-audit.ps1 -Base http://localhost:8080`.
