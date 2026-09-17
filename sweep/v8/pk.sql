SET NOCOUNT ON;
DECLARE @n nvarchar(260), @sql nvarchar(500);
SELECT @n = i.name FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'exercises') AND i.type = 1;
SET @sql = N'ALTER INDEX ' + QUOTENAME(@n) + N' ON dbo.exercises REBUILD WITH (ONLINE = OFF);';
PRINT 'target=' + @n;
EXEC sp_executesql @sql;
SELECT id = i.index_id, name = i.name, frag = CAST(ips.avg_fragmentation_in_percent AS numeric(8,2)), pg = ips.page_count
FROM sys.indexes i CROSS APPLY sys.dm_db_index_physical_stats(DB_ID(), i.object_id, i.index_id, NULL, N'LIMITED') ips
WHERE i.object_id = OBJECT_ID(N'exercises') AND i.index_id IN (1, 14, 17);
SET STATISTICS IO ON;
SELECT n = COUNT(*) FROM exercises;
SET STATISTICS IO OFF;
