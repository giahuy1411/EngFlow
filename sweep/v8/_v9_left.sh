SET QUOTED_IDENTIFIER ON;
SELECT 'exercises' k, COUNT(*) v FROM exercises WHERE lesson_id = 102049
UNION ALL SELECT 'sections', COUNT(*) FROM lesson_sections WHERE lesson_id = 102049
UNION ALL SELECT 'submissions', COUNT(*) FROM lesson_submissions WHERE lesson_id = 102049
UNION ALL SELECT 'snapshots', COUNT(*) FROM lesson_snapshots WHERE lesson_id = 102049
UNION ALL SELECT 'vocab', COUNT(*) FROM vocabulary WHERE lesson_id = 102049
UNION ALL SELECT 'video', COUNT(*) FROM video_lessons WHERE lesson_id = 102049;
SELECT vocab_id, word, lesson_id FROM vocabulary WHERE vocab_id = 50261;
GO