# Checklist chất lượng requirements — audit-v6-full

- [x] R1: Mọi yêu cầu của user được ánh xạ sang task (codebase scan→A1, API→A2, UI↔API→A3, DB+perf→A4, browser verify→A5, design sync→A6, font BVP→A6, báo cáo→A7, sub-agent→A8, workflow→A9, vá prompt→A10)
- [x] R2: DoD đo được (không có "kiểm tra kỹ" mơ hồ — mỗi mục có số/boolean)
- [x] R3: Lỗ hổng prompt thiết kế được liệt kê + vá trước khi code (G1–G7)
- [x] R4: Phạm vi in/out rõ ràng, ràng buộc kế thừa từ constitution + AGENTS.md
- [x] R5: Assumptions ghi thành clarify table, user review được
- [x] R6: Không mâu thuẫn với audit-v5 (font đã đổi → chỉ verify; TTS/CLS đã xong → không làm lại)
- [x] R7: Baseline test được đo trước khi sửa (247 backend / 73 frontend)
- [x] R8: Endpoint 404/405 được phân loại "đúng hành vi" vs "lỗi" (xem api-sweep)
- [ ] R9: Mỗi fix có test hoặc bằng chứng tái kiểm (đang thực hiện ở phase fix)
- [ ] R10: REPORT đủ 4 phần: đã làm / chưa / fix+cách / skill đã nạp (phase 6)
