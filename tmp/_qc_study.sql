-- /tmp/_qc_study.sql: đếm query study_days cho N+1 measurement
SET NOCOUNT ON;
SELECT 'study_days_q' AS t, CAST(ISNULL(SUM(execution_count),0) AS varchar(20)) AS v
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%study_days%';
