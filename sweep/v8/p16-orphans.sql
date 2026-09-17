SET NOCOUNT ON;
-- column name verified against INFORMATION_SCHEMA before writing this:
--   user_vocabulary_progress.vocabulary_id  (NOT vocab_id)
SELECT (SELECT COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id = e.lesson_id WHERE l.lesson_id IS NULL) AS orphan_ex,
       (SELECT COUNT(*) FROM user_vocabulary_progress p LEFT JOIN vocabulary v ON v.vocab_id = p.vocabulary_id WHERE v.vocab_id IS NULL) AS orphan_uvp_vocab,
       (SELECT COUNT(*) FROM user_vocabulary_progress p LEFT JOIN users u ON u.user_id = p.user_id WHERE u.user_id IS NULL) AS orphan_uvp_user,
       (SELECT COUNT(*) FROM lesson_snapshots s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL) AS orphan_snap,
       (SELECT COUNT(*) FROM lesson_submissions s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL) AS orphan_sub;
GO
