SET NOCOUNT ON;

PRINT '=== T2.2 ORPHAN CHECK (mọi FK, single-column) ===';
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql = @sql +
  N'SELECT ''' + fk.name + N''' AS fk, ''' + OBJECT_NAME(fk.parent_object_id) + N''' AS tbl, COUNT(*) AS orphans FROM '
  + QUOTENAME(OBJECT_SCHEMA_NAME(fk.parent_object_id)) + N'.' + QUOTENAME(OBJECT_NAME(fk.parent_object_id)) + N' c WHERE c.'
  + QUOTENAME(pc.name) + N' IS NOT NULL AND NOT EXISTS (SELECT 1 FROM '
  + QUOTENAME(OBJECT_SCHEMA_NAME(fk.referenced_object_id)) + N'.' + QUOTENAME(OBJECT_NAME(fk.referenced_object_id)) + N' p WHERE p.'
  + QUOTENAME(rc.name) + N' = c.' + QUOTENAME(pc.name) + N');' + CHAR(10)
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
WHERE (SELECT COUNT(*) FROM sys.foreign_key_columns x WHERE x.constraint_object_id = fk.object_id) = 1;
EXEC sp_executesql @sql;

PRINT '=== T2.3 FK COLUMNS WITHOUT LEADING INDEX ===';
SELECT fk.name AS fk_name, OBJECT_NAME(fk.parent_object_id) AS child_table,
       COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS fk_column
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
WHERE NOT EXISTS (
  SELECT 1 FROM sys.index_columns ic
  JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id
  WHERE ic.object_id = fkc.parent_object_id AND ic.column_id = fkc.parent_column_id
    AND ic.is_included_column = 0 AND i.index_id > 0 AND ic.key_ordinal = 1
)
ORDER BY child_table, fk_column;

PRINT '=== T2.3b FK COUNT + TABLE COUNT ===';
SELECT (SELECT COUNT(*) FROM sys.foreign_keys) AS fk_count,
       (SELECT COUNT(*) FROM sys.tables) AS table_count;
