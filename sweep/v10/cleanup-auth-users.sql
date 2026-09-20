-- Don user kich ban do deep-auth.js tao ra.
-- Danh sach ID CU THE, khong dung LIKE 'zz%' (bai hoc F111).
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

PRINT '=== user kich ban con lai ===';
SELECT user_id, username, email FROM users WHERE email LIKE 'zzauth%@example.com' ORDER BY user_id;

DECLARE @ids TABLE (id bigint);
INSERT INTO @ids (id) SELECT user_id FROM users WHERE email LIKE 'zzauth%@example.com';

DELETE p FROM user_vocabulary_progress p JOIN @ids i ON p.user_id = i.id;
DELETE d FROM study_days d JOIN @ids i ON d.user_id = i.id;
DELETE u FROM users u JOIN @ids i ON u.user_id = i.id;

PRINT '=== SAU: phai con 0 ===';
SELECT COUNT(*) AS remaining FROM users WHERE email LIKE 'zzauth%@example.com';

PRINT '=== TONG users (baseline moi la 72) ===';
SELECT COUNT(*) AS total_users FROM users;
