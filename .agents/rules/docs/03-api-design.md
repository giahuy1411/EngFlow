# 03 — REST API Design

---

## 1. URL Convention

```
🔴 Dùng kebab-case cho URL path
🔴 Resource name là danh từ số nhiều
🔴 Không dùng động từ trong URL (động từ thể hiện qua HTTP method)
🔴 Không có trailing slash
```

```
✅ Đúng
GET    /api/lessons
GET    /api/lessons/{id}
GET    /api/lessons/{id}/exercises
POST   /api/lessons
PUT    /api/lessons/{id}
DELETE /api/lessons/{id}
POST   /api/auth/refresh-token
GET    /api/exercise-submissions/{userId}

❌ Sai
GET  /api/getLesson          ← động từ trong URL
GET  /api/lesson             ← số ít
GET  /api/lessons/           ← trailing slash
GET  /api/Lessons            ← uppercase
POST /api/lessons/create     ← động từ thừa
```

---

## 2. HTTP Methods

| Method | Dùng Cho | Idempotent | Body |
|--------|----------|-----------|------|
| GET | Lấy dữ liệu | ✅ | ❌ |
| POST | Tạo mới | ❌ | ✅ |
| PUT | Cập nhật toàn bộ | ✅ | ✅ |
| PATCH | Cập nhật một phần | ❌ | ✅ |
| DELETE | Xóa | ✅ | ❌ |

```
🔴 Không dùng GET để thay đổi data
🔴 Không dùng POST cho tất cả mọi thứ
🟡 Dùng PATCH khi chỉ update 1-2 trường (ví dụ: toggle isPublished)
```

---

## 3. HTTP Status Codes

```
🔴 Trả về status code đúng nghĩa — không trả 200 cho mọi thứ
```

| Tình huống | Code |
|-----------|------|
| Lấy dữ liệu thành công | 200 OK |
| Tạo mới thành công | 201 Created |
| Xóa thành công (không có body) | 204 No Content |
| Lỗi validation / input sai | 400 Bad Request |
| Chưa xác thực (không có token) | 401 Unauthorized |
| Không có quyền (sai role) | 403 Forbidden |
| Không tìm thấy resource | 404 Not Found |
| Conflict (email đã tồn tại) | 409 Conflict |
| Lỗi server | 500 Internal Server Error |

---

## 4. Response Format

```
🔴 Tất cả response phải nhất quán về format
🔴 Không trả về entity trực tiếp — luôn dùng DTO Response
🔴 Không trả về password_hash hoặc thông tin nhạy cảm
```

### Success Response
```json
// Single object
{
  "id": 1,
  "title": "Bài học 1 — Chào hỏi cơ bản",
  "level": "BEGINNER",
  "createdAt": "2024-01-15T10:30:00"
}

// List (không wrap thêm)
[
  { "id": 1, "title": "..." },
  { "id": 2, "title": "..." }
]

// Paginated list
{
  "content": [...],
  "totalElements": 50,
  "totalPages": 5,
  "currentPage": 0,
  "size": 10
}
```

### Error Response
```json
// Single error
{
  "error": "Không tìm thấy bài học với id=99"
}

// Validation errors (multiple fields)
{
  "title": "Tiêu đề không được để trống",
  "level": "Cấp độ không hợp lệ"
}
```

---

## 5. Pagination & Filtering

```
🟡 Endpoint trả về list phải hỗ trợ pagination
🟡 Default page size: 10, max: 100
🔴 Không trả về toàn bộ data không phân trang (nguy cơ OOM)
```

```
// Query params chuẩn
GET /api/lessons?page=0&size=10&sort=createdAt,desc
GET /api/lessons?level=BEGINNER&page=0&size=10
GET /api/vocabulary?lessonId=5&page=0&size=20

// Spring controller
@GetMapping
public Page<LessonResponse> getAll(
    @RequestParam(defaultValue = "0")  int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false)    String level) { ... }
```

---

## 6. API Versioning

```
🟡 Version trong path: /api/v1/...
🟢 Áp dụng từ khi có breaking change — không cần versioning ngay từ đầu
```

---

## 7. Request/Response Naming

```
🔴 camelCase cho tất cả field trong JSON
🔴 Timestamp format: ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)
🟡 Boolean field dùng prefix is hoặc has: isPublished, hasCompleted
```

```json
// ✅ Đúng
{
  "lessonId": 1,
  "isPublished": true,
  "createdAt": "2024-01-15T10:30:00",
  "completionPct": 75.5
}

// ❌ Sai
{
  "lesson_id": 1,        // snake_case
  "published": true,     // thiếu is prefix
  "created_at": "...",   // snake_case
  "completion": 75.5     // tên mơ hồ
}
```

---

## 8. Auth Header

```
🔴 Dùng Bearer token trong Authorization header
🔴 Không truyền token qua query param hoặc request body
```

```
// ✅
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

// ❌
GET /api/lessons?token=eyJhbGciOiJIUzI1NiJ9...
```

---

## 9. Swagger/OpenAPI Documentation

```
🟡 Mọi endpoint phải có @Operation annotation mô tả
🟡 Response codes phải khai báo đủ trong @ApiResponse
🟢 Request/Response DTO phải có @Schema annotation mô tả field
```

```java
@Operation(summary = "Lấy danh sách bài học", description = "Có hỗ trợ phân trang và lọc theo level")
@ApiResponse(responseCode = "200", description = "Thành công")
@ApiResponse(responseCode = "401", description = "Chưa xác thực")
@GetMapping
public Page<LessonResponse> getAll(...) { ... }
```
