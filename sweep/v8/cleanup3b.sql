SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET NOCOUNT ON;
DELETE FROM video_attempts WHERE id = 16;
DELETE FROM payment_transactions WHERE order_code = 'ENGB6355036DC65';
DELETE FROM lesson_submissions WHERE submission_text = 'zz audit probe';
DELETE FROM speaking_submissions WHERE id = 40024;
SELECT rest_attempt=(SELECT COUNT(*) FROM video_attempts WHERE id=16), rest_pay=(SELECT COUNT(*) FROM payment_transactions WHERE order_code='ENGB6355036DC65'), rest_ls=(SELECT COUNT(*) FROM lesson_submissions WHERE submission_text='zz audit probe'), rest_ss=(SELECT COUNT(*) FROM speaking_submissions WHERE id=40024);