SET NOCOUNT ON;
BACKUP DATABASE english_learning
TO DISK = N'/var/opt/mssql/backup/engflow_2026-09-21-audit-v12-phase10.bak'
WITH COMPRESSION, CHECKSUM, INIT, STATS = 25;
GO
RESTORE VERIFYONLY
FROM DISK = N'/var/opt/mssql/backup/engflow_2026-09-21-audit-v12-phase10.bak'
WITH CHECKSUM;
GO
