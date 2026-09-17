SET NOCOUNT ON;
SELECT execs = qs.execution_count, avg_ms = CAST(qs.total_elapsed_time/1000.0/NULLIF(qs.execution_count,0) AS numeric(10,1)),
       avg_reads = CAST(qs.total_logical_reads/1.0/NULLIF(qs.execution_count,0) AS numeric(12,0)),
       avg_lob = CAST(qs.total_physical_reads/1.0/NULLIF(qs.execution_count,0) AS numeric(10,1)),
       head = SUBSTRING(st.text, 1, 60)
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE N'exercises e1_0' AND st.text LIKE N'join lessons';
