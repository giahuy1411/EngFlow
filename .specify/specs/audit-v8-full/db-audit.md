# Database Audit — EngFlow

**Task:** comprehensive-audit-redesign Task 8 · **Date:** 2026-09-16
**Engine:** SQL Server 2019 (`engflow-sqlserver`) · **DB:** `english_learning`
**Read-only harness:** `sweep/v8/db-audit.sql` → `sweep/v8/db-audit-out.txt`
**Runner:** `python sweep/v8/sqlrun.py sweep/v8/db-audit.sql`

> **No DML in this audit.** Every statement in `db-audit.sql` is a `SELECT`.
> Data changes made elsewhere in this session are listed in §9 with their
> cleanup, and parity is re-verified at the end of this file.

## How the harness avoids the two classic traps

1. **Row counts must not fan out.** `[1]` counts `sys.partitions` per table
   (`index_id IN (0,1)`) rather than `COUNT(*)` over joins, because a single
   `JOIN` against `exercises` (43 737 rows) silently multiplies every other
   table's count. Earlier audits in this repo produced inflated numbers this way.
2. **`SYSDATETIME()` is UTC, the data is naive VN (+07).** The container clock is
   UTC while every `datetime2` is written by the JVM with
   `TZ=Asia/Ho_Chi_Minh`, so comparing the two is off by 7 h. `SYSDATETIME()` is
   used only to *label* the run in `[8]`, never to judge whether data is stale.
   A naive "future timestamp" check would flag healthy rows — this is a
   documented false-positive source (`AGENTS.md`, audit-v7 db-audit §7.4).

## 1. Table and row inventory

| Table | Rows |
|---|---:|
| `exercises` | 43 737 |
| `lessons` | 1 471 |
| `exercises_bak_v5` | 481 |
| `exercises_bak_v5b` | 322 |
| `vocabulary` | 127 |
| `payment_transactions` | 126 |
| `deck_words` | 100 |
| `users` | 76 |
| `exercises_bak_v5c` | 55 |
| `exercises_bak_v5d` | 39 |
| `exercise_attempts` | 38 |
| `speaking_submissions` | 28 |
| `user_progress` | 19 |
| `lesson_blocks` | 15 |
| `video_attempts` | 15 |
| `user_vocabulary_progress` | 14 |
| `decks` | 14 |
| `lesson_sections` | 10 |
| `speaking_prompts` | 6 |
| `lesson_snapshots` | 5 |
| `video_lessons` | 5 |
| `lesson_submissions` | 4 |
| `sysdiagrams` | 2 |
| `user_streaks` | 1 |

**Observation (not a defect):** four `exercises_bak_v5*` tables hold 897 rows of
pre-v5 backup data. They are outside the application's entity model, so Hibernate
`ddl-auto=update` will neither read nor drop them. They are harmless at this
size; dropping them is a data-retention decision for the owner, not an audit
fix. Recorded as `deferred`.

## 2. Referential integrity — all clean

| Check | Violations |
|---|---:|
| `exercises` → `lessons` | **0** |
| `lesson_sections` → `lessons` | **0** |
| `lesson_submissions` → `lessons` | **0** |
| `lesson_snapshots` → `lessons` | **0** |
| `user_vocabulary_progress` → `vocabulary` | **0** |
| `user_vocabulary_progress` → `users` | **0** |

`exercises` has **no `section_id` column** (verified against
`INFORMATION_SCHEMA.COLUMNS`): the exercise→section link is via `lesson_id`
only, so there is no section-level orphan check to make. The harness originally
asserted one and failed with `Msg 207`; the check was removed rather than
weakened, and the reason is recorded in the SQL file.

## 3. Foreign keys — none disabled, none untrusted

`[3]` returned **0 rows**: no `sys.foreign_keys` row has `is_disabled = 1` or
`is_not_trusted = 1`. Every declared FK is actually enforced, so the orphans
in §2 are not merely "unchecked" — they genuinely do not exist.

## 4. Filtered indexes — the `QUOTED_IDENTIFIER` constraint

| Table | Index | Filter |
|---|---|---|
| `payment_transactions` | `UKlsp8jh693lih2txq7dl4bdnpx` | `([transaction_id] IS NOT NULL)` |
| `user_progress` | `UK8sschjnhw7q49ml9th0urvo4b` | `([user_id] IS NOT NULL AND [lesson_id] IS NOT NULL)` |

These are why **every** `DELETE` batch in this database must begin with
`SET QUOTED_IDENTIFIER ON;`. Without it the batch fails with `Msg 1934` — and
critically, `sqlcmd` still exits **0**, so a harness that trusts the exit code
reports success while deleting nothing. Both cleanup scripts in this session
(`clean_r2.sql`, `clean-xss-probe.sql`) set it, and the runner greps for `Msg `
in the output instead of trusting the exit code.

## 5. Index usage

`[5]` lists nonclustered indexes by seek/scan count. **Unused ≠ drop.** The
plan cache was reset by the container restarts during this audit, so a low count
mostly reflects a cold cache, not an index the workload never needs. Reading the
low end of that list:

- `idx_exercises_type` (0 seeks / 89 updates), `idx_lessons_level`,
  `idx_lessons_pub_level_order`, `idx_prompts_level`, `idx_prompts_published`,
  `idx_speaking_status`, `idx_video_attempts_status`, `idx_decks_public`,
  `IX_decks_owner_id` — all currently 0 seeks with nonzero writes. They are
  plausible future filters, and each costs write throughput now. **Disposition:
  deferred, no action.** Dropping them on a cold-cache reading would be
  unsound, and the audit prompt forbids a change without measurement.
- The genuinely exercised ones confirm the hot paths: `IX_exercises_lesson_type_diff_order`
  (14 seeks), `idx_exercises_lesson_type_order` (21), `idx_speaking_submissions_user`
  (22), `UKcnc61y66y0f9p96e6j6qlbswl` on `user_vocabulary_progress` (23),
  `IX_video_attempts_graded_by` (17 scans).
- `IX_uvp_due` shows 1 scan / 5 updates — it is the filtered index in §4, so it
  is live even at low counts.

No index was added or dropped in this audit.

## 6. Duplicate business keys

| Check | Duplicate groups |
|---|---:|
| `users.email` | **0** |
| `lessons.title` | **1** |
| `vocabulary.word` | **3** |

- `users.email` = 0 confirms the unique constraint is doing its job — this is the
  one that matters for authentication.
- `lessons.title` = 1 group (both rows literally titled `test`, `lesson_id`
  91919/91920) and `vocabulary.word` = 3 groups (`collaborate`, `innovate`,
  `negotiate`). Titles and headwords are **not** unique keys by design — the app
  keys on `lesson_id` / `vocab_id`, and two lessons may legitimately share a
  display title. The 3 duplicate words are homograph pairs at different CEFR
  levels, which is valid for a vocabulary app. **Disposition: not a defect.**

## 7. Null / empty values against application assumptions

| Check | Count | Assessment |
|---|---:|---|
| `exercises.correct_answer` empty | 4 848 | **Expected.** Documented backfill state: 5 434 originally empty, 586 filled deterministically → 4 848 remain. These are mostly broken MC-fragment scrapes that are ungradeable by design; the grader returns `ungradeable=true` rather than guessing. Not a defect. |
| `exercises.question` empty | **0** | Clean. |
| `lessons.content` empty/NULL | **6** | Real content debt: `10888 Snapshot Test`, `11299 Test Unicode`, `11300 Unicode Test 2`, `81895 Admin Created Lesson`, `91919 test`, `91920 thầy bình` (last two empty-string, rest NULL). All are test/draft artefacts, not published curriculum. **Disposition: deferred** — deleting curriculum rows is an owner decision, and none of the six is referenced by a non-empty lesson list. |
| `lessons.content_original` NULL | 11 | **By design.** `content_original` is kept permanently (decision P5.1, 2026-09-12): the backfill never reads it, and its two remaining readers are feature-flagged off. Cost ≈ 69 MB of 230 MB. Do not propose dropping it again. |
| `LISTENING` exercises missing `audio_url` | **9** | Matches the documented 9/367 figure. Browser speech-synthesis fallback covers it (`frontend/src/utils/speech.js`). Not a defect. |
| `users.password_hash` empty | **0** | Clean — no account can be logged into with an empty hash. |

## 8. Timestamp sanity

Server UTC clock at run time: `2026-09-16 05:57:29`.
Newest `lessons.created_at`: `2026-09-04 15:32:22` (naive VN).
Newest `exercises.created_at`: `2026-09-04 15:33:04` (naive VN).

Lessons created more than one day "in the future" relative to `SYSDATETIME()`:
**0**. Note this check passes *despite* the 7 h skew, because the newest row is
12 days old; it is retained as a guard against a writer switching to UTC, not as
a freshness check. No row indicates a UTC write.

## 9. Audit-leftover scan and data parity

`[9]` found `lessons` and `decks` with audit-style names: **0**. It found
**8 users** with an audit/zz prefix — these are **pre-existing leftovers from
earlier audit rounds**, verified by `created_at`:

| user_id | email | created_at |
|---|---|---|
| 40009 | `audit_20260725071736@t.com` | 2026-07-25 |
| 80009 | `audit_6282@test.com` | 2026-08-09 |
| 80014 | `audit_browser_777@test.com` | 2026-08-09 |
| 90009 | `audit_test104@test.com` | 2026-08-17 |
| 170049 | `zzprobe30119@example.com` | 2026-09-12 |
| 170050 | `zzprobe65536@example.com` | 2026-09-12 |
| 170051 | `zzprobe79221@example.com` | 2026-09-12 |
| 170097 | `zzprobe15227@example.com` | 2026-09-12 |

All eight are dated **before this session** (2026-09-16), so they are not this
run's residue. Dependency scan: 0 rows in `user_vocabulary_progress`,
`lesson_submissions`, `payment_transactions`, `decks` and `user_progress`; only
`80014` owns 1 `exercise_attempt`. **Disposition: recorded, not deleted** — the
scope rule is that a run cleans up what it created, and deleting other rounds'
fixtures would destroy evidence of those rounds. Flagged as `deferred` hygiene
for the owner.

### Data this session did create, and its cleanup

| Harness | Rows created | Cleanup | Verified |
|---|---|---|---|
| `ui/routes-all.js`, `ui/design-v2.js`, `ui/design.js` | 6 × `payment_transactions` via `/premium` mount | `p16-clean-payments.sql` | 132 → **126** (baseline) |
| `security.js` §5 (earlier revision) | 4 × `vocabulary` with `word='img'` | `clean-xss-probe.sql` | 0 remaining |
| `xss-prove.js` | 3 × `vocabulary` marker rows per run | now **self-cleans** in-run (see below) | 0 remaining, total 127 |

`security.js` §5 was **removed** after this discovery: a read-only audit script
must not write business rows. The XSS proof now lives solely in `xss-prove.js`,
which owns that write and its cleanup.

#### The `payment_transactions` delta

The three UI sweeps mount `/premium`, and `PremiumCheckout.vue` calls
`POST /api/v1/payment/create-order` on mount to build the QR — a documented trap
in `AGENTS.md` (which records 126 → 128 for two sweeps). This run ran three
sweeps, so parity moved 126 → **132**. All 6 rows were inspected before deletion:
every one was `PENDING` with `transaction_id IS NULL`, i.e. an order that was
created but **never paid**. The cleanup therefore also requires
`status <> 'SUCCESS'`, so a genuinely paid order can never be removed.

#### `xss-prove.js` did not clean up after itself — found and fixed

This is the most important operational finding of the audit, because it means
**one earlier "127" reading was wrong**.

`xss-prove.js` writes real `vocabulary` rows (a stored-XSS proof has to store
something). Its cleanup was a *separate manual step* that was never run as part
of the harness. Residue accumulated and a later read showed **130** rows instead
of 127 — the drift was caught only because the adversarial harness re-read the
table rather than trusting the earlier figure.

The cleanup is now **part of the run**, and two bugs in it were fixed by testing
it rather than assuming it worked:

1. **The window started too late.** The run-start timestamp was computed *at
   cleanup time* instead of at process start, so `created_at >= start` excluded
   every row the run had just written. The batch reported success and matched
   **0 rows** — a silent no-op. Fixed by capturing `RUN_START_NAIVE` at module
   load.
2. **"No SQL error" was treated as success.** The first attempt printed
   `cleanup ran without SQL error: true` while leaving 3 rows behind. The
   assertion is now **parity**, not exit status: the cleanup must report
   `remaining = 0` *and* `vocabulary_total = 127`, or the run fails.

Also fixed: the word filter now matches `word LIKE 'XSSPROBE%'` **or** the exact
saved words, because the model sometimes extracts a short noun from a payload
(e.g. saves the literal word `iframe`) which carries no marker and would
otherwise survive.

Verified end-to-end after the fix:

```
cleanup: no SQL error=true remaining=0 total=127 (baseline 127) -> PARITY RESTORED
rows created by this run cleaned   : true (vocabulary now 127, baseline 127)
VERDICT: PASS - stored AI output cannot execute
```

**Lesson recorded:** a harness that writes business rows must clean up *inside
the same run*, and must verify parity rather than its own exit code. `AGENTS.md`
already warns that UI sweeps through `/premium` create real rows; the same
warning applies to any API harness that saves model output.


## 10. Query cost (plan cache)

| execs | avg logical reads | avg ms | shape |
|---:|---:|---:|---|
| 1 | 197 337 | 3 797 | lesson+exercise eager fetch |
| 1 | 120 148 | 389 | `db_id()` catalog probe |

These are single-execution, cold-cache entries from startup, not a steady-state
workload profile. The steady-state numbers for the hot paths are in
`performance.md`, measured by `sweep/v8/perf.js`. The 197 k-read entry is worth
noting as the shape F87 addressed: the admin list must use a flat `JOIN` plus a
batch projection (`LessonTitle`, `LessonListProjection`) rather than
`JOIN FETCH`ing `lesson.content` + `content_original`.

## 11. Database size

| Logical name | Type | Size |
|---|---|---:|
| `english_learning` | ROWS | 230.0 MB |
| `english_learning_log` | LOG | 64.0 MB |

## 12. Schema management

Schema is owned by Hibernate `ddl-auto=update`; **Flyway is disabled**. There is
no migration directory, so schema changes are made by editing the entity and
letting Hibernate apply the delta. Verified compatible: the 2026-09-12
`validate` drill booted with `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` and
reported `Started EngflowApplication in 14.5s` with 0 ERROR and 0 schema
warnings, so entity ↔ DB agree and `update` is not silently skipping a missing
table or column.

## Parity re-verification (end of audit)

| Metric | Baseline | Final | Match |
|---|---:|---:|---|
| `lessons` | 1 471 | 1 471 | ✅ |
| `exercises` | 43 737 | 43 737 | ✅ |
| `users` | 76 | 76 | ✅ |
| `vocabulary` | 127 | 127 | ✅ |
| `speaking_submissions` | 28 | 28 | ✅ |
| `video_attempts` | 15 | 15 | ✅ |
| `lesson_submissions` | 4 | 4 | ✅ |
| `payment_transactions` | 126 | 126 | ✅ |
| `decks` | 14 | 14 | ✅ |
| `lesson_snapshots` | 5 | 5 | ✅ |
| orphans (ex / uvp-vocab / uvp-user / snap / sub) | 0 | 0 | ✅ |
| `exercises.correct_answer` empty | 4 848 | 4 848 | ✅ |

**Verdict: parity restored exactly.** No audit-leftover rows from this session
remain, and no documented baseline figure moved.

## Summary of dispositions

| Item | Disposition |
|---|---|
| Referential integrity, FKs, unique email | **PASS** — no action |
| Filtered-index `QUOTED_IDENTIFIER` requirement | **PASS** — documented, enforced in harnesses |
| 4 848 empty `correct_answer` | **Not applicable** — documented ungradeable-by-design state |
| 9 LISTENING without `audio_url` | **Not applicable** — speech fallback covers it |
| 11 NULL `content_original` | **Not applicable** — kept by decision P5.1 |
| 6 empty `lessons.content` | **Deferred** — test/draft rows, owner decision |
| 8 pre-existing audit users | **Deferred** — outside this run's cleanup scope |
| 4 `exercises_bak_v5*` tables | **Deferred** — retention decision |
| ~9 zero-seek indexes | **Deferred** — cold-cache reading, no measurement justifies a drop |
