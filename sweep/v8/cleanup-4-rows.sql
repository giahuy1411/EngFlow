SET QUOTED_IDENTIFIER ON;
DELETE FROM payment_transactions
WHERE CAST(created_at AS date) = '2026-09-17'
  AND status <> 'SUCCESS'
  AND transaction_id IS NULL;
SELECT 'AUDIT_CLEAN_TOTAL=' + CAST(COUNT(*) AS varchar(20)) FROM payment_transactions;