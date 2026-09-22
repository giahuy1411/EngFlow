
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT 'DEEP_LESSONS=' + CAST((SELECT COUNT(*) FROM lessons WHERE title LIKE 'AUDIT-V13-MC-%') AS varchar(10))
     + ' DEEP_DECKS=' + CAST((SELECT COUNT(*) FROM decks WHERE name LIKE 'AUDIT-V13-DEEP-%') AS varchar(10))
     + ' DEEP_EX=' + CAST((SELECT COUNT(*) FROM exercises WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'AUDIT-V13-MC-%')) AS varchar(10)) AS marker;
