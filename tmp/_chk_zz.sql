SET NOCOUNT ON;
SELECT 'zz_users' AS t, CAST(COUNT(*) AS varchar(20)) AS v FROM users WHERE email LIKE 'zzv3%';
SELECT 'emails' AS t, email AS v FROM users WHERE email LIKE 'zzv3%';
SELECT 'uvp' AS t, CAST(COUNT(*) AS varchar(20)) AS v FROM user_vocabulary_progress WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE 'zzv3%');
