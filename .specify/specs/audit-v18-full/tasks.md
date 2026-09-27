# audit-v18-full — tasks

## Phase 0 — SpecKit + baseline + prompt-flaw `[gate]`
- [x] T0.1 Xác nhận nhánh `audit-v15-full` + cây sạch → `evidence/baseline.md`
- [x] T0.2 Pipeline docs: spec.md / clarify.md / checklist.md / plan.md / tasks.md
- [x] T0.3 `assert-harness.js` → FAIL check 6 (default cũ) trước fix
- [x] T0.4 Đổi default → `audit-v18-full` (2 chỗ) + seed `harness-restore.md` → **ALL CLEAN (7/7)** ✅
- [x] T0.5 Backend suite → **537 / 0 / 0 / 11 — BUILD SUCCESS** → `evidence/baseline-backend.log`
- [x] T0.6 Frontend **194/1 (32)** + build **177.75 kB** → `baseline-frontend.log`, `baseline-build.log` ✅
- [x] T0.7 Container 8 up + parity `1470|43738|5|118|29|4|3|12|10` + inventory 121/137/135/25 ✅
- [x] T0.8 Kiểm lỗ hổng prompt (đo lại) → `evidence/prompt-flaws.md` ✅
- [x] T0.9 Falsify-first ledger → `evidence/prior-hypotheses.md` ✅
- [x] T0.10 `prompt-rewritten-v18.md` ✅
- [x] T0.11 Xác nhận 2 MCP engine dùng được → `evidence/mcp-engines.md` ✅

## Phase 1 — API sweep `[gate]`
- [x] T1.1 `api-sweep.js --audit audit-v18-full` → **145 pass / 0 fail / 0 blocked / 2 n/a** ✅
- [x] T1.2 Reconciliation 100% vs `api-inventory.js` → `evidence/phase-1-api.md`
- [x] T1.3 `deep-probe.js` → **58 / 0 / 0** ✅
- [x] T1.4 `search-sort.js` → pass hết ✅
- [x] T1.5 Rate-limit bucket (0 tự-gây-429) ✅
- [x] T1.6 AI stress ×10+×5 → **15/15 = 200**, không 500/504 → `evidence/ai-stress.md` ✅

## Phase 2 — DB audit + secret-scan `[gate]`
- [x] T2.1 Parity + baseline khớp ✅
- [x] T2.2 Orphan FK → **0 orphan thật** (5 FK nullable bị đếm nhầm) → `evidence/db-integrity.md` ✅
- [x] T2.3 FK index coverage → **22/22 HAS_INDEX** ✅
- [x] T2.4 Slow query → chỉ JDBC metadata + admin-exercises LIKE (40–193 ms) ✅
- [x] T2.5 Timezone re-verify → `evidence/db-integrity.md` ✅
- [x] T2.6 `ddl-auto=validate` drill (Phase 6)
- [x] T2.7 Secret-scan → 0 secret trong tracked source → `evidence/db-integrity.md` ✅
- [x] T2.8 Backup trước DML (nếu có)

## Phase 3 — UI/UX automated sweep `[gate]`
- [x] T3.1 `ui-sweep.js` → 0 contrast/alt/name/console/api/page/guard/overflow ✅
- [x] T3.2 `design-v2.js` → 0 badFont/legacy/drift/overflow, BVP LOADED ✅
- [x] T3.3 Contrast → 0 fail ✅
- [x] T3.4 `routes-all.js` → 228 visits, 0 wrong landing ✅
- [x] T3.5 CLS → 0.00069/0.00095/0.00003 ✅
- [x] T3.6 danger-tint + focused-probe + f1302-a1-a11y ✅ (fix F-18-01 chromium path)
- [x] T3.7 Tự dọn + assertClean CLEAN ✅
- [x] T3.8 Triage `smallTargets=115` → 0 vi phạm thật (inline link + label checkbox) ✅

## Phase U — MCP thủ công MỌI chức năng, CẢ 2 ENGINE cho MỌI luồng `[gate]`
- [x] TU.0 Xác nhận chrome-devtools MCP + Playwright MCP
- [x] TU.1 Auth: đăng ký, đăng nhập, khoá 5 lần, guard, 401, forgot/reset — × 2 engine
- [x] TU.2 Bài học/Bài tập: list, 3 tab, Kiểm-tra-vs-Nộp, answer-leak — × 2 engine
- [x] TU.3 Streak: profile + lịch + 5 đường ghi — × 2 engine
- [x] TU.4 Search/Sort: tra từ, 1 ký tự, sắp xếp — × 2 engine
- [x] TU.5 CRUD: decks, admin lessons/users/exercises, vocab — × 2 engine
- [x] TU.6 AI: generate-vocab, enrich, admin AI exercises, speaking assess — × 2 engine
- [x] TU.7 Game/Flashcard/SRS — × 2 engine
- [x] TU.8 Speaking/Video — × 2 engine
- [x] TU.9 Leaderboard/Profile/Premium — × 2 engine
- [x] TU.10 Admin: mọi trang — × 2 engine
- [x] TU.11 Home landing + decor + 5 width — × 2 engine
- [x] TU.12 `mcp-walkthrough.md` + `shots/mcp/<engine>/**`
- [x] TU.13 Dọn residue + re-assert parity

## Phase D — Đi sâu 6 nhóm chức năng `[gate]`
- [x] TD.1 Claim ledger từ demo doc + source → `evidence/demo-claims.md`
- [x] TD.2 Verify từng claim (CONFIRMED/REFUTED + fix)
- [x] TD.3 API↔UI cross-check từng khẳng định
- [x] TD.4 Coverage guard tính bằng code → `unprobed[]`/`missing[]` rỗng
- [x] TD.5 CRUD + AI deep coverage
- [x] TD.6 `analyze` giữa kỳ → `evidence/analyze-mid.md`

## Phase 5 — Performance
- [x] T5.1 perf-probe before → `evidence/perf-before.json`
- [x] T5.2 N+1/slow query → `evidence/db-perf.md`
- [x] T5.3 Bundle → `evidence/baseline-build.log`
- [x] T5.4 Fix win rõ (hoặc kết luận "không win") → `evidence/perf-conclusion.md`
- [x] T5.5 Redis hoạt động

## Phase 6 — Fix + regression + review chéo
- [x] T6.1 Findings FIXED + regression guard → `findings.md`
- [x] T6.2 Review chéo đối kháng → `evidence/review-v18.md`
- [x] T6.3 Mutation-test test hồi quy mới
- [x] T6.4 Suite xanh 0 regression

## Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
- [x] T7.1 Chạy lại toàn bộ → `evidence/round-2.md`
- [x] T7.2 Case biên → nt
- [x] T7.3 Dừng: 2 vòng liên tiếp 0 finding mới → nt

## Phase 8 — Docs + Converge/Analyze + Report + Cleanup
- [x] T8.1 Cập nhật README/AGENTS/CLAUDE/.agents-AGENTS/.gitignore/docs → `evidence/docs-drift.md`
- [x] T8.2 Verify + sửa demo doc + regenerate prompt doc
- [x] T8.3 converge + analyze → `evidence/converge.md`, `evidence/analyze.md`
- [x] T8.4 REPORT.md
- [x] T8.5 Cleanup manifest + thực thi → `evidence/cleanup-manifest.md`
- [x] T8.6 Commit Conventional Commits
