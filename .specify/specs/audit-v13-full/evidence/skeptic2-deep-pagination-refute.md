# Skeptic #2 — Refutation of: "Deep-pagination slowness did NOT reproduce (negative result)"

**Area:** performance (Phase 5) · **Claimed severity:** LOW · **Claimed verdict:** NOT a finding
**Skeptic verdict:** `refuted = true` — the claim's central empirical assertions do not hold, though the corrected severity remains **LOW**.
**Date:** 2026-09-22 · **Stack:** backend :8080, frontend :5173, SQL Server `engflow-sqlserver`, redis `engflow-redis` (all live).
**Method:** real HTTP (`fetch`, body fully read before clock stop), real DB (`sys.dm_exec_query_stats` delta + `SET STATISTICS IO/TIME`), bucket `rate_limit:*` flushed before every call. No mocks.

---

## 1. The claim, restated

> First observation page 0 = 158.0ms, page 800 = 376.6ms, page 1658 = 448.9ms (under concurrent load).
> Controlled interleave (5 depths × 7 rounds): p0 213.4 / p400 197.3 / p800 199.6 / p1200 218.9 / p1658 **109.4** —
> "no upward trend, last page fastest". SQL: OFFSET 16580 vs OFFSET 0 = 72/63/79ms vs 30/31/33ms at **identical 1364 reads**.
> Root cause: "the 448ms reading was contention … not a pagination-depth effect."

The evidence in `sweep/v13/perf-before.json` (`ancillary.adminExerciseSearch.deep_pagination`) and
`evidence/performance.md` §4 is **real and present in the repo** — I did not dispute its existence. I disputed its conclusion.

---

## 2. What reproduces

### 2a. Contention explains the 448ms first reading — CONFIRMED
The claim's root-cause for the *first* reading is correct and I reproduced it directly. Same endpoint, same page 1658:

| condition | samples (ms) | median |
|---|---|---|
| quiet | 141.1, 82.9, 93.9, 92.2, 79, 102.2, 88.6, 78.6 | **90.4** |
| contended (6 parallel heavy same-endpoint calls) | 602.8, 656.6, 1057.5, 830.7, 930.7, 392.9, 747.7, 398.8 | **702.2** |

Contention inflates page 1658 from 90ms to 700ms+ — more than enough to manufacture a 448ms reading.
**This sub-claim stands.**

### 2b. The SQL delta exists — but only for the claim's hand-written literal query
`sweep/v8/sqlrun.py` on a literal `LOWER(question) LIKE '%the%' OR LOWER(COALESCE(explanation,'')) LIKE '%the%'`
statement, 12 columns, `ORDER BY order_index, exercise_id`:

| | reads | elapsed |
|---|---|---|
| `OFFSET 0` | 1364 | 44 / 49 ms |
| `OFFSET 16580` | 1364 | 77 / 91 ms |

Reads identical (1364), ~35–45ms elapsed delta. Reproduces the claim's SQL table.
**But this literal query is NOT the query the application issues** — see §3.

---

## 3. What the claim got wrong — the refutation

### 3a. There IS a reproducible upward trend at the API layer (the claim says there is none)

Controlled interleave, randomized order, bucket flushed before **every** call:

| page | 0 | 200 | 400 | 600 | 800 | 1000 | 1200 | 1400 | 1658 |
|---|---|---|---|---|---|---|---|---|---|
| median (ms) | 192.7 | 200.7 | 249.0 | 233.3 | 230.8 | 259.4 | 261.5 | 268.9 | **99.3** |

Decisive **paired** test, p0 vs p1200, alternating, 20 pairs, bucket flushed each call:

```
raw (p0,p1200): [158,229.3] [159.1,220.6] [169.9,226.4] [161.7,313.4] [189.3,218] [187.2,238.3]
                [203.6,252.8] [169,243.6] [158.8,243.4] [204.3,236.7] [216.6,239.1] [162.4,236.5]
                [175.1,347.2] [164.9,205.4] [182.3,262.1] [175.6,223.4] [188.4,248.7] [161.3,201.3]
                [200.3,237.4] [220.2,223.3]
paired diff mean = +61.9 ms   sd = 40.1   t = 6.92 (df=19)   p1200 > p0 in 20/20 pairs
```

**20/20 pairs, t=6.92** — page 1200 is reliably ~62ms slower than page 0. The claim's "no upward trend" is false.
(The claim's own table also shows p1200 = 218.9 as the highest of the interior depths, which it dismissed as noise.)

### 3b. The app's actual (parameterized) query reads grow with depth — the claim's "identical 1364 reads" is the wrong query

`sys.dm_exec_query_stats` delta across **one real API call** each
(`GET /api/admin/exercises?page=N&size=10&q=the`, admin JWT), the app's real page statement
(`select e1_0.exercise_id,e1_0.audio_url,… from exercises e1_0 … offset ? rows fetch next ? rows only`):

| page | page-stmt logical reads | page-stmt elapsed |
|---|---|---|
| 0 | **4 251** | 192.6 ms total |
| 600 | **17 424** | 239.6 ms total |
| 1200 | **30 256** | 238.4 ms total |
| 1658 | **36 820** | 129.8 ms total |

That is an **8.7× growth in logical reads** from page 0 to page 1658 — the exact OFFSET-depth cost the claim
declared flat. The claim's `reads_both: 1364` measured a *literal* statement that plans differently from the
app's parameterized catch-all (the claim itself noted the app's parameterized statement measures 4 770 reads
vs the literal's 1 364 — yet used the literal's constant figure for the depth comparison).

### 3c. "Last page fastest" is a count-skip artefact, not evidence of no depth effect

The claim leans on p1658 = 109ms being fastest. That is a confound. Statement breakdown per call:

| page | rows returned | SQL statements | count_big stmt | page stmt |
|---|---|---|---|---|
| 0 | 10 | 3 | 1301 reads / ~112 ms | 4251 reads |
| 600 | 10 | 3 | 1301 reads / ~112 ms | 17424 reads |
| 1200 | 10 | 3 | 1301 reads / ~122 ms | 30256 reads |
| **1658** | **8 (< size 10)** | **2** | **SKIPPED** | 36820 reads |

Page 1658 returns 8 rows (< page size 10), so Spring Data JPA (`PageableExecutionUtils.getPage`) **skips the
`select count_big(...)` query entirely**. That removes a ~112–122ms / 1301-read statement, which is why the
last page measures ~90ms while every full page measures ~200–220ms. The last page is faster **despite**
doing the most OFFSET work — it is not evidence that depth is free. The claim's headline datum
("page cuối còn nhanh nhất") is therefore invalid as support for "no trend".

**Independent confirmation by arithmetic:** with the count skipped, `totalElements` must equal
`offset + content.size() = 1658×10 + 8 = 16588` — and the live response reported `totalElements = 16588`.
The count-skip is confirmed by both the missing statement *and* the reconstructed total.

---

## 4. Verdict

- **Refuted = true.** The claim's central empirical assertions — "no upward trend" and "a real but small
  ~35ms SQL cost that does not surface at the API layer" — are false. A depth effect **does** reproduce:
  +61.9ms p0→p1200 (20/20 paired, t=6.92) at the API, with the app's real query's logical reads growing
  4 251 → 36 820 (8.7×). The specific datum used to argue "no trend" (last page fastest) is a count-skip artefact.
- **What the claim got right:** the 448ms first reading was contention (reproduced: 90ms quiet → 702ms contended),
  and the literal-SQL OFFSET delta is ~35–45ms. The claim is a *misframed* negative result, not a fabrication.
- **Corrected severity: LOW (unchanged in practice).** The effect is real but ~62ms on an **admin-only**,
  hand-typed pagination reached only by typing page ~1200. It does not justify keyset pagination. So the
  *conclusion* ("not worth fixing") survives; the *reasoning and evidence* do not.

## 5. Reproduce

- `node tmp/s2_deep_rand.js` — randomized 9-depth interleave (trend).
- `node tmp/s2_paired.js` — 20-pair p0 vs p1200 (t=6.92, 20/20).
- `node tmp/s2_contend.js` — page 1658 quiet vs contended (90.4 → 702.2 ms).
- `python tmp/s2_final.py` — per-call statement/read breakdown (count-skip on last page).
- `python sweep/v8/sqlrun.py tmp/s2_offset2.sql` — literal-SQL OFFSET 0 vs 16580 (1364 reads both).

## 6. Cleanup

No probe rows created (read-only GETs and read-only DMV/STATISTICS queries). `tmp/s2_*` scratch files only;
no application source modified.
