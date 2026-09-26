SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
DELETE FROM study_days WHERE study_date = '2026-09-26' AND user_id IN (SELECT user_id FROM users WHERE email IN ('user@gmail.com','admin@gmail.com'));
SELECT 'SD_TODAY=' + CAST(COUNT(*) AS varchar(10)) AS marker FROM study_days WHERE study_date='2026-09-26';
