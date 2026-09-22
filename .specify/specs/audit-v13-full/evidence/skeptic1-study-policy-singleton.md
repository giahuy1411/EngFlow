# Skeptic #1 — review of claim: study_policy singleton CHECK absent from live DB

Verdict: **NOT REFUTED (claim confirmed)**. Severity: claim says HIGH; honest severity is **MEDIUM** (see "Severity" below).

## 1. Source reading (confirmed)

`tasks/streak-study/deploy.sql:27-32` — the CHECK is inside the table-existence guard:

```sql
IF OBJECT_ID(N'dbo.study_policy', N'U') IS NULL
    CREATE TABLE dbo.study_policy (
        id int NOT NULL PRIMARY KEY,
        effective_from date NOT NULL,
        CONSTRAINT ck_study_policy_singleton CHECK (id = 1)   -- line 31
    );
```

`study_days` immediately below (lines 55-81) splits table / unique / FK into three independent
guards, with an explicit comment (lines 40-54) describing this exact failure class as F124.
The `study_policy` CHECK was left inside the table guard — no guarded `ALTER TABLE` follows it.
`grep -rn ck_study_policy_singleton` shows it appears **only** at deploy.sql:31 (plus audit docs);
it is never created anywhere else.

`StudyPolicy.java:13-24` — `@Entity @Table(name="study_policy")`, so Hibernate `ddl-auto=update`
creates the table before deploy.sql runs → guard false → CHECK never created.

## 2. Live DB proof (real rows, reproducible)

```
CHECK_NAME=... (8 rows, all on exercises/lesson_blocks/lesson_submissions/lessons/
             video_lessons/speaking_prompts/speaking_submissions)
STUDY_POLICY_CHECK_COUNT=0
STUDY_POLICY_ROWS=1
ID=1|EFF=2026-09-20
```

`sys.objects` for parent_object_id = study_policy: only `PRIMARY_KEY_CONSTRAINT`
(PK__study_po__3213E83F657CA4B3, clustered unique). `trigger_count=0`, `fk_count=0`.
No alternate enforcement path exists.

## 3. Enforceability test (rolled-back txn — real, reproduced)

```sql
BEGIN TRANSACTION;
INSERT INTO dbo.study_policy (id, effective_from) VALUES (2, '2020-01-01');  -- SUCCEEDED
-- ROWS_IN_TXN=2 :  ID=1|EFF=2026-09-20  and  ID=2|EFF=2020-01-01
ROLLBACK TRANSACTION;
-- ROWS_AFTER_ROLLBACK=1
```

A second policy row is accepted by the live DB. DB left unchanged (1 row). This is real, not a
probe artefact — no rate limiting involved, no collation error, no guard redirect.

## 4. Impact bound (skeptic angle — verified, does not refute)

Every consumer reads id=1 explicitly:
- `StudyActivityService.java:175` → `policies.findById(1)` (only production read).
- `StudyPolicyRepository` declares no finder for any other id.
- `sys.foreign_keys` referencing study_policy = 0, so no FK can resolve a wrong policy.

Consequence: a rogue `id=2` row has **no observable runtime effect** today; the app's invariant is
maintained by code discipline, not by the DB. So the missing CHECK is a pure defense-in-depth gap.

## 5. Severity

- The finding is real, the mechanics are exactly as claimed, and the proposed fix (one guarded
  `ALTER TABLE ADD CONSTRAINT ck_study_policy_singleton CHECK (id = 1)`, mirroring study_days)
  is correct and idempotent.
- HIGH overstates the impact: the sole consumer hard-codes id=1, no FK references the table, and no
  exploit path was demonstrated. The harm is schema-vs-intent divergence in a deployment script,
  not a live integrity failure.
- **Corrected severity: MEDIUM** — a genuine data-layer/defense-in-depth defect, and a recurrence
  of the F124 class the team already chose to fix for the sibling table.

## 6. Repro commands

```
python sweep/v8/sqlrun.py tmp/skeptic1_check.sql     # check inventory + rows
python sweep/v8/sqlrun.py tmp/skeptic1_insert.sql    # INSERT id=2, rollback
python sweep/v8/sqlrun.py tmp/skeptic1_allcons.sql   # sys.objects on study_policy
```
