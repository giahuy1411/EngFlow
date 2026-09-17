# Converge — audit-v8-full

**Feature:** audit-v8-full · **Date:** 2026-09-16
**Step:** `… → implement → converge → analyze 2 → taskstoissues → report`

Convergence asks one question: **does the codebase now satisfy the spec, and if
not, what unbuilt work remains?** Remaining work is appended as new tasks rather
than described in prose. This file records the assessment, the appended tasks,
and — importantly — the work deliberately **not** appended and why.

## 1. Spec ↔ implementation coverage

| Spec requirement | Implemented | Evidence | Gap |
|---|---|---|---|
| Audit backend, DB, frontend, UI/UX, security, AI, performance | ✅ | `REPORT.md` §1, five audit documents | none |
| Be Vietnam Pro is the only font | ✅ (verified, not re-migrated) | `design-audit.md` §1 | none |
| Findings have ID/severity/repro/root cause/files/fix/test/status | ✅ | `findings.json`, 10/10 | none |
| Required evidence layout complete | ✅ | 6 documents written during analyze 1 | none |
| Every claim carries fresh evidence | ✅ | `analyze.md` §A6, 12/12 fresh, 3 stated boundaries | none |
| Viewports 360/768/1280/1440/1920 | ✅ | `design-v2.js`, 60 combos | none |
| No outage reported as pass | ✅ | all services healthy throughout; no `BLOCKED` status used | none |
| DML preceded by backup / scoped cleanup | ✅ | `db-audit.sql` read-only; cleanup row-scoped and verified | none |
| `SET QUOTED_IDENTIFIER ON` for DELETE | ✅ | `clean_r2.sql`, `clean-xss-probe.sql` | none |

**No spec requirement is unimplemented.** Convergence therefore appends **no new
implementation tasks** — the honest outcome when the gap list is empty.

## 2. What convergence actually changed

Convergence did not add features. It closed **artifact** gaps that analyze 1
found, each backed by a re-runnable harness rather than prose:

| Artifact written | Harness that produces its evidence | Result |
|---|---|---|
| `route-map.md` | `sweep/v8/route_inventory.py`, `route_verify.py`, `ui/routes-all.js` | 39 routes, CONSISTENT, 228 visits / 0 defects |
| `db-audit.md` | `sweep/v8/db-audit.sql` | 0 SQL errors, parity exact |
| `performance.md` | `sweep/v8/perf.js`, `perf-reads.sql` | N=7 live, F87 shape confirmed |
| `design-audit.md` | `ui/design-v2.js`, `ui/design.js` | 0/7 685 non-BVP, 0 AA violations |
| `security-audit.md` | `security.js`, `xss-prove.js`, `p8_bucket_coverage.js` | 0 failures |
| `constitution.md` | — (compliance record) | 0 violations, 1 deferred PATCH |
| `analyze.md` | — (this pass's record) | 2 passes, 0 code changes |

**Six harnesses were also corrected during convergence**, because each was
producing a wrong number. These corrections matter more than the documents they
feed — a harness that lies is worse than no harness:

| Harness defect | Wrong result it produced | Fix |
|---|---|---|
| Route parser appended the catch-all twice | 40 routes (true: 39) | removed the duplicate; added a hard `exit(2)` on duplicate paths |
| One lesson id substituted into every `:id` | 20 phantom 404s across decks/speaking/videos | per-family ids verified against the live DB |
| Sweep exhausted its own rate limit + JWT TTL | 3 phantom 401/429 "defects" | re-seed token + flush only own `rate_limit:*` every 8 visits |
| XSS check matched the substring `onload=` | false positive on already-defanged JSON | test for real tags + real browser execution |
| Over-correction: raw tag in JSON = failure | flaky 1-in-3 false failure | reclassified as informational; verdict moved to `xss-prove.js` |
| `security.js` saved model output as vocab rows | a read-only audit was writing business data (4 rows) | moved the write into `xss-prove.js`; cleaned up; parity restored to 127 |

The last one is the most serious: an audit script that mutates business data
cannot be trusted as evidence. It was found by re-checking DB parity rather than
by assuming the script was safe, and `security.js` is now write-free.

## 3. Deliberately NOT converged

Appending work here would be dishonest, because each item below is either
already correct, unmeasurable, or outside this audit's authority.

| Item | Why not appended |
|---|---|
| **F90 filter ordering** | Measured, not a bypass: 0 of 17 `permitAll` surfaces reach the app unthrottled. Changing `@Order` alters the security chain for **no measured benefit**. Status stays `CLOSED-NOT-A-BUG`. |
| **F93 live 504** | The only way to force it is making local Ollama exceed 30 s, which is GPU-load-dependent. Unit tests + container inspection cover it; the boundary is recorded, not papered over. |
| **F87 before/after re-measurement** | Requires reverting the fix and rebuilding. The "before" is a documented historical measurement and `performance.md` labels it as such. |
| **138 tap targets at 24–44 px** | WCAG 2.2 **AA** requires ≥24 px and is met. 44 px is AAA. Widening dense admin tables would be a real usability regression. |
| **`noShadow=46`, `thinBorder=5`** | The system applies hard shadows and 2 px borders to containers, not every descendant. Not defects. |
| **9 zero-seek indexes** | Plan cache was reset by container restarts, so a low count reflects a cold cache. Dropping on that reading would be unsound, and P5 forbids an unmeasured optimisation. |
| **8 pre-existing audit users** | Created by *earlier* rounds (July–Sept 12), not this one. A run cleans up what it created; deleting other rounds' fixtures destroys their evidence. |
| **6 empty-content lessons** | Test/draft artefacts, not published curriculum. Deleting curriculum rows is an owner decision. |
| **4 `exercises_bak_v5*` tables** | Outside the entity model, harmless at 897 rows. Retention decision. |
| **11 NULL `content_original`** | Kept permanently by decision P5.1 (2026-09-12). Re-proposing a drop is explicitly forbidden. |
| **Constitution P1 numbers (221/73 → 378/89)** | P1 itself obliges each audit to re-measure and record in the report, which is done. Amending the constitution is a governance act (Article III, semver bump + Sync Impact Report). Exact diff proposed in `constitution.md` §3. |
| **`tasks.md` retro-fill for F90–F94** | **Withdrawn — see §4.1.** Originally: writing a task plan that did not exist at the time would fabricate history; `REPORT.md` §2b + `findings.json` are the authoritative record. On review that reasoning was too broad: recording *completed, evidenced* work is provenance, not fabrication. T42–T62 were appended so `tasks.md` no longer contradicts `REPORT.md`. |
| **`taskstoissues`** | A GitHub remote exists, so the step is *applicable*, but no unbuilt work remains to file. Filing "audit is complete" issues would be noise. Decided in `REPORT.md` §6. |

## 4. Convergence tasks appended

**None appended by convergence.** No spec requirement is unmet, so convergence
itself adds no task. This is a deliberate outcome, not an omission: convergence
appends tasks when *work remains*, and claiming otherwise would inflate the list
with work already done.

### 4.1 Correction: `tasks.md` provenance catch-up (T42–T62)

A separate, later pass appended **T42–T62** (21 entries) to `tasks.md`. This
**revises an earlier decision in §3 of this file**, which argued that a
retro-fill "would fabricate history." That reasoning was **too broad** and is
withdrawn:

- What would fabricate history is inventing a task *plan* that never existed and
  presenting it as pre-planned. That is **not** what happened.
- What did happen: recording *completed, already-verified* work, with each entry
  citing the harness and log that produced it. That is **provenance**, not
  fabrication — the same standard `findings.json` already uses.
- The decisive argument: `tasks.md` is the machine-readable task list and the
  input to `taskstoissues` (Task 15). Leaving it at "369 tests, T41 final" while
  `REPORT.md` reports **378 tests** and 15 findings made the two artifacts
  **contradict each other** — precisely the contradiction Task 4 asks the
  consistency analysis to catch, and it would have made the Task 15 decision rest
  on a stale file.

Each of T42–T62 follows the same shape as T01–T41: the finding or task, the
harness, and the measured result. No T-number asserts work that has no evidence
behind it. The distinction that matters is **claimed vs. evidenced**, not
**pre-planned vs. recorded**.

## 5. Final state verification

Run after the last write, on the frozen tree:

| Check | Result |
|---|---|
| `cmd /c "mvnw.cmd test"` | **378/378**, `BUILD SUCCESS` |
| `npx vitest run` | **89/89 in 18 files** |
| `npx vite build` | **176.80 kB / gzip 67.39** |
| `git diff --check` | **clean** — no whitespace errors |
| DB parity | lessons 1 471 · exercises 43 737 · users 76 · vocabulary 127 · submissions 28 · video_attempts 15 · lesson_submissions 4 · payments 126 · decks 14 · snapshots 5; orphans **0/0/0/0/0** |
| Audit rows left behind by this session | **0** |
| Files with BOM | **0 / 26** |
| Placeholder scan (`TODO/TBD/FIXME`) | **0** |

> **Correction to an earlier revision of this table.** It previously read
> `git diff HEAD` = **empty** — "tested tree == committed tree". That was true at
> the moment convergence ran, but the artifact-authoring pass afterwards
> modified `REPORT.md`, `findings.json`, `tasks.md`, `workflow-log.md` and this
> file, so the statement is **no longer accurate** and has been replaced with the
> check that is still meaningful (`git diff --check`). The tracked-file diff is
> now non-empty by design: artifacts were edited after the last commit. Stating
> otherwise in a final report would be exactly the kind of unsupported claim this
> audit forbids.

## Converge verdict

| Question | Answer |
|---|---|
| Does the codebase satisfy the spec? | **Yes** — 9/9 requirements met |
| Unbuilt work remaining? | **No** |
| New tasks appended *by convergence*? | **0** |
| Provenance entries appended later (§4.1)? | **21** (T42–T62) |
| Blocking issues? | **0** |
| Deliberate non-convergences | 13, each with a stated reason |
| Readiness for `taskstoissues` | Applicable but **no open work to file** |
| Readiness for the final report | **Ready** |
