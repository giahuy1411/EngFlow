SET NOCOUNT ON;
SELECT execs = qs.execution_count, avg_ms = CAST(qs.total_elapsed_time/1000.0/NULLIF(qs.execution_count,0) AS numeric(10,1)),
       avg_logical = CAST(qs.total_logical_reads/1.0/NULLIF(qs.execution_count,0) AS numeric(12,0)),
       sql = SUBSTRING(st.text, CHARINDEX(N'select', st.text), 520)
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE N'from exercises e1_0'
ORDER BY avg_logical DESC;
