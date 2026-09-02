# Checklist: audit-v3

## Test infrastructure
- [x] `src/test/resources/application.properties` tạo (DB_PASSWORD + jwt.secret default)
- [x] Backend suite chạy lại sau fix (pwsh-8)
- [x] Frontend suite xanh 73/73

## API audit (HTTP thật :8080)
- [x] AUTH: login user/admin, wrong-pass, register, me, change-password
- [x] LESSONS: list, detail, content, structure
- [x] EXERCISES: list, grade, submit
- [x] ADMIN: stats, users, lessons, exercises CRUD, speaking submissions, AI gen
- [x] SPEAKING: prompts list/search/detail, submissions
- [x] VIDEO: list, detail, attempts
- [x] PAYMENT: create-order, status, webhook signature paths
- [x] GAMES: quiz/memory/typing session flow
- [x] DECKS: public/my/detail/create/update/delete
- [x] FLASHCARDS: status, review
- [x] SRS: due, review
- [x] VOCABULARY: search, AI generate
- [x] PROGRESS: progress, streak, history, leaderboard, dashboard
- [x] ERROR PATHS: 401/403/404/409/400 cho representative endpoints

## UI audit (2 MCP)
- [x] chrome-devtools-mcp: Home, Login, Lessons, Lesson detail flow, Videos, Decks,
      Vocabulary, Dashboard, Premium, Admin
- [x] playwright-mcp: same core flows (cross-verify)
- [x] Console error scan mỗi page
- [x] Font check: computed style `Be Vietnam Pro`

## Payment human-in-the-loop
- [x] Create order premium → QR SePay hiển thị
- [x] ⏸ PAUSE — nhắc user chuyển khoản, chờ xác nhận
- [x] Poll status → PAID
- [x] Premium activated (DB + UI)

## DB / Performance
- [x] Row counts + DB size
- [x] Orphan check (24 FK)
- [x] Missing FK indexes
- [x] Latency đo trước/sau tối ưu
- [x] Recovery model / log growth

## Design system compliance
- [x] Font grep 0 Outfit / 0 Plus Jakarta Sans
- [x] Tokens --geo-* đầy đủ
- [x] Hard shadow utilities
- [x] prefers-reduced-motion

## Deliverables
- [x] REPORT.md
- [x] GitHub issues (taskstoissues)
