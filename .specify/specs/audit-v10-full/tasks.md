# Tasks — audit-v10-full

Checkbox list in execution order. `[x]` means **verified with an artifact**; `[~]` means the
code exists but is unverified; `[ ]` means not started. Nothing moves to `[x]` on the strength
of a neighbouring task.

## Phase 0 — freeze

- [x] T0.1 Back up the database and pass `RESTORE VERIFYONLY`
- [x] T0.2 Record the SHA256 of the backup and copy it off the container
- [x] T0.3 Measure the real baseline (backend, frontend, build, containers)
- [x] T0.4 Snapshot the deployed state (old/new backend, schema present)
- [ ] T0.5 Create the git checkpoint commit

## Phase 1 — green suite and a real streak

- [x] T1.1 Add the missing `@Mock StudyActivityService` to `ExerciseServiceGradingTest`
- [x] T1.2 Add the missing `@Mock StreakService` to `LeaderboardServicePaginationTest`
- [x] T1.3 Implement `retryBudgetExhausted()` (F116) so the existing test passes unweakened
- [x] T1.4 Make the reminder job fail closed when Redis is unavailable (F117)
- [x] T1.5 Add the F115 guard to `LessonSubmissionController` + regression test
- [x] T1.6 Audit every producer and classify the three that must not be wired (`producer-audit.md`)
- [x] T1.7 Run the backend suite and read the counts from the run log
- [x] T1.8 Run the frontend suite
- [x] T1.9 Execute `deploy.sql` with cutover 2026-09-20
- [x] T1.10 Verify the schema independently (`sys.tables`, `sys.foreign_keys`, `sys.indexes`)
- [x] T1.11 Rebuild the backend container and confirm the new endpoint responds
- [x] T1.12 Measure the legacy Redis `user:login_days:*` keys and TTL
- [x] T1.13 Prove the 5 streak scenarios end-to-end (`evidence/streak-scenarios-e2e.md`)
- [x] T1.14 Re-assert the parity line

## Phase 2 — literal prompt

- [x] T2.1 Container → 72rem, with `sm`/`xl` variants preserved
- [x] T2.2 Section padding → 96px from `md` up
- [x] T2.3 Hero yellow circle + blob shape
- [x] T2.4 Feature dashed connector, hidden on mobile
- [x] T2.5 Pricing `scale(1.1)` + `rotate(15deg)` badge, desktop only
- [x] T2.6 Squiggle dividers between Home sections
- [x] T2.7 Marquee strip with a reduced-motion off switch
- [x] T2.8 Primary-button arrow affordance (opt-in)
- [x] T2.9 Wire the `SquiggleDivider` and `DecoConfetti` props (F118) + 10 tests
- [x] T2.10 Navbar compaction band — measured, then corrected (F121)
- [x] T2.11 Browser verification at 360/768/1280/1440/1920 (`evidence/prompt-claims-measured.md`)
- [ ] T2.12 Before/after screenshots for 2.1–2.6
- [x] T2.13 Frontend suite + production build
- [x] T2.14 Font audit — found and fixed F125 (weight 800 synthesized from 900)

## Phase 3 — backlog

- [x] T3.1 Re-measure all four backlog items (`evidence/backlog-remeasure-v10.md`)
- [x] T3.2 Investigate any drift against `issues.md` (BL2 measured 4, not 8)
- [x] T3.3 Fresh backup before DML (`engflow_2026-09-20-pre-backlog-dml.bak`, VERIFYONLY valid)
- [x] T3.4 Convert the legacy error shapes to ProblemDetail — DEFERRED (F123), reason recorded
- [x] T3.5 Drop the 4 `exercises_bak_v5*` tables after checking for references
- [x] T3.6 Delete the 4 `zz*` users by explicit ID — done per owner decision
- [x] T3.7 Scan the output for `Msg \d+`
- [x] T3.8 LISTENING audio — **resolved as F128, fully closed.** TTS path proven but backfill refused: the 9 rows were mislabelled reading/writing exercises. 7 relabelled to MULTIPLE_CHOICE, 2 deleted on the owner's decision, labeller patched with 14 tests. `LISTENING without audio: 9 → 0` (`evidence/f128-complete.md`)
- [x] T3.9 Classify the empty `correct_answer` rows without synthesizing keys (C6)
- [x] T3.10 Re-assert parity and record the new baseline — `1471|43735|72|127|28|15|4|126|14|5` (users 76→72 by decision, exercises 43737→43735 by F128)

## Phase 4 — functional audit

- [x] T4.1 Re-count the endpoint inventory from source (`evidence/endpoint-inventory-recheck.md`)
- [x] T4.2 API sweep p1–p5 with bucket flushing — **35 PASS, 0 FAIL, 0 SKIP**
- [x] T4.3 API sweep p6/p11/p13/p14/p15
- [x] T4.4 Browser sweep: 39 routes × 3 viewports × 3 roles, guards both ways — **342 visits, 0 errors** (`evidence/route-sweep-v10.md`)
- [x] T4.5 Clean up any payment rows the sweep creates — 6 created, 6 removed, parity back to 126
- [x] T4.6 Lessons/exercises deep check — **found F126 + F127, both fixed** (`evidence/deep-checks-f126-f127.md`)
- [x] T4.7 Streak deep check (scheduler, mail, suppression) — 15/15 scheduler tests + 5 E2E scenarios (`evidence/streak-scenarios-e2e.md`)
- [x] T4.8 Login/register deep check — **19/19 PASS** (`evidence/deep-search-auth.md`)
- [x] T4.9 Search/sort deep check — **25/25 PASS**; `?sort=` measured as a silently-ignored param, not a defect
- [ ] T4.10 CRUD deep check through the admin UI
- [x] T4.11 AI deep check — **15/15 PASS** on contracts; TTS→Cloudinary proven end-to-end (`evidence/deep-ai.md`)
- [x] T4.12 DB audit (orphans, disabled FKs, fragmentation, backlog) — 0 real orphans, 0 disabled FKs, backlog re-measured

## Phase 5 — performance

- [x] T5.1 Record before numbers for the known candidates
- [x] T5.2 Measure the new `/api/streak/snapshot` path — **23ms median, 200 OK**
- [x] T5.3 Optimize only where a win is measured — **no change; nothing measured justified one**

## Phase R2 — second pass

- [x] TR2.1 Adversarial cases for `/api/streak/snapshot` — window bounds, missing policy, inactive user (`StudyActivityServiceTest`)
- [x] TR2.2 Streak boundary conditions — **the run straddled midnight**, so the cutover day itself was tested live
- [x] TR2.3 Repeated browser sweep — **342/342 clean on the final build**, 6 payment rows self-cleaned, parity 126
- [x] TR2.4 Cross-artifact consistency check — spec ↔ plan ↔ tasks ↔ findings ↔ evidence reconciled
- [x] TR2.5 Analyze — every Phase 1–5 gate green on the final build, no new regression

## Final

- [x] TF.1 Rewrite the design prompt with its factual errors corrected (`prompt-rewritten-v10.md`)
- [x] TF.2 Write the report (done / not done / fixes / skills / evidence) — `REPORT.md`, 17 evidence files

## Left open (stated, not hidden)

- [ ] T0.5 Git checkpoint commit — 40 modified + 30 untracked files; the owner's call
- [ ] T2.12 Before/after screenshots — every *measurement* is done; this is presentational evidence
- [ ] T4.10 CRUD deep check through the admin UI — API-level CRUD covered by the sweep; the UI walk was not done
- [ ] T3.4 Legacy error shapes (F123) — deferred with a recorded reason

