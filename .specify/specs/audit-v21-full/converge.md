# audit-v21-full — converge (soát codebase vs spec/plan/tasks)

Đối chiếu codebase hiện tại với `spec.md` / `tasks.md`. Mục nào chưa làm → ghi rõ.

| Objective | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| O1 Comment tiếng Việt | 44 Java + 33 FE thiếu → 0 | **XONG** | grep diacritics: 0 file thiếu |
| O2 API | 121 endpoint, 0 bỏ sót | **XONG** | api-sweep 145/0, deep-probe 58/0 |
| O3 UI/UX 2 MCP | mọi route × role × viewport | **XONG** | ui-sweep ALL CLEAN + mcp-ui.md |
| O4 DB | parity/orphan/index/query-plan/validate | **XONG** | db-audit.md |
| O5 Hiệu năng | bundle/Lighthouse/CWV có số | **XONG** | perf-frontend.md |
| O6 Security | scan + authz/upload/AI/secret | **XONG** | security review + secret-scan.md |
| O7 Verify design | font/token/contrast/responsive/a11y | **XONG** | mcp-ui.md + design-v2 |
| O8 2 vòng lặp | vòng 2 sâu hơn | **XONG** | round2-*.log |
| O9 Review AI sinh | /simplify + cross-review | **XONG** | findings F-21-04/06/07/08 |
| O10 Dọn rác | verify-trước-khi-xoá | **XONG** | cleanup.md |

## Task chưa hoàn thành (append nếu cần làm tiếp)

**Không có.** Mọi task trong `tasks.md` đều `[x]` hoặc được ghi rõ là "Giữ có lý do" / "OPTIONAL không làm".

## Việc phát sinh trong quá trình (đã xử lý, không nằm trong plan ban đầu)

| Phát sinh | Xử lý |
|---|---|
| F-21-03 (Vue fragment do comment) | Fix + gia cố `comment_only.py` |
| F-21-07 (AdminLayout cùng lớp, ẩn) | Fix |
| F-21-06 (nguồn gốc default 2 chỗ) | Fix tận gốc |
| F-21-12 (5 citation doc lệch) | Remap tay |
| F-21-04 (công cụ che string) | Fix + mutation-test |

## Kết luận

Codebase **khớp spec**: 0 objective dang dở, 0 task bỏ sót, 0 mục BLOCKED.
