SET NOCOUNT ON;
SELECT t.name AS tbl, rows = SUM(p.rows), used_mb = CAST(SUM(a.total_pages)*8/1024 AS numeric(10,1)),
       ai_mb = CAST(SUM(a.data_pages)*8/1024 AS numeric(10,1)), idx_mb = CAST(SUM((a.used_pages-a.data_pages))*8/1024 AS numeric(10,1))
FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id
JOIN sys.allocation_units a ON a.container_id = CASE WHEN a.type=2 THEN p.partition_id ELSE p.hobt_id END
GROUP BY t.name ORDER BY used_mb DESC;
