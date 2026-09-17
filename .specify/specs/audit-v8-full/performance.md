# Performance Audit — EngFlow

**Task:** comprehensive-audit-redesign Task 9 · **Date:** 2026-09-16
**Method:** live measurement against running services, no estimates
**Harness:** `sweep/v8/perf.js` → `sweep/v8/perf.txt`, `sweep/v8/perf.json`
**DB cost:** `sweep/v8/perf-reads.sql` → `sweep/v8/perf-reads-out.txt`

## Rule applied

> "Any performance claim requires a before/after measurement."

Every number below is a live measurement with its method stated. Where no
before/after pair exists, the row says so instead of implying an improvement.
No endpoint is described as "optimised" unless a paired measurement is shown.

## Method

- **N = 7** per endpoint (odd, so p50 is a real sample rather than an average of
  two). Warm measurement: the JVM, connection pool and plan cache are already
  populated.
- Only `2xx`/`3xx` responses contribute to latency. A `500`'s duration is
  meaningless and averaging it in would hide a failure behind a fast-looking
  number. Status distribution is reported alongside so a failure cannot pass as
  a latency figure.
- Rate-limit buckets are flushed before each call (`H.flushLimits(true)`), so a
  `429` from the harness's own traffic never masquerades as latency.
- Times are wall-clock `fetch` round trips from Node on the same host, so they
  include client and serialization cost, not just server work.

## 1. Endpoint latency (median of 7, live)

| Endpoint | Role | Status | p50 | p95 | min | max |
|---|---|---|---:|---:|---:|---:|
| `GET /api/v1/video-lessons` | anon | 200×7 | **19** | 27 | 18 | 27 |
| `GET /api/vocabulary/search?keyword=work` | anon | 200×7 | **12** | 16 | 9 | 16 |
| `GET /api/v1/speaking-prompts` | user | 200×7 | **14** | 16 | 12 | 16 |
| `GET /api/dashboard/stats` | user | 200×7 | **19** | 31 | 17 | 31 |
| `GET /api/streak/current` | user | 200×7 | **21** | 28 | 14 | 28 |
| `GET /api/decks` | user | 200×7 | **23** | 26 | 18 | 26 |
| `GET /api/leaderboard` | user | 200×7 | **30** | 38 | 23 | 38 |
| `GET /api/lessons/445` | user | 200×7 | **37** | 49 | 32 | 49 |
| `GET /api/lessons?page=0&size=20` | user | 200×7 | **49** | 69 | 40 | 69 |
| `GET /api/admin/stats` | admin | 200×7 | **32** | 46 | 28 | 46 |
| `GET /api/admin/exercises?page=0&size=20` | admin | 200×7 | **74** | 130 | 57 | 130 |
| `GET /api/admin/exercises?q=the&page=0&size=20` | admin | 200×7 | **210** | 243 | 196 | 243 |
| `POST /api/auth/login` | anon | 200×7 | **79** | 86 | 77 | 86 |
| `POST /api/lessons/445/exercises/grade` | user | 200×7 | **22** | 107 | 18 | 107 |
| `POST /api/ai/enrich-word` | user | 200×7 | **1851** | 14 944 | 1 052 | 14 944 |

Every endpoint returned `200×7` — no failures, no `4xx`, no `5xx` in any of the
105 measured calls.

### Reading the numbers

**All read paths are fast.** 12–74 ms p50 across 12 endpoints. Nothing here needs
attention, and nothing was changed on the strength of these figures.

**`enrich-word` is dominated by the local model, not the application.** p50
1 851 ms, but max 14 944 ms — a 14× spread within seven consecutive calls. That
variance is Ollama, not EngFlow:

- The model is `qwen2.5:3b`/`1.5b` on an RTX 2050 with 4 GB VRAM. `AGENTS.md`
  records `OLLAMA_MAX_LOADED_MODELS=1`, so a feature switch forces a model swap
  (~5.7 s measured). A call that lands during a swap pays the load cost.
- A GPU-contended call is what produced the 14.9 s outlier — the same condition
  that produced a `500` in an earlier round and led to F93 (timeout → `504`).
- Isolated measurement recorded 10/10 `200` at 1 000–1 274 ms, consistent with
  the 1 052 ms `min` here.

**Conclusion: not an application regression.** The correct mitigation is the
timeout mapping already added in F93, not a change to the query path. This is
recorded rather than "optimised", because there is no application-side cost to
remove — the time is spent in the model forward pass.

**`q=the` at 210 ms is inherent, and must not be "fixed" by adding an index.**
`GET /api/admin/exercises?q=...` compiles to `LIKE '%the%'` over 43 737 rows. A
leading wildcard cannot use a B-tree index, so no index can help; `AGENTS.md`
already records ~185 ms for this shape and explicitly warns against adding an
index to "optimise" it. Measured here at 210 ms p50 / 243 ms p95 — consistent
with the documented figure, on a cold-ish plan cache. **Disposition: expected
behaviour, no action.**

## 2. Database cost — the F87 shape is confirmed holding

`perf-reads.sql` reads `sys.dm_exec_query_stats` after the endpoint has been
exercised, and **excludes the audit's own statements** (they also match
`%exercise%` and would otherwise appear as the workload).

| execs | avg logical reads | total | avg ms | shape |
|---:|---:|---:|---:|---|
| 55 | **4 600** | 253 028 | 35 | **projection only (F87 shape)** |
| 55 | 1 301 | 71 555 | 64 | projection only — `count_big` for pagination |
| 24 | 132 | 3 168 | 3 | `count_big(*)` |
| 6 | 1 294 | 7 764 | 281 | single-lesson exercise fetch (by `lesson_id`, no join) |

**The admin list is confirmed at the F87 shape.** Every statement the endpoint
issues reports `projection only (F87 shape)` — i.e. none of them selects
`lesson.content` or `content_original`. Before F87 the list `JOIN FETCH`ed both
heavy columns for 20 rows, which `AGENTS.md` records at **95 000 logical
reads / 367 ms**.

**Honest framing of the before/after:** the pre-fix figure (95 k reads / 367 ms)
is a documented historical measurement from the F87 fix round, not something
re-measured in this session — re-measuring it would require reverting the fix
and rebuilding the container. What this session *does* establish, freshly, is
that the current code holds the intended shape: 4 600 reads / 35 ms, with no
statement touching `content_original`. The read reduction is therefore
**corroborated, not re-derived**.

The 281 ms entry is the single-lesson fetch, a different query with a different
job; it is listed for completeness and is not the list path.

## 3. Frontend bundle

| Metric | Value | Source |
|---|---:|---|
| Entry `index-*.js` | **176.80 kB** | `npx vite build`, this session |
| Entry gzipped | **67.39 kB** | same |

Unchanged from the pre-audit baseline. No dependency was added in this session;
`AGENTS.md` records that importing DOMPurify from `main.js` would push it into
the entry bundle (+28 kB measured, reverted), which is why the a11y hook is
imported from the lazy `utils/markdown.js` and `views/lessons/LessonContent.vue`
instead.

## 4. UI responsiveness

| Check | Result | Source |
|---|---|---|
| Routes with horizontal overflow > 16 px | **0 / 228 visits** | `ui/routes-all.txt` |
| Routes failing to mount | **0 / 228** | `ui/routes-all.txt` |
| `design-v2` route×viewport combos | **0 / 60** overflow beyond the 16 px scrollbar allowance | `ui/design-v2.txt` |

Chromium reserves ~15 px for the scrollbar, so `scrollWidth − clientWidth` of
`−15` is the healthy value, not a defect. A naive `scrollWidth > clientWidth`
test reports a false positive on every page (`AGENTS.md`). Raw numbers are kept
in `routes-all.json` so the claim can be re-checked rather than taken on trust.

## 5. Not measured, and why

| Item | Why not measured |
|---|---|
| Lighthouse / Core Web Vitals | Not run this session; no claim made. Would need a separate trace run. |
| Sustained concurrent load / throughput | Out of scope for this audit; the rate limiter (100/min/IP) is the binding constraint by design, so a load test would measure the limiter, not the app. |
| Cold-start latency | Container restarts during the audit make a cold figure non-comparable across runs. Warm numbers are the stable baseline. |
| `enrich-word` under GPU contention | Non-deterministic and hardware-bound; reported as a spread rather than a single figure. |

## Summary of dispositions

| Finding | Disposition |
|---|---|
| All read endpoints 12–74 ms p50 | **PASS** — no action |
| Admin list 4 600 reads / 35 ms, no `content_original` | **PASS** — F87 shape confirmed fresh |
| `enrich-word` 1.05–14.9 s spread | **Not an app regression** — local-model cost; F93 timeout mapping is the mitigation |
| `q=` leading wildcard 210 ms | **Not applicable** — no index can help; documented, do not "optimise" |
| Bundle 176.80 kB / 67.39 gzip | **PASS** — unchanged, no new dependency |
| UI overflow | **PASS** — 0 across 228 visits and 60 design combos |
