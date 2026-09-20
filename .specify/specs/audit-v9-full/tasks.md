# Tasks — audit-v9-full

> Current status: OPEN. Round 0-2 entries are historical. Round 3 mock tests and
> JAR markers have narrower scope than runtime integration verification.
> reverify-status.md and checklist.md supersede broad DONE claims below.

Legend: `DONE` = executed with an artifact referenced; `PARTIAL` = executed but a stated sub-item is missing;
`BLOCKED` = cannot be executed in this environment, with the reason. Nothing is marked DONE without a file path.

## Round 0 — Foundation

| # | Task | Status | Evidence |
|---|---|---|---|
| T0.1 | Commit a checkpoint of the previous round before any measurement | DONE | `79ced4a chore(audit): checkpoint audit-v8-full round 4 before audit-v9` (working tree carried the v9 edits on top) |
| T0.2 | Re-measure the baseline (backend/frontend/build/mappings) | DONE | `evidence/baseline.md`, `sweep/v8/audit-v9-baseline-*.log` |
| T0.3 | Verify live infrastructure before believing any result | DONE | `evidence/baseline.md` (8 containers UP, 5173/8080/9000/11434/9002 open) |
| T0.4 | Fix the harness before trusting its output | DONE | F107 (`p2.js` leakCheck), F109 (`p4e.js` bucket flush), F110 (`p24_journeys.js` selectors), F111 (cleanup + parity gate) |
| T0.5 | Backup before DML (constitution gate) | DONE | `sweep/v8/v9-backup.sql` → `C:\\Users\\ASUS\\engflow-backups\\engflow_2026-09-17-audit-v9.bak` (35.3 MB compressed, `RESTORE VERIFYONLY` valid, `is_damaged=0`) |

## Round 1 — Sweep and root-cause fixing

| # | Task | Status | Evidence |
|---|---|---|---|
| T1.1 | Live endpoint registry from `/actuator/mappings` | BLOCKED | endpoint not exposed (404). Substituted a source-derived inventory of 144 mappings / 26 controllers: `evidence/endpoint-inventory.json`, `endpoint-map.md` |
| T1.2 | Route map + guard classification | DONE | `evidence/route-inventory.json`, `route-map.md` (39 routes) |
| T1.3 | API sweep over every mapping | DONE | `p1.json`…`p5.json` (round 2 logs `r2-p*.log`), coverage gate `evidence/coverage-round2.json` (144/144) |
| T1.4 | Core features probed closely (auth/lockout, streak, search/sort, CRUD, AI) | DONE | `p3a` AI 36/36, `p3b` speaking 19/19, `p4a` auth 18/18, `p5` 95 probes + 60 asserts (streak cycle measured against the DB) |
| T1.5b | Screenshot set re-taken after the fixes (`p23_screens_after.js`) | NOT DONE | Unused-CSS analysis is not pixel-equivalence evidence; `design-v2` (60 cells) + `routes-all` (228 visits) were re-run instead as the visual-regression signal |
| T1.5 | Real-browser UI/UX | DONE | `ui/routes-all.js` 228 visits, `ui/design-v2.js` 60 cells, `p24_journeys.js` 8/8 journeys, `ui/a11y2.js`, `ui/altcheck.js` |
| T1.6 | DB audit | DONE | `p6_db_audit_v9.sql` → `sweep/v8/v9b-db-audit.txt` → `db-audit.md` |
| T1.7 | Performance measured (no fixes yet) | DONE | `p9_perf_probe_v9.js before` → `p9_perf_v9_before.json`, `v9b-perf-before.log` |
| T1.8 | Every finding gets an ID, a root cause, a patch, a regression test and a re-measurement | DONE | `findings.json` (F105–F113) |

## Round 2 — Broader and deeper

| # | Task | Status | Evidence |
|---|---|---|---|
| T2.1 | Re-run every round-1 harness on the final build | DONE | `r2-p1..p5.log`, `r2-p14.log`, `r2-p15.log`, `r2-p6.log`, `r2-p11.log`, `r2-p13.log`, `r2-routes-all.log`, `r2-design-v2.log`, `r2-p24.log`, `r2-a11y2.log`, `r2-altcheck.log` |
| T2.2 | Adversarial re-derivation from the live DTO contracts | PARTIAL | `p14_adversarial.js` (0 fail) + `p15_adversarial_deep.js` (21/21) re-run on the final build. A dedicated `p25_adversarial_rederive_v9.js` that regenerates fixtures from the DTO source was NOT written — the existing two suites cover SQL metacharacters, pagination, stale ids, upload shapes, slow network, service down and 503 handling |
| T2.3 | Performance fixes with before/after numbers | DONE | F108: `v9_perf_exercise_list_before/after.json`, `performance.md` |
| T2.4 | Index decisions backed by the plan cache | DONE (decision: change nothing) | `performance.md` §"What was measured and NOT changed"; `db-audit.txt` blocks [5] [13] |
| T2.5 | Verify every round-1 finding is actually closed | DONE | `findings.json` status column + the per-finding verification strings |
| T2.6 | Coverage gate 144/144 | DONE | `evidence/coverage-round2.json` (shipped as `v9_coverage_check.py`, not `p11_coverage_gate_v9.js`) |
| T2.7 | Two independent read-only reviews | DONE (2026-09-18, review only) | evidence/reverify-code-review.txt and evidence/reverify-evidence-review.txt; both processes exited 0, finding proof gaps rather than audit PASS |
| T2.8 | Backlog measured, not mutated | DONE | `db-audit.md` §Backlog, `issues.md` |

## Round 3 — Re-verification after interrupted state

| # | Task | Status | Evidence |
|---|---|---|---|
| T3.1 | Re-verify restored F105/F106 | DONE (stated scope) | 20 focused tests/package PASS; F105 security slice 6/6; original F106 JPA tests 3/3 in isolated SQL Server DB; old-formula JDBC control 1/1. No full API sweep claim |
| T3.2 | Rebuild backend and verify artifact identity | DONE | sweep/v8/v9-class-identity.json: four relevant class hashes match container; public exercise list HTTP 200; no new database integration claim |
| T3.3 | Backup and cleanup verified fixture | PARTIAL | Exact fixture removed, backup checksum verified and host copy saved; row counts match. Accidental full-suite startup effects still require integrity reconciliation |
| T3.4 | Synchronize audit report with re-verified state | PARTIAL | Current evidence ledger and historical disclaimers added; review findings and final gates still require reconciliation |

## Artifacts

| Artifact | Status |
|---|---|
| `findings.json` | Historical ledger: 10 findings, 8 FIXED and 2 BLOCKED; current proof limits are in reverify-status.md |
| `converge.md`, `analyze.md` | DONE |
| `issues.md` (local draft, no GitHub call) | DONE |
| `workflow-log.md` | DONE |
| `REPORT-v5.md` | DONE |
| `prompt-gap-v5.md`, `prompt-rewritten-v5.md` | DONE |
| v9 spec / clarify / checklist / constitution | Authored retrospectively 2026-09-18; existence DONE, original pipeline execution not proven; acceptance gates remain explicit |
