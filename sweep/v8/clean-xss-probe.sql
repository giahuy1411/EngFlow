-- clean-xss-probe.sql — remove ONLY the vocabulary rows created by the XSS
-- probes on 2026-09-16. Two shapes, both verified by hand before deleting:
--   - word LIKE 'XSSPROBE%'  (xss-prove.js marker words)
--   - word IN ('img','iframe') with lesson_id NULL (security.js section 5 saved
--     whatever noun the model extracted from the payload)
-- Scoped by marker/word AND lesson_id IS NULL AND the run date, so a legitimate
-- word with the same spelling but a real lesson_id can never be caught.
--
-- Safety: SET QUOTED_IDENTIFIER ON is required for every DELETE batch in this
-- DB because of the filtered indexes; sqlcmd still returns exit code 0 when the
-- batch fails, so the caller must grep for 'Msg ' in the output.
--
-- Verified before deleting: 7 rows total, 0 user_vocabulary_progress dependents.

SET QUOTED_IDENTIFIER ON;
GO

PRINT '--- BEFORE ---';
SELECT vocab_id, word, lesson_id, created_at FROM vocabulary
WHERE (word LIKE 'XSSPROBE%' OR word IN ('img', 'iframe'))
  AND lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17'
ORDER BY created_at;
GO

DELETE FROM vocabulary
WHERE (word LIKE 'XSSPROBE%' OR word IN ('img', 'iframe'))
  AND lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17';
GO

PRINT '--- AFTER (must be 0 rows) ---';
SELECT COUNT(*) AS remaining FROM vocabulary
WHERE (word LIKE 'XSSPROBE%' OR word IN ('img', 'iframe'))
  AND lesson_id IS NULL
  AND created_at >= '2026-09-16' AND created_at < '2026-09-17';
GO

PRINT '--- parity re-check (baseline is 127) ---';
SELECT COUNT(*) AS vocabulary_total FROM vocabulary;
GO

