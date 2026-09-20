-- audit-v11 — revert the state the SePay webhook test changed.
--
-- The test did two REAL things that must be undone:
--   1. created a payment row (order ENG28DE407E2FDF) and settled it to SUCCESS
--   2. activated premium on user_id=2, moving premium_expiry 2026-10-03 -> 2026-10-21
--
-- Both are reverted to the values captured BEFORE the test in
-- sweep/v11/_premium-before.txt:  USERID=2 PREMIUM=1 EXPIRY=2026-10-03
--
-- SET QUOTED_IDENTIFIER ON is required: payment_transactions carries a filtered unique
-- index, and DELETE fails Msg 1934 without it while sqlcmd still exits 0.
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT OFF;

PRINT '--- before ---';
SELECT COUNT(*) AS payments_before FROM payment_transactions;
SELECT 'user2 premium=' + CAST(is_premium AS varchar(5)) + ' expiry=' + ISNULL(CONVERT(varchar(30), premium_expiry, 120), 'NULL')
FROM users WHERE user_id = 2;

-- 1. remove ONLY the row this test created, matched by its transaction id prefix
DELETE FROM payment_transactions WHERE transaction_id LIKE 'AUDITV11%';

-- 2. restore the premium expiry captured before the test
UPDATE users SET is_premium = 1, premium_expiry = '2026-10-03' WHERE user_id = 2;

PRINT '--- after ---';
SELECT COUNT(*) AS payments_after FROM payment_transactions;
SELECT 'user2 premium=' + CAST(is_premium AS varchar(5)) + ' expiry=' + ISNULL(CONVERT(varchar(30), premium_expiry, 120), 'NULL')
FROM users WHERE user_id = 2;
