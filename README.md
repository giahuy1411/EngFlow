# 📚 EngFlow - Website Tự Học Ngoại Ngữ (Tiếng Anh)

Dự án này là nền tảng tự học ngoại ngữ trực tuyến được xây dựng bằng **Spring Boot 4.x / 3.x** cho Backend, **Vue.js 3 + Vite + Pinia** cho Frontend và hệ quản trị cơ sở dữ liệu **Microsoft SQL Server + Redis** chạy trên môi trường Docker.

---

## 🛠️ Yêu Cầu Hệ Thống
- **Docker Desktop** (yêu cầu duy nhất — tất cả chạy trong container)

---

## 🚀 Hướng Dẫn Cài Đặt và Chạy Dự Án

### Bước 0: Tạo file `.env`
```bash
cp .env.example .env
```
Chỉnh sửa `.env` với password và secret phù hợp.

### Chạy toàn bộ hệ thống (khuyên dùng)
```bash
docker-compose up -d
```
Lệnh này sẽ khởi động **tất cả** dịch vụ: SQL Server, Redis, Backend (JDK 25), Frontend (Node 24).

Sau khi các container khởi động (khoảng 1-2 phút lần đầu), truy cập:
- **Frontend:** `http://localhost:5173`
- **Backend API:** `http://localhost:8080`

*Lưu ý: SQL Server port host `1434` (tránh xung đột), Redis port `6379`.*

### Chạy từng phần (dành cho development)

#### Backend (Spring Boot)
```bash
docker-compose up -d backend
```
*Không cần cài JDK/Maven — backend build & chạy trong container với JDK 25*

Sau đó có thể chạy thủ công nếu muốn:
```bash
./mvnw clean install
./mvnw spring-boot:run
```

#### Frontend (Vue 3 + Vite)
```bash
docker-compose up -d frontend
```
*Không cần cài Node.js/npm — frontend chạy trong container với Node 24*

Sau đó có thể chạy thủ công nếu muốn (vào thư mục `frontend/`):
```bash
npm install
npm run dev
```

### Kết nối Database (từ host)
- **Host:** `localhost,1434`
- **Username:** `sa`
- **Password:** `YourPassword123`
- **Database:** `english_learning` (tự động tạo bởi init container)

---

## 🔑 Tài Khoản Học Thử (Default Seed Accounts)

Sau khi hệ thống khởi tạo thành công ở Bước 3, bạn có thể sử dụng các tài khoản mẫu dưới đây để đăng nhập trực tiếp trên giao diện:

| Email | Mật khẩu | Quyền |
|-------|----------|-------|
| `user@gmail.com` | `123456` | Học viên (USER) |
| `admin@gmail.com` | `123456` | Quản trị (ADMIN) |

---

## 📂 Cấu Trúc Dự Án
- `/src/main/java/com/datn/engflow/` : Mã nguồn Java Spring Boot backend.
- `/src/main/resources/db/migration/` : Các file migration của Flyway tạo cấu trúc DB & chèn dữ liệu mẫu.
- `/frontend/` : Mã nguồn ứng dụng Frontend Vue 3 + Vite.
  - `/frontend/src/store/` : Quản lý State bằng Pinia.
  - `/frontend/src/views/` : Giao diện người dùng (Lessons, Details, Profile, v.v.).
  - `/frontend/src/services/` : Gọi API thông qua Axios Service Pattern.
