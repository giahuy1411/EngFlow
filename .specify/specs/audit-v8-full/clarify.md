# Clarify — audit-v8-full

Câu hỏi trong prompt gốc và câu trả lời chốt (không cần hỏi lại user; suy luận từ AGENTS.md + hiện trạng repo):

1. **"kiểm tra backend bằng chạy toàn bộ api"** → phạm vi = 139 endpoint mapping đã enumerate từ source (`sweep/v8/sigs.txt`), không phải 53 như v7. Đã phủ 100% mapping bằng p1/p2/p3a/p3b.
2. **"bài học/bài tập, streak, đăng nhập/đăng kí, tìm kiếm/sắp xếp"** → mỗi mục có test UI thật + API thật (xem REPORT §2). Streak được verify bằng tài khoản freshly-registered để thấy 0→1, không chỉ đọc số có sẵn.
3. **"verify giao diện đã đồng bộ với prompt chưa"** → prompt thiết kế là Playful Geometric; chuẩn đo = constitution P6 (đã đổi font → Be Vietnam Pro). Đo bằng computed style trên 92 lượt route, không đo bằng mắt.
4. **"thay toàn bộ font = Be Vietnam Pro"** → ĐẠT: 0 phần tử dùng font khác trên mọi route đã quét.
5. **"tuân thủ workflow constitution → specify → clarify → checklist → plan → tasks → implement → converge (+analyze)"** → áp dụng; `.specify/specs/audit-v8-full/` là artefact.
6. **"dùng skill trong ~/.dsh/skills phù hợp ngữ cảnh"** → đã nạp và dùng: prompt-master, speckit-* (workflow), frontend-design, java-springboot/java-coding-standards/java-docs, karpathy-guidelines, accessibility, generate-test-cases/generate-tests, addyosmani-* (debugging, code-review, performance, security-hardening, spec-driven, test-driven, increment-implementation).
7. **Mâu thuẫn tiềm ẩn trong prompt**: yêu cầu "chạy toàn bộ API" + "tương tác toàn bộ chức năng" xung đột với guardrail "không mutate dữ liệu thật / backup-before-DML". Cách giải quyết: mutation chỉ trên bản ghi `ZZ v8*` tự tạo tự xoá; AI generation chạy trên lesson tạm rồi DELETE cascade; các endpoint huỷ diệt hàng loạt (`/seed`, `generate-batch?force`) KHÔNG gọi, chỉ verify guard.
8. **Thiếu trong prompt**: không nêu tiêu chí "lỗi" là gì. Đã tự định nghĩa và ghi thành checklist: 5xx từ input client hợp lệ = lỗi; hành vi bảo mật sai = lỗi; lệch design token = lỗi; console error trên route = lỗi.
