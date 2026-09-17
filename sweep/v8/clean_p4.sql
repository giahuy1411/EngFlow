SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
DELETE FROM speaking_submissions WHERE id = 40025;
DELETE FROM user_vocabulary_progress WHERE user_id = 180189;
DELETE FROM user_streaks WHERE user_id = 180189;
DELETE FROM users WHERE user_id = 180189;
SELECT subs = (SELECT COUNT(*) FROM speaking_submissions), users = (SELECT COUNT(*) FROM users), vids = (SELECT COUNT(*) FROM video_lessons), zzp4_users = (SELECT COUNT(*) FROM users WHERE username LIKE 'zzp4%'), zz_vids = (SELECT COUNT(*) FROM video_lessons WHERE title LIKE 'ZZ v8%');
