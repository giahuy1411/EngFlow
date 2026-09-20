-- audit-v10 — Phuong an A: doi nhan 7 row "LISTENING" thanh "MULTIPLE_CHOICE".
--
-- LY DO (do duoc, khong suy doan — evidence/listening-9-options.md):
--   9 row mang nhan LISTENING nhung noi dung la TRAC NGHIEM doc/viet:
--     - 7 row co options la MANG JSON >=2 phan tu
--     - correct_answer TRUNG KHIT mot lua chon
--     - tieu de lesson la "Reading - VOCABULARY", "Writing - READING", ...
--     - UI hien nhan "NGHE" + nut "🔊 Nghe" doc to cau lenh
--   Phep thu bat chuoc client (prove-mc-reclassify-works.js): 7/7 cham DUNG
--   khi bam lua chon. Tuc DU LIEU da dung, chi NHAN sai.
--
-- KHONG backfill audio: sinh TTS se doc to chinh cau lenh
-- ("Write a description of your new home"), lam loi tro nhu da sua xong.
--
-- 2 row 745673 va 745808 KHONG nam trong danh sach nay:
--   - 745673: options la chuoi van ban, khong chua lua chon nao
--   - 745808: options dang "A) ...B) ...", correct_answer = "A"
--   => can quyet dinh noi dung, khong sua tu dong.
--
-- AN TOAN:
--   - Backup: engflow_2026-09-20-pre-listening-fix.bak, RESTORE VERIFYONLY = valid
--   - Danh sach ID CU THE, khong dung LIKE
--   - SET QUOTED_IDENTIFIER ON
--   - Doi NHAN khong tao/xoa row -> parity KHONG doi
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

PRINT '=== TRUOC: 7 row muc tieu ===';
SELECT exercise_id, lesson_id, exercise_type,
       ISNULL(LEFT(CAST(question AS varchar(60)), 60), '') AS question
FROM exercises
WHERE exercise_id IN (745643, 745708, 745713, 745718, 745748, 745878, 745904)
ORDER BY exercise_id;

PRINT '=== TRUOC: so row LISTENING thieu audio (phai la 9) ===';
SELECT COUNT(*) AS listening_no_audio FROM exercises
WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '');

-- Doi nhan. Dieu kien `exercise_type = 'LISTENING'` de UPDATE idempotent:
-- chay lan hai se khong khop row nao, khong ghi de gi.
UPDATE exercises
SET exercise_type = 'MULTIPLE_CHOICE'
WHERE exercise_id IN (745643, 745708, 745713, 745718, 745748, 745878, 745904)
  AND exercise_type = 'LISTENING';

PRINT '=== so row da doi ===';
SELECT @@ROWCOUNT AS updated_rows;

PRINT '=== SAU: 7 row do phai la MULTIPLE_CHOICE ===';
SELECT exercise_id, exercise_type FROM exercises
WHERE exercise_id IN (745643, 745708, 745713, 745718, 745748, 745878, 745904)
ORDER BY exercise_id;

PRINT '=== SAU: so row LISTENING thieu audio (phai con 2) ===';
SELECT COUNT(*) AS listening_no_audio_left FROM exercises
WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '');

COMMIT TRANSACTION;

PRINT '=== PARITY (exercises phai VAN la 43737) ===';
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
