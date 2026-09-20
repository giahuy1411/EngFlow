-- audit-v10 Phase 1.13 — doc trang thai study_days truoc/sau moi kich ban.
-- Tham so: :uid  (user id)
SET NOCOUNT ON;

PRINT '=== study_days cua user ===';
SELECT id, user_id, study_date FROM study_days WHERE user_id = $(uid) ORDER BY study_date;

PRINT '=== TONG study_days toan bang ===';
SELECT COUNT(*) AS total_rows FROM study_days;

PRINT '=== study_policy ===';
SELECT id, effective_from FROM study_policy;

PRINT '=== progress rows cua user (de cleanup) ===';
SELECT TOP 5 id, user_id, vocabulary_id, review_count, mastery_level
FROM user_vocabulary_progress WHERE user_id = $(uid) ORDER BY id DESC;
