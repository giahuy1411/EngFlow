-- V004__index_maintenance.sql
-- Index maintenance for EngFlow (applied manually; Flyway is disabled).
-- Created: 2026-08-28
-- Based on Phase 3 DB performance inspection (see db_perf_report.txt).
--
-- Changes:
--   1. Drop 7 redundant indexes (exact duplicates of UNIQUE constraints or
--      prefix-redundant). Drops write overhead and shrinks the database.
--   2. Rebuild the fragmented clustered/leaf indexes on the two hot tables
--      (lessons 80.7%, exercises 24-27% fragmentation).
--
-- SAFETY: All drops use IF EXISTS. The drop candidates were verified against
-- sys.indexes + sys.dm_db_index_usage_stats (never-touched / duplicate keys).

-- ============================================
-- 1. DROP REDUNDANT INDEXES
-- ============================================

-- user_progress.idx_progress_user_lesson (user_id, lesson_id)
--   == duplicates UNIQUE constraint UK8sschjnhw7q49ml9th0urvo4b (user_id, lesson_id)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_progress_user_lesson' AND object_id = OBJECT_ID('user_progress'))
    DROP INDEX idx_progress_user_lesson ON user_progress;

-- user_progress.idx_progress_user (user_id)
--   == left-most prefix of UNIQUE (user_id, lesson_id) index
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_progress_user' AND object_id = OBJECT_ID('user_progress'))
    DROP INDEX idx_progress_user ON user_progress;

-- payment_transactions.idx_payment_transaction_id (transaction_id)
--   == duplicates UNIQUE constraint UKlsp8jh693lih2txq7dl4bdnpx (transaction_id)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_payment_transaction_id' AND object_id = OBJECT_ID('payment_transactions'))
    DROP INDEX idx_payment_transaction_id ON payment_transactions;

-- user_vocabulary_progress.idx_uvp_user_vocab (user_id, vocabulary_id)
--   == duplicates UNIQUE constraint UKcnc61y66y0f9p96e6j6qlbswl (user_id, vocabulary_id)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_uvp_user_vocab' AND object_id = OBJECT_ID('user_vocabulary_progress'))
    DROP INDEX idx_uvp_user_vocab ON user_vocabulary_progress;

-- lessons.idx_lessons_published (is_published)
--   == prefix of lessons.idx_lessons_pub_level_order (is_published, level, order_index)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_lessons_published' AND object_id = OBJECT_ID('lessons'))
    DROP INDEX idx_lessons_published ON lessons;

-- exercises.idx_exercises_lesson (lesson_id)
--   == prefix of exercises.idx_exercises_lesson_order (lesson_id, order_index) [hot index]
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_exercises_lesson' AND object_id = OBJECT_ID('exercises'))
    DROP INDEX idx_exercises_lesson ON exercises;

-- vocabulary.idx_vocabulary_word (word)
--   == prefix of vocabulary.idx_vocabulary_word_cefr (word, includes ...)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vocabulary_word' AND object_id = OBJECT_ID('vocabulary'))
    DROP INDEX idx_vocabulary_word ON vocabulary;

-- ============================================
-- 2. REBUILD FRAGMENTED INDEXES
-- ============================================

-- lessons.PK (clustered): 80.70% fragmented
ALTER INDEX PK__lessons__6421F7BE05A3095C ON lessons REBUILD WITH (ONLINE = OFF, SORT_IN_TEMPDB = ON);

-- exercises.idx_exercises_lesson_order: 24.52% fragmented (hot index)
ALTER INDEX idx_exercises_lesson_order ON exercises REBUILD WITH (ONLINE = OFF, SORT_IN_TEMPDB = ON);

-- exercises.PK (clustered): 7.72% fragmented
ALTER INDEX PK__exercise__C121418ED83A5E11 ON exercises REBUILD WITH (ONLINE = OFF, SORT_IN_TEMPDB = ON);

GO
