# EngFlow DB Audit (SQL Server 2019 CU32-GDR + Redis 8.6.3) — v7

**Scope**: read-only audit. No INSERT/UPDATE/DELETE/CREATE/DROP/ALTER was executed against `english_learning` or Redis. Every `CREATE INDEX`/`ALTER TABLE` below is a **proposal**, not applied.

**Snapshot time**: 2026-09-12 ~05:35–05:50 UTC (container uptime: SQL 2h, Redis uptime 9285s).
**⚠ The database is LIVE and being written during the audit.** `users` measured 72 rows at first pass and 75 at a later pass (row counts for other tables were stable: `exercises` 43,737 and `lessons` 1,471 re-verified unchanged). The 46,705 total and the per-table `rows` column below are the first-pass figures; `sp_spaceused`'s `users` row (75) reflects the later pass. D-08/D-08b report **both** measurements rather than silently picking one.
**Instance start**: `2026-09-12 03:15:00` (`sys.dm_os_sys_info.sqlserver_start_time`) — all DMV counters below are **since that restart**, which is a ~2.3h window. This materially limits the strength of "unused index" conclusions.
**DB created**: `2026-06-07 15:43:01`.

Containers verified up: `engflow-sqlserver` (healthy), `engflow-redis`, `engflow-backend`, `engflow-frontend`, `engflow-tts`, `engflow-whisper`, `engflow-minio`, `engflow-tailscale`.

---

## SECTION A — Inventory

24 tables, **46,705 total rows** across all tables.

Sizes from `sp_spaceused` per table (authoritative, run as a cursor over `sys.tables`). "reserved" = data + index + unused.

| # | table | rows | reserved KB | reserved MB | data KB | index KB | unused KB |
|---|---|---|---|---|---|---|---|
| 1 | lessons | 1,471 | 164,944 | **161.06** | 120,608 | 312 | 44,024 |
| 2 | exercises | 43,737 | 27,768 | **27.12** | 18,672 | 7,616 | 1,480 |
| 3 | exercises_bak_v5 | 481 | 1,176 | 1.15 | 416 | 8 | 752 |
| 4 | exercises_bak_v5b | 322 | 472 | 0.46 | 280 | 8 | 184 |
| 5 | sysdiagrams | 2 | 408 | 0.40 | 272 | 24 | 112 |
| 6 | video_attempts | 15 | 360 | 0.35 | 8 | 72 | 280 |
| 7 | speaking_submissions | 28 | 360 | 0.35 | 32 | 80 | 280 |
| 8 | lesson_blocks | 16 | 344 | 0.34 | 176 | 32 | 136 |
| 9 | speaking_prompts | 6 | 288 | 0.28 | 8 | 56 | 224 |
| 10 | deck_words | 100 | 288 | 0.28 | 8 | 56 | 224 |
| 11 | exercise_attempts | 38 | 280 | 0.27 | 136 | 32 | 112 |
| 12 | exercises_bak_v5d | 39 | 280 | 0.27 | 56 | 8 | 216 |
| 13 | lesson_snapshots | 5 | 280 | 0.27 | 96 | 24 | 160 |
| 14 | vocabulary | 127 | 280 | 0.27 | 48 | 48 | 184 |
| 15 | video_lessons | 5 | 216 | 0.21 | 88 | 32 | 96 |
| 16 | user_progress | 15 | 216 | 0.21 | 8 | 40 | 168 |
| 17 | user_vocabulary_progress | 14 | 216 | 0.21 | 8 | 40 | 168 |
| 18 | users | 75 | 216 | 0.21 | 32 | 48 | 136 |
| 19 | lesson_submissions | 4 | 216 | 0.21 | 8 | 40 | 168 |
| 20 | payment_transactions | 124 | 216 | 0.21 | 32 | 48 | 136 |
| 21 | decks | 14 | 216 | 0.21 | 8 | 40 | 168 |
| 22 | exercises_bak_v5c | 55 | 216 | 0.21 | 40 | 8 | 168 |
| 23 | lesson_sections | 11 | 144 | 0.14 | 8 | 24 | 112 |
| 24 | user_streaks | 1 | 144 | 0.14 | 8 | 24 | 112 |

**Database-level:**
| metric | value |
|---|---|
| DB total size (data+log files) | **592.00 MB** |
| data file allocated | 264.00 MB |
| log file allocated | 328.00 MB |
| actually used by objects | 199.16 MB |
| log space used | 327.99 MB allocated, **3.63% used** (~11.9 MB) |
| recovery model | FULL, `log_reuse_wait_desc = NOTHING` |
| backup history in msdb | **0 rows** (never backed up) |

**Top 15 by reserved size** = rows 1–15 above (`lessons` + `exercises` = 188.18 MB = **94.5% of all object bytes**).

### Oversized text columns

`lessons` holds 116.84 MB of its 161 MB in **LOB pages** (`sys.allocation_units.type = 2`). Root cause is two `nvarchar(max)` columns:

| column | rows | avg chars | max chars | sum chars | est. bytes |
|---|---|---|---|---|---|
| `lessons.content_original` | 1,460 | **24,827** | 57,506 | 36,248,516 | ~72.5 MB |
| `lessons.content` | 1,467 | 8,607 | 33,657 | 12,626,633 | ~25.3 MB |

`content_original` is **2.87× larger than `content`** and is used in **0/1471** rows equal to `content` (`orig_eq_content = 0`), 11 rows NULL. It is a pure one-time backup written by `HtmlCleanupMigration.java:65-67` (`config/HtmlCleanupMigration.java`) — "Backup original content to `content_original` column before cleaning". That migration is `@ConditionalOnProperty(engflow.html-cleanup.enabled=true)` and is a completed one-shot. **It is ~72.5 MB of dead LOB = 45% of the whole database's object bytes.**

`LessonListProjection.java:10` already documents that the app avoids selecting these columns — so the read path is protected, but the write/backup path is not.

### Leftover backup / junk tables

| table | rows | reserved | verdict |
|---|---|---|---|
| `exercises_bak_v5` | 481 | 1,176 KB | **100% redundant** — all 481 `exercise_id` values also exist in live `exercises` |
| `exercises_bak_v5b` | 322 | 472 KB | **100% redundant** — all 322 ids also in `exercises` |
| `exercises_bak_v5c` | 55 | 216 KB | **100% redundant** — all 55 ids also in `exercises`; also has **no `lesson_id` column at all** (5 cols only) |
| `exercises_bak_v5d` | 39 | 280 KB | 39/39 ids also in `exercises` |
| `sysdiagrams` | 2 | 408 KB | SSMS artifact, not app data |

Backup-table total = **2,144 KB (2.19 MB)** + sysdiagrams **336 KB**. None have a primary key — all four are **heaps** (`sys.indexes.is_primary_key` returned no PK row for any `*_bak_*`). They are referenced by **no FK** and by **zero cached queries**.

### LOB / max-length column inventory (37 columns)

`nvarchar(max)` or `varbinary(max)` columns, with measured max length — showing which are absurdly unbounded:

| table.column | measured MAX chars | sane bound |
|---|---|---|
| `lessons.content_original` | 57,506 | drop column (see B-01) |
| `lesson_blocks.data` | 32,275 | `nvarchar(max)` justified (TEXT block) |
| `lessons.content` | 33,657 | justified |
| `video_lessons.transcript_json` | 20,259 | justified (avg 5,057) |
| `exercise_attempts.details` | 7,891 | justified (JSON, avg 1,385) |
| `lesson_snapshots.snapshot` | 7,433 | justified |
| `exercises.question` | **1,132** | → `nvarchar(2000)` |
| `exercises.options` | 1,546 | → `nvarchar(2000)` (avg 44, only 2,027 populated) |
| `exercises.explanation` | 493 | → `nvarchar(1000)` (avg 111) |
| `vocabulary.definition_en` | 159 | → `nvarchar(500)` |
| `vocabulary.example_sentence` | 106 | → `nvarchar(500)` |
| `lessons.description` | 108 | → `nvarchar(255)` |
| `decks.description` | 69 | → `nvarchar(255)` |
| `speaking_prompts.description` | 65 | → `nvarchar(255)` |
| `speaking_prompts.reference_text` | 205 | → `nvarchar(500)` |
| `speaking_submissions.transcript` | 221 | → `nvarchar(1000)` |
| `speaking_submissions.feedback` | 139 | → `nvarchar(500)` |
| `speaking_submissions.pronunciation_details_json` | 217 | → `nvarchar(1000)` |
| `speaking_submissions.admin_feedback` | 88 | → `nvarchar(500)` |
| `speaking_submissions.private_note` | 59 | → `nvarchar(500)` |
| `payment_transactions.webhook_raw` | 413 | → `nvarchar(1000)` |
| `video_attempts.admin_feedback` | 320 | → `nvarchar(500)` |
| `lesson_submissions.submission_text` | 27 | → `nvarchar(500)` |
| `lesson_submissions.feedback` | 0 rows populated | → `nvarchar(1000)` |
| `speaking_prompts.prompt` | 49 | → `nvarchar(255)` |
| `decks.description`, `video_lessons.description`, `vocabulary.*` | ≤159 | see above |
| `sysdiagrams.definition` | varbinary(max) | SSMS-internal, ignore |

Note `exercises.correct_answer` is `nvarchar(500)` and measured max is **401** — at 80% of its cap. See B-06.

---

## SECTION B — Schema findings

`ID | priority | finding | concrete DDL or action`

**B-01 | P1 | `lessons.content_original` is a completed one-time migration backup consuming ~72.5 MB (45% of DB object bytes), read by nothing in the query cache, and `HtmlCleanupMigration` is gated off.**
Action (after a real backup exists — see C-06):
```sql
-- proposal only, NOT executed
ALTER TABLE dbo.lessons DROP COLUMN content_original;
```
If the raw HTML must be retained for audit, move it to a cold table or object storage (MinIO is already in the stack) rather than a live LOB column joined by the entity. Note the column is mapped at `model/entity/Lesson.java:41-42` (`columnDefinition = "NVARCHAR(MAX)"`) — the entity must be changed first or Hibernate `ddl-auto=update` will re-add it. Do **not** write a Flyway migration (project rule: Flyway disabled).

**B-02 | P1 | Four heap backup tables `exercises_bak_v5/v5b/v5c/v5d` (897 rows, 2,144 KB, no PK, no FK, never queried) are 100% id-overlapping with live `exercises`.**
Verified: `v5` 481/481 ids in `exercises`; `v5b` 322/322; `v5c` 55/55; `v5d` 39/39.
```sql
-- proposal only. Archive first, then:
DROP TABLE dbo.exercises_bak_v5;  DROP TABLE dbo.exercises_bak_v5b;
DROP TABLE dbo.exercises_bak_v5c; DROP TABLE dbo.exercises_bak_v5d;
```
**B-03 | P2 | `sysdiagrams` (2 rows, 408 KB) is an SSMS design artifact in an app schema.**
```sql
-- proposal only
EXEC sp_removediagrammingdatabase 'english_learning';
```
**B-04 | P1 | `exercise_attempts.lesson_id` (`bigint NOT NULL`) is a genuine FK child column with **no FK constraint** — the only such column in the schema after excluding PKs and `_bak_` tables.**
It is currently referentially clean (0 orphans), but nothing prevents future orphans, and `sp_fkeys`-driven tooling won't see the relationship.
```sql
-- proposal: verify then add.  Existing rows pass (0 orphans measured).
ALTER TABLE dbo.exercise_attempts WITH CHECK
 ADD CONSTRAINT FK_exercise_attempts_lesson
 FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(lesson_id);
```
**B-05 | P2 | Remaining `*_id` columns without FK are all either primary keys (13 of the 14) or `exercises_bak_v5c`'s absent `lesson_id`.** No further action; the schema's FK coverage is otherwise complete: **24 FK constraints**, and **every FK child column has a leading index** (`sys.foreign_keys` join test returned 0 rows lacking an index with `key_ordinal=1`). This is unusually good — deletes/cascades will not table-scan.

**B-06 | P2 | `exercises.correct_answer nvarchar(500)` has measured max length 401 (80% of cap), and MATCHING exercises store a comma-joined pair list in it (`"l=r,..."` contract).** A long MATCHING set over 500 chars will fail the insert at runtime with a truncation error rather than being caught in validation.
```sql
-- proposal
ALTER TABLE dbo.exercises ALTER COLUMN correct_answer NVARCHAR(2000) NOT NULL;
```
**B-07 | P2 | `nvarchar(max)` used where a bound is measurable and small — 15 of the 37 max columns have measured MAX < 500 chars** (full table in Section A). Each unbounded column defeats index-key inclusion and forces LOB-page indirection on the row.
Highest-value narrowing (biggest tables first):
```sql
-- proposals
ALTER TABLE dbo.exercises     ALTER COLUMN question    NVARCHAR(2000) NULL;
ALTER TABLE dbo.exercises     ALTER COLUMN options     NVARCHAR(2000) NULL;
ALTER TABLE dbo.exercises     ALTER COLUMN explanation NVARCHAR(1000) NULL;
ALTER TABLE dbo.lessons       ALTER COLUMN description NVARCHAR(255)  NULL;
ALTER TABLE dbo.vocabulary    ALTER COLUMN definition_en    NVARCHAR(500) NULL;
ALTER TABLE dbo.vocabulary    ALTER COLUMN example_sentence NVARCHAR(500) NULL;
ALTER TABLE dbo.payment_transactions ALTER COLUMN webhook_raw NVARCHAR(1000) NULL;
ALTER TABLE dbo.speaking_submissions ALTER COLUMN transcript NVARCHAR(1000) NULL;
ALTER TABLE dbo.speaking_submissions ALTER COLUMN feedback NVARCHAR(500) NULL;
ALTER TABLE dbo.speaking_submissions ALTER COLUMN pronunciation_details_json NVARCHAR(1000) NULL;
ALTER TABLE dbo.decks         ALTER COLUMN description NVARCHAR(255) NULL;
ALTER TABLE dbo.speaking_prompts ALTER COLUMN prompt NVARCHAR(255) NULL;
ALTER TABLE dbo.speaking_prompts ALTER COLUMN description NVARCHAR(255) NULL;
ALTER TABLE dbo.speaking_prompts ALTER COLUMN reference_text NVARCHAR(500) NULL;
ALTER TABLE dbo.video_attempts ALTER COLUMN admin_feedback NVARCHAR(500) NULL;
ALTER TABLE dbo.lesson_submissions ALTER COLUMN submission_text NVARCHAR(500) NULL;
ALTER TABLE dbo.lesson_submissions ALTER COLUMN feedback NVARCHAR(1000) NULL;
```
Caveat: these are Hibernate-mapped. `ddl-auto=update` will not revert a narrowing, but the entity `@Column(columnDefinition=...)` must be edited in lockstep or the DB and model diverge.

**B-08 | P2 | 8 of 9 `bit` columns are nullable with no default, and the DB has **0 default constraints** (`sys.default_constraints` count = 0).** Flag semantics that business logic treats as boolean are tri-state in SQL, so every read needs `ISNULL(...)`. `lessons.is_published` is 0 NULLs but nullable; `users.is_admin` and `user_progress.is_completed` are correctly `NOT NULL`.
```sql
-- proposals
ALTER TABLE dbo.lessons     ALTER COLUMN is_published  BIT NOT NULL;
ALTER TABLE dbo.video_lessons  ALTER COLUMN is_published BIT NOT NULL;
ALTER TABLE dbo.speaking_prompts ALTER COLUMN is_published BIT NOT NULL;
ALTER TABLE dbo.speaking_prompts ALTER COLUMN is_premium   BIT NOT NULL;
ALTER TABLE dbo.decks       ALTER COLUMN is_public     BIT NOT NULL;
ALTER TABLE dbo.users       ALTER COLUMN is_premium    BIT NOT NULL;
ALTER TABLE dbo.users       ALTER COLUMN is_active     BIT NOT NULL;
-- plus explicit defaults so INSERTs omitting the column do not silently produce NULL
ALTER TABLE dbo.lessons     ADD CONSTRAINT DF_lessons_is_published     DEFAULT (0) FOR is_published;
ALTER TABLE dbo.users       ADD CONSTRAINT DF_users_is_active          DEFAULT (1) FOR is_active;
ALTER TABLE dbo.users       ADD CONSTRAINT DF_users_is_premium         DEFAULT (0) FOR is_premium;
ALTER TABLE dbo.decks       ADD CONSTRAINT DF_decks_is_public          DEFAULT (0) FOR is_public;
ALTER TABLE dbo.speaking_prompts ADD CONSTRAINT DF_sp_is_published     DEFAULT (0) FOR is_published;
ALTER TABLE dbo.speaking_prompts ADD CONSTRAINT DF_sp_is_premium       DEFAULT (0) FOR is_premium;
ALTER TABLE dbo.video_lessons  ADD CONSTRAINT DF_vl_is_published       DEFAULT (0) FOR is_published;
```
All seven measured 0 NULLs today, so the `NOT NULL` flips are non-destructive.

**B-09 | P2 | `float` used for money-adjacent and mastery values.** 7 `float` columns: `lesson_submissions.score`, `speaking_submissions.score_total`, `speaking_submissions.pronunciation_{accuracy,completeness,fluency,prosody}`, `user_vocabulary_progress.ease_factor`.
- `speaking_submissions.score_total float` — **this is intended**: `SpeakingSubmission.java:85-86` maps `Double scoreTotal` on a 0-10 scale, and the project rule confirms "scoreTotal speaking: thang 0-10 (Double)". Keep as-is for the scale, but note the artifact: rows 40016/40017/40018 store `9.6999999999999993` where `SpeakingRubricResult.total()` (`Math.round(...*10.0)/10.0`) means `9.7`. Binary double cannot represent 9.7; the 0-10 one-decimal domain is a natural fit for `decimal(3,1)`, which is **already what the sibling teacher-graded column uses** (`speaking_submissions.score numeric(3,1)`).
```sql
-- proposal: align AI-graded total with the human-graded column's type
ALTER TABLE dbo.speaking_submissions ALTER COLUMN score_total DECIMAL(3,1) NULL;
```
- `user_vocabulary_progress.ease_factor float` — SM-2 ease factor, measured range 2.36–2.60. `float` is defensible for a multiplicative coefficient; `decimal(5,4)` would be exact. Low priority.
- `pronunciation_*` floats: all 28 rows NULL (`pronunciation_accuracy NULL = 28`), so these are currently dead columns — see D-14.
- Money is already correct: `payment_transactions.amount numeric(18,0)` (VND, no minor units) — good choice, no action.
- `exercise_attempts.percentage numeric(5,2)` and `user_progress.completion_percentage numeric(5,2)` — correct, no action.

**B-10 | P3 | Date/datetime choices are clean.** 0 columns use legacy `datetime` (all `datetime2`); `user_streaks.study_date`, `users.last_study_date`, `users.premium_expiry`, `users.ai_quota_date`, `payment_transactions.premium_expiry` are `date` — correct for day-granularity. `datetimeoffset` is absent, meaning all timestamps are naive local `datetime2` with no zone stored; for a single-region deployment that is acceptable, but `premium_expiry date` means "expires end-of-day" is implicit and unenforced. No DDL proposed.

**B-11 | P2 | Overlapping index: `deck_words.idx_deck_words_deck [deck_id]` is a strict key prefix of unique `UKbwm3hnc7qphev33xfbxsx6c9j [deck_id, vocab_id]` — fully redundant.**
```sql
-- proposal
DROP INDEX idx_deck_words_deck ON dbo.deck_words;
```
(Confirmed 0 usage stats for it; the UK serves all `where dw1_0.deck_id = @P0` patterns, which is the observed access path at 26 execs.)
A prefix-overlap sweep across all 62 indexes found **exactly this one** strict-prefix redundancy, and **zero** indexes sharing an identical key list. The rest of the schema has no duplicate indexes.
Adjacent note: `exercises.idx_exercises_lesson_order [lesson_id, order_index]` is **not** a prefix of `idx_exercises_lesson_type_order [lesson_id, exercise_type, order_index]` (different ordinal position for `exercise_type`), so it is legitimately separate — it serves `findByLessonIdOrderByOrderIndexAsc` and recorded 4 seeks.

**B-12 | P2 | Unused indexes (`user_updates > 0 AND user_seeks = 0 AND user_lookups = 0`).** Because the instance restarted 2.3h ago, I split this into "has stats" (safe to act on) vs "no stats row at all" (cannot conclude).
With write-activity but zero reads since restart:
| index | keys | updates | seeks | scans | verdict |
|---|---|---|---|---|---|
| `speaking_prompts.idx_prompts_level` | level | 16 | 0 | 0 | 3 distinct values / 6 rows — useless selectivity |
| `speaking_prompts.idx_prompts_published` | is_published | 16 | 0 | 0 | 2 distinct values / 6 rows — useless |
| `lessons.idx_lessons_level` | level | 4 | 0 | 0 | superseded by `idx_lessons_pub_level_order [is_published, level, order_index]` |
| `users.UKr43af9ap4edm43mmtq01oddj6` | username | 11 | 0 | 0 | **keep — enforces uniqueness** (75/75 distinct), login reads email not username |
| `video_lessons.idx_video_lessons_level` | level, is_published | 1 | 0 | 2 | too new to conclude |
| `vocabulary.idx_vocabulary_word_cefr` | word | 1 | 0 | 6 | too new to conclude |
```sql
-- proposals, safe on cardinality grounds regardless of the short window:
DROP INDEX idx_prompts_level     ON dbo.speaking_prompts;
DROP INDEX idx_prompts_published ON dbo.speaking_prompts;
DROP INDEX idx_lessons_level     ON dbo.lessons;
```
Zero-write, zero-read (never touched since restart — inconclusive, monitor one week before dropping): `deck_words.UKbwm3hnc7qphev33xfbxsx6c9j`, `decks.idx_decks_public`, `payment_transactions.UKlsp8jh693lih2txq7dl4bdnpx`, `user_progress.UK8sschjnhw7q49ml9th0urvo4b`, `video_attempts.idx_video_attempts_status`, `video_attempts.idx_video_attempts_user`. Two of these are UNIQUE constraints — drop only if uniqueness is genuinely not required.

**B-13 | P3 | Only 8 CHECK constraints in the whole DB**, and none enforce numeric domains. Existing: `exercises` (difficulty, exercise_type), `lessons.level`, `video_lessons.level`, `lesson_submissions.skill_type`, `speaking_prompts.mode`, `speaking_submissions.status`, `lesson_blocks.block_type`. Given D-08 found 5 `score_total` rows > 10, the rubric domain is unenforced.
```sql
-- proposals (validate current rows first — D-08 shows 5 would FAIL)
ALTER TABLE dbo.speaking_submissions ADD CONSTRAINT CK_ss_score_total_range
 CHECK (score_total IS NULL OR (score_total BETWEEN 0 AND 10));
ALTER TABLE dbo.speaking_submissions ADD CONSTRAINT CK_ss_rubric_dims_range
 CHECK (score_grammar IS NULL OR score_grammar BETWEEN 0 AND 10) ...
ALTER TABLE dbo.exercise_attempts ADD CONSTRAINT CK_ea_totals CHECK (total >= 0 AND score >= 0 AND score <= total);
ALTER TABLE dbo.exercise_attempts ADD CONSTRAINT CK_ea_pct CHECK (percentage BETWEEN 0 AND 100);
ALTER TABLE dbo.users ADD CONSTRAINT CK_users_streak CHECK (current_streak IS NULL OR current_streak >= 0);
ALTER TABLE dbo.users ADD CONSTRAINT CK_users_points CHECK (total_points IS NULL OR total_points >= 0);
```
**B-14 | P3 | 0 `NOT NULL` on `created_at`/`updated_at` in any of the 24 tables**, yet every measured row is populated (`exercises`: 0 NULL `created_at`, 0 NULL `updated_at`, 0 future). Audit columns that are always written should be non-null to prevent silent NULL history.
```sql
-- proposal, exercises as the exemplar:
ALTER TABLE dbo.exercises ALTER COLUMN created_at DATETIME2 NOT NULL;
ALTER TABLE dbo.exercises ALTER COLUMN updated_at DATETIME2 NOT NULL;
```
**B-15 | P3 | `exercises_bak_v5c` is a structurally divergent backup: it has 5 columns and **no `lesson_id`** at all, unlike `v5/v5b/v5d`.** Any tooling that treats `*_bak_*` as `exercises`-shaped will break. Covered by the drop in B-02.

---

## SECTION C — Performance findings + missing-index proposals

### C-01 | The missing-index DMVs are EMPTY — reported honestly, not fabricated

```
SELECT COUNT(*) FROM sys.dm_db_missing_index_details WHERE database_id = DB_ID()   --> 0
SELECT COUNT(*) FROM sys.dm_db_missing_index_details                                --> 0 (instance-wide, all DBs)
SELECT COUNT(*) FROM sys.dm_db_missing_index_group_stats                            --> 0
SELECT COUNT(*) FROM sys.dm_db_missing_index_groups                                 --> 0
```
Top-10-by-`avg_user_impact × user_seeks` is therefore **not obtainable** on this instance right now. Cause: `sqlserver_start_time = 2026-09-12 03:15:00`, i.e. ~2.3h of uptime against a very low-traffic dev workload (`total_commands_processed` on Redis = 591; `instantaneous_ops_per_sec = 0`). With 382 cached plans but 0 accumulated missing-index requests, the optimizer has not been asked a question it lacked an index for during this window.

**The proposals below are therefore derived from the actual observed predicates in `sys.dm_exec_query_stats`, not from the DMV** — arguably a stronger basis, since it reflects real executed shapes. Every one is a proposal; **none executed**.

### C-02 | Slowest cached queries, this DB only, ordered by logical reads

| ms/exec | execs | reads/exec | statement (one line, trimmed) |
|---|---|---|---|
| 1634.32 | 1 | 31,831 | `SELECT COUNT(*) AS n, AVG(LEN(content)) ... FROM lessons` — *my own audit probe, excluded from findings* |
| 331.01 | 1 | 120,148 | `SELECT db_id() as database_id, sm.[is_inlineable] ... FROM sys.sql_modules` — engine internal |
| 307.98 | 6 | **2,343** | `select e1_0.exercise_id,... from exercises e1_0` — **full-table scan, no WHERE** |
| 187.02 | 4 | 12,812 | `INSERT INTO @mssqljdbc_temp_sp_columns_result EXEC sp_columns_100` — JDBC metadata |
| 84.12 | 4 | 11,844 | `SELECT TABLE_QUALIFIER = s_cov.TABLE_QUALIFIER ...` — JDBC metadata |
| 97.15 | 1 | 1,935 | my audit index dump |
| 41.83 | 1 | 4,342 | `SELECT db_id() AS database_id, c.system_type_id ...` — engine internal |
| **63.48** | **1** | **4,769** | `select top (@P0) e1_0... join lessons ... where (@P1 is null or l1_0.lesson_id=@P2) and (@P3 is null or @P4='' or e1_0.exercise_type=@P5) and (@P6 is null or @P7='' or e1_0.difficulty=@P8) and (@P9 is null or @P10='' or lower(e1_0.question) like lower('%'+@P11+'%'))` |
| 29.52 | 6 | 259 | `INSERT INTO @temp_sp_statistics EXEC sp_statistics` — JDBC metadata |
| **13.34** | **1** | **2,350** | `select count_big(e1_0.exercise_id) from exercises e1_0 join lessons l1_0 ... where (@P0 is null or l1_0.lesson_id=@P1) and (@P2 is null or @P3='' or e1_0.exercise_type=@P4) and (@P5 is null or @P6='' or e1_0.difficulty=@P7) and (@P8 is null or @P9='' or lower(e1_0.question) like lower('%'+@P10+'%'))` — the COUNT twin of the row query |
| 7.87 | 30 | 249 | `INSERT INTO @jdbc_temp_fkeys_result EXEC sp_fkeys` — JDBC metadata, most-frequent |
| 2.80 | 4 | 136 | `select e1_0... ,l1_0.lesson_id,l1_0.audio_url,l1_0.category,l1_0.content,l1_0.content_original,... from exercises e1_0 join lessons` — **pulls both lesson LOBs per exercise row** |
| 1.62 | 2 | 8 | `select v1_0.... from vocabulary v1_0 order by v1_0.word offset @P0 rows` |
| 1.24 | 2 | 6 | `UPDATE users SET current_level='ELEMENTARY' WHERE current_level='BEGINNER'` |
| 0.66 | 7 | 6 | `select pt1_0.... from payment_transactions pt1_0 where pt1_0.status='PENDING' and pt1_0.created_at>=@P0 order by pt1_0.id desc` |
| 0.41 | 20 | 22 | `select dw1_0...,v1_0.... from deck_words join vocabulary where dw1_0.deck_id=@P0 order by dw1_0.order_index` |
| 0.40 | 4 | 3 | `select top (@P0) sp1_0.... from speaking_prompts where sp1_0.is_premium=0 and sp1_0.is_published=1 order by sp1_0.order_index, sp1_0.id` |

**Everything here is fast in absolute terms** — max app ms/exec is 308ms, and the read-heavy admin filter path is ≤64ms (63.48ms row branch / 13.34ms count branch). This is a small dev DB; there is no latency emergency. The findings are about *growth trajectory*, not current pain.

### C-03 | Findings with concrete DDL

**C-03a | P1 | Unbounded `findAll()` on `exercises` — 2,343 reads/exec, 43,737 rows pulled.**
`ExerciseService.java:196` → `exerciseRepository.findAll()` whenever `lessonId == null`. The query cache shows it executed 6× at 308ms each. This is the single worst scaling risk in the system: at 43k rows it is already the top app query, and `exercises` is the fastest-growing table (max `exercise_id` 766,150 vs min 648,903).
Action is code-first, not index-first — an index cannot make a 13-column × 43,737-row projection cheap:
```java
// ExerciseService.getAllExercises: replace the findAll() branch with a paginated or
// lesson-filtered query; the search filter is applied in Java AFTER loading all rows,
// which means every unfiltered admin search does a full table load.
```
The in-Java `keyword` filter (`.filter(e -> e.getQuestion()...contains(keyword))`) is the reason `findAll()` exists — it should be pushed into SQL, which is precisely what `getAdminExercisePage` (line 205+) already does with `findAdminPage`. The non-paged `getAllExercises` should delegate to the same.

**C-03b | P2 | The admin exercise filter cannot use the existing 3-part index — `difficulty` is only an INCLUDE column.**
Observed predicate shape (C-02, 4,769 and 2,350 reads/exec): `lesson_id = @p AND exercise_type = @p AND difficulty = @p AND lower(question) LIKE '%@p%'`. Current index is `(lesson_id, exercise_type, order_index) INCLUDE (difficulty, correct_answer)`. `difficulty` as INCLUDE can be *residually filtered* without a lookup, but it is not a key, so it cannot seek, and `order_index` sits before it — so `ORDER BY` satisfaction and `difficulty` filtering are mutually exclusive.
```sql
-- proposal (additive; do not drop idx_exercises_lesson_type_order — see Section F)
CREATE NONCLUSTERED INDEX IX_exercises_lesson_type_diff_order
  ON dbo.exercises (lesson_id, exercise_type, difficulty, order_index)
  INCLUDE (question, options, explanation, audio_url, image_url, created_at, updated_at);
```
**C-03c | P2 | The same filter's `lower(question) LIKE '%'+@p+'%'` is non-sargable and will defeat any B-tree index.** This is why the sibling index `lessons.idx_lessons_pub_level_order [is_published, level, order_index]` records **4 updates but 0 seeks and 0 scans** despite `where l1_0.is_published=1 and (l1_0.level=@p) and (lower(title) like '%..%' or lower(coalesce(description,'')) like '%..%' or lower(coalesce(category,'')) like '%..%')` running 8× at 249 reads/exec. The `OR` of three leading-wildcard `LIKE`s forces a clustered scan.
```sql
-- proposal: full-text instead of LIKE '%%'
CREATE FULLTEXT CATALOG EngFlowCatalog AS DEFAULT;
CREATE FULLTEXT INDEX ON dbo.lessons (title, description, category)
  KEY INDEX PK__lessons__6421F7BE05A3095C WITH CHANGE_TRACKING AUTO;
CREATE FULLTEXT INDEX ON dbo.exercises (question, explanation)
  KEY INDEX PK__exercise__C121418ED83A5E11 WITH CHANGE_TRACKING AUTO;
-- then rewrite the repository predicate to CONTAINS(...) instead of LOWER(...) LIKE '%%'
```
Caveat: `exercises.question` is `nvarchar(max)` — usable in a full-text index but not in a B-tree key, which is a second reason for B-07.

**C-03d | P2 | `select e1_0... ,l1_0.content, l1_0.content_original ... from exercises e1_0 join lessons` fetches both lesson LOBs — including the 72.5 MB `content_original` — for every exercise list row.** 4 execs × 136 reads today only because the join was filtered by `lesson_id`, but this is the query that turns B-01 from "wasted space" into "wasted bandwidth". `LessonListProjection` exists precisely to avoid this; the exercise→lesson path is not using it.
Action: select the projection, or drop `content_original` (B-01), so this JOIN cannot pull it.

**C-03e | P2 | `payment_transactions`: 107 of 124 rows are permanently `PENDING` with `transaction_id` NULL and `webhook_raw` NULL.** The polling query `where status='PENDING' and created_at>=@P0 order by id desc` (18 execs) will keep scanning an ever-growing dead set, and the existing index is `[user_id, status, created_at]` — **wrong leading column** for a status-driven query.
```sql
-- proposal
CREATE NONCLUSTERED INDEX IX_payment_status_created_id
  ON dbo.payment_transactions (status, created_at DESC, id DESC)
  INCLUDE (user_id, amount, plan_type, gateway, premium_expiry);
```
Note the selectivity caveat: `status` has only 2 distinct values and 86% are PENDING, so this index pays off mainly by satisfying `ORDER BY id DESC` and staying small — it will not become seek-efficient until stale pendings are swept. The real fix is a job to expire abandoned orders.
`UKlsp8jh693lih2txq7dl4bdnpx [transaction_id]` is a UNIQUE index over a column that is NULL in 107/124 rows — the uniqueness guarantee is vacuous for exactly the rows that need it. Consider a filtered unique index:
```sql
-- proposal
CREATE UNIQUE NONCLUSTERED INDEX UX_payment_txn_id_present
  ON dbo.payment_transactions (transaction_id) WHERE transaction_id IS NOT NULL;
```

**C-03f | P3 | SRS due-review query has no supporting index.** `user_vocabulary_progress` measured 12 rows with `next_review_date < SYSDATETIME()` and 1 row >1 year out. Its only indexes are PK(id), `UK(user_id, vocabulary_id)`, `IX(vocabulary_id)` — a "cards due now" query must scan.
```sql
-- proposal
CREATE NONCLUSTERED INDEX IX_uvp_due
  ON dbo.user_vocabulary_progress (next_review_date, user_id)
  INCLUDE (vocabulary_id, mastery_level, repetitions, review_count, srs_interval, ease_factor);
```
**C-03g | P3 | `exercises` covering-index proposal for the audio-repair job.** AGENTS.md records 89/449 listening items missing `audio_url`; the audit measures **9** today (improved). The repair/scan pattern `WHERE exercise_type='LISTENING' AND audio_url IS NULL` has no index that avoids a scan, since `idx_exercises_type [exercise_type]` has 5 distinct values across 43,737 rows.
```sql
-- proposal (filtered index — tiny and exact)
CREATE NONCLUSTERED INDEX IX_exercises_listening_no_audio
  ON dbo.exercises (lesson_id, order_index)
  WHERE exercise_type = 'LISTENING' AND audio_url IS NULL;
```
**C-03h | P3 | `video_attempts.idx_video_attempts_user [user_id]` duplicates access already served by `idx_video_attempts_lesson`/PK for the observed predicate `where va1_0.user_id=@P0 and va1_0.video_lesson_id=@P1`.** Only after the short window closes:
```sql
-- proposal (verify 1 week of stats first)
CREATE NONCLUSTERED INDEX IX_video_attempts_user_lesson
  ON dbo.video_attempts (user_id, video_lesson_id) INCLUDE (score, status, submitted_at);
DROP INDEX idx_video_attempts_user ON dbo.video_attempts;
```

### C-04 | Queries with scans > 100k logical reads

Exactly one, and it is **not an application query**:
| reads | statement | attribution |
|---|---|---|
| 120,148 | `SELECT db_id() as database_id, sm.[is_inlineable] ... FROM sys.sql_modules sm ...` | SQL Server internal scalar-inlining telemetry |

No app query exceeded 31,831 reads (that one being my own `LEN(content)` audit probe over `lessons`). **Threshold not breached by any real workload query.** The largest app read count observed was 4,769 (C-03b).

### C-05 | Blocking: 0 (as expected)

```
SELECT COUNT(*) FROM sys.dm_os_waiting_tasks
 WHERE blocking_session_id IS NOT NULL AND blocking_session_id <> @@SPID AND session_id <> @@SPID  --> 0
SELECT COUNT(*) FROM sys.dm_tran_active_transactions                                               --> 6
SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process = 1                                --> 12
```
No blocked process, no long-running transaction. `LCK_M_S` has 4 waits totalling 1,043 ms **cumulative since restart** — i.e. one 260ms shared-lock wait, and it was almost certainly behind my own `LEN(content)` full-LOB scan of `lessons`. Not a contention signal.

### C-06 | Wait stats summary — the box is healthy, memory-constrained

| wait_type | tasks | wait_ms | signal_ms | avg ms | reading |
|---|---|---|---|---|---|
| SOS_WORK_DISPATCHER | 38,855 | 313,188,635 | 4,572 | 8,060 | **idle scheduler parking — ignore** |
| LOGMGR_QUEUE | 132,544 | 17,029,955 | 357 | 128.5 | idle log writer wait — ignore |
| XE_DISPATCHER_WAIT | 133 | 8,483,207 | 0 | 63,783 | idle Extended Events — ignore |
| PWAIT_EXTENSIBILITY_CLEANUP_TASK | 30 | 8,460,619 | 8,460,619 | 282,020 | idle background task, 100% signal — ignore |
| QDS_PERSIST_TASK_MAIN_LOOP_SLEEP | 142 | 8,460,149 | 24 | 59,578 | Query Store sleep — ignore |
| **MEMORY_ALLOCATION_EXT** | **199,762** | **2,208** | 0 | 0.01 | real, but trivial per-op; high count = churn |
| PREEMPTIVE_OS_AUTHENTICATIONOPS | 1,979 | 1,310 | 0 | 0.66 | auth calls |
| **ASYNC_NETWORK_IO** | 15 | **1,164** | 1 | 77.6 | client-drain; consistent with big result sets (C-03a) |
| **PAGEIOLATCH_SH** | **2,832** | **1,057** | 72 | 0.37 | disk reads for data pages — low total, cache misses |
| **LCK_M_S** | 4 | 1,043 | 0 | 260.8 | see C-05 |
| CXPACKET | 35 | 298 | 3 | 8.5 | no parallelism problem |
| RESERVED_MEMORY_ALLOCATION_EXT | 4,507 | 127 | 0 | 0.03 | trivial |
| WRITELOG | 112 | 73 | 6 | 0.65 | log writes negligible (FULL recovery, 0 backups) |
| IO_COMPLETION | 135 | 41 | 0 | 0.30 | negligible |
| PAGEIOLATCH_EX / _UP, PAGELATCH_SH / _EX | 14 / 9 / 59 / 63 | 3 / 1 / 4 / 1 | — | — | noise |

**Conclusion: no IO bottleneck, no latch contention, no parallelism skew.** Total real (non-idle) wait time across every meaningful type is ~6.0 seconds (6,048 ms summed) over 2.3h.

**Memory is the one genuine constraint:**
| metric | value |
|---|---|
| host total physical | 3,022 MB |
| host available | 661 MB |
| SQL Server `max server memory` | **2,048 MB** |
| Target Server Memory | 1,031 MB |
| Total (Granted) Server Memory | **515 MB** |
| `physical_memory_in_use` | 4,096 MB |
| `memory_utilization_percentage` | 100 |
| `system_memory_state_desc` | "Available physical memory is high" |

Target (1,031 MB) sits at half of the configured max (2,048 MB) while the container is on a 3,022 MB host — the engine is throttling itself against host pressure. With only 515 MB actually granted and a **264 MB data file**, the buffer pool cannot hold the whole DB: `PAGEIOLATCH_SH` = 2,832 page reads confirms repeated eviction, and this is exactly why `exercises` full scans stay at ~2,343 reads/exec rather than going memory-resident. This box also hosts Ollama on a 4 GB RTX 2050 (per project notes), so RAM is genuinely contended — the honest recommendation is **reduce working-set demand (C-03a, B-01)** rather than raise `max server memory`.

**Index fragmentation (`sys.dm_db_index_physical_stats LIMITED`, pages > 50):**
| index | avg_fragmentation | pages | depth |
|---|---|---|---|
| `exercises.PK__exercise__C121418ED83A5E11` | **95.11%** | 2,330 | 3 |
| `lessons.PK__lessons__6421F7BE05A3095C` | 10.92% | 119 | 2 |
| `exercises.idx_exercises_type` | 7.10% | 183 | 2 |
| `exercises.idx_exercises_difficulty` | 2.96% | 135 | 2 |
| `exercises.idx_exercises_lesson_order` | 2.80% | 143 | 2 |
| `exercises.idx_exercises_lesson_type_order` | 2.75% | 437 | 3 |
| `lessons.PK__lessons__6421F7BE05A3095C` (LOB-level) | 0.00% | 14,954 | 1 |

**The clustered PK of `exercises` is 95% fragmented over 2,330 pages** — the direct consequence of inserting new exercises at ever-higher `exercise_id` while the underlying rows are 25 MB of LOB-heavy pages. This inflates every `exercises` scan by roughly the full page count. This is the one maintenance action with a clear measurable payoff:
```sql
-- proposal (OFFLINE on dev; requires brief write pause)
ALTER INDEX PK__exercise__C121418ED83A5E11 ON dbo.exercises REBUILD WITH (FILLFACTOR = 90, ONLINE = OFF);
ALTER INDEX ALL ON dbo.lessons REORGANIZE;
```
Related: `lessons` reports **44,024 KB `unused`** and `exercises` 1,480 KB — 21% of the `lessons` allocation is free-but-allocated pages. Combined with the 264 MB data file vs 199 MB used, there is ~65 MB reclaimable via `DBCC SHRINKFILE` *after* B-01 drops the column (shrink first is an anti-pattern).

**Statistics freshness:** `exercises` and `lessons` stats were last updated **2026-09-03/04** for every index-stat except two auto-stats from 2026-09-12. `sp_updatestats` has not run in 9 days against a table whose id range grew (min 648,903 → max 766,150). Stale stats on the largest table is a plausible contributor to the optimizer choosing `findAll()`-shaped plans. Proposal (maintenance-plan job, not a schema change):
```sql
-- proposal
UPDATE STATISTICS dbo.exercises WITH FULLSCAN;
```

---

## SECTION D — Data quality counts

Every figure from a query I ran. Where the DB changed between measurements, both values are given.

| # | metric | value | re-check SQL |
|---|---|---|---|
| D-01 | exercises total | **43,737** | `SELECT COUNT(*) FROM exercises` |
| D-02 | exercises `correct_answer` IS NULL | **0** | `SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL` |
| D-03 | exercises `correct_answer` empty/whitespace | **5,434** (12.42%) | `SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL OR DATALENGTH(correct_answer)=0 OR correct_answer = N''` (confirmed identical as `LTRIM(RTRIM(correct_answer))=''` = 5,434) |
| D-03b | ├ MULTIPLE_CHOICE | 5,039 | `... GROUP BY exercise_type` → `MULTIPLE_CHOICE 5039` |
| D-03c | └ FILL_BLANK | 395 | `... GROUP BY exercise_type` → `FILL_BLANK 395` |
| D-03d | MC rows **both** blank answer AND `options` NULL (utterly ungradeable) | **5,039** | `SELECT COUNT(*) FROM exercises WHERE exercise_type='MULTIPLE_CHOICE' AND LTRIM(RTRIM(ISNULL(correct_answer,'')))='' AND options IS NULL` |
| D-04 | exercises `audio_url` IS NULL (all types) | **43,378** (99.18%) | `SELECT COUNT(*) FROM exercises WHERE audio_url IS NULL` |
| D-05 | **LISTENING** exercises with `audio_url` NULL | **9** of 367 (2.5%) | `SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING' AND audio_url IS NULL` |
| D-05b | ├ by difficulty | 9 of 262 EASY; 0 of 27 MEDIUM; 0 of 78 NULL-difficulty | `SELECT difficulty, COUNT(*), SUM(CASE WHEN audio_url IS NULL THEN 1 ELSE 0 END) FROM exercises WHERE exercise_type='LISTENING' GROUP BY difficulty` |
| D-05c | non-LISTENING row that *has* audio | 1 (`exercise_id 651441`, MULTIPLE_CHOICE, `http://localhost:8080/audio/lesson_510_5_198.mp3`) | `SELECT TOP 3 exercise_id, exercise_type, audio_url FROM exercises WHERE audio_url IS NOT NULL AND exercise_type<>'LISTENING'` |
| D-06 | audio_url by type: MC 33,548/33,549 NULL; FILL_BLANK 9,112/9,112; TRANSLATION 377/377; MATCHING 332/332; LISTENING 9/367 | — | `SELECT exercise_type, COUNT(*) total, SUM(CASE WHEN audio_url IS NULL THEN 1 ELSE 0 END) audio_null FROM exercises GROUP BY exercise_type` |
| D-07 | lessons with **zero** exercises | **5** | `SELECT COUNT(*) FROM (SELECT l.lesson_id FROM lessons l LEFT JOIN exercises e ON e.lesson_id=l.lesson_id WHERE e.lesson_id IS NULL GROUP BY l.lesson_id) x` |
| D-07b | lessons uncounted-but-published context | 1,465 published, 6 unpublished, **3 unpublished WITH exercises** | `SELECT COUNT(*) FROM (SELECT l.lesson_id FROM lessons l JOIN exercises e ON e.lesson_id=l.lesson_id WHERE ISNULL(l.is_published,0)=0 GROUP BY l.lesson_id) y` |
| D-08 | users with zero `user_progress` rows | **70 of 75** (93%) — was 67 of 72 at first measurement; live DB grew | `SELECT COUNT(*) FROM users u WHERE NOT EXISTS(SELECT 1 FROM user_progress p WHERE p.user_id=u.user_id)` |
| D-08b | users with zero activity of ANY kind (progress/attempts/submissions/speaking/vocab) | **64 of 75** (85%) | `SELECT COUNT(*) FROM users u WHERE NOT EXISTS(...user_progress...) AND NOT EXISTS(...exercise_attempts a ON a.user_id...) AND NOT EXISTS(...lesson_submissions s...) AND NOT EXISTS(...speaking_submissions ss...) AND NOT EXISTS(...user_vocabulary_progress v...)` |
| D-09 | `user_streaks` total rows | **1** | `SELECT COUNT(*) FROM user_streaks` |
| D-09b | streak rows with FUTURE `study_date` | **0** | `SELECT COUNT(*) FROM user_streaks WHERE study_date > CAST(GETDATE() AS date)` |
| D-09c | streak rows with NULL `study_date` | **0** (column is `NOT NULL`) | `SELECT COUNT(*) FROM user_streaks WHERE study_date IS NULL` |
| D-09d | the single streak row | `streak_id 1, user_id 3, study_date 2026-06-27, games_played 2, words_studied 4` — **77 days stale** while `users.last_study_date = 2026-09-12` | `SELECT streak_id,user_id,study_date,games_played,words_studied,created_at FROM user_streaks` |
| D-10 | users with FUTURE `last_study_date` | **0** | `SELECT COUNT(*) FROM users WHERE last_study_date > CAST(GETDATE() AS date)` |
| D-10b | users with NULL `last_study_date` | **56 of 75** | `SELECT COUNT(*) FROM users WHERE last_study_date IS NULL` |
| D-11 | speaking `score_total` OUTSIDE 0-10 (**> 10**) | **5** | `SELECT COUNT(*) FROM speaking_submissions WHERE score_total > 10` |
| D-11b | detail: ids 40010, 40011 = **27.0**; 30006 = **19.0**; 40008 = **13.0**; id 2 = **12.0** | — | `SELECT id,user_id,prompt_id,score_total,score_fluency,score_grammar,score_vocabulary FROM speaking_submissions WHERE score_total>10 ORDER BY id` |
| D-11c | **proof these are pre-fix SUMs, not averages**: id 40010 has dims 9+10+8=**27**=score_total; 30006 6+7+6=**19**; 40008 3+4+6=**13**; id 2 3+3+3 but score_total 12 (inconsistent) | — | `SELECT id,score_total,score_fluency+score_grammar+ISNULL(score_vocabulary,0) AS sum3,(flu+gram+vocab)/3.0 AS avg3 FROM speaking_submissions WHERE score_total IS NOT NULL` |
| D-11d | post-fix rows correctly averaged: 40015→9.0 (avg of 8,10,9=9.0); 40016/17/18→9.7 (avg of 9,10,10=9.67); 40023→9.0 | **6 in range** | same query as D-11b |
| D-11e | speaking `score_total > 100` | **0** — no 0-100/0-10 unit confusion, purely sum-vs-average | `SELECT COUNT(*) FROM speaking_submissions WHERE score_total > 100` |
| D-12 | speaking `score_total` NULL | **17 of 28** (61%) | `SELECT COUNT(*) FROM speaking_submissions WHERE score_total IS NULL` |
| D-12b | **of which 9 are in a terminal graded state** (`COMPLETED`/`GRADED`) with NULL score → the learner sees no result | **9** | `SELECT COUNT(*) FROM speaking_submissions WHERE status IN ('COMPLETED','GRADED') AND score_total IS NULL` |
| D-12c | status distribution | COMPLETED 7, GRADED 11, **FAILED 7**, SUBMITTED 2, NULL 1 | `SELECT status, COUNT(*) FROM speaking_submissions GROUP BY status` |
| D-13 | rubric dimension cols `score_pronunciation` NULL | **27 of 28** — and `pronunciation_accuracy/completeness/fluency/prosody` **28/28 NULL**: all 5 float+int pronunciation columns are dead | `SELECT COUNT(*) FROM speaking_submissions WHERE score_pronunciation IS NULL` / `... WHERE pronunciation_accuracy IS NULL` |
| D-13b | teacher-graded `score numeric(3,1)` populated | **11** rows, range 8.0–9.5 (all in 0-10) | `SELECT COUNT(*), MIN(score), MAX(score) FROM speaking_submissions WHERE score IS NOT NULL` |
| D-14 | `exercise_attempts` total | **38** | `SELECT COUNT(*) FROM exercise_attempts` |
| D-14b | attempts with `total <= 0` (score 0 / total 0, `percentage` 0.00 — vacuous submissions) | **4** | `SELECT * FROM exercise_attempts WHERE total<=0` ; count via `SELECT COUNT(*) FROM exercise_attempts WHERE total<=0` |
| D-14c | attempts with `score > total` | **0** | `SELECT COUNT(*) FROM exercise_attempts WHERE score>total` |
| D-14d | attempts with `percentage` inconsistent with `score*100/total` | **0** | `SELECT COUNT(*) FROM exercise_attempts WHERE total>0 AND ABS(CAST(percentage AS decimal(10,4))-(CAST(score AS decimal(10,4))*100/total))>0.01` |
| D-14e | attempts `percentage` outside 0-100 | **0** | `SELECT COUNT(*) FROM exercise_attempts WHERE percentage<0 OR percentage>100` |
| D-15 | `video_attempts` (numeric(3,1), 0-10 scale per `VideoLessonService.java:177` guard `score<0 \|\| score>10`) | 15 rows, range 0.0–**9.2**, `>10`: **0**, NULL: 8 | `SELECT COUNT(*), MIN(score), MAX(score) FROM video_attempts` |
| D-16 | `lesson_submissions.score` (float, 0-100 expected) | **4 rows, ALL NULL** (min/max NULL) — `LessonSubmissionService.java:58` explicitly `existing.setScore(null)` | `SELECT MIN(score),MAX(score) FROM lesson_submissions` |
| D-17 | **Scale verification (the audit's asked-for mapping)** — `speaking_submissions.score_total` = **0-10 Double** (entity `SpeakingSubmission.java:85-86` `Double scoreTotal`; `SpeakingRubricResult.total()` divides by 3.0 and documents "between 0.0 and 10.0"; `AGENTS.md`: "scoreTotal speaking: thang 0-10 (Double)"). `exercise_attempts.score` = **0..total** raw count (`int`). `exercise_attempts.percentage` / `user_progress.completion_percentage` = **0-100 numeric(5,2)**. `video_attempts.score` / `speaking_submissions.score` = **0-10 numeric(3,1)** (teacher path, guarded at `VideoLessonService.java:177` and `SpeakingSubmissionService.java:179-180` `compareTo(BigDecimal.TEN) > 0`). `lesson_submissions.score` = **0-100 float** by intent but 100% NULL so unverifiable. | — | source read + `sys.columns` type dump |
| D-18 | **Orphan rows — every child table checked, ALL ZERO** | 0 | see D-18b |
| D-18b | `exercises.lesson_id`→lessons 0; `lesson_sections` 0; `lesson_blocks` 0; `lesson_snapshots` 0; `lesson_submissions`(lesson,user) 0/0; `speaking_prompts.lesson_id` 0; `speaking_submissions`(prompt,user) 0/0; `video_attempts.video_lesson_id` 0; `decks.owner_id` 0; `deck_words`(deck,vocab) 0/0; `vocabulary.lesson_id` 0; `payment_transactions.user_id` 0; `user_vocabulary_progress`(user,vocab) 0/0; `user_progress`(user,lesson) 0/0; `user_streaks.user_id` 0; `exercise_attempts`(lesson,user) 0/0 | — | `SELECT COUNT(*) FROM <child> c WHERE NOT EXISTS(SELECT 1 FROM <parent> p WHERE p.<pk>=c.<fk>)` for each; `exercises_bak_v5/v5b/v5d`→lessons also 0 |
| D-18c | Backup tables fully subsumed by live table | v5 481/481, v5b 322/322, v5c 55/55, v5d 39/39 ids present in `exercises` | `SELECT COUNT(*) FROM exercises_bak_v5 b WHERE EXISTS(SELECT 1 FROM exercises e WHERE e.exercise_id=b.exercise_id)` (repeat per table) |
| D-19 | **Duplicate exercises** (same lesson + same type + first 200 chars of question) | **629 groups, 2,534 rows involved (5.8%)** | `SELECT COUNT(*) FROM (SELECT lesson_id, exercise_type, LEFT(CAST(question AS nvarchar(max)),200) q, COUNT(*) c FROM exercises GROUP BY lesson_id, exercise_type, LEFT(CAST(question AS nvarchar(max)),200) HAVING COUNT(*)>1) d` and `SELECT SUM(c) FROM (...) d` |
| D-19b | worst groups | lesson 11180 MC ×**25**, lesson 11838 FB ×25, lesson 760 MC ×25, lesson 641 MC ×20, lesson 11074 MC ×20 — all with `question` of pure ellipsis characters | `SELECT TOP 5 lesson_id, exercise_type, COUNT(*) c, LEFT(MAX(CAST(question AS nvarchar(max))),60) FROM exercises GROUP BY lesson_id, exercise_type, LEFT(CAST(question AS nvarchar(max)),200) HAVING COUNT(*)>1 ORDER BY c DESC` |
| D-20 | exercises whose `question` contains the U+2026 ellipsis character | **14,587** (33.4%) | `SELECT COUNT(*) FROM exercises WHERE CAST(question AS nvarchar(max)) LIKE N'%…%'` |
| D-20b | exercises whose `question` is ONLY dots/ellipsis/dashes/spaces | **0** — the 5,434 bad rows have visible (if placeholder-y) question text, so they are recoverable by regenerating answers | `SELECT COUNT(*) FROM exercises WHERE LTRIM(RTRIM(CAST(question AS nvarchar(max))))<>'' AND REPLACE(REPLACE(REPLACE(REPLACE(CAST(question AS nvarchar(max)),'.',''),CHAR(8230),''),'-',''),' ','')=''` |
| D-20c | exercises with question shorter than 3 chars | **105** | `SELECT COUNT(*) FROM exercises WHERE LEN(LTRIM(RTRIM(CAST(question AS nvarchar(max)))))<3` |
| D-21 | **Root cause signature**: bad rows sample shows `question` = `"answered 13 was 14 was 15 decided 16 gave"` / `"The sun ……………………………………."` with `options` NULL and `correct_answer` `''` — numbered-list fragments and unresolved blanks, i.e. LLM output that the JSON-salvage layer accepted as text | — | `SELECT TOP 8 exercise_id,lesson_id,exercise_type,difficulty,question,options,'['+correct_answer+']' FROM exercises WHERE LTRIM(RTRIM(ISNULL(correct_answer,'')))='' ORDER BY exercise_id` |
| D-22 | **Answerability gap**: MULTIPLE_CHOICE with `options IS NULL` | **32,814 of 33,549 (97.8%)** — the UI has no choices to render; these are only answerable via free-text compare | `SELECT COUNT(*) FROM exercises WHERE exercise_type='MULTIPLE_CHOICE' AND options IS NULL` |
| D-22b | FILL_BLANK with `options IS NULL` | 8,765 of 9,112 (expected — no options needed) | `SELECT COUNT(*) FROM exercises WHERE exercise_type='FILL_BLANK' AND options IS NULL` |
| D-22c | MATCHING with `options IS NULL` | **0** — all 332 MATCHING correctly carry the `"left\|right"` option contract | `SELECT COUNT(*) FROM exercises WHERE exercise_type='MATCHING' AND options IS NULL` |
| D-23 | exercises with `difficulty` NULL | **312** (0.7%; 41,789 MEDIUM / 1,624 EASY / 12 HARD) | `SELECT ISNULL(difficulty,'(NULL)') d, COUNT(*) FROM exercises GROUP BY difficulty` |
| D-24 | exercises with `order_index` NULL | **3** | `SELECT COUNT(*) FROM exercises WHERE order_index IS NULL` |
| D-25 | exercises with `image_url` populated | **0** (43,737 NULL) — column is entirely unused | `SELECT COUNT(*) FROM exercises WHERE image_url IS NULL` |
| D-26 | exercises with NULL `question` / empty `question` | 0 / 0 | `SELECT COUNT(*) FROM exercises WHERE question IS NULL` |
| D-27 | exercises with future `created_at` / NULL `created_at` / NULL `updated_at` | 0 / 0 / 0 | `SELECT COUNT(*) FROM exercises WHERE created_at>SYSDATETIME()` etc. |
| D-28 | FILL_BLANK with no blank marker (`_` or `…`) in question | **0** — the blank convention holds | `SELECT COUNT(*) FROM exercises WHERE exercise_type='FILL_BLANK' AND question NOT LIKE '%[_]%' AND question NOT LIKE N'%…%'` |
| D-29 | `vocabulary` duplicate (word, cefr_level) pairs | **2** | `SELECT COUNT(*) FROM (SELECT word, ISNULL(cefr_level,'') c FROM vocabulary GROUP BY word, ISNULL(cefr_level,'') HAVING COUNT(*)>1) y` |
| D-30 | `users` duplicate email | **0** (UK enforced) | `SELECT COUNT(*) FROM (SELECT email FROM users GROUP BY email HAVING COUNT(*)>1) x` |
| D-31 | `users` with `is_active = 0` | **22 of 75** (29%) | `SELECT COUNT(*) FROM users WHERE is_active=0` |
| D-32 | `payment_transactions` non-terminal (still `PENDING`) | **107 of 124** (86%); `transaction_id` NULL 107; `webhook_raw` NULL 107 | `SELECT COUNT(*) FROM payment_transactions WHERE status NOT IN ('SUCCESS','FAILED','CANCELLED')` |
| D-33 | `lesson_snapshots`: 5 rows, **all 5 byte-identical** (14,866 bytes each, all lesson 567) — pure duplication | 5 duplicate snapshots | `SELECT snapshot_id, lesson_id, DATALENGTH(snapshot) bytes, COUNT(*) OVER (PARTITION BY snapshot) same FROM lesson_snapshots` |
| D-34 | `user_vocabulary_progress` SRS sanity: `ease_factor` 2.36–2.60 (SM-2 valid), 12 due, 1 >1yr out, 0 NULL `next_review_date` | — | `SELECT MIN(ease_factor),MAX(ease_factor) FROM user_vocabulary_progress` |
| D-35 | `users.total_points` range 0–33, no negatives; `current_streak` 0 NULL | — | `SELECT MIN(total_points),MAX(total_points) FROM users` |
| D-36 | `video_lessons`: 5 rows, 0 duplicate `youtube_video_id` | — | `SELECT COUNT(*) FROM (SELECT youtube_video_id FROM video_lessons GROUP BY youtube_video_id HAVING COUNT(*)>1) x` |

### Score-scale verdict

`speaking_submissions.score_total` is **0-10 Double**, confirmed three ways: the entity maps `Double scoreTotal`; `SpeakingRubricResult.total()` returns `Math.round((grammar+vocabulary+fluency)/3.0*10.0)/10.0` documented as "between 0.0 and 10.0"; and project rules state "scoreTotal speaking: thang 0-10 (Double)". **All other score columns are 0-100** (`percentage`, `completion_percentage`) **or raw counts** (`exercise_attempts.score` bounded by `total`) — with the exception of the two teacher-graded `numeric(3,1)` columns (`speaking_submissions.score`, `video_attempts.score`) which are also 0-10.

**5 of 11 scored speaking submissions (45%) violate the 0-10 domain** because they were written by the pre-fix SUM branch, before commit `754ab16 feat(speaking): normalize AI score to 0-10 average scale across UI and API`. D-11c proves the mechanism arithmetically (`sum3 == score_total` on 30006/40008/40010/40011). These rows will render 12–27 on a UI that clamps at 10. Remediation is a bounded, reviewable `UPDATE` of exactly 5 rows — **not executed here**, listed for approval:
```sql
-- proposal: recompute from stored dimensions, not a blanket clamp
UPDATE dbo.speaking_submissions
   SET score_total = ROUND((score_fluency + score_grammar + score_vocabulary)/3.0, 1)
 WHERE score_total > 10
   AND score_fluency IS NOT NULL AND score_grammar IS NOT NULL AND score_vocabulary IS NOT NULL;
```
(id 2 is the odd one: dims 3,3,3 → avg 3.0 but stored 12.0, so it too is caught by the same statement.)

---

## SECTION E — Redis report

**Instance**: redis 8.6.3, `engflow-redis` (redis:alpine), uptime 9,285 s (2.6 h), 3 connected clients, 0 blocked.

### Totals and namespaces

**44 keys**, all in **db0** (db1–db15 confirmed `DBSIZE 0`). `expires = 44` → **100% TTL coverage.**

| prefix | keys | type | measured TTL | code source of truth |
|---|---|---|---|---|
| `user:login_days:*` | **28** | `set` | 6,671,581–7,775,441 s (**77–90 days**) | `RedisConstants.LOGIN_DAYS_TTL_DAYS = 90L`, refreshed at `StreakService.java:161-164` |
| `streak:comeback:*` | **12** | `string` | 2,445,575–2,445,675 s (**28.3 days**) | `COMEBACK_SUPPRESSION_TTL = Duration.ofDays(30)` |
| `dictionary::*` | **2** | `string` | 2,625 / 2,728 s (**~44 min**) | Spring `@Cacheable(value="dictionary")` on `DictionaryService.java:22`; TTL from `RedisConfig.java:35-41` `${cache.ttl-hours:1}` = 1h |
| `streak:reminder:*` | **2** | `string` | 26,330 / 112,763 s (**7.3 h / 31 h**) | `REMINDER_MARKER_TTL = Duration.ofDays(2)` |

Key names (from `redis-cli --scan`): `dictionary::celebrate`, `dictionary::happy`; `streak:comeback:{110010,150011,40008,40009,40011,50007,60009,70009,80009,80010,80011,90009}`; `streak:reminder:{2026-09-10,2026-09-11}`; `user:login_days:{2,3,120010,150014,150016,150017,150018,150020,150021,150022,150023,150024,150025,150026,150028,150029,150030,150031,150033,150034,150035,150036,150037,150038,150039,150040,160040,160114}`.

### TTL coverage — **no leak risk**

**0 keys with no TTL** (`expires=44` == `keys=44`; independently verified by running `TTL` on all 44 keys — every one returned a positive integer, no `-1`, no `-2`). `expired_keys = 4` with `expired_keys_active = 4` (so background expiry is working) and **`evicted_keys = 0`** (no LRU pressure).

The two remaining `dictionary::` keys are a Spring-cache artifact, not hand-rolled: `RedisCacheConfiguration.defaultCacheConfig().entryTtl(1h)` explains the ~44 min remaining, and `.disableCachingNullValues()` explains why the `unless = "#result == '[]'"` guard on `DictionaryService.java:22` has produced only 2 survivors. `dictionary::` is **not declared in `RedisConstants.java`** — the only cache prefix in the app that isn't centralized (see E-03).

### Memory

| metric | value |
|---|---|
| `used_memory` | **1,662,888 bytes = 1.59 MB** |
| `used_memory_peak` | 1.59 MB (currently at peak) |
| `maxmemory` | **268,435,456 = 256.00 MB** |
| utilisation | **0.62%** |
| `maxmemory-policy` | `allkeys-lru` |
| `mem_fragmentation_ratio` | **5.10** |
| `mem_fragmentation_bytes` | 6,744,016 (6.4 MB) |
| `evicted_keys` | **0** |
| `keyspace_hits / misses` | 213 / 8 (96.4% hit) |
| `total_commands_processed` | 591 |
| `slowlog` | **LEN 0** — no slow command ever recorded |
| persistence | AOF **enabled** (`appendonly yes`), RDB `3600 1 / 300 100 / 60 10000`, `rdb_last_bgsave_status: ok`, `rdb_changes_since_last_save: 68` |
| `notify-keyspace-events` | **empty string** (disabled) |

1.59 MB of 256 MB with 0 evictions and 0 slowlog entries — **Redis is not a problem and has 160× headroom.** The `mem_fragmentation_ratio` of 5.10 looks alarming but is an artifact of tiny-dataset measurement: 6.4 MB of fragmentation against 1.59 MB of keys is allocator/replica-buffer overhead, not a leak; it will fall as the keyspace grows. No `MEMORY PURGE` warranted.

### Unbounded structure check — one growth pattern, one bounded

**`user:login_days:<userId>` is a growing SET — the only unbounded-by-design structure.** `SCARD` measured: `user:login_days:2` = **10** members, `:120010` = **5**, `:3` = **9**, and the other 25 keys = **1** each. Members are ISO dates (`2026-08-30 … 2026-09-12` from `SMEMBERS user:login_days:3`).

This is **not** an unbounded leak, and here is the proof rather than an assumption: `StreakService.java:161-164` does `opsForSet().add(key, date)` followed by `redisTemplate.expire(key, 90, DAYS)` on **every** write. One member per calendar day × a TTL that is refreshed on each touch ⇒ **hard ceiling ≈ 90 members per key**. At 10 bytes per `YYYY-MM-DD` member the worst case is a few KB per user; at 75 users that is KB-scale. Growth is bounded by TTL, not by data volume.

Two real consequences worth noting:
- The ceiling is silently **correctness-affecting, not just memory**: a user inactive for 91 days loses their entire login history, and `StreakService` reads that set to compute streaks (`:108` `members(...)`). A returning user will see a broken streak. The 30-day `streak:comeback:*` suppression window does not cover a 90-day gap, so the two TTLs are misaligned. If a 12-month streak is a product goal, `LOGIN_DAYS_TTL_DAYS` must rise (memory cost is negligible at 0.62% utilisation).
- Per-user key count scales with **users**, not activity: 1 key/user + 1 key/prompt-relationship. At 100k users this is ~100k keys ≈ 30–50 MB — still inside 256 MB. Fine.

**`streak:comeback:<id>`** — 12 keys, TTL 28.3 d, values are the literal `"1"` (a suppression flag). Keys are per-user (confirmed: all 12 ids exist in `users.user_id`, which ranges 2–170051), so 1 key/user, self-expiring. Bounded.
**`streak:reminder:<date>`** — 1 key/calendar-day, TTL 2 days ⇒ **steady state ≤ 2 keys**. Bounded, and correctly so.

### E — findings

| ID | pri | finding | action |
|---|---|---|---|
| E-01 | P3 | `user:login_days` TTL (90 d) and `streak:comeback` suppression TTL (30 d) are misaligned, so a 31–90 day absence produces an inconsistent streak. | Align deliberately: either `LOGIN_DAYS_TTL_DAYS` → 365, or shorten suppression. Cost of 365 d is < 1 MB. |
| E-02 | P3 | `notify-keyspace-events` is empty, so no key-expiry observability — a leaked key would be invisible until `maxmemory` is hit. | `CONFIG SET notify-keyspace-events Ex` (or in the compose command) if expiry telemetry is wanted. Not required at 0.62% utilisation. |
| E-03 | P2 | `dictionary::` is the only Redis prefix **not** declared in `RedisConstants.java`; it comes implicitly from `@Cacheable(value="dictionary")` + Spring's `<cache>::<key>` layout, and its TTL lives in a different knob (`${cache.ttl-hours:1}`) than every other key. | Add `public static final String DICTIONARY_CACHE = "dictionary";` to `RedisConstants` and reference it in `@Cacheable`, so the whole namespace + its 1 h TTL is auditable in one file. |
| E-04 | P3 | AOF is on (`appendonly yes`) with default RDB `save` points, but **SQL Server has 0 backups in msdb** (see C-02/section A) — Redis state is more durable than the system of record it caches. Redis holds only recomputable streak/cache state, so this inversion is harmless today but signals a missing backup discipline on the SQL side. | No Redis change. Prioritise the SQL backup job (Section A: `log_reuse_wait_desc = NOTHING` under FULL recovery means the 328 MB log will grow unbounded once writes increase and no log backup runs). |
| E-05 | P3 | No `rate_limit:*`, `login_fail:*`, `login_lock:*`, `otp:*`, `game:session:*`, or `game:points:today:*` keys exist despite all being declared in `RedisConstants.java` — consistent with 2.3 h of idle traffic and 591 total commands. | No action; recorded so a later audit can distinguish "absent" from "never created". |

---

## SECTION F — Prior-fix verification: `idx_exercises_lesson_type_order`

### Verdict: **PRESENT, correctly defined, and IN USE (2 user scans, 27 range scans, 11 live cached plans).**

### F-1 Existence

`sys.indexes` filtered on `object_id = OBJECT_ID('dbo.exercises')` returns the index by exact name. It is the 5th of 5 indexes on `exercises` (alongside the clustered PK, `idx_exercises_type`, `idx_exercises_difficulty`, `idx_exercises_lesson_order`).

### F-2 Definition — matches the audit-v6 spec exactly

| property | measured |
|---|---|
| table | `dbo.exercises` |
| name | `idx_exercises_lesson_type_order` |
| type | `NONCLUSTERED` |
| `is_unique` | 0 |
| key columns (ordinal) | **`lesson_id ASC, exercise_type ASC, order_index ASC`** |
| `INCLUDE` columns | **`difficulty, correct_answer`** |
| filtered? | `has_filter = 0`, `filter_definition = NULL` |

Cross-checked against `.specify/specs/audit-v6-full/REPORT.md:44`, which specifies `idx_exercises_lesson_type_order (lesson_id, exercise_type, order_index) INCLUDE (difficulty, correct_answer)` — **exact match**, so the deployed DDL equals the recorded decision. The physical size is 437 pages (3.4 MB) at depth 3.

### F-3 Usage — three independent signals, all positive

**(a) `sys.dm_db_index_usage_stats`** (since the 2026-09-12 03:15 restart):
```
user_seeks = 0 | user_scans = 2 | user_lookups = 0 | user_updates = 0
last_user_scan = 2026-09-12 05:39:57.493 | last_user_seek = NULL | last_user_update = NULL
```
`user_scans = 2 > 0` ⇒ **the threshold "user_seeks/scans > 0" is satisfied via scans.** `last_user_scan` timestamp is inside this audit window — the index is being touched *right now*, not historically.

**(b) `sys.dm_db_index_operational_stats`** — a second, independent counter:
```
range_scan_count = 27 | singleton_lookup_count = 0
```
27 range scans confirms real access; usage_stats' `user_scans = 2` counts *operations at the query level*, operational_stats counts *physical range scans*, so 27 ≫ 2 is expected and mutually corroborating.

**(c) Plan cache — 11 cached plans reference the index by name:**
```sql
SELECT COUNT(*) FROM sys.dm_exec_cached_plans cp CROSS APPLY sys.dm_exec_query_plan(cp.plan_handle) pq
 WHERE pq.dbid = DB_ID()
   AND pq.query_plan.exist('//*[local-name()="Index" or local-name()="Object"][contains(@Index,"idx_exercises_lesson_type_order")]') = 1
--> 11
```
11 distinct plans are compiled against it, so the optimiser *chooses* it, and the choice is being made repeatedly.

**(d) Statistics exist and were updated separately from the rest of the table** — `STATS_DATE` for `exercises.idx_exercises_lesson_type_order` = **2026-09-04 04:47:17**, i.e. created and auto-analysed on the 4th, nine days *after* every other `exercises` stat (2026-09-03 05:14:30). That timestamp is a second proof the index is a later addition (consistent with the audit-v6 fix being applied after that date) and that SQL Server considers it worth maintaining.

### F-4 Honest caveats on the "is it helping?" question

1. **`user_seeks = 0`.** Every access so far has been a *range scan* of the index, not a *seek*. That is correct behaviour for `WHERE lesson_id = @p AND exercise_type = @p ORDER BY order_index` (an equality on the first key degrades into a narrow scan of the rest), so it is not a red flag — but it does mean the index is being used for its **`INCLUDE`-driven covering** property (avoiding 43,737-key lookups on `difficulty`/`correct_answer`) rather than for seek efficiency.
2. **It does not serve the heaviest observed admin filter.** The 4,769-reads/exec query filters on `lesson_id + exercise_type + **difficulty** + order_index`, and `difficulty` is an INCLUDE column, not a key — so the engine cannot seek on it and `order_index` sits in the wrong ordinal position for both goals at once. That gap is what proposal **C-03b** closes. The fix is doing its job for the pattern it was designed for; the residual cost is a *different* predicate shape.
3. **The v6 measurement ("7 ms → 0 ms") could not be reproduced** in this audit — no cached query in the 2.3 h window hits exactly that shape at 0 ms, and I ran no workload. What I *can* say is the index is loaded, chosen by the optimiser 11×, scanned 27×, and freshly analysed. **The fix is present, deployed, and live.**

### F-5 Re-check SQL (copy-paste ready)

```bash
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' \
  -d english_learning -C -I -W -s "|" -Q "
SELECT i.name, i.type_desc, i.is_unique,
  STUFF(CAST((SELECT ', '+c.name+CASE WHEN ic.is_descending_key=1 THEN ' DESC' ELSE '' END
     FROM sys.index_columns ic JOIN sys.columns c
       ON c.object_id=ic.object_id AND c.column_id=ic.column_id
    WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=0
    ORDER BY ic.key_ordinal FOR XML PATH('')) AS varchar(max)),1,2,'') AS keys,
  STUFF(CAST((SELECT ', '+c.name FROM sys.index_columns ic JOIN sys.columns c
       ON c.object_id=ic.object_id AND c.column_id=ic.column_id
    WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=1
    ORDER BY ic.index_column_id FOR XML PATH('')) AS varchar(max)),1,2,'') AS included,
  us.user_seeks, us.user_scans, us.user_lookups, us.user_updates, us.last_user_seek, us.last_user_scan
FROM sys.indexes i LEFT JOIN sys.dm_db_index_usage_stats us
  ON us.object_id=i.object_id AND us.index_id=i.index_id AND us.database_id=DB_ID()
WHERE i.name = 'idx_exercises_lesson_type_order';"
```

---

## Appendix — headline SQL for every Section A/B number

**Total rows across all tables (46,705):**
```sql
SELECT SUM(rows) FROM (SELECT t.name, SUM(CASE WHEN p.index_id IN (0,1) THEN p.rows ELSE 0 END) AS rows
 FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id GROUP BY t.name) x;
```
**Per-table sizes:** `EXEC sp_spaceused @objname = N'<table>'` for each of the 24 tables in `sys.tables`. (A single-shot JOIN of `sys.partitions`×`sys.allocation_units` **inflates row counts** because LOB/RDOVFL units fan out — `lessons` read 4,413 vs true 1,471 and `exercises` 131,211 vs 43,737. Use `sp_spaceused` or pre-aggregate `index_id IN (0,1)` separately, as done here.)
**DB/file sizes (592 / 264 / 328 MB, 199.16 used):**
```sql
SELECT CAST(SUM(size)*8/1024.0 AS decimal(10,2)) FROM sys.master_files WHERE database_id=DB_ID('english_learning');
SELECT CAST(SUM(size)*8/1024.0 AS decimal(10,2)) FROM sys.database_files WHERE type=0;  -- data
SELECT CAST(SUM(size)*8/1024.0 AS decimal(10,2)) FROM sys.database_files WHERE type=1;  -- log
SELECT CAST(SUM(a.total_pages)*8/1024.0 AS decimal(10,2)) FROM sys.allocation_units a JOIN sys.partitions p ON p.partition_id=a.container_id;
```
**LOB split (lessons = 116.84 MB in LOB pages):** same partition/allocation join with `SUM(CASE WHEN a.type=2 THEN a.used_pages ELSE 0 END)*8/1024.0`.
**FK list (24 constraints) / `*_id` columns without FK (14, of which 13 are PKs):**
```sql
SELECT fk.name, OBJECT_NAME(fk.parent_object_id)+'.'+COL_NAME(fc.parent_object_id,fc.parent_column_id) AS child,
       OBJECT_NAME(fc.referenced_object_id)+'.'+COL_NAME(fc.referenced_object_id,fc.referenced_column_id) AS parent
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id;
-- NB: the FK-column join column is constraint_object_id, NOT constraint_id (SQL 2019).
SELECT t.name+'.'+c.name FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id
 JOIN sys.types ty ON ty.user_type_id=c.user_type_id
WHERE c.name LIKE '%[_]id' AND ty.name='bigint'
 AND NOT EXISTS(SELECT 1 FROM sys.foreign_key_columns f
      WHERE f.parent_object_id=c.object_id AND f.parent_column_id=c.column_id)
 AND NOT EXISTS(SELECT 1 FROM sys.index_columns ic
      WHERE ic.object_id=c.object_id AND ic.column_id=c.column_id AND ic.is_included_column=0
        AND EXISTS(SELECT 1 FROM sys.indexes i
             WHERE i.object_id=ic.object_id AND i.index_id=ic.index_id AND i.is_primary_key=1))
 AND t.name NOT LIKE '%bak%' AND t.name<>'sysdiagrams';
```
**varchar(max) inventory (37 columns):** `WHERE (ty.name IN ('varchar','nvarchar','varbinary') AND c.max_length=-1) OR ty.name IN ('text','ntext','image')`.
**Prefix-overlap sweep:** build `#ik` of `RTRUNC = STUFF(CAST((SELECT ' '+c.name ... FOR XML PATH('')) AS varchar(max)),1,1,'')` + `nkeys`, then self-join on `a.nkeys < b.nkeys AND LEFT(' '+b.RTRUNC+' ', LEN(a.RTRUNC)+2) = ' '+a.RTRUNC+' '`. (Two SQL 2019 gotchas hit here: `STUFF` needs `CAST(...AS varchar(max))` around the XML, and system-catalog names collide with `Latin1_General_CI_AS_KS_WS` — every concatenation needs `COLLATE DATABASE_DEFAULT`.)
**Stats freshness:** `SELECT OBJECT_NAME(s.object_id), STATS_DATE(s.object_id, s.stats_id) FROM sys.stats s WHERE s.object_id IN (OBJECT_ID('dbo.exercises'),OBJECT_ID('dbo.lessons'))`.
**Backup history (0 rows):** `SELECT type, backup_start_date, backup_size/1024/1024 FROM msdb.dbo.backupset WHERE database_name='english_learning'`.
**Log space:** `DBCC SQLPERF(LOGSPACE)` — 327.99 MB allocated, 3.63% used.

### Redis commands behind Section E

```bash
docker exec engflow-redis redis-cli DBSIZE                  # 44
docker exec engflow-redis redis-cli INFO keyspace           # db0:keys=44,expires=44
for i in $(seq 1 15); do docker exec engflow-redis redis-cli -n $i DBSIZE; done   # all 0
docker exec engflow-redis redis-cli --scan                  # 44 key names
docker exec engflow-redis redis-cli CONFIG GET maxmemory    # 268435456
docker exec engflow-redis redis-cli CONFIG GET maxmemory-policy   # allkeys-lru
docker exec engflow-redis redis-cli CONFIG GET databases     # 16
docker exec engflow-redis redis-cli INFO memory | grep -E 'used_memory:|used_memory_human|maxmemory|fragment|evicted'
docker exec engflow-redis redis-cli INFO stats  | grep -E 'keyspace_hit|keyspace_miss|evicted_keys|expired_keys|total_commands'
docker exec engflow-redis redis-cli INFO persistence | grep -E 'aof_enabled|rdb_last_bgsave_status|changes_since'
docker exec engflow-redis redis-cli SLOWLOG LEN              # 0
docker exec engflow-redis redis-cli CONFIG GET notify-keyspace-events   # ''
# per key:  TYPE <k> / TTL <k> / MEMORY USAGE <k> / SCARD <k> (sets) / GET <k> (strings)
# NOTE: DEBUG OBJECT is rejected on this build (rejected_calls=44 in commandstats) — use MEMORY USAGE.
```

---

## Priority roll-up (what to actually do first)

| rank | ID | why first |
|---|---|---|
| 1 | **B-01** | drop `lessons.content_original` → −72.5 MB (45% of DB), removes the LOB that C-03d drags onto the wire |
| 2 | **C-03a** | kill the unbounded `findAll()` on 43,737 rows — worst scaling risk, pure code fix |
| 3 | **B-04** | add the missing FK on `exercise_attempts.lesson_id` while orphans = 0 |
| 4 | **D-11** | repair 5 out-of-domain speaking scores; 0-10 contract is broken for 45% of graded rows |
| 5 | **C-SQL perf** | `exercises` clustered PK at **95.11% fragmentation** + 9-day-stale stats on the biggest table |
| 6 | **D-19/D-03** | 2,534 duplicate + 5,434 unanswerable exercises (12.4%) — content-integrity backlog, biggest number in the report |
| 7 | **B-02 / B-03 / B-11 / B-12** | housekeeping: 4 heaps + sysdiagrams + 1 redundant + 3 useless indexes ≈ 3 MB and less write amplification |
| 8 | **Section A** | `DBCC SHRINKFILE` after B-01 (65 MB reclaimable) — only *after*, never before |
