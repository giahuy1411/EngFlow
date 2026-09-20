# EngFlow Audit Baseline Metrics (2026-09-19)

## Infrastructure

- Backend Port: 8080 (engflow-backend) OK
- Frontend Port: 5173 (engflow-frontend) OK
- SQL Server: 1433 (engflow-sqlserver, healthy) OK
- Redis: 6379 (engflow-redis) OK
- MinIO: 9000-9001 (engflow-minio) OK
- Whisper: 9002 (engflow-whisper) OK
- TTS Sidecar: 8001 (engflow-tts) OK

## Database

- Name: english_learning
- Backup: /tmp/engflow-backup-2026-09-19.bak (36 MB, 25,066 pages)
- Compress: CHECKSUM, FORMAT
- Duration: 0.404 seconds
- Verify Status: VALID (RESTORE VERIFYONLY passed)

## Design System Status

- Framework: Playful Geometric
- Primary Font: Be Vietnam Pro (400/700/900)
- Design Tokens: 11 semantic properties defined
- Border Radius: 8px/16px/24px/9999px
- Hard Shadows: 4px offset, no blur
- Accessibility: 0 violations (WCAG 2.2 AA)

## Backend Test Results (Run 1)

- Total Tests: 432
- Passed: 425
- Failed: 5
- Errors: 2 (NPEs)
- Skipped: 10

### Failure Breakdown

- ExerciseServiceGradingTest.submitSavesAttemptWhenGradeable: NPE studyActivityService is null
- LeaderboardServicePaginationTest.assignsGlobalRanksAcrossPages: NPE streakService is null
- StreakReminderSchedulerTest.redisFailureMustNotSendWithoutDeduplication: mock stub mismatch
- StreakReminderSchedulerTest.retryBudgetStopsAfterThreeAttempts: stub mismatch for increment
- AuditV9DraftLessonGradeGuardTest.publishedLesson_submit_studentStill200: status 500 vs expected 200

## Acceptance Criteria

- [x] Database backup valid
- [x] Infrastructure operational
- [x] Design system verified
- [x] Accessibility baseline 0 violations
- [ ] Backend tests pass (Currently 425/432)
- [ ] Frontend tests pass
- [ ] A11y sweep confirms 0 violations
