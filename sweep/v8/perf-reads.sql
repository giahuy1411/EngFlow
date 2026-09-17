-- perf-reads.sql — Task 9 evidence: logical reads / duration for the admin
-- exercise list query, the hot path F87 fixed. READ-ONLY.
-- Run AFTER exercising the endpoint so its plan is in the cache.

SET NOCOUNT ON;
GO

PRINT '=== admin exercise list query: logical reads + duration ===';
-- NOTE: total_lob_logical_reads does not exist on sys.dm_exec_query_stats in
-- this SQL Server 2019 build (Msg 207). Removed rather than left to error.
--
-- The audit's OWN statements land in the plan cache too and match '%exercise%',
-- so they must be excluded or the report shows the audit measuring itself.
-- The filters below drop: catalog probes, and the db-audit/perf-reads text.
SELECT TOP 5
  qs.execution_count                                   AS execs,
  qs.total_logical_reads / NULLIF(qs.execution_count,0) AS avg_logical_reads,
  qs.total_logical_reads                               AS total_logical_reads,
  qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 AS avg_ms,
  SUBSTRING(st.text, (qs.statement_start_offset/2)+1,
    ((CASE qs.statement_end_offset WHEN -1 THEN DATALENGTH(st.text)
      ELSE qs.statement_end_offset END - qs.statement_start_offset)/2)+1) AS query_text
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%exercise%'
  AND st.text NOT LIKE '%sys.%'
  AND st.text NOT LIKE '%dm_exec%'
  AND st.text NOT LIKE '%check_name%'
  AND st.text NOT LIKE '%content_original%'
  AND st.text NOT LIKE '%avg_logical_reads%'
ORDER BY qs.total_logical_reads DESC;
GO

PRINT '=== heavy-column read cost on the admin list shape (projection check) ===';
-- F87 replaced the JOIN FETCH of lesson.content + content_original with a flat
-- JOIN plus a batch projection. The app query is the one selecting exercise
-- columns joined to lesson columns; it must NOT include content_original.
SELECT TOP 3
  qs.execution_count AS execs,
  qs.total_logical_reads / NULLIF(qs.execution_count,0) AS avg_logical_reads,
  qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 AS avg_ms,
  CASE WHEN st.text LIKE '%content_original%' THEN 'STILL FETCHING content_original'
       ELSE 'projection only (F87 shape)' END AS shape,
  SUBSTRING(st.text, 1, 120) AS query_head
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%from exercises%'
  AND st.text NOT LIKE '%avg_logical_reads%'
  AND st.text NOT LIKE '%content_original%'
ORDER BY qs.execution_count DESC;
GO


