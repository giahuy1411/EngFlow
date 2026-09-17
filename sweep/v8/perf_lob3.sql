SET NOCOUNT ON;
CREATE TABLE #t (eid bigint, x nvarchar(max), y nvarchar(max));
DECLARE @ls bigint = 568;
SET STATISTICS IO, TIME ON;
INSERT INTO #t SELECT TOP 20 e1_0.exercise_id, l1_0.content, l1_0.content_original
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
WHERE l1_0.lesson_id = @ls ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
PRINT N'--- WITH LOBS done ---';
TRUNCATE TABLE #t;
SET STATISTICS IO, TIME ON;
INSERT INTO #t (eid) SELECT TOP 20 e1_0.exercise_id
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
WHERE l1_0.lesson_id = @ls ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
PRINT N'--- TITLE ONLY done ---';
DROP TABLE #t;
