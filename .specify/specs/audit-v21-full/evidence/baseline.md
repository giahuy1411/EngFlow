# audit-v21-full — Phase 0: baseline (đo thật)

**Ngày đo:** 2026-09-28 (giờ VN, +07) · **Nhánh:** `audit-v15-full` · **Nền:** commit `b539d11`
**Người/harness:** Claude Code, đo trực tiếp trên máy này.

## 1. Runtime

| Container | Trạng thái |
|---|---|
| `engflow-backend` | Up (:8080) |
| `engflow-frontend` | Up (:5173) |
| `engflow-sqlserver` | Up (healthy, :1433) |
| `engflow-redis` | Up (:6379) |
| `engflow-minio` | Up (:9000-9001) |
| `engflow-whisper` | Up (:9002) |
| `engflow-tts` | Up (:8001) |
| `engflow-tailscale` | Up |

`GET /actuator/health` → **401** (đúng — path sau `anyRequest().authenticated()`; F-20-05 by design).
`GET http://localhost:5173/` → **200**.

## 2. Bộ test chuẩn (đọc từ run log, KHÔNG đọc XML surefire)

| Hạng mục | Kết quả | Baseline kỳ vọng | Khớp |
|---|---|---|---|
| `.\mvnw.cmd test` (repo root) | **541 tests / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** | 541/0/0/11 | ✓ |
| `npx vitest run` (frontend) | **194 passed / 1 skipped (32 files)** | 194/1/32 | ✓ |
| `npx vite build` (frontend) | exit 0, entry `index-DZ76lATv.js` **177.75 kB** (gzip 67.69) | 177.75 kB | ✓ |

Bằng chứng: `evidence/baseline-backend-v21.log`, `evidence/baseline-frontend-v21.log`, `evidence/baseline-build.log`.

## 3. Parity DB

`python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql`:

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks
1470|43738|5|118|29|4|3|13|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
EXERCISE_ATTEMPTS=33
```

**Khớp 100%** baseline (payments **13** = giao dịch SePay thật `ENG73E2D3AA2DF6`, giữ theo V9).

## 4. Harness tĩnh

`node sweep/harness/assert-harness.js` — sau khi sửa default v20→v21 (F-21-01):

```
PASS  every route a browser harness visits exists in the router
PASS  every internal require() resolves to an existing file
PASS  no hardcoded parity/payments baselines outside lib.js
PASS  every harness that visits a write-on-mount route calls a cleanup helper
PASS  harness-restore.md exists, non-empty, documents source + fixes
PASS  _config.js default audit round is the LATEST round  -- default=audit-v21-full latest=audit-v21-full
PASS  no harness hardcodes an audit round path outside _config.js
PASS  no harness defaults --audit to a stale round outside _config.js
=== assert-harness: ALL CLEAN ===
```

## 5. Fix Phase 0

| Mã | Vấn đề | Fix | Bằng chứng |
|---|---|---|---|
| **F-21-01** | Default audit round còn `audit-v20-full` ở **2 nơi** (`sweep/harness/_config.js:38` + `sweep/v12/api-sweep.js:45`) → probe chạy không `--audit` ghi evidence vào thư mục v20 (đúng lớp F-20-02b). | Sửa **cả hai** → `audit-v21-full`; tạo `harness-restore.md`. | assert-harness check 6+8: FAIL trước → PASS sau |

## 6. Khoảng trống comment tiếng Việt (đo cho Phase 1)

Đo bằng grep diacritics tiếng Việt (`[\x{00C0}-\x{1EF9}]`):

| Nhóm | Tổng | Đã có tiếng Việt | **Thiếu** |
|---|---|---|---|
| Java (`src/main/java/**`) | 196 | 152 | **44** |
| Frontend (`frontend/src/**`, trừ test) | ~118 | ~85 | **33** |

Danh sách 77 file thiếu → `evidence/comment-gap.txt`. **Phase 1 sẽ bù đúng phần thiếu này** (không viết lại phần đã có — tránh rủi ro F-20-06).
