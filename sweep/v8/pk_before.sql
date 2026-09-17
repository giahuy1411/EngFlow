SET NOCOUNT ON;
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS n FROM exercises;
SELECT COUNT_BIG(*) AS n2 FROM exercises WHERE difficulty IS NOT NULL;
SET STATISTICS IO, TIME OFF;
SELECT id = 1, frag = CAST(avg_fragmentation_in_percent AS numeric(8,2)), pg = page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), OBJECT_ID(N'exercises'), 1, NULL, N'LIMITED');
