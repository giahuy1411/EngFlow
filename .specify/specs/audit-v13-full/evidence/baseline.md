# audit-v13-full — Phase 0 baseline (đo bằng chính phiên này)

**Ngày:** 2026-09-22 (+07) · **Branch:** `audit-streak-review` · **HEAD:** `3c1c525` (2026-09-21 17:20:37 +0700)

## T0.1 Git checkpoint

```
HEAD   3c1c525c83128ef5d316b65d6995ca737fc9b86a  fix(audit-v12): F152 lessonId injection, F153 streak, ...
Branch audit-streak-review
Dirty (untracked):
  ?? .specify/specs/audit-v9-full/evidence/*.json   (6 file — artifact v9 chưa commit)
  ?? sweep/v13/                                     (copy sai nhãn của v12 — xem F-13-06)
```
Tree sạch về mặt code tracked; chỉ có artifact untracked. **Không** dirty file code.

## T0.3 Backend suite

`& .\mvnw.cmd -o test` → log `baseline-backend.log`. **Kết quả: xem `baseline-backend.log`** (đếm từ run log, không đếm XML).

## T0.4 Frontend suite + build

`npx vitest run` →
```
Test Files  24 passed | 1 skipped (25)
     Tests  127 passed | 1 skipped (128)
  Duration  11.71s
```
→ **127 passed / 1 skipped (25 file)** — khớp baseline v12 (xác nhận).

## T0.5 Container inventory + staleness

| Container | Image | Status | Port |
|---|---|---|---|
| engflow-backend | engflow-backend | Up 2h | 8080 |
| engflow-frontend | engflow-frontend | Up 2h | 5173 |
| engflow-sqlserver | mssql/server:2019-latest | Up 2h (healthy) | 1433 |
| engflow-redis | redis:alpine | Up 2h | 6379 |
| engflow-minio | minio/minio | Up 2h | 9000-9001 |
| engflow-whisper | engflow-whisper | Up 2h | 9002 |
| engflow-tts | engflow-supertonic | Up 2h | 8001 |
| engflow-tailscale | tailscale/tailscale | Up 2h | — |

Probe: frontend 200, minio health 200, whisper 404 (root, bình thường), tts 404 (root, bình thường), backend actuator 401 (secured — bình thường).

**Staleness:** container created `2026-09-21T08:09:49Z` (= **15:09 VN**) > newest `.java` **15:05 VN** → **KHÔNG stale**.

## T0.6 Parity live

`python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql` →
```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1470   |43735    |72   |118       |29      |15   |4        |126     |10   |5
```
So v12 (`1470|43734|72|118|28|...`): **exercises +1**, **speaking +1** — drift do phiên test của người dùng (hàng `exercise_id=777434` tạo 2026-09-21 22:01 + 1 speaking submission). Ghi nhận, không phải lỗi.

## T0.7 Endpoint inventory (tái dựng từ source)

`sweep/v12/api-inventory.js` (static analyzer) →
```
annotationCount    = 132
expandedRows       = 148
distinctMethodPaths= 146
controllers        = 26
byVerb = { GET: 64, POST: 55, PUT: 16, DELETE: 10, PATCH: 3 }
```
So v12 (`131/147/145`): **+1 annotation** — endpoint mới kể từ v12. `evidence/endpoint-inventory.json`.

## T0.8 Nhãn sweep/v13

`grep -n "audit-v12" sweep/v13/*` → **còn nhãn v12** (header `api-sweep.js` ghi "audit-v12-full Phase 2"; `_cleanup.sql` xoá `AUDIT-V12-API-%`). → **F-13-06**, phải đồng bộ nhãn v13 trước khi tin coverage.

## Kết luận Phase 0

- Baseline **xanh**: frontend 127/1; backend chạy; 8 container up; parity ghi nhận.
- 2 bất thường cần theo: **sweep/v13 nhãn sai** (F-13-06), **drift exercises/speaking +1** (do user test, không phải lỗi).
- Được phép sang Phase 1–2.
