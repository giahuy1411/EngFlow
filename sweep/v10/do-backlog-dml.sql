-- audit-v10 Phase 3.5/3.6 — XOA 4 user rac + DROP 4 bang backup.
--
-- DA DUOC CHU DUYET: chu du an chon "Xoa 4 user zz*" va "Xoa 4 bang backup".
--
-- AN TOAN DA CHUNG MINH TRUOC KHI CHAY (sweep/v10/pre-delete-check.js):
--   - 4 user 170049/170050/170051/170097: is_admin=0, is_premium=0
--   - 0 row phu thuoc o CA 12 FK tro toi users
--   - 4 bang backup: 0 FK tro toi, 0 view/procedure phu thuoc
--   - parity truoc: 1471|43737|76|127|28|15|4|126|14|5
--   - backup: engflow_2026-09-20-pre-backlog-dml.bak, RESTORE VERIFYONLY = valid
--
-- QUY UOC BAT BUOC (AGENTS.md):
--   - SET QUOTED_IDENTIFIER ON cho moi batch DELETE
--   - Danh sach ID CU THE, khong dung LIKE 'zz%' (bai hoc F111: da xoa nham 4 user that)
--   - sqlcmd exit 0 ke ca khi that bai -> phai quet Msg o tang goi
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

-- ============================================================
-- BUOC 1: xoa 4 user rac, theo ID CU THE
-- ============================================================
PRINT '=== TRUOC: 4 user muc tieu ===';
SELECT user_id, username, email FROM users
WHERE user_id IN (170049, 170050, 170051, 170097) ORDER BY user_id;

-- Xoa con truoc cha sau. Ca 12 bang deu da do = 0, nhung van liet ke tuong minh
-- de neu mot ngay nao do co du lieu thi no bi xoa chu khong chan FK.
DELETE FROM study_days               WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM user_vocabulary_progress WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM exercise_attempts        WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM lesson_submissions       WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM speaking_submissions     WHERE user_id IN (170049,170050,170051,170097) OR graded_by IN (170049,170050,170051,170097);
DELETE FROM video_attempts           WHERE user_id IN (170049,170050,170051,170097) OR graded_by IN (170049,170050,170051,170097);
DELETE FROM payment_transactions     WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM decks                    WHERE owner_id IN (170049,170050,170051,170097);
DELETE FROM user_progress            WHERE user_id IN (170049,170050,170051,170097);
DELETE FROM user_streaks             WHERE user_id IN (170049,170050,170051,170097);

DELETE FROM users WHERE user_id IN (170049,170050,170051,170097);
PRINT '=== users da xoa ===';
SELECT @@ROWCOUNT AS deleted_users;

PRINT '=== SAU: phai con 0 ===';
SELECT COUNT(*) AS remaining FROM users WHERE user_id IN (170049,170050,170051,170097);

-- ============================================================
-- BUOC 2: drop 4 bang backup
-- ============================================================
DROP TABLE IF EXISTS dbo.exercises_bak_v5;
DROP TABLE IF EXISTS dbo.exercises_bak_v5b;
DROP TABLE IF EXISTS dbo.exercises_bak_v5c;
DROP TABLE IF EXISTS dbo.exercises_bak_v5d;

PRINT '=== SAU: so bang exercises_bak_v5* con lai (phai = 0) ===';
SELECT COUNT(*) AS bak_tables_left FROM sys.tables WHERE name LIKE 'exercises_bak_v5%';

COMMIT TRANSACTION;

-- ============================================================
-- BUOC 3: parity moi
-- ============================================================
PRINT '=== PARITY SAU KHI XOA (users phai la 72) ===';
SELECT
  (SELECT COUNT(*) FROM lessons) AS lessons,
  (SELECT COUNT(*) FROM exercises) AS exercises,
  (SELECT COUNT(*) FROM users) AS users,
  (SELECT COUNT(*) FROM vocabulary) AS vocabulary,
  (SELECT COUNT(*) FROM speaking_submissions) AS speaking,
  (SELECT COUNT(*) FROM video_attempts) AS video,
  (SELECT COUNT(*) FROM lesson_submissions) AS lesson_sub,
  (SELECT COUNT(*) FROM payment_transactions) AS payments,
  (SELECT COUNT(*) FROM decks) AS decks,
  (SELECT COUNT(*) FROM lesson_snapshots) AS snapshots;
