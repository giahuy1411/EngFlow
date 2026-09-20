-- audit-v11 — remove the speaking submission created by sweep/v11/speaking-record-test.js.
--
-- The test creates ONE row (plus one MinIO object). This script removes the row; the
-- MinIO object is removed separately (see the runbook in the evidence file), because SQL
-- Server cannot reach MinIO.
--
-- Enumerated id only — never a bare LIKE. An earlier audit deleted four baseline users
-- with a pattern-based delete; that mistake is not repeated here.
--
-- Replace <ID> with the id the test printed.
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT OFF;

PRINT '--- before ---';
SELECT COUNT(*) AS submissions_before FROM speaking_submissions;
SELECT id, status, media_object_key FROM speaking_submissions WHERE id = <ID>;

DELETE FROM speaking_submissions WHERE id = <ID>;

PRINT '--- after ---';
SELECT COUNT(*) AS submissions_after FROM speaking_submissions;
