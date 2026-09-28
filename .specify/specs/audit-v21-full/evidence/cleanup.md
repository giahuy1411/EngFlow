# audit-v21-full — Dọn rác (verify-trước-khi-xoá)

**Đo:** 2026-09-28. Mọi mục **verify tham chiếu trước khi xoá**; chỉ xoá khi 0 tham chiếu.

## Đã xoá

| Đối tượng | Bằng chứng trước khi xoá | Kết quả |
|---|---|---|
| `tasks.md` (root) | nội dung **7 byte** = `Start`; `grep -rln "tasks.md"` trong `src/ frontend/src/ sweep/` = **0**; không phải tài liệu được tham chiếu | xoá |
| `frontend/docs/screenshots/e2e-2026-08-28/` | dir **rỗng** (`0 files`), untracked | xoá (kèm `frontend/docs/screenshots/` sau khi rỗng) |
| `frontend/src/docs/superpowers/playful-redesign/` | dir **rỗng**, `grep playful-redesign` = 0 | xoá (kèm `frontend/src/docs/` sau khi rỗng) |
| `frontend/src/components/video/` | dir **rỗng**, `grep "components/video"` = **0** | xoá |
| `sweep/harness/__pycache__/` | Python bytecode do harness chạy sinh ra | xoá |
| **13 comment stub rỗng** trong `frontend/src/assets/design-system.css` | header không có rule nào bên dưới (`/* --- Typography Utilities --- */`, `/* --- Inputs --- */`, `/* --- Badges / Tags --- */`, `/* --- Dot Grid Pattern --- */`, `/* --- Squiggle Divider --- */`, `/* --- Checkbox (Geo styled) --- */`…) + 3 dòng trắng thừa trong `@media` | xoá |

## Kiểm chứng SAU khi dọn

| Kiểm tra | Kết quả |
|---|---|
| `npx vite build` | exit 0, entry **177.75 kB** (không đổi) |
| `npx vitest run` | **194 passed / 1 skipped (32 files)** |
| `node sweep/v8/ui/design-v2.js` | non-BVP 0, legacy 0, overflow 0/60, tokens missing 0/660, drift 0, lucide 0, mobile shadow 0, wrong page 0 |
| class bị xoá có được dùng? | `geo-checkbox`, `Typography Utilities`, `Badges / Tags` — **0 tham chiếu** (đúng là dead scaffolding) |

## TUYỆT ĐỐI KHÔNG xoá (giữ có lý do)

| Đối tượng | Lý do |
|---|---|
| `scripts/figma-export/node_modules` | `sweep/harness/cls-probe.js:36` **cố ý** resolve `playwright-core` từ đó (đã kiểm chứng: cls-probe in ra `playwright-core from: .../figma-export/node_modules/playwright-core`) |
| `.env.bak-*` (4 file) | chứa secret thật, gitignored, cần rollback (quyết định v20: dọn an toàn) |
| `sweep/harness/fixtures/*` | fixture có license (LibriSpeech CC BY 4.0) |
| `.specify/specs/*` | audit trail 21 vòng |
| `uploads/`, `backups/`, `.playwright-mcp/` | runtime/gitignored |
| `sweep/harness/_db-audit-v21.sql` | artifact của vòng, được `db-audit.md`/`findings.md` tham chiếu |

## Kết luận

Xoá **17 mục rác** (1 file + 4 dir + 1 pycache + 13 stub CSS + dòng trắng). Build/test/design **vẫn xanh**.
Không xoá nhầm mục nào có giá trị — mọi mục "giữ" đều nêu lý do và đã kiểm chứng tham chiếu.
