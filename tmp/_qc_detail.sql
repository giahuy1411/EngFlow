-- Đếm riêng từng loại query để hiểu +3
SET NOCOUNT ON;
SELECT 'findDatesForUsers_batch' AS t, CAST(ISNULL(SUM(execution_count),0) AS varchar(20)) AS v
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%findDatesForUsers%' OR st.text LIKE '%IN :userIds%';
SELECT 'findDates_single' AS t, CAST(ISNULL(SUM(execution_count),0) AS varchar(20)) AS v
FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.text LIKE '%d.userId = :userId%' OR (st.text LIKE '%study_days%' AND st.text LIKE '%:userId%');
