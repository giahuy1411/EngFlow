# audit-v14-full — T8.3 secret / PII scan của evidence

**Ngày:** 2026-09-25 (+07) · **Phạm vi:** toàn bộ `.specify/specs/audit-v14-full/`

## Quét

| Mẫu | Kết quả |
|---|---|
| JWT thật (3 đoạn base64 phân cách dấu chấm) | **0** |
| `Bearer <token>` literal trong evidence | **0** |
| `YourPassword123` (SA password) | **0** |
| `password=`/`secret=`/`api_key=` gán giá trị thật | **0** (chỉ chuỗi mô tả) |
| `eyJ…` | chỉ trong `lh-home/report.html` — là JSON của Lighthouse (không phải JWT) |
| PII học viên (email thật ngoài seed) | chỉ email **seed công khai** (`user@gmail.com`, `admin@gmail.com`) + email audit `*@test.local` tự tạo |

## Kết luận

- **0 secret**, **0 PII nhạy cảm** trong evidence.
- Token JWT dùng trong probe **không** bị ghi vào artifact (chỉ ghi trạng thái `tokens: {user:true, admin:true}`).
- Không copy `.bak` (36MB PII) hay `.env` vào evidence.
- Chỉ còn WARN đã biết: mật khẩu seed `123456` (AGENTS.md đã ghi; là tài khoản demo công khai).
