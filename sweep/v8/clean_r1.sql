-- Cleanup leftover rows created by audit-v8 Round 1 (run r1-20260916).
-- Every batch MUST start with SET QUOTED_IDENTIFIER ON: the DB contains a
-- filtered index (IX_uvp_due) and sqlcmd otherwise fails with Msg 1934 while
-- still returning exit code 0.
SET QUOTED_IDENTIFIER ON;

-- p6 run 1 leaked one vocabulary row (the cleanup step used the wrong route).
DELETE FROM vocabulary WHERE word LIKE 'ZZ r1 %';

-- leftovers from any earlier probe generation (must also be zero)
DELETE FROM vocabulary WHERE word LIKE 'ZZ v8 %';
DELETE FROM vocabulary WHERE word LIKE 'ZZ r1%';
DELETE FROM vocabulary WHERE word LIKE 'ZZ v8%';

-- report what remains (expect 0 rows)
SELECT COUNT(*) AS remaining_zz_vocab FROM vocabulary WHERE word LIKE 'ZZ %';
