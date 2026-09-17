SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
DELETE FROM payment_transactions WHERE created_at >= '2026-09-14 00:00:00';
SELECT total = COUNT(*), oldest = CAST(MIN(created_at) AS varchar(19)), newest = CAST(MAX(created_at) AS varchar(19)) FROM payment_transactions;
