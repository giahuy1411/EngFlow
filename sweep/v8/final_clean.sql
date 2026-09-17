SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
DELETE FROM vocabulary WHERE word LIKE 'zzprobe%';
DELETE FROM user_vocabulary_progress WHERE user_id = 170225;
DELETE FROM user_streaks WHERE user_id = 170225;
DELETE FROM users WHERE user_id = 170225;
SELECT vocab = (SELECT COUNT(*) FROM vocabulary), users = (SELECT COUNT(*) FROM users), zz = (SELECT COUNT(*) FROM vocabulary WHERE word LIKE 'zz%'), today_users = (SELECT COUNT(*) FROM users WHERE created_at >= '2026-09-14');
