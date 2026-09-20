SET NOCOUNT ON;
SELECT 'fk' AS t, name AS v FROM sys.foreign_keys
WHERE referenced_object_id = OBJECT_ID('dbo.users');
