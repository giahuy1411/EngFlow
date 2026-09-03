# Clarify: audit-v5-full

Câu hỏi đã hỏi người dùng (trước khi viết spec) và câu trả lời — ghi lại để về sau không đoán lại:

| # | Câu hỏi | Trả lời | Hệ quả vào spec/plan |
|---|---------|---------|----------------------|
| Q1 | Đơn hàng ENGF8AB9431CE85 — đã chuyển khoản thật chưa? | **"Tôi đã chuyển khoản thật"** | Phải xác minh DB có SUCCESS transaction khớp; nếu không → reset premium và chạy E2E webhook sạch. **Kết quả:** DB không có giao dịch thật cho user 2 (premium cũ từ webhook giả audit-v4) → đã reset, chạy lại E2E qua funnel với order `ENG2B698A6BC850`, idempotent replay verified. Ghi rõ trong REPORT là test-fake, không phải tiền thật. |
| Q2 | Phạm vi font: giữ JetBrains Mono cho code block hay thay hết? | **"Be Vietnam Pro tuyệt đối 100%"** | `--geo-font-mono` và mọi font-family phụ → Be Vietnam Pro; xóa Google Fonts link Jakarta Sans. |
| Q3 | (suy ra từ yêu cầu gốc) Data seed hỏng (MATCHING/FILL_BLANK placeholder) có sửa DB không? | Không được trả lời trực tiếp — quyết định theo hướng an toàn nhất | **Không** đụng hàng chục nghìn dòng seed (rủi ro mất bài thật của user). Guard phía UI (fallback input) + ghi nhận số dòng hỏng trong REPORT để chủ dự án quyết định migrate sau. |

Không cần thêm câu hỏi — các ambiguity còn lại đều có default an toàn theo constitution.
