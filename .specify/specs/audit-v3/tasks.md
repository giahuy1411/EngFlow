# Tasks: audit-v3

## Phase 1 — Infra & baseline
- [x] T1. Fix DB_PASSWORD default (host path) — `application.properties`
- [x] T2. Purge `.env.bak-*` khỏi git + gitignore `.env.*` (secrets hygiene)
- [x] T3. Backend full suite xanh lại (≥221 tests, 0 context error) — verifying pwsh-9
- [x] T4. Frontend suite xanh (73/73) — verified

## Phase 2 — API audit (HTTP thật)
- [x] T5. Script API audit: auth (login/register/me/change-password + error paths) — 54 endpoints
- [x] T6. Lessons + exercises + grading + submissions (CRUD + submit flow) — re-test 11/11 PASS
- [x] T7. Admin endpoints (stats/users/lessons/exercises/speaking/AI-gen) + 403 cho user
- [x] T8. Speaking (prompts/submissions/assess), Video (list/detail/attempts)
- [x] T9. Payment (create-order/status/webhook-invalid) — webhook invalid sig → 200 không 5xx
- [x] T10. Games (quiz/memory/typing), Decks CRUD, Flashcards, SRS, Vocabulary+AI (AI gen OK 9.7s)
- [x] T11. Progress/streak/leaderboard/dashboard/snapshot/media + no-auth 401 sweep

## Phase 3 — DB audit
- [x] T12. Row counts, DB size 592MB, orphan check 8 FK path = 0, FK index = 100% (2 idx mới)
- [x] T13. Mojibake re-check: PS decode artifact — data thật OK (UTF-8 đúng); recovery FULL → đề xuất SIMPLE

## Phase 4 — Perf
- [x] T14. Baseline: dictionary 19.5-21.9s/call (upstream ~20s), payment.status 3.5s, login đầu 2.3s (warmup)
- [x] T15. FIX: DictionaryService + Redis cache 1h + timeout 3/30s (cần tách class tránh self-invocation);
      SePay 401 circuit-breaker (log spam 300ms → im); 221/221 tests xanh sau fix; rebuild + verify runtime
      cached 23-258ms; DB: 2 FK index mới (0ms@scale hiện tại, phòng ngừa)

## Phase 5 — UI audit
- [ ] T16. chrome-devtools-mcp: Home→Auth→Lessons→Lesson flow→Videos→Decks→Vocab→
       Dashboard→Admin (console + font + tokens)
- [ ] T17. playwright-mcp: cross-verify core flows + responsive spot-check 375px
- [ ] T18. Design system compliance scan (grep font/hex, tokens, reduced-motion)

## Phase 6 — Payment human-in-the-loop
- [ ] T19. Create premium order qua UI → QR hiển thị → ⏸ PAUSE nhắc user chuyển khoản
- [ ] T20. Sau user xác nhận: poll status → PAID → verify premium DB + UI

## Phase 7 — Repeat round
- [ ] T21. Re-run backend+frontend tests sau mọi fix; UI smoke round 2; fix tận gốc

## Phase 8 — Deliverables
- [ ] T22. REPORT.md (đã làm/chưa làm/đã fix+cách fix/skills đã nạp)
- [ ] T23. GitHub issues cho bug còn tồn (taskstoissues) — remote github.com/giahuy1411/EngFlow
- [ ] T24. Commit final

## Analyze (mid-check, read-only)
- [x] A1. Spec ↔ plan ↔ tasks consistency check — PASS (sau khi viết xong; scope khớp
      constitution P1–P8, payment human-in-loop = spec AC5 = task T19/T20)
