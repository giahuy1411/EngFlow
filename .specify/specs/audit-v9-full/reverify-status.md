# Re-verification status — 2026-09-18

Audit remains OPEN. This ledger supersedes broad closure statements in REPORT-v5.

## Verified evidence

- Original AuditV9SrsIntervalOverflowTest: 3/3 PASS on english_learning_audit_v9_srs, using real Hibernate repositories, flush and test rollback. Assertions unchanged; only Spring configuration isolated. No DatabaseSeeder/ExerciseFixRunner/SePay scheduler startup. Temporary DB/login both independently confirmed absent after cleanup. Logs: sweep/v8/v9-isolated-jpa-f106.log and v9-isolated-jpa-cleanup.log.
- Empty-DB Hibernate create-drop emitted expected missing-table drop warnings; no test failure. Real application column independently confirmed datetime2(7).
- Vite soak completed, 60 cycles / 180 HTTP requests over 30 minutes: sweep/v8/v9-vite-soak-final-30m.log, exit 0. This does not reproduce multi-hour browser/HMR workload; root cause remains unresolved.

- F106 SQL Server round-trip PASS: sweep/v8/v9-srs-sql-roundtrip.log; one executed test, zero skipped. Actual SrsService output persisted/read through JDBC datetime2(7); old uncapped formula rejected. Only a connection-local temporary table was written and dropped, verified by OBJECT_ID-null. Not full JPA persistence proof.

- Final targeted package run: 20 tests, zero failures/errors/skips, BUILD SUCCESS; sweep/v8/v9-final-package-verification.log. This is not the full backend suite.
- Security slice uses actual SecurityConfig/JwtAuthenticationFilter with mocked token provider, user lookup and services: anonymous 401, student draft 404, admin preview 200 for grade and submit. No JPA/seeder startup markers. JWT cryptography and real DB visibility are not covered by this slice.
- Four relevant compiled classes match running container bytes exactly: sweep/v8/v9-class-identity.json. This is stronger than string markers, still not DB integration proof.
- Frontend: 90/90 in 19 files, build exit 0; sweep/v8/v9-final-frontend-tests.log and v9-final-frontend-build.log.
- Coverage checker now requires manifest run/image identifiers, SHA256-bound files and passing named assertions; nine regression tests pass in sweep/v8/v9-coverage-regression.log. Expected errors are negative controls, not functional coverage.
- Fresh benign guest probe against the current image covers 1/144 mappings. sweep/v8/reverify-current-run/coverage.json returns gate exit 1 as expected. No replay of historical files as new proof.

- F105 guard mutation: disabling the controller guard produced five assertion failures; restored guard passed. This is local mock evidence, not HTTP authorization proof.
- F106 large interval: mock persisted interval is 365. Negative-interval fixture corrected to repetitions=15 so it exercises the clamp rather than first-review initialization.
- Targeted seven-test suite passed; logs: sweep/v8/v9-reverify-targeted-test.log and repeat logs. Repetition does not expand coverage.
- Backend image was rebuilt and started. Both class markers were present; public exercise list returned HTTP 200. No draft-answer probing against real data.
- Backup CHECKSUM / VERIFYONLY succeeded before fixture cleanup. Host copy now exists at C:/Users/ASUS/engflow-backups/engflow_2026-09-18-audit-v9-reverify.bak. SHA256: 23ABB04F4E508CA7584B46F35ED12E0239BCE3EB00AD01FC3C9D1464630C4745.
- Cleanup removed one exact fixture user and its progress row. Post-counts: lessons 1471, exercises 43737, users 76, vocabulary 127, speaking 28, video attempts 15, lesson submissions 4, payments 126. Counts do not prove row-level integrity.

## Execution deviations requiring explicit accounting

- Full backend suite was mistakenly started against the dev DB, then interrupted. No full-suite PASS may be claimed. Its startup executed DatabaseSeeder, including a CHECK-constraint operation. Subsequent row counts matched, but schema/row equality has not been proved.
- A raw environment read exposed credential material in tool output. Do not copy that output into artifacts. Rotation/revocation of exposed service credentials requires owner action; no automatic rotation performed.
- F105 was temporarily disabled in the source for mutation checking and restored. No disabled-guard build was deployed during this re-verification. Do not repeat mutation in the active source tree.
- The earlier patch backup was overwritten with status text; it is not a valid diff backup. Original evidence must not rely on that file.

## Remaining gates / accepted boundaries

Two actual independent CLI review processes completed read-only (exit 0).
Reports: target/v9-independent-review.txt (code/tests) and target/v9-evidence-review.txt (audit claims).
They identify coverage and accounting gaps; their completion does not mean the audit passed.
Admin submit guard-before-service test passed separately in sweep/v8/v9-admin-submit-targeted.log.

- [x] Old-formula negative control versus actual capped service output executed against a temporary SQL Server table. No active-source mutation; not a historical-binary comparison.
- [x] Actual SecurityConfig HTTP slice supplemented mock tests; original F106 JPA regression executed on isolated SQL Server DB (3/3). Token cryptography and deployed full-flow replay remain outside this proof.
- [ ] DTO-wide p25 contract requirement resolved; focused F105/F106 tests are not a substitute.
- [x] Two actual read-only CLI reviews completed; findings retained. No simulated reviewer claim.
- [ ] Runtime inventory/coverage bound to one run and artifact, or owner accepts the source-only boundary as a deliberate PARTIAL tooling decision.
- [ ] SpecKit documents corrected from retrospectively checked boxes to requirement/evidence traceability.
- [x] Vite 30-minute soak completed: 60 cycles / 180 requests, zero failures, exit 0. F114 remains root-cause unresolved (NOT REPRODUCED).
- [ ] Final report/task/checklist consistency and source/deployed artifact verification.

## Scope decisions retained

Independent post-cleanup read: sweep/v8/v9-independent-cleanup-check.log. Fixture residue=0, orphan progress owners=0, expected counts unchanged; no users CHECK constraints currently present. No claim of equality against the pre-suite schema snapshot.

Source-only inventory is the planned tooling fallback. This does not accept or prove current-run endpoint coverage; that gate remains open.

No index drop or new Redis cache without a measured benefit. Preserve empty-answer/content and legacy data pending approved ID manifest. No commit, push, branch creation, or broad cleanup.
