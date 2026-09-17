-- p15-clean-residue.sql — remove the 3 rows the first xss-prove cleanup attempt
-- missed (its WHERE window began after the rows were written).
-- Scope: no lesson link (user-saved words, never curriculum) + today + the exact
-- words observed: XSSPROBE* markers and the model-extracted noun 'iframe'.
SET QUOTED_IDENTIFIER ON;
GO

PRINT '--- BEFORE ---';
SELECT vocab_id, word, lesson_id, created_at FROM vocabulary
WHERE lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17'
  AND (word LIKE 'XSSPROBE%' OR word = 'iframe')
ORDER BY vocab_id;
GO

DELETE FROM vocabulary
WHERE lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17'
  AND (word LIKE 'XSSPROBE%' OR word = 'iframe');
GO

PRINT '--- AFTER (must be 0) ---';
SELECT COUNT(*) AS remaining FROM vocabulary
WHERE lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17'
  AND (word LIKE 'XSSPROBE%' OR word = 'iframe');
GO

PRINT '--- parity (baseline 127) ---';
SELECT COUNT(*) AS vocabulary_total FROM vocabulary;
GO
