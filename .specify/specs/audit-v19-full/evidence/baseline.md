# audit-v19-full — Phase 0 baseline (đo phiên này, 2026-09-27 +07)

**Nhánh:** `audit-v15-full` @ `0dbd66c` · **Cây:** sạch (`git status --porcelain` rỗng)

## Container (docker ps)

backend :8080 · frontend :5173 · sqlserver :1433 (healthy) · redis :6379 · minio :9000-9001 ·
whisper :9002 · tts :8001 · tailscale — **8 up**. Ollama host :11434.

## Suites (đếm từ RUN LOG, KHÔNG đếm XML)

| Bộ | Kết quả | So v18 | Log |
|---|---|---|---|
| Backend `.\mvnw.cmd test` | **537 / 0 / 0 / 11 — BUILD SUCCESS** | khớp | `baseline-backend.log` |
| Frontend `npx vitest run` | **194 passed / 1 skipped (32 file)** | khớp | `baseline-frontend.log` |
| Frontend `npx vite build` | **177.75 kB** (gzip 67.69) | khớp | `baseline-build.log` |
| `assert-harness.js` | **ALL CLEAN (7/7)** | khớp | stdout |

→ Không regression.

## DB parity

```
1470|43738|5|118|29|4|3|12|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
EXERCISE_ATTEMPTS=33
```
→ Khớp chính xác v18 cuối.

## Endpoint inventory

`annotationCount=121 · expandedRows=137 · distinctMethodPaths=135 · controllers=25`
(byVerb: GET 61, POST 51, PUT 14, DELETE 8, PATCH 3) — khớp v16/v17/v18.

## Harness namespace

Default đổi → `audit-v19-full` ở 2 chỗ (`sweep/harness/_config.js`, `sweep/v12/api-sweep.js`) — check 6 PASS.

## Prompt flaw (T0.8) — ĐO LẠI

| Cặp | Tỉ lệ (đo phiên này) | Ngưỡng | Verdict |
|---|---|---|---|
| `#1E293B` trên `#FFFDF5` | **14.36** | ≥7 AAA | ✅ (chỉ cặp này) |
| `#FFFFFF` trên `#8B5CF6` | **4.23** | ≥4.5 | ❌ FAIL AA |
| `#FFFFFF` trên `#F472B6` | **2.65** | ≥3 | ❌ FAIL cả AA-large |
| `#8B5CF6` text trên `#FFFDF5` | **4.16** | ≥4.5 | ❌ FAIL |
| `#FFFFFF` trên `#7C3AED` | **5.70** | ≥4.5 | ✅ (codebase dùng) |
| `#6D28D9` trên `#FFFDF5` | **6.98** | ≥4.5 | ✅ |

Type scale bước: `1.167 1.143 1.125 1.111 1.200 1.250 1.200 1.333 1.250` → **không hằng số 1.25**.
`grep lucide-react` = **0**. Banned fonts word-boundary = **1** (comment lịch sử `index.html:32`).
→ **4 lỗ hổng y hệt v17/v18** (AAA sai, type-scale sai, "Lucide React" sai, font).
"Thay font = Be Vietnam Pro" vẫn là **verify** (đã là BVP duy nhất).
