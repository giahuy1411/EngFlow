# audit-v14-full — constitution (bản áp dụng cho phiên này)

Kế thừa `.specify/memory/constitution.md` v1.0.1 (EngFlow, ratified 2026-09-01). Phiên v14 **không sửa**
hiến pháp dự án; file này ghi lại các nguyên tắc **ràng buộc phiên audit v14** và cách áp dụng.

## Article I — Nguyên tắc kỹ thuật (áp dụng nguyên)

- **P1** Baseline xanh là tiền đề. Trước/sau mọi thay đổi: backend `mvnw.cmd -o test`, frontend
  `npx vitest run`. **Đếm từ run log**, không đếm `target/surefire-reports` XML (từng ảo +8).
  Mỗi kỳ audit PHẢI đo lại và ghi giá trị thực tế của phiên mình.
- **P2** Kiến trúc phân lớp bất biến: Controller → Service → Repository; DTO tách riêng;
  package `com.datn.engflow`. Vue `<script setup>`, service module gọi qua `api` instance.
- **P3** Schema DB do Hibernate `ddl-auto=update` quản lý; Flyway disabled. Đổi schema = SQL trực
  tiếp + entity + kiểm chứng trên container `engflow-sqlserver`.
- **P4** AI local là ràng buộc sản phẩm: Ollama `qwen2.5:1.5b`/`3b`, Whisper sidecar :9002.
  Không thêm dependency cloud AI trả phí.
- **P5** Hiệu năng được đo, không bịa. Mọi tuyên bố tối ưu phải kèm chỉ số trước/sau. Không tối ưu
  khi chưa đo.

## Article II — Nguyên tắc thiết kế (áp dụng nguyên)

- **P6** Design System "Playful Geometric" + font **Be Vietnam Pro** (duy nhất). Cấm Outfit /
  Plus Jakarta Sans trong code mới. Shadow cứng `pop-*`, border 2px `#1E293B`, radius 8/16/24/full.
- **P7** UI text tiếng Việt, thuật ngữ kỹ thuật tiếng Anh. A11y bắt buộc: focus-visible, skip-link,
  contrast AA, `prefers-reduced-motion`.
- **P8** Mỗi thay đổi có bằng chứng runtime: CRUD/AI verify bằng API thật (:8080) **và** UI thật
  (chrome-devtools/playwright MCP) **và** hàng DB thật — không dừng ở "code nhìn có vẻ đúng".

## Article III — Ràng buộc bổ sung của phiên v14

- **V1 (ràng buộc người dùng).** Mọi file/folder rác sinh ra khi kiểm thử PHẢI được xoá sau khi
  hoàn thành; báo cáo cuối liệt kê chi tiết file đã xoá. → `cleanup-manifest.md` là artifact bắt buộc.
- **V2 (ràng buộc người dùng).** Phạm vi fix = **chỉ lỗi đo được trong v14**. F-13-02 A2/A3 và
  F-13-09 giữ là quyết định của chủ sản phẩm, không tự triển khai.
- **V3.** Không tạo GitHub issue. `tasks.md` là nguồn duy nhất.
- **V4.** Kết luận các bản v1–v13 là **giả thuyết để kiểm chứng**, không phải nguồn. Đo lại mọi số.
- **V5.** Mỗi finding phải có **probe thứ 2 độc lập** bác-hoặc-xác-nhận trước khi đặt status cuối.

## Governance

- **Compliance check:** đầu mỗi phase, so việc làm vs Article I/II/III.
- **Conflict resolution:** AGENTS.md quy định chi tiết vận hành; hiến pháp dự án quyết định khi mâu thuẫn.
- **Complexity budget:** ưu tiên giải pháp nhàm chán, đúng; cấm abstraction đơn-use và feature không
  được yêu cầu.
