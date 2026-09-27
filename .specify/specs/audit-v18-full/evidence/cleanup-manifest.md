# audit-v18-full — cleanup manifest

**Ngày:** 2026-09-27 · **Nguyên tắc:** chỉ xoá khi (a) `git ls-files`=0 **VÀ** (b) không có tham chiếu thật.

## ĐÃ XOÁ (rác sinh trong quá trình chạy)

| Path | Loại | Bằng chứng 2 chiều | Hành động |
|---|---|---|---|
| `.playwright-mcp/*.log`, `*.yml` (snapshots/console phiên này) | scratch runtime của Playwright MCP | gitignored (`.gitignore:88`); không file nào tham chiếu | Xoá nội dung |
| `sweep/harness/_probe_cleanup.js` | helper tạm tôi tạo để gọi cleanup | untracked; đã xoá ngay sau dùng | Xoá |
| `sweep/harness/_deep-cleanup.sql`, `_g6-sepay.sql`, `_g7-speaking.sql`, `_g8-speaking.sql` | per-run SQL probes tự sinh | gitignored (`.gitignore:121`); untracked | Xoá |
| `sweep/harness/*.json` (ui-sweep.json, deep-probe.json, ... ở harness dir) | output per-run | gitignored (`.gitignore:127`) | Xoá |
| `target/**` (22M) | build output Maven | gitignored (`.gitignore:10`) | Xoá |
| `frontend/dist/**` (1.4M) | build output Vite | gitignored (`.gitignore:36`) | Xoá |

## KHÔNG XOÁ (keep đã verify 2 chiều)

| Path | Lý do giữ |
|---|---|
| `scripts/figma-export/node_modules/` | `sweep/harness/cls-probe.js` **cố ý** resolve `playwright-core` từ đây |
| `frontend/node_modules/`, `node_modules/` | dependencies |
| `uploads/**` | data volume thật (bind-mount) |
| `.env`, `.env.bak-*`, `.env.example` | secret/config (gitignored) |
| `.specify/specs/audit-v8-full … v17-full/**` | artifact vòng trước |
| `.specify/specs/audit-v18-full/**` | artifact vòng này |
| `sweep/harness/**` (14 file source + fixtures) | harness chuẩn (whitelist `.gitignore:97-109`) |
| `sweep/v8/sqlrun.py`, `p16-parity.sql`, `v8/lib.js`, `v8/ui/*`, `v12/api-sweep.js` | tooling chuẩn |
| `frontend/public/e2e-tts.wav`, `sweep/harness/fixtures/human-speech-librispeech.wav` | fixture có license (CC BY 4.0) |
| `docs/**`, `.agents/mcp_config.json`, `.claude/settings.local.json` | docs + tooling config |

## DB residue

Mọi probe (api-sweep, deep-probe, ui-sweep, design-v2, routes-all, g6/g7/g8/l2, MCP Phase U) **tự dọn trong cùng
run**. Parity cuối: `1470|43738|5|118|29|4|3|12|10` + STUDY_DAYS=4 + PENDING_PAYMENTS=0 + EXERCISE_ATTEMPTS=33 → **CLEAN**.
Không có row residue cần xoá thủ công.
