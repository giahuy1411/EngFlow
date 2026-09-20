-- Tim CHINH XAC moi bang/cot dang tro toi dbo.users.
-- Khong doan ten cot: doc tu sys.foreign_key_columns.
SET NOCOUNT ON;

PRINT '=== Moi FK tro toi dbo.users: bang + cot ===';
SELECT
    OBJECT_NAME(fkc.parent_object_id) AS child_table,
    COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS child_column,
    COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS ref_column
FROM sys.foreign_key_columns fkc
WHERE fkc.referenced_object_id = OBJECT_ID(N'dbo.users')
ORDER BY child_table, child_column;

PRINT '=== Moi bang co cot ten chua user/id (khong can FK) ===';
SELECT t.name AS bang, c.name AS cot
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
WHERE c.name LIKE '%user%' OR c.name LIKE '%student%' OR c.name LIKE '%owner%'
ORDER BY t.name, c.name;
