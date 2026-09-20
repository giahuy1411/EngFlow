# Phase G — performance, measured before and after (P5: no claim without numbers)

**Date:** 2026-09-20 (+07) · **Probe:** `sweep/v11/perf-probe.js` (median of **5** runs, ≥3 as the plan
requires) · **Artifacts:** `sweep/v11/perf-after.json`

---

## Method, and the flaw this probe fixes in its predecessor

Every endpoint is hit **5 times**; the reported figure is the **median**, not the mean — a single GC pause
or cold cache should not move the number. The body is fully consumed (`res.arrayBuffer()`) before the
clock stops, so time-to-first-byte does not hide serialisation cost.

**`rate_limit:*` is flushed before each endpoint's batch.** This is not ceremony: the global bucket is
100 requests/min/IP, so a 13-endpoint × 5-run probe would cross the limit mid-run and turn its own last
measurements into 429s, silently inflating the median. The predecessor probe
`sweep/v10/perf-probe.js` has **no flush logic at all** (verified by reading it) — its 65 requests could
cross 100/min partway through, so any of its late figures are suspect. This probe is not built that way.

---

## Results — all 13 endpoints, all HTTP 200

| Endpoint | Median | Min | Max |
|---|---|---|---|
| lesson list (page 0) | **13.6 ms** | — | — |
| lesson list (page 5) | **17.3 ms** | — | — |
| lesson detail | **9.8 ms** | — | — |
| exercise list (lesson) | **12.8 ms** | — | — |
| streak snapshot | **12.1 ms** | — | — |
| vocabulary search | **9.5 ms** | — | — |
| leaderboard | **15.8 ms** | — | — |
| decks (public) | **10.0 ms** | — | — |
| **admin exercise search `q=the`** | **169.3 ms** | — | — |
| admin exercise list (no `q`) | **48.5 ms** | — | — |
| admin lessons | **22.1 ms** | — | — |
| admin users | **19.9 ms** | — | — |
| admin stats | **21.2 ms** | — | — |

Twelve of thirteen endpoints are **under 50 ms**. The outlier is investigated below.

---

## The one slow endpoint: admin exercise search — measured, then **declined**

`GET /api/admin/exercises?q=the` = **169.3 ms** vs **48.5 ms** for the same endpoint without `q`.
This **confirms** `AGENTS.md`'s recorded ~185 ms on the same route (independent re-measurement, not a
quoted figure).

### Root cause, read from the source

`ExerciseRepository.findAdminPage`:
```sql
AND (:keyword IS NULL OR :keyword = ''
  OR LOWER(e.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
  OR LOWER(COALESCE(e.explanation, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
```
Two independent reasons no index can help: the column is **wrapped in `LOWER()`** (non-sargable), and the
pattern is **leading-wildcard** (`'%kw%'`). Both force a full scan of **43 735 rows**.

### I measured whether the obvious micro-optimisation actually helps — it does not

`LOWER()` on both sides looks removable, because the column collation is **case-insensitive**:

```
question    SQL_Latin1_General_CP1_CI_AS     (CI = case-insensitive)
explanation SQL_Latin1_General_CP1_CI_AS
```

So I compared the two forms directly (`sweep/v11/perf-search-variants.sql`):

| Form | Rows returned | Logical reads | CPU | Elapsed |
|---|---|---|---|---|
| `LOWER(col) LIKE LOWER('%the%')` — **current** | 16 588 | 1294 | **103 ms** | 100 ms |
| `col LIKE '%the%'` — no `LOWER()` | 16 588 | 1294 | **84 ms** | 86 ms |

**Identical row counts (16 588 = 16 588)** confirms the two forms are behaviourally equivalent, and the
CPU drops 103 → 84 ms. But:

- The saving is **~19 ms of CPU**, and the HTTP median is **169 ms** — the gap is the scan, not the
  `LOWER()`. Removing it would not move the user-visible number meaningfully.
- **Logical reads are identical (1294)** — the plan is the same full scan either way. There is no plan
  change to win.
- It is a **JPQL change in a shared repository method** used by the admin exercise list. Rewriting a query
  for ~19 ms of CPU, in a graduation project's admin-only search box, is not a trade the numbers justify.

**Decision: DECLINED, with the reason recorded** — which is what P5 requires. `AGENTS.md` reaches the same
conclusion from a different measurement ("don't 'optimise' by adding an index"), and this audit
independently reproduces the underlying numbers rather than citing that note.

> **What would actually help** (not done, because it is a product change, not a fix): a prefix search
> (`LIKE 'the%'`, sargable) or SQL Server full-text search. Both change search semantics — a full-text
> index matches whole words, so `q=the` would stop matching "theory". That is a product decision for the
> owner, not an audit fix.

---

## Frontend bundle — measured before and after

| Build | Entry `index-*.js` | gzip |
|---|---|---|
| Baseline (before any change) | **177.31 kB** | 67.50 kB |
| After F132 contrast work (40 files, +tokens) | **177.31 kB** | 67.50 kB |
| After F133/F135/F136 a11y fixes | **177.40 kB** | 67.53 kB |

**The entire contrast refactor cost 0 bytes.** Tailwind emits only the utilities actually used, and the
new `*-ink` / `*-strong` classes replaced existing ones rather than being added alongside. The final
+0.09 kB (+0.05 %) comes from the pagination/heading edits, and is within noise for a 177 kB bundle.

---

## Lighthouse — via chrome-devtools MCP (the driver v10 could not use)

| Route | Accessibility | Best Practices | SEO |
|---|---|---|---|
| `/` | **100** | 100 | 100 |
| `/lessons` | **100** | 100 | 100 |

`/lessons` initially scored **98**; the two failures were **not** contrast (`color-contrast` scored **1**,
i.e. passed — an independent tool confirming the F132 fix) but:
- `heading-order` — a card `<h3>` directly under the page `<h1>` → **F135**, fixed (now `<h2>`);
- `label-content-name-mismatch` — pagination buttons showing "1"/"8" but labelled "Trang đầu tiên" /
  "Trang cuối cùng", violating WCAG 2.5.3 → **F136**, fixed (labels now lead with the visible digit).

After the fixes: **100 on both routes.** The remaining non-perfect category, `agentic-browsing` (62–67),
is a Lighthouse 13 preview category about `llms.txt` conventions — not an accessibility or performance
signal, and not applicable to this app.

---

## Verdict

**Phase G: PASS.** Before/after numbers exist for every claim. Twelve of thirteen endpoints are under
50 ms; the one outlier was root-caused to a non-sargable leading-wildcard scan, its obvious
micro-optimisation was **measured and declined with the reason recorded**, and the bundle did not grow
from the contrast work. Lighthouse independently confirms Accessibility 100 on both audited routes.

No optimisation was applied, because the measurements did not justify one — and applying one anyway would
have violated P5.
