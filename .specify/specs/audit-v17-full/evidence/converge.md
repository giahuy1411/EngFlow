# audit-v17-full — converge (spec ↔ code gap closure)

Đối chiếu `spec.md` (O1–O9, R1–R11) với việc đã làm. Mục nào chưa làm ghi rõ lý do.

| Mục | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| O1 | Mọi endpoint có probe | ✅ | `api-sweep` 143/0 (+1 blocked, 2 n/a) · `deep-probe` 58/0 |
| O2 | Mọi route × role × 5 viewport | ✅ | `ui-sweep` 0 guard/api/page/overflow; `design-v2` 0/60 overflow |
| **O2b** | **Mọi chức năng điều khiển thủ công qua MCP** | ✅ | `mcp-walkthrough.md` U1–U22; chrome-devtools thật; engine 2 = playwright-core |
| O3 | DB toàn vẹn | ✅ | 22 FK / 0 orphan / 0 thiếu index; parity khớp |
| O4 | 4 chức năng verify từng khẳng định | ✅ | `demo-claims.md`: 24 CONFIRMED, 4 sửa |
| O5 | Design khớp prompt (đã sửa lỗ hổng) | ✅ | `design-v2` 7670 el, 0 badFont/legacy/drift; `prompt-flaws.md` + `prompt-rewritten-v17.md` |
| O6 | Perf before/after | ✅ (không win) | `perf-conclusion.md` |
| O7 | 2 vòng | ✅ | `round-2.md` |
| O8 | Docs + prompt doc + demo doc | ✅ | `docs-drift.md` |
| O9 | Rác dọn + liệt kê | ✅ | `cleanup-manifest.md` |

## Requirements

| # | Yêu cầu | Trạng thái |
|---|---|---|
| R1 | Số liệu đo phiên này | ✅ mọi con số có log/JSON |
| R2 | Probe thứ 2 + review chéo | ✅ `review-v17.md`; mọi verdict đối chiếu lại code |
| R3 | Bằng chứng runtime | ✅ HTTP thật + MCP thật |
| R4 | DML an toàn (xoá theo ID) | ✅ 3 hàng xoá theo ID liệt kê; parity giữ |
| R5 | Probe tự dọn | ✅ `assertClean CLEAN` |
| R6 | Design chỉ fix lỗi thật | ✅ 3 fix design, mỗi cái có số đo before/after |
| R7 | Perf before/after | ✅ (kết luận "không win") |
| R8 | Dọn rác + liệt kê | ✅ 5 mục |
| R9 | Docs khớp | ✅ |
| R10 | Prompt đã sửa lỗ hổng | ✅ `prompt-flaws.md` (4 lỗ hổng) + `prompt-rewritten-v17.md` |
| R11 | Phase U 2 engine | ✅ chrome-devtools MCP + playwright-core; Playwright MCP ghi BLOCKED + lỗi thật |

## Gap còn lại (có lý do)

| Gap | Lý do |
|---|---|
| F-17-03/04/05/06/07 `OPEN` | 3 là **sửa doc** (đã sửa trong Phase 8) / 2 là **harness** — không sửa code app trong phiên này |
| Playwright **MCP** | BLOCKED môi trường (thiếu Chrome channel) — dùng playwright-core thay |
| Webhook SePay chữ ký thật | BLOCKED real-money (từ v11) |
| Speaking media thật | Cần mic/file thật; mic giả → FAILED đúng thiết kế |
| Timezone → UTC | V2 v15: chưa có consumer thứ hai |
| GitHub issues | V3: không tạo |
