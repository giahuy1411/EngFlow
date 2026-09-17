SET NOCOUNT ON;
SELECT TOP 10 execs = qs.execution_count,
 avg_ms = CAST(qs.total_elapsed_time/1000.0/NULLIF(qs.execution_count,0) AS numeric(10,1)),
 avg_logical = CAST(qs.total_logical_reads/1.0/NULLIF(qs.execution_count,0) AS numeric(12,0)),
 head = SUBSTRING(st.text, 1, 130)
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
ORDER BY qs.total_logical_reads/1.0/NULLIF(qs.execution_count,0) DESC;