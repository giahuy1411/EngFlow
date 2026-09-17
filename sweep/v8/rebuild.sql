SET NOCOUNT ON;
ALTER INDEX idx_exercises_lesson_type_order ON exercises REBUILD;
SELECT idx = index_id, frag = CAST(avg_fragmentation_in_percent AS numeric(8,2)), pg = page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), OBJECT_ID(N'exercises'), NULL, NULL, N'LIMITED')
WHERE index_id IN (1, 14);
