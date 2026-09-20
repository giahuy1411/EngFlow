SET NOCOUNT ON;
SELECT q.text, qs.execution_count
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS q
WHERE q.text LIKE '%study_days%'
AND q.text NOT LIKE '%sys.%'
AND q.text NOT LIKE '%dm_exec%';
