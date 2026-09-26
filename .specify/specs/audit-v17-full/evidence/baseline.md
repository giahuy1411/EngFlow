# audit-v17-full — Phase 0 baseline (đo phiên này)

**Ngày:** 2026-09-26 (+07) · **Nhánh:** `audit-v15-full` @ `baaa61f` · **Cây:** sạch (`git status` = nothing to commit)

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

Ollama :11434 (qwen2.5:1.5b, 3b).

## Suites (đếm từ run log, KHÔNG đếm XML)

| Bộ | Kết quả | Log |
|---|---|---|
| Backend `.\mvnw.cmd test` | **515 / 0 / 0 / 11 — BUILD SUCCESS** | `baseline-backend.log` |
| Frontend `npx vitest run` | **178 passed / 1 skipped (29 file)** | `baseline-frontend.log` |
| Frontend `npx vite build` | **177.74 kB** (gzip 67.67), built in 7.46s | `baseline-build.log` |
| `assert-harness.js` | **ALL CLEAN (5/5)** | (stdout phiên) |

→ **Khớp baseline AGENTS.md** (515/0/0/11 · 178/1/29 · 177.74 kB). Không regression.

## DB parity (đo phiên này)

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks
1470|43738|5|118|29|4|3|12|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
```

→ **Khớp chính xác** parity v16. Không residue.

## Endpoint inventory

`annotationCount=121 · expandedRows=137 · distinctMethodPaths=135 · controllers=25`
(byVerb: GET 61, POST 51, PUT 14, DELETE 8, PATCH 3) — **khớp v16**.

## Ghi chú môi trường

- Shell không tự tìm cwd cho exe → phải dùng `.\mvnw.cmd` (đã gặp + sửa trong phiên).
- Test backend **không có config DB riêng** (`src/test/resources` trống) → dùng chung DB live; phải đợi suite xong
  trước khi chạy API sweep để tránh nhiễu.
