# Workflow log — audit-v9-full (2026-09-17, +07)

> Historical 2026-09-17 evidence. Current audit remains OPEN. Do not use the
> historical PASS tables as current coverage or completion proof. See
> reverify-status.md for 2026-09-18 evidence, interrupted full-suite execution,
> two completed independent reviews, and unresolved acceptance gates.

Chronological, with the command that produced each line of evidence. Times are naive VN clock (the JVM writes naive VN;
`SYSDATETIME()` inside the SQL container is UTC and is only ever used to label a run).

## Session resume

- This session resumed `codex://threads/01a0ae7e-f10d-7342-8695-843e6f669a23`, whose state was: checkpoint commit done,
  baseline measured, dead `.geo-*` CSS already stripped, F105 patch applied but never verified live, an SRS 500 open
  (`p2-v9b.log: FAIL SRS review … got=500`), and a first (tautological) SRS test that could never fail.
- State was re-derived from the working tree, not from the transcript: `git status --short`, the `v9*` logs, and a DB
  parity read. Findings carried over: F105 (unverified), F106 (open), F107 (unresolved warning).

## Round 1

| When | Action | Evidence |
|---|---|---|
| 21:06 | Backup before DML (`BACKUP … WITH COMPRESSION, CHECKSUM` + `RESTORE VERIFYONLY`, copied to the host staging dir) | `sweep/v8/v9-backup.sql`, `engflow_2026-09-17-audit-v9.bak` (35.3 MB) |
| 21:06 | Deleted two leftovers from the previous session's aborted runs (draft lesson 102049 + exercise + attempt, vocab 50261) and asserted parity back to baseline | `sweep/v8/v9-clean-leftovers.sql` → `1471\|43737\|76\|127\|28\|15\|4\|126\|14\|5` |
| 21:08 | Reproduced F106 live and captured the server-side root cause | `sweep/v8/v9-f106-repro.log` (500) + `docker logs engflow-backend` → `DataIntegrityViolationException … out of range of values for the datetime2 SQL Server data type` on `update user_vocabulary_progress … next_review_date=?` |
| 21:09 | Replaced the tautological SRS test with two real tests, ran them RED | `src/test/java/com/datn/engflow/service/AuditV9SrsIntervalOverflowTest.java`, `sweep/v8/f106-red.log` (2 failures) |
| 21:11 | F106 fix (`MAX_INTERVAL_DAYS = 365`), ran GREEN | `sweep/v8/f106-green.log` (3/3) |
| 21:12 | Rebuilt the backend container, verified F106 and F105 live | `docker compose up -d --build backend`; `v9-f106-repro2.log` (200), `v9-f105-verify.log` (6/6: student 404 on draft, admin preview 200) |
| 21:14 | DB check: the bricked row healed itself on the next review | row id=20002: `srs_interval` 1,537,216 → 365, `next_review_date` → 2027-09-17 |
| 21:16 | Full backend suite | `v9-r1-backend-full.log` 391/391 BUILD SUCCESS |
| 21:16 | Frontend unit tests | `v9-r1-frontend-tests.log` 90/90 in 19 files |
| 21:17 | `vite build` — dead-CSS effect on the bundle | `v9-r1-build.log`: CSS 92.42 → 85.22 kB, JS entry 176.80 kB unchanged |
| 21:19 | Dead-CSS gate | `sweep/v8/v9_deadcss_check.py`: selectors=4 used=4 UNUSED=0 |
| 21:21 | Endpoint/route inventories from source | `v9_endpoint_inventory.py` (144/26), `v9_route_inventory.py` (39) |
| 21:25 | Browser sweep: routes | `v9-routes-all.log` 228 visits, 0 errors, 0 wrong landing |
| 21:32 | Browser sweep: design (font/tokens/overflow/lucide) | `v9-design-v2.log` 0 violations, 7685 elements |
| 21:38 | Browser: a11y + alt + interactions + video | `v9-a11y2.log`, `v9-altcheck.log`, `v9-inter1/2/3.log`, `v9-video1.log` |
| 21:41 | API sweep batch 1 (p1–p3b) | `v9b-p1.log` 124/124, `v9b-p2.log` 60/60, `v9b-p3a.log` 36/36, `v9b-p3b.log` 19/19 |
| 21:45 | API sweep batch 2 (p4a–p5) | `v9b-p4a…p5.log`; p4e initially 429 (harness defect F109) |
| 21:47 | F109 fix + re-run | `v9b-p4e2.log` 6/6 |
| 21:49 | Parity check after the batch → residue found in 6 tables | `p16-parity.sql` → `77\|128\|…\|30\|16\|5\|127` |
| 21:52 | Wrote a sweep-level cleanup with markers; **it deleted 4 legacy audit users** (bare `email LIKE 'zz%'`) | `v9_cleanup_sweep.py` → users 76 → 72 |
| 21:53 | Restored those 4 rows verbatim from the 21:06 backup via a side restore database, then dropped it | `_v9_restore_extract.sql`, `_v9_restore_users.sql`, `_v9_drop_restore.sql`; parity back to 76 |
| 21:55 | Scoped the cleanup to the audit day and re-ran it | `PARITY_OK=True` |
| 21:56 | Built `p24_journeys.js`; first pass 3/8 because of invented selectors | `v9b-p24.log` |
| 21:58 | Probed the real markup, fixed the harness, iterated | `v9_probe_selectors.js`, `v9_probe_deck.js`, `v9_probe_admin.js`; `v9b-p24c.log` 8/8 |
| 22:00 | Wrote the focused perf probe, measured BEFORE on lesson 651 | `v9-perf-before`, `v9_perf_exercise_list_before.json`: median 153 ms, worst 293 ms |
| 22:02 | F108 fix: flat projection for the exercise list + regression test | `ExerciseLessonProjection.java`, `ExerciseRepository`, `ExerciseService`, `AuditV9ExerciseListProjectionTest` |
| 22:06 | Rebuilt, measured AFTER | `v9_perf_exercise_list_after.json`: median 29 ms; old statement exec count 48 → 48 |
| 22:09 | Full backend suite on the fixed build | `v9-r2-backend-full.log` 393/393 |
| 22:12 | DB audit | `p6_db_audit_v9.sql` → `v9b-db-audit.txt` → `db-audit.md` |
| 22:14 | Round-2 API sweep, all suites | `r2-p1…p5.log` (124/60/36/19/18/16/10/6/6 + 95 probes / 60 asserts), `r2-p14.log` 0 fail, `r2-p15.log` 21/21, `r2-p6.log`, `r2-p11.log` 10/10, `r2-p13.log` |
| 22:19 | Coverage gate | `evidence/coverage-round2.json`: 144/144, 472 probe requests |
| 22:30 | Round-2 browser sweep | `r2-routes-all.log`, `r2-design-v2.log`, `r2-p24.log` 8/8, `r2-a11y2.log`, `r2-altcheck.log` |
| 22:36 | Cleanup + parity after the round-2 batch | `AUDIT_CLEAN_PARITY=1471\|43737\|76\|127\|28\|15\|4\|126\|14\|5`, `PARITY_OK=True` |

## What this log deliberately records as NOT done

- `/actuator/mappings` (404) → source-derived inventory instead (F112, BLOCKED).
- No dedicated `p25_adversarial_rederive_v9.js`; the two existing adversarial suites were re-run instead (see `tasks.md` T2.2).
- No read-only subagents (T2.7) — substituted by two independent measurement channels that must agree.
- `spec.md` / `clarify.md` / `checklist.md` / `constitution.md` were not re-authored for v9 (see `tasks.md`, Artifacts).
- Perf work stopped at the one measured hotspot. Index drops/rebuilds and Redis caching were considered and rejected WITH the
  measurement that justifies the decision (`performance.md`), rather than executed for the appearance of optimisation.
