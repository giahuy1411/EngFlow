-- p15-keyword-check.sql — read-only: confirm which search keywords have real
-- matches, so the adversarial harness asserts against the real data rather than
-- against a keyword that legitimately has no rows.
SET NOCOUNT ON;
SELECT COUNT(*) AS total_vocab FROM vocabulary;
SELECT COUNT(*) AS like_work FROM vocabulary WHERE word LIKE '%work%';
SELECT COUNT(*) AS like_the  FROM vocabulary WHERE word LIKE '%the%';
SELECT COUNT(*) AS like_book FROM vocabulary WHERE word LIKE '%book%';
SELECT TOP 5 word, cefr_level FROM vocabulary WHERE word LIKE '%the%' ORDER BY word;
GO
