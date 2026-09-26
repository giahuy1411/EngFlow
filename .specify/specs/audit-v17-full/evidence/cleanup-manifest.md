# audit-v17-full — cleanup manifest

Mỗi mục: đường dẫn · loại · bằng chứng untracked/unreferenced (2 chiều) · hành động.

## Đã xoá

| # | Đường dẫn | Loại | Kiểm chứng 2 chiều | Lý do |
|---|---|---|---|---|
| 1 | `src/main/java/com/datn/engflow/controller/.mimosa/` (218 B) | rác runtime (hook-state của tooling) | `git ls-files` = 0 (untracked); `.gitignore:73` khớp `.mimosa/`; grep tham chiếu trong `src/**` = 0 | Thư mục trạng thái phiên do tooling sinh **trong cây source Java** — không phải mã, không được build đọc. Xoá. |
| 2 | `target/**` | build output | `.gitignore` `target/`; không commit | Sinh bởi `mvnw test` trong phiên; xoá sau khi đo. |
| 3 | `frontend/dist/**` | build output | `.gitignore` `dist/`; không commit | Sinh bởi `vite build` trong phiên. |
| 4 | `frontend/.mimosa/` | rác runtime | untracked, gitignored | Cùng lớp #1, ở frontend. |
| 5 | `.specify/.mimosa/` | rác runtime | untracked, gitignored | Cùng lớp #1. |

## Probe residue (DB) — đã dọn trong phiên

| # | Bảng | Hàng | Bằng chứng | Hành động |
|---|---|---|---|---|
| 6 | `study_days` | id **40082** (user 2, 2026-09-26) | sinh bởi U8 "Nộp bài" (MCP walkthrough) | `DELETE WHERE id=40082` → count về **4** |
| 7 | `study_days` | id **40083** (user 2, 2026-09-26) | sinh bởi U13 flashcard (MCP walkthrough) | `DELETE WHERE id=40083` → count về **4** |
| 8 | `exercise_attempts` | attempt_id **70147** (user 2, lesson 445) | sinh bởi U8 submit (MCP walkthrough) | `DELETE WHERE attempt_id=70147` → 38 → **37** |

**Còn tồn (KHÔNG xoá — cần điều tra thêm, R4):** `exercise_attempts` 70144 (user 2, lesson 447, 2026-09-26 14:07 —
**trước** phiên này 21:40) + phần tăng 28→37 chưa giải thích hết. Không xoá vì chưa chắc probe nào tạo và không
được xoá oan dữ liệu học viên thật (user 150040 có 3 attempt). → finding **F-17-07**.

## KHÔNG xoá (đã xác minh 2 chiều — tránh "dọn rác" phá hoại)

| Đường dẫn | Lý do giữ |
|---|---|
| `scripts/figma-export/node_modules/` (19 MB) | `sweep/harness/cls-probe.js:36` **cố ý** resolve `playwright-core` từ đó (fallback). `git ls-files` = 0. |
| `frontend/node_modules/` (121 MB) | dependency, gitignored. |
| `uploads/**` | volume dữ liệu thật (bind-mount vào backend). |
| `.env*` | cấu hình bí mật — không commit, không xoá. |
| `.specify/specs/audit-v8..v16/**` | artifact các vòng trước (đã commit một phần). |
| `frontend/public/e2e-tts.wav` | fixture được track có chủ đích (AGENTS.md). |
| `.agents/mcp_config.json` | cấu hình MCP của harness `.agents/` (không track). |

## Kiểm tra cuối

- `git status` sau cleanup: chỉ còn thay đổi **cố ý** (3 file fix + test + docs + `.specify/specs/audit-v17-full/**`).
- Parity sau mọi DML: `1470|43738|5|118|29|4|3|12|10`, `STUDY_DAYS=4`, `PENDING_PAYMENTS=0`.
