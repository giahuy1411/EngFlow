# audit-v16-full — tasks

## Phase 0 — SpecKit + baseline `[gate]`
- [x] T0.1 Xác nhận nhánh `audit-v15-full` + cây sạch → `baseline.md`
- [x] T0.2 Pipeline docs: spec.md / clarify.md / checklist.md / plan.md / tasks.md
- [x] T0.3 `assert-harness.js` → ALL CLEAN (5/5)
- [x] T0.4 Backend suite → **515/0/0/11** → `baseline-backend.log`
- [x] T0.5 Frontend 178/1 (29) + build 177.74 kB → `baseline-frontend.log`, `baseline-build.log`
- [x] T0.6 Container 8 up + parity + inventory → `baseline.md`, `endpoint-inventory.json`
- [x] T0.7 Falsify-first ledger → `prior-hypotheses.md`

## Phase 1 — API sweep `[gate]`
- [x] T1.1 `api-sweep.js` → **143/0** → `api-sweep.json`
- [x] T1.2 Reconciliation 100% (121/137/135) → `endpoint-inventory.json`
- [x] T1.3 Contract check → deep-probe contract 13/0
- [x] T1.4 Error-path role → api-sweep + deep-probe roles 25/0
- [x] T1.5 Rate-limit bucket (flushBuckets) → 0 tự-gây-429
- [x] T1.6 `deep-probe.js` **58/0** + `search-sort.js` → `deep-probe.json`, `search-sort.log`

## Phase 2 — DB audit + secret-scan `[gate]`
- [x] T2.1 Parity + baseline → khớp
- [x] T2.2 Orphan FK → **22 FK, 0 orphan** → `db-integrity.md`
- [x] T2.3 FK index coverage → **0 thiếu**
- [x] T2.4 Slow query → `db-perf.md`
- [x] T2.5 Timezone re-verify → `tz-audit.md`
- [x] T2.6 Schema drift → parity + entity khớp
- [x] T2.7 Secret-scan → `secret-scan.md` (2 kết luận SAI bác bỏ)

## Phase 3 — UI/UX `[gate]`
- [x] T3.1 `ui-sweep.js` → 0 guard/console/api/page → `ui-sweep.md`
- [x] T3.2 `design-v2.js` → 0/0/0/0 → `design-conformance.md`
- [x] T3.3 Contrast → 0 fail
- [x] T3.4 Tap-target + alt → 0 REAL
- [x] T3.5 MCP tương tác thật → `shots/mcp-*.png`
- [x] T3.6 CLS → 0.00069/0.00095/0.00003
- [x] T3.7 Responsive 5 viewport → 0 tràn

## Phase 4 — Core E2E (API↔UI) `[gate]`
- [x] T4.1–T4.9 6 nhóm chức năng → `e2e-core-features.md`
- [x] T4.10 `analyze` giữa kỳ → `analyze-mid.md`

## Phase 5 — Performance
- [x] T5.1 perf-probe before → `perf-before.json`
- [x] T5.2 N+1/slow query → `db-perf.md`
- [x] T5.3 Bundle → `baseline-build.log`
- [x] T5.4 Fix win rõ → **0 win (đúng P5)** → `perf-conclusion.md`
- [x] T5.5 Redis → hoạt động

## Phase 6 — Fix + regression + review chéo
- [x] T6.1 8 finding FIXED + regression guard → `findings.md`
- [x] T6.2 Review chéo đối kháng → `review-v16.md`
- [x] T6.3 Suite xanh 0 regression (515/0, 178/1) → nt

## Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
- [x] T7.1 Chạy lại toàn bộ → `round-2.md`
- [x] T7.2 Case biên → nt
- [x] T7.3 Dừng: vòng 2 = 0 finding mới → nt

## Phase 8 — Docs + Converge/Analyze + Report + Cleanup
- [x] T8.1 Cập nhật docs → `docs-drift.md`
- [x] T8.2 Viết lại `docs/demo-engflow-4-chuc-nang.md` (kèm code đã verify)
- [x] T8.3 converge + analyze → `converge.md`, `analyze.md`
- [x] T8.4 REPORT.md
- [x] T8.5 Cleanup manifest + thực thi → `cleanup-manifest.md`
