SET NOCOUNT ON;
PRINT N'=== A: join with all lesson columns (current shape) ===';
SET STATISTICS IO, TIME ON;
SELECT TOP 20 e1_0.exercise_id, l1_0.lesson_id, l1_0.title, l1_0.audio_url, l1_0.category, l1_0.content,
       l1_0.content_original, l1_0.created_at, l1_0.description, l1_0.duration_minutes, l1_0.is_published,
       l1_0.level, l1_0.order_index, l1_0.skill_type, l1_0.thumbnail_url, l1_0.updated_at, e1_0.question
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
PRINT N'=== B: same join but only lesson title/id ===';
SET STATISTICS IO, TIME ON;
SELECT TOP 20 e1_0.exercise_id, l1_0.lesson_id, l1_0.title, e1_0.question
FROM exercises e1_0 JOIN lessons l1_0 ON l1_0.lesson_id = e1_0.lesson_id
ORDER BY e1_0.order_index, e1_0.exercise_id;
SET STATISTICS IO, TIME OFF;
