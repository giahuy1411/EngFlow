SET NOCOUNT ON;
-- The canonical DB parity row. 9 columns, in this order:
--   lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks
-- audit-v15: lesson_snapshots was dropped with the Lesson Builder (Đường B) removal,
-- so the `snapshots` column is gone. See docs/lesson-builder-removal.md.
SELECT (SELECT COUNT(*) FROM lessons) AS lessons,
       (SELECT COUNT(*) FROM exercises) AS exercises,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM vocabulary) AS vocabulary,
       (SELECT COUNT(*) FROM speaking_submissions) AS speaking,
       (SELECT COUNT(*) FROM video_attempts) AS video,
       (SELECT COUNT(*) FROM lesson_submissions) AS lesson_sub,
       (SELECT COUNT(*) FROM payment_transactions) AS payments,
       (SELECT COUNT(*) FROM decks) AS decks;
GO
-- audit-v15 hardening (weakness L1): `study_days` is NOT in the 9-column row above
-- because it legitimately grows as learners study, so a fixed baseline would be wrong.
-- But it is a real residue channel — 5 services call StudyActivityService.recordStudy()
-- (Flashcard/Srs/Exercise/Speaking/Streak), several reachable from swept endpoints, and
-- `recordStudy` is idempotent per (user, day) so a stray row is easy to mistake for the
-- baseline (exactly what happened: v14 ended at 4, a probe row made it 5, and v15 recorded
-- "study_days 5" as if it were the baseline). Emitting it as its own marker makes it
-- VISIBLE to assertClean() and to a human reading the parity output.
SELECT 'STUDY_DAYS=' + CAST((SELECT COUNT(*) FROM study_days) AS varchar(10)) AS study_days_marker;
GO
-- Unpaid orders (a `/premium/checkout` visit mints one). SUCCESS rows are real money and
-- are never counted here. Used by assertClean() to prove a harness cleaned up after itself.
SELECT 'PENDING_PAYMENTS=' + CAST((SELECT COUNT(*) FROM payment_transactions
       WHERE transaction_id IS NULL AND status <> 'SUCCESS') AS varchar(10)) AS pending_payments_marker;
GO
-- audit-v17 F-17-07: `exercise_attempts` is a residue channel too. A submit through the real UI
-- (POST /api/lessons/{id}/exercises/submit -> ExerciseService.submitExercises) persists one, and
-- the MCP walkthrough does exactly that; nothing counted or cleaned it before.
-- audit-v17 round 2 (F-17-21, cross-review): counted for the TWO PROBE ACCOUNTS ONLY. The first
-- version counted the whole table, which would report DIRTY the moment a REAL learner submitted
-- (there is a third user with real rows) even though the harness left nothing behind. Scoping the
-- marker to the same two emails the cleanup uses makes the assertion exact.
SELECT 'EXERCISE_ATTEMPTS=' + CAST((SELECT COUNT(*) FROM exercise_attempts ea
       WHERE ea.user_id IN (SELECT user_id FROM users
                            WHERE email IN ('user@gmail.com','admin@gmail.com'))) AS varchar(10)) AS exercise_attempts_marker;
GO
