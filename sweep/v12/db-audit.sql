-- audit-v12-full T1 — DB audit (READ-ONLY; no DML in this file)
-- Run: python sweep/v8/sqlrun.py sweep/v12/db-audit.sql
-- Rule: sqlcmd exits 0 even when a batch fails -> every batch is scanned for 'Msg \d+'.
SET NOCOUNT ON;

PRINT '=== T1.0 DATABASE / COLLATION ===';
SELECT DB_NAME() AS db, DATABASEPROPERTYEX(DB_NAME(),'Collation') AS collation,
       (SELECT COUNT(*) FROM sys.tables) AS tables,
       (SELECT COUNT(*) FROM sys.foreign_keys) AS foreign_keys,
       (SELECT COUNT(*) FROM sys.indexes WHERE is_unique = 1) AS unique_indexes;

PRINT '=== T1.1 CONSTRAINT INVENTORY (FK) ===';
SELECT fk.name, OBJECT_NAME(fk.parent_object_id) AS child, OBJECT_NAME(fk.referenced_object_id) AS parent,
       fk.is_disabled, fk.is_not_trusted
FROM sys.foreign_keys fk ORDER BY child, fk.name;

PRINT '=== T1.1b UNIQUE INDEXES (incl. filtered) ===';
SELECT i.name, OBJECT_NAME(i.object_id) AS tbl, i.is_unique, i.has_filter, i.filter_definition
FROM sys.indexes i WHERE i.is_unique = 1 AND i.name IS NOT NULL
ORDER BY tbl, i.name;

PRINT '=== T1.2 ORPHAN SCAN (hand-written; NULL FK counted SEPARATELY) ===';
SELECT 'exercise_attempts->lessons' AS relation,
       (SELECT COUNT(*) FROM exercise_attempts ea WHERE ea.lesson_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM lessons l WHERE l.lesson_id = ea.lesson_id)) AS orphans,
       (SELECT COUNT(*) FROM exercise_attempts WHERE lesson_id IS NULL) AS null_fk;
SELECT 'uvp->vocabulary' AS relation,
       (SELECT COUNT(*) FROM user_vocabulary_progress p WHERE p.vocabulary_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM vocabulary v WHERE v.vocab_id = p.vocabulary_id)) AS orphans,
       (SELECT COUNT(*) FROM user_vocabulary_progress WHERE vocabulary_id IS NULL) AS null_fk;
SELECT 'uvp->users' AS relation,
       (SELECT COUNT(*) FROM user_vocabulary_progress p WHERE p.user_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = p.user_id)) AS orphans,
       (SELECT COUNT(*) FROM user_vocabulary_progress WHERE user_id IS NULL) AS null_fk;
SELECT 'deck_words->decks' AS relation,
       (SELECT COUNT(*) FROM deck_words dw WHERE dw.deck_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM decks d WHERE d.deck_id = dw.deck_id)) AS orphans,
       (SELECT COUNT(*) FROM deck_words WHERE deck_id IS NULL) AS null_fk;
SELECT 'deck_words->vocabulary' AS relation,
       (SELECT COUNT(*) FROM deck_words dw WHERE dw.vocab_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM vocabulary v WHERE v.vocab_id = dw.vocab_id)) AS orphans,
       (SELECT COUNT(*) FROM deck_words WHERE vocab_id IS NULL) AS null_fk;
SELECT 'vocabulary->lessons' AS relation,
       (SELECT COUNT(*) FROM vocabulary v WHERE v.lesson_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM lessons l WHERE l.lesson_id = v.lesson_id)) AS orphans,
       (SELECT COUNT(*) FROM vocabulary WHERE lesson_id IS NULL) AS null_fk;
SELECT 'study_days->users' AS relation,
       (SELECT COUNT(*) FROM study_days s WHERE s.user_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = s.user_id)) AS orphans,
       (SELECT COUNT(*) FROM study_days WHERE user_id IS NULL) AS null_fk;

PRINT '=== T1.3 STREAK SCHEMA ===';
SELECT 'study_policy rows' AS metric, CAST(COUNT(*) AS varchar(30)) AS value FROM study_policy
UNION ALL SELECT 'study_policy.effective_from (max)', CONVERT(varchar(30), MAX(effective_from), 23) FROM study_policy
UNION ALL SELECT 'study_days rows', CAST(COUNT(*) AS varchar(30)) FROM study_days
UNION ALL SELECT 'fk_study_days_user present', CAST(COUNT(*) AS varchar(30)) FROM sys.foreign_keys WHERE name='fk_study_days_user'
UNION ALL SELECT 'fk_study_days_user enabled', CAST(COUNT(*) AS varchar(30)) FROM sys.foreign_keys WHERE name='fk_study_days_user' AND is_disabled=0
UNION ALL SELECT 'uq_study_days_user_date present', CAST(COUNT(*) AS varchar(30)) FROM sys.indexes WHERE name='uq_study_days_user_date';

PRINT '=== T1.4 ROW-LEVEL SANITY ===';
SELECT 'lessons total' AS metric, CAST(COUNT(*) AS varchar(30)) AS value FROM lessons
UNION ALL SELECT 'lessons published', CAST(COUNT(*) AS varchar(30)) FROM lessons WHERE is_published = 1
UNION ALL SELECT 'lessons draft', CAST(COUNT(*) AS varchar(30)) FROM lessons WHERE is_published = 0
UNION ALL SELECT 'exercises total', CAST(COUNT(*) AS varchar(30)) FROM exercises
UNION ALL SELECT 'exercises empty correct_answer', CAST(COUNT(*) AS varchar(30)) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer))=''
UNION ALL SELECT 'users total', CAST(COUNT(*) AS varchar(30)) FROM users
UNION ALL SELECT 'users premium', CAST(COUNT(*) AS varchar(30)) FROM users WHERE is_premium = 1
UNION ALL SELECT 'decks total', CAST(COUNT(*) AS varchar(30)) FROM decks
UNION ALL SELECT 'decks system (no owner)', CAST(COUNT(*) AS varchar(30)) FROM decks WHERE owner_id IS NULL
UNION ALL SELECT 'vocabulary total', CAST(COUNT(*) AS varchar(30)) FROM vocabulary
UNION ALL SELECT 'payment_transactions total', CAST(COUNT(*) AS varchar(30)) FROM payment_transactions
UNION ALL SELECT 'payments PENDING txn NULL', CAST(COUNT(*) AS varchar(30)) FROM payment_transactions WHERE status='PENDING' AND transaction_id IS NULL
UNION ALL SELECT 'speaking_submissions', CAST(COUNT(*) AS varchar(30)) FROM speaking_submissions
UNION ALL SELECT 'video_attempts', CAST(COUNT(*) AS varchar(30)) FROM video_attempts
UNION ALL SELECT 'lesson_submissions', CAST(COUNT(*) AS varchar(30)) FROM lesson_submissions
UNION ALL SELECT 'lesson_snapshots', CAST(COUNT(*) AS varchar(30)) FROM lesson_snapshots;

PRINT '=== T1.4b LISTENING missing audio_url (re-measure, do not inherit) ===';
SELECT 'exercises type LISTENING' AS metric, CAST(COUNT(*) AS varchar(30)) AS value FROM exercises WHERE exercise_type='LISTENING'
UNION ALL SELECT 'LISTENING missing audio_url', CAST(COUNT(*) AS varchar(30)) FROM exercises
        WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='');

PRINT '=== T1.5 DUPLICATE / INTEGRITY ===';
SELECT 'dup users.email' AS check_name, CAST(COUNT(*) AS varchar(30)) AS offenders FROM (SELECT email FROM users GROUP BY email HAVING COUNT(*)>1) x
UNION ALL SELECT 'dup users.username', CAST(COUNT(*) AS varchar(30)) FROM (SELECT username FROM users GROUP BY username HAVING COUNT(*)>1) x
UNION ALL SELECT 'dup study_days(user,date)', CAST(COUNT(*) AS varchar(30)) FROM (SELECT user_id, study_date FROM study_days GROUP BY user_id, study_date HAVING COUNT(*)>1) x
UNION ALL SELECT 'dup deck_words(deck,vocab)', CAST(COUNT(*) AS varchar(30)) FROM (SELECT deck_id, vocab_id FROM deck_words GROUP BY deck_id, vocab_id HAVING COUNT(*)>1) x
UNION ALL SELECT 'dup payment transaction_id', CAST(COUNT(*) AS varchar(30)) FROM (SELECT transaction_id FROM payment_transactions WHERE transaction_id IS NOT NULL GROUP BY transaction_id HAVING COUNT(*)>1) x;

PRINT '=== T1.6 INDEX INVENTORY ON HOT TABLES ===';
SELECT OBJECT_NAME(i.object_id) AS tbl, i.name AS idx, i.type_desc, i.is_unique, i.has_filter
FROM sys.indexes i
WHERE i.object_id IN (OBJECT_ID('lessons'), OBJECT_ID('exercises'), OBJECT_ID('vocabulary'),
                      OBJECT_ID('deck_words'), OBJECT_ID('payment_transactions'), OBJECT_ID('study_days'))
  AND i.name IS NOT NULL
ORDER BY tbl, i.name;

PRINT '=== T1.7 FRAGMENTATION (hot indexes) ===';
SELECT OBJECT_NAME(ips.object_id) AS tbl, i.name AS idx, ips.page_count,
       CAST(ips.avg_fragmentation_in_percent AS decimal(5,2)) AS frag_pct
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips
JOIN sys.indexes i ON i.object_id = ips.object_id AND i.index_id = ips.index_id
WHERE ips.page_count > 50 AND ips.avg_fragmentation_in_percent > 5
ORDER BY ips.avg_fragmentation_in_percent DESC;

PRINT '=== END DB AUDIT ===';
