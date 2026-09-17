SET NOCOUNT ON;
SELECT DB_NAME() AS db, physical_name = physical_name, size_on_disk_mb = CAST(size*8/128 AS numeric(10,1)), state_desc
FROM sys.master_files WHERE database_id = DB_ID();