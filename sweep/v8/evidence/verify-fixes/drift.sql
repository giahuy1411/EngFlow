UPDATE users SET last_study_date = '2026-09-14' WHERE email = 'user@gmail.com';
SELECT email, current_streak, last_study_date FROM users WHERE email = 'user@gmail.com';