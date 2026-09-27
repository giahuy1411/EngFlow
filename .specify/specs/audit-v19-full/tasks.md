# audit-v19-full — tasks

## Phase 0 — SpecKit + baseline + prompt-flaw `[gate]`
- [x] T0.1 Xác nhận nhánh `audit-v15-full` + cây sạch → `evidence/baseline.md`
- [x] T0.2 Pipeline docs: spec.md / clarify.md / checklist.md / plan.md / tasks.md
- [x] T0.3 `assert-harness.js` → **ALL CLEAN (7/7)** ✅
- [x] T0.4 Đổi default → `audit-v19-full` (2 chỗ) + seed `harness-restore.md` ✅
- [x] T0.5 Backend **537 / 0 / 0 / 11 — BUILD SUCCESS** ✅
- [x] T0.6 Frontend **194/1 (32)** + build **177.75 kB** ✅
- [x] T0.7 Container 8 up + parity `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + inventory 121/137/135/25 ✅
- [x] T0.8 Kiểm lỗ hổng prompt (đo lại) → `evidence/prompt-flaws.md` ✅
- [x] T0.9 Falsify-first ledger → `evidence/prior-hypotheses.md` ✅
- [x] T0.10 `prompt-rewritten-v19.md` ✅

## Phase 1 — API sweep `[gate]`
- [x] T1.1 `api-sweep.js --audit audit-v19-full` → `evidence/api-sweep.json`
- [x] T1.2 Reconciliation 100% vs `api-inventory.js`
- [x] T1.3 `deep-probe.js` → `evidence/deep-probe.json`
- [x] T1.4 `search-sort.js` → `evidence/search-sort.json`
- [x] T1.5 Rate-limit bucket (0 tự-gây-429)
- [x] T1.6 AI stress ×N≥10 → `evidence/ai-stress.md`

## Phase 2 — DB audit + secret-scan `[gate]`
- [x] T2.1 Parity + baseline khớp
- [x] T2.2 Orphan FK (loại NULL) → `evidence/db-integrity.md`
- [x] T2.3 FK index coverage
- [x] T2.4 Slow query → `evidence/db-perf.md`
- [x] T2.5 Timezone re-verify
- [x] T2.6 `ddl-auto=validate` drill
- [x] T2.7 Secret-scan → `evidence/secret-scan.md`
- [x] T2.8 Backup trước DML (nếu có)

## Phase 3 — UI/UX automated sweep `[gate]`
- [x] T3.1 `ui-sweep.js` → `evidence/ui-sweep.md`
- [x] T3.2 `design-v2.js` → `evidence/design-conformance.md`
- [x] T3.3 Contrast → 0 fail
- [x] T3.4 `routes-all.js` → 0 wrong landing
- [x] T3.5 CLS → `evidence/cls-before.json`
- [x] T3.6 danger-tint + focused-probe + f1302-a11y
- [x] **T3.7 FIX harness `smallTargets` (W2)**
- [x] T3.8 Tự dọn + assertClean CLEAN

## Phase U — MCP thủ công MỌI chức năng, CẢ 2 ENGINE `[gate]`
- [x] TU.0 Xác nhận chrome-devtools MCP + Playwright MCP
- [x] TU.1 Auth × 2 engine
- [x] TU.2 Bài học/Bài tập × 2 engine
- [x] TU.3 Streak × 2 engine
- [x] TU.4 Search/Sort × 2 engine
- [x] TU.5 CRUD × 2 engine
- [x] TU.6 AI × 2 engine
- [x] TU.7 Game/Flashcard/SRS × 2 engine
- [x] TU.8 Speaking/Video × 2 engine
- [x] TU.9 Leaderboard/Profile × 2 engine
- [x] **TU.13 Premium × 2 engine**
- [x] TU.10 Admin × 2 engine
- [x] TU.11 Home + 5 width × 2 engine
- [x] TU.12 `mcp-walkthrough.md` + `shots/mcp/<engine>/**`
- [x] TU.14 Dọn residue + re-assert parity

## Phase D — Đi sâu 6 nhóm chức năng `[gate]`
- [x] TD.1 Claim ledger → `evidence/demo-claims.md`
- [x] TD.2 Verify từng claim
- [x] TD.3 API↔UI cross-check
- [x] TD.4 Coverage guard code
- [x] TD.5 CRUD + AI deep
- [x] TD.6 `analyze` giữa kỳ

## Phase W — 4 WORKSTREAM `[gate]`
- [x] **W1** AI gloss: sửa prompt + guard CJK + test → `evidence/w1-ai-gloss.md`
- [x] **W2** smallTargets harness + a11y word-chip → `evidence/w2-tap-targets.md`
- [x] **W3** perf admin projection + đo before/after → `evidence/w3-perf-admin.md`
- [x] **W4** SePay chữ ký thật (chuyển khoản) → `evidence/w4-sepay-real.md`

## Phase 5 — Performance
- [x] T5.1 perf-probe before → `evidence/perf-before.json`
- [x] T5.2 N+1/slow query
- [x] T5.3 Bundle
- [x] T5.4 [W3] fix + before/after → `evidence/perf-conclusion.md`
- [x] T5.5 Redis

## Phase 6 — Fix + regression + review chéo
- [x] T6.1 Findings FIXED + regression → `findings.md`
- [x] T6.2 Review chéo → `evidence/review-v19.md`
- [x] T6.3 Mutation-test
- [x] T6.4 Suite xanh 0 regression

## Phase 7 — Vòng 2 `[gate]`
- [x] T7.1 Chạy lại toàn bộ → `evidence/round-2.md`
- [x] T7.2 Case biên
- [x] T7.3 Dừng: 2 vòng liên tiếp 0 finding mới

## Phase 8 — Docs + Converge/Analyze + Report + Cleanup
- [x] T8.1 Cập nhật README/AGENTS/CLAUDE/.gitignore/docs → `evidence/docs-drift.md`
- [x] T8.2 Verify + sửa demo doc + prompt doc
- [x] T8.3 converge + analyze
- [x] T8.4 REPORT.md
- [x] T8.5 Cleanup manifest + thực thi
- [ ] T8.6 Commit
