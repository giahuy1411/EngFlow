# EngFlow DB Audit — SQL Server 2019 + Redis (read-only)

**File**: `.specify/specs/audit-v7-full/db-audit.md` · built incrementally, one batch appended per query round.
**Method**: 7 batches, SELECT/DMV only. No INSERT/UPDATE/DELETE/CREATE/DROP/ALTER was executed. All DDL below is **proposal, not applied**.
**Access**: `docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<pw>' -d english_learning -C -I -W -s "|" -h -1 -i /tmp/bN.sql`

> **Reconciliation note vs. the earlier full audit** (`.specify/specs/audit-v7-full/subreports/db-audit.md`): the two biggest findings are *absent from the 7 batches scoped here*, so they are carried over and labelled as such rather than silently dropped:
> - `lessons.content_original` (nvarchar(max)) — **72,497,032 bytes ≈ 69.1 MiB = 34.7% of the 199.16 MB of reserved object space** (≈43% of `lessons`' 159.01 MB LOB allocation) — a completed one-time migration backup (batch 3 is scoped to `exercises`/`lessons` max columns, so this is re-measured there at §3.4/§3.5, not carried over).
> - `sys.dm_db_missing_index_details` returns **0 rows instance-wide** (batch 5) — reported honestly, not filled in.

---

## BATCH 1 — INVENTORY

### 1.1 Tables and row counts (24 tables, 46,705 rows total)

| table | rows |
|---|---|
| exercises | 43,737 |
| lessons | 1,471 |
| exercises_bak_v5 | 481 |
| exercises_bak_v5b | 322 |
| vocabulary | 127 |
| payment_transactions | 124 |
| deck_words | 100 |
| users | 75 |
| exercises_bak_v5c | 55 |
| exercises_bak_v5d | 39 |
| exercise_attempts | 38 |
| speaking_submissions | 28 |
| lesson_blocks | 16 |
| user_progress | 15 |
| video_attempts | 15 |
| user_vocabulary_progress | 14 |
| decks | 14 |
| lesson_sections | 11 |
| speaking_prompts | 6 |
| lesson_snapshots | 5 |
| video_lessons | 5 |
| lesson_submissions | 4 |
| sysdiagrams | 2 |
| user_streaks | 1 |

**TOTAL = 46,705 rows across 24 tables.**

Re-check:
```sql
SELECT t.name AS tbl, SUM(p.rows) AS [rows]
FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1)
GROUP BY t.name ORDER BY 2 DESC;
```

### 1.2 Size MB, top 15 by `reserved_page_count`

Columns from `sys.dm_db_partition_stats`: reserved = in-row + LOB + row-overflow.

| # | table | reserved MB | in-row MB | LOB MB | row-overflow MB |
|---|---|---|---|---|---|
| 1 | **lessons** | **161.08** | 2.00 | **159.01** | 0.07 |
| 2 | **exercises** | **27.12** | 26.98 | 0.07 | 0.07 |
| 3 | exercises_bak_v5 | 1.15 | 1.01 | 0.07 | 0.07 |
| 4 | exercises_bak_v5b | 0.46 | 0.32 | 0.07 | 0.07 |
| 5 | sysdiagrams | 0.40 | 0.14 | 0.26 | 0.00 |
| 6 | speaking_submissions | 0.35 | 0.35 | 0.00 | 0.00 |
| 7 | video_attempts | 0.35 | 0.35 | 0.00 | 0.00 |
| 8 | lesson_blocks | 0.34 | 0.14 | 0.20 | 0.00 |
| 9 | speaking_prompts | 0.28 | 0.28 | 0.00 | 0.00 |
| 10 | deck_words | 0.28 | 0.28 | 0.00 | 0.00 |
| 11 | lesson_snapshots | 0.27 | 0.14 | 0.13 | 0.00 |
| 12 | exercises_bak_v5d | 0.27 | 0.13 | 0.07 | 0.07 |
| 13 | exercise_attempts | 0.27 | 0.20 | 0.07 | 0.00 |
| 14 | vocabulary | 0.27 | 0.27 | 0.00 | 0.00 |
| 15 | payment_transactions | 0.21 | 0.21 | 0.00 | 0.00 |

**Headline: `lessons` (161.08 MB) + `exercises` (27.12 MB) = 188.20 MB of 199.16 MB total object bytes = 94.5%.**
`lessons` is **159.01 MB of LOB pages against only 2.00 MB in-row** — i.e. ~98.7% of that table lives in LOB alloc units, entirely explained by its `nvarchar(max)` columns (see Batch 3).

Re-check:
```sql
SELECT TOP 15 t.name AS tbl,
  CAST(SUM(ps.reserved_page_count)*8/1024.0 AS decimal(10,2)) AS reserved_mb,
  CAST(SUM(ps.in_row_reserved_page_count)*8/1024.0 AS decimal(10,2)) AS inrow_mb,
  CAST(SUM(ps.lob_reserved_page_count)*8/1024.0 AS decimal(10,2)) AS lob_mb,
  CAST(SUM(ps.row_overflow_reserved_page_count)*8/1024.0 AS decimal(10,2)) AS rvfo_mb
FROM sys.dm_db_partition_stats ps JOIN sys.tables t ON t.object_id=ps.object_id
GROUP BY t.name ORDER BY reserved_mb DESC;
```

### 1.3 Tables named in the brief that DO NOT EXIST

The audit brief asked for indexes on `flashcard_states`, `flashcards`, `srs`. Probed via `OBJECT_ID`:

| name | status |
|---|---|
| `flashcard_states` | **MISSING** |
| `flashcards` | **MISSING** |
| `srs` | **MISSING** |
| `user_vocabulary_progress` | EXISTS |
| `decks` | EXISTS |
| `deck_words` | EXISTS |

→ These three names are **not in this schema**. The SRS/flashcard feature is implemented as `user_vocabulary_progress` (SM-2 columns: `ease_factor`, `srs_interval`, `repetitions`, `review_count`, `mastery_level`, `next_review_date`) + `vocabulary` + `decks` + `deck_words`. Batch 4 therefore covers `user_vocabulary_progress` **in place of** `flashcard_states`/`srs`, and reports batch-7 SRS orphan checks against `user_vocabulary_progress`. No numbers were invented for the missing tables.

Re-check:
```sql
SELECT v AS tbl, CASE WHEN OBJECT_ID('dbo.'+v) IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS st
FROM (VALUES ('flashcard_states'),('flashcards'),('srs'),
             ('user_vocabulary_progress'),('decks'),('deck_words')) x(v);
```

---

## BATCH 2 — LEFTOVER BACKUP / JUNK TABLES

### 2.1 Matches on `%_bak%` / `%v5%` / `sysdiagrams`

`exercises_bak_v5`, `exercises_bak_v5b`, `exercises_bak_v5c`, `exercises_bak_v5d`, `sysdiagrams` — **5 tables, 4 backup + 1 SSMS artifact.**

```sql
SELECT name FROM sys.tables
WHERE name LIKE '%_bak%' OR name LIKE '%v5%' OR name='sysdiagrams' ORDER BY name;
```

### 2.2 Size, rows, structure (de-fanned-out; FK-reference count corrected in 2.4)

| table | rows | reserved MB | PK? | cols | has `lesson_id`? | created / last modified |
|---|---|---|---|---|---|---|
| exercises_bak_v5 | 481 | 1.148 | **NO (HEAP)** | 13 | yes | 2026-09-03 15:23:57 |
| exercises_bak_v5b | 322 | 0.461 | **NO (HEAP)** | 13 | yes | 2026-09-03 15:34:08 |
| exercises_bak_v5d | 39 | 0.273 | **NO (HEAP)** | 13 | yes | 2026-09-03 16:13:21 |
| exercises_bak_v5c | 55 | 0.211 | **NO (HEAP)** | **5** | **NO** | 2026-09-03 16:05:04 |
| sysdiagrams | 2 | 0.398 | yes (system) | 5 | — | 2026-09-04 00:48:01 |

- **Backup total = 2.093 MB / 897 rows.** All four are **heaps with no primary key** → no uniqueness, no update path, purely dump artifacts.
- The four were created in a **49-minute window on 2026-09-03** (15:23 → 16:13), i.e. four successive retry attempts of one exercise-regeneration run, each left behind.
- `exercises_bak_v5c` is **structurally divergent**: 5 columns and **no `lesson_id`**, unlike the other three. Any tooling that assumes `*_bak_*` is `exercises`-shaped breaks on it.
- `sysdiagrams` holds 2 rows, `definition` = 114,692 bytes each — SSMS DB-diagram artifact, not application data.

```sql
SELECT t.name AS tbl, SUM(CASE WHEN ps.index_id IN (0,1) THEN ps.row_count ELSE 0 END) AS [rows],
  CAST(SUM(ps.reserved_page_count)*8/1024.0 AS decimal(10,3)) AS reserved_mb
FROM sys.tables t JOIN sys.dm_db_partition_stats ps ON ps.object_id=t.object_id
WHERE t.name LIKE '%_bak%' OR t.name LIKE '%v5%' OR t.name='sysdiagrams'
GROUP BY t.name ORDER BY reserved_mb DESC;
```

### 2.3 Redundancy proof — 100% of every backup is subsumed by live `exercises`

| table | ids still present in live `exercises` | verdict |
|---|---|---|
| exercises_bak_v5 | **481 of 481** | fully redundant |
| exercises_bak_v5b | **322 of 322** | fully redundant |
| exercises_bak_v5c | **55 of 55** | fully redundant |
| exercises_bak_v5d | **39 of 39** | fully redundant |

**All 897 backup rows duplicate a `exercise_id` that already exists in the live table → zero unique data in these tables.**

```sql
SELECT COUNT(*) FROM exercises_bak_v5 b
WHERE EXISTS(SELECT 1 FROM exercises e WHERE e.exercise_id=b.exercise_id);
-- repeated per table; v5c/v5d in a UNION ALL
```

### 2.4 Nothing references them

FKs pointing at each: **0, 0, 0, 0, 0**. And they appear in **0 cached query plans** (no app statement in `sys.dm_exec_query_stats` names a `*_bak_*` table — verified in Batch 6 output).

> **Correction of my own earlier attempt:** a first run of this check reported "1 FK referencing each table". That was a `LEFT JOIN … COUNT(*)` bug — `COUNT(*)` counts the NULL-extended row. Restated as `COUNT(fk.object_id)` it is **0 for all five**, which is the correct number. Both the bug and the fix are recorded here rather than silently overwritten.

```sql
SELECT (t.name COLLATE DATABASE_DEFAULT) AS tbl, COUNT(fk.object_id) AS n_fks_referencing_this
FROM sys.tables t LEFT JOIN sys.foreign_keys fk ON fk.referenced_object_id=t.object_id
WHERE t.name LIKE '%_bak%' OR t.name LIKE '%v5%' GROUP BY t.name;
```

### 2.5 Action (proposal — NOT executed)

```sql
-- proposals only; take a real backup first (msdb shows 0 backups for this DB)
DROP TABLE dbo.exercises_bak_v5;
DROP TABLE dbo.exercises_bak_v5b;
DROP TABLE dbo.exercises_bak_v5c;
DROP TABLE dbo.exercises_bak_v5d;
EXEC sp_removediagrammingdatabase 'english_learning';   -- drops sysdiagrams (2 rows, 0.398 MB)
```
Net reclaim: **~2.5 MB** — small, but the value is removing 4 unindexed heaps with no PK that a future `SELECT *`-style migration or entity-scan job could silently pick up (Hibernate `ddl-auto=update` will not drop them, and they are not `@Entity`-mapped).

Also note: `DBCC SHRINKFILE` could reclaim ~65 MB (data file 264 MB allocated vs 199.16 MB used) but only **after** the Batch 3 `content_original` finding is addressed — shrinking first is an anti-pattern that just re-fragments.

---

## BATCH 3 — WIDE COLUMNS, ANSWER LENGTHS, MISSING AUDIO

### 3.1 `nvarchar(max)` / ≥4000 columns on `exercises` + `lessons` — 6 total

| table | column | type | declared bound | nullability |
|---|---|---|---|---|
| exercises | `question` | nvarchar | **UNLIMITED (max)** | NULL |
| exercises | `options` | nvarchar | **UNLIMITED (max)** | NULL |
| exercises | `explanation` | nvarchar | **UNLIMITED (max)** | NULL |
| lessons | `content` | nvarchar | **UNLIMITED (max)** | NULL |
| lessons | `content_original` | nvarchar | **UNLIMITED (max)** | NULL |
| lessons | `description` | nvarchar | **UNLIMITED (max)** | NULL |

No `ntext`/`text`/`image` legacy types. Note every one is **NULLable**, including `exercises.question` (which has 0 NULLs in data — see 3.6).

```sql
SELECT t.name, c.name, ty.name,
  CASE WHEN c.max_length=-1 THEN 'UNLIMITED(max)' ELSE CAST(CASE WHEN ty.name LIKE 'n%' THEN c.max_length/2 ELSE c.max_length END AS varchar(20)) END AS bound
FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id JOIN sys.types ty ON ty.user_type_id=c.user_type_id
WHERE t.name IN ('exercises','lessons')
 AND ((ty.name IN ('varchar','nvarchar') AND (c.max_length=-1 OR CASE WHEN ty.name LIKE 'n%' THEN c.max_length/2 ELSE c.max_length END >= 4000))
   OR ty.name IN ('text','ntext','image'));
```

### 3.2 `exercises.correct_answer` — length statistics (the headline DQ number)

| metric | value |
|---|---|
| total rows | 43,737 |
| MIN(LEN) | **0** |
| MAX(LEN) | **401** |
| AVG(LEN) | **12.97** |
| zero-length / NULL | **5,434 (12.42%)** |
| `COUNT(DISTINCT correct_answer)` | **4,130** |

**Distribution by length bucket:**
| bucket | rows |
|---|---|
| 0 (empty) | **5,434** |
| 1–10 | 22,647 |
| 11–50 | 13,604 |
| 51–200 | 2,032 |
| 201–400 | 19 |
| 401–500 (**near the 500-char cap**) | 1 |

Two findings:
1. **5,434 unusable answers.** `correct_answer` is `NOT NULL` in DDL, so the bad rows are **empty strings, not NULLs** — a constraint that is satisfied but meaningless. `MIN(LEN)=0` and 5,434 zero-length confirm both measures agree.
2. **The 500-char cap is at risk.** 1 row already sits at 401 chars and 19 more at 201–400. `MATCHING` exercises store a whole comma-joined pair list (`"l=r,..."` per the frontend contract) in this one column, so a long matching set will fail with a **truncation error at insert time** rather than being caught upstream.

```sql
SELECT COUNT(*) AS total, MIN(LEN(correct_answer)) AS min_len, MAX(LEN(correct_answer)) AS max_len,
  CAST(AVG(CAST(LEN(correct_answer) AS decimal(10,2))) AS decimal(10,2)) AS avg_len,
  SUM(CASE WHEN LEN(correct_answer)=0 OR correct_answer IS NULL THEN 1 ELSE 0 END) AS zero_len,
  COUNT(DISTINCT correct_answer) AS distinct_ans
FROM exercises;
```

Corroboration that the 5,434 are MC/FILL_BLANK only, and all 5,039 MC cases also lack `options`:
| exercise_type | rows with empty answer |
|---|---|
| MULTIPLE_CHOICE | 5,039 |
| FILL_BLANK | 395 |
```sql
SELECT exercise_type, COUNT(*) FROM exercises
WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer))='' GROUP BY exercise_type;
SELECT COUNT(*) FROM exercises WHERE exercise_type='MULTIPLE_CHOICE'
 AND LTRIM(RTRIM(ISNULL(correct_answer,'')))='' AND options IS NULL;   -- 5,039
```

### 3.3 `audio_url` NULL count grouped by `exercise_type`

| exercise_type | total | audio_url NULL | % NULL |
|---|---|---|---|
| MULTIPLE_CHOICE | 33,549 | 33,548 | 100.0 |
| FILL_BLANK | 9,112 | 9,112 | 100.0 |
| TRANSLATION | 377 | 377 | 100.0 |
| **LISTENING** | **367** | **9** | **2.5** |
| MATCHING | 332 | 332 | 100.0 |
| **all types** | 43,737 | **43,378** | 99.18 |

**The number that matters is 9, not 43,378.** `audio_url` is only meaningful for LISTENING; MC/FB/TRANSLATION/MATCHING legitimately have no audio. So the real gap is **9 of 367 listening exercises (2.5%)**.
This contradicts the project's recorded figure of "89/449 bài thiếu `audio_url`" in `AGENTS.md` — the gap has been closed to 9 since that note was written, and listening rows grew from 449→367 (a re-generation happened). **`AGENTS.md` is now stale on this point.**

```sql
SELECT exercise_type, COUNT(*) AS total, SUM(CASE WHEN audio_url IS NULL THEN 1 ELSE 0 END) AS audio_null,
  CAST(SUM(CASE WHEN audio_url IS NULL THEN 1 ELSE 0 END)*100.0/COUNT(*) AS decimal(6,1)) AS pct_null
FROM exercises GROUP BY exercise_type ORDER BY total DESC;
```

The 9 are concentrated in `difficulty='EASY'` (9 of 262 EASY listening; 0 of 27 MEDIUM; 0 of 78 NULL-difficulty), so the repair target is one identifiable batch, not a scatter:
```sql
SELECT difficulty, COUNT(*) AS total, SUM(CASE WHEN audio_url IS NULL THEN 1 ELSE 0 END) AS audio_null
FROM exercises WHERE exercise_type='LISTENING' GROUP BY difficulty;
```

### 3.4 Actual lengths of the max-columns (is `max` justified?)

**`exercises`:**
| column | measured MAX | measured AVG | verdict |
|---|---|---|---|
| `question` | 1,132 | 53.4 | → `nvarchar(2000)` |
| `options` | 1,546 | 44 (only 2,027 populated) | → `nvarchar(2000)` |
| `explanation` | 493 | 111 | → `nvarchar(1000)` |

**`lessons`:**
| column | rows | MAX chars | AVG chars | est. bytes | verdict |
|---|---|---|---|---|---|
| `content` | 1,467 | 33,657 | **8,607.1** | 25,253,266 (~24.1 MB) | unbounded is **justified** |
| `content_original` | 1,460 | 57,506 | **24,827.8** | **72,497,032 (~69.1 MB)** | **DELETE — see 3.5** |
| `description` | 1,469 | **108** | ~46 | — | → `nvarchar(255)` |

```sql
SELECT MAX(LEN(CAST(content AS nvarchar(max)))) AS content_max,
  CAST(AVG(CAST(LEN(content) AS decimal(12,2))) AS decimal(10,1)) AS content_avg,
  SUM(CAST(LEN(content) AS bigint))*2 AS content_bytes_est,
  MAX(LEN(CAST(content_original AS nvarchar(max)))) AS orig_max,
  CAST(AVG(CAST(LEN(content_original) AS decimal(12,2))) AS decimal(10,1)) AS orig_avg,
  SUM(CAST(LEN(content_original) AS bigint))*2 AS orig_bytes_est,
  SUM(CASE WHEN content_original IS NULL THEN 1 ELSE 0 END) AS orig_null,
  MAX(LEN(CAST(description AS nvarchar(max)))) AS desc_max
FROM lessons;
```

### 3.5 `content_original` — the single largest finding in this audit

- avg **24,827 chars ≈ 48.5 KB per lesson row**, total **~69.1 MB**.
  *(Unit note: 72,497,032 bytes = **72.5 MB decimal = 69.1 MiB**. The earlier full-report's "72.5 MB" and this file's "69.1 MB" are the same measurement in different units — no conflict. Percentages below use MiB over the `sp_spaceused`-derived 199.16 MB, so all figures here are MiB-consistent.)*
- `lessons` reserved = 161.08 MB, of which **159.01 MB is LOB** (Batch 1.2) → `content_original` + `content` **are** essentially the whole table's footprint, and `content_original` alone is **~2.9× `content`** (72,497,032 vs 25,253,266 bytes).
- It is a **completed one-time backup**: `config/HtmlCleanupMigration.java:64-67` writes it once (`if (lesson.getContentOriginal() == null) lesson.setContentOriginal(source);`) and the runner is `@ConditionalOnProperty(engflow.html-cleanup.enabled=true)` — i.e. off after the migration.
- 0 of 1,471 rows have `content_original = content`; 11 rows NULL. **`orig_eq_content = 0`** ⇒ it is pure historical raw HTML, no live role.
- `model/dto/projection/LessonListProjection.java:10` already documents that list queries deliberately avoid these LOBs — so read paths exclude it, but `exercises … JOIN lessons` in the query cache **does** select `l1_0.content, l1_0.content_original` (Batch 6) — the dead column is still being dragged onto the wire.

**Est. DB-size impact: dropping it takes `lessons` from 161.08 MB → ~92 MB, i.e. the whole database from ~199 MB used → ~130 MB (−35%).**

### 3.6 DDL proposals from Batch 3 (NOT executed)

```sql
-- P1: remove the dead 69 MB migration backup. Edit model/entity/Lesson.java:41-42 FIRST
-- (ddl-auto=update will otherwise re-add it). No Flyway file — project rule.
ALTER TABLE dbo.lessons DROP COLUMN content_original;

-- P2: relieve the 80%-consumed cap before it throws
ALTER TABLE dbo.exercises ALTER COLUMN correct_answer NVARCHAR(2000) NOT NULL;

-- P3: bound the small max-columns (measured MAX all < 2000)
ALTER TABLE dbo.exercises ALTER COLUMN question    NVARCHAR(2000) NOT NULL;
ALTER TABLE dbo.exercises ALTER COLUMN options     NVARCHAR(2000) NULL;
ALTER TABLE dbo.exercises ALTER COLUMN explanation NVARCHAR(1000) NULL;
ALTER TABLE dbo.lessons   ALTER COLUMN description NVARCHAR(255)  NULL;
```
Each is paired with an entity `@Column(columnDefinition=…)` edit; a DB-only change would let schema and model diverge.

---

## BATCH 4 — INDEXES & FK COVERAGE

> **Measurement window matters.** `sys.dm_os_sys_info.sqlserver_start_time = 2026-09-12 03:15:00` — every `user_seeks/scans/lookups/updates` below is over a **~3-hour** window on a dev box. "Unused" is therefore *evidence*, not proof; I separate "0 reads despite writes" (act) from "no stats row at all" (wait).
> The DB is also **live**: these numbers moved during the audit (e.g. `idx_exercises_lesson_type_order` went from 0 seeks/2 scans in the earlier pass to 2 seeks/10 scans here, and `deck_words.idx_deck_words_deck` from *no stats row* to 2 seeks/6 scans). Both passes are real; this batch reports the later one.

### 4.1 Indexes on the 5 tables of interest (20 indexes)

`flashcard_states`/`flashcards`/`srs` do not exist (Batch 1.3) → `user_vocabulary_progress` substituted.

**exercises (43,737 rows, 5 indexes):**
| index | keys | INCLUDE | seeks | scans | lookups | updates | last seek |
|---|---|---|---|---|---|---|---|
| `PK__exercise__C121418ED83A5E11` (CLUSTERED, U) | exercise_id | — | 13 | 52 | **17** | 3 | 06:10 |
| `idx_exercises_lesson_type_order` | lesson_id, exercise_type, order_index | **difficulty, correct_answer** | **2** | **10** | 0 | 3 | 05:48 |
| `idx_exercises_lesson_order` | lesson_id, order_index | — | 14 | 3 | 0 | 3 | 06:11 |
| `idx_exercises_difficulty` | difficulty | — | 1 | 11 | 0 | 3 | 05:43 |
| `idx_exercises_type` | exercise_type | — | **NO STATS** | — | — | — | **never** |

**speaking_submissions (28 rows, 5 indexes):** `idx_speaking_status[status]` 6 seeks/0 scans · `idx_speaking_submissions_user[user_id, submitted_at]` 5/2 · `idx_speaking_submissions_prompt[prompt_id, submitted_at]` 2/1 · `IX_speaking_submissions_graded_by[graded_by]` 0/4 · PK `[id]` 0/27/2 lookups. **All 5 in use** → healthy for a 28-row table, though 5 indexes on 28 rows is over-indexed by weight (see 4.6).

**decks (14 rows, 3):** PK `[deck_id]` 18/21 · `IX_decks_owner_id[owner_id]` 2/3 · `idx_decks_public[is_public]` **1 seek, 2 updates, 0 scans, 0 lookups** — but `is_public` has **2 distinct values over 14 rows**, so the seek is incidental.

**deck_words (100 rows, 4):** PK `[deck_word_id]` 0/52 · `idx_deck_words_deck[deck_id]` 2/6 · `idx_deck_words_vocab[vocab_id]` 0/2 · **`UKbwm3hnc7qphev33xfbxsx6c9j` [deck_id, vocab_id] — NO STATS (never touched)**.

**user_vocabulary_progress (14 rows, 3):** **`UKcnc61y66y0f9p96e6j6qlbswl` [user_id, vocabulary_id] 11 seeks** (busiest thing on this table) · PK `[id]` 0/5/11 lookups · `IX_uvp_vocabulary_id[vocabulary_id]` 0/1. **No index on `next_review_date`** despite SRS due-queries → Batch 5 proposal.

```sql
SELECT t.name AS tbl, i.name AS idx, i.type_desc, [keys], [incl], us.user_seeks, us.user_scans, us.user_lookups, us.user_updates
FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
LEFT JOIN sys.dm_db_index_usage_stats us ON us.object_id=i.object_id AND us.index_id=i.index_id AND us.database_id=DB_ID()
WHERE t.name IN ('exercises','speaking_submissions','decks','deck_words','user_vocabulary_progress') AND i.name IS NOT NULL;
```

### 4.2 FK columns WITHOUT a supporting index — **0 rows**

```sql
SELECT OBJECT_NAME(fk.parent_object_id)+'.'+COL_NAME(fc.parent_object_id,fc.parent_column_id) AS fk_col
FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=fk.object_id
WHERE NOT EXISTS (SELECT 1 FROM sys.index_columns ic
  WHERE ic.object_id=fk.parent_object_id AND ic.column_id=fc.parent_column_id AND ic.key_ordinal=1);
--> (0 rows affected)
```
**This is a genuinely good result:** all **24 FK** child columns have a leading index, so parent deletes will not table-scan. No FK-index remediation needed.

### 4.3 `*_id` columns with no FK constraint — exactly **1 real offender**

| column | note |
|---|---|
| **`exercise_attempts.lesson_id`** | `bigint NOT NULL`, has `idx_exercise_attempts_user_lesson`, **but no FK to `lessons`** |

After excluding primary keys (13 of the 14 raw matches) and the `*_bak*` heaps, one remains. Currently referentially clean (Batch 7 shows 0 orphans), but nothing prevents future orphans and `sp_fkeys`/tooling won't see the relationship.
```sql
-- proposal, validated by 0 orphans
ALTER TABLE dbo.exercise_attempts WITH CHECK
 ADD CONSTRAINT FK_exercise_attempts_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(lesson_id);
```
*(Full FK list: 24 constraints — `deck_words→decks`, `deck_words→vocabulary`, `decks.owner→users`, `exercise_attempts.user→users`, `exercises.lesson→lessons`, `lesson_blocks.section→lesson_sections`, `lesson_sections.lesson→lessons`, `lesson_snapshots.lesson→lessons`, `lesson_submissions.lesson→lessons`, `lesson_submissions.user→users`, `payment_transactions.user→users`, `speaking_prompts.lesson→lessons`, `speaking_submissions.graded_by→users`, `speaking_submissions.prompt→speaking_prompts`, `speaking_submissions.user→users`, `user_progress.lesson→lessons`, `user_progress.user→users`, `user_streaks.user→users`, `user_vocabulary_progress.user→users`, `user_vocabulary_progress.vocabulary→vocabulary`, `video_attempts.graded_by→users`, `video_attempts.user→users`, `video_attempts.video_lesson→video_lessons`, `vocabulary.lesson→lessons`. All `NO_ACTION` on delete.)*

### 4.4 UNUSED indexes — writes but zero reads (the actionable list)

| index | keys | updates | seeks | scans | distinct values / rows | verdict |
|---|---|---|---|---|---|---|
| **`speaking_prompts.idx_prompts_published`** | is_published | **34** | **0** | **0** | **1 / 6** | **DROP** — the column has a single value; index can never help |
| `speaking_prompts.idx_prompts_level` | level | 34 | 0 | 1 | 3 / 6 | DROP — 6 rows, 3 values |
| `user_progress.UK8sschjnhw7q49ml9th0urvo4b` | user_id, lesson_id | 1 | 0 | 0 | — | **KEEP** — UNIQUE constraint enforcing one-progress-per-user-lesson |
| `lessons.idx_lessons_level` | level | 9 | 0 | 1 | — | DROP — superseded by `idx_lessons_pub_level_order[is_published,level,order_index]` |
| `users.UKr43af9ap4edm43mmtq01oddj6` | username | 23 | 0 | 4 | 75 / 75 | **KEEP** — uniqueness guard (login uses `email`) |
| `users.UK6dotkott2kjsp8vw4d0m25fb7` | email | 23 | 0 | 30 | 75 / 75 | **KEEP** — this is the login path |
| `video_lessons.idx_video_lessons_level` | level, is_published | 2 | 0 | 4 | — | too new to conclude |
| `vocabulary.idx_vocabulary_word_cefr` | word | 2 | 0 | 14 | — | too new to conclude |

```sql
SELECT t.name+'.'+i.name AS idx, us.user_updates, us.user_seeks, us.user_scans, us.user_lookups
FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
LEFT JOIN sys.dm_db_index_usage_stats us ON us.object_id=i.object_id AND us.index_id=i.index_id AND us.database_id=DB_ID()
WHERE i.is_primary_key=0 AND i.name IS NOT NULL
 AND ISNULL(us.user_updates,0)>0 AND ISNULL(us.user_seeks,0)=0 AND ISNULL(us.user_lookups,0)=0 ORDER BY us.user_updates DESC;
```

**Cardinality proof for the two drops** (why they are structurally useless, not just idle):
```sql
SELECT 'speaking_prompts.is_published', COUNT(DISTINCT is_published), COUNT(*) FROM speaking_prompts; --> 1 distinct / 6 rows
SELECT 'speaking_prompts.level',        COUNT(DISTINCT level),        COUNT(*) FROM speaking_prompts; --> 3 distinct / 6 rows
SELECT 'exercises.difficulty',          COUNT(DISTINCT difficulty),   COUNT(*) FROM exercises;        --> 3 distinct / 43,737 rows
SELECT 'decks.is_public',               COUNT(DISTINCT is_public),    COUNT(*) FROM decks;            --> 2 distinct / 14 rows
```

### 4.5 Never-touched indexes with **no usage-stats row** — inconclusive, do not drop yet

`deck_words.UKbwm3hnc7qphev33xfbxsx6c9j` · `exercises.idx_exercises_type` · `payment_transactions.UKlsp8jh693lih2txq7dl4bdnpx` · `video_attempts.idx_video_attempts_status` · `video_attempts.idx_video_attempts_user`

```sql
SELECT t.name+'.'+i.name AS idx, us.user_updates, us.user_seeks, us.user_scans
FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
LEFT JOIN sys.dm_db_index_usage_stats us ON us.object_id=i.object_id AND us.index_id=i.index_id AND us.database_id=DB_ID()
WHERE i.is_primary_key=0 AND i.name IS NOT NULL
 AND ISNULL(us.user_seeks,0)=0 AND ISNULL(us.user_scans,0)=0 AND ISNULL(us.user_lookups,0)=0;
```
Three of the five are UNIQUE constraints — dropping them removes a data-integrity guarantee, so hold for a week of stats. `exercises.idx_exercises_type` (5 distinct values / 43,737 rows) is the only non-unique one and is a safe drop once confirmed idle.

### 4.6 Overlapping / redundant indexes

A strict key-prefix sweep across all 62 indexes found **exactly one** redundancy, and **zero** index pairs with an identical key list:

| smaller index | larger index that contains it | note |
|---|---|---|
| `deck_words.idx_deck_words_deck [deck_id]` | `UKbwm3hnc7qphev33xfbxsx6c9j [deck_id, vocab_id]` (UNIQUE) | the 1-col index is a strict prefix → redundant. It is currently *receiving* the traffic (2 seeks/6 scans) only because the UK has no stats row yet; both serve `where deck_id=@p`. |

```sql
-- proposal
DROP INDEX idx_deck_words_deck ON dbo.deck_words;
```
Explicitly **not** redundant: `exercises.idx_exercises_lesson_order [lesson_id, order_index]` is *not* a prefix of `idx_exercises_lesson_type_order [lesson_id, exercise_type, order_index]` (`exercise_type` sits between them), and both are separately used (14 and 2 seeks) — so this is a legitimate pair, not a duplicate.

### 4.7 Over-indexing on tiny tables (P3)

`speaking_submissions` carries **5 secondary indexes for 28 rows** (0.35 MB) and `video_attempts` 4 for 15 rows. Each adds write cost on every grading insert for no measurable read benefit at this size. Not worth dropping (they will matter at scale), but `idx_speaking_status[status]` and `idx_video_attempts_status[status]` are the two I'd revisit first, since `status` is 5-distinct over 28 rows.

---

## BATCH 5 — MISSING INDEXES

### 5.1 The missing-index DMVs are EMPTY — top-10 cannot be produced

| DMV | rows |
|---|---|
| `sys.dm_db_missing_index_details` **this DB** | **0** |
| `sys.dm_db_missing_index_details` **all DBs instance-wide** | **0** |
| `sys.dm_db_missing_index_groups` | 0 |
| `sys.dm_db_missing_index_group_stats` | 0 |
| `sys.dm_exec_cached_plans` (for contrast) | 560 |

```sql
SELECT (SELECT COUNT(*) FROM sys.dm_db_missing_index_details) AS details_all_dbs,
       (SELECT COUNT(*) FROM sys.dm_db_missing_index_details WHERE database_id=DB_ID()) AS details_this_db,
       (SELECT COUNT(*) FROM sys.dm_db_missing_index_groups) AS groups_all,
       (SELECT COUNT(*) FROM sys.dm_db_missing_index_group_stats) AS groupstats_all,
       (SELECT COUNT(*) FROM sys.dm_exec_cached_plans) AS cached_plans;
--> 0|0|0|0|560
```

**So the requested "top 10 by `avg_user_impact × user_seeks`" is genuinely unavailable — 560 cached plans but zero accumulated missing-index requests.** I am not filling this with invented impact scores. Cause: `sqlserver_start_time = 2026-09-12 03:15:00`, so ~3h of low dev traffic (`Redis total_commands_processed` 591, `instantaneous_ops_per_sec` 0); the optimizer has simply not been asked a question during this window that it lacked an index for.

**The proposals below are therefore derived from the real executed predicate shapes in Batch 6 plus the structural gaps in Batch 4** — a defensible substitute, arguably stronger, since it reflects queries that actually ran rather than optimizer guesses.

### 5.2 Proposals, ordered by measured cost of the query they serve (all **NOT executed**)

**M-1 | P2 — `user_vocabulary_progress`: SRS due-queue has no index at all.**
Batch 4 confirmed the table's only 3 indexes are PK`[id]`, `UK[user_id, vocabulary_id]`, `IX[vocabulary_id]` — nothing on `next_review_date`, the column a due-cards query must filter and sort by. 14 rows today, 12 already overdue (Batch 7) ⇒ guaranteed scan.
```sql
CREATE NONCLUSTERED INDEX IX_uvp_due
  ON dbo.user_vocabulary_progress (next_review_date, user_id)
  INCLUDE (vocabulary_id, mastery_level, repetitions, review_count, srs_interval, ease_factor);
```

**M-2 | P2 — `exercises`: the 4,769-reads/exec admin filter cannot seek on `difficulty`.**
The heaviest single-execution app query (Batch 6.1 row #2, and 6.2 row #1) is `lesson_id + exercise_type + difficulty + lower(question) LIKE '%%'` with `ORDER BY`. The existing prior-fix index keys `(lesson_id, exercise_type, order_index)` and carries `difficulty` only as **INCLUDE** — so `difficulty` is residually filtered, never sought, and `order_index` sits ahead of it, making sort-satisfaction and difficulty-filtering mutually exclusive.
```sql
CREATE NONCLUSTERED INDEX IX_exercises_lesson_type_diff_order
  ON dbo.exercises (lesson_id, exercise_type, difficulty, order_index)
  INCLUDE (question, options, explanation, audio_url, image_url, created_at, updated_at);
```
Do **not** drop `idx_exercises_lesson_type_order` when adding this (see Batch 8.1 — it is live and serving 12 scans) — it serves the `lesson_id+exercise_type+order_index` shape that M-2's extra key column would break.

**M-3 | P2 — `decks`: "my decks" sorts by `name, deck_id` but the owner index is single-column.**
Predicate shape verified live in the plan cache (not from the missing-index DMV). Full text of both cached deck-listing shapes, `ORDER BY` confirmed:
```sql
-- "my decks": 2 execs, 3 reads/exec
select top (@P0) d1_0.deck_id,d1_0.cefr_level,d1_0.created_at,d1_0.description,d1_0.is_public,d1_0.name,
  d1_0.owner_id,d1_0.source,d1_0.thumbnail_url,d1_0.updated_at
  from decks d1_0 where d1_0.owner_id=@P1
  and (@P2 is null or @P3='' or lower(d1_0.name) like lower(('%'+@P4+'%'))
       or lower(coalesce(d1_0.description,'')) like lower(('%'+@P5+'%')))
  order by d1_0.name, d1_0.deck_id;
-- public browse: 1 exec, 3 reads/exec, same ORDER BY, where d1_0.is_public=1
```
`IX_decks_owner_id[owner_id]` satisfies the filter only, forcing the sort; `is_public` has **no index at all**. Today this is trivially cheap at 14 rows; it matters because `AGENTS.md` records **3 new regression tests `DeckControllerMyDecksTest`** in the 314-test baseline, i.e. this is a covered, load-bearing endpoint that will grow.
```sql
CREATE NONCLUSTERED INDEX IX_decks_owner_name
  ON dbo.decks (owner_id, name, deck_id);
-- then the old single-col index becomes a strict prefix and can go:
-- DROP INDEX IX_decks_owner_id ON dbo.decks;
```

**M-4 | P2 — `payment_transactions`: PENDING poller uses the wrong leading column.**
Full cached text, verified in the plan cache: `select pt1_0.id,pt1_0.amount,pt1_0.content,pt1_0.created_at,pt1_0.gateway,pt1_0.order_code,pt1_0.plan_type,pt1_0.premium_expiry,pt1_0.status,pt1_0.transaction_id,pt1_0.user_id,pt1_0.webhook_raw from payment_transactions pt1_0 where pt1_0.status='PENDING' and pt1_0.created_at>=@P0 order by pt1_0.id desc` — **13 execs, 6 reads/exec, 78 total reads**. It is served by `idx_payment_transactions_user[user_id, status, created_at]` — **`user_id` first**, so a status-driven poll cannot seek it. Cheap at 124 rows; the structural problem is that **107 of 124 rows are permanently `PENDING`** (measured this batch), so the dead set grows monotonically and the poll's filter selectivity keeps worsening.
```sql
CREATE NONCLUSTERED INDEX IX_payment_status_created_id
  ON dbo.payment_transactions (status, created_at DESC, id DESC)
  INCLUDE (user_id, amount, plan_type, gateway, premium_expiry);
```
Honest caveat: `status` has only **2 distinct values across 124 rows** (measured directly: `SELECT COUNT(DISTINCT status) FROM payment_transactions` → 2), so this pays off by keeping the index small and covering `ORDER BY id DESC`, **not** by becoming seek-efficient. The real fix is a job to expire abandoned orders.

**M-5 | P3 — `user_progress` completed-filter lacks an index.**
Verified in cache: `select p1_0.… from user_progress p1_0 where p1_0.user_id=@P0 and p1_0.lesson_id in (@P1 , @P2 , …)` (4 execs / 2 reads; a 12-parameter variant at 1 exec / 2 reads) and `select count_big(p1_0.progress_id) … where p1_0.user_id=@P0 and p1_0.is_completed=1` (1 exec / 2 reads). All served by `UK_user_progress…[user_id, lesson_id]` — seek on `user_id`, filter the rest. At ≤2 reads/exec this is as cheap as it gets; **no action**, revisit only if the table grows and `is_completed` becomes selective.

**M-6 | NOT an index problem: the `LIKE '%%'` searches are non-sargable and no B-tree can fix them.**
`lower(title) like lower('%'+@p+'%')` OR'd across `title`/`description`/`category` (lessons, 3.29ms/578 reads × 14 execs) and the same shape on `exercises.question` are the reason `lessons.idx_lessons_pub_level_order[is_published, level, order_index]` records **9 updates but 0 seeks** while the query runs 13 times. The correct tool is full-text:
```sql
CREATE FULLTEXT CATALOG EngFlowCatalog AS DEFAULT;
CREATE FULLTEXT INDEX ON dbo.lessons (title, description, category)
  KEY INDEX PK__lessons__6421F7BE05A3095C WITH CHANGE_TRACKING AUTO;
CREATE FULLTEXT INDEX ON dbo.exercises (question, explanation)
  KEY INDEX PK__exercise__C121418ED83A5E11 WITH CHANGE_TRACKING AUTO;
-- then rewrite the repository predicate from LOWER(...) LIKE '%%' to CONTAINS(...)
```
Secondary benefit: an index-keyed `question` also stops `exercises.question nvarchar(max)` from being unusable in any index key (Batch 3.1).

> Correction in pass 2: at Batch 4 time this index showed 0 seeks; a later re-check measured **`idx_lessons_pub_level_order` = 3 seeks / 2 scans / 15 updates** while `idx_lessons_level` stayed at 0 seeks / 1 scan / 15 updates. The pub_level_order index *is* being sought for the published+level filter — the residual problem is narrower than "the search can't use an index": only the `LIKE '%…%'` OR-predicate is non-sargable, and full-text addresses exactly that while the existing composite index keeps serving the equality part. The DROP of `idx_lessons_level` in 5.3 stands (it is covered by the composite); full-text remains P3, not urgent.

**M-7 | P3 — audio-repair scan for the 9 listening rows (Batch 3.3) has no index.**
`WHERE exercise_type='LISTENING' AND audio_url IS NULL` — `idx_exercises_type[exercise_type]` has 5 distinct values over 43,737 rows so it is a scan + filter. Only worth it if the job becomes recurring:
```sql
CREATE NONCLUSTERED INDEX IX_exercises_listening_no_audio
  ON dbo.exercises (lesson_id, order_index)
  WHERE exercise_type = 'LISTENING' AND audio_url IS NULL;
```

### 5.3 DROP proposals (net −5 indexes, from Batch 4)

```sql
-- structurally useless regardless of window (Batch 4.4 cardinality proof):
DROP INDEX idx_prompts_published ON dbo.speaking_prompts;  -- is_published: 1 distinct value / 6 rows
DROP INDEX idx_prompts_level     ON dbo.speaking_prompts;  -- level: 3 distinct / 6 rows
DROP INDEX idx_lessons_level     ON dbo.lessons;           -- covered by idx_lessons_pub_level_order
DROP INDEX idx_deck_words_deck   ON dbo.deck_words;        -- strict prefix of the UNIQUE(deck_id,vocab_id)
-- after a week of stats confirms it idle:
DROP INDEX idx_exercises_type    ON dbo.exercises;         -- 5 distinct / 43,737 rows; no stats row yet
```
**KEEP despite zero seeks** (uniqueness enforcement, not performance): `users.UK…username`, `users.UK…email`, `user_progress.UK…user_id+lesson_id`, `deck_words.UK…deck_id+vocab_id`, `payment_transactions.UK…transaction_id`.

### 5.4 Index write-burden and totals

| metric | value |
|---|---|
| total indexes on user tables | **62** |
| primary keys | 20 |
| unique constraints | 6 |
| plain nonclustered | **36** |

Tables with >3 indexes: `exercises` 5 · `speaking_submissions` 5 · `video_attempts` 5 · `lessons` 4 · `speaking_prompts` 4 · `deck_words` 4.

### 5.5 Fragmentation & statistics — the one maintenance action with a measured payoff

| index | avg_fragmentation | pages | type |
|---|---|---|---|
| **`exercises.PK__exercise__C121418ED83A5E11`** | **95.11%** | 2,330 | CLUSTERED |
| `lessons.PK__lessons__6421F7BE05A3095C` | 10.92% | 119 | CLUSTERED |
| `exercises.idx_exercises_type` | 7.10% | 183 | NONCLUSTERED |
| `exercises.idx_exercises_difficulty` | 2.96% | 135 | NONCLUSTERED |
| `exercises.idx_exercises_lesson_order` | 2.80% | 143 | NONCLUSTERED |
| `exercises.idx_exercises_lesson_type_order` | 2.75% | 437 | NONCLUSTERED |
| `lessons.PK__lessons__6421F7BE05A3095C` (LOB level) | 0.00% | 14,954 | CLUSTERED |

The `exercises` clustered PK at **95.11% over 2,330 pages** inflates every full scan of that table by roughly its full page count — which is precisely what makes the 2,343-reads/exec `findAll()` (Batch 6) expensive. Its neighbours are all <8%, so this is targeted, not general decay.

Statistics: **every `exercises` and `lessons` index-stat is 9 days old** (2026-09-03 05:14) except `idx_exercises_lesson_type_order` at 8 days (2026-09-04 04:47) — that later timestamp independently corroborates the prior-fix index being added after the others.
```sql
SELECT TOP 10 (OBJECT_NAME(s.object_id) COLLATE DATABASE_DEFAULT)+'.'+(s.name COLLATE DATABASE_DEFAULT) AS statname,
  STATS_DATE(s.object_id, s.stats_id) AS last_updated,
  DATEDIFF(day, STATS_DATE(s.object_id,s.stats_id), SYSDATETIME()) AS age_days
FROM sys.stats s JOIN sys.tables t ON t.object_id=s.object_id
WHERE t.name IN ('exercises','lessons') AND s.name NOT LIKE '\_WA%' ESCAPE '\' ORDER BY last_updated;
```
```sql
-- proposals, not executed:
ALTER INDEX PK__exercise__C121418ED83A5E11 ON dbo.exercises REBUILD WITH (FILLFACTOR = 90, ONLINE = OFF);
UPDATE STATISTICS dbo.exercises WITH FULLSCAN;
UPDATE STATISTICS dbo.lessons  WITH FULLSCAN;
```

---

## BATCH 6 — SLOWEST CACHED QUERIES, BIG SCANS, BLOCKING

### 6.0 First: the raw view is contaminated by this audit's own probes

A naive `TOP 10 ORDER BY total_elapsed_time/execution_count` on this instance returns **my own audit queries** at the top — e.g. a duplicate-group `GROUP BY` over `exercises` at **2,621.52 ms / 467,424 reads**, and a `MAX(LEN())` sweep at 1,367.60 ms / 29,918 reads. These are artefacts of a 3-hour audit, not application behaviour, and are **excluded below**. (The 467,424-read query is also the single largest read count in the entire cache — it is mine.)

**Isolating genuine application SQL took two passes, and the first pass had a bug worth recording:** I matched Hibernate-generated SQL with `stmt LIKE '%_0.%'`. In T-SQL `LIKE`, **`_` is a single-character wildcard, not a literal underscore**, so `%_0.%` matched any text containing the digit `0` — leaking this audit's own probes into the "app-only" set. Corrected to `LIKE 'select %!_0.%' ESCAPE '!'`, which pins the real alias form (`e1_0`, `l1_0`).

Consequence: the row first reported at **52.16 ms / 2,343 reads** (`SELECT exercise_type, COUNT(*) … audio_url IS NULL …`) is **my own Batch 3.3 probe, not application SQL** and is removed below. The corrected table is authoritative.

**Corrected totals: 80 application plans in cache · 24,308 cumulative logical reads · max single statement 14,058 reads · max latency 265.95 ms/exec.**

### 6.1 Top 10 application cached statements by ms/execution — **PASS 1, superseded by 6.1b**

> ⚠ **Read 6.1b instead.** Pass 1 has two defects: row **#3 is this audit's own probe** (the `LIKE '%_0.%'` wildcard bug in 6.0), and rows **#5/#6 belong to plan entries that were replaced between passes** — a live-cache artefact, see the drift note in 6.1b.

| # | ms/exec | execs | total ms | reads/exec | total reads | **total_rows** | object | statement (first 160 chars) |
|---|---|---|---|---|---|---|---|---|
| 1 | **265.95** | 6 | 1,595.7 | **2,343** | 14,058 | **262,422** | `exercises` | `select e1_0.exercise_id,e1_0.audio_url,e1_0.correct_answer,e1_0.created_at,e1_0.difficulty,e1_0.exercise_type,e1_0.explanation,e1_0.image_url,e1_0.lesson_id,e1_` |
| 2 | 63.48 | 1 | 63.5 | **4,769** | 4,769 | 5 | `exercises`+`lessons` | `select top ( @P0 ) e1_0.exercise_id,e1_0.audio_url,… e1_0.l… where (@P1 is null or l1_0.lesson_id=@P2) and (… e1_0.exercise_type=@P5) and (… e1_0.difficulty=@P8) and (… lower(e1_0.question) like …)` |
| 3 | 52.16 | 1 | 52.2 | 2,343 | 2,343 | 5 | `exercises` | `SELECT exercise_type, COUNT(*) … audio_url IS NULL …` *(audit-adjacent shape; retained for transparency — 2,343 reads matches the full-scan cost)* |
| 4 | 13.34 | 1 | 13.3 | 2,350 | 2,350 | 1 | `exercises`+`lessons` | `select count_big(e1_0.exercise_id) from exercises e1_0 join lessons l1_0 on l1_0.lesson_id=e1_0.lesson_id where (@P0 is null or l1_0.lesson_id=@P1) and (@P2…` |
| 5 | 4.04 | 13 | 52.6 | 121 | 1,573 | 13 | `lessons` | `select count_big(*) from lessons l1_0 where l1_0.is_published=1 and (@P0 is null or l1_0.level=@P1) and (… lower(l1_0.title) like l…` |
| 6 | 3.29 | 14 | 46.1 | 578 | 8,102 | 162 | `lessons` | `select top (@P0) l1_0.lesson_id,l1_0.title,l1_0.description,l1_0.level,l1_0.category,l1_0.duration_minutes,l1_0.thumbnail_url,l1_0.audio_url,l1_0.skill_type,l…` |
| 7 | 2.60 | 1 | 2.6 | 121 | 121 | 1 | `lessons` | `select count_big(l1_0.lesson_id) from lessons l1_0 where (@P0 is null or l1_0.level=@P1) and (… lower(l1_0.title) like lower(('%'+…` |
| 8 | 1.52 | 1 | 1.5 | 3 | 3 | 1 | `speaking_prompts` | `select top (@P0) sp1_0.id,sp1_0.attempt_limit,sp1_0.category,… where sp1_0.is_premium=0 and sp1_0.is_published=1 order by sp1_0.order_index,sp1_0.id` |
| 9 | 1.34 | 4 | 5.4 | 20 | 80 | 2 | `lessons` | `select distinct l1_0.lesson_id,l1_0.audio_url,l1_0.category,l1_0.content,l1_0.content_original,l1_0.created_at,l1_0.description,… ` |
| 10 | 1.29 | 2 | 2.6 | 6 | 12 | 25 | `speaking_submissions` | `select ss1_0.id,ss1_0.admin_feedback,ss1_0.assessment_error,ss1_0.assessment_provider,ss1_0.feedback,ss1_0.graded_at,ss1_0.graded_by,ss1_0.media_object_key,ss1_…` |

```sql
;WITH q AS (SELECT qs.execution_count, qs.total_elapsed_time/1000.0/NULLIF(qs.execution_count,0) AS ms_exec,
  qs.total_logical_reads/NULLIF(qs.execution_count,0) AS rd_exec, qs.total_logical_reads AS tot_rd,
  qs.total_rows, qp.dbid, CAST(SUBSTRING(st.text,(qs.statement_start_offset/2)+1,
   ((CASE WHEN qs.statement_end_offset=-1 THEN DATALENGTH(st.text) ELSE qs.statement_end_offset END - qs.statement_start_offset)/2)+1) AS varchar(max)) AS stmt
  FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
  CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle) qp)
SELECT TOP 12 ms_exec, execution_count, rd_exec, tot_rd, total_rows, LEFT(stmt,160)
FROM q WHERE dbid=DB_ID() AND stmt LIKE '%_0.%' AND stmt NOT LIKE '%sys.%' AND stmt NOT LIKE 'WITH %'
ORDER BY ms_exec DESC;
```

**Reading of this table (the important part):**

1. **Row #1 is the `findAll()` problem, and `total_rows` proves it.** 262,422 rows returned over 6 executions = **exactly 43,737 rows per execution** — the whole `exercises` table, every time, with all 13 columns including `question`/`options`/`explanation`. Source is `ExerciseService.java:196` (`exerciseRepository.findAll()` when `lessonId == null`), and the reason it exists is that the keyword filter is applied **in Java after loading** rather than in SQL — while `getAdminExercisePage` right below it already does it correctly via `findAdminPage`. **This is a code fix, not an index fix; no index can make a 13-column × 43,737-row projection cheap.**
2. **Row #2 is the worst per-execution read cost (4,769)** and is exactly the predicate M-2 addresses.
3. **Rows #5/#6/#7 are the non-sargable search trio** (M-6) — cheap now (≤4ms) only because `lessons` is 1,471 rows.
4. **Row #9 confirms the LOB leak flagged in Batch 3.5**: `select distinct l1_0.…, l1_0.content, l1_0.content_original …` — an `exercises`→`lessons` path selecting the 69 MB dead column onto the wire.
5. Nothing here is slow in absolute terms. Max app latency 266ms on a dev box; **there is no latency emergency** — the findings are about growth trajectory.

### 6.1b Top 10 application cached statements — **PASS 2, AUTHORITATIVE** (escaped alias filter, app SQL only)

| # | ms/exec | execs | reads/exec | total reads | **total_rows** | object | statement (first 150 chars) |
|---|---|---|---|---|---|---|---|
| 1 | **265.95** | 6 | 2,343 | **14,058** | **262,422** | `exercises` | `select e1_0.exercise_id,e1_0.audio_url,e1_0.correct_answer,e1_0.created_at,e1_0.difficulty,e1_0.exercise_type,e1_0.explanation,e1_0.image_url,e1_0.les…` — **no WHERE clause** |
| 2 | **63.48** | 1 | **4,769** | 4,769 | 5 | `exercises`+`lessons` | `select top (@P0) e1_0.… join lessons … where (@P1 is null or l1_0.lesson_id=@P2) and (…e1_0.exercise_type=@P5) and (…e1_0.difficulty=@P8) and (…lower(e1_0.question) like …)` |
| 3 | 13.34 | 1 | 2,350 | 2,350 | 1 | `exercises`+`lessons` | `select count_big(e1_0.exercise_id) from exercises e1_0 join lessons l1_0 on l1_0.lesson_id=e1_0.lesson_id where (@P0 is null or l1_0.lesson_id=@P1) …` — COUNT twin of #2 |
| 4 | 4.90 | 1 | 121 | 121 | 1 | `lessons` | `select count_big(*) from lessons l1_0 where l1_0.is_published=1 and (@P0 is null or l1_0.level=@P1) and (@P2 is null or @P3='' or lower(l1_0.title) like …` |
| 5 | 2.60 | 1 | 121 | 121 | 1 | `lessons` | `select count_big(l1_0.lesson_id) from lessons l1_0 where (@P0 is null or l1_0.level=@P1) and (… lower(l1_0.title) like lower(('%'+ …` |
| 6 | 2.36 | 1 | 137 | 137 | 1 | `exercises` | `select count_big(*) from exercises e1_0` |
| 7 | 1.90 | 1 | 10 | 10 | 1 | `lessons` | `insert into lessons (audio_url,category,content,**content_original**,created_at,description,duration_minutes,is_published,level,order_index,skill_type,thu…` |
| 8 | 1.52 | 1 | 3 | 3 | 1 | `speaking_prompts` | `select top (@P0) sp1_0.id,sp1_0.attempt_limit,sp1_0.category,… where sp1_0.is_premium=0 and sp1_0.is_published=1 order by sp1_0.order_index, sp1_0.id` |
| 9 | 1.44 | 1 | 3 | 3 | 6 | `speaking_prompts` | `select sp1_0.id,sp1_0.attempt_limit,… from speaking_prompts sp1_0 where sp1_0.is_premium=0 and sp1_0.is_published=1 …` |
| 10 | 1.34 | 4 | 20 | 80 | 2 | `lessons` | `select distinct l1_0.lesson_id,l1_0.audio_url,l1_0.category,**l1_0.content,l1_0.content_original**,l1_0.created_at,l1_0.description,l1_0.duration_minutes,…` |

```sql
;WITH q AS (SELECT qs.execution_count, qs.total_elapsed_time/1000.0/NULLIF(qs.execution_count,0) AS ms_exec,
  qs.total_logical_reads/NULLIF(qs.execution_count,0) AS rd_exec, qs.total_logical_reads AS tot_rd, qs.total_rows, qp.dbid,
  CAST(SUBSTRING(st.text,(qs.statement_start_offset/2)+1,
   ((CASE WHEN qs.statement_end_offset=-1 THEN DATALENGTH(st.text) ELSE qs.statement_end_offset END - qs.statement_start_offset)/2)+1) AS varchar(max)) AS stmt
  FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle) qp)
SELECT TOP 10 ms_exec, execution_count, rd_exec, tot_rd, total_rows, LEFT(stmt,150)
FROM q WHERE dbid=DB_ID()
 AND (stmt LIKE 'select %!_0.%' ESCAPE '!' OR stmt LIKE '(@P%!_0.%' ESCAPE '!' OR stmt LIKE 'insert into %')
 AND stmt NOT LIKE '%sys.%' AND stmt NOT LIKE '%mssqljdbc%' AND stmt NOT LIKE '%jdbc_temp%' AND stmt NOT LIKE 'WITH %'
ORDER BY ms_exec DESC;
```

**Live-cache drift between the two passes (why the two tables differ beyond my filter bug).** The same statements measured ~30 minutes apart changed rank because plan entries are replaced under memory pressure (target 1,031 MB; see §8.3.2):

| statement | pass 1 | pass 2 |
|---|---|---|
| lesson-list `select top (@P0) l1_0.lesson_id,l1_0.title,…` | 3.29 ms × 14 execs, 578 r/e, 8,102 total, 162 rows | **0.40 ms × 3 execs, 46 r/e, 138 total, 36 rows** |
| `speaking_submissions` select | 1.29 ms × 2, 25 rows | dropped out of top 10 |
| `select e1_0.… from exercises` (filtered variant) | absent | **1.14 ms × 4, 136 r/e, 544 total, 16 rows** |

The lesson-list entry did not merely age — `execution_count` *fell* from 14 to 3, which is only possible if that plan handle was evicted and a fresh one cached. **Treat every `dm_exec_query_stats` figure in this report as "as of 2026-09-12 ~06:20", not as a cumulative truth for the instance's life.**

**Reading of the authoritative table:**

1. **Row #1 is the `findAll()` problem, and `total_rows` proves it:** 262,422 rows over 6 executions = **exactly 43,737 rows/execution** — the entire `exercises` table, all 13 columns, `question`/`options`/`explanation` included. Source `ExerciseService.java:196` (`exerciseRepository.findAll()` when `lessonId == null`); it exists because the keyword filter is applied **in Java after loading**, while `getAdminExercisePage` just below already does it correctly via `findAdminPage`. **Code fix, not index fix — no index makes a 13-column × 43,737-row projection cheap.**
2. **Row #1 alone = 14,058 of the 24,308 cumulative application reads = 57.8%.** One repository call accounts for over half of every logical read this app has issued since the 03:15 restart.
3. **Row #2 is the worst per-execution cost (4,769 reads)** — precisely the predicate M-2 targets.
4. **Rows #4/#5 are the non-sargable search COUNT twins** (M-6): 121 reads each, cheap only because `lessons` is 1,471 rows.
5. **Row #7 puts `content_original` on the write path** of every lesson insert — the dead 69 MB column (Batch 3.5) isn't just storage, it's serialised on create.
6. **Row #10 confirms the LOB leak** flagged in Batch 3.5: a `distinct` select pulling `l1_0.content, l1_0.content_original` onto the wire. Two further cached shapes do the same (`select top (@P0) l1_0.…content, content_original…` 0.59 ms/20 r/e; `select l1_0.…content, content_original…` 0.50 ms × 3).
7. **Nothing here is slow in absolute terms** (max 266 ms on a dev box, 0 blocking, 0 app queries over 100k reads). **There is no latency emergency — every finding is about growth trajectory**, chiefly #1.

### 6.2 Top 5 by logical reads/execution (application only, pass 2)

| reads/exec | total reads | execs | ms/exec | rows | statement |
|---|---|---|---|---|---|
| **4,769** | 4,769 | 1 | 63.48 | 5 | `select top (@P0) e1_0.… join lessons … lesson_id/exercise_type/difficulty/lower(question) like '%%'` |
| **2,350** | 2,350 | 1 | 13.34 | 1 | `select count_big(e1_0.exercise_id) from exercises e1_0 join lessons l1_0 …` |
| **2,343** | **14,058** | 6 | 265.95 | **262,422** | `select e1_0.… from exercises e1_0` — **unbounded `findAll()`, largest cumulative reader** |
| **137** | 137 | 1 | 2.36 | 1 | `select count_big(*) from exercises e1_0` |
| **136** | 544 | 4 | 1.14 | 16 | `select e1_0.… from exercises e1_0 …` (filtered variant, same 13-column shape) |

By **cumulative** reads the `findAll()` ranks first by a wide margin (14,058 vs 4,769) — that is the one to fix.

### 6.3 Scans > 100k logical reads — **threshold NOT breached by any application query**

```sql
;WITH q AS (SELECT qs.total_logical_reads AS tot_rd, qp.dbid, CAST(SUBSTRING(st.text,1,300) AS varchar(max)) AS stmt
 FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st CROSS APPLY sys.dm_exec_query_plan(qs.plan_handle) qp)
SELECT SUM(tot_rd) AS app_total_reads, MAX(tot_rd) AS app_max_single,
       SUM(CASE WHEN tot_rd>100000 THEN 1 ELSE 0 END) AS over_100k, COUNT(*) AS n_app_plans
FROM q WHERE q.dbid=DB_ID() AND (q.stmt LIKE 'select %!_0.%' ESCAPE '!' OR q.stmt LIKE '(@P%!_0.%' ESCAPE '!'
  OR q.stmt LIKE 'insert into %') AND q.stmt NOT LIKE '%sys.%' AND q.stmt NOT LIKE '%mssqljdbc%';
--> app_total_reads=24308 | app_max_single=14058 | over_100k=0 | n_app_plans=80
```
**Application maximum = 14,058 reads; application queries over 100,000 = 0.** Only two statements in the whole cache exceed 100k, and neither is application SQL:

| reads | attribution |
|---|---|
| 467,424 | **this audit's** duplicate-`GROUP BY` probe over `exercises` |
| 120,148 | SQL Server internal scalar-inlining telemetry (`sys.sql_modules`) |

### 6.4 Blocking — **0, as expected**

| metric | value |
|---|---|
| `dm_os_waiting_tasks` with a blocking session | **0** |
| `dm_exec_requests WHERE blocking_session_id <> 0` | **0** |
| user sessions | 12 |
| active transactions | 7 |

```sql
SELECT (SELECT COUNT(*) FROM sys.dm_os_waiting_tasks WHERE blocking_session_id IS NOT NULL
          AND blocking_session_id<>@@SPID AND session_id<>@@SPID) AS blocking_reqs,
       (SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process=1) AS user_sessions,
       (SELECT COUNT(*) FROM sys.dm_tran_active_transactions) AS open_txns,
       (SELECT COUNT(*) FROM sys.dm_exec_requests WHERE blocking_session_id<>0) AS currently_blocked;
--> 0|12|7|0
```
No contention. (The only lock waits in `sys.dm_os_wait_stats` are `LCK_M_S` × 4 / 1,043 ms cumulative, which for a 3-hour idle window is almost certainly background activity queued behind my own full-LOB scans of `lessons` — not an application signal.)

---

## BATCH 7 — DATA QUALITY

### 7.1 Core row counts (re-measured in this batch)

| table | COUNT(*) |
|---|---|
| users | **75** |
| lessons | **1,471** |
| exercises | **43,737** |
| decks | 14 |
| deck_words | 100 |
| vocabulary | 127 |
| user_vocabulary_progress | 14 |
| speaking_submissions | 28 |

Note `users` = **75** here vs 72 in the very first pass of this session: the DB is **live and receiving inserts during the audit**, so all counts are "as of 2026-09-12 ~06:15".

### 7.2 Orphan rows — **0 across all 18 parent/child pairs**

| child → parent | orphans |
|---|---|
| exercises → lessons | **0** |
| deck_words → decks | **0** |
| deck_words → vocabulary | **0** |
| decks.owner_id → users | **0** |
| user_vocabulary_progress → users | **0** |
| user_vocabulary_progress → vocabulary | **0** |
| vocabulary → lessons | **0** |
| exercise_attempts → lessons | **0** |
| exercise_attempts → users | **0** |
| speaking_submissions → speaking_prompts | **0** |
| speaking_submissions → users | **0** |
| user_progress → users | **0** |
| user_progress → lessons | **0** |
| user_streaks → users | **0** |
| lesson_sections → lessons | **0** |
| lesson_blocks → lesson_sections | **0** |
| video_attempts → video_lessons | **0** |
| payment_transactions → users | **0** |

**TOTAL ORPHANS (10 core pairs summed in one query): 0.**
Note the brief asked for "srs→decks" and "flashcards→users": neither table exists (Batch 1.3), so I checked the real SRS implementation `user_vocabulary_progress` → `users` and → `vocabulary` instead (both 0). `user_vocabulary_progress` does not reference `decks` at all in this schema.
```sql
SELECT 'exercises -> lessons' AS pair, COUNT(*) AS orphans FROM exercises c
WHERE NOT EXISTS(SELECT 1 FROM lessons p WHERE p.lesson_id=c.lesson_id);
-- same NOT EXISTS pattern repeated per pair; plus a UNION ALL sum
SELECT SUM(x.n) FROM (SELECT COUNT(*) AS n FROM exercises c WHERE NOT EXISTS(SELECT 1 FROM lessons p WHERE p.lesson_id=c.lesson_id)
  UNION ALL SELECT COUNT(*) FROM deck_words c WHERE NOT EXISTS(SELECT 1 FROM decks p WHERE p.deck_id=c.deck_id)
  UNION ALL SELECT COUNT(*) FROM deck_words c WHERE NOT EXISTS(SELECT 1 FROM vocabulary p WHERE p.vocab_id=c.vocab_id)
  UNION ALL SELECT COUNT(*) FROM user_vocabulary_progress c WHERE NOT EXISTS(SELECT 1 FROM users p WHERE p.user_id=c.user_id)
  UNION ALL SELECT COUNT(*) FROM user_vocabulary_progress c WHERE NOT EXISTS(SELECT 1 FROM vocabulary p WHERE p.vocab_id=c.vocabulary_id)
  UNION ALL SELECT COUNT(*) FROM speaking_submissions c WHERE NOT EXISTS(SELECT 1 FROM speaking_prompts p WHERE p.id=c.prompt_id)
  UNION ALL SELECT COUNT(*) FROM speaking_submissions c WHERE NOT EXISTS(SELECT 1 FROM users p WHERE p.user_id=c.user_id)
  UNION ALL SELECT COUNT(*) FROM exercise_attempts c WHERE NOT EXISTS(SELECT 1 FROM lessons p WHERE p.lesson_id=c.lesson_id)
  UNION ALL SELECT COUNT(*) FROM user_progress c WHERE NOT EXISTS(SELECT 1 FROM users p WHERE p.user_id=c.user_id)
  UNION ALL SELECT COUNT(*) FROM user_streaks c WHERE NOT EXISTS(SELECT 1 FROM users p WHERE p.user_id=c.user_id)) x;
--> total_orphans = 0
```
This is consistent with Batch 4.2/4.3: 24 FKs cover every relationship **except** `exercise_attempts.lesson_id`, which nonetheless has 0 orphans today. Referential integrity is currently intact by convention, not by enforcement.

### 7.3 `speaking_submissions.score_total` outside the 0-10 scale — **5 rows**

| metric | value |
|---|---|
| rows total | 28 |
| `score_total` NULL | **17 (60.7%)** |
| in range 0–10 | **6** |
| **> 10 (out of range)** | **5** |
| negative | **0** |
| MIN / MAX | 0.0 / **27.0** |

```sql
SELECT COUNT(*) AS rows_total, SUM(CASE WHEN score_total IS NULL THEN 1 ELSE 0 END) AS null_score,
 SUM(CASE WHEN score_total BETWEEN 0 AND 10 THEN 1 ELSE 0 END) AS in_range,
 SUM(CASE WHEN score_total>10 THEN 1 ELSE 0 END) AS over_10,
 SUM(CASE WHEN score_total<0 THEN 1 ELSE 0 END) AS negative, MIN(score_total) AS mn, MAX(score_total) AS mx
FROM speaking_submissions;
--> 28|17|6|5|0|0.0|27.0
```

**Root cause proven arithmetically — these are pre-fix SUMs, not averages:**
| id | user | prompt | score_total | fluency | grammar | vocabulary | **dim sum** | **dim avg** | status |
|---|---|---|---|---|---|---|---|---|---|
| 2 | 2 | 3 | **12.0** | 3 | 3 | 3 | 9 | 3.00 | NULL |
| 30006 | 3 | 3 | **19.0** | 6 | 7 | 6 | **19** | 6.33 | COMPLETED |
| 40008 | 3 | 50007 | **13.0** | 3 | 4 | 6 | **13** | 4.33 | FAILED |
| 40010 | 3 | 50007 | **27.0** | 9 | 10 | 8 | **27** | 9.00 | COMPLETED |
| 40011 | 3 | 50007 | **27.0** | 8 | 10 | 9 | **27** | 9.00 | COMPLETED |

**`score_total == score_fluency + score_grammar + score_vocabulary` exactly on 4 of 5 rows** ⇒ written by the old sum branch, before commit `754ab16 feat(speaking): normalize AI score to 0-10 average scale across UI and API`. Post-fix rows confirm the new behaviour: 40015→9.0 (avg of 8,10,9), 40016/17/18→9.7 (avg of 9,10,10), 40023→9.0. Row `id=2` (12.0 vs dims 3,3,3) is internally inconsistent on top of it.
Crucially **`score_total > 100` is 0** — so this is *not* a 0-100 vs 0-10 unit mix-up, purely sum-vs-average.

Scale verified from source, as the brief required: `model/entity/SpeakingSubmission.java:85-86` maps `@Column(name="score_total") private Double scoreTotal`, and `service/assessment/SpeakingRubricResult.java:27-29` returns `Math.round((grammar+vocabulary+fluency)/3.0*10.0)/10.0` documented "between 0.0 and 10.0"; `service/SpeakingSubmissionService.java:122` assigns `outcome.rubric().total()`. `AGENTS.md` agrees: "scoreTotal speaking: thang 0-10 (Double), DB column `score_total` là float".
```sql
SELECT id,user_id,prompt_id,score_total,score_fluency,score_grammar,score_vocabulary,
 CAST(score_fluency+score_grammar+ISNULL(score_vocabulary,0) AS int) AS dim_sum,
 CAST((score_fluency+score_grammar+ISNULL(score_vocabulary,0))/3.0 AS decimal(6,2)) AS dim_avg, status
FROM speaking_submissions WHERE score_total>10 OR score_total<0 ORDER BY id;
```
```sql
-- proposal, NOT executed: recompute from stored dimensions rather than a blanket clamp
UPDATE dbo.speaking_submissions
   SET score_total = ROUND((score_fluency + score_grammar + score_vocabulary)/3.0, 1)
 WHERE score_total > 10
   AND score_fluency IS NOT NULL AND score_grammar IS NOT NULL AND score_vocabulary IS NOT NULL;
```

Related: `pronunciation_accuracy/completeness/fluency/prosody` are **28/28 NULL** and `score_pronunciation` is **27/28 NULL** — five entirely dead columns; and **9 rows sit in a terminal `COMPLETED`/`GRADED` state with `score_total` NULL**, i.e. the learner sees no result.
```sql
SELECT COUNT(*) FROM speaking_submissions WHERE status IN ('COMPLETED','GRADED') AND score_total IS NULL;  --> 9
```

### 7.4 Future / NULL dates — **and a timezone finding that re-labels two of them**

| check | value |
|---|---|
| `user_streaks.study_date` in future | **0** |
| `user_streaks.study_date` NULL | **0** (column is NOT NULL) |
| `user_progress.last_accessed` > DB clock | **1** ⚠ |
| `user_progress.completed_at` future | 0 |
| `user_progress` `is_completed=1` but `completed_at` NULL | 0 |
| `users.last_study_date` future | 0 |
| `users.last_study_date` NULL | **59 / 75** |
| `users.premium_expiry` past while `is_premium=1` | 0 |
| `user_vocabulary_progress.next_review_date` > 1 year out | **1** ⚠ |
| `user_vocabulary_progress.next_review_date` NULL | 0 |
| `exercises.created_at` / `lessons.created_at` future | 0 / 0 |
| `decks.created_at` NULL | 0 |

**⚠ Finding A — the 1 "future" `last_accessed` is a TIMEZONE ARTIFACT, not corrupt data.** Measured directly:
| clock | value |
|---|---|
| `engflow-sqlserver` container | `2026-09-12 **06:19:33** UTC` |
| `engflow-backend` container | `2026-09-12 **13:19:33**` (= **UTC+7**, Asia/Ho_Chi_Minh) |
| SQL `SYSDATETIME()` vs `SYSUTCDATETIME()` | identical → **offset 0 (SQL clock is UTC)** |

The row is `progress_id 20009, user 3, lesson 445, last_accessed 2026-09-12 12:42:42`, **23,010 s = 6.4 h** ahead of the DB clock — almost exactly the missing 7 hours. Corroborated by `MAX(users.updated_at) = 2026-09-12 12:48:47`, a write that happened minutes ago in real time.
⇒ **The application writes naive local (UTC+7) timestamps into `datetime2` columns while SQL Server's own clock is UTC.** So "future date" tests are systematically **false positives** for every app-written `datetime2`, and the DB's `created_at`/`updated_at`/`completed_at`/`last_accessed` are all ~7 h "in the future" relative to server time.
This is a real design issue worth its own line item, but it is **not** DQ corruption and I will not report it as such:
- `datetime2` stores no zone, so correctness depends on every writer agreeing. `AGENTS.md` implies mixed origins (backend container local time vs `GETDATE()`-style server time in any SQL-side default), and `lessons`/`exercises` `created_at` came from a Java writer (UTC+7) while any DB-side default would be UTC.
- Day-granularity `date` columns (`user_streaks.study_date`, `users.last_study_date`, `users.ai_quota_date`, `premium_expiry`) are computed in Java from `LocalDate.now()`; if any scheduler uses `LocalDate.now(ZoneOffset.UTC)` (the `StreakReminderScheduler` takes a `Clock`), the daily quota/streak boundary shifts by 7 hours — i.e. a streak can flip at 17:00 local. That is the concrete risk, and it explains why a user with `last_study_date = today` has a 77-day-old streak row (§7.5).
```sql
-- proposal: make the storage unambiguous (largest change, do deliberately)
ALTER TABLE dbo.user_progress ALTER COLUMN last_accessed DATETIMEOFFSET(7) NULL;  -- and siblings
-- cheaper alternative: standardise ALL writers on UTC (backend TZ or Instant.now()) and convert at the edge.
```
```sql
docker exec engflow-sqlserver date -u "+%Y-%m-%d %H:%M:%S"
docker exec engflow-backend date "+%Y-%m-%d %H:%M:%S"
SELECT SYSDATETIME(), SYSUTCDATETIME(), DATEDIFF(minute, SYSUTCDATETIME(), SYSDATETIME()) AS tz_offset_minutes;
SELECT progress_id,user_id,lesson_id,last_accessed, DATEDIFF(second, SYSDATETIME(), last_accessed) AS secs_ahead
FROM user_progress WHERE last_accessed>SYSDATETIME();
```

**⚠ Finding B — the 1 far-future `next_review_date` IS a genuine bug.**
```
user_vocabulary_progress id=20002, user 2, vocab 10017
next_review_date = 2031-11-24  →  1,899 days ahead (5.2 years)
srs_interval = 1914 (days) | ease_factor = 2.6 | repetitions = 8 | review_count = 16 | mastery_level = 2
```
SM-2 with 8 repetitions and ease 2.6 yields an interval of weeks, **not 1,914 days** — `review_count 16` with only `repetitions 8` suggests repeated interval multiplication without a reset on lapses (an interval that compounds every review). Effect: this card never reappears in the due queue. Table range is `MIN 2026-07-05` → `MAX 2031-11-24`; 12 of 14 rows are already due.
```sql
-- action: cap the interval on write (SM-2 practice) and repair the one outlier row.
-- proposal, NOT executed:
UPDATE dbo.user_vocabulary_progress SET srs_interval = 30, next_review_date = DATEADD(day,30,SYSDATETIME())
 WHERE id = 20002;
-- and add the guard so it cannot recur:
ALTER TABLE dbo.user_vocabulary_progress ADD CONSTRAINT CK_uvp_interval CHECK (srs_interval IS NULL OR srs_interval BETWEEN 0 AND 365);
```
```sql
SELECT id,user_id,vocabulary_id,next_review_date,DATEDIFF(day,SYSDATETIME(),next_review_date) AS days_ahead,
       mastery_level,repetitions FROM user_vocabulary_progress WHERE next_review_date>DATEADD(year,1,SYSDATETIME());
--> 20002|2|10017|2031-11-24 05:35:20|1899|2|8      (and srs_interval=1914, ease_factor=2.6, review_count=16)
```

### 7.5 Streak data is effectively absent — one row, 77 days stale

`user_streaks` contains exactly **1 row**: `streak_id 1, user_id 3, study_date 2026-06-27, games_played 2, words_studied 4` → **77 days old**, while `users.current_streak = 1` and `users.last_study_date = 2026-09-12` for that same user.
⇒ **`user_streaks` and `users.current_streak`/`last_study_date` disagree**, because the streak mechanism now lives in **Redis** (`user:login_days:<id>` sets — see Batch 8.2 / `StreakService.java`), leaving `user_streaks` as a near-abandoned table with 1 row and 0.14 MB. Either resume writing it or drop it; a 77-day-stale table that looks authoritative is the worst middle state. Its `games_played`/`words_studied` columns belong to the removed achievements/games feature.

### 7.6 Coverage gaps

| metric | value | re-check SQL |
|---|---|---|
| users with **zero** `user_progress` rows | **70 of 75 (93%)** | `SELECT COUNT(*) FROM users u WHERE NOT EXISTS(SELECT 1 FROM user_progress p WHERE p.user_id=u.user_id)` |
| lessons with **zero** exercises | **5 of 1,471** | `SELECT COUNT(*) FROM (SELECT l.lesson_id FROM lessons l LEFT JOIN exercises e ON e.lesson_id=l.lesson_id WHERE e.lesson_id IS NULL GROUP BY l.lesson_id) z` |
| `exercise_attempts` rows | 38 | `SELECT COUNT(*) FROM exercise_attempts` |
| ├ `total <= 0` (vacuous 0/0 submissions) | **4** | `SELECT COUNT(*) FROM exercise_attempts WHERE total<=0` |
| ├ `score > total` | 0 | `SELECT COUNT(*) FROM exercise_attempts WHERE score>total` |
| └ `percentage` outside 0–100 | 0 | `SELECT COUNT(*) FROM exercise_attempts WHERE percentage NOT BETWEEN 0 AND 100` |

70-of-75 zero-progress is expected for a seeded dev DB (most of the 75 users are test accounts created by integration runs — `newuser@test.com`, `testuser99@test.com`, `tddtest@test.com`, and `users` grew 72→75 during this audit), **not** a data-integrity fault. It does mean any analytics query over `user_progress` describes 5 users, not 75.

### 7.7 Cross-reference to Batch 3 (the two largest DQ numbers)

| metric | value |
|---|---|
| exercises with empty `correct_answer` | **5,434 of 43,737 (12.42%)** |
| LISTENING exercises missing `audio_url` | **9 of 367 (2.5%)** |
| all-types `audio_url` NULL (mostly by design) | 43,378 |
| duplicate exercise groups / rows | 629 groups / **2,534 rows (5.8%)** |
| MULTIPLE_CHOICE rows with `options` NULL | **32,814 of 33,549 (97.8%)** |

The `AGENTS.md` line "Listening exercises: 89/449 bài thiếu `audio_url`" is **stale — measured 9/367 now**, and the listening population itself changed (449→367), so a re-generation ran since that note. Worth updating the rules file.

---

## BATCH 8 (supplementary) — PRIOR-FIX VERIFICATION, REDIS, CAVEATS

The 7-batch scope did not include the prior-fix index or Redis, so they are confirmed here with a fresh pass.

### 8.1 `idx_exercises_lesson_type_order` — **EXISTS, correctly defined, IN USE**

| property | measured |
|---|---|
| table | `dbo.exercises` |
| name | `idx_exercises_lesson_type_order` (index_id 14) |
| type | NONCLUSTERED, `is_unique = 0` |
| **key columns** | **`lesson_id, exercise_type, order_index`** |
| **INCLUDE** | **`difficulty, correct_answer`** |
| **`user_seeks`** | **2** |
| **`user_scans`** | **12** |
| `user_lookups` / `user_updates` | 0 / 3 |
| `last_user_seek` | 2026-09-12 **05:48:54** |
| `last_user_scan` | 2026-09-12 **06:20:48** |
| cached plans referencing it | **15** |
| stats auto-updated | 2026-09-04 04:47:17 (8 d) — 9 days later than every other `exercises` stat |
| fragmentation | 2.75% over 437 pages (healthy) |

**Verdict: the pass criterion "user_seeks/scans > 0" is MET on both counters (seeks=2, scans=12), with `last_user_scan` timestamped minutes before this measurement** — it is being used *right now*, not historically. Definition is a byte-exact match to the audit-v6 spec (`REPORT.md:44`: `(lesson_id, exercise_type, order_index) INCLUDE (difficulty, correct_answer)`).

**Two honest caveats:**
1. It is used for its **covering** property, not seek efficiency — `user_lookups = 0` with 12 scans means the engine scans the narrow index instead of doing key lookups, which is exactly what `INCLUDE (difficulty, correct_answer)` buys. The v6 claim "7ms → 0ms" could not be reproduced (no matching workload in this window); what is provable is presence, correct definition, 15 live plans, and fresh scans.
2. **It does not serve the heaviest admin filter.** That query adds `difficulty` to the predicate, and `difficulty` is an INCLUDE column (not seekable) with `order_index` sitting before it — hence proposal **M-2**. The prior fix is working for its intended shape; the residual cost is a *different* shape.

```sql
SELECT i.name, i.type_desc, us.user_seeks, us.user_scans, us.user_lookups, us.user_updates,
       us.last_user_seek, us.last_user_scan
FROM sys.indexes i LEFT JOIN sys.dm_db_index_usage_stats us
  ON us.object_id=i.object_id AND us.index_id=i.index_id AND us.database_id=DB_ID()
WHERE i.name='idx_exercises_lesson_type_order';

SELECT COUNT(*) FROM sys.dm_exec_cached_plans cp CROSS APPLY sys.dm_exec_query_plan(cp.plan_handle) pq
 WHERE pq.dbid=DB_ID()
   AND pq.query_plan.exist('//*[local-name()="Index" or local-name()="Object"][contains(@Index,"idx_exercises_lesson_type_order")]')=1;
```

### 8.2 Redis — 49 keys, **0 without TTL**, 1.80 MB of 256 MB

Re-measured at the end of the audit (grew from 44 keys earlier — the box is live):

| prefix | keys | TTL state |
|---|---|---|
| `user:login_days:*` | **28** | 77–90 d (`LOGIN_DAYS_TTL_DAYS = 90`) |
| `streak:comeback:*` | **12** | 28.3 d (`COMEBACK_SUPPRESSION_TTL = 30d`) |
| **`game:session:*`** | **5** | 83,964–83,965 s ≈ **23.3 h** (`GAME_SESSION_TTL = 24h`) — *newly appeared mid-audit* |
| `dictionary::*` | **2** | ~44 min (Spring `@Cacheable`, `${cache.ttl-hours:1}`) |
| `streak:reminder:*` | **2** | 7.3 h / 31 h (`REMINDER_MARKER_TTL = 2d`) |

| metric | value |
|---|---|
| `DBSIZE` | 49 (db0; db1–15 = 0) |
| `expires` | **49 → 100% TTL coverage; per-key `TTL` loop found 0 keys returning -1** |
| `used_memory_human` | **1.80 MB** / `maxmemory` 256 MB = **0.70%** |
| `mem_fragmentation_ratio` | 3.37 (was 5.10 earlier — falling as the dataset grows, i.e. overhead artefact, not a leak) |
| `evicted_keys` | **0** |
| `expired_keys` | **17** (was 4 — active expiry working; 13 keys aged out during the audit) |
| `maxmemory-policy` | `allkeys-lru` |
| SLOWLOG | LEN 0 |

`game:session:<uuid>` values are `GameSessionRedisDTO` JSON (`userId`, `deckId`, `gameType:"QUIZ"`, `totalQuestions:10`, a 10-entry `answerMap`) — matching `AGENTS.md`'s "deck quiz server-side (`GameService.generateQuiz`, Redis, cap 10)". **The cap is enforced in the value (10 answers), not the key count, and each session is one key with a 24 h TTL** ⇒ bounded. No unbounded list/set/zset found: the only collection type is the `user:login_days` SETs, hard-capped at ~90 members because `StreakService.java:161-164` re-issues `expire()` on every `SADD`.

One real (non-memory) consequence worth flagging: a 91-day absence silently deletes a user's login history and therefore their streak. `AGENTS.md`'s baseline note also confirms `game:*`/`rate_limit:*`/`otp:*` prefixes exist in `RedisConstants.java`; `rate_limit`/`login_fail`/`otp` had **0 keys** at audit time (no failed logins or OTPs in this window).

```bash
docker exec engflow-redis redis-cli DBSIZE                       # 49
docker exec engflow-redis redis-cli INFO keyspace                # db0:keys=49,expires=49
for k in $(docker exec engflow-redis redis-cli --scan); do docker exec engflow-redis redis-cli TTL $k; done   # all > 0
docker exec engflow-redis redis-cli INFO memory | grep -E 'used_memory_human|maxmemory_human|fragmentation'
docker exec engflow-redis redis-cli INFO stats  | grep -E 'evicted_keys|expired_keys'
```

### 8.3 Measurement caveats — read before re-running this audit

1. **The database is LIVE throughout.** Observed drift during a ~50-minute audit: `users` 72 → 75, Redis keys 44 → 49, `expired_keys` 4 → 17, `idx_exercises_lesson_type_order` scans 2 → 12 / plans 11 → 15, `idx_lessons_level` 0 scans → 1, `idx_deck_words_deck` no-stats-row → 2 seeks/6 scans. **Any single read of a DMV here is a snapshot, not a truth.** Re-runs will not reproduce exact numbers; the re-check SQL is what is stable.
2. **`sys.dm_db_index_operational_stats` is NOT reliable on this instance.** I measured `exercises.PK` at `range_scan_count = 296` early in the session and `= 2` later, while `sys.dm_db_index_usage_stats` on the same index rose monotonically (seeks 1→13, scans 6→62). A restart would explain it, but is ruled out: container `StartedAt = 2026-09-12T03:14:44Z`, `RestartCount = 0`, `sqlserver_start_time` unchanged. `AUTO_CLOSE`/`AUTO_SHRINK` are both **0** on `english_learning`. Most likely cause is eviction of the per-HoBt stats memory under buffer-pool pressure (documented: target 1,031 MB, granted 515 MB, host available 661 MB). ⇒ **Section 8.1's conclusion rests on `usage_stats` (monotonic, corroborated by 15 cached plans), not on `operational_stats`.**
3. **Missing-index DMVs contribute nothing** (Batch 5.1: 0 rows even instance-wide), so every index proposal here is inferred from executed predicates. They are *hypotheses with measured motivation*, not optimizer-confirmed gaps — validate with actual execution plans before creating.
4. **`*_bak_*` tables are heaps with no PK**, so any `sys.partitions`-based row-count query against them is an approximation; `COUNT(*)` was used for the redundancy proofs (Batch 2.3) and those are exact.
5. Two of my own SQL errors during this batch run are documented inline where they occurred (§2.4 `COUNT(*)` vs `COUNT(fk.object_id)`, and the `INCLUDE`-subquery / `qs.objectid` column-source failures in Batches 4 and 6, which were fixed and re-run rather than reported). The `AGENTS.md` warning about stale `target/surefire-reports` XML inflating counts is the DB analogue of the same trap: **filter by what you're actually measuring, not by row presence.**

### 8.4 Priority order for remediation

| rank | item | batch | payoff |
|---|---|---|---|
| 1 | Drop `lessons.content_original` (edit `Lesson.java:41-42` first) | 3.5 | **−69 MB, −35% of DB** |
| 2 | Replace `ExerciseService.java:196 findAll()` with the paged path | 6.1b | removes **57.8% of all application logical reads** (14,058 / 24,308); worst growth risk |
| 3 | Add FK on `exercise_attempts.lesson_id` while orphans = 0 | 4.3 | closes the only unenforced relationship |
| 4 | Fix the UTC / UTC+7 timestamp mismatch (§7.4A) | 7.4 | correctness: streak & quota day boundaries |
| 5 | Repair 5 out-of-range `score_total` + cap SRS `srs_interval` | 7.3 / 7.4B | learner-visible: 27/10 scores, a card lost for 5.2 years |
| 6 | Rebuild `exercises` clustered PK (95.11%) + `UPDATE STATISTICS` (9 d old) | 5.5 | directly reduces the 2,343-reads/exec scan |
| 7 | Drop 3 useless indexes + 4 backup heaps + `sysdiagrams` | 4.4 / 2 | −2.5 MB, less write amplification, fewer traps |
| 8 | Update stale `AGENTS.md` listening figure (89/449 → 9/367) | 3.3 | stops future agents chasing a closed issue |


