SET NOCOUNT ON;
SELECT (SELECT COUNT(*) FROM lessons) AS lessons,
       (SELECT COUNT(*) FROM exercises) AS exercises,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM vocabulary) AS vocabulary,
       (SELECT COUNT(*) FROM speaking_submissions) AS speaking,
       (SELECT COUNT(*) FROM video_attempts) AS video,
       (SELECT COUNT(*) FROM lesson_submissions) AS lesson_sub,
       (SELECT COUNT(*) FROM payment_transactions) AS payments,
       (SELECT COUNT(*) FROM decks) AS decks,
       (SELECT COUNT(*) FROM lesson_snapshots) AS snapshots;
GO
