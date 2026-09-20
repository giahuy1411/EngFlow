# Phase B — Database audit in Docker (read-only)

**Date:** 2026-09-20 (+07) · **DB:** `english_learning` on `engflow-sqlserver` (:1433)
**Probe:** `sweep/v11/db-audit.sql` → `sweep/v11/db-audit.out` · exit code **0** · **`Msg` count = 0**

Method: `docker exec -i engflow-sqlserver bash -c 'cd /opt/mssql-tools18/bin && ./sqlcmd … -i /dev/stdin'`.
No DML in this phase. Every batch output was scanned for `Msg \d+` — which caught three real probe bugs
(below), proving the rule earns its place.

---

## 0. Probe bugs this phase caught in ITSELF (recorded, per the "probe error counts as a defect" rule)

| Draft | Symptom | Cause | Fix |
|---|---|---|---|
| 1st | 8 × `Msg 207 Invalid column name 'id'` | I assumed every PK is `id`. It is not: `vocab_id`, `deck_id`, `lesson_id`, `user_id`, `attempt_id`, `deck_word_id` | queried `sys.columns` first, then rewrote every join |
| 2nd | 3 × `Msg 207 Invalid column name 'user_id'` | `lessons` has **no** author column; `decks` owner column is **`owner_id`**, not `user_id` | dropped the `lessons→users` orphan probe (the relation does not exist) and used `owner_id` |

Both drafts **exited 0** while failing. Without the `Msg \d+` scan they would have reported "clean".
Final run: **0 errors**.

---

## 1. Streak schema (T1.3) — deployed and correct

| Check | Result | Meaning |
|---|---|---|
| `study_policy` rows | **1** | Policy row present |
| `study_policy.effective_from` | **2026-09-20** | Cutover is today |
| `study_days` rows | **0** | No study day recorded yet (consistent with cutover = today) |
| FK `fk_study_days_user` → `users` | **1, `is_disabled=0`** | F124's fix held; the constraint exists AND is enabled |
| UQ `uq_study_days_user_date` | **1** | A duplicate `(user, date)` is impossible at the schema level |

**Confirms** audit-v10's F124 claim, independently: the FK is present and enabled in the live DB.

## 2. Constraint inventory (T1.1)

**26 foreign keys**, all `is_disabled = 0`. Notably:

- `exercise_attempts → users` (FK62kd…) — but **no FK from `exercise_attempts.lesson_id` to `lessons`**
  (the column is a plain `Long`, as the entity read predicted). This is the one relation the orphan scan
  has to check **by hand**, because the database will not.
- `vocabulary → lessons` (FKcv8t…) — nullable, which is why 126 deck-owned rows have `lesson_id NULL`.
- `video_attempts` has **two** FKs to `users` (`user_id` and `graded_by`) — distinct columns, so this is
  **correct modelling, not a duplicate constraint**. (I checked; it is not a finding.)

Unique indexes include `uq_study_days_user_date`, `UKbwm3hnc7qphev33xfbxsx6c9j` (deck_words),
`UK6dotkott2kjsp8vw4d0m25fb7` / `UKr43af9ap4edm43mmtq01oddj6` (users — email + username),
`UKcnc61y66y0f9p96e6j6qlbswl` (user_vocabulary_progress), and the **filtered** unique index
`UKlsp8jh693lih2txq7dl4bdnpx` on `payment_transactions` (`has_filter = 1`).

> The filtered index is the reason `AGENTS.md` requires `SET QUOTED_IDENTIFIER ON` on every DELETE
> against `payment_transactions` — without it SQL Server raises `Msg 1934` while `sqlcmd` still exits 0.

## 3. Orphan scan (T1.2) — written so a NULL FK cannot masquerade as an orphan

| Relation | Orphans | Checked by |
|---|---|---|
| `exercise_attempts → lessons` | **0** | hand-written `NOT EXISTS` (no DB FK exists) |
| `user_vocabulary_progress → vocabulary` | **0** | hand-written |
| `user_vocabulary_progress → users` | **0** | hand-written |
| `deck_words → decks` | **0** | hand-written |
| `deck_words → vocabulary` | **0** | hand-written |

NULL-FK counts, **reported separately** so they cannot be folded into the orphan number:

| Column | NULLs | Legitimate? |
|---|---|---|
| `exercise_attempts.lesson_id` | 0 | — |
| `user_vocabulary_progress.vocabulary_id` | 0 | — |
| `vocabulary.lesson_id` | **126** | **Yes** — deck-owned vocabulary has no lesson. This is exactly the row set a naive orphan query would miscount. |

**No orphans. No integrity violation.**

## 4. Row-level sanity (T1.4) — measured live

| Metric | Value |
|---|---|
| lessons total / published / draft | **1471 / 1465 / 6** |
| exercises total | **43 735** |
| exercises with empty `correct_answer` | **4 848** |
| exercises `LISTENING` missing `audio_url` | **0** |
| users total / premium | **72 / 6** |
| payment_transactions total | **127** |
| — of which `PENDING` with `transaction_id NULL` | **110** |
| decks total / system (no owner) | **14 / 10** |
| study_days rows | **0** |

Two notes that keep these numbers honest:

- **4 848 empty answers is a known, documented limitation**, not a new finding — `AGENTS.md` records
  that 586/5 434 were backfilled and the remainder are "MC-fragment scrape failures … ungradeable-by-
  design". It is re-measured here (4 848) and **matches** the documented figure.
- **`LISTENING` missing `audio_url` is now 0**, where `AGENTS.md` records 9. The v10 F128 fix
  (re-labelling 7 mislabelled rows + deleting 2) accounts for it. Recorded as a **confirmation of F128**,
  not as new work.

## 5. Integrity / duplicate risks (T1.4b)

| Check | Result | Verdict |
|---|---|---|
| users duplicate email | **0** | clean |
| users duplicate username | **0** | clean |
| study_days duplicate (user, date) | **0** | clean (and the UQ enforces it) |
| vocabulary duplicate (word, lesson_id) | **3** | **NOT a defect — probe artifact** |

### Why the 3 "duplicates" are not a defect

```
collaborate  lesson_id=NULL  vocab_id=10089 (OXFORD5000 → deck "Oxford 5000 (C1-C2)")
collaborate  lesson_id=NULL  vocab_id=10078 (TOEIC      → deck "Workplace English")
innovate     lesson_id=NULL  vocab_id=30124 (AI_GENERATED → no deck)
innovate     lesson_id=NULL  vocab_id=10094 (OXFORD5000 → deck "Oxford 5000 (C1-C2)")
negotiate    lesson_id=NULL  vocab_id=10038 (TOEIC      → deck "TOEIC 600 Essential Words")
negotiate    lesson_id=NULL  vocab_id=10083 (TOEIC      → deck "Workplace English")
```

The same English word legitimately appears in **more than one deck** (Oxford 5000 and Workplace English
are different vocabulary lists). Grouping by `(word, lesson_id)` collapses them because `lesson_id` is
NULL for all deck-owned rows. The correct key would include the deck. **Reported as a probe artifact so
it is not "fixed" by deleting real vocabulary.**

## 6. Index inventory on hot read paths (T1.5)

| Table | Indexes |
|---|---|
| `lessons` | PK, `idx_lessons_level`, `idx_lessons_order_index`, `idx_lessons_pub_level_order` |
| `exercises` | PK, `idx_exercises_lesson_order`, `idx_exercises_lesson_type_order`, `idx_exercises_type`, `idx_exercises_difficulty`, `IX_exercises_lesson_type_diff_order` |
| `vocabulary` | PK, `idx_vocabulary_lesson`, `idx_vocabulary_word_cefr` |
| `deck_words` | PK, UQ, `idx_deck_words_deck`, `idx_deck_words_vocab` |
| `payment_transactions` | PK, `idx_payment_transactions_user`, **filtered UQ** |
| `study_days` | PK, `uq_study_days_user_date` |

The hot read paths are covered. Note `idx_vocabulary_word_cefr` cannot rescue a **leading-wildcard**
`LIKE '%kw%'` — consistent with `AGENTS.md`'s measured ~185 ms on `/api/admin/exercises?q=`, and the
reason it says *not* to "optimise" that with an index.

## 7. Fragmentation (T1.5b) — nothing actionable

| Table | Index | Frag % | Pages |
|---|---|---|---|
| `lessons` | clustered PK | 11.57 | 121 |
| `exercises` | `IX_exercises_lesson_type_diff_order` | 10.92 | 293 |

Both are just over the 10 % threshold on a **121-page** and **293-page** index. Rebuilding these would
touch ~400 pages total and cannot plausibly move a read latency — so **declining to act**, with the
number recorded (per P5: no optimisation without a measurement that justifies it).

## Verdict

**Phase B: PASS with 0 findings against the product.** The database is internally consistent: no
orphans, no duplicate keys, all constraints enabled, streak schema deployed, and the two numbers that
differ from the older documentation (4 848 empty answers; 0 missing LISTENING audio) both **match known
fixes or known limitations** rather than being new defects. Three probe bugs were found in my own SQL by
the `Msg \d+` rule, and one apparent duplicate finding was falsified before it was written up.
