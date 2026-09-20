-- audit-v10 Phase 3.5/3.6 — KIEM TRA TRUOC KHI XOA. Read-only.
--
-- Danh sach bang/cot o day KHONG duoc doan. No lay tu sys.foreign_key_columns
-- (sweep/v10/find-user-fks.sql), vi ban nhap dau tien cua file nay doan sai:
--   - `decks` dung `owner_id`, khong phai `user_id`   -> Msg 207
--   - thieu han `user_progress` va `user_streaks`
--   - `speaking_submissions` va `video_attempts` co HAI FK (user_id + graded_by)
-- Doan ten cot sinh ra Msg 207, va sqlcmd VAN exit 0 — nen neu khong quet Msg
-- thi mot bang khong duoc kiem se trong y het mot bang rong.
SET NOCOUNT ON;

PRINT '=== 1. Xac nhan 4 user nay TON TAI va dung la rac ===';
SELECT user_id, username, email, is_admin, is_premium, created_at
FROM users WHERE user_id IN (170049, 170050, 170051, 170097) ORDER BY user_id;

PRINT '=== 2. Dem row cua 4 user nay o TUNG bang co FK toi users ===';
SELECT 'study_days' AS bang, COUNT(*) AS n FROM study_days WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'user_vocabulary_progress', COUNT(*) FROM user_vocabulary_progress WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'exercise_attempts',   COUNT(*) FROM exercise_attempts   WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'lesson_submissions',  COUNT(*) FROM lesson_submissions  WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'speaking_submissions (user_id)',  COUNT(*) FROM speaking_submissions WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'speaking_submissions (graded_by)',COUNT(*) FROM speaking_submissions WHERE graded_by IN (170049,170050,170051,170097)
UNION ALL SELECT 'video_attempts (user_id)',   COUNT(*) FROM video_attempts WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'video_attempts (graded_by)', COUNT(*) FROM video_attempts WHERE graded_by IN (170049,170050,170051,170097)
UNION ALL SELECT 'payment_transactions',COUNT(*) FROM payment_transactions WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'decks (owner_id)',    COUNT(*) FROM decks    WHERE owner_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'user_progress',       COUNT(*) FROM user_progress WHERE user_id IN (170049,170050,170051,170097)
UNION ALL SELECT 'user_streaks',        COUNT(*) FROM user_streaks  WHERE user_id IN (170049,170050,170051,170097);

PRINT '=== 3. 4 bang backup co bi FK hay doi tuong nao tro toi khong? ===';
SELECT t.name AS bang,
       (SELECT COUNT(*) FROM sys.foreign_keys WHERE referenced_object_id = t.object_id) AS fk_tro_toi,
       (SELECT COUNT(*) FROM sys.sql_expression_dependencies WHERE referenced_id = t.object_id) AS phu_thuoc_khac
FROM sys.tables t WHERE t.name LIKE 'exercises_bak_v5%' ORDER BY t.name;

PRINT '=== 4. Co VIEW hay PROCEDURE nao ten chua exercises_bak khong? ===';
SELECT name, type_desc FROM sys.objects
WHERE name LIKE '%exercises_bak%' AND type IN ('V','P','FN','IF','TF','TR');

PRINT '=== 5. Tong users truoc khi xoa (baseline 76) ===';
SELECT COUNT(*) AS total_users FROM users;
