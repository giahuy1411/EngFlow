-- audit-v10 T1.10 — verify schema streak doc lap (khong tin deploy.sql tu bao)
-- Chay: docker exec engflow-sqlserver sh -c 'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -Q "..."'
-- File nay duoc truyen qua -i hoac copy vao container.

SET NOCOUNT ON;

PRINT '=== 1. TABLES ===';
SELECT name FROM sys.tables WHERE name IN ('study_policy','study_days') ORDER BY name;

PRINT '=== 2. study_policy CONTENT ===';
-- StudyPolicy entity chi khai 2 cot (id, effective_from) — xem
-- src/main/java/com/datn/engflow/model/entity/StudyPolicy.java. Khong SELECT
-- cot khac: Hibernate ddl-auto=update chi tao dung 2 cot do.
SELECT id, effective_from FROM study_policy;

PRINT '=== 3. study_policy ROW COUNT (phai = 1) ===';
SELECT COUNT(*) AS policy_rows FROM study_policy;

PRINT '=== 4. FOREIGN KEY study_days -> users ===';
SELECT fk.name AS fk_name,
       OBJECT_NAME(fk.parent_object_id) AS child_table,
       OBJECT_NAME(fk.referenced_object_id) AS parent_table
FROM sys.foreign_keys fk
WHERE OBJECT_NAME(fk.parent_object_id) = 'study_days';

PRINT '=== 5. UNIQUE INDEX tren study_days ===';
SELECT i.name AS index_name, i.is_unique, i.type_desc
FROM sys.indexes i
WHERE OBJECT_NAME(i.object_id) = 'study_days' AND i.is_unique = 1;

PRINT '=== 6. study_days ROW COUNT ===';
SELECT COUNT(*) AS study_day_rows FROM study_days;

PRINT '=== 7. COT cua study_days ===';
SELECT c.name AS col, t.name AS typ, c.is_nullable
FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
WHERE OBJECT_NAME(c.object_id) = 'study_days' ORDER BY c.column_id;
