# audit-v13 · Phase 1 — SQL Server container audit (data layer, read-only)

Run: 2026-09-22 ~03:05–03:15 (+07) / 2026-09-21 ~20:05–20:15 UTC
Target: container `engflow-sqlserver` (mcr.microsoft.com/mssql/server:2019-latest), db `english_learning`
Method: `python sweep/v8/sqlrun.py <file.sql>` (sqlcmd inside the container) + `tmp/v13/run.py` for
unfiltered IO capture. All numbers below are from live queries, not estimates.

No application source was modified. Scratch DB was created and dropped. One write-probe
(a deck) was created through the real API and deleted by its listed id.

---

## 1. Parity re-measurement

`python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql`

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1470|43735|72|118|29|15|4|126|10|5
```

**Matches the stated baseline `1470|43735|72|118|29|15|4|126|10|5` exactly — all 10 numbers.**

### Transient drift from concurrent audit phases (not caused by this audit)

Two other audit phases were writing to the same live database during this run. Neither write
is mine; my only write was deck `50106`, created and deleted by its listed id.

**Instance 1 — F13 multiple-choice phase.** At 03:09:54 the counts read `1471|43739`.
Investigation (`tmp/v13/q49_delta.sql`, `tmp/v13/q50_audit.sql`) showed lesson `103580`
"`AUDIT-V13-MC-10006`" plus 4 exercises (`777493`–`777496`). This is the F13 work visible in
the working tree (`ExerciseService.java` +61, `LessonExerciseTab.vue` +69, plus the two new
untracked `*.f13.test.js` / `ExerciseServiceMultipleChoiceOptionsTest.java` files). It cleaned
up after itself — re-read at 03:10 returned the baseline exactly:

```
audit_lesson_left 0    probe_deck_left 0    lessons_total 1470    exercises_total 43735
```

**Instance 2 — payment-flow phase.** A final confirmation read at 03:12 returned
`…|payments 128|…` (baseline 126). `tmp/v13/q61_pay.sql` shows two new `PENDING` rows:

```
80274 | NULL | user 2 | PENDING | 2026-09-22 03:11:08
80275 | NULL | user 3 | PENDING | 2026-09-22 03:11:11
```

These are in-flight checkout sessions from a payment-flow phase — live evidence of a
concurrent writer, not a regression.

**Bearing on this report:** the parity check **passed at baseline on the first read (03:05)
and again after the F13 cleanup (03:10)**. Both later movements are attributable to identified
concurrent phases. All constraint, orphan, streak, index and timezone measurements were taken
between 03:05 and 03:10 and are unaffected. The one row-count that appears in the index
section is 43,735 (baseline).

---

## 2. Constraint EFFECTIVENESS (not existence)

Two independent proofs.

### 2a. Scratch database — created, tested, dropped

Created `engflow_audit_scratch`, built a parent/child pair mirroring the production shapes
(FK, UNIQUE constraint, unique index, filtered unique index, composite filtered unique index),
then attempted violations.

> **Operational note:** the filtered index would not create until `SET QUOTED_IDENTIFIER ON`
> was added to the batch — `Msg 1934 ... CREATE INDEX failed because the following SET options
> have incorrect settings: 'QUOTED_IDENTIFIER'`. The AGENTS.md rule about
> `SET QUOTED_IDENTIFIER ON` for DELETE batches applies equally to creating filtered indexes.

| # | Attempt | Expected | Result |
|---|---|---|---|
| 1 | `child.parent_id=999` (no such parent) | blocked | **Msg 547** — FOREIGN KEY constraint `fk_child_parent` |
| 2 | `parent.code='A'` twice (UNIQUE constraint) | blocked | **Msg 2627** — UNIQUE KEY constraint `uq_parent_code` |
| 3a | two rows with `txn_id IS NULL` under a filtered unique index | allowed | **2 rows inserted** — NULLs correctly excluded from the filter |
| 3b | two rows with `txn_id='TXN-1'` | blocked | **Msg 2601** — unique index `ux_child_txn_filtered` |
| 4 | `parent_id=NULL` into a NOT NULL FK column | blocked | **Msg 515** — column does not allow nulls |
| 5 | composite filtered unique `(user_id,lesson_id)=(1,10)` twice | blocked | **Msg 2601** — `UK_prog` |
| 6 | `(1,11)` — distinct, same user | allowed | **2 rows** |

Drop confirmed: `SELECT DB_ID('engflow_audit_scratch')` → `NULL`.

### 2b. The ACTUAL production constraints, inside a rolled-back transaction

`tmp/v13/q16_realviolations.sql` — real tables, real rows, `BEGIN TRAN … ROLLBACK`.

| Prod table / constraint | Attempt | Result |
|---|---|---|
| `deck_words` FK `FKh8ccj9tko1jr2ub7p5g3yxnnl` → `decks` | `deck_id=999999` | **Msg 547** |
| `deck_words` UNIQUE `UKbwm3hnc7qphev33xfbxsx6c9j` (deck_id,vocab_id) | duplicate `(10006,10017)` | **Msg 2627** |
| `study_days` UNIQUE `uq_study_days_user_date` (user_id,study_date) | duplicate `(2,'2026-09-21')` | **Msg 2627** |
| `payment_transactions` filtered UNIQUE `UKlsp8jh693lih2txq7dl4bdnpx` | duplicate `transaction_id` | **Msg 2601** |
| `deck_words.deck_id` NOT NULL | `NULL` | **Msg 515** |
| `exercise_attempts` FK `FK62kdjx8ri5yvscw9ugujr2dki` → `users` | `user_id=999999` | **Msg 547** |

Post-rollback row counts unchanged: `deck_words 100 · study_days 3 · payment_transactions 126 · exercise_attempts 46`.
**Constraint enforcement is real and effective; no violation slipped through.**

### 2c. Constraint inventory

```
FKs total 25   disabled 0   not trusted 0
CHECKs total 8  disabled 0  not trusted 0
indexes disabled 0
```
No disabled or untrusted constraint, so no `WITH NOCHECK` shadow state.

---

## 3. Orphan scan across FKs

`tmp/v13/q8_orphan.sql` — generated a per-FK `NOT EXISTS` probe. **NULLs are counted on the
same line but in a separate column**, so they can never be miscounted as orphans.

**Orphans: 0 across all 25 FKs.** NULL FK counts (all on nullable columns, all legitimate):

| FK | orphans | null_fk |
|---|---|---|
| `decks.owner_id → users.user_id` | 0 | **10** |
| `video_attempts.graded_by → users.user_id` | 0 | **12** |
| `speaking_submissions.graded_by → users.user_id` | 0 | **18** |
| `speaking_prompts.lesson_id → lessons.lesson_id` | 0 | **7** |
| `vocabulary.lesson_id → lessons.lesson_id` | 0 | **118** |
| all other 20 FKs | 0 | 0 |

`vocabulary.lesson_id` being 100% NULL and `decks.owner_id` 100% NULL matches the codebase
(118/118 vocabulary rows, 10/10 decks) — these are optional associations, not corruption.

---

## 4. Streak schema, cutover semantics, row-level sanity

### Schema

```
study_days   id bigint IDENTITY PK · user_id bigint NOT NULL · study_date date NOT NULL
             FK fk_study_days_user → users(user_id)   [present, enabled, trusted]
             UNIQUE uq_study_days_user_date (user_id, study_date)
study_policy id int PK · effective_from date NOT NULL
user_streaks streak_id bigint IDENTITY PK · user_id bigint NOT NULL · study_date date NOT NULL
             games_played int NULL · words_studied int NULL · created_at datetime2 NULL
```

### Cutover semantics (read from `StudyActivityService` + verified live)

- `recordStudy(userId)`: `today = LocalDate.now(clock.withZone("Asia/Ho_Chi_Minh"))`. If
  `today.isBefore(effectiveFrom)` it **returns silently** (no row, no error — the audit-v10 F122
  fix); a *missing* policy row still throws, deliberately, to surface a deployment fault.
- `snapshot()` splits the calendar: `studiedDays` = SQL rows `>= effectiveFrom`; `legacyAccessDays`
  = Redis `user:login_days:*` members `< effectiveFrom` — legacy access is never relabelled as
  completed learning.
- `currentStreak()` starts at today if studied, else yesterday; walks back one day at a time.

Live API `GET /api/streak/snapshot` (real login `user@gmail.com`):

```json
{"today":"2026-09-22","currentStreak":2,"studiedToday":true,"effectiveFrom":"2026-09-20",
 "studiedDays":["2026-09-21","2026-09-22"],"legacyAccessDays":[...16 dates...],
 "legacyHistoryAvailable":true}
```

This is self-consistent with the DB rows below and with the +07 wall clock at query time.

### Row-level sanity

| Check | Value |
|---|---|
| `study_days` rows | 3 |
| rows before cutover (`< 2026-09-20`) | **0** |
| rows in the future (`> 2026-09-22` VN today) | **0** |
| NULL `user_id` | **0** |
| duplicate `(user_id, study_date)` groups | **0** |
| distinct users with study days | 2 |
| `study_policy` rows | 1 (`effective_from = 2026-09-20`) |

Per user: `user 2` → 2 days (09-21, 09-22); `user 3` → 1 day (09-21). Both users' rows are
`>= effectiveFrom`; the unique key holds; no back-dating or future-dating.

### `user_streaks` is a dead legacy table

1 row (`user_id=3, study_date=2026-06-27, games_played=2, words_studied=4`). **No JPA entity maps
it** — the complete `@Table(name=...)` list in `model/entity/` contains no `user_streaks`. It is
retained only because the streak README says "do not delete old user streak fields".

---

## FINDINGS

### F-V13-DB-01 · HIGH · `study_policy` singleton is NOT enforced — the F124 bug class, unfixed for this table

`tasks/streak-study/deploy.sql:27-31` creates the singleton guard **inside the
`IF OBJECT_ID(N'dbo.study_policy','U') IS NULL` block**:

```sql
IF OBJECT_ID(N'dbo.study_policy', N'U') IS NULL
    CREATE TABLE dbo.study_policy (
        id int NOT NULL PRIMARY KEY,
        effective_from date NOT NULL,
        CONSTRAINT ck_study_policy_singleton CHECK (id = 1)   -- line 31
    );
```

`StudyPolicy` is a mapped `@Entity`, so Hibernate `ddl-auto=update` **already created the table**
before the script runs. The guard is false, the whole block is skipped, and the CHECK constraint
is **never created**. This is precisely the failure mode the script's own F124 comment (lines 38-49)
describes — "Hibernate created the table first, so the whole block was skipped and the FK was
never created" — except the F124 fix was applied only to `study_days` (table / unique / FK split
into three independent guards), while `study_policy`'s CHECK was left inside the table guard.

**Live proof the singleton is unenforced:**

```
study_policy rows before: 1
INSERT INTO study_policy(id, effective_from) VALUES (2, '2020-01-01');   -- SUCCEEDS
  id | effective_from
   1 | 2026-09-20
   2 | 2020-01-01        <-- second policy row accepted
```

Rolled back; `policy_rows_after = 1`. Constraint inventory confirms **0 check constraints on
`study_policy`** (`ck_study_policy_singleton` count = 0).

**Impact:** `StudyActivityService.effectiveFrom()` reads `policies.findById(1)`. A second row
(`id=2`) is inert for that lookup, so the cutover itself is not corrupted today — but the
`CHECK (id = 1)` the deployment doc claims protects the singleton is absent, so the schema does
not match its stated contract. A future reader that does `findAll()`/`findFirst()` would silently
pick an arbitrary policy. The fix is one guarded `ALTER TABLE … ADD CONSTRAINT`, mirroring what
was already done for `study_days`.

### F-V13-DB-02 · MEDIUM · `users.last_study_date` / `users.current_streak` are stale, and the admin dashboard still reads them

The SQL cutover moved streak truth to `study_days`, and every user-facing streak surface now
calls `StudyActivityService`. But `users.last_study_date` is **never written by the new path**,
and one consumer still reads it:

- `AdminService.java:51` — `recentUsers = userRepository.countByLastStudyDateAfter(sevenDaysAgo.toLocalDate())`
  → queries `users.last_study_date`.

Measured divergence for the two users who actually studied:

| user | `users.last_study_date` (legacy) | `MAX(study_days.study_date)` (truth) |
|---|---|---|
| 2 (student) | 2026-09-19 | **2026-09-22** |
| 3 (administrator) | 2026-09-19 | **2026-09-21** |

Same query shape at tighter windows:

| window | legacy `users.last_study_date` count | truth `study_days` count |
|---|---|---|
| 7d (`>= 2026-09-15`) | 2 | 2 |
| 3d (`>= 2026-09-19`) | 2 | 2 |
| 2d (`>= 2026-09-20`) | **0** | **2** |
| 1d (`>= 2026-09-21`) | **0** | **2** |

At the 7-day window the two happen to agree (both = 2), which is why the bug is currently
invisible; at any window tighter than 3 days the legacy column reports **0 recent users while
two users studied**. `users.current_streak` is likewise frozen at 1 for user 2 while
`study_days` yields a streak of 2. The legacy repository queries
(`findUsersWhoHaveNotLoggedInSince`, `findUsersWithBrokenStreak`,
`findActiveUsersWhoLastStudiedOn`) are **unreferenced** — the scheduler uses the new
`findStudyReminderAtRisk`/`findStudyReminderBroken`, which correctly read `study_days`. So the
blast radius is the admin dashboard stat, not the reminder mail.

### F-V13-DB-03 · LOW · `SYSDATETIME()` on this container is UTC, not +07 — three clocks disagree

| clock | reading at the same instant |
|---|---|
| host (Windows) | `2026-09-22 03:09 +07:00` |
| `engflow-backend` container | `2026-09-22 03:09 +0700` (compose sets `TZ=Asia/Ho_Chi_Minh`) |
| `engflow-sqlserver` container | `2026-09-21 20:09 +0000` (no TZ set) |
| `SELECT SYSDATETIME()` in SQL Server | `2026-09-21 20:09` (== `SYSUTCDATETIME()`, offset 0) |

So the brief's warning is confirmed and is a live trap: **any ad-hoc `SYSDATETIME()`-based
comparison against a datetime2 column is off by 7 hours**, and `DATEDIFF(MINUTE, col, SYSDATETIME())`
silently reports every stored timestamp as 7h older than it is. It does *not* indicate the data is
wrong — see §6 below. Also note the two containers run in **different zones**, so "the server clock"
is ambiguous in this stack; use `DATEADD(hour,7,SYSDATETIME())` (or better, the host clock) as the
+07 reference.

### F-V13-DB-04 · LOW · three of the five `exercises` nonclustered indexes are prefix-redundant

Key composition (`tmp/v13/q19_idxcols.sql`):

| index | key columns | verdict |
|---|---|---|
| `idx_exercises_lesson_order` | `(lesson_id, order_index)` | **strict prefix** of the next two |
| `idx_exercises_lesson_type_order` | `(lesson_id, exercise_type, order_index)` | superset; +`difficulty`,`correct_answer` included |
| `IX_exercises_lesson_type_diff_order` | `(lesson_id, exercise_type, difficulty, order_index)` | superset |
| `idx_exercises_type` | `(exercise_type)` | **strict prefix** of `idx_exercises_lesson_type_order` |
| `idx_exercises_difficulty` | `(difficulty)` | standalone |

`idx_exercises_lesson_order` (1.13 MB) is a leading-column subset of
`idx_exercises_lesson_type_order` (3.66 MB), and `idx_exercises_type` (1.40 MB) is a
leading-column subset of the same. Total `exercises` index footprint = **19.92 MB** on a
43,735-row table.

**This does NOT justify action now** (see §5): 43k rows, 19.9 MB, all fragmentation < 11%,
and the redundant indexes are not measurably hurting anything. Recorded as a cleanup candidate
for when the table is next rebuilt, not a defect.

---

## 5. Index usage / fragmentation on the hot read paths

### Fragmentation — `sys.dm_db_index_physical_stats(..., 'LIMITED')`, leaf, IN_ROW_DATA

| table.index | pages | avg_frag % |
|---|---|---|
| `exercises.IX_exercises_lesson_type_diff_order` | 293 | 10.92 |
| `exercises.idx_exercises_type` | 177 | 4.52 |
| `exercises.idx_exercises_lesson_type_order` | 446 | 4.48 |
| `exercises.idx_exercises_difficulty` | 131 | 3.05 |
| `exercises.idx_exercises_lesson_order` | 142 | 1.41 |
| `exercises.PK` (clustered) | 1,283 | 0.86 |
| `lessons.PK` (clustered, IN_ROW_DATA) | 121 | 11.57 |
| `lessons.PK` (LOB_DATA) | 14,954 | 0.00 |
| `lessons.idx_lessons_level` | 7 | 42.86 |
| `lessons.idx_lessons_order_index` | 5 | 20.00 |
| `lessons.idx_lessons_pub_level_order` | 7 | 14.29 |

Note the `lessons` table is 117.91 MB total but only ~0.12 MB of that is its clustered
IN_ROW_DATA — the bulk is a 14,954-page LOB allocation (the long `content`/`content_original`
text), which is why `PK__lessons` appeared twice in a naive per-level query.

**Recommendation: no index maintenance.** Every index is far below the conventional
10–30% rebuild threshold in *page* terms; the three `lessons` indexes showing 20–43% are 5–7
pages each, where a single out-of-order page moves the percentage by 14 points and a rebuild
costs nothing to skip. Rebuilding them would be motion without benefit.

### Usage — `sys.dm_db_index_usage_stats`

Indexes with **zero** seeks and zero scans:

| index | seeks | scans | updates | size |
|---|---|---|---|---|
| `lessons.idx_lessons_level` | 0 | 0 | **85** | 0.070 MB |
| `users.UKr43af9ap4edm43mmtq01oddj6` | 0 | 0 | **60** | 0.016 MB |

`lessons.idx_lessons_level` is maintained on every one of the 85 lesson writes and never used
for a read — a pure write tax. `users.UKr43af9ap4edm43mmtq01oddj6` is a unique constraint
(the second of `users`' two unique indexes; `UK6dotkott2kjsp8vw4d0m25fb7` on `email` is used
17×/2×), so it is not droppable without checking which column it guards.

**Caveat, stated explicitly:** this DMV is **cleared on SQL Server restart** and the container
has ~2h uptime, so "0 seeks" here means "unused in the last 2 hours of a low-traffic dev
database", not "unused ever". The 85/60 update counts are the meaningful part (they prove the
indexes are being written). Do not drop anything on this evidence alone — it is a flag for a
longer observation window, not a finding.

### Measured IO on the real app query

The hot admin/lesson exercise query is a catch-all:

```sql
select top (@P0) e1_0.exercise_id, … from exercises e1_0
join lessons l1_0 on l1_0.lesson_id = e1_0.lesson_id
where (@P1 is null or l1_0.lesson_id = @P2)
  and (@P3 is null or @P4 = '' or e1_0.exercise_type = @P5)
  and (@P6 is null or @P7 = '' or e1_0.difficulty = @P8)
  and (@P9 is null or @P10 = '' or lower(e1_0.question) like lower('%'+@P11+'%')
                                  or lower(coalesce(e1_0.explanation,'')) like lower('%'+@P12+'%'))
order by e1_0.order_index, e1_0.exercise_id
```

| variant | logical reads | plan |
|---|---|---|
| no filters (`@P1…@P12` NULL) | **1,294** | `Sort(TOP 20)` ← `Index Scan(idx_exercises_lesson_order)` — full scan |
| `lesson_id = 10889` | **3** | index seek |
| `exercise_type='MULTIPLE_CHOICE' AND difficulty='EASY'` | **303** | index scan + read-ahead 369 |
| `lower(question) LIKE '%book%'` | **1,364** (scan count 13) | scan |

DMV aggregate for the unfiltered shape: 10 executions, 45,914 total logical reads,
**~4,591 logical reads/execution, ~200 ms CPU/execution** (`sys.dm_exec_query_stats`).

**Does this justify an optimization?** The unfiltered path costs ~1,294 logical reads
(≈10 MB) per execution at 43,735 rows — noticeable but not pathological, and it is an *admin*
listing, not a student hot path. Two honest options: (a) the `(@P is null or col = @P)` pattern
defeats parameter sniffing and forces one plan for every filter combination — the standard fix
is separate queries or `OPTION (RECOMPILE)`, which is a code change outside this phase's scope;
(b) leave it. Given the measured 200 ms CPU and the fact that the filtered student-facing paths
already seek in 3 reads, **I do not recommend acting on this in Phase 1** — the number is
recorded so a later phase can decide with data rather than a hunch.

---

## 6. Timezone check — datetime2 columns are naive Vietnam time (+07)

**Reference discipline:** per the brief, `SYSDATETIME()` was **not** used as the reference
(§F-V13-DB-03 shows why — it is UTC here). The reference used is the **host wall clock** plus a
**controlled live write**.

**Proof by controlled write.** A deck was created through the real API at a known instant:

```
container UTC clock at the request : 2026-09-21T20:09:16
deck row as stored                  : created_at = 2026-09-22 03:09:16.8152075
                                      updated_at = 2026-09-22 03:09:16.8152714
```

`20:09:16 UTC + 7h = 03:09:16 (+07)` — **exact match to the millisecond.** The stored
`datetime2` is naive Vietnam time.

**Why:** `engflow-backend` runs with `TZ=Asia/Ho_Chi_Minh` (docker-compose.yml:52). All
`created_at`/`updated_at` columns are `@CreationTimestamp`/`@UpdateTimestamp`, which Hibernate
stamps from the JVM default zone → +07.

**Corroboration from recent writes** (all near the query time of 03:08 +07 / 20:08 UTC):

| column | latest value |
|---|---|
| `decks.created_at` | 2026-09-22 03:08:37 |
| `user_vocabulary_progress.updated_at` | 2026-09-22 03:08:40 |
| `exercises.updated_at` | 2026-09-21 22:01:00 |
| `speaking_submissions.submitted_at` | 2026-09-21 21:57:15 |

**No mixed-zone hazard from DB defaults.** `SELECT COUNT(*) FROM sys.default_constraints` = **0**
across the entire database — every default was stripped by Hibernate, so nothing is being
stamped by the SQL Server's UTC `GETDATE()`. The `V1__init_schema.sql` `DEFAULT GETDATE()`
clauses are reference-only and were not materialized.

**Column-type hygiene:** every application datetime column is naive `datetime2` or `date`
(`users`, `lessons`, `exercises`, `decks`, `study_days`, `study_policy`, `payment_transactions`,
`exercise_attempts`, `speaking_submissions`, `video_attempts`, `user_vocabulary_progress`, …).
There is **no `datetimeoffset` column in any application table** (the only `datetimeoffset`
columns belong to SQL Server's own `plan_persist_*` internals). This is internally consistent
with the "naive Vietnam time" convention, but it is a convention the schema cannot enforce:
a future writer on a UTC JVM would silently store UTC into the same columns.

**Conclusion:** datetime2 columns are naive **+07 Vietnam** time, confirmed by a controlled
write. The trap is the *reader's* clock, not the stored data.

---

## Cleanup / side effects

| item | action | verification |
|---|---|---|
| `engflow_audit_scratch` database | created then dropped | `DB_ID(...)` → NULL |
| deck `50106` `ZZ_AUDIT_TZ_PROBE` | created via real API, deleted via real API | `probe_rows_left 0`, `decks_total 10` |
| production violation tests | wrapped in `BEGIN TRAN … ROLLBACK` | row counts unchanged (100/3/126/46) |
| `study_policy` id=2 test insert | rolled back | `policy_rows_after 1` |

My net effect on the database is **zero rows**. The only deltas seen during the window are the
two concurrent-phase writes documented above (F13 lesson/exercises, payment-flow `PENDING` rows),
which are outside this audit's control and were not authored by it.

---

## Verdict

| area | result |
|---|---|
| Parity | **PASS** — 10/10 numbers match baseline |
| Constraint effectiveness | **PASS** — Msg 547 / 2627 / 2601 / 515 all proven, scratch + production, 0 disabled/0 untrusted |
| Orphan integrity | **PASS** — 0 orphans across 25 FKs; NULLs separated |
| Streak schema & cutover | **PASS** with one schema gap — rows sane, cutover honoured, but `study_policy` singleton CHECK missing (**F-V13-DB-01**) |
| Index usage / fragmentation | **PASS** — nothing justifies maintenance; 2 unused indexes + 3 prefix-redundant indexes recorded as cleanup candidates |
| Timezone | **PASS** — datetime2 is naive +07, proven by controlled write |

Findings: 1 HIGH, 1 MEDIUM, 2 LOW. The HIGH is a real schema-vs-contract gap
(`study_policy` CHECK absent) with a one-line fix; the MEDIUM is a real staleness bug in the
admin dashboard whose visibility depends on the reporting window.
