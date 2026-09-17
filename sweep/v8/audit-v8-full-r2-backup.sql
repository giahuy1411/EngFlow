-- audit-v8-full-r2-backup.sql — create a verified restore point for the audit's DML.
--
-- WHY: PLAN.md Phase 2 default + Phase 4.4 ("tạo backup trước DML") and the
-- constitution's "DML hàng loạt phải backup trước" gate. The audit ran DELETE
-- statements on 2026-09-16 (9 rows of audit-created data) while the newest
-- restore point on disk was from 2026-09-12 — a process gap. The policy says
-- back up first; "the rows were only audit rows" is a judgement made after the
-- fact, not a control.
--
-- Honest scope: this restore point is taken AFTER the round-2 DML. It covers any
-- FURTHER DML (and makes the drill re-runnable), it does not retroactively cover
-- the 9 already-deleted audit rows.
BACKUP DATABASE english_learning
TO DISK = '/var/opt/mssql/backup/engflow_2026-09-16-audit-v8-full.bak'
WITH COMPRESSION, CHECKSUM, INIT,
     NAME = 'engflow audit-v8-full 2026-09-16',
     DESCRIPTION = 'Restore point for audit-v8-full round-2 DML';
GO

RESTORE VERIFYONLY
FROM DISK = '/var/opt/mssql/backup/engflow_2026-09-16-audit-v8-full.bak'
WITH CHECKSUM;
GO

-- NOTE: `is_compressed` does not exist on this SQL Server 2019 build (it is a
-- 2022+ column) — the first revision of this file selected it and failed with
-- Msg 207 while sqlcmd still exited 0. Use the columns that do exist.
PRINT '--- backup records for this database (newest first) ---';
SELECT
    database_name,
    backup_start_date,
    backup_finish_date,
    CAST(backup_size / 1048576.0 AS DECIMAL(10,1))            AS size_mb,
    CAST(compressed_backup_size / 1048576.0 AS DECIMAL(10,1)) AS compressed_mb,
    is_damaged,
    has_backup_checksums,
    recovery_model
FROM msdb.dbo.backupset
WHERE database_name = 'english_learning'
ORDER BY backup_start_date DESC;
GO
