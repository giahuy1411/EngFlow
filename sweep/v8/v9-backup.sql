-- audit-v9: restore point BEFORE any audit-v9 DML/schema work (constitution gate).
BACKUP DATABASE english_learning
TO DISK = '/var/opt/mssql/backup/engflow_2026-09-17-audit-v9.bak'
WITH COMPRESSION, CHECKSUM, INIT,
     NAME = 'engflow audit-v9 2026-09-17',
     DESCRIPTION = 'Restore point before audit-v9-full DML + index work';
GO

RESTORE VERIFYONLY
FROM DISK = '/var/opt/mssql/backup/engflow_2026-09-17-audit-v9.bak'
WITH CHECKSUM;
GO

SELECT TOP 3 database_name, backup_start_date,
    CAST(compressed_backup_size / 1048576.0 AS DECIMAL(10,1)) AS compressed_mb,
    is_damaged, has_backup_checksums
FROM msdb.dbo.backupset
WHERE database_name = 'english_learning'
ORDER BY backup_start_date DESC;
GO