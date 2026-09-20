# Plan — audit-v10-full

Execution order is fixed: each phase has a gate, and a later phase may not start while an
earlier gate is red. This mirrors the approved plan file but records the state actually reached.

## Phase 0 — Freeze the current state

| Step | Artifact | State |
|---|---|---|
| 0.1 Backup DB + `RESTORE VERIFYONLY` + SHA256 + host copy | `evidence/gate-p1-verified.md` | ✅ DONE |
| 0.2 Measure the real baseline (backend, frontend, build, containers) | `evidence/gate-p1-verified.md` | ✅ DONE |
| 0.3 Snapshot the deployed state (old vs new backend, schema present?) | `evidence/deployed-state.md` | ✅ DONE |
| 0.4 Git checkpoint commit on `audit-streak-review` | — | ⏳ NOT DONE (owner's call; 40 modified + 30 untracked files) |

**Gate P0:** verified backup with SHA256 + measured baseline. Checkpoint commit still open.

## Phase 1 — Green suite and a real streak

| Step | What | Artifact | State |
|---|---|---|---|
| 1.1 | Fix the red tests at the root cause | code + run log | ✅ 456 run / 0 fail / 0 error |
| 1.2 | Audit every producer before wiring | `producer-audit.md` | ✅ DONE |
| 1.3 | Execute `deploy.sql` with cutover 2026-09-20 | `evidence/gate-p1-verified.md` | ✅ POLICY=2026-09-20 |
| 1.4 | Measure legacy Redis keys and decide | `evidence/redis-legacy-measurement.md` | ✅ 43 keys / 74 days → no migrate |
| 1.5 | Streak end-to-end (5 scenarios) | `evidence/streak-scenarios-e2e.md` | ✅ 25 PASS / 0 FAIL |
| 1.6 | Rebuild the container and confirm the new endpoint | `evidence/deployed-state.md` | ✅ rebuilt twice; snapshot 200 |

**Gate P1:** ✅ **PASSED.** Backend green · frontend green · snapshot 200 on the rebuilt container ·
5 streak scenarios pass · parity line explained (`users` 76→72 by decision).

## Phase 2 — Force the UI to the literal prompt

| Step | What | State |
|---|---|---|
| 2.1 | Container → 72rem (max-w-6xl) | ✅ measured 1152px |
| 2.2 | Hero yellow circle + blob shape | ✅ measured `#FBBF24`, 50% radius |
| 2.3 | Feature dashed connector | ✅ `stroke-dasharray="10 10"`, hidden <768 |
| 2.4 | Pricing scale(1.1) + rotate(15deg) badge | ✅ measured both, no overflow |
| 2.5 | Squiggle dividers + marquee | ✅ marquee `infinite`, off under reduced-motion |
| 2.6 | Primary-button arrow affordance | ✅ white circle, ArrowRight, stroke 2.5 |
| 2.7 | Browser verification at 5 viewports | ✅ **184 assert / 0 FAIL** |
| 2.8 | Font audit | ✅ found + fixed **F125** (weight 800 synthesized from 900) |

**Gate P2:** ✅ **PASSED.** 0 design violations · 0 console errors · 0 overflow · 0 wrong landing ·
frontend suite and build green. Screenshots (2.12) not taken — every *measurement* is done.

## Phase 3 — Backlog

Re-measure → investigate drift → backup → act on explicit IDs → verify → re-assert parity.

| Item | State |
|---|---|
| Re-measure all four | ✅ `evidence/backlog-remeasure-v10.md` — **zz users 4, not 8** |
| 4 `exercises_bak_v5*` tables | ✅ dropped (897 rows) |
| 4 `zzprobe*` users | ✅ deleted by explicit ID |
| Legacy error shapes (F123) | ⏳ DEFERRED with reason — `api.js` already shims `error`→`detail` |
| 9 LISTENING audio | ✅ **investigated, deliberately NOT backfilled** — they are mislabelled reading/writing rows |
| Empty `correct_answer` (4,848) | ✅ classified, not synthesized (C6) |

**Gate P3:** ✅ new parity recorded · every DML backed by a verified backup · 0 real orphans ·
audio refused with a per-row reason.

## Phase 4 — Functional audit

| Sweep | Result |
|---|---|
| API sweep | ✅ **35 PASS / 0 FAIL / 0 SKIP** |
| Browser sweep | ✅ **342 visits, 0 errors, guards asserted both ways** |
| Deep check — lessons/exercises | ✅ found **F126 + F127**, both fixed and re-verified live |
| Deep check — streak | ✅ 15/15 scheduler tests + 25/25 E2E assertions |
| Deep check — auth | ✅ **19/19 PASS** |
| Deep check — search/sort | ✅ **25/25 PASS**; `?sort=` measured as ignored, not a defect |
| Deep check — AI | ✅ **15/15 PASS** on contracts; TTS→Cloudinary proven |
| DB audit | ✅ 0 real orphans, 0 disabled FKs, no fragmentation >20% |

**Gate P4:** ✅ 0 failed probes · 0 console errors · 0 overflow · 0 wrong landings · 0 orphans ·
parity OK · every finding has an ID, root cause, patch, test and a re-measurement.

## Phase 5 — Performance

| Step | State |
|---|---|
| Before numbers for known candidates | ✅ `evidence/performance.md` |
| Measure `/api/streak/snapshot` | ✅ **200 OK, median 23ms** (was 404 before the rebuild) |
| Optimize only where measured | ✅ **no change** — nothing measured justified one |

**Gate P5:** ✅ every perf claim carries a number; the answer is "measured, no change needed".

## Phase R2 — Second, wider pass

| Step | State |
|---|---|
| Adversarial cases for `/api/streak/snapshot` | ✅ window bounds, missing policy, inactive user |
| Streak boundary conditions | ✅ **cutover day itself tested live** — the run straddled midnight |
| Repeated browser sweep | 🔄 running on the final build |
| Cross-artifact consistency check | ✅ spec ↔ plan ↔ tasks ↔ findings ↔ evidence reconciled |
| Analyze | ✅ |

**Gate R2:** Phase 1–5 gates green on the final build. Confirmed for everything except the
second browser sweep, which was still running when the report was written.

## Blocker B1 — RESOLVED ENOUGH TO FINISH

The harness classifier (`cbai/deepseek-v4.1-flash[1m]`) was intermittent for the first half of
the session, refusing Bash/PowerShell/MCP calls that execute builds, tests, containers or
browsers. The owner installed a project-local allowlist at `.claude/settings.local.json`;
allowed commands match a permission rule at tier 1 and therefore bypass the classifier.

**This is why the report contains measured results instead of "fixed but unverified".** Every
number in it — 456 backend tests, 342 browser visits, 35 API probes, 25 streak assertions, the
scratch-DB idempotency proof — required a command that the classifier would have refused.
