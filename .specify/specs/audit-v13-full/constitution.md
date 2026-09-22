# audit-v13-full — constitution compliance

**Ratified for this audit:** 2026-09-22 (+07) · **Base:** EngFlow Constitution v1.0.1 (`.specify/memory/constitution.md`)

## Article I — Kỹ thuật

| # | Nguyên tắc | Áp dụng trong v13 | Trạng thái |
|---|---|---|---|
| P1 | Baseline xanh, **đếm từ run log** | Backend **499 / 0 fail / 0 error / 11 skipped** (`evidence/baseline-backend.log`); frontend **127 / 1 skipped (25 file)** → sau fix **130 / 1 skipped (26 file)** | ✅ |
| P2 | Controller → Service → Repository; DTO tách riêng | Fix F-13-01 đặt validation ở `ExerciseService` (service layer), không nhét vào controller | ✅ |
| P3 | `ddl-auto=update`, Flyway disabled, đổi schema = SQL + entity + verify container | v13 **không đổi schema**; mọi DML probe xoá bằng ID liệt kê | ✅ |
| P4 | AI local (Ollama 1.5b/3b, Whisper :9002) | Không thêm cloud AI | ✅ |
| P5 | Hiệu năng phải **đo trước/sau** | Phase 5: median 5 lần; từ chối tối ưu khi số không biện minh | ⏳ |

## Article II — Thiết kế

| # | Nguyên tắc | Áp dụng | Trạng thái |
|---|---|---|---|
| P6 | **Một** font Be Vietnam Pro; cấm Outfit/Plus Jakarta | Verify: `index.html:54`, `tailwind.config.js:76-80`, `design-system.css:86`; grep Outfit/PlusJakarta = **0** | ✅ |
| P7 | UI tiếng Việt; **WCAG 2.2 AA bắt buộc** | Fix F-13-01 dùng thông điệp tiếng Việt + `role="alert"`; Phase 3 đo contrast/tap-target live | ⏳ |
| P8 | Mỗi thay đổi có **bằng chứng runtime** | F-13-01: ảnh UI trước/sau + HTTP 400/200 thật + 9 test hồi quy | ✅ |

## Article III — Governance

- **Amendment:** không sửa constitution (không có nguyên tắc nào bị đổi).
- **Compliance check:** thực hiện ở Phase 0 (mục này).
- **Complexity budget:** fix F-13-01 dùng giải pháp nhàm chán — validate ở service + không đổi loại ở UI; không thêm abstraction mới.

## Ràng buộc bổ sung từ người dùng (v13)

- **Cấm mock data linh tinh** — chỉ dùng dữ liệu THẬT để chứng minh. Mock chỉ trong unit test và phải ghi rõ.
- **Review chéo mọi fix** do AI sinh / mọi liên kết UI↔API↔DB mới (adversarial verify).
- **Chạy 2 vòng**, sửa tận gốc; loop-until-dry.
- **Không tạo GitHub issues** — `tasks.md` là nguồn duy nhất.
