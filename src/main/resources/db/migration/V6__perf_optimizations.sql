-- V6__perf_optimizations.sql
-- Purpose: Add missing nonclustered indexes to back hot lookup paths.
-- Scope: INDEX-ONLY changes. No schema/column/drop/alter operations.
-- Safe to run idempotently: every statement is guarded by IF NOT EXISTS.
-- Generated from sys.dm_db_missing_index_details + FK analysis on english_learning.
-- Verified against sys.indexes: all statements below create indexes that did
-- NOT exist at audit time.
SET QUOTED_IDENTIFIER ON;
GO

-- 1. deck_words (100 rows) — FK to decks (deck_id) and FK to vocabulary (vocab_id).
--    Only the PK and one UK existed; both FK columns were unindexed.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_deck_words_deck')
BEGIN
    CREATE NONCLUSTERED INDEX idx_deck_words_deck
        ON dbo.deck_words (deck_id);
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_deck_words_vocab')
BEGIN
    CREATE NONCLUSTERED INDEX idx_deck_words_vocab
        ON dbo.deck_words (vocab_id);
END
GO

-- 2. payment_transactions (22 rows) — FK to users (user_id) is backed by
--    idx_payment_transactions_user (user_id, status, created_at). The payment
--    webhook's idempotency lookup on transaction_id was served only by the
--    UNIQUE constraint (non-seekable for point lookups). Add a dedicated index.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_payment_transaction_id')
BEGIN
    CREATE NONCLUSTERED INDEX idx_payment_transaction_id
        ON dbo.payment_transactions (transaction_id);
END
GO

-- 3. vocabulary (114 rows) — admin word search already uses idx_vocabulary_word.
--    Add a covering index so word lookups avoid key-lookup on the wide PK row.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_vocabulary_word_cefr')
BEGIN
    CREATE NONCLUSTERED INDEX idx_vocabulary_word_cefr
        ON dbo.vocabulary (word)
        INCLUDE (cefr_level, lesson_id, vocab_id);
END
GO
