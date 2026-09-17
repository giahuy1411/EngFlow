SET NOCOUNT ON;
SELECT submission_id, user_id, lesson_id, skill_type, CAST(created_at AS varchar(19)) AS created FROM lesson_submissions ORDER BY submission_id;
SELECT avatar_user2 = avatar_url FROM users WHERE user_id=2;
SELECT avatar_admin3 = avatar_url FROM users WHERE user_id=3;