SET NOCOUNT ON;
PRINT N'=== 1. zzprobe users (claim: 4 pre-existing from 09-12) ===';
SELECT user_id, username, CAST(created_at AS varchar(19)) AS c
FROM users WHERE username LIKE 'zzprobe%' ORDER BY created_at;
PRINT N'=== 2. empty answers (claim: 4848, untouched) ===';
SELECT empty_answer = COUNT(*) FROM exercises
WHERE correct_answer IS NULL OR LTRIM(RTRIM(CAST(correct_answer AS nvarchar(max)))) = '';
SELECT total_exercises = COUNT(*) FROM exercises;
PRINT N'=== 3. index usage right now (seeks=0 claim) ===';
SELECT tbl = OBJECT_NAME(us.object_id), idx = i.name, user_seeks = us.user_seeks,
       user_scans = us.user_scans, user_lookups = us.user_lookups, writes = us.user_updates,
       last_user_seek = CAST(us.last_user_seek AS varchar(19))
FROM sys.dm_db_index_usage_stats us
JOIN sys.indexes i ON i.object_id = us.object_id AND i.index_id = us.index_id
WHERE us.database_id = DB_ID() AND i.name IS NOT NULL
  AND i.name IN ('idx_lessons_level','idx_prompts_level','idx_prompts_published','idx_decks_public',
                 'IX_uvp_due','idx_exercises_difficulty','idx_payment_transactions_user',
                 'IX_exercises_lesson_type_diff_order','idx_exercises_lesson_type_order')
ORDER BY user_seeks, i.name;
