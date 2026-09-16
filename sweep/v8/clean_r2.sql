-- audit-v8 Round 1 — cleanup of AI-sweep artifacts leaked by the p3a run that
-- hit the 120s harness timeout mid-flight (before its own cleanup step ran).
-- Scoped by exact id + ZZ title prefix so nothing else can be touched.
-- QUOTED_IDENTIFIER ON is mandatory: this DB has a filtered index (IX_uvp_due)
-- and a DELETE batch fails Msg 1934 without it (AGENTS.md rule).
SET QUOTED_IDENTIFIER ON;
GO

DELETE FROM exercises WHERE lesson_id = 101987;
DELETE FROM lessons   WHERE lesson_id = 101987 AND title LIKE 'ZZ%';
GO

SELECT 'lessons=' + CAST(COUNT(*) AS VARCHAR) AS after_cleanup FROM lessons;
SELECT 'exercises=' + CAST(COUNT(*) AS VARCHAR) AS after_cleanup FROM exercises;
GO
