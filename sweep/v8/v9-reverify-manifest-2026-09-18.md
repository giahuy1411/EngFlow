# audit-v9 re-verify manifest 2026-09-18

Run ID: `v9-reverify-2026-09-18`

## Allowed actions

- Rebuild and restart `engflow-backend` from the current restored working tree.
- Backup `english_learning` before any DML.
- Delete only the verified fixture `zzverify106@example.com` and its dependent rows.

## Forbidden

- Do not delete by broad `zz%` or audit-prefix patterns.
- Do not mutate legacy audit users, backup tables, empty answers, duplicate content, or indexes.
- Do not send SRS review requests against real learner data.

## Expected cleanup evidence

- Before: `users=77`, `zzverify106@example.com=1`.
- After: `users=76`, `zzverify106@example.com=0`.
- Markers required: `AUDIT_CLEAN_TOTAL=<n>` and `AUDIT_CLEAN_RESIDUE=0`.
- Lessons, exercises, speaking submissions, and payment counts must remain unchanged.
