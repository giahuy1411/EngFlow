-- ============================================================================
-- Database Cleanup V2
-- NOTE: Flyway is disabled. Run manually if applying to a real DB.
-- ============================================================================

-- 1. Replace role NVARCHAR with is_admin BIT
ALTER TABLE users DROP COLUMN role;

-- 2. Drop orphan tables (no entity, no service, no controller)
DROP TABLE IF EXISTS lesson_skills;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS exercise_submissions;
DROP TABLE IF EXISTS grammar;

-- 2b. Drop Docker orphan tables (Phase 2) — order: dependent tables first
DROP TABLE IF EXISTS user_shop_items;
DROP TABLE IF EXISTS skill_submissions;
DROP TABLE IF EXISTS shop_items;

-- 3. Fix nullable FKs that should always have values
ALTER TABLE user_progress ALTER COLUMN user_id BIGINT NOT NULL;
ALTER TABLE user_progress ALTER COLUMN lesson_id BIGINT NOT NULL;
ALTER TABLE user_achievements ALTER COLUMN user_id BIGINT NOT NULL;
ALTER TABLE user_achievements ALTER COLUMN achievement_id BIGINT NOT NULL;

-- 4. Reduce over-sized column widths
ALTER TABLE vocabulary ALTER COLUMN pronunciation NVARCHAR(50);
ALTER TABLE users ALTER COLUMN avatar_url NVARCHAR(300);

-- 5. Missing indexes for FK columns
CREATE INDEX idx_user_streaks_user ON user_streaks(user_id);
CREATE INDEX idx_user_vocab_progress_user ON user_vocabulary_progress(user_id);
CREATE INDEX idx_user_vocab_progress_vocab ON user_vocabulary_progress(vocabulary_id);
CREATE INDEX idx_lesson_submissions_user ON lesson_submissions(user_id);
CREATE INDEX idx_lesson_submissions_lesson ON lesson_submissions(lesson_id);
CREATE INDEX idx_decks_owner ON decks(owner_id);
CREATE INDEX idx_deck_words_deck ON deck_words(deck_id);
CREATE INDEX idx_deck_words_vocab ON deck_words(vocab_id);
