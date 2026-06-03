# 📐 Bộ Rule — English Learning Platform

> Áp dụng bắt buộc cho toàn bộ team từ ngày khởi động dự án.  
> Mọi thay đổi rule phải được review và approve bởi Tech Lead.

---

## Danh Sách Rule

| # | File | Phạm Vi |
|---|------|---------|
| 01 | `docs/01-backend-java.md` | Java, Spring Boot, naming, code style |
| 02 | `docs/02-frontend-vue.md` | Vue.js 3, Pinia, Axios, Bootstrap |
| 03 | `docs/03-api-design.md` | REST API, request/response, versioning |
| 04 | `docs/04-database.md` | SQL Server, schema, query, migration |
| 05 | `docs/05-git-workflow.md` | Branch, commit message, PR, merge |
| 06 | `docs/06-security.md` | JWT, password, input validation, CORS |

## Config Files

| File | Dùng Cho |
|------|----------|
| `config/.editorconfig` | Toàn bộ dự án — indent, encoding |
| `config/checkstyle.xml` | Java code style (Maven plugin) |
| `config/.eslintrc.js` | Vue/JavaScript linting |
| `config/.prettierrc` | Vue/JavaScript formatting |
| `config/sonar-project.properties` | SonarQube code quality |

---

## Mức Độ Ưu Tiên

```
🔴 MUST    — Bắt buộc, không có ngoại lệ (lỗi → block merge)
🟡 SHOULD  — Nên làm, chỉ bỏ qua khi có lý do rõ ràng
🟢 MAY     — Khuyến nghị, tùy ngữ cảnh
```

---

## Quick Reference — Những Điều Cấm Tuyệt Đối

```
🚫 Commit trực tiếp lên main/develop
🚫 Push code chứa secret, password, API key
🚫 Bỏ qua @Valid trên request body
🚫 Gọi Axios trực tiếp trong Vue component (phải qua service)
🚫 Hardcode URL, port trong code
🚫 Dùng SELECT * trong production queries
🚫 Để TODO/FIXME quá 3 ngày không xử lý
🚫 Merge PR khi chưa có ít nhất 1 reviewer approve
🚫 Dùng System.out.println thay vì logger
🚫 Trả về password hash hoặc thông tin nhạy cảm trong API response
```
