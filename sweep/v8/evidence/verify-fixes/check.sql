SELECT email, current_streak, last_study_date FROM users WHERE email = 'user@gmail.com';
SELECT COUNT(*) AS published_lessons FROM lessons WHERE is_published = 1;
SELECT COUNT(*) AS total_lessons FROM lessons;