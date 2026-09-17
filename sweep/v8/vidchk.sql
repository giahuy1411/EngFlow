SET NOCOUNT ON;
SELECT id, LEFT(title,28) t, level, is_published, youtube_video_id, transcript_len = LEN(CAST(transcript_json AS nvarchar(max))), created_at FROM video_lessons ORDER BY id DESC OFFSET 0 ROWS FETCH NEXT 8 ROWS ONLY;
SELECT zz_left = COUNT(*) FROM video_lessons WHERE title LIKE 'ZZ v8%';
SELECT total_videos = COUNT(*) FROM video_lessons;
