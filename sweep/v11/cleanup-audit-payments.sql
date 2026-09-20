-- audit-v11-full — remove payment rows created by the audit sweeps themselves.
-- These are audit residue, NOT product data. Both are PENDING with a NULL transaction_id,
-- which is the exact signature of PremiumCheckout.vue's mount-time create-order call.
-- SET QUOTED_IDENTIFIER ON is REQUIRED: payment_transactions carries a filtered unique index
-- (UKlsp8jh693lih2txq7dl4bdnpx) and DELETE fails Msg 1934 without it.
-- sqlcmd exits 0 even when this batch fails -> caller must scan for `Msg \d+`.
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT OFF;

PRINT '--- before ---';
SELECT COUNT(*) AS payments_before FROM payment_transactions;

DELETE FROM payment_transactions
WHERE status = 'PENDING'
  AND transaction_id IS NULL
  AND created_at >= '2026-09-20 00:00:00';

PRINT '--- after ---';
SELECT COUNT(*) AS payments_after FROM payment_transactions;
