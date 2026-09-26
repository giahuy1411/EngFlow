# audit-v17-full — Phase 8: docs drift (cập nhật tài liệu)

Mọi thay đổi tài liệu trong phiên, kèm lý do + bằng chứng.

## Đã sửa

| File | Thay đổi | Lý do (đo được) |
|---|---|---|
| `docs/demo-engflow-4-chuc-nang.md` | "Chấm thử" → **"Kiểm tra"** (3 chỗ) | UI thật là "Kiểm tra"/"Nộp bài"; `grep "Chấm thử" frontend/src` = 0 (F-17-03) |
| `docs/demo-engflow-4-chuc-nang.md` | "không có trường `correctAnswer`" → **"có mặt nhưng luôn null"** | response thật có key `correctAnswer:null` (F-17-04) |
| `docs/demo-engflow-4-chuc-nang.md` | Thứ tự fallback tra từ: cache → **local DB** → direct | code `vocabularyService.js:117,152,155` (C-17-01) |
| `docs/demo-engflow-4-chuc-nang.md` | Thêm cảnh báo **~20 s lần tra đầu** | đo 19.5–20.7 s (F-17-05) |
| `.specify/memory/constitution.md` | v1.0.1 → **v1.0.2**; P1 baseline 221/73 → **520/183** | số audit-v3 đã stale; đo lại phiên này |
| `AGENTS.md` | baseline backend **520**, frontend **183** | +5 test mỗi bên |
| `AGENTS.md` | Thêm: `.\mvnw.cmd`, từ điển ~20s, `/search` đi `/dictionary`, `bg-accent text-white` fail, `exercise_attempts` không parity, ui-sweep warn YouTube, `.mimosa` rác, AI vocab 500/504 | 7 bài học mới đo trong phiên |
| `.specify/specs/audit-v17-full/prompt-rewritten-v17.md` | **Tạo mới** từ v10 + 4 sửa lỗ hổng | R10 |
| `.specify/feature.json` | trỏ `specs/audit-v17-full` | speckit resolve đúng vòng |

## Tài liệu đã verify (không cần sửa)

| File | Kết quả |
|---|---|
| `README.md` | "25 REST controllers" — đúng (inventory = 25) |
| `CLAUDE.md` | "V1-V10 migrations" — đúng |
| `docs/erd-sql-guide.md` | 18 bảng / 22 FK — khớp DB (19 bảng gồm `sysdiagrams`) |
| `docs/lesson-builder-removal.md` | mô tả đúng trạng thái sau v15 |

## Đã kiểm lỗ hổng (R10)

`evidence/prompt-flaws.md` — 4 lỗ hổng của prompt dán vào:
1. **"AAA"** sai (white-on-accent 4.23:1 FAIL, white-on-secondary 2.65:1 FAIL).
2. **"1.25 Major Third"** sai (bước thật 1.111–1.333).
3. **"Lucide React"** sai (stack là `lucide-vue-next`).
4. **"Plus Jakarta Sans"/Outfit** — vi phạm P6; người dùng đã thay Be Vietnam Pro.

→ Đã sửa trong `prompt-rewritten-v17.md` (bảng corrections + token map cập nhật `*-ink`/`*-strong` + `#556070`).
