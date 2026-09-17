SET NOCOUNT ON;
SELECT [db] = DB_NAME(), [files] = COUNT(*), [size_mb] = CAST(SUM(size)*8/128 AS numeric(10,1))
FROM sys.master_files WHERE database_id = DB_ID();
SELECT name, [state] = state_desc FROM sys.databases ORDER BY name;
