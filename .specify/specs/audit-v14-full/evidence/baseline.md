# audit-v14-full — Phase 0 baseline (đo bằng chính phiên này)

**Ngày:** 2026-09-25 (+07) · **Branch:** `audit-v14-full` · **HEAD:** `e729b2c` (tree sạch)

## T0.1 Git checkpoint

```
HEAD   e729b2c19c27950507138bf120506ba3c9cf9529
Branch main (trước) → audit-v14-full (tạo mới, tree sạch)
git status --short = (rỗng)
```

## T0.6 Backend suite

`& .\mvnw.cmd -o test` → `baseline-backend.log`

```
[INFO] Tests run: 523, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS
```

→ **523 / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** (đếm từ run log, không đếm XML).
**Khớp v13** (523) → *xác nhận*.

**Ghi chú thao tác:** `cmd /c "mvnw.cmd -o test"` qua Bash/PowerShell KHÔNG chạy được (mở shell rỗng
hoặc "not recognized"). Phải gọi `& .\mvnw.cmd -o test` từ PowerShell (cwd = repo root). Ghi lại để
các phase sau không lặp lỗi.

## T0.7 Frontend suite + build

**Điều kiện tiên quyết đã xử lý:** host `frontend/node_modules` **RỖNG** (chỉ `.vite-temp`) — container
`engflow-frontend` dùng **named volume** cho `/app/node_modules` nên deps container KHÔNG nằm trên host.
Đã chạy `npm install` trong `frontend/` trước.

`npx vitest run` → `baseline-frontend.log`

```
Test Files  30 passed | 1 skipped (31)
     Tests  194 passed | 1 skipped (195)
  Duration  29.16s
```

→ **194 passed / 1 skipped (31 file)** — **+1 test so v13 (193)**. Chênh +1 do cây hiện tại có thêm 1
test so với thời điểm v13 chốt; ghi nhận là **delta có nguồn**, không phải drift.

`npx vite build` → `baseline-build.log`

```
dist/assets/index-xR3BfCqS.js   177.98 kB │ gzip: 67.76 kB
✓ built in 10.16s
```

→ entry **177.98 kB** (gzip 67.76) — **khớp v13** (177.98 kB) → *xác nhận*.
**Build hash (D9):** `sha256(index-xR3BfCqS.js) = 56e02221851332b528b6afed687b7ffa45f20386a5824482090fb896d0d5d2c4`

## T0.8 Container inventory + staleness

| Container | Status | Port |
|---|---|---|
| engflow-backend | Up 44m | 8080 |
| engflow-frontend | Up 45m | 5173 |
| engflow-sqlserver | Up 45m (healthy) | 1433 |
| engflow-redis | Up 45m | 6379 |
| engflow-minio | Up 45m | 9000-9001 |
| engflow-whisper | Up 45m | 9002 |
| engflow-tts | Up 45m | 8001 |
| engflow-tailscale | Up 45m | — |

**Staleness:** backend `StartedAt = 2026-09-24T16:25:54.752224533Z`; `.java` mới nhất `2026-09-22 17:50`
→ **KHÔNG stale**.

## T0.9 Parity + endpoint inventory

`python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql` → `parity-before.txt`

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1470   |43735    |72   |118       |29      |15   |4        |126     |10   |5
```

→ **khớp v13 baseline chính xác cả 10 số** → *xác nhận*.

`node sweep/v14/api-inventory.js` → `endpoint-inventory.json`

```
annotationCount    = 132
expandedRows       = 148
distinctMethodPaths= 146
controllers        = 26
byVerb = { GET: 64, POST: 55, PUT: 16, DELETE: 10, PATCH: 3 }
```

→ **khớp v13 (132/148/146/26)** → *xác nhận*.

## Kết luận Phase 0

- Baseline **xanh**: backend 523/0/0/11; frontend 194/1 (31 file); build 177.98 kB; 8 container up; không stale.
- Harness khôi phục 16 file, namespace sạch (guard PASS).
- Parity + inventory khớp baseline.
- **Được phép sang Phase 1–2.**
