# Checklist — audit-v10-full

Pre-flight gates that must hold before the audit claims anything. Each row names the artifact
that proves it. A row with no artifact is not done.

## Baseline (must be green before any finding is closed)

- [x] `mvnw.cmd test` run **after** the Phase 1.1 fixes → counts read from the run log
- [x] `npx vitest run` in `frontend/` → counts read from the run log
- [x] `npx vite build` → JS/CSS entry sizes recorded
- [x] `docker ps` → container health recorded
- [x] Baseline counts differ from the RED entry state (432 run / 3 failures / 2 errors) → **456 / 0 / 0**

## Schema (streak refactor)

- [x] Backup taken, `RESTORE VERIFYONLY` passed, SHA256 recorded
- [x] `deploy.sql` executed with `EffectiveFrom=2026-09-20`
- [x] `sys.tables` shows `study_policy` and `study_days`
- [x] `sys.foreign_keys` shows the `study_days.user_id → users.id` FK
- [x] `sys.indexes` shows the unique index on `(user_id, study_date)`
- [x] `study_policy` has exactly one row, `effective_from = 2026-09-20`
- [x] Re-running `deploy.sql` refuses to change the existing cutover
- [x] **F124:** FK creation proven idempotent on a scratch DB (0 → 1 → 1, no duplicate)

## Streak semantics (the whole point of the refactor)

- [x] `GET /api/streak/snapshot` returns 200 with all 7 contract fields
- [x] Completing a real activity writes a `study_days` row for today
- [x] Logging in alone writes **no** `study_days` row — **KB1: 3 logins, still 0 rows**
- [x] A gap day breaks the streak — covered by `fourDayJourneyDoesNotInheritLegacyStreak`
- [x] `StreakCalendar` distinguishes legacy access history from completed study days
- [x] `recordStudy` outside a transaction fails loudly (MANDATORY propagation holds)

## Producers

- [x] Each wired producer calls `recordStudy` inside the persisting transaction
- [x] Rollback removes both the result row and the study day (test re-run, failure injector moved to the inactive-user path)
- [x] Producers that must NOT be wired are documented with the reason (video quiz, skill submission, shadowing upload)

## UI vs the literal prompt

- [x] Container measures 1152px at ≥1280px viewport — **measured, 3 viewports**
- [x] Section padding measures 96px from `md` up — **measured on every `.geo-section`**
- [x] Hero has a yellow circle (`#FBBF24`, 50% radius) and a blob shape
- [x] Feature cards have a dashed connector (`stroke-dasharray="10 10"`), hidden on mobile
- [x] Featured pricing card is scaled 1.1 with a rotated 15deg badge, and does **not** overflow
- [x] Squiggle dividers render between sections
- [x] Marquee animates infinitely, disabled under `prefers-reduced-motion`
- [x] Primary button arrow affordance renders (white circle, ArrowRight, stroke 2.5)
- [x] Zero horizontal overflow at 360/768/1280/1440/1920
- [x] Zero console errors on every route — **342 visits**
- [x] Zero text nodes not in Be Vietnam Pro — **7 routes, only family found**
- [x] Zero token drift across the 11 `--geo-*` tokens
- [x] Zero `<img>` missing the `alt` attribute — **0 / 878**
- [x] No tap target below 24px (WCAG 2.5.8 AA)
- [x] **F125:** every weight actually used has a loaded face (800 was synthesized from 900)

## Backlog

- [x] All four items re-measured; any drift investigated before action — **zz users measured 4, not 8**
- [x] Fresh backup before DML (`engflow_2026-09-20-pre-backlog-dml.bak`, VERIFYONLY valid)
- [x] Deletions use an explicit ID list, never a bare `LIKE`
- [x] `SET QUOTED_IDENTIFIER ON` present in every DELETE batch
- [x] Output scanned for `Msg \d+` (sqlcmd exits 0 on failure)
- [x] Parity line re-asserted and the new baseline recorded (`…|72|…`)

## Evidence discipline

- [x] Every claim in the report points at an artifact path — **14 evidence files**
- [x] Anything unverified is marked BLOCKED or PARTIAL with the reason
- [x] No claim rests on a neighbour's success

## Probe discipline (learned the hard way this round)

- [x] Every probe that failed was checked against the source before the app was blamed
      — three probe bugs found (`querySelector(".app-btn")` picked a secondary button,
      `stroke-dasharray` read from `<svg>` not `<path>`, `NOT EXISTS` counted NULLs as orphans)
- [x] Every probe that passed was checked to make sure it tested what the client actually does
      — the first F127 probe passed by sending a raw string the UI never sends
- [x] No hard-coded dates in probes (they become time bombs: two probes failed at midnight)
- [x] No `sqlcmd` exit code trusted without scanning for `Msg \d+`

