# audit-v16-full — Phase 8.1: docs drift (đo lại, không chép)

Mọi con số dưới đây **đo trong phiên này** (2026-09-26), đối chiếu tài liệu với thực tế source/DB.

| # | Tài liệu | Ghi sai | Đo thật | Bằng chứng | Xử lý |
|---|---|---|---|---|---|
| F-16-07 | `README.md:95` | "26 REST controllers" | **25** | `grep -rlE "@RestController([^A]|$)" src/main/java` = 25 (21 `controller/` + 4 `controller/payment|speaking|video/`); `GlobalExceptionHandler` là `@RestControllerAdvice` | **SỬA** → "25 REST controllers (21 + 4)" |
| F-16-08 | `CLAUDE.md:93` | migrations "V1-V8" | **V1–V10** | `ls src/main/resources/db/migration/` = 10 file (V10 thêm ở v15) | **SỬA** → "V1-V10" |

## Đối chiếu các con số KHỚP (không cần sửa)

| Tài liệu | Ghi | Đo thật | Kết luận |
|---|---|---|---|
| `README.md:101` | "46 .vue files" trong `views/` | 46 | ✅ khớp |
| `README.md:127` | backend "515 tests" | 515 (baseline đo phiên này) | ✅ khớp |
| `README.md:130` | frontend "178 tests, 29 files" | 178 pass / 1 skip / 29 file | ✅ khớp |
| `README.md:177` | "18 tables" (liệt kê) | 18 bảng thật (+ `sysdiagrams`) | ✅ khớp |
| `AGENTS.md:14` | backend "515 / 0 / 0 / 11" | 515 / 0 / 0 / 11 | ✅ khớp |
| `AGENTS.md:15` | frontend "178 / 1 (29 file)" | khớp | ✅ khớp |
| `AGENTS.md:16` | build "177.74 kB" | 177.74 kB | ✅ khớp |
| `AGENTS.md:17` | parity `1470\|43738\|5\|118\|29\|4\|3\|12\|10` | khớp | ✅ khớp |
| `docs/erd-sql-guide.md` | "18 bảng, 22 FK" | 19 `sys.tables` (18 thật), 22 FK | ✅ khớp |

## Ghi chú

- `README.md` giữ nguyên phần "Database Schema" 18 bảng — **đúng** (18 entity/table thật; `sysdiagrams` là bảng hệ thống SQL Server).
- Không phát hiện drift nào khác ở tài liệu vận hành sau v15 (v15 đã tự sửa nhiều drift — F-15-10).
- **`docs/demo-engflow-4-chuc-nang.md`** được **viết lại toàn bộ** (xem REPORT §deliverable): bản cũ mô tả streak đọc
  `users.current_streak`/`lastStudyDate` — **đã lỗi thời** vì v13 đã chuyển sang bảng `study_days`
  (`StreakService` nay uỷ quyền `StudyActivityService`). Bản mới dẫn code hiện tại + kiểm chứng tồn tại.
