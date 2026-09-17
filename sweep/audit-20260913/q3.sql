SET NOCOUNT ON;
PRINT '=== lessons partition page breakdown ===';
SELECT index_id, in_row_pages = SUM(CASE WHEN a.type=1 THEN a.used_pages ELSE 0 END),
       lob_pages = SUM(CASE WHEN a.type=2 THEN a.used_pages ELSE 0 END),
       rowoverflow_pages = SUM(CASE WHEN a.type=3 THEN a.used_pages ELSE 0 END)
FROM sys.partitions p JOIN sys.allocation_units a ON a.container_id = CASE WHEN a.type=2 THEN p.partition_id ELSE p.hobt_id END
WHERE p.object_id = OBJECT_ID('lessons') GROUP BY index_id;
PRINT '=== lessons cols ===';
SELECT c.name, t.name AS type, c.max_length FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id WHERE c.object_id=OBJECT_ID('lessons') ORDER BY c.column_id;