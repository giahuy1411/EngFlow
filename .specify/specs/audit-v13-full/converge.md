# audit-v13-full — converge

**Ngày:** 2026-09-22 (+07) · Soát codebase vs `spec.md` / `plan.md` / `tasks.md`, append việc còn thiếu.

## 1. Soát từng requirement trong spec

| Req | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| **R1** | Mọi con số là đo của phiên này | ✅ | `baseline.md`, `baseline-after.md`; mọi finding trỏ artifact |
| **R2** | API surface được **chạy**, kiểm hợp đồng, role 2 chiều | ✅ | `api-sweep.json` 143 pass; workflow sweep 199 pass |
| **R3** | UI đối chiếu API, không test rời | ✅ | `e2e-flows.md` T4.1–T4.7 (UI→API→DB) |
| **R4** | DB audit như dữ liệu, constraint **hiệu lực** | ✅ | `db-audit.md` (scratch DB + DROP) |
| **R5** | Design system verify + tìm lỗ hổng prompt | ✅ | `prompt-fixes.md` (H1–H4); `ui-sweep.md` (font, contrast) |
| **R6** | Vòng 2 rộng hơn, lặp đến khi cạn | ✅ | `round-2.md` — 2 vòng liên tiếp 0 lỗi mới |
| **R7** | Kỷ luật bằng chứng, không cap im lặng | ✅ | mọi giới hạn ghi trong `REPORT.md` §3 |
| **R8** | **Chỉ dữ liệu THẬT** | ✅ | F-13-01 dùng hàng 777434; E2E tạo/chấm thật; mock chỉ trong unit test và ghi rõ |
| **R9** | Review chéo mọi fix AI sinh | ✅ | adversarial review bắt 6 điểm của F-13-01; tôi verify độc lập F-13-07/14/15 |

## 2. Soát từng Phase trong plan

| Phase | Trạng thái | Ghi chú |
|---|---|---|
| P0 Freeze & baseline | ✅ | T0.1–T0.8 |
| P1 DB trong Docker | ✅ | có phát hiện F-13-07/08/09/10 |
| P2 API sweep | ✅ | 143 pass v13 + 199 pass workflow |
| P3 UI sweep 2 MCP | ✅ | 117 route×role, 0 guardFails |
| P4 E2E 3 tầng | ✅ | T4.1, T4.2b/c, T4.3, T4.4, T4.5, T4.6, T4.7 |
| P5 Hiệu năng | ✅ | 35 endpoint median-5; CLS; từ chối tối ưu có lý do |
| P6 Fix tận gốc + test | ✅ | 12 finding fixed; +30 test |
| P7 Vòng 2 loop-until-dry | ✅ | 0 lỗi mới ở 2 vòng liên tiếp |
| P8 Converge/Analyze/Report | ✅ | file này + `analyze.md` + `REPORT.md` |

## 3. Việc còn thiếu — append vào tasks.md

Các mục **chưa làm** đã được ghi rõ trong `findings.md` và `REPORT.md` §3. Không có requirement nào bị bỏ mà không ghi:

- **F-13-02** (Lesson Builder không tới học viên) — **cần quyết định chủ sản phẩm**.
- **F-13-07** (`study_policy` CHECK) — cần **backup trước DML**; hướng fix đã ghi.
- **F-13-08** (`users.current_streak` cũ) — **nợ dữ liệu**, cần quyết định.
- **F-13-10** (index dư) — **DEFERRED có số**, P5 cấm tối ưu khi chưa đo.
- **T4.8** (payment checkout E2E) — chỉ chạm create-order + webhook sai chữ ký, **biên giới tiền thật**.
- **AI `generate-async` E2E** — chưa chạy (GPU); đã verify đường **validate** thay thế + ghi BLOCKED.

## 4. Kết luận converge

Codebase sau v13 **khớp với spec**: mọi requirement R1–R9 có bằng chứng; mọi finding có trạng thái rõ ràng (FIXED/OPEN/DEFERRED) kèm lý do. **Không có task nào bị bỏ quên.**
