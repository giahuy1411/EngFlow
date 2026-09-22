-- audit-v13 F-13-08 — drop the two legacy streak columns.
--
-- WHY A HAND-RUN SQL FILE AND NOT THE ENTITY ALONE
-- ------------------------------------------------
-- Schema is owned by Hibernate ddl-auto=update, but `update` only ADDS what is
-- missing — it never drops a column that the entity no longer declares. Removing
-- the fields from User.java is therefore necessary but NOT sufficient: the columns
-- stay in the table (and in every SELECT *) unless this runs.
--
-- SAFETY
--   * Precondition: fail loudly if either column is referenced by a constraint,
--     index, or computed column — a DROP would then break something.
--   * Idempotent: re-running is a no-op.
--   * QUOTED_IDENTIFIER ON is required for this DB (filtered index IX_uvp_due),
--     and sqlcmd returns exit 0 even on failure, so the caller must scan for `Msg`.
--   * Backup taken before this file was run:
--     C:\Users\ASUS\engflow-backups\engflow_2026-09-22-predrop-deadcolumns.bak

SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

DECLARE @dep nvarchar(400) = N'';

SELECT @dep = @dep + N'constraint ' + c.name + N'; '
FROM sys.check_constraints c
JOIN sys.columns col
  ON col.object_id = c.parent_object_id AND col.column_id = c.parent_column_id
WHERE c.parent_object_id = OBJECT_ID('dbo.users')
  AND col.name IN ('last_study_date', 'current_streak');

SELECT @dep = @dep + N'index ' + i.name + N'; '
FROM sys.indexes i
JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
JOIN sys.columns col ON col.object_id = ic.object_id AND col.column_id = ic.column_id
WHERE i.object_id = OBJECT_ID('dbo.users')
  AND col.name IN ('last_study_date', 'current_streak');

SELECT @dep = @dep + N'computed ' + cc.name + N'; '
FROM sys.computed_columns cc
WHERE cc.object_id = OBJECT_ID('dbo.users')
  AND cc.name IN ('last_study_date', 'current_streak');

IF @dep <> N''
BEGIN
    THROW 51010, N'ABORT: a dependency still references the legacy streak columns', 1;
END

IF COL_LENGTH('dbo.users', 'last_study_date') IS NOT NULL
BEGIN
    ALTER TABLE dbo.users DROP COLUMN last_study_date;
    PRINT 'dropped users.last_study_date';
END
ELSE PRINT 'users.last_study_date already absent';

IF COL_LENGTH('dbo.users', 'current_streak') IS NOT NULL
BEGIN
    ALTER TABLE dbo.users DROP COLUMN current_streak;
    PRINT 'dropped users.current_streak';
END
ELSE PRINT 'users.current_streak already absent';

SELECT 'remaining_legacy_cols=' + CAST(COUNT(*) AS varchar(10)) AS result
FROM sys.columns
WHERE object_id = OBJECT_ID('dbo.users')
  AND name IN ('last_study_date', 'current_streak');
