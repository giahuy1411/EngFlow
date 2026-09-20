SET QUOTED_IDENTIFIER ON;
DELETE FROM exercise_attempts WHERE lesson_id = 102049;
DELETE FROM exercises WHERE lesson_id = 102049;
DELETE FROM lessons WHERE lesson_id = 102049;
DELETE FROM vocabulary WHERE vocab_id = 50261;
GO
SET QUOTED_IDENTIFIER ON;
SELECT 'lessons' k, COUNT(*) v FROM lessons
UNION ALL SELECT 'exercises', COUNT(*) FROM exercises
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'vocabulary', COUNT(*) FROM vocabulary
UNION ALL SELECT 'speaking', COUNT(*) FROM speaking_submissions
UNION ALL SELECT 'video', COUNT(*) FROM video_attempts
UNION ALL SELECT 'lesson_sub', COUNT(*) FROM lesson_submissions
UNION ALL SELECT 'payments', COUNT(*) FROM payment_transactions
UNION ALL SELECT 'decks', COUNT(*) FROM decks
UNION ALL SELECT 'snapshots', COUNT(*) FROM lesson_snapshots;
GO