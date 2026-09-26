# audit-v15-full — secret scan

**Ngày:** 2026-09-25 (+07) · Phạm vi: mọi file sửa/tạo trong phiên + `evidence/**`.

## Phương pháp

1. Quét mẫu khóa thật: `sk-`, `pk_`, `xoxb-`, `ghp_`, `gho_`, `AIza`, `AKIA`, `-----BEGIN`.
2. Quét entropy cao: chuỗi `[A-Za-z0-9+/]{40,}` (lọc nhiễu identifier/camelCase).
3. Quét từ khóa `api_key|secret|password|token|bearer`.

## Kết quả

| Loại match | Đánh giá |
|---|---|
| `password_hash`, `jwt.secret`, `DB_PASSWORD=...`, `SEPAY_*`, `CLOUDINARY_*`, `MINIO_*` | **Tên biến / placeholder trong doc** — không phải giá trị |
| `password123`, `123456`, `DEFAULT_*_PASSWORD` | **Mật khẩu seed công khai đã biết** (AGENTS.md §41/§98) — không phải secret |
| `currentPassword`, `newPassword`, `reset-password`, `forgot-password` | Tên field/route |
| `localStorage.getItem('token')`, `Bearer ${token}`, `UsernamePasswordAuthenticationToken` | Identifier code |
| Chuỗi dài `[A-Za-z0-9+/]{40,}` | **Toàn camelCase identifier** (`findByIsPublishedTrueOrderByOrderIndexAsc`, …) |

## Kết luận

**0 secret thật.** Không có API key, token, hay mật khẩu thật trong file thay đổi hay evidence.
Backup DB (`.bak`, chứa PII) **giữ ngoài repo** tại `C:\Users\ASUS\engflow-backups\` — không commit.
Không có `.env` nào bị đụng.
