SET NOCOUNT ON;
SELECT TOP 12
       reads = qs.total_logical_reads, n = qs.execution_count,
       per_exec = CAST(qs.total_logical_reads * 1.0 / qs.execution_count AS numeric(12,1)),
       txt = SUBSTRING(ST.REPLACE(DB.text, CHAR(10), N' '), 1, 110)
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) DB
CROSS APPLY (SELECT REPLACE = REPLACE(REPLACE(DB.text, CHAR(9), N' '), CHAR(13), N' ')) ST
WHERE DB.dbid = DB_ID()
ORDER BY qs.total_logical_reads DESC;
