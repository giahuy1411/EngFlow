-- p16-clean-payments.sql — remove the payment_transactions rows created by the
-- UI sweeps that mounted /premium (PremiumCheckout.vue calls
-- POST /api/v1/payment/create-order on mount to build the QR).
--
-- Documented in AGENTS.md: running ui/design.js + ui/routes.js moves parity
-- 126 -> 128. This run ran design.js, design-v2.js AND routes-all.js, so the
-- delta is larger. Baseline is 126.
--
-- Scope: created_at within today (naive VN wall clock, matching how the app
-- writes it) AND the order has no successful payment behind it. Real orders a
-- user paid for are never touched.
SET QUOTED_IDENTIFIER ON;
GO

PRINT '--- BEFORE: rows created today ---';
SELECT transaction_id, user_id, amount, status, created_at
FROM payment_transactions
WHERE created_at >= '2026-09-16' AND created_at < '2026-09-17'
ORDER BY created_at;
GO

PRINT '--- BEFORE: total ---';
SELECT COUNT(*) AS total_before FROM payment_transactions;
GO

DELETE FROM payment_transactions
WHERE created_at >= '2026-09-16' AND created_at < '2026-09-17'
  AND status <> 'SUCCESS';
GO

PRINT '--- AFTER: total (baseline 126) ---';
SELECT COUNT(*) AS total_after FROM payment_transactions;
GO

PRINT '--- AFTER: any rows left from today ---';
SELECT transaction_id, status, created_at
FROM payment_transactions
WHERE created_at >= '2026-09-16' AND created_at < '2026-09-17'
ORDER BY created_at;
GO
