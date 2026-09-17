SET NOCOUNT ON;
SELECT k, v FROM (VALUES
  ('exercises', (SELECT COUNT(*) FROM exercises)),
  ('lessons', (SELECT COUNT(*) FROM lessons)),
  ('users', (SELECT COUNT(*) FROM users)),
  ('attempts', (SELECT COUNT(*) FROM exercise_attempts)),
  ('speaking_subs', (SELECT COUNT(*) FROM speaking_submissions)),
  ('empty_ans', (SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(CAST(correct_answer AS nvarchar(max)))) = N''))
) t(k,v);
SELECT idx = i.index_id, name = i.name, type = i.type_desc,
       frag = CAST(ips.avg_fragmentation_in_percent AS numeric(8,2)), pg = ips.page_count
FROM sys.indexes i
JOIN sys.dm_db_index_physical_stats(DB_ID(), OBJECT_ID(N'exercises'), NULL, NULL, N'LIMITED') ips
  ON ips.object_id = i.object_id AND ips.index_id = i.index_id
WHERE i.object_id = OBJECT_ID(N'exercises') AND ips.page_count > 50
ORDER BY i.index_id;
