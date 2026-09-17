SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

PRINT '--- users 180214 (probe dang ky p4a) ---';
SELECT 'UVP' AS t, COUNT(*) AS n FROM user_vocabulary_progress WHERE user_id = 180214;
SELECT 'STREAK' AS t, COUNT(*) AS n FROM user_streaks WHERE user_id = 180214;
SELECT 'PAY' AS t, COUNT(*) AS n FROM payment_transactions WHERE user_id = 180214;

DELETE FROM user_vocabulary_progress WHERE user_id = 180214;
DELETE FROM user_streaks WHERE user_id = 180214;
DELETE FROM payment_transactions WHERE user_id = 180214;
DELETE FROM users WHERE user_id = 180214;
PRINT 'users deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);

PRINT '--- vocabulary probe 50224 ---';
DELETE FROM user_vocabulary_progress WHERE vocabulary_id = 50224;
DELETE FROM vocabulary WHERE vocab_id = 50224;
PRINT 'vocab deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);

PRINT '--- submissions / attempts ---';
DELETE FROM speaking_submissions WHERE id IN (40029, 40030, 40031);
PRINT 'speaking_submissions deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);
DELETE FROM video_attempts WHERE id = 17;
PRINT 'video_attempts deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);
DELETE FROM lesson_submissions WHERE submission_id = 9;
PRINT 'lesson_submissions deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);

PRINT '--- payment_transactions (probe trong phien 2026-09-16) ---';
DELETE FROM payment_transactions WHERE created_at >= '2026-09-16 00:00:00' AND order_code LIKE 'ENG%';
PRINT 'payments deleted: ' + CAST(@@ROWCOUNT AS VARCHAR);
