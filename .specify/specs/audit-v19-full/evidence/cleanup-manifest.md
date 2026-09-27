# audit-v19-full — cleanup manifest

**Ngày:** 2026-09-27 · Nguyên tắc: xoá khi `git ls-files`=0 **VÀ** không tham chiếu thật.

## ĐÃ XOÁ

| Path | Loại | Bằng chứng 2 chiều |
|---|---|---|
| `.playwright-mcp/*` | scratch runtime Playwright MCP | gitignored `.gitignore:88` |
| `sweep/harness/_*.sql`, `sweep/harness/*.json` | per-run output | gitignored `.gitignore:121,127` |
| `target/**` (23M) | build Maven | gitignored |
| `frontend/dist/**` (1.4M) | build Vite | gitignored |
| `sweep/harness/_v19_cleanup.js`, `_mutation_test.js` | helper tạm | untracked, xoá ngay sau dùng |

## KHÔNG XOÁ (keep 2 chiều)

| Path | Lý do |
|---|---|
| `scripts/figma-export/node_modules/` | `cls-probe.js` resolve `playwright-core` từ đây |
| `uploads/**` | data thật |
| `.env`, `.env.bak-*`, `.env.example` | secret/config (gitignored) |
| `.specify/specs/audit-v8..v19/**` | artifact các vòng |
| `sweep/harness/**` | harness chuẩn (whitelist) |
| `frontend/public/e2e-tts.wav`, `sweep/harness/fixtures/*` | fixture có license |
| `sql/migrations/V005__exercises_order_index.sql` | migration mới (W3) — GIỮ |

## DB residue

- Probe tự dọn; parity cuối `1470|43738|5|118|29|4|3|12|10` + PENDING=0 + EX_ATTEMPTS=33 → **CLEAN**.
- **W4:** order `ENG2143E44D4DEC` (PENDING) là **chờ chuyển khoản thật** — **KHÔNG xoá** (ràng buộc V9);
  sẽ thành SUCCESS khi người dùng chuyển.
