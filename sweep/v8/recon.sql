SET NOCOUNT ON;
SELECT total_lessons = COUNT(*), published = SUM(CASE WHEN is_published=1 THEN 1 ELSE 0 END) FROM lessons;
SELECT vocab_total = COUNT(*), by_day = CAST(created_at AS date), n = COUNT(*) FROM vocabulary GROUP BY CAST(created_at AS date) ORDER BY by_day DESC;
SELECT TOP 5 vocab_id, word, CAST(created_at AS varchar(19)) AS c FROM vocabulary ORDER BY vocab_id DESC;
