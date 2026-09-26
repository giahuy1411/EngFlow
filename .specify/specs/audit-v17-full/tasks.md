# audit-v17-full — tasks

## Phase 0 — SpecKit + baseline `[gate]`
- [x] T0.1 Xác nhận nhánh `audit-v15-full` + cây sạch → `evidence/baseline.md`
- [x] T0.2 Pipeline docs: spec.md / clarify.md / checklist.md / plan.md / tasks.md
- [x] T0.3 `assert-harness.js` → ALL CLEAN (5/5) ✅
- [x] T0.4 Backend suite → `evidence/baseline-backend.log`
- [x] T0.5 Frontend 178/1 (29) + build 177.74 kB → `evidence/baseline-frontend.log`, `baseline-build.log` ✅
- [x] T0.6 Container 8 up + parity + inventory → `evidence/baseline.md`, `endpoint-inventory.json`
- [x] T0.7 Falsify-first ledger → `evidence/prior-hypotheses.md`
- [x] T0.8 Kiểm lỗ hổng prompt + sửa → `evidence/prompt-flaws.md`

## Phase 1 — API sweep `[gate]`
- [x] T1.1 `api-sweep.js --audit audit-v17-full` → `evidence/api-sweep.json`
- [x] T1.2 Reconciliation 100% vs `api-inventory.js`
- [x] T1.3 Contract check → deep-probe contract
- [x] T1.4 Error-path role → api-sweep + deep-probe roles
- [x] T1.5 Rate-limit bucket (flushBuckets) → 0 tự-gây-429
- [x] T1.6 `deep-probe.js` + `search-sort.js` → `evidence/deep-probe.json`, `search-sort.log`

## Phase 2 — DB audit + secret-scan `[gate]`
- [x] T2.1 Parity + baseline → khớp `1470|43738|5|118|29|4|3|12|10`
- [x] T2.2 Orphan FK → `evidence/db-integrity.md`
- [x] T2.3 FK index coverage
- [x] T2.4 Slow query → `evidence/db-perf.md`
- [x] T2.5 Timezone re-verify → `evidence/tz-audit.md`
- [x] T2.6 `ddl-auto=validate` drill
- [x] T2.7 Secret-scan → `evidence/secret-scan.md`

## Phase 3 — UI/UX automated sweep `[gate]`
- [x] T3.1 `ui-sweep.js` → `evidence/ui-sweep.md`
- [x] T3.2 `design-v2.js` → `evidence/design-conformance.md`
- [x] T3.3 Contrast → 0 fail
- [x] T3.4 Tap-target + alt → 0 REAL
- [x] T3.6 CLS → `evidence/cls-probe.log`
- [x] T3.7 Responsive 5 viewport → 0 tràn

## Phase U — Interactive MCP UI/UX walkthrough (TRỌNG TÂM) `[gate]`
- [x] TU.0 Xác nhận chrome-devtools MCP + Playwright MCP dùng được (hoặc ghi BLOCKED + lỗi thật)
- [x] TU.1 Auth: U2 đăng ký, U3 đăng nhập (khoá lần 5), U4 guard, U5 401, U23 forgot/reset
- [x] TU.2 Bài học/Bài tập: U6 list, U7 tabs, U8 chấm thử vs nộp, U9 answer-leak
- [x] TU.3 Streak: U10 profile + lịch + hành động học
- [x] TU.4 Search: U11 hello + 1 ký tự
- [x] TU.5 CRUD: U12 decks, U13 flashcard/game, U14 SRS
- [x] TU.6 AI: U15 vocab gen, U16 admin AI exercises
- [x] TU.7 Speaking/Video: U17, U18
- [x] TU.8 Payment/Leaderboard/Profile: U19, U20, U21
- [x] TU.9 Admin: U22 mọi trang admin
- [x] TU.10 U1 Home landing + decor + 5 width
- [x] TU.11 Re-drive luồng trọng yếu bằng Playwright MCP
- [x] TU.12 `mcp-walkthrough.md` + `shots/mcp/**` + dọn residue

## Phase D — Deep 4-function logic audit `[gate]`
- [x] TD.1 Claim ledger từ demo doc + source → `evidence/demo-claims.md`
- [x] TD.2 Workflow W1: pipeline-over-claims, 2 lens độc lập mỗi chức năng
- [x] TD.3 API↔UI cross-check từng khẳng định
- [x] TD.4 Coverage guard tính bằng code → `unprobed[]`/`missing[]` rỗng
- [x] TD.5 CRUD + AI deep coverage
- [x] TD.6 `analyze` giữa kỳ → `evidence/analyze-mid.md`

## Phase 5 — Performance
- [x] T5.1 perf-probe before → `evidence/perf-before.json`
- [x] T5.2 N+1/slow query → `evidence/db-perf.md`
- [x] T5.3 Bundle → `evidence/baseline-build.log`
- [x] T5.4 Fix win rõ (hoặc kết luận "không win") → `evidence/perf-conclusion.md`
- [x] T5.5 Redis → hoạt động

## Phase 6 — Fix + regression + review chéo
- [x] T6.1 Findings FIXED + regression guard → `findings.md`
- [x] T6.2 Review chéo đối kháng → `evidence/review-v17.md`
- [x] T6.3 Suite xanh 0 regression

## Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
- [x] T7.1 Chạy lại toàn bộ → `evidence/round-2.md`
- [x] T7.2 Case biên → nt
- [x] T7.3 Dừng: vòng 2 = 0 finding mới → nt

## Phase 8 — Docs + Converge/Analyze + Report + Cleanup
- [x] T8.1 Cập nhật docs → `evidence/docs-drift.md`
- [x] T8.2 Sửa demo doc + regenerate prompt doc (`prompt-rewritten-v17.md`)
- [x] T8.3 converge + analyze → `evidence/converge.md`, `evidence/analyze.md`
- [x] T8.4 REPORT.md
- [x] T8.5 Cleanup manifest + thực thi → `evidence/cleanup-manifest.md`
