# audit-v16-full — cleanup manifest (V1)

Rác sinh ra trong quá trình triển khai/kiểm thử. **Mọi mục đã xác minh 2 chiều trước khi xoá.**

## A. Build / scratch output (untracked, gitignored) — XOÁ

| Đường dẫn | Nội dung | Kích thước | Bằng chứng |
|---|---|---|---|
| `target/**` | build output backend (Maven) | 22 MB / 1015 file | `git check-ignore` = IGNORED |
| `frontend/dist/**` | build output frontend (Vite) | 1.4 MB / 84 file | IGNORED |
| `frontend/node_modules/.vite/**` | Vite cache | 4 KB | IGNORED |
| `.playwright-mcp/**` | 14 snapshot a11y YAML cũ (2026-09-24) từ Playwright MCP | 62 KB | IGNORED, `git ls-files` = 0 |

## B. Script chết trong `scripts/` (tracked) — XOÁ

Xác minh: **0 tham chiếu code** (grep toàn repo trừ chính nó); chỉ được **nhắc trong báo cáo lịch sử** v3/v4.

| File | Vì sao chết | Ref còn lại |
|---|---|---|
| `scripts/audit-v4-api-sweep.mjs` | thay bằng `sweep/v12/api-sweep.js` (v12) | `audit-v4-full/REPORT.md` (lịch sử) |
| `scripts/create-audit-issues.js` | V3: không tạo GitHub issue | — |
| `scripts/close-audit-issues.js` | V3 | — |
| `scripts/read-audit-issues.js` | V3 | — |
| `scripts/clean-lesson444-block.js` | one-off đã áp (v3) | `audit-v3/REPORT.md` (lịch sử) |
| `scripts/restore-lesson444.js` | one-off đã áp (v3) | `audit-v3/REPORT.md` (lịch sử) |

## C. KHÔNG đụng (đã kiểm chứng)

| Đường dẫn | Lý do GIỮ |
|---|---|
| `scripts/figma-export/**` | **`cls-probe.js:36` cố ý resolve `playwright-core` từ đây** (đã verify) — không phải rác |
| `scripts/figma-export/node_modules/**` | dependency thật của cls-probe; `git ls-files`=0 (không commit) |
| `uploads/**` | media thật |
| `.specify/specs/audit-v8..v15/**` | artifact lịch sử |
| `.agents/mcp_config.json` | không track, đã gitignore (không phải rò rỉ) |
| `sweep/harness/**`, `sweep/v8/**`, `sweep/v12/**` | harness chuẩn, tracked |

## D. Dọn probe residue (DB)

| Mục | Xử lý | Bằng chứng |
|---|---|---|
| `study_days` hàng 40076 (do ui-sweep vòng 1) | DELETE theo ID/ngày + tài khoản probe | `STUDY_DAYS=4` khôi phục |
| `payment_transactions` PENDING (ui-sweep tạo) | dọn cùng run | `remaining=0 after=12 baseline=12` |
| deck/vocab/lesson audit (`AUDIT-V12-%`, `zzv12%`) | api-sweep tự dọn | `AUDIT_DECKS=0 AUDIT_VOCAB=0 AUDIT_LESSONS=0` |

## Kết quả sau dọn

- Parity: `1470|43738|5|118|29|4|3|12|10` · `STUDY_DAYS=4` · `PENDING_PAYMENTS=0`.
- `git status` sạch (chỉ còn thay đổi có chủ ý của v16).
