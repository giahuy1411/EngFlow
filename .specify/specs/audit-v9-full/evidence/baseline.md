# Baseline — audit-v9-full

Measured 2026-09-17, 17:37–17:45 (+07), BEFORE any v9 change. Every number comes from the artifact named beside it;
nothing here is carried over from the AGENTS.md note or from the previous round.

## Infrastructure (verified before measuring anything)

| Check | Result | Source |
|---|---|---|
| Containers `engflow-*` | 8 UP (backend, frontend, sqlserver, redis, minio, ollama, whisper, tts) | `docker ps` |
| Ports | 5173 / 8080 / 9000 / 11434 / 9002 all open | `Test-NetConnection` |
| Ollama models | `qwen2.5:1.5b`, `qwen2.5:3b` present | `docker exec engflow-ollama ollama list` |
| SQL Server | `engflow-sqlserver` healthy (up 41 h at measurement time) | `docker ps` |

## Test / build baseline

| Metric | Value | Command | Log |
|---|---|---|---|
| Backend tests | 383 PASS / 0 FAIL, BUILD SUCCESS | `cmd /c "mvnw.cmd test"` | `sweep/v8/audit-v9-baseline-backend.log` |
| Frontend tests | 90 tests / 19 files PASS | `npx vitest run` | `sweep/v8/audit-v9-baseline-frontend.log` |
| Vite build | entry `index-27RDIRhB.js` 176.80 kB / gzip 67.38 kB; CSS `index-dm2RY88L.css` 92.42 kB / gzip 15.91 kB | `npx vite build` | `sweep/v8/audit-v9-baseline-build.log` |

## Endpoint / route / DB inventory

| Metric | Value | Source |
|---|---|---|
| HTTP mappings from source | **144** across **26** controllers (10 with no explicit path) | `sweep/v8/v9_endpoint_inventory.py` → `evidence/endpoint-inventory.json` |
| Frontend routes | 39 (37 named); guards {public 8, guestOnly 4, auth+premium 6, auth 11, admin-redirect 1, admin 9} | `sweep/v8/v9_route_inventory.py` → `evidence/route-inventory.json` |
| DB tables with rows | 24 | `db-audit.md` block [1] |
| Row parity baseline (10 tables) | `1471\|43737\|76\|127\|28\|15\|4\|126\|14\|5` | `sweep/v8/p16-parity.sql` |

## Post-fix state (same day, for contrast)

| Metric | Baseline | After |
|---|---|---|
| Backend tests | 383 | **393** (+5 F105 guard, +3 F106 SRS, +2 F108 projection) |
| Frontend tests | 90 / 19 files | 90 / 19 files |
| JS entry | 176.80 kB | 176.80 kB (unchanged) |
| CSS entry | 92.42 kB | **85.22 kB** |
| `GET /api/lessons/651/exercises` median | 153 ms / ~5,724 logical reads | **29 ms / 182 logical reads** |
| `POST /api/srs/review` (large interval) | 500 | **200** |

## Measurement-discipline notes carried into this round

- Test counts are taken from the run LOG, never from `target/surefire-reports` XML (stale XML from a deleted test class
  once inflated an aggregate by 8).
- DB timestamps are naive VN; `SYSDATETIME()` inside the container is UTC and is used only to label a run.
- Harness artifacts are CWD-relative, so every browser/API harness runs from `sweep/v8`.
- A sweep that mutates ends by asserting the parity line above; a mismatch is a finding, not a pass.
