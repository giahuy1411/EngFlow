-- Don user rac do lan chay streak-scenarios.js bi loi de lai.
--
-- BOI CANH: lan chay dau tien dung ham sql() sai (JSON.stringify bien newline
-- thanh \n literal -> Msg 102), nen cleanup KHONG chay. Dong thoi ham sqlNum()
-- bat so dau tien trong output sqlcmd — ke ca ma loi — nen no tuong "user id
-- = 207" trong khi 207 la ma cua Msg 207.
--
-- PHAI liet ke ID CU THE. Bai hoc F111 (v9): mot filter `email LIKE 'zz%'`
-- da xoa 4 user that. O day ta SELECT truoc, roi DELETE dung danh sach do.

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

PRINT '=== TRUOC: cac user khop mau ten rac ===';
SELECT user_id, username, email, created_at
FROM users
WHERE email LIKE 'zzauditv10%' OR username LIKE 'zzauditv10%'
ORDER BY user_id;

PRINT '=== chi xoa nhung user co email khop CHINH XAC mau da sinh ===';
DECLARE @ids TABLE (id BIGINT);
INSERT INTO @ids (id)
SELECT user_id FROM users
WHERE email LIKE 'zzauditv10%@example.com'
  AND email NOT IN ('user@gmail.com','admin@gmail.com');

PRINT '=== so luong se xoa ===';
SELECT COUNT(*) AS to_delete FROM @ids;

-- Xoa con truoc, cha sau (FK)
DELETE p FROM user_vocabulary_progress p JOIN @ids i ON p.user_id = i.id;
PRINT '=== user_vocabulary_progress da xoa ===';
SELECT @@ROWCOUNT AS deleted_progress;

DELETE d FROM study_days d JOIN @ids i ON d.user_id = i.id;
PRINT '=== study_days da xoa ===';
SELECT @@ROWCOUNT AS deleted_days;

DELETE u FROM users u JOIN @ids i ON u.user_id = i.id;
PRINT '=== users da xoa ===';
SELECT @@ROWCOUNT AS deleted_users;

PRINT '=== SAU: phai con 0 ===';
SELECT COUNT(*) AS remaining
FROM users
WHERE email LIKE 'zzauditv10%@example.com';

PRINT '=== TONG users (baseline la 76) ===';
SELECT COUNT(*) AS total_users FROM users;
