-- audit-v12 Phase 10 — remove audit/test artifacts.
-- Evidence for every ID here: sweep/v12/cleanup-precheck.sql (run 2026-09-21, all measured live).
-- Every FK touching these tables is NO_ACTION (sweep/v12/fk-map.sql) -> children before parents.
-- Filtered index IX_uvp_due on user_vocabulary_progress requires SET QUOTED_IDENTIFIER ON.
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRAN;

PRINT 'STEP-1-uvp-blockers';
-- Exactly 1 row: adminword (50154, user 2). Show it before removing it.
SELECT 'before: uvp for delete set = ' + CAST(COUNT(*) AS varchar(10))
FROM user_vocabulary_progress
WHERE vocabulary_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);

DELETE FROM user_vocabulary_progress
WHERE vocabulary_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);
PRINT 'STEP-1-uvp-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-2-deck_words-for-deleted-vocab';
DELETE FROM deck_words
WHERE vocab_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);
PRINT 'STEP-2-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-3-deck_words-for-4-empty-decks';
DELETE FROM deck_words WHERE deck_id IN (10016,30033,50038,50039);
PRINT 'STEP-3-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-4-4-empty-decks';
DELETE FROM decks WHERE deck_id IN (10016,30033,50038,50039);
PRINT 'STEP-4-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-5-lesson-61882-children';
DELETE FROM lesson_blocks
WHERE section_id IN (SELECT section_id FROM lesson_sections WHERE lesson_id = 61882);
PRINT 'STEP-5a-blocks-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM lesson_sections WHERE lesson_id = 61882;
PRINT 'STEP-5b-sections-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM exercises WHERE lesson_id = 61882;
PRINT 'STEP-5c-exercises-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM lesson_snapshots WHERE lesson_id = 61882;
PRINT 'STEP-5d-snapshots-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM speaking_prompts WHERE lesson_id = 61882;
PRINT 'STEP-5e-prompts-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM lesson_submissions WHERE lesson_id = 61882;
PRINT 'STEP-5f-subs-deleted=' + CAST(@@ROWCOUNT AS varchar(10));
DELETE FROM user_progress WHERE lesson_id = 61882;
PRINT 'STEP-5g-progress-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-6-9-junk-vocabulary';
DELETE FROM vocabulary
WHERE vocab_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);
PRINT 'STEP-6-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

PRINT 'STEP-7-lesson-61882';
DELETE FROM lessons WHERE lesson_id = 61882;
PRINT 'STEP-7-deleted=' + CAST(@@ROWCOUNT AS varchar(10));

COMMIT;

PRINT 'POST-CHECK';
SELECT 'orphans_left=' + CAST(COUNT(*) AS varchar(10))
FROM vocabulary v WHERE NOT EXISTS (SELECT 1 FROM deck_words dw WHERE dw.vocab_id = v.vocab_id);
SELECT 'deleted_ids_left=' + CAST(COUNT(*) AS varchar(10))
FROM vocabulary WHERE vocab_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);
SELECT 'test_decks_left=' + CAST(COUNT(*) AS varchar(10))
FROM decks WHERE deck_id IN (10016,30033,50038,50039);
SELECT 'lesson_61882_left=' + CAST(COUNT(*) AS varchar(10))
FROM lessons WHERE lesson_id = 61882;
