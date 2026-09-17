SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT u = user_id, attempts = (SELECT COUNT(*) FROM exercise_attempts a WHERE a.user_id=u.user_id), subs = (SELECT COUNT(*) FROM lesson_submissions ls WHERE ls.user_id=u.user_id), speaking = (SELECT COUNT(*) FROM speaking_submissions ss WHERE ss.user_id=u.user_id), srs = (SELECT COUNT(*) FROM user_vocabulary_progress vp WHERE vp.user_id=u.user_id), pay = (SELECT COUNT(*) FROM payment_transactions pt WHERE pt.user_id=u.user_id), streak = (SELECT COUNT(*) FROM user_streaks sk WHERE sk.user_id=u.user_id), prog = (SELECT COUNT(*) FROM user_progress pr WHERE pr.user_id=u.user_id) FROM users u WHERE username LIKE 'zzprobe%';
SELECT sqlserver_start_time FROM sys.dm_os_sys_info;
