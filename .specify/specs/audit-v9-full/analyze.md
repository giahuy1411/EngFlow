# Analyze — audit-v9-full

> Historical 2026-09-17 evidence. Current audit remains OPEN. Do not use the
> historical PASS tables as current coverage or completion proof. See
> reverify-status.md for 2026-09-18 evidence, interrupted full-suite execution,
> two completed independent reviews, and unresolved acceptance gates.

Cross-artifact consistency check (spec ↔ plan ↔ tasks ↔ findings ↔ evidence), aimed at catching claims that the
artifacts do not actually support.

## Consistency matrix

| Claim in a report | Backing artifact | Verdict |
|---|---|---|
| "F105 closed" | `v9-f105-verify.log` 6/6 (student 404 on draft, admin 200) + `AuditV9DraftLessonGradeGuardTest` 5 tests + 393/393 suite | CONSISTENT |
| "F106 closed" | `f106-red.log` (2 failures) → `f106-green.log` (3/3) → `v9-f106-repro2.log` (200) + DB row healed to 365 | CONSISTENT |
| "F107 was a false positive" | `p1.json` snippet shows `"correctAnswer":null` for a guest; `p2.js` now parses JSON | CONSISTENT |
| "F108: 153 → 29 ms" | `v9_perf_exercise_list_before/after.json` (8 runs each) + the old statement exec count 48 → 48 | CONSISTENT |
| "144/144 endpoint coverage" | `evidence/coverage-round2.json` (472 probe requests) | CONSISTENT |
| "0 overflow / 0 console errors over 228 visits" | `r2-routes-all.log` + `design-v2.json` raw `scrollWidth - clientWidth` per cell | CONSISTENT |
| "`.geo-*` 0 unused" | `v9_deadcss_check.py` (comment-stripping) + `git diff` of the CSS file | CONSISTENT |
| "DB parity restored" | `v9_cleanup_sweep.py` marker line + independent `p16-parity.sql` read | CONSISTENT |
| "font set = {Be Vietnam Pro}" | `r2-design-v2.log`: 7685 elements, 0 non-BVP, 0 legacy hits | CONSISTENT |
| "bundle did not grow" | `audit-v9-baseline-build.log` vs `v9-r1-build.log` (JS entry identical, CSS smaller) | CONSISTENT |

## Contradictions found and resolved during analysis

1. **First v9 pass reported `WARNING: correctAnswer leaked in /exercises response`.** Contradicted by the raw body in
   `p1.json` (`"correctAnswer":null`). Resolution: harness defect (F107), not an application defect. The lesson —
   a substring test on a serialized field name can never distinguish "present but null" from "exposed".
2. **`p4e` reported "admin create (hợp lệ) 429 UNEXPECTED" in BOTH v9 passes.** Contradicted by the v8 log where the
   same probe returned 201. Resolution: the harness bypasses its own bucket flush (F109). The rate limiter is correct;
   the harness was measuring its own throttling.
3. **The first cleanup script reported `PARITY_OK=False`** with users 76 → 72 and a bare `email LIKE 'zz%'` filter.
   Resolution: the filter also matched four legacy audit users that are part of the baseline and a documented backlog
   item (F111). They were restored verbatim from the 21:06 backup and the filter was scoped to the audit day.
4. **The old SRS test could never fail.** `assertTrue(x == false || x == true)` — tautology. It was replaced by two
   tests that flush the persistence context so the failing UPDATE actually runs (RED before the fix, GREEN after).
   Worth stating because the original test's presence in the tree could have been mistaken for coverage.

## Quality gates

| Gate | Threshold | Measured | Verdict |
|---|---|---|---|
| ERROR/CRITICAL findings open | 0 | 0 (9 findings: 8 FIXED, 1 BLOCKED/INFO) | PASS |
| Endpoint coverage | 144/144 | 144/144 | PASS |
| Failed probes | 0 | 0 (p1–p5, p6, p8, p10–p15, p24) | PASS |
| Backend tests | all green | 393/393 | PASS |
| Frontend tests | all green | 90/90 in 19 files | PASS |
| Console errors in browser sweep | 0 | 0 | PASS |
| Horizontal overflow | 0 | 0 of 60 cells + 0 of 228 visits | PASS |
| Tap targets < 24 px | 0 | 0 | PASS |
| Images missing `alt` | 0 | 0 | PASS |
| Bundle entry growth | <= baseline | JS 0 kB, CSS -7.20 kB | PASS |
| DB parity | baseline | baseline, `PARITY_OK=True` | PASS |

No gate is satisfied by a skipped test: the backend run reports 0 skipped, and every probe failure would have printed
`FAIL=` > 0 in the aggregate lines quoted above.

## Residual risk

- `p13` still documents 3 endpoints that answer a legacy `{"error": ...}` shape instead of RFC-7807 ProblemDetail
  (empty word, missing word, save-vocab empty array). Unchanged from v8, deliberately not "fixed" in this pass —
  changing error shapes is a contract change for the frontend and belongs to its own task. Listed in `issues.md`.
- The 4,848 ungradeable exercises and the 9 LISTENING items without audio remain learner-visible data gaps.
- `user_vocabulary_progress` intervals above 365 days written by the pre-fix code are not all audited: the one known
  bricked row is healed, and the new cap makes future writes safe (max interval measured in the table is 365).
