# 📚 EngFlow - Website Tự Học Ngoại Ngữ (Tiếng Anh)

Dự án này là nền tảng tự học ngoại ngữ trực tuyến được xây dựng bằng **Spring Boot 4.x / 3.x** cho Backend, **Vue.js 3 + Vite + Pinia** cho Frontend và hệ quản trị cơ sở dữ liệu **Microsoft SQL Server + Redis** chạy trên môi trường Docker.

---

## 🛠️ Yêu Cầu Hệ Thống
- **Docker Desktop** (để khởi chạy SQL Server và Redis)
- **Java JDK 17 hoặc 21 hoặc 25**
- **Node.js 16+ & npm**
- **Maven 3.8+**

---

## 🚀 Hướng Dẫn Cài Đặt và Chạy Dự Án

### Bước 1: Khởi chạy các dịch vụ lưu trữ (Docker)
Mở terminal tại thư mục gốc của dự án và chạy lệnh sau để khởi động SQL Server và Redis:
```bash
docker-compose up -d
```
*Lưu ý: SQL Server sẽ chạy trên port `1433` và Redis chạy trên port `6379`.*

### Bước 2: Tạo Cơ Sở Dữ Liệu `english_learning`
Do SQL Server JDBC không tự động tạo database mới, bạn cần kết nối vào SQL Server và tạo cơ sở dữ liệu tên là `english_learning`.
Bạn có thể dùng công cụ như Azure Data Studio, DBeaver hoặc SSMS kết nối bằng thông tin sau:
- **Host:** `localhost,1433`
- **Username:** `sa`
- **Password:** `YourPassword123`

Sau đó chạy truy vấn SQL để tạo database:
```sql
CREATE DATABASE english_learning;
```

### Bước 3: Khởi chạy Backend (Spring Boot)
1. Di chuyển vào thư mục gốc của dự án.
2. Chạy lệnh biên dịch và khởi động:
```bash
mvn clean install
mvn spring-boot:run
```
Khi ứng dụng khởi động thành công, Flyway Migrations sẽ tự động tạo bảng (schema) và chèn dữ liệu mẫu (seed data) vào database.
*Backend API chạy tại: `http://localhost:8080`*

### Bước 4: Khởi chạy Frontend (Vue 3 + Vite)
1. Mở một terminal mới và di chuyển vào thư mục `frontend/`:
```bash
cd frontend
```
2. Cài đặt các thư viện phụ thuộc:
```bash
npm install
```
3. Chạy dev server:
```bash
npm run dev
```
*Frontend chạy tại: `http://localhost:5173`*

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
