SET NOCOUNT ON;
SELECT f.db = DB_NAME(f.database_id), f.file = RIGHT(f.physical_name, 22), mb = CAST(f.size*8/128 AS numeric(10,1)), f.state_desc
FROM sys.master_files f WHERE f.database_id IN (DB_ID(), DB_ID('english_learning_drill'))
ORDER BY mb DESC;
