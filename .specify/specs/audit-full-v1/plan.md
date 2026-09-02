# Plan: Comprehensive Audit

## Phase 1: Health Check
- Verify all 8 Docker services running
- Get admin token
- Verify backend + frontend test suites

## Phase 2: Backend API Audit
- 26 controllers × 3-5 endpoints mỗi cái = ~100 API calls
- Categories: Auth, Lesson, Exercise, Admin, Game, Deck, Flashcard, SRS, Vocabulary, Speaking, Video, Payment, Leaderboard, Progress, Streak, Dashboard, AiVocab
- Use direct curl from PowerShell (token 230 chars)
- Log pass/fail + response time

## Phase 3: Database Audit
- Schema check (21 tables, FK, indexes)
- Sample 100 rows mỗi bảng chính
- EXPLAIN plans cho 5 query chậm
- Index recommendations

## Phase 4: AI Audit
- Exercise generation (limit=5, repeat 3x)
- Vocab generation
- Speaking rubric (if premium)
- Backfill status
- Test edge cases: empty input, weird chars, long input

## Phase 5: Frontend Audit
- Chrome DevTools MCP: navigate to /, /lessons, /luyentu, /admin, /speaking, /profile
- Take snapshot, verify text
- Take screenshot, verify colors
- Check Be Vietnam Pro loaded
- Verify hard shadows, borders, radii

## Phase 6: Performance
- Measure API latencies
- Identify N+1
- Recommend indexes
- Document bottlenecks

## Phase 7: Fix & Re-verify
- Fix all discovered bugs
- Re-run test suites (207 + 73)
- Re-verify audit points
