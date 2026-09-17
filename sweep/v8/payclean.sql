SET NOCOUNT ON;
SELECT before_total = COUNT(*) FROM payment_transactions;
DELETE FROM payment_transactions WHERE created_at >= '2026-09-14 11:00:00';
SELECT after_total = COUNT(*), oldest = MIN(created_at), newest = MAX(created_at) FROM payment_transactions;
SELECT d = CAST(created_at AS date), n = COUNT(*) FROM payment_transactions GROUP BY CAST(created_at AS date) ORDER BY d DESC;
