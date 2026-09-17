SET NOCOUNT ON;
SELECT k, v FROM (VALUES
  ('lessons', (SELECT COUNT(*) FROM lessons)),
  ('exercises', (SELECT COUNT(*) FROM exercises)),
  ('users', (SELECT COUNT(*) FROM users)),
  ('vocabulary', (SELECT COUNT(*) FROM vocabulary)),
  ('speaking_submissions', (SELECT COUNT(*) FROM speaking_submissions)),
  ('video_attempts', (SELECT COUNT(*) FROM video_attempts)),
  ('lesson_submissions', (SELECT COUNT(*) FROM lesson_submissions)),
  ('payment_transactions', (SELECT COUNT(*) FROM payment_transactions)),
  ('decks', (SELECT COUNT(*) FROM decks)),
  ('lesson_snapshots', (SELECT COUNT(*) FROM lesson_snapshots)),
  ('exercise_attempts', (SELECT COUNT(*) FROM exercise_attempts))) x(k,v);
SELECT orphan_v = (SELECT COUNT(*) FROM vocabulary WHERE word LIKE 'zz%'), orphan_u = (SELECT COUNT(*) FROM users WHERE username LIKE 'zz%'), orphan_l = (SELECT COUNT(*) FROM lessons WHERE title LIKE 'ZZ%'), orphan_p = (SELECT COUNT(*) FROM speaking_prompts WHERE title LIKE 'ZZ%'), orphan_d = (SELECT COUNT(*) FROM decks WHERE name LIKE 'ZZ%'), orphan_e = (SELECT COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id=e.lesson_id WHERE l.lesson_id IS NULL);