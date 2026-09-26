# audit-v15-full — constitution (áp dụng cho phiên này)

Kế thừa `.specify/memory/constitution.md` (EngFlow v1.0.1) + `.specify/specs/audit-v14-full/constitution.md`.
Phiên v15 **không sửa** hiến pháp dự án; ghi lại ràng buộc riêng.

## Article I — Nguyên tắc kỹ thuật (P1–P5, áp dụng nguyên)
- **P1** baseline xanh, đếm **từ run log** (không đếm XML).
- **P2** phân lớp Controller → Service → Repository; DTO tách riêng.
- **P3** schema do Hibernate `ddl-auto=update`; Flyway disabled; đổi schema = SQL trực tiếp + entity + verify container.
- **P4** AI local (Ollama/Whisper); không thêm cloud AI trả phí.
- **P5** hiệu năng đo trước/sau; không tối ưu khi chưa đo.

## Article II — Nguyên tắc thiết kế (P6–P8)
- **P6** font duy nhất **Be Vietnam Pro**; cấm Outfit/Plus Jakarta.
- **P7** UI tiếng Việt; WCAG 2.2 AA bắt buộc.
- **P8** mỗi thay đổi có **bằng chứng runtime** (API thật + UI thật + hàng DB thật).

## Article III — Ràng buộc bổ sung phiên v15
- **V1 (người dùng).** Dọn rác sinh khi test + **liệt kê file đã xoá** → `cleanup-manifest.md`.
- **V2 (người dùng).** Phạm vi = **3 hạng mục đã chốt**: dọn nợ dữ liệu (C1/C2), **gỡ toàn bộ Đường B
  (Lesson Builder)**, timezone verify-only. Không mở rộng.
  *(Đổi hướng giữa phiên: ban đầu là "làm đầy đủ A2+A3"; người dùng sau đó xác nhận nội dung bài học lấy
  hoàn toàn từ `english-practice.net` và khối "Nội dung biên soạn" phải gỡ — xem `clarify.md` Q5/Q6.)*
- **V3.** Không GitHub issue; `tasks.md` là nguồn duy nhất.
- **V4.** Kết luận v1–v14 là **giả thuyết để kiểm chứng**.
- **V5.** Mỗi finding có **probe thứ 2 độc lập**.
- **V6 (mới).** Mọi DML hàng loạt **bắt buộc backup + restore-drill trước khi xoá**; parity sau cleanup
  phải khớp giá trị tính trước `1470|43735|5|118|29|4|3|12|10|5`.

## Governance
- Compliance check đầu mỗi phase. Conflict: AGENTS.md vận hành, hiến pháp quyết định.
- Complexity budget: ưu tiên giải pháp nhàm chán, đúng; cấm abstraction đơn-use và feature không được yêu cầu.
