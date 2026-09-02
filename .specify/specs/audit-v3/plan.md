# Plan: audit-v3

## Phases
1. **Infra/test baseline** (IN PROGRESS)
   - Fix #0a: DB_PASSWORD default (root cause: dual-path config Docker vs host).
   - Fix #0b: git hygiene — purge .env.bak-* khỏi git (secrets, chưa push), gitignore `.env.*`.
   - Full backend suite → xanh lại (target ≥ 221 tests).
2. **API audit** — HTTP thật trên :8080, mọi domain (auth/lessons/exercises/admin/speaking/
   video/payment/games/decks/flashcards/srs/vocab/progress/snapshot/media), kèm error paths.
3. **DB audit** — schema, orphan, index, size, mojibake, slow queries.
4. **Perf** — đo latency trước, tối ưu, đo sau (constitution P5).
5. **UI audit** — chrome-devtools-mcp + playwright-mcp, mọi chức năng chính + font/token check.
6. **Payment human-in-the-loop** — create order → QR → PAUSE nhắc user chuyển khoản →
   poll → verify premium.
7. **Lặp lại (round 2)** — re-run test suite + UI smoke sau mọi fix; fix tận gốc lỗi mới.
8. **Deliverables** — REPORT.md, GitHub issues (taskstoissues), commit.

## Risk notes
- DB password dev đã public trong AGENTS.md — chấp nhận; KHÔNG đưa secret khác vào git.
- SePay webhook: test bằng signature sai (must not 500).
- Ollama swap model ~5.7s khi test AI endpoints.
