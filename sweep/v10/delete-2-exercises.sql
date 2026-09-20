-- audit-v10 F128 — XOA 2 row exercise con lai (phuong an 3, chu du an chon).
--
-- 2 row nay mang nhan LISTENING sai va KHONG THE SUA TU DONG:
--   745673 (lesson 11477): options la CHUOI VAN BAN "These Terms are for the
--           guidance of our Service...", khong chua lua chon nao. correct_answer
--           cung la mot doan van. Khong co gi de parse -> phai TAO du lieu moi.
--   745808 (lesson 11539): options dang "A) ...B) ...C) ...D) ...",
--           correct_answer = "A". Regex tach chi duoc 2/4 do dau '.' giua cac
--           lua chon gay nham lan.
--
-- Ca hai van hien nut "🔊 Nghe" doc to chinh cau lenh:
--   "Please read these Terms and Conditions..."
--   "Listen to the following text and identify the main idea."
--
-- AN TOAN DA CHUNG MINH TRUOC KHI CHAY (sweep/v10/pre-delete-2-exercises.js
-- va check-attempt-details.js):
--   - KHONG co FK nao tro toi dbo.exercises
--   - exercise_attempts chi luu lesson_id, KHONG luu exercise_id
--   - details (JSON) CO chua exerciseId o 36 row, nhung KHONG row nao nhac
--     toi 745673 hay 745808
--   - 0 attempt thuoc lesson 11477 hoac 11539
--   - backup: engflow_2026-09-20-pre-delete-2-exercises.bak, VERIFYONLY = valid
--
-- HAU QUA PHAI CHAP NHAN: moi lesson con 4 exercise thay vi 5, lech cau truc
-- so voi 1.459 lesson khac. Do la danh doi da duoc chu du an dong y.
--
-- PARITY SE DOI: exercises 43737 -> 43735.
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

PRINT '=== TRUOC: 2 row muc tieu ===';
SELECT exercise_id, lesson_id, exercise_type,
       ISNULL(LEFT(CAST(question AS varchar(70)), 70), '') AS question
FROM exercises WHERE exercise_id IN (745673, 745808) ORDER BY exercise_id;

PRINT '=== TRUOC: so exercise cua 2 lesson ===';
SELECT lesson_id, COUNT(*) AS so_exercise FROM exercises
WHERE lesson_id IN (11477, 11539) GROUP BY lesson_id ORDER BY lesson_id;

-- Xoa theo ID CU THE. Dieu kien `exercise_type = 'LISTENING'` de idempotent:
-- chay lan hai se khong khop row nao.
DELETE FROM exercises
WHERE exercise_id IN (745673, 745808)
  AND exercise_type = 'LISTENING';

PRINT '=== SAU: 2 row phai BIEN MAT ===';
SELECT COUNT(*) AS con_lai FROM exercises WHERE exercise_id IN (745673, 745808);

PRINT '=== SAU: so exercise cua 2 lesson (phai la 4) ===';
SELECT lesson_id, COUNT(*) AS so_exercise FROM exercises
WHERE lesson_id IN (11477, 11539) GROUP BY lesson_id ORDER BY lesson_id;

PRINT '=== SAU: LISTENING thieu audio (phai la 0) ===';
SELECT COUNT(*) AS listening_no_audio FROM exercises
WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '');

COMMIT TRANSACTION;

PRINT '=== PARITY SAU KHI XOA (exercises phai la 43735) ===';
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

PRINT '=== exercises theo type, SAU ===';
SELECT exercise_type, COUNT(*) AS n FROM exercises GROUP BY exercise_type ORDER BY exercise_type;
