# Study activity implementation — in progress

## Contract

Learning, not login, counts. One completed valid activity per Vietnam calendar day
adds at most one day. Yesterday's streak remains available until midnight;
a whole missed day breaks it. Returning starts at one. Score is not a threshold.
Preserve legacy access dates without labeling them completed learning.
Mail stays at 20:00 Vietnam: at-risk reminder, then comeback with 30-day suppression;
no opt-out UI was requested. Use sandbox mail only during verification.

## Implemented slices

- Login/profile no longer write a study day. Empty game check-in is ignored.
- `StudyDay`, `StudyPolicy`, repositories and `StudyActivityService` provide an
  SQL-backed calendar snapshot, fixed cutover and Vietnam date semantics.
- `recordStudy` requires the caller's result transaction and locks the user before
  checking the unique user/date key. Unit mocks do not prove database concurrency.
- `/api/streak/snapshot` and Profile use one snapshot, explicit loading/error/retry,
  focus/visibility refresh and a Vietnam-midnight timer.
- Yellow uses the existing `tertiary` token, not the violet `accent` token.

## What counts as a study day

`StudyActivityService.recordStudy` is called from exactly four producers, each inside the
transaction that persists the user's result:

| Producer | Call site |
|---|---|
| Exercise submit | `ExerciseService.submitExercises` |
| SRS review | `SrsService.reviewWord` |
| Flashcard review | `FlashcardService.reviewFlashcard` |
| Game submit | `GameService.submitGameResult` → `StreakService.checkin` |
| Speaking assessment | `SpeakingSubmissionService.assessSubmission` |

### Shadowing (video) is deliberately NOT wired

`VideoLessonService.submitAttempt` only uploads audio. The attempt is graded later by an
**admin** — either manually (`PATCH /api/v1/admin/video-attempts/{id}/grade`) or via AI
(`POST /api/v1/admin/video-attempts/{id}/ai-grade`, Whisper + LLM). The student UI says
"Đã nộp — đang chờ giáo viên".

So there is no point in the student's own flow that can tell "completed" from "submitted":

- Counting at upload would violate the "no upload-only" rule below — submitting a junk
  recording would earn a study day.
- Counting at grading records the **wrong day** (admin grades tomorrow → tomorrow counts),
  and a user whose attempt is never graded never earns a day at all.

Wiring shadowing needs its own design first (e.g. auto-grade synchronously on submit) so
"completed" is decidable inside the student's transaction. Do not add a `recordStudy` call
to `submitAttempt` without that.

## Deployment gate — NOT deployed

Do not rebuild/start the main backend with these entity changes until reviewed
backup and deployment steps are complete. Hibernate update can create tables,
but deliberately does not create the policy row. Missing policy fails explicitly.

1. Stop application writes for cutover. Record an explicit Vietnam effective date.
2. Create a fresh full SQL backup with CHECKSUM outside the repository; snapshot
   Redis legacy access data and preserve a run-ID manifest. Backups contain PII.
3. Execute `deploy.sql` via sqlcmd against the explicitly selected database with
   `-b -v EffectiveFrom="YYYY-MM-DD" BackupFile="<server-local backup path>"`.
   The script validates recent backup metadata and runs RESTORE VERIFYONLY before
   DDL. Do not guess a backup path. It refuses changing an existing cutover.
4. Validate schema, policy and unique constraint in an isolated DB first.
5. Complete all remaining gates below before rebuilding/deploying the main backend.

The script has been authored, not executed. No Flyway migration is introduced.
Do not delete old user streak fields, Redis keys or `content_original`.

## Remaining required work

- Wire every real completion producer to `recordStudy` inside its persistence
  transaction: exercises, skill submissions, game, SRS, video quiz, processed
  shadowing and speaking. No upload-only or client-declared completion.
- Validate content visibility, nonempty answers, empty/failed audio and replay;
  persist/validate video quiz answers server-side. Keep grading semantics intact.
- Convert existing streak consumers/endpoints and mail eligibility to SQL; remove
  legacy day writes. Preserve legacy access history durably before Redis expiry.
- Configure and HTML-escape mail CTA/name; recheck eligibility before send, bound
  retries, preserve successful-send markers and 30-day suppression.
- Improve calendar legacy visual legend and distinguish pre-account/cutover days
  from genuinely missed days. Verify responsive layout with real screenshots.
- Isolated SQL transaction/rollback/concurrency tests; isolated Redis failures;
  browser desktop/mobile journeys across controlled dates; SMTP inbox evidence.
- Five independent UX evaluator results and mechanical aggregation; no score
  until all five are available. Multi-agent tool discovery failed in this session.
- Full safe regression, frontend build, evidence report and independently verified
  run-ID cleanup/parity. No aggregate completion claim from targeted unit tests.

## Current verification boundaries

`StudyActivityServiceTest`: six unit tests with repository mocks, no SQL proof.
Profile/calendar: seven component tests, no browser E2E proof.
The normal Maven test compile was blocked by the unrelated working-tree
`AuditV9SrsIntervalOverflowTest` annotation using `name` instead of `named`.
Do not alter another ongoing audit's files to work around that failure.
Targeted javac using the existing Surefire classpath plus `surefire:test` can
validate this slice without booting Spring or writing the main database.

An earlier broad test invocation booted the main application/database and logged
DatabaseSeeder dropping a check constraint. This was not an isolated verification
and has not had an independent parity check; do not report mutation cleanup PASS.
