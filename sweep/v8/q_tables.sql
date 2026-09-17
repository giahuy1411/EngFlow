SET NOCOUNT ON;
SELECT tbl = OBJECT_NAME(p.object_id),
       rows = SUM(p.rows),
       used_mb = CAST(SUM(a.total_pages) * 8 / 1024.0 AS numeric(10,1)),
       lob_mb = CAST(SUM(CASE WHEN a.type = 2 THEN a.used_pages ELSE 0 END) * 8 / 1024.0 AS numeric(10,1))
FROM sys.partitions p
JOIN sys.allocation_units a ON a.container_id = CASE WHEN a.type = 2 THEN p.partition_id ELSE p.hobt_id END
JOIN sys.tables t ON t.object_id = p.object_id
WHERE p.index_id < 2
GROUP BY p.object_id
HAVING SUM(a.total_pages) > 100
ORDER BY used_mb DESC;
