SET NOCOUNT ON;
SELECT tbl = OBJECT_NAME(ips.object_id), idx = ISNULL(i.name,'(heap)'), ips.index_id,
       rows = ips.record_count, frag_pct = CAST(ips.avg_fragmentation_in_percent AS numeric(8,2)),
       pages = ips.page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips
JOIN sys.indexes i ON i.object_id = ips.object_id AND i.index_id = ips.index_id
WHERE ips.page_count > 500 AND ips.index_id IN (0,1)
ORDER BY frag_pct DESC;
