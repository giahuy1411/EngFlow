SET NOCOUNT ON;
DECLARE @ls bigint = (SELECT MIN(lesson_id) FROM lessons WHERE lesson_id IN (SELECT lesson_id FROM exercises GROUP BY lesson_id HAVING COUNT(*) > 40));
PRINT N'lesson ' + CAST(@ls AS nvarchar(20));
SET STATISTICS IO, TIME ON;
SELECT TOP 20 e1_0.exercise_id, l1_0.title, l1_0.content, l1_0.content_original
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
WHERE l1_0.lesson_id = @ls ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
SET STATISTICS IO, TIME ON;
SELECT TOP 20 e1_0.exercise_id, l1_0.title
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
WHERE l1_0.lesson_id = @ls ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
