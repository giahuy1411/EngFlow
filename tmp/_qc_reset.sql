-- Xoá plan cache để đếm sạch
DBCC FREEPROCCACHE;
SET NOCOUNT ON;
SELECT 'baseline' AS t, CAST(ISNULL(SUM(execution_count),0) AS varchar(20)) AS v
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%study_days%';
