-- audit-v13 F-13-07 — add the missing singleton CHECK to the live study_policy table.
--
-- WHY THIS FILE EXISTS: Hibernate's ddl-auto=update does NOT add a check constraint to
-- an ALREADY EXISTING table (official docs: update "only export what's missing in the
-- schema"; a Hibernate maintainer states hbm2ddl UPDATE is not a migration tool). The
-- @CheckConstraint on the StudyPolicy entity therefore only helps a FRESH schema. The
-- live database must be patched explicitly, once.
--
-- SAFE TO RUN REPEATEDLY: guarded by sys.check_constraints.
-- ROLLBACK: ALTER TABLE dbo.study_policy DROP CONSTRAINT ck_study_policy_singleton;
--
-- PRECONDITION (checked 2026-09-22: 0 rows): the ALTER fails if any row has id <> 1.
--   SELECT COUNT(*) FROM dbo.study_policy WHERE id <> 1;   -- must be 0

SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

IF EXISTS (SELECT 1 FROM dbo.study_policy WHERE id <> 1)
    THROW 51003, 'study_policy has a row with id <> 1; cannot add the singleton CHECK.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = N'ck_study_policy_singleton'
      AND parent_object_id = OBJECT_ID(N'dbo.study_policy')
)
    ALTER TABLE dbo.study_policy
        ADD CONSTRAINT ck_study_policy_singleton CHECK (id = 1);

SELECT 'study_policy_checks=' + CAST(COUNT(*) AS varchar(10)) AS marker
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.study_policy');
