# Tasks: Comprehensive Audit

## T1. Health Check [P0]
- T1.1 Verify all 8 Docker services (engflow-sqlserver, redis, backend, minio, whisper, frontend, tailscale, sqlserver-init)
- T1.2 Verify backend + frontend test suites pass (207 + 73)
- T1.3 Get fresh admin token

## T2. Backend API Audit [P0]
- T2.1 Auth: login, register, forgot-password
- T2.2 Lessons: list, get, structure, snapshot, submission
- T2.3 Exercise: list, get, submit, answer-key
- T2.4 Admin: exercises, lessons, users, dashboard, AI exercise, AI vocab, AI backfill
- T2.5 Game: generate quiz, submit answer, get result
- T2.6 Deck: list, create, get detail
- T2.7 Flashcard: list by deck, submit
- T2.8 SRS: get due cards, review
- T2.9 Vocabulary: list, get detail, AI generate
- T2.10 Speaking: list prompts, get, submit (record)
- T2.11 Video: list, get detail, attempt
- T2.12 Payment: premium packages, checkout, webhook
- T2.13 Leaderboard: top, my rank
- T2.14 Progress: me, summary
- T2.15 Streak: current, history, calendar
- T2.16 Media proxy: serve MinIO files

## T3. Database Audit [P0]
- T3.1 Schema check 21 tables
- T3.2 FK constraints
- T3.3 Indexes on FK columns
- T3.4 Sample 100 rows users / lessons / exercises / decks / vocabulary
- T3.5 EXPLAIN slow queries
- T3.6 No orphan records
- T3.7 Backfill stats (1,654 filled, 5,422 empty)

## T4. AI Features Audit [P0]
- T4.1 Exercise generation (qwen2.5:1.5b) - 3 calls
- T4.2 Vocab generation (qwen2.5:3b) - 2 calls
- T4.3 Speaking rubric (3b) - skip if no premium user
- T4.4 Backfill status check
- T4.5 Edge case: empty input, very long input
- T4.6 Model swap timing measurement

## T5. Frontend Audit (Chrome DevTools MCP) [P0]
- T5.1 Navigate to / and verify Home page
- T5.2 Navigate to /lessons and verify
- T5.3 Navigate to /luyentu/decks and verify
- T5.4 Navigate to /admin/dashboard and verify
- T5.5 Navigate to /speaking and verify (premium gate)
- T5.6 Navigate to /videos and verify
- T5.7 Verify Be Vietnam Pro font loaded
- T5.8 Verify color tokens
- T5.9 Verify hard shadows
- T5.10 Verify responsive (375px, 768px, 1440px)
- T5.11 Take screenshots of key pages

## T6. Performance [P1]
- T6.1 Measure all backend API latencies
- T6.2 Identify N+1 queries
- T6.3 Recommend indexes
- T6.4 Document bottlenecks

## T7. Fix Discovered Bugs [P0]
- T7.1 Fix router duplicate registration
- T7.2 Fix any 500 errors
- T7.3 Fix font load if missing
- T7.4 Fix color inconsistencies
- T7.5 Fix N+1 queries found
- T7.6 Fix missing indexes
- T7.7 Re-run test suites

## T8. Report [P0]
- T8.1 Generate Vietnamese audit report
- T8.2 Save to .specify/specs/audit-full-v1/REPORT.md
