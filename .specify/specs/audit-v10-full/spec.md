# audit-v10-full specification

**Authored:** 2026-09-20 (+07) · **Predecessor:** `audit-v9-full` · **Branch:** `audit-streak-review`

## Objective

Run a full local backend/API/DB/UI audit of EngFlow that (a) first restores the green test
baseline broken by an in-flight streak refactor, (b) completes that refactor so streak means
*completed learning activity* rather than *login*, (c) forces the UI to the literal
Playful Geometric prompt, (d) clears four measured backlog items, and (e) re-runs everything
a second, wider time before reporting.

## Requirements

### R1 — Green baseline is a precondition
The working tree entered this audit RED (425/432 backend tests). No finding may be closed,
no backlog item touched, and no UI change made until the backend and frontend suites pass
again, with the counts read from the run log (never from `target/surefire-reports` XML).

### R2 — The streak refactor must be completed, not parked
`StudyActivityService.recordStudy()` throws `IllegalStateException("Study policy is not
effective yet")` when `today < effectiveFrom`, and `effectiveFrom()` throws when the
`study_policy` row is absent. The new code and the new schema are therefore one unit.
`tasks/streak-study/deploy.sql` is authored but has never been executed. This audit executes
it against the development database with an explicit, immutable cutover date.

### R3 — Every real completion producer is wired, and only real ones
`tasks/streak-study/README.md` requires that every genuine completion producer call
`recordStudy` **inside the transaction that persists the result**. Upload-only, client-declared
or otherwise unverified completion must NOT be wired. Each candidate producer is verified
against its source before being wired, and wiring is proven by a test that shows the study day
rolls back with the result.

### R4 — The UI is forced to the literal prompt
The prompt's concrete visual claims are implemented: `max-w-6xl` container, a massive yellow
circle plus blob-masked shape in the hero, a dashed connector between feature cards, a
`scale(1.1)` featured pricing card with a `rotate(15deg)` badge, squiggle section dividers, an
infinite marquee, and an ArrowRight-in-white-circle affordance on the primary button. Where the
prompt is factually wrong about this codebase (Lucide React, Tailwind utility names), the prompt
is corrected rather than the code bent to a fiction. Every change is measured in a real browser
at 360/768/1280/1440/1920 px.

### R5 — Backlog items are researched before they are touched
The four measured backlog items (4,848 empty `correct_answer`, 9 LISTENING rows without
`audio_url`, 8 legacy `zz*` users, 4 `exercises_bak_v5*` tables, 3 legacy error shapes) are
re-measured first. Any number that has drifted is investigated before action. Destructive
steps require a fresh backup and an explicit ID list — never a bare `LIKE` pattern.

### R6 — The second pass is wider than the first
After the first pass closes its gates, the whole surface is re-run on the final build with
additional adversarial cases for the new streak endpoint, streak boundary conditions, a
repeated browser sweep, and a cross-artifact consistency check.

### R7 — Evidence discipline
Every claim points at the artifact that produced it. Anything not verified is reported as
BLOCKED or PARTIAL with the reason, never omitted and never implied by a neighbour's success.

## Success criteria — measured outcomes

| Criterion | Result |
|---|---|
| Backend and frontend suites green; counts from the run log | ✅ **456 run / 0 failures / 0 errors / 11 skipped** · frontend **106 passed / 1 skipped** |
| `vite build` succeeds; JS entry does not grow without a stated reason | ✅ **177.31 kB**, unchanged from the pre-audit baseline |
| `GET /api/streak/snapshot` returns 200 with the documented contract on the deployed container | ✅ **200**, all 7 fields, `effectiveFrom = 2026-09-20` |
| Study-day semantics proven end-to-end | ✅ **KB1**: 3 logins → `study_days` still 0. **KB2**: SRS review → row written, repeat does not double. **KB3**: snapshot reflects it. **KB4**: cutover blocks the day before it. **KB5**: login-only user has `studiedToday=false`. **25/25 PASS** |
| UI verified at five viewports, zero overflow / console errors / wrong landings / non-Be-Vietnam-Pro nodes / token drift / missing `alt` | ✅ **184 assert / 0 FAIL** at 360/768/1280/1440/1920 · **342 browser visits, 0 errors** · **0/878 images missing alt** |
| DB row parity re-asserted after every mutating run, new baseline recorded | ✅ `1471\|43737\|72\|127\|28\|15\|4\|126\|14\|5` — only `users` moved (76→72), explained |
| Report states exactly which items are DONE, PARTIAL, BLOCKED or DEFERRED | ✅ `REPORT.md` §4 and §7 |

## Findings beyond the plan

Three defects were found that the specification did not anticipate. They are recorded because
an audit that only confirms its own checklist is not an audit:

- **F126 (HIGH)** — draft lesson *content* leaked through `GET /api/lessons/{id}/structure`.
  Measured: lesson 10889 returned **8,013 bytes** of real lesson content to a student while
  `/api/lessons/10889` returned 404.
- **F127 (HIGH)** — MATCHING exercises could never be graded correct. Measured: 330/331
  published rows store the answer as text, the client sends indices, and 4/4 published lessons
  returned `correct=false` when answered correctly.
- **F125 (MEDIUM)** — `font-weight: 800` used 17 times but absent from the font URL, so the
  browser synthesized it from 900. Measured by string width: 800 and 900 both **633.000px**.

## Probe defects found and corrected

Equally part of the record — a probe that reports a defect the app does not have is as costly
as a missed defect, because the natural "fix" is to change correct code.

| Probe bug | How it surfaced |
|---|---|
| `document.querySelector(".app-btn")` picked the **secondary** "Thoát" button, so the primary button appeared to lack its shadow and arrow | 15 FAILs across 5 viewports; `diag-buttons.js` showed the primary buttons had the shadow all along |
| `stroke-dasharray` read from `<svg>` instead of the `<path>` inside it | connector reported as "not dashed" while `path@stroke-dasharray="10 10"` was present |
| `NOT EXISTS (...)` counted **NULL** foreign keys as orphans | reported 39 orphans; all 39 were NULL in nullable columns, real orphans = 0 |
| A probe sent the **raw** `correct_answer` string, which the UI never sends | "passed" while the real UI path was broken — this is the bug that hid F127 |
| Two probes hard-coded the date `2026-09-19` | both failed at midnight while the app was correct |

## Boundaries

- Development/verification environment only; this is a graduation project, not a production rollout.
- Do not expose additional actuator endpoints for auditing.
- Do not add a Flyway migration; the schema is Hibernate `ddl-auto=update` plus reviewed SQL.
- Do not drop indexes or add caching without a measured benefit.
- Do not commit `.env`, credentials, or `frontend/public/*.wav`.
- Do not synthesize missing answer keys; classify them instead.
- Do not mutate backlog data unless a fresh backup exists and the exact IDs are enumerated.
