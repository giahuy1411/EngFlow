-- audit-v10 Phase 3.1 — DO LAI ca 4 backlog item truoc khi lam gi.
-- Read-only. Khong xoa, khong sua.
SET NOCOUNT ON;

PRINT '=== BL1: bang backup exercises_bak_v5* ===';
SELECT name, create_date FROM sys.tables WHERE name LIKE 'exercises_bak_v5%' ORDER BY name;

PRINT '=== BL2: user kieu zz* ===';
-- Liet ke CU THE, khong dung LIKE de xoa. Day chi la do.
SELECT user_id, username, email, created_at FROM users
WHERE email LIKE 'zz%' OR username LIKE 'zz%'
ORDER BY user_id;

PRINT '=== BL3: exercise co correct_answer rong ===';
SELECT COUNT(*) AS empty_answer FROM exercises
WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = '';

PRINT '=== BL3b: phan bo theo type ===';
SELECT exercise_type, COUNT(*) AS n FROM exercises
WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = ''
GROUP BY exercise_type ORDER BY n DESC;

PRINT '=== BL4: LISTENING thieu audio_url ===';
SELECT COUNT(*) AS listening_no_audio FROM exercises
WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '');

PRINT '=== BL4b: tong LISTENING de so sanh ===';
SELECT COUNT(*) AS total_listening FROM exercises WHERE exercise_type = 'LISTENING';

PRINT '=== BL5: kich thuoc cac bang backup (neu con) ===';
SELECT t.name AS table_name, p.rows AS row_count
FROM sys.tables t
JOIN sys.partitions p ON t.object_id = p.object_id AND p.index_id IN (0,1)
WHERE t.name LIKE 'exercises_bak_v5%' OR t.name LIKE '%_bak_%'
ORDER BY t.name;
