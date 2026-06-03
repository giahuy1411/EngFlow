# 06 — Security

---

## 1. Authentication & JWT

```
🔴 JWT secret tối thiểu 256-bit, lưu trong environment variable — không hardcode
🔴 Access token TTL: 24h. Refresh token TTL: 7 ngày
🔴 Không lưu token trong localStorage — dùng memory hoặc httpOnly cookie
🔴 Refresh token phải được invalidate khi logout
🟡 Revoke refresh token khi đổi password
```

```java
// ✅ Đúng — từ env
@Value("${jwt.secret}")
private String jwtSecret;

// ❌ Sai — hardcode
private String jwtSecret = "mysecretkey123";
```

```js
// ✅ Frontend — lưu token trong memory (Pinia store), không trong localStorage
const token = ref(null)  // mất khi reload — đây là đánh đổi cần biết

// ⚠️ Nếu cần persist qua reload, dùng sessionStorage (không phải localStorage)
// và chấp nhận risk XSS attack thấp hơn
```

---

## 2. Password

```
🔴 Hash password bằng BCrypt với strength 10+
🔴 Không log, không trả về, không so sánh plain text password
🔴 Validate độ phức tạp: tối thiểu 8 ký tự, có chữ và số
```

```java
// ✅ Hash khi đăng ký
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}

// Dùng
String hashed = passwordEncoder.encode(request.getPassword());
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);

// ❌ Sai
String hashed = MD5(password);        // MD5 không an toàn
String hashed = SHA1(password);       // SHA1 không an toàn
user.setPassword(rawPassword);        // plain text
```

```java
// ✅ Validate password complexity
@Pattern(
    regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,}$",
    message = "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số"
)
private String password;
```

---

## 3. Input Validation & SQL Injection

```
🔴 Validate mọi input từ người dùng — không tin tưởng bất kỳ data nào từ client
🔴 Dùng JPA/Hibernate parameterized queries — không ghép chuỗi SQL
🔴 Validate và sanitize tất cả path variable, request param, header
```

```java
// ✅ JPA — tự động parameterized
repository.findByEmail(email);  // an toàn

// ✅ @Query với @Param
@Query("SELECT l FROM Lesson l WHERE l.level = :level")
List<Lesson> findByLevel(@Param("level") LessonLevel level);

// ❌ String concat — SQL injection
@Query(value = "SELECT * FROM lessons WHERE level = '" + level + "'",
       nativeQuery = true)  // NGUY HIỂM
```

---

## 4. Authorization

```
🔴 Kiểm tra quyền ở Service layer — không chỉ dựa vào SecurityConfig
🔴 User chỉ được thao tác trên data của chính họ
🔴 Admin endpoints phải có @PreAuthorize("hasRole('ADMIN')")
🟡 Dùng @PreAuthorize trên method — defense in depth
```

```java
// ✅ Kiểm tra ownership trong Service
public ProgressResponse getMyProgress(Long userId, Authentication auth) {
    String currentEmail = auth.getName();
    User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();

    if (!currentUser.getId().equals(userId)) {
        throw new ForbiddenException("Không có quyền xem progress của người khác");
    }
    // ...
}

// ✅ Admin only
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteLesson(@PathVariable Long id) { ... }
```

---

## 5. CORS

```
🔴 Chỉ allow origins đã biết — không dùng allowedOrigins("*") trên production
🔴 Chỉ allow HTTP methods cần thiết
🟡 Riêng dev và prod có CORS config khác nhau
```

```java
// ✅ Production CORS
cfg.setAllowedOrigins(List.of("https://yourdomain.com"));

// ✅ Dev CORS
cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

// ❌ Production với wildcard
cfg.setAllowedOrigins(List.of("*"));  // KHÔNG BAO GIỜ trên production
```

---

## 6. Rate Limiting

```
🟡 Rate limit cho auth endpoints (login, register, refresh-token)
🟡 Login: tối đa 5 lần/phút per IP — sau đó block 15 phút
🟢 General API: 100 requests/phút per user
```

```java
// Dùng Bucket4j hoặc Redis để implement rate limiting
// Hoặc để Nginx xử lý (xem docker-deployment skill)

// Nginx config
limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
location /api/auth/login {
    limit_req zone=login burst=3 nodelay;
    proxy_pass http://backend;
}
```

---

## 7. Secrets Management

```
🔴 Không commit bất kỳ secret, password, API key nào lên git
🔴 Dùng environment variables hoặc vault cho secrets
🔴 File .env không được commit (thêm vào .gitignore)
🟡 Tạo .env.example với placeholder values để team reference
```

```bash
# ✅ .env.example (commit được — không có giá trị thật)
DB_PASSWORD=your-strong-password-here
JWT_SECRET=your-base64-encoded-256bit-secret
REDIS_PASSWORD=your-redis-password

# ❌ .env (không commit)
DB_PASSWORD=Dev@12345realpassword
JWT_SECRET=abc123
```

---

## 8. XSS Protection

```
🟡 Escape HTML trước khi render user-generated content
🟡 Không dùng v-html với content từ user
🟡 Content-Security-Policy header qua Nginx
```

```vue
<!-- ✅ Text binding — tự escape -->
<p>{{ userContent }}</p>

<!-- ❌ v-html với user content — XSS risk -->
<p v-html="userContent"></p>  <!-- NGUY HIỂM nếu content từ user -->
```

```nginx
# Nginx security headers
add_header X-Content-Type-Options nosniff;
add_header X-Frame-Options DENY;
add_header X-XSS-Protection "1; mode=block";
add_header Content-Security-Policy "default-src 'self'; script-src 'self'";
```

---

## 9. File Upload (Media)

```
🔴 Validate file type bằng MIME type — không chỉ dựa vào extension
🔴 Giới hạn file size (audio: 10MB, image: 5MB)
🔴 Không lưu file trực tiếp trên server — upload lên AWS S3/CloudFlare
🔴 Không cho phép execute file upload (chặn .exe, .php, .jsp)
🟡 Rename file khi lưu — không dùng tên file do user cung cấp
```

---

## 10. Security Checklist Trước Khi Deploy

```
□ JWT secret đủ mạnh và từ env var
□ CORS chỉ allow domain production
□ Rate limiting bật cho auth endpoints
□ HTTPS được cấu hình (không có HTTP thuần)
□ Không có debug endpoint còn mở (/h2-console, /actuator không được expose)
□ SQL injection: tất cả query dùng JPA parameterized
□ Không có secret trong git history (git log --all --grep="password")
□ Security headers được set trong Nginx
□ Password validation bật phía server
□ File upload validation bật
```
