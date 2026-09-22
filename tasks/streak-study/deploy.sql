SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
SET NOCOUNT ON;

DECLARE @EffectiveFrom date = TRY_CONVERT(date, '$(EffectiveFrom)', 23);
DECLARE @BackupFile nvarchar(4000) = N'$(BackupFile)';

IF @EffectiveFrom IS NULL
    THROW 51000, 'EffectiveFrom must be an explicit ISO date.', 1;

IF NOT EXISTS (
    SELECT 1 FROM msdb.dbo.backupset AS backups
    JOIN msdb.dbo.backupmediafamily AS media
      ON backups.media_set_id = media.media_set_id
    WHERE backups.database_name = DB_NAME()
      AND backups.type = 'D'
      AND backups.has_backup_checksums = 1
      AND backups.backup_finish_date >= DATEADD(hour, -24, GETDATE())
      AND media.physical_device_name = @BackupFile
)
    THROW 51001, 'A recent full checksum backup of this database is required.', 1;

RESTORE VERIFYONLY FROM DISK = @BackupFile WITH CHECKSUM;

BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.study_policy', N'U') IS NULL
    CREATE TABLE dbo.study_policy (
        id int NOT NULL PRIMARY KEY,
        effective_from date NOT NULL
    );

-- audit-v13 F-13-07: the singleton CHECK used to live INSIDE the table-existence guard
-- above, so on a real DB (Hibernate ddl-auto=update creates the table first) the guard
-- was skipped and the constraint was NEVER created — the same bug class as audit-v10
-- F124 for study_days. Measured live 2026-09-22: sys.check_constraints on study_policy
-- = 0. Give the constraint its OWN guard, exactly like the study_days constraints below.
-- Precondition mirrors fix-study-policy-check.sql: a bad row would otherwise abort the
-- ALTER with a raw constraint error instead of an actionable message.
IF EXISTS (SELECT 1 FROM dbo.study_policy WHERE id <> 1)
    THROW 51003, 'study_policy has a row with id <> 1; cannot add the singleton CHECK.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = N'ck_study_policy_singleton'
      AND parent_object_id = OBJECT_ID(N'dbo.study_policy')
)
    ALTER TABLE dbo.study_policy
        ADD CONSTRAINT ck_study_policy_singleton CHECK (id = 1);

IF EXISTS (SELECT 1 FROM dbo.study_policy WHERE id = 1 AND effective_from <> @EffectiveFrom)
    THROW 51002, 'The established study cutover cannot be changed by redeployment.', 1;

IF NOT EXISTS (SELECT 1 FROM dbo.study_policy WHERE id = 1)
    INSERT INTO dbo.study_policy (id, effective_from) VALUES (1, @EffectiveFrom);

-- audit-v10 F124: bang va rang buoc phai co GUARD RIENG.
--
-- Ban truoc gom ca ba vao mot khoi `IF OBJECT_ID(...) IS NULL`. Trong moi
-- truong that, Hibernate ddl-auto=update da tao bang study_days TRUOC khi
-- script nay chay, nen ca khoi bi bo qua va FK KHONG BAO GIO duoc tao. Do
-- duoc ngay sau khi chay ban cu: FK_COUNT = 0.
--
-- Te hon: unique index VAN co mat (Hibernate tao no cung bang), nen mot phep
-- kiem "bang da ton tai chua" khong he phat hien ra thieu FK. Hau qua la DB
-- khong chan duoc study_days tro toi user khong ton tai.
--
-- Dieu kien cua "tao bang" va cua "tao rang buoc" khac nhau: bang co the da
-- ton tai trong khi rang buoc van chua. Tach ra thi script moi thuc su
-- idempotent — chay tren DB trong (Hibernate chua chay) hay DB da co bang deu
-- dat cung mot schema.
IF OBJECT_ID(N'dbo.study_days', N'U') IS NULL
    CREATE TABLE dbo.study_days (
        id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id bigint NOT NULL,
        study_date date NOT NULL
    );

-- Rang buoc unique. Guard tren sys.indexes chu khong tren su ton tai cua bang:
-- Hibernate tao unique index cung luc voi bang nen no thuong da co, nhung mot
-- DB duoc tao bang tay thi chua.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_study_days_user_date'
      AND object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT uq_study_days_user_date UNIQUE (user_id, study_date);

-- Khoa ngoai. Day chinh la rang buoc ban cu bo sot.
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = N'fk_study_days_user'
      AND parent_object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT fk_study_days_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);

COMMIT TRANSACTION;

SELECT 'STUDY_POLICY_EFFECTIVE_FROM=' + CONVERT(varchar(10), effective_from, 23)
FROM dbo.study_policy WHERE id = 1;
