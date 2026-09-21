SET NOCOUNT ON;

PRINT 'SECTION-ORPHAN-COUNT';
SELECT 'orphans=' + CAST(COUNT(*) AS varchar(10))
FROM vocabulary v WHERE NOT EXISTS (SELECT 1 FROM deck_words dw WHERE dw.vocab_id = v.vocab_id);

PRINT 'SECTION-ALL-ORPHANS';
SELECT CAST(v.vocab_id AS varchar(20)) + ' ~ ' + v.word + ' ~ ' + ISNULL(v.source,'-') + ' ~ lesson=' + ISNULL(CAST(v.lesson_id AS varchar(20)),'-') + ' ~ cefr=' + ISNULL(v.cefr_level,'-') + ' ~ ' + CONVERT(varchar(19), v.created_at, 126)
FROM vocabulary v WHERE NOT EXISTS (SELECT 1 FROM deck_words dw WHERE dw.vocab_id = v.vocab_id)
ORDER BY v.vocab_id;

PRINT 'SECTION-DELETE-SET';
SELECT CAST(v.vocab_id AS varchar(20)) + ' ~ ' + v.word + ' ~ ' + ISNULL(v.source,'-') + ' ~ lesson=' + ISNULL(CAST(v.lesson_id AS varchar(20)),'-') + ' ~ ' + CONVERT(varchar(19), v.created_at, 126)
FROM vocabulary v WHERE v.vocab_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157)
ORDER BY v.vocab_id;

PRINT 'SECTION-BLOCKERS';
SELECT 'uvp_rows=' + CAST(COUNT(*) AS varchar(10))
FROM user_vocabulary_progress WHERE vocabulary_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);
SELECT 'deck_words_links=' + CAST(COUNT(*) AS varchar(10))
FROM deck_words WHERE vocab_id IN (20121,30124,40137,40148,40154,50154,50155,50156,50157);

PRINT 'SECTION-DECKS';
SELECT CAST(d.deck_id AS varchar(20)) + ' ~ ' + d.name + ' ~ owner=' + CAST(d.owner_id AS varchar(20)) + ' ~ public=' + CAST(d.is_public AS varchar(1)) + ' ~ src=' + ISNULL(d.source,'-') + ' ~ words=' + CAST((SELECT COUNT(*) FROM deck_words dw WHERE dw.deck_id = d.deck_id) AS varchar(10))
FROM decks d WHERE d.deck_id IN (10016,30033,50038,50039) ORDER BY d.deck_id;

PRINT 'SECTION-LESSON61882';
SELECT CAST(l.lesson_id AS varchar(20)) + ' ~ ' + l.title + ' ~ published=' + CAST(l.is_published AS varchar(1))
       + ' ~ sections=' + CAST((SELECT COUNT(*) FROM lesson_sections s WHERE s.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ blocks=' + CAST((SELECT COUNT(*) FROM lesson_blocks b WHERE b.section_id IN (SELECT s.section_id FROM lesson_sections s WHERE s.lesson_id = l.lesson_id)) AS varchar(10))
       + ' ~ exercises=' + CAST((SELECT COUNT(*) FROM exercises e WHERE e.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ vocab=' + CAST((SELECT COUNT(*) FROM vocabulary v WHERE v.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ snapshots=' + CAST((SELECT COUNT(*) FROM lesson_snapshots sn WHERE sn.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ prompts=' + CAST((SELECT COUNT(*) FROM speaking_prompts sp WHERE sp.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ subs=' + CAST((SELECT COUNT(*) FROM lesson_submissions ls WHERE ls.lesson_id = l.lesson_id) AS varchar(10))
       + ' ~ progress=' + CAST((SELECT COUNT(*) FROM user_progress up WHERE up.lesson_id = l.lesson_id) AS varchar(10))
FROM lessons l WHERE l.lesson_id = 61882;

PRINT 'SECTION-VOCAB-NAMED-TESTWORD-OR-DUP';
SELECT CAST(v.vocab_id AS varchar(20)) + ' ~ ' + v.word + ' ~ src=' + ISNULL(v.source,'-') + ' ~ lesson=' + ISNULL(CAST(v.lesson_id AS varchar(20)),'-')
FROM vocabulary v WHERE v.word IN ('testword','testword45851','qatestword','adminword','test123','finaltest5','aisaved','planeta','innovate')
ORDER BY v.word, v.vocab_id;
