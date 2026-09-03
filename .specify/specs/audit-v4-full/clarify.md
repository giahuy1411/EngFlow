# Clarify — audit-v4-full

Nguyên tắc: chỉ hỏi những câu KHÔNG tự trả lời được từ ngữ cảnh. 5 ambiguity
chính đã được giải quyết như sau (không block người dùng vì đã có chỉ thị rõ):

| # | Câu hỏi | Trả lời / Nguồn |
|---|---|---|
| 1 | Gói premium nào để test chuyển khoản thật? | Gói 10.000đ như audit-v3 (order mẫu ENG15C67AA3B771); xác nhận lại giá/gói thực tế trên PremiumPage khi test. **User đã hứa chuyển khoản khi được nhắc** — gate duy nhất phải dừng. |
| 2 | Được tạo/sửa data test không? | Được — đặt tên nhận diện "Audit v4 ..." và DỌN SAU (sự cố lesson 444 ở v3 là bài học: KHÔNG auto-accept dialog). |
| 3 | SePay token 401 thì xử lý thế nào? | Chẩn đoán root cause, báo user cần làm gì (cấp token/webhook); test tối đa đường hiện có và minh bạch kết quả. |
| 4 | Thời gian? | Không giới hạn — "chạy test lặp toàn bộ chức năng" (lời user). |
| 5 | Bug tìm thấy xử lý ngay? | Sửa tận gốc ngay trong phiên, commit Conventional Commits tiếng Anh, author giahuy1411. |

**Câu hỏi còn treo cho user (sẽ hỏi đúng lúc, không block bây giờ):**
- **GATE P: "Bạn đã chuyển khoản chưa?"** — hỏi tại bước S5 sau khi tạo order và hiển thị đủ thông tin CK.
