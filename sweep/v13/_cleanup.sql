
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT 'AUDIT_PAY_CANDIDATES=' + CAST(COUNT(*) AS varchar(10)) AS marker FROM payment_transactions
 WHERE transaction_id IS NULL AND status='PENDING' AND created_at >= CONVERT(date, '2026-09-21');
DELETE FROM deck_words WHERE deck_id IN (SELECT deck_id FROM decks WHERE name LIKE 'AUDIT-V13-API-%');
DELETE FROM decks WHERE name LIKE 'AUDIT-V13-API-%';
DELETE FROM deck_words WHERE vocab_id IN (SELECT vocab_id FROM vocabulary WHERE word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word = 'zzv12probe');
DELETE FROM vocabulary WHERE word LIKE 'zzv12authshape%' OR word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word = 'zzv12probe';
DELETE FROM payment_transactions
 WHERE transaction_id IS NULL AND status = 'PENDING'
   AND (order_code = 'ENGF8192C0BE1E8' OR created_at >= CONVERT(date, '2026-09-21'));
SELECT 'AUDIT_DECKS=' + CAST((SELECT COUNT(*) FROM decks WHERE name LIKE 'AUDIT-V13-API-%') AS varchar(10))
     + ' AUDIT_VOCAB=' + CAST((SELECT COUNT(*) FROM vocabulary WHERE word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word='zzv12probe') AS varchar(10))
     + ' AUDIT_LESSONS=' + CAST((SELECT COUNT(*) FROM lessons WHERE title LIKE 'AUDIT-V13-%') AS varchar(10))
     + ' AUDIT_PAY=' + CAST((SELECT COUNT(*) FROM payment_transactions WHERE transaction_id IS NULL AND status='PENDING' AND created_at >= CONVERT(date, '2026-09-21')) AS varchar(10)) AS marker;
