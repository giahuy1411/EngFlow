SET NOCOUNT ON;
SELECT 'tbl' AS t, OBJECT_NAME(parent_object_id) AS v
FROM sys.foreign_keys
WHERE referenced_object_id = OBJECT_ID('dbo.users');
