# audit-v21-full — Secret scan

**Đo:** 2026-09-28 · quét `git ls-files` + `application.properties` + `.gitignore`.

## 1. File nhạy cảm có bị track không?

`git ls-files | grep -iE '\.env|secret|credential|\.pem|\.key'`:

| File | Track? |
|---|---|
| `.env.example` | ✓ (mẫu, không secret) |
| `frontend/.env.example` | ✓ (mẫu, không secret) |
| `.env` | ✗ — **gitignored** (`!!`) |
| `.env.bak-20260902`, `.env.bak-20260902b`, `.env.bak-pre-model-fix`, `.env.bak-user-sepay` | ✗ — **gitignored** (`.gitignore:30-32` `.env` / `.env.*`; dòng 89 `*.bak`) |
| `frontend/.env` | ✗ — gitignored |

`git ls-files | grep -c env.bak` = **0**. → **Không có secret nào bị commit.**

## 2. Giá trị mặc định yếu trong `application.properties` (đã biết, có mitigation)

| Key | Default | Rủi ro | Mitigation hiện có |
|---|---|---|---|
| `jwt.secret` | `${JWT_SECRET}` — **không default** | — | Bắt buộc set env, app không boot nếu thiếu |
| `spring.datasource.password` | `${DB_PASSWORD}` — **không default** | — | Bắt buộc set env |
| `sepay.webhook.secret` | `${SEPAY_WEBHOOK_SECRET:supersecret}` | Chữ ký HMAC đoán được nếu quên set | AGENTS.md: instance ngoài laptop PHẢI set env trước boot |
| `minio.secret-key` | `${MINIO_SECRET_KEY:minioadmin}` | Truy cập MinIO local | Chỉ ảnh hưởng container local |
| `cloudinary.api-secret` / `api-key` / `cloud-name` | `demo` | Upload Cloudinary fail (không lộ dữ liệu) | `.env` thật đã có key thật |
| `spring.mail.password` | `${MAIL_PASSWORD:testpassword}` | Gửi mail fail | `.env` thật đã set |

→ Đây là **default dev đã được tài liệu hoá** (AGENTS.md "Boundaries"), không phải finding mới.
Rủi ro thật chỉ khi triển khai instance ngoài mà quên set env — đã có cảnh báo trong repo.

## 3. Kết luận

- **0 secret bị commit.** `.env` + 4 file `.env.bak-*` đều gitignored.
- Default yếu là dev-only, có mitigation documented; **không xoá `.env.bak-*`** (chứa secret thật,
  cần cho rollback, đúng quyết định v20 — người dùng đã chọn "dọn an toàn").
- Không phát hiện credential nào lọt vào code Java/Vue/harness.
