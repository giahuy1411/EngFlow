SET NOCOUNT ON;
SELECT n = COUNT(*), min_d = MIN(created_at), max_d = MAX(created_at) FROM payment_transactions;
SELECT TOP 12 id, order_code, amount, status, CAST(created_at AS varchar(19)) AS c FROM payment_transactions ORDER BY created_at DESC;
SELECT sfx = RIGHT(order_code,3), n = COUNT(*) FROM payment_transactions GROUP BY RIGHT(order_code,3) ORDER BY n DESC;