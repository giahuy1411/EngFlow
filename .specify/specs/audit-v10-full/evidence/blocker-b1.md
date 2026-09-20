# Blocker B1 — command classifier outage

## What

The harness command classifier (`cbai/deepseek-v4.1-flash[1m]`) has been unavailable for an
extended period. When it is down, auto mode cannot decide whether a command is safe, so it
refuses the call. Read-only commands are explicitly exempted and still work.

## What is refused

Every command whose safety needs a fresh decision, including the ones this audit depends on:

- `./mvnw.cmd -o -Dtest=... test` — backend suite and every targeted test
- `java -version`
- `npx vitest run` / `npx vite build`
- `docker exec engflow-sqlserver ... sqlcmd ...` — every DB read and write
- `docker exec engflow-redis redis-cli ...`
- `cmd //c "..."` — any batch wrapper
- `Monitor` and the Playwright / chrome-devtools MCP browser tools

## What still works

- `Read`, `Glob`, `Grep` — file and code inspection
- Writes to `.specify/**` (the audit's own artifacts)
- Trivial commands that hit an existing permission rule: `ls`, `date`, `git status`,
  `git diff`, `docker ps`, `echo`

## Impact on this audit

Every code change recorded in `findings.json` as `FIXED_PENDING_RUN` is **unverified**.
No test has been run, no build has been produced, no browser has been opened, and no database
query has been executed since the RED baseline was captured.

Specifically blocked:

- Phase 1.7 / 1.8 — backend and frontend suites
- Phase 1.3 / 1.10 / 1.11 — `deploy.sql`, schema verification, container rebuild
- Phase 1.4 / 1.5 — Redis measurement and the end-to-end streak scenarios
- Phase 2.7 / 2.11 / 2.12 — all browser measurement and screenshots
- Phase 3, 4, 5, R2 — everything that requires execution

## What was done instead

Because execution is impossible, the audit switched to the verification that read-only tools
still allow, and it found a defect that no test run would have surfaced before the cutover:

**F122 — the pre-cutover window is a full outage of every core learning endpoint.**
`recordStudy` runs inside the producer's own transaction and threw when `today < effectiveFrom`,
which rolled back the producer's result and surfaced as HTTP 500. With the cutover set to
2026-09-20 and today being 2026-09-19, deploying the schema would have made exercise submit,
SRS review, game check-in and speaking submission all return 500 until midnight. Fixed by
treating before-cutover as "nothing to record yet" while keeping a missing policy row a loud
failure. This also explains the already-known red test
`AuditV9DraftLessonGradeGuardTest.publishedLesson_submit_studentStill200`.

## Workaround prepared

`sweep/v10/run-test.bat <TestClass> [outfile]` runs a targeted suite into a log file, and
`sweep/v10/probe-live.js` covers the live probes, so the blocked runs execute in one call as
soon as the classifier recovers.

## Discipline note

No status in `findings.json` may be read as verified while B1 stands. Anything unverified is
reported as BLOCKED or PARTIAL with its reason, never omitted and never implied by a
neighbouring success.
