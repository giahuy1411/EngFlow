-- audit-v12-full T1.1 — CONSTRAINT EFFECT: prove the FK and the UNIQUE index actually REJECT violations.
-- Runs in a scratch DB so the live database is never touched. Dropped at the end.
-- Run: python sweep/v8/sqlrun.py sweep/v12/constraint-effect.sql
SET NOCOUNT ON;

PRINT '=== T1.1 CONSTRAINT EFFECT (scratch DB) ===';
IF DB_ID('english_learning_v12probe') IS NOT NULL DROP DATABASE english_learning_v12probe;
CREATE DATABASE english_learning_v12probe;
GO
USE english_learning_v12probe;
GO
-- AGENTS.md: a FILTERED index requires QUOTED_IDENTIFIER ON at creation AND at every DML
-- against the table. This is the same rule that makes `SET QUOTED_IDENTIFIER ON`
-- mandatory on every DELETE batch in this project.
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

CREATE TABLE parent_t (id INT PRIMARY KEY);
CREATE TABLE child_t (
    id INT PRIMARY KEY,
    parent_id INT NULL,
    email NVARCHAR(255) NULL
);
ALTER TABLE child_t ADD CONSTRAINT fk_child_parent FOREIGN KEY (parent_id) REFERENCES parent_t(id);
CREATE UNIQUE INDEX uq_child_email ON child_t(email) WHERE email IS NOT NULL;

INSERT INTO parent_t (id) VALUES (1);
PRINT '--- baseline: valid insert should SUCCEED ---';
INSERT INTO child_t (id, parent_id, email) VALUES (10, 1, 'a@x.com');
SELECT 'valid row inserted' AS probe, COUNT(*) AS rows_now FROM child_t;

PRINT '--- EFFECT 1: FK violation (parent_id = 999) must FAIL ---';
BEGIN TRY
    INSERT INTO child_t (id, parent_id, email) VALUES (11, 999, 'b@x.com');
    PRINT 'RESULT: FK DID NOT BLOCK  <-- CONSTRAINT INEFFECTIVE';
END TRY
BEGIN CATCH
    PRINT 'RESULT: FK BLOCKED -> ' + ERROR_MESSAGE();
END CATCH

PRINT '--- EFFECT 2: unique violation (duplicate email) must FAIL ---';
BEGIN TRY
    INSERT INTO child_t (id, parent_id, email) VALUES (12, 1, 'a@x.com');
    PRINT 'RESULT: UNIQUE DID NOT BLOCK  <-- CONSTRAINT INEFFECTIVE';
END TRY
BEGIN CATCH
    PRINT 'RESULT: UNIQUE BLOCKED -> ' + ERROR_MESSAGE();
END CATCH

PRINT '--- EFFECT 3: filtered unique allows multiple NULLs (must SUCCEED) ---';
INSERT INTO child_t (id, parent_id, email) VALUES (13, 1, NULL);
INSERT INTO child_t (id, parent_id, email) VALUES (14, 1, NULL);
SELECT 'filtered-unique NULL handling' AS probe, COUNT(*) AS null_rows FROM child_t WHERE email IS NULL;

PRINT '--- EFFECT 4: NULL FK is permitted (FK is not NOT NULL) ---';
INSERT INTO child_t (id, parent_id, email) VALUES (15, NULL, 'c@x.com');
SELECT 'nullable FK accepted' AS probe, COUNT(*) AS rows_now FROM child_t;

PRINT '=== T1.1 DONE ===';
GO
USE master;
GO
ALTER DATABASE english_learning_v12probe SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
DROP DATABASE english_learning_v12probe;
PRINT 'scratch DB dropped';
GO
