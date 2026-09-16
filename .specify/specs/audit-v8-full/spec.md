# Spec — audit-v8-full (kiểm toán toàn diện EngFlow, vòng 2)

**Ngày**: 2026-09-14 · **Tiền nhiệm**: `audit-v7-full` (2026-09-12, 327 tests)
**Phạm vi**: toàn bộ codebase (139 endpoint mapping), DB SQL Server trong Docker,
toàn bộ route frontend bằng Chromium thật, design system Playful Geometric + Be Vietnam Pro.

## Mục tiêu đo được bằng bằng chứng

| # | Mục tiêu | Bằng chứng bắt buộc |
|---|---|---|
| G1 | Chạy TOÀN BỘ API, không mẫu chọn lọc | liệt kê 139 mapping từ source → sweep mỗi endpoint × 3 vai trò (noauth/user/admin) |
| G2 | Mọi API tương ứng có tương tác UI thật | browser Playwright: route + click + network capture |
| G3 | Chức năng chính sát sao: Bài học/Bài tập, streak, login/register, search/sort, CRUD, AI | CRUD vòng đời đầy đủ + streak đo live + AI gọi Ollama/Whisper thật |
| G4 | DB Docker: audit + tối ưu hiệu năng | inventory/phân mảnh/index usage/query stats + TRƯỚC/SAU theo constitution P5 |
| G5 | UI đồng bộ prompt design system + font Be Vietnam Pro | computed-style từng route, token, shadow, radius, fontFamily |
| G6 | Tìm lỗ hổng của chính prompt yêu cầu → viết lại (prompt-master) | artifact review + rewritten prompt |
| G7 | Báo cáo chi tiết: đã làm / chưa / fix thế nào / skill đã nạp | REPORT.md |

## Ràng buộc (constitution v1.0.1)

- P1 baseline xanh là tiền đề: mọi sửa đổi phải giữ `mvnw test` + `vitest run` xanh.
- P3 schema do Hibernate `ddl-auto=update`, Flyway disabled → đổi schema = SQL trực tiếp + entity.
- P5 hiệu năng phải ĐO trước/sau, không ước lượng.
- P6 font duy nhất Be Vietnam Pro; shadow cứng `pop-*`, border 2px.
- P8 mỗi thay đổi có bằng chứng runtime (HTTP thật + UI thật).
- Boundary: không seed demo; không commit `.env`/key/fixture; grep tham chiếu trước khi xoá module.

## Định nghĩa hoàn thành

Mỗi mục tiêu G1–G7 có file bằng chứng trong `sweep/v8/` hoặc `.specify/specs/audit-v8-full/`,
mỗi lỗi tìm thấy có ID finding, bản vá, regression test và số đo xác nhận.
