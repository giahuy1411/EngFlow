SET NOCOUNT ON;
DELETE FROM speaking_submissions WHERE id=40024;
DELETE FROM video_attempts WHERE id=16;
DELETE FROM lesson_submissions WHERE submission_id=8;
DELETE FROM payment_transactions WHERE order_code='ENGB6355036DC65';
SELECT ss=(SELECT COUNT(*) FROM speaking_submissions), va=(SELECT COUNT(*) FROM video_attempts), ls=(SELECT COUNT(*) FROM lesson_submissions), zzl=(SELECT COUNT(*) FROM lessons WHERE title LIKE 'ZZ v8%'), zxp=(SELECT COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id=e.lesson_id WHERE l.lesson_id IS NULL);