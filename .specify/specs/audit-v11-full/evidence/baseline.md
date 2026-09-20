# Baseline — measured by this session (audit-v11-full)

**Taken:** 2026-09-20 ~22:20–22:35 (+07) · **Branch:** `audit-streak-review` · **Checkpoint:** `dae9667`
**Method:** every number below comes from a command run in this session. Nothing is quoted from
audit-v10. Where a v11 number matches v10's record, that is noted as a **confirmation of v10**.

## 1. Test suites — counts read from the RUN LOG, never from `target/surefire-reports` XML

### Backend — `./mvnw.cmd -o test` (repo root)
```
[INFO] Results:
[INFO] Tests run: 483, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS
```
Log: `.p0-v11-backend-test.log` · exit code **0**

| Metric | v11 (measured now) | v10 record | Delta |
|---|---|---|---|
| Tests run | **483** | 470 | **+13** |
| Failures | **0** | 0 | — |
| Errors | **0** | 0 | — |
| Skipped | **11** | 11 | — |

**Drift worth recording:** +13 tests vs the v10 report. v10's own text is internally inconsistent —
its §0 header says *"Backend 456 test"* and *"Tests run: 470"* while §8's table says **456 run**. The
v11 measurement is **483**, read from the log. The likely cause is that v10 counted before its final
round of test additions, but that is an inference — what is **measured** is 483 green now. The
`+13` is not treated as a defect; it is recorded so the number is not carried forward unexamined.

### Frontend — `npx vitest run` (frontend/)
```
 Test Files  21 passed | 1 skipped (22)
      Tests  109 passed | 1 skipped (110)
   Duration  24.22s
```
Log: `.p0-v11-frontend-test.log` · exit code **0**

| Metric | v11 (measured now) | v10 record | Delta |
|---|---|---|---|
| Test files | **21 passed / 1 skipped (22)** | 21 files | +1 skipped file |
| Tests | **109 passed / 1 skipped (110)** | 106 passed / 1 skipped | **+3** |

**Drift worth recording:** v10 reported *106 passed*; v11 measures **109 passed**. +3 tests exist
now that v10's number does not account for. Recorded, not explained away.

### Production build — `npx vite build` (frontend/)
```
dist/assets/index-DgNek2KA.js   177.31 kB │ gzip: 67.50 kB
✓ built in 7.61s
```
Log: `.p0-v11-build.log` · exit code **0**

| Metric | v11 | v10 record | Delta |
|---|---|---|---|
| Entry `index-*.js` | **177.31 kB** | 177.31 kB | **0 — exact match** |
| gzip | **67.50 kB** | 67.51 kB | 0.01 kB (rounding) |

**Confirmation:** the entry bundle is **byte-identical in size** to v10's record. v10's claim that no
size regression was introduced is confirmed by independent re-measurement.

## 2. Containers — `docker ps`

| Container | Status | Ports |
|---|---|---|
| `engflow-backend` | Up | 8080 |
| `engflow-frontend` | Up | 5173 |
| `engflow-sqlserver` | Up (**healthy**) | 1433 |
| `engflow-redis` | Up | 6379 |
| `engflow-minio` | Up | 9000–9001 |
| `engflow-whisper` | Up | 9002 |
| `engflow-tts` | Up | 8001 |
| `engflow-tailscale` | Up | — |

## 3. Live service probes (this session)

| Probe | Result | Interpretation |
|---|---|---|
| `GET localhost:5173/` | **200** | SPA served |
| `GET localhost:9000/minio/health/live` | **200** | MinIO healthy |
| `GET localhost:8080/actuator/health` | **401** | **Correct, not a fault** — `anyRequest().authenticated()` guards actuator. Recorded so it is not mistaken for an outage. |
| `POST /api/auth/login` (`user@gmail.com`) | **200** + JWT | Auth path live; token shape confirmed |
| `GET /api/lessons?page=0&size=2` | **200** + content | Lesson list live |

## 4. Database parity — measured independently, then compared to v10

Canonical query (`sweep/v10/parity-check.sql`, column order fixed):

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1471   |43735    |72   |127       |28      |15   |4         |126     |14   |5
```

**Parity line: `1471|43735|72|127|28|15|4|126|14|5`**

**Confirmation of v10:** this matches audit-v10's recorded post-audit parity **exactly, all ten
columns**. v10's claim that its final state was stable is independently confirmed.

> Note on column identity: the user's prompt asks about "CRUD" and the v10 parity file uses
> `vocabulary`/`video`/`snapshots` columns. An earlier ad-hoc query in this session labelled columns
> differently and is superseded by the canonical query above.

## 5. Streak schema — measured live (`sys.*` catalogs, not assumed)

| Check | Result | Meaning |
|---|---|---|
| `study_policy` rows | **1** | Policy deployed |
| `study_policy.effective_from` | **2026-09-20** | Cutover is today |
| `study_days` rows | **0** | No study day recorded yet |
| FK `fk_study_days_user` | **1 present** | F124's fix held |
| UQ `uq_study_days_user_date` | **1 present** | Duplicate day impossible |

**Confirmation of v10 + F124:** v10 claimed it fixed `deploy.sql` so the FK is created even when
Hibernate made the table first. The FK is present in the live DB. Phase 1 additionally proves the
constraints **bind** (a violating insert is rejected), rather than only existing.

## 6. Endpoint inventory — re-derived from source

```
@GetMapping      56
@PostMapping     49
@PutMapping      15
@DeleteMapping    9
@PatchMapping     2
                ── 131 annotations, 26 controllers
```

v10 records **144 routes** from 131 annotations, explaining that 14 annotations carry **two** path
literals (the speaking/video alias pairs) so 1 annotation = 2 routes. Phase 2 verifies this by
counting path literals directly rather than re-asserting the arithmetic.

## 7. Environment

- Java **25.0.3** LTS (Temurin) — matches the stack requirement
- `mvnw.cmd` present at repo root
- `sqlcmd` runs via `MSYS_NO_PATHCONV=1` in this shell (MSYS mangles `/opt/...` into a Windows path
  otherwise) — recorded because a naive invocation fails with a misleading *"no such file"* error
- Working tree **clean** at `dae9667`

## Gate status

**Phase 0 gate: PASS.** Backend green, frontend green, build green, containers up, DB reachable,
parity measured and matching v10, streak schema deployed. Two drifts (+13 backend, +3 frontend) are
recorded rather than smoothed over. No code has been changed in this audit yet.
