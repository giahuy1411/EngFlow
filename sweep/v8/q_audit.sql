SET NOCOUNT ON;
PRINT "== rows ==";
SELECT tbl = OBJECT_NAME(p.object_id), n = OBJECT_NAME(p.object_id), rows = MAX(p.rows)
FROM sys.partitions p JOIN sys.tables t ON t.object_id = p.object_id
WHERE p.index_id < 2 GROUP BY p.object_id HAVING MAX(p.rows) > 1000 ORDER BY rows DESC;
PRINT "== reserved mb (top 8) ==";
SELECT TOP 8 tbl = t.name, mb = CAST(SUM(a.total_pages)*8/1024.0 AS numeric(10,1)),
       in_row = CAST(SUM(CASE WHEN a.type=1 THEN a.used_pages ELSE 0 END)*8/1024.0 AS numeric(10,1)),
       lob = CAST(SUM(CASE WHEN a.type=2 THEN a.used_pages ELSE 0 END)*8/1024.0 AS numeric(10,1))
FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id
JOIN sys.allocation_units a ON a.container_id = CASE WHEN a.type IN (2,3) THEN p.partition_id ELSE p.hobt_id END
GROUP BY t.name ORDER BY mb DESC;
PRINT "== clustered frag ==";
SELECT tbl = OBJECT_NAME(ips.object_id), idx = i.name, frag = CAST(ips.avg_fragmentation_in_percent AS numeric(6,2)),
       pages = ips.page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, LIMITED) ips
JOIN sys.indexes i ON i.object_id=ips.object_id AND i.index_id=ips.index_id
WHERE ips.index_id IN (0,1) AND ips.page_count > 400 ORDER BY frag DESC;
PRINT "== unused indexes ==";
SELECT TOP 12 tbl = OBJECT_NAME(s.object_id), idx = i.name, reads = ISNULL(s.user_seeks,0)+ISNULL(s.user_scans,0),
       writes = ISNULL(s.user_updates,0), last_user_seek = CONVERT(varchar(10), s.last_user_seek, 23)
FROM sys.dm_db_index_usage_stats s JOIN sys.indexes i ON i.object_id=s.object_id AND i.index_id=s.index_id
WHERE s.database_id = DB_ID() AND i.name IS NOT NULL AND i.is_unique = 0
  AND ISNULL(s.user_seeks,0)+ISNULL(s.user_scans,0) < 50
ORDER BY writes DESC;
PRINT "== backups ==";
SELECT backups = COUNT(*), last_bak = MAX(backup_finish_date) FROM msdb.dbo.backupset WHERE database_name = DB_NAME();
PRINT "== content_original ==";
SELECT total = COUNT(*), with_orig = SUM(CASE WHEN LEN(ISNULL(CAST(content_original AS nvarchar(max)),0)) THEN 1 ELSE 0 END),
       orig_mb = CAST(SUM(LEN(ISNULL(CAST(content_original AS nvarchar(max)),0)))/1024.0/1024.0/2 AS numeric(10,1)),
       cur_mb  = CAST(SUM(LEN(ISNULL(CAST(content AS nvarchar(max)),0)))/1024.0/1024.0/2 AS numeric(10,1))
FROM lessons;
