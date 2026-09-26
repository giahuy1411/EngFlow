# audit-v16-full — Phase 0: freeze + baseline

**Ngày đo:** 2026-09-26 (+07)

## T0.1 — Nhánh + cây làm việc

| Kiểm tra | Kết quả |
|---|---|
| Nhánh | `audit-v15-full` (**giữ nguyên** — không tạo nhánh mới, theo yêu cầu người dùng) |
| HEAD | `6f046b6 docs(audit-v15): full audit artifacts -- findings, evidence, report` |
| `git status --porcelain` | **0 dòng** → `nothing to commit, working tree clean` |
| Kết luận | v15 **đã commit hết** → không cần nhánh mới |

## T0.3 — Harness drift guard

`node sweep/harness/assert-harness.js` → **ALL CLEAN (5/5 PASS)**:
- mọi route browser harness ghé đều tồn tại trong router
- mọi `require()` nội bộ resolve được
- không harness nào hardcode parity/payments baseline ngoài `lib.js`
- mọi harness ghé route write-on-mount đều gọi cleanup helper
- `harness-restore.md` tồn tại, không rỗng

## T0.4 — Backend suite

| Bộ | Kết quả | Ghi chú |
|---|---|---|
| `mvnw.cmd test` | **515 tests / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** | khớp baseline v15; đếm từ run log |

## T0.5 — Frontend suite + build

| Bộ | Kết quả | So v15 |
|---|---|---|
| `npx vitest run` | **178 passed / 1 skipped (29 file)** | khớp baseline v15 |
| `npx vite build` | entry `index-*.js` **177.74 kB** (gzip 67.68) | khớp baseline v15 |

## T0.6 — Container + parity + inventory

**Container (8 up):** engflow-backend :8080, engflow-frontend :5173, engflow-sqlserver :1433 (healthy),
engflow-redis :6379, engflow-minio :9000/9001, engflow-whisper :9002, engflow-tts :8001, engflow-tailscale.
**Ollama host :11434** — `qwen2.5:1.5b`, `qwen2.5:3b`.

**Parity:** `1470|43738|5|118|29|4|3|12|10` — **khớp baseline v15**.
`STUDY_DAYS=4`, `PENDING_PAYMENTS=0`.

**Endpoint inventory** (`sweep/harness/api-inventory.js`):
- 121 annotation · **137 expanded rows** · **135 distinct method+path** · **25 controller**
- byVerb: GET 61, POST 51, PUT 14, DELETE 8, PATCH 3

## Ghi chú kỷ luật (tự phát hiện, tự sửa)

Chạy `api-inventory.js` **lần đầu thiếu `--audit`** → ghi đè `.specify/specs/audit-v15-full/evidence/endpoint-inventory.json`
(artifact đã commit của v15). Phát hiện qua `git status`, **khôi phục bằng `git checkout --`** rồi chạy lại đúng
`--audit audit-v16-full`. Đây đúng lớp lỗi **F-15-17/F-13-06** (harness ghi đè evidence lịch sử) — ghi lại để vòng sau
không lặp.
