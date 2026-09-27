# audit-v18-full — Phase 0 baseline (đo phiên này, 2026-09-27 +07)

**Nhánh:** `audit-v15-full` @ `c76cc1c` · **Cây:** sạch (`git status --porcelain` rỗng)

## Container (docker ps)

| Service | Port | Status |
|---|---|---|
| engflow-backend | 8080 | Up |
| engflow-frontend | 5173 | Up |
| engflow-sqlserver | 1433 | Up (healthy) |
| engflow-redis | 6379 | Up |
| engflow-minio | 9000-9001 | Up |
| engflow-whisper | 9002 | Up |
| engflow-tts | 8001 | Up |
| engflow-tailscale | — | Up |

Ollama host :11434 (qwen2.5:1.5b, 3b).

## Suites (đếm từ RUN LOG, KHÔNG đếm XML)

| Bộ | Kết quả | So v17 | Log |
|---|---|---|---|
| Backend `.\mvnw.cmd test` | **537 / 0 / 0 / 11 — BUILD SUCCESS** | v17: 520 → **+17** | `baseline-backend.log` |
| Frontend `npx vitest run` | **194 passed / 1 skipped (32 file)** | v17: 192/1 → **+2** | `baseline-frontend.log` |
| Frontend `npx vite build` | **177.75 kB** (gzip 67.69), built in 7.54s | v17: 177.74 → +0.01 | `baseline-build.log` |
| `assert-harness.js` | **ALL CLEAN (7/7)** | v17: 5/5 → +2 check | stdout |

→ Không regression. Backend tăng do các test hồi quy v17 + test mới.

## DB parity (đo phiên này)

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks
1470|43738|5|118|29|4|3|12|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
EXERCISE_ATTEMPTS=33
```

→ **Khớp chính xác** parity v17 cuối. Không residue.

## Endpoint inventory

`annotationCount=121 · expandedRows=137 · distinctMethodPaths=135 · controllers=25`
(byVerb: GET 61, POST 51, PUT 14, DELETE 8, PATCH 3) — **khớp v16/v17**.

## Harness namespace

Default đã đổi sang `audit-v18-full` ở **2 chỗ** (`sweep/harness/_config.js:38`,
`sweep/v12/api-sweep.js:41`) — `assert-harness` check 6 PASS.

## Ghi chú môi trường

- Shell không tự tìm cwd cho exe → dùng `.\mvnw.cmd`.
- `mvnw -q` **nuốt dòng tổng kết** `Tests run:` → phải chạy KHÔNG `-q` để lấy số (bài học T0.5).
- Backend test dùng chung DB live → chờ suite xong trước API sweep.
