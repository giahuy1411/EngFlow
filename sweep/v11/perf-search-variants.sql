-- audit-v11 Phase 6 — is there a cheaper form of the admin exercise search?
-- The JPA query is:
--   LOWER(e.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
--      OR LOWER(COALESCE(e.explanation,'')) LIKE LOWER(CONCAT('%', :keyword, '%'))
-- Two things could matter: (a) the LOWER() wrapping, (b) the leading wildcard.
-- Measure, do not assume. SET STATISTICS IO/TIME ON gives logical reads + CPU.
SET NOCOUNT ON;
SET STATISTICS IO ON;
SET STATISTICS TIME ON;

PRINT '--- 0. collation of the question column (decides whether LOWER() is even needed) ---';
SELECT c.name AS col, c.collation_name
FROM sys.columns c
WHERE c.object_id = OBJECT_ID('exercises') AND c.name IN ('question','explanation');

PRINT '--- 1. CURRENT FORM: LOWER(col) LIKE LOWER(%kw%)  [what the app runs] ---';
SELECT COUNT(*) AS n FROM exercises e
WHERE LOWER(e.question) LIKE LOWER(CONCAT('%','the','%'))
   OR LOWER(COALESCE(e.explanation,'')) LIKE LOWER(CONCAT('%','the','%'));

PRINT '--- 2. WITHOUT LOWER(): relies on collation being case-insensitive ---';
SELECT COUNT(*) AS n FROM exercises e
WHERE e.question LIKE '%the%'
   OR COALESCE(e.explanation,'') LIKE '%the%';

PRINT '--- 3. SAME COUNT? if identical, form 2 is behaviourally equivalent ---';
SELECT
  (SELECT COUNT(*) FROM exercises e
     WHERE LOWER(e.question) LIKE LOWER(CONCAT('%','the','%'))
        OR LOWER(COALESCE(e.explanation,'')) LIKE LOWER(CONCAT('%','the','%'))) AS with_lower,
  (SELECT COUNT(*) FROM exercises e
     WHERE e.question LIKE '%the%'
        OR COALESCE(e.explanation,'') LIKE '%the%') AS without_lower;

PRINT '--- 4. index the plan could use, if any ---';
SELECT i.name, i.type_desc, i.has_filter
FROM sys.indexes i WHERE i.object_id = OBJECT_ID('exercises') AND i.name IS NOT NULL;

SET STATISTICS IO OFF;
SET STATISTICS TIME OFF;
