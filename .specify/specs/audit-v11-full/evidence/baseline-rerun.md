# Baseline — re-measured from scratch (audit-v11-full redo)

**Taken:** 2026-09-20 ~22:45–23:00 (+07) · **Branch:** `audit-streak-review`
**Method:** every number below was produced by a command run in THIS session. Nothing is quoted from
the pre-existing `evidence/baseline.md` or from audit-v10. Where a number agrees, it is recorded as a
**confirmation**; where it disagrees, the disagreement is the finding.

---

## 1. Test suites — counts read from the RUN LOG, never from `target/surefire-reports` XML

### Backend — `.\mvnw.cmd -o test` (PowerShell, repo root)

```
[INFO] Tests run: 483, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS
```

Log: `.p0-v11-backend-test-rerun.log` · exit code **0**

| Metric | This session | Pre-existing v11 baseline.md | Delta |
|---|---|---|---|
| Tests run | **483** | 483 | **0 — confirms** |
| Failures / Errors | **0 / 0** | 0 / 0 | — |
| Skipped | **11** | 11 | — |

### Frontend — `npx vitest run` (frontend/)

```
 Test Files  21 passed | 1 skipped (22)
      Tests  109 passed | 1 skipped (110)
   Duration  9.91s
```

Log: `.p0-v11-frontend-test-rerun.log` · exit code **0**

| Metric | This session | Pre-existing | Delta |
|---|---|---|---|
| Test files | **21 passed / 1 skipped (22)** | same | **0 — confirms** |
| Tests | **109 passed / 1 skipped (110)** | same | **0 — confirms** |

### Production build — `npx vite build` (frontend/)

```
dist/assets/index-D0l6HySg.js   177.31 kB │ gzip: 67.50 kB
✓ built in 4.97s
```

Log: `.p0-v11-build-rerun.log` · exit code **0**

| Metric | This session | Pre-existing | Delta |
|---|---|---|---|
| Entry `index-*.js` | **177.31 kB** | 177.31 kB | **0 — exact match** |
| gzip | **67.50 kB** | 67.50 kB | 0 |

> Note: the hash changed (`index-DgNek2KA.js` → `index-D0l6HySg.js`) while the **size is byte-identical**.
> The hash differs because `frontend/src/assets/app-logo.css` carries an uncommitted F129 edit; size is
> unaffected. Recorded so the changed hash is not mistaken for a regression.

---

## 2. Parity line — **DISAGREEMENT WITH THE PRE-EXISTING BASELINE (this is a finding)**

Canonical query `sweep/v10/parity-check.sql` (column order fixed — must not be relabelled):

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1471|43735|72|127|28|15|4|127|14|5
```

**Parity line (this session): `1471|43735|72|127|28|15|4|127|14|5`**

| Column | This session | `evidence/baseline.md` | Match? |
|---|---|---|---|
| lessons | 1471 | 1471 | ✅ |
| exercises | 43735 | 43735 | ✅ |
| users | 72 | 72 | ✅ |
| vocabulary | 127 | 127 | ✅ |
| speaking | 28 | 28 | ✅ |
| video | 15 | 15 | ✅ |
| lesson_sub | 4 | 4 | ✅ |
| **payments** | **127** | **126** | ❌ **+1** |
| decks | 14 | 14 | ✅ |
| snapshots | 5 | 5 | ✅ |

### Root cause of the +1 (measured, not inferred)

```
id=80256  order_code=ENG340D56BE7C33  status=PENDING  transaction_id=NULL  created_at=2026-09-20 22:23:20
```

This row was created **inside the pre-existing baseline's own measurement window** (`~22:20–22:35`)
by the `/premium/checkout` mount behaviour documented in `AGENTS.md` — i.e. **audit residue, not
product data**. The pre-existing `baseline.md` therefore recorded a contaminated number as the
baseline.

**Why this matters (F130, OPEN):** six call sites hard-code `cleanupAuditPayments(126)`:

```
sweep/v10/prompt-claims.js:394   cleanupAuditPayments(126)
sweep/v10/routes-all-v10.js:240  cleanupAuditPayments(126)
sweep/v8/p17_screenshots.js:199  cleanupAuditPayments(126)
```

and `sweep/v8/ui/lib.js` / `sweep/v10/ui-lib.js` default to the same expectation. Each of them will
delete the residue row, print `after=126 expected=126 -> PARITY OK`, and **pass for the wrong reason** —
masking that the true pre-sweep count is 127 and that any sweep which creates rows is being judged
against a stale target. A `after === expected` check that cannot distinguish "cleaned up correctly"
from "the baseline was already wrong" is a **harness defect**, not a product defect.

> This is the same class of mistake the repo already made once (`STALE-P8-EVIDENCE`, audit-v8). It is
> recorded rather than silently corrected, because the correction has to land in six files.

---

## 3. Containers — `docker ps`

| Container | Image | Ports | Status |
|---|---|---|---|
| `engflow-backend` | engflow-backend | 8080 | Up (created 2026-09-20 02:21 UTC) |
| `engflow-frontend` | engflow-frontend | 5173 | Up — **Vite dev, `./frontend:/app` bind mount** |
| `engflow-sqlserver` | mssql/server:2019-latest | 1433 | Up (**healthy**) |
| `engflow-redis` | redis:alpine | 6379 | Up |
| `engflow-minio` | minio/minio | 9000–9001 | Up |
| `engflow-whisper` | engflow-whisper | 9002 | Up |
| `engflow-tts` | engflow-supertonic | 8001 | Up |
| `engflow-tailscale` | tailscale/tailscale | — | Up |

### Backend container is NOT stale (checked, because a stale JAR would invalidate every API result)

```
find src/main/java -name "*.java" -newermt "2026-09-20 09:21:59" | wc -l   →  0
```

**0** source files are newer than the running container → the JAR matches the working tree.
The frontend needs no such check: it is a bind-mounted Vite dev server (verified via `ps aux` inside
the container showing `npm run dev --host 0.0.0.0`).

---

## 4. Live service probes

| Probe | Result | Interpretation |
|---|---|---|
| `GET localhost:5173/` | **200** | SPA served |
| `POST /api/auth/login` with `{}` | **400** | Route exists; validation rejected the empty body. **Not** 401/404 |
| `POST /api/auth/login` (valid creds) | **200** + JWT | Auth path live |
| `GET localhost:8080/` | **401** | **Correct, not a fault** — `anyRequest().authenticated()` guards everything by default. Recorded so it is not mistaken for an outage |
| `GET localhost:9000/minio/health/live` | **200** | MinIO healthy |

---

## 5. Endpoint inventory — re-derived FROM SOURCE, not quoted

```
@GetMapping      56
@PostMapping     49
@PutMapping      15
@DeleteMapping    9
@PatchMapping     2
@RequestMapping  19   (class-level base paths)
                 ── 26 controller files
```

Path literals extracted from the annotation arguments: **159**.

**Disagreement with the pre-existing baseline:** it records *131 annotations, 26 controllers* and
audit-v10 records *144 routes*. My count of the five method-mapping annotations is
`56+49+15+9+2 = 131` — which **matches** the 131. The route count differs because the `speaking/` and
`video/` controllers carry **array-form** mappings, e.g.

```java
@GetMapping({"/api/v1/admin/speaking-prompts", "/api/v1/admin/video-prompts"})
```

I counted **7** such array-form annotations in source. 131 annotations − 7 array + 14 extra literals
= 138 method-mapped routes, plus 19 class-level bases that some routes inherit. The exact route total
is therefore **not** settled by arithmetic alone; Phase 2 will settle it by **probing** each candidate
and recording which answer, rather than by re-asserting a derived number. This is recorded as an open
discrepancy, not smoothed over.

---

## 6. Environment

- Java **25.0.3** LTS (Temurin) — matches the stack requirement
- `mvnw.cmd` present at repo root; **`cmd /c` is mangled by this MSYS shell** — `cmd //c` also failed,
  so the suite is run via PowerShell `& .\mvnw.cmd -o test`. Recorded because the naive invocation
  silently produced a 3-line log containing only the Windows banner and exited 0.
- `sqlcmd` must be invoked as
  `docker exec -i engflow-sqlserver bash -c 'cd /opt/mssql-tools18/bin && ./sqlcmd … -P "$SA_PASSWORD"'`
  — Git-Bash rewrites `/opt/...` into a Windows path and fails with a misleading *"no such file"*.
- Live Redis rate-limit buckets at session start: **none present** (`--scan --pattern "rate_limit:*"`
  returned empty) → no bucket was pre-charged by a previous session.

## Gate status

**Phase A gate: PASS.** Backend green (483), frontend green (109/22), build green (177.31 kB), containers
up, DB reachable. Two discrepancies against the pre-existing baseline are recorded as findings
(**payments 126→127**, **route count unsettled**) rather than inherited. No code has been changed yet.
