-- p6_db_audit_v9.sql — audit-v9 Task DB evidence: schema, integrity, indexes, hygiene.
-- READ-ONLY. No DML in this file. Every claim in db-audit.md must cite a block
-- below. Run via: python sweep/v8/sqlrun.py sweep/v8/db-audit.sql
--
-- Convention note (AGENTS.md): all datetime2 columns are naive VN (+07) written
-- by the JVM. SYSDATETIME() in this container returns UTC, so comparing it to a
-- business timestamp is off by 7h. We therefore only use SYSDATETIME() to label
-- the run, never to judge data.

SET NOCOUNT ON;
GO

PRINT '=== [1] TABLE + ROW INVENTORY (per-table COUNT, no join fan-out) ===';
SELECT t.name AS table_name, SUM(p.rows) AS row_count
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0,1)
GROUP BY t.name
HAVING SUM(p.rows) > 0
ORDER BY SUM(p.rows) DESC;
GO

PRINT '=== [2] ORPHAN / REFERENTIAL INTEGRITY (must all be 0) ===';
SELECT 'orphan exercises->lessons'      AS check_name, COUNT(*) AS violations FROM exercises e LEFT JOIN lessons l ON l.lesson_id = e.lesson_id WHERE l.lesson_id IS NULL
UNION ALL SELECT 'orphan lesson_sections->lessons', COUNT(*) FROM lesson_sections s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL
UNION ALL SELECT 'orphan lesson_submissions->lessons', COUNT(*) FROM lesson_submissions s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL
UNION ALL SELECT 'orphan lesson_snapshots->lessons', COUNT(*) FROM lesson_snapshots s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL
UNION ALL SELECT 'orphan uvp->vocabulary', COUNT(*) FROM user_vocabulary_progress p LEFT JOIN vocabulary v ON v.vocab_id = p.vocabulary_id WHERE v.vocab_id IS NULL
UNION ALL SELECT 'orphan uvp->users', COUNT(*) FROM user_vocabulary_progress p LEFT JOIN users u ON u.user_id = p.user_id WHERE u.user_id IS NULL;
-- NOTE: exercises has NO section_id column (verified against
-- INFORMATION_SCHEMA.COLUMNS 2026-09-16). The exercise->section link is via
-- lesson_id only, so there is no section-level orphan check to make.
GO

PRINT '=== [3] DISABLED FK CONSTRAINTS (a real FK should be enforced, not implied) ===';
SELECT fk.name AS fk_name, OBJECT_NAME(fk.parent_object_id) AS child_table, fk.is_disabled, fk.is_not_trusted
FROM sys.foreign_keys fk WHERE fk.is_disabled = 1 OR fk.is_not_trusted = 1;
GO

PRINT '=== [4] FILTERED INDEXES (these force SET QUOTED_IDENTIFIER ON for DELETE) ===';
SELECT OBJECT_NAME(i.object_id) AS table_name, i.name AS index_name, i.has_filter, i.filter_definition
FROM sys.indexes i WHERE i.has_filter = 1;
GO

PRINT '=== [5] INDEX USAGE (unused = candidate, NOT an instruction to drop) ===';
SELECT OBJECT_NAME(i.object_id) AS table_name, i.name AS index_name, i.type_desc,
       ISNULL(us.user_seeks,0) AS seeks, ISNULL(us.user_scans,0) AS scans,
       ISNULL(us.user_lookups,0) AS lookups, ISNULL(us.user_updates,0) AS updates
FROM sys.indexes i
LEFT JOIN sys.dm_db_index_usage_stats us
  ON us.object_id = i.object_id AND us.index_id = i.index_id AND us.database_id = DB_ID()
WHERE i.type_desc = 'NONCLUSTERED' AND OBJECTPROPERTY(i.object_id,'IsUserTable') = 1
  AND i.name IS NOT NULL
ORDER BY ISNULL(us.user_seeks,0) + ISNULL(us.user_scans,0) ASC;
GO

PRINT '=== [6] DUPLICATE BUSINESS KEYS ===';
SELECT 'duplicate user email' AS check_name, COUNT(*) AS dup_groups FROM (SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1) x
UNION ALL SELECT 'duplicate lesson title', COUNT(*) FROM (SELECT title FROM lessons GROUP BY title HAVING COUNT(*) > 1) x
UNION ALL SELECT 'duplicate vocab word', COUNT(*) FROM (SELECT word FROM vocabulary GROUP BY word HAVING COUNT(*) > 1) x;
GO

PRINT '=== [7] NULL/EMPTY VIOLATING APP ASSUMPTIONS ===';
SELECT 'exercises with empty correct_answer' AS check_name, COUNT(*) AS n FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = ''
UNION ALL SELECT 'exercises with null question', COUNT(*) FROM exercises WHERE question IS NULL OR LTRIM(RTRIM(question)) = ''
UNION ALL SELECT 'lessons with null content', COUNT(*) FROM lessons WHERE content IS NULL OR LTRIM(RTRIM(content)) = ''
UNION ALL SELECT 'lessons with null content_original (KEPT by design)', COUNT(*) FROM lessons WHERE content_original IS NULL
UNION ALL SELECT 'LISTENING exercises missing audio_url', COUNT(*) FROM exercises WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '')
UNION ALL SELECT 'users with null password_hash', COUNT(*) FROM users WHERE password_hash IS NULL OR LTRIM(RTRIM(password_hash)) = '';
GO

PRINT '=== [8] TIMESTAMP SANITY (naive VN; SYSDATETIME() is UTC so only for labelling) ===';
SELECT 'server UTC now' AS label, CONVERT(varchar(19), SYSDATETIME(), 120) AS value
UNION ALL SELECT 'max lessons.created_at', CONVERT(varchar(19), MAX(created_at), 120) FROM lessons
UNION ALL SELECT 'max exercises.created_at', CONVERT(varchar(19), MAX(created_at), 120) FROM exercises;
SELECT 'lessons created more than 1 day in the future (would mean a UTC write)' AS check_name, COUNT(*) AS n
FROM lessons WHERE created_at > DATEADD(day, 1, SYSDATETIME());
GO

PRINT '=== [9] AUDIT-LEFTOVER SCAN (no audit namespace rows may survive) ===';
SELECT 'lessons with ZZ/AUDIT prefix' AS check_name, COUNT(*) AS n FROM lessons WHERE title LIKE 'ZZ%' OR title LIKE 'audit[_]%'
UNION ALL SELECT 'users with audit prefix' , COUNT(*) FROM users WHERE email LIKE 'audit[_]%' OR email LIKE 'zz%'
UNION ALL SELECT 'decks with audit prefix', COUNT(*) FROM decks WHERE name LIKE 'ZZ%' OR name LIKE 'audit[_]%';
GO

PRINT '=== [10] TOP QUERIES BY LOGICAL READS (plan cache; empty after restart) ===';
SELECT TOP 8
  qs.execution_count AS execs,
  qs.total_logical_reads / NULLIF(qs.execution_count,0) AS avg_logical_reads,
  qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 AS avg_ms,
  SUBSTRING(st.text, (qs.statement_start_offset/2)+1,
    ((CASE qs.statement_end_offset WHEN -1 THEN DATALENGTH(st.text) ELSE qs.statement_end_offset END
      - qs.statement_start_offset)/2)+1) AS query_text
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text NOT LIKE '%sys.%'
ORDER BY qs.total_logical_reads / NULLIF(qs.execution_count,0) DESC;
GO

PRINT '=== [11] DB SIZE / FILE LAYOUT ===';
SELECT DB_NAME() AS db_name, name AS logical_name, type_desc,
       CAST(size * 8.0 / 1024 AS DECIMAL(10,1)) AS size_mb
FROM sys.database_files;
GO

PRINT '=== [12] DDL-AUTO SETTING IN EFFECT (Hibernate owns schema) ===';
SELECT 'ddl-auto is managed by application.properties, Flyway disabled' AS note;
GO

PRINT '=== [13] INDEX FRAGMENTATION (avg_fragmentation_in_percent > 30 = worth a rebuild) ===';
SELECT OBJECT_NAME(ips.object_id) AS table_name, i.name AS index_name,
       CAST(ips.avg_fragmentation_in_percent AS DECIMAL(5,1)) AS frag_pct, ips.page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips
JOIN sys.indexes i ON i.object_id = ips.object_id AND i.index_id = ips.index_id
WHERE ips.page_count > 100 AND ips.avg_fragmentation_in_percent > 10
ORDER BY ips.avg_fragmentation_in_percent DESC;
GO

PRINT '=== [14] TABLE SIZE / SPACE USED ===';
SELECT t.name AS table_name,
       SUM(p.rows) AS row_count,
       CAST(SUM(a.total_pages) * 8.0 / 1024 AS DECIMAL(10,1)) AS total_mb,
       CAST(SUM(a.used_pages) * 8.0 / 1024 AS DECIMAL(10,1)) AS used_mb
FROM sys.tables t
JOIN sys.indexes i ON i.object_id = t.object_id
JOIN sys.partitions p ON p.object_id = i.object_id AND p.index_id = i.index_id
JOIN sys.allocation_units a ON a.container_id = p.partition_id
WHERE a.type IN (1,3)
GROUP BY t.name
ORDER BY SUM(a.total_pages) DESC;
GO

PRINT '=== [15] PARTITIONING (expect: no partitioned tables) ===';
SELECT COUNT(*) AS partitioned_tables
FROM sys.partitions WHERE partition_number > 1;
GO

PRINT '=== [16] TRANSACTION LOG SPACE REUSE ===';
SELECT name AS logical_name, type_desc, CAST(size * 8.0 / 1024 AS DECIMAL(10,1)) AS size_mb,
       CAST(FILEPROPERTY(name,'SpaceUsed') * 8.0 / 1024 AS DECIMAL(10,1)) AS used_mb
FROM sys.database_files WHERE type_desc = 'LOG';
DBCC SQLPERF(LOGSPACE);
GO

PRINT '=== [17] LONGEST TEXT COLUMNS (NVARCHAR(MAX) payloads) ===';
SELECT 'lessons.content' AS col, MAX(DATALENGTH(content)) AS max_bytes FROM lessons
UNION ALL SELECT 'lessons.content_original', MAX(DATALENGTH(content_original)) FROM lessons
UNION ALL SELECT 'exercises.options', MAX(DATALENGTH(options)) FROM exercises
UNION ALL SELECT 'exercises.explanation', MAX(DATALENGTH(explanation)) FROM exercises;
GO

PRINT '=== [18] STATISTICS FRESHNESS (oldest first) ===';
SELECT TOP 8 OBJECT_NAME(s.object_id) AS table_name, s.name AS stat_name,
       CONVERT(varchar(19), sp.last_updated, 120) AS last_updated, sp.rows, sp.modification_counter
FROM sys.stats s
CROSS APPLY sys.dm_db_stats_properties(s.object_id, s.stats_id) sp
WHERE OBJECTPROPERTY(s.object_id,'IsUserTable') = 1
ORDER BY sp.last_updated ASC;
GO
