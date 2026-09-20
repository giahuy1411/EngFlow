-- audit-v11-full Phase 1 — DB audit (READ-ONLY on english_learning)
-- Rule: caller scans output for `Msg \d+` (sqlcmd exits 0 even when a batch fails).
-- Rule: NULL FKs must NOT be counted as orphans.
-- Schema verified against sys.columns before writing this (PKs are lesson_id / deck_id /
-- vocab_id / user_id / attempt_id / deck_word_id — NOT `id`; my first draft used `id`
-- and the Msg-207 scan caught it. That is the rule working, recorded in the evidence.)
SET NOCOUNT ON;

PRINT '=== T1.3 STREAK SCHEMA ===';
SELECT 'study_policy rows' AS c, COUNT(*) AS n FROM study_policy;
SELECT id, effective_from FROM study_policy;
SELECT 'study_days rows' AS c, COUNT(*) AS n FROM study_days;
SELECT 'FK fk_study_days_user' AS c, COUNT(*) AS n FROM sys.foreign_keys WHERE name = 'fk_study_days_user';
SELECT 'UQ uq_study_days_user_date' AS c, COUNT(*) AS n FROM sys.indexes WHERE name = 'uq_study_days_user_date';

PRINT '=== T1.1 CONSTRAINT INVENTORY ===';
SELECT fk.name AS fk_name, OBJECT_NAME(fk.parent_object_id) AS tbl,
       OBJECT_NAME(fk.referenced_object_id) AS ref_tbl, fk.is_disabled
FROM sys.foreign_keys fk ORDER BY tbl, fk_name;
SELECT i.name AS uq_name, OBJECT_NAME(i.object_id) AS tbl, i.has_filter
FROM sys.indexes i
WHERE i.is_unique = 1 AND i.name IS NOT NULL
ORDER BY tbl, uq_name;

PRINT '=== T1.2 ORPHAN SCAN (NULL FK excluded, not hidden) ===';
SELECT 'exercise_attempts -> lessons' AS rel, COUNT(*) AS orphans
FROM exercise_attempts ea
WHERE ea.lesson_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM lessons l WHERE l.lesson_id = ea.lesson_id);
SELECT 'user_vocabulary_progress -> vocabulary' AS rel, COUNT(*) AS orphans
FROM user_vocabulary_progress p
WHERE p.vocabulary_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM vocabulary v WHERE v.vocab_id = p.vocabulary_id);
SELECT 'user_vocabulary_progress -> users' AS rel, COUNT(*) AS orphans
FROM user_vocabulary_progress p
WHERE p.user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = p.user_id);
SELECT 'deck_words -> decks' AS rel, COUNT(*) AS orphans
FROM deck_words dw
WHERE dw.deck_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM decks d WHERE d.deck_id = dw.deck_id);
SELECT 'deck_words -> vocabulary' AS rel, COUNT(*) AS orphans
FROM deck_words dw
WHERE dw.vocab_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM vocabulary v WHERE v.vocab_id = dw.vocab_id);

PRINT '=== T1.2b NULL-FK COUNTS (shown separately so they cannot masquerade as orphans) ===';
SELECT 'exercise_attempts NULL lesson_id' AS c, COUNT(*) AS n FROM exercise_attempts WHERE lesson_id IS NULL;
SELECT 'user_vocabulary_progress NULL vocab_id' AS c, COUNT(*) AS n FROM user_vocabulary_progress WHERE vocabulary_id IS NULL;
SELECT 'vocabulary NULL lesson_id (deck-owned)' AS c, COUNT(*) AS n FROM vocabulary WHERE lesson_id IS NULL;

PRINT '=== T1.4 ROW-LEVEL SANITY ===';
SELECT 'lessons total' AS c, COUNT(*) AS n FROM lessons
UNION ALL SELECT 'lessons published', COUNT(*) FROM lessons WHERE is_published = 1
UNION ALL SELECT 'lessons draft', COUNT(*) FROM lessons WHERE is_published = 0
UNION ALL SELECT 'exercises total', COUNT(*) FROM exercises
UNION ALL SELECT 'exercises empty correct_answer', COUNT(*) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = ''
UNION ALL SELECT 'exercises LISTENING missing audio_url', COUNT(*) FROM exercises
   WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '')
UNION ALL SELECT 'users total', COUNT(*) FROM users
UNION ALL SELECT 'users premium', COUNT(*) FROM users WHERE is_premium = 1
UNION ALL SELECT 'payments total', COUNT(*) FROM payment_transactions
UNION ALL SELECT 'payments PENDING txn NULL (audit residue)', COUNT(*) FROM payment_transactions
   WHERE status = 'PENDING' AND transaction_id IS NULL
UNION ALL SELECT 'decks total', COUNT(*) FROM decks
UNION ALL SELECT 'decks system (no owner)', COUNT(*) FROM decks WHERE owner_id IS NULL
UNION ALL SELECT 'study_days rows', COUNT(*) FROM study_days;

PRINT '=== T1.4b DUPLICATE / INTEGRITY RISKS ===';
SELECT 'users duplicate email' AS c, COUNT(*) AS n FROM (
  SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1) t;
SELECT 'users duplicate username' AS c, COUNT(*) AS n FROM (
  SELECT username FROM users GROUP BY username HAVING COUNT(*) > 1) t;
SELECT 'vocabulary duplicate (word,lesson)' AS c, COUNT(*) AS n FROM (
  SELECT word, lesson_id FROM vocabulary GROUP BY word, lesson_id HAVING COUNT(*) > 1) t;
SELECT 'study_days duplicate (user,date)' AS c, COUNT(*) AS n FROM (
  SELECT user_id, study_date FROM study_days GROUP BY user_id, study_date HAVING COUNT(*) > 1) t;

PRINT '=== T1.5 INDEX INVENTORY ON HOT READ PATHS ===';
SELECT OBJECT_NAME(i.object_id) AS tbl, i.name AS idx, i.type_desc, i.is_unique, i.has_filter
FROM sys.indexes i
WHERE i.object_id IN (OBJECT_ID('lessons'), OBJECT_ID('exercises'),
                      OBJECT_ID('vocabulary'), OBJECT_ID('deck_words'),
                      OBJECT_ID('payment_transactions'), OBJECT_ID('study_days'))
  AND i.name IS NOT NULL
ORDER BY tbl, idx;

PRINT '=== T1.5b FRAGMENTATION (page_count > 100, frag > 10%) ===';
SELECT OBJECT_NAME(ips.object_id) AS tbl, i.name AS idx,
       CAST(ips.avg_fragmentation_in_percent AS DECIMAL(6,2)) AS frag_pct, ips.page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips
JOIN sys.indexes i ON i.object_id = ips.object_id AND i.index_id = ips.index_id
WHERE ips.page_count > 100 AND ips.avg_fragmentation_in_percent > 10
ORDER BY ips.avg_fragmentation_in_percent DESC;
