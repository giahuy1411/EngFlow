SET NOCOUNT ON;
DECLARE @ls bigint = (SELECT MIN(lesson_id) FROM lessons);
SET STATISTICS IO, TIME ON;
SELECT TOP 20 exercise_id, question FROM exercises
 WHERE lesson_id = @ls AND exercise_type = N'MULTIPLE_CHOICE' ORDER BY order_index;
SELECT TOP 30 exercise_id FROM exercises WHERE lesson_id = @ls ORDER BY order_index;
SET STATISTICS IO, TIME OFF;
SELECT idx = index_id, frag = CAST(avg_fragmentation_in_percent AS numeric(8,2)), pg = page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), OBJECT_ID(N'exercises'), NULL, NULL, N'LIMITED') WHERE page_count > 100;
