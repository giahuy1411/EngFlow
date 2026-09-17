SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT minute_bucket = CAST(created_at AS varchar(16)), n = COUNT(*) FROM payment_transactions WHERE created_at >= '2026-09-13' GROUP BY CAST(created_at AS varchar(16)) ORDER BY 1;
