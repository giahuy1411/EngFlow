# Performance — audit-v9-full (2026-09-17)

Two probes, both measuring the SAME built artifact set the audit ships:
- `sweep/v8/p9_perf_probe_v9.js <label>` — 9 hot endpoints x 10 sequential HTTP calls + top statements by average logical reads from `sys.dm_exec_query_stats`.
- `sweep/v8/v9_perf_exercise_list.js <label> 651` — focused probe on the worst lesson (99 exercises, content 26 kB + content_original 77 kB).

Logical reads are the honest metric: they do not depend on a warm client or HTTP cache. Timings are medians of 10 runs.

## F108 — exercise list no longer hydrates the lesson LOBs

| Measurement | Before | After | Delta |
|---|---|---|---|
| GET /api/lessons/651/exercises (median of 8) | 153 ms | 29 ms | -124 ms |
| worst observed call | 293 ms | 110 ms | -183 ms |
| statement avg logical reads (v9 prose) | 1054 | 182 (new projection statement) | -872 |
| statement avg server ms | 15 | 0 | -15 |
| worst planned execution in cache | 197337 reads / 3250 ms | not re-planned | — |

The pre-fix statement (`select e1_0.exercise_id,…,l1_0.content,l1_0.content_original,… from exercises e1_0 join lessons l1_0`)
had **48 executions before and 48 after** the after-probe — it did not grow while the list was exercised again,
which is the evidence that the list path no longer runs it (the new projection statement ran 17 times).

Fix: `ExerciseLessonProjection` + `ExerciseRepository.findLessonExercisesProjection()`; `ExerciseService.getExercisesByLesson` maps the projection.
Regression test: `src/test/java/com/datn/engflow/controller/AuditV9ExerciseListProjectionTest.java` (2 tests: contract + ordering + answer hiding).

## Full hot-endpoint sweep (before vs after, median ms)

| Endpoint | Before | After |
|---|---|---|
| lesson exercises (guest) | 20 | 18 |
| lesson content (guest) | 16 | 22 |
| lesson detail (guest) | 11 | 15 |
| lesson list page0 | 15 | 22 |
| admin lessons page | 55 | 29 |
| admin exercises q | 53 | 60 |
| admin exercises keyword | 243 | 259 |
| vocab search | 9 | 11 |
| streak | 12 | 9 |

Interpretation: every endpoint stays within noise of its before value except the exercise list (F108).
`admin exercises keyword` (~243 -> ~259 ms) is the documented LIKE `%kw%` scan over 43,737 rows; AGENTS.md already establishes
no index can rescue a leading wildcard, so it is reported, not 'optimised'.

## Bundle

| Artifact | Baseline (17:39) | Final | Delta |
|---|---|---|---|
| `dist/assets/index-*.js` | 176.80 kB (gzip 67.38) | 176.80 kB (gzip 67.39) | 0 |
| `dist/assets/index-*.css` | 92.42 kB (gzip 15.91) | 85.22 kB (gzip 14.86) | -7.20 kB raw / -1.05 kB gzip |

The CSS delta is the dead-`.geo-*` removal (F113). The JS entry is unchanged: DOMPurify stayed on the lazy import path
(`utils/markdown.js` + `views/lessons/LessonContent.vue`), never `main.js`.

## What was measured and NOT changed (with the reason)

| Observation | Decision |
|---|---|
| 0-seek indexes (`idx_lessons_level`, `idx_decks_public`, `idx_prompts_*`, `idx_speaking_status`, `idx_video_attempts_status`, …) | NOT dropped. `sys.dm_db_index_usage_stats` only covers the window since the last SQL Server restart and the counter is reset by restarts; on a dataset this small a 0-seek index can still be chosen by a plan tomorrow. Dropping on this evidence would trade a real (if small) write cost for a plan regression risk with no measured win. |
| `lessons` filtered/pagination index on `order_index` shows 1188 scans | Keep: the published-list query orders by it. |
| Fragmentation: only `PK__lessons__…` at 11.6% / 121 pages | Below the 30% rebuild threshold; rebuilding 121 pages is churn. Reported, not executed. |
| Redis caching of read-hotspots | Not added. Every measured read path is single-digit-to-tens of ms with the plan-cache warm; adding a cache layer would add invalidation risk (the app writes lessons/exercises from the admin UI) for no measured win. The rate-limit keyspace stays the only Redis user in the request path. |

## AI path latency (real Ollama, not mocked)

`p11_enrich_reliability.js` (round 2): 10/10 word enrichments returned HTTP 200, min 925 ms / median 1156 ms / max 1549 ms.
`p3a` exercise generation: 36 probes, 0 fail; 4 validation cases (2 valid / 2 invalid refused).
These are the local-model numbers the AGENTS.md baseline documents; they are stable in this environment.
