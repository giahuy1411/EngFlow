-- audit-v10 F124 — chung minh deploy.sql idempotent tren DB SCRATCH.
--
-- KHONG chay tren english_learning. Tao mot DB tam, tao bang study_days TRUOC
-- (gia lap Hibernate da chay), roi ap dung phan DDL cua deploy.sql va kiem
-- xem FK co duoc tao khong. Sau do chay LAN HAI de chung minh khong loi.
--
-- Chay bang: sqlcmd -i (khong qua shell string).

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

-- ============================================================
-- 1. Tao DB scratch + bang gia lap
-- ============================================================
IF DB_ID(N'engflow_f124_scratch') IS NOT NULL
BEGIN
    ALTER DATABASE engflow_f124_scratch SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE engflow_f124_scratch;
END
CREATE DATABASE engflow_f124_scratch;
GO

USE engflow_f124_scratch;
GO
SET NOCOUNT ON;

-- Gia lap: Hibernate da tao users + study_days + unique index, NHUNG CHUA co FK.
CREATE TABLE dbo.users (
    user_id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY
);
CREATE TABLE dbo.study_days (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id bigint NOT NULL,
    study_date date NOT NULL,
    CONSTRAINT uq_study_days_user_date UNIQUE (user_id, study_date)
);

PRINT '=== TRUOC: FK count (ky vong 0) ===';
SELECT COUNT(*) AS fk_before FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID(N'dbo.study_days');

-- ============================================================
-- 2. Ap dung DDL MOI cua deploy.sql (phan study_days)
-- ============================================================
IF OBJECT_ID(N'dbo.study_days', N'U') IS NULL
    CREATE TABLE dbo.study_days (
        id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id bigint NOT NULL,
        study_date date NOT NULL
    );

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_study_days_user_date'
      AND object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT uq_study_days_user_date UNIQUE (user_id, study_date);

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = N'fk_study_days_user'
      AND parent_object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT fk_study_days_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);

PRINT '=== SAU LAN 1: FK count (ky vong 1) ===';
SELECT COUNT(*) AS fk_after_run1 FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID(N'dbo.study_days');

PRINT '=== SAU LAN 1: ten FK ===';
SELECT name FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID(N'dbo.study_days');

-- ============================================================
-- 3. Chay LAN HAI — phai khong loi, khong tao trung
-- ============================================================
IF OBJECT_ID(N'dbo.study_days', N'U') IS NULL
    CREATE TABLE dbo.study_days (
        id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id bigint NOT NULL,
        study_date date NOT NULL
    );

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_study_days_user_date'
      AND object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT uq_study_days_user_date UNIQUE (user_id, study_date);

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = N'fk_study_days_user'
      AND parent_object_id = OBJECT_ID(N'dbo.study_days')
)
    ALTER TABLE dbo.study_days
        ADD CONSTRAINT fk_study_days_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);

PRINT '=== SAU LAN 2: FK count (ky vong VAN 1 — khong nhan doi) ===';
SELECT COUNT(*) AS fk_after_run2 FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID(N'dbo.study_days');

-- `is_unique = 1` bao gom CA khoa chinh clustered (PK__study_da__...) lan unique
-- index nghiep vu. Vay ky vong dung la 2, khong phai 1:
--   1) PK clustered tren cot id
--   2) uq_study_days_user_date NONCLUSTERED tren (user_id, study_date)
-- Lan chay dau tien toi da ghi "ky vong 1" va doc ket qua 2 nhu mot bat thuong;
-- do la comment sai, khong phai schema sai. Liet ke TEN de thay ro.
PRINT '=== SAU LAN 2: cac unique index (ky vong 2: PK + uq nghiep vu) ===';
SELECT name, type_desc FROM sys.indexes
WHERE object_id = OBJECT_ID(N'dbo.study_days') AND is_unique = 1 ORDER BY type_desc;

-- ============================================================
-- 4. Chung minh FK THUC SU chan duoc du lieu sai
-- ============================================================
PRINT '=== Thu chen study_days tro toi user khong ton tai (PHAI bi chan) ===';
BEGIN TRY
    INSERT INTO dbo.study_days (user_id, study_date) VALUES (999999, '2026-09-20');
    PRINT '>>> THAT BAI: FK KHONG chan — day la loi';
END TRY
BEGIN CATCH
    PRINT '>>> THANH CONG: FK chan duoc. Loi: ' + ERROR_MESSAGE();
END CATCH

PRINT '=== Thu chen user THAT (phai THANH CONG) ===';
INSERT INTO dbo.users DEFAULT VALUES;
DECLARE @uid bigint = SCOPE_IDENTITY();
INSERT INTO dbo.study_days (user_id, study_date) VALUES (@uid, '2026-09-20');
PRINT '>>> THANH CONG: chen duoc voi user that, user_id=' + CAST(@uid AS varchar(20));

PRINT '=== Thu chen TRUNG (user_id, study_date) — PHAI bi chan ===';
BEGIN TRY
    INSERT INTO dbo.study_days (user_id, study_date) VALUES (@uid, '2026-09-20');
    PRINT '>>> THAT BAI: unique index KHONG chan — day la loi';
END TRY
BEGIN CATCH
    PRINT '>>> THANH CONG: unique index chan duoc. Loi: ' + ERROR_MESSAGE();
END CATCH

-- ============================================================
-- 5. Don sach
-- ============================================================
USE master;
GO
SET NOCOUNT ON;
ALTER DATABASE engflow_f124_scratch SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
DROP DATABASE engflow_f124_scratch;
PRINT '=== DA DON DB SCRATCH ===';
GO
