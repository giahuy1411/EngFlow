SET NOCOUNT ON;
SELECT k, v FROM (VALUES
  ('lessons', (SELECT COUNT(*) FROM lessons)),
  ('exercises', (SELECT COUNT(*) FROM exercises)),
  ('users', (SELECT COUNT(*) FROM users)),
  ('vocabulary', (SELECT COUNT(*) FROM vocabulary)),
  ('empty_answer', (SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(CAST(correct_answer AS nvarchar(max)))) = '')),
  ('listening_no_audio', (SELECT COUNT(*) FROM exercises WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR audio_url = ''))),
  ('zz_leftovers', (SELECT COUNT(*) FROM lessons WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZ v8p4%' OR title LIKE 'ZZ v8 AI%' OR title LIKE 'ZZ probe%' OR title LIKE 'ZZ v8p3a%' OR title LIKE 'ZZ v8b%' OR title LIKE 'ZZ v8b2%')),
  ('orphan_ex', (SELECT COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id = e.lesson_id WHERE l.lesson_id IS NULL)),
  ('orphan_snap', (SELECT COUNT(*) FROM lesson_snapshots s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL))
) t(k, v);
SELECT reserved_mb = CAST(SUM(reserved_page_count) * 8 / 1024.0 AS numeric(10,1))
FROM sys.dm_db_partition_stats WHERE object_id = OBJECT_ID('lessons') OR object_id = OBJECT_ID('exercises');
