# EngFlow — Học thuộc demo tốt nghiệp: 4 chức năng (bản kiểm chứng từ code)

> Mỗi chức năng: ý tưởng 1 câu → luồng end-to-end → code thật → file liên quan → câu trả lời 30 giây.
> Mọi khẳng định dưới đây đã đối chiếu source ngày 16/09/2026. Đoạn code trích nguyên văn, chỉ cắt bớt phần không liên quan.
> Tài khoản demo: `user@gmail.com` / `admin@gmail.com`, mật khẩu `123456`.

---

## 1. Đăng nhập / Đăng ký

**Thuộc 1 câu:** Register kiểm tra trùng email + username, mã hoá BCrypt, tự trả JWT; login chuẩn hoá email, khoá tạm sau 5 lần sai (Redis), JWT HS256 TTL 900s; frontend lưu `token` + `user`, router guard 4 loại meta.

### 1.1. Đăng ký — `UserService.register()` (`src/main/java/com/datn/engflow/service/UserService.java`)

```java
if (userRepository.existsByEmail(request.getEmail())) {
    throw new ConflictException("Email đã tồn tại");          // → HTTP 409
}
if (userRepository.existsByUsername(request.getUsername())) {
    throw new ConflictException("Tên đăng nhập đã tồn tại");  // → HTTP 409
}
User user = User.builder()
        .passwordHash(passwordEncoder.encode(request.getPassword())) // BCrypt
        .currentLevel(LessonLevel.ELEMENTARY)                        // mặc định
        .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + request.getUsername())
        .isActive(true)
        .build();
// ... save rồi generateToken(...) → tự đăng nhập luôn, không bắt login lại
```

### 1.2. Đăng nhập — `UserService.login()` (cùng file, dòng 165+)

```java
String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
String lockKey = RedisConstants.LOGIN_LOCK_PREFIX + normalizedEmail; // "login_lock:"
String failKey = RedisConstants.LOGIN_FAIL_PREFIX + normalizedEmail; // "login_fail:"
// 1. Còn lock → báo thời gian chờ (đọc TTL của key), không thèm xác thực
// 2. authenticationManager.authenticate(...) sai → fails++, đủ 5 lần → set lock 15 phút
// 3. Đúng → xoá fail counter → generateToken(...) → streakService.recordAccess(userId)
```

Số liệu thật trong `config/RedisConstants.java`: `MAX_LOGIN_FAILS = 5`, `LOGIN_FAIL_TTL = 15 phút`, `LOGIN_LOCKOUT_MINUTES = 15`. Mọi thao tác Redis đều try/catch **fail-open**: Redis chết thì login vẫn chạy (chỉ mất chống brute-force), không sập app.

### 1.3. JWT — `security/JwtTokenProvider.java`, `application.properties`

```java
return Jwts.builder()
        .subject(email)                       // sub = email
        .claim("role", role)                  // "ADMIN" | "USER"
        .claim("isPremium", isPremium)
        .expiration(new Date(now.getTime() + jwtExpirationInMs))
        .signWith(getSigningKey())            // HS256, secret từ JWT_SECRET
        .compact();
```

`jwt.expiration=900000` (900 giây = 15 phút). Frontend `services/api.js` tự decode `payload.exp` **trước khi gửi request**: hết hạn thì `logout()` + đẩy về `/login`, không đợi server trả 401. Timeout axios: 10s mặc định, 60s cho `/api/admin`.

### 1.4. Phân quyền — `config/SecurityConfig.java` (stateless, tắt CSRF)

| Nhóm | Rule |
|---|---|
| permitAll | `POST /api/auth/register\|login\|forgot-password\|reset-password`, `GET /api/lessons/**`, `GET /api/vocabulary/search`, `/dictionary/*`, leaderboard, video-lessons/video-prompts/decks public, `/api/resources/**`, webhook SEPay |
| authenticated | `POST /api/lessons/*/exercises/submit\|grade`, `/attempts/**`, `POST /api/vocabulary`, `POST /api/auth/**` |
| `hasRole('ADMIN')` | `POST/PUT/DELETE /api/lessons/**`, `/api/admin/**`, `/api/v1/admin/**`, `/api/exercises/**` |

⚠️ Bẫy từng sập thật (F54): Spring lấy **rule khớp đầu tiên**, nên rule hẹp `GET /api/lessons/*/exercises/attempts/** authenticated` phải đặt **trước** rule rộng `GET /api/lessons/** permitAll`, nếu không lịch sử làm bài thành public.

### 1.5. Router guard — `frontend/src/router/index.js`

```js
if (to.meta.requiresPremium && !auth.isAdmin && !auth.isPremium) // đá /premium
if (to.meta.requiresAuth && !auth.isLoggedIn)                    // đá /login
if (to.meta.guestOnly && auth.isLoggedIn)                        // /login khi đã login → đá /lessons
if (to.meta.requiresAdmin && !auth.isAdmin)                      // đá /
```

`isAdmin` đọc từ `localStorage.user` (do `store/modules/auth.js` `mapUser()` chuẩn hoá từ `UserResponse`), **không** decode từ token. Quên mật khẩu: OTP 6 số (`SecureRandom`), TTL 10 phút (`otp:reset:`), giới hạn 3 OTP/15 phút (`OTP_MAX_PER_WINDOW`).

**Trả lời 30 giây:** "Đăng ký chặn trùng email và username, mật khẩu BCrypt. Đăng nhập chuẩn hoá email, sai 5 lần khoá 15 phút bằng Redis. Token JWT 15 phút, phân quyền 3 lớp ở SecurityConfig, router Vue guard 4 loại meta."

---

## 2. Bài học / Bài tập

**Thuộc 1 câu:** Lesson là lộ trình (`orderIndex`), Exercise thuộc lesson (`orderIndex`); học: xem danh sách → chi tiết → làm bài → `grade` chấm thử / `submit` chấm + lưu `ExerciseAttempt` (details JSON) → xem lịch sử attempts.

### 2.1. Danh sách — `LessonController.getAllLessons()` + `LessonService.getPublishedLessonPage()`

```java
// Controller: sort CỐ ĐỊNH, client không được truyền sort (giữ lộ trình học)
PageRequest.of(Math.max(page, 0), size /* kẹp 1–100 */,
    Sort.by("orderIndex").ascending().and(Sort.by("id")))
```

```java
// Service: projection nhẹ, không hydrate cột content NVARCHAR(MAX)
Page<LessonListProjection> page = lessonRepository.findPublishedPageProjection(
        keyword != null && !keyword.isBlank() ? keyword.trim() : null, level, pageable);
// ... rồi join Progress của ĐÚNG các lesson trong trang (findByUserIdAndLessonIdIn)
// → isCompleted, completionPercentage. Ẩn danh: toàn false / 0.
```

Bài học nháp (`is_published=false`) bị chặn ở `assertLessonVisible()` — ném **404 chứ không phải 403** để không tiết lộ sự tồn tại của bản nháp; admin được preview (F88). Áp dụng cho cả `GET /lessons/{id}` lẫn `GET /lessons/{id}/exercises`.

### 2.2. Chấm bài — `ExerciseService.gradeExercises()` + `normalizeAnswer()`

```java
boolean ungradeable = ex.getCorrectAnswer() == null || ex.getCorrectAnswer().isBlank();
if (ungradeable) { /* loại khỏi tử/mẫu, gắn cờ ungradeable=true */ continue; }
// So sánh "" với "" mà tính đúng thì oan → vì vậy bài không key KHÔNG được tính điểm
correct = normalizeAnswer(item.getUserAnswer()).equals(normalizeAnswer(ex.getCorrectAnswer()));

private String normalizeAnswer(String s) {
    if (s == null) return "";
    return s.trim().toLowerCase().replaceAll("\\s+", " ");
}
```

`POST /grade` chấm thử **không lưu**; `POST /submit` (cần login) chấm + lưu. `GET .../exercises?includeAnswers=true` đòi `ROLE_ADMIN`, user thường nhận 403 — chống lộ đáp án.

### 2.3. Nộp bài — `ExerciseService.submitExercises()` (dòng 256+)

```java
GradeResponse grade = gradeExercises(lessonId, request);   // chấm lại ở server, không tin client
// batch load exercises (tránh N+1) → dựng detailsJson từng câu:
"{\"exerciseId\":...,\"question\":...,\"userAnswer\":...,\"correctAnswer\":...,\"isCorrect\":...,\"explanation\":...}"
ExerciseAttempt attempt = ExerciseAttempt.builder()
        .user(user).lessonId(lessonId)
        .score(...).total(...).percentage(...)   // BigDecimal, HALF_UP
        .details(detailsJson).completedAt(LocalDateTime.now())
        .build();
attemptRepository.save(attempt);
```

Xem lại: `GET /attempts` (mới nhất trước) + `GET /attempts/{id}` (chi tiết từng câu, parse details JSON). Guard principal lạ → 401 chứ không NPE 500 (F54). Tổng quan: `ProgressService.getProgressSummary()` = `countByIsPublishedTrue` + đếm `Progress.isCompleted` + `totalPoints`.

### 2.4. Nộp writing/audio — `LessonSubmissionService.submitLessonSkill()`

Upsert theo bộ ba (user, lesson, skillType): có rồi thì ghi đè + reset `score/feedback` + status `PENDING` (chờ chấm). Upload audio qua `SafeUploadNames.extensionOf()` — allowlist `BINARY` (png/jpg/mp3/wav/webm/...) và `OPAQUE` (txt/srt/vtt/...); `.html/.svg/.js` bị từ chối vì route `/api/resources/**` là permitAll same-origin với SPA (stored-XSS đọc JWT — đã verify end-to-end bằng Chromium thật, F81). File lưu tên UUID trong `uploads/`.

**Trả lời 30 giây:** "Bài học và bài tập đều sắp xếp bằng orderIndex. Chấm ở server: chuẩn hoá chữ thường và khoảng trắng, bài không có đáp án thì loại khỏi điểm. Nút chấm thử không lưu, nút nộp bài lưu ExerciseAttempt để xem lại. Đáp án chỉ admin được lấy."

---

## 3. Cơ chế streak

**Thuộc 1 câu:** Mỗi user có `currentStreak + lastStudyDate` (ngày VN) + tập Redis `user:login_days:<id>` (TTL 90 ngày); `recordAccess` lũy đẳng trong ngày: lần đầu = 1, liền kề +1, nghỉ ≥ 2 ngày reset 1; hiển thị dùng `effectiveStreak` (gap > 1 → 0).

### 3.1. Thuật toán — `StreakService.recordAccess()` (nguyên văn, chỉ giữ logic)

```java
LocalDate today = LocalDate.now(clock);              // Clock múi VN, test được
LocalDate lastStudyDate = user.getLastStudyDate();
boolean firstActivityToday = lastStudyDate == null || !lastStudyDate.equals(today);
if (!firstActivityToday) {
    return; // đã tính hôm nay rồi — không đổi streak, không đụng Redis
}
if (lastStudyDate == null) {
    user.setCurrentStreak(1);                        // hoạt động đầu đời
} else if (ChronoUnit.DAYS.between(lastStudyDate, today) == 1) {
    user.setCurrentStreak(streakOrZero(user) + 1);    // học liên tục → +1
} else {
    user.setCurrentStreak(1);                        // bỏ ≥ 2 ngày → reset
}
user.setLastStudyDate(today);
userRepository.save(user);
recordLoginDateInRedis(userId, today);               // Set add + expire, fail-soft
```

### 3.2. Ba điểm kích hoạt (đã grep toàn source, chỉ 3 chỗ gọi)

| # | File | Dòng | Ngữ cảnh |
|---|---|---|---|
| 1 | `service/UserService.java` | 199 | `login()` — đăng nhập |
| 2 | `service/UserService.java` | 231 | `getProfile()` (`GET /api/auth/me`) — App mount là tính |
| 3 | `service/GameService.java` | 258 | nộp game → `streakService.checkin(...)` → `recordAccess()` |

⚠️ Nộp bài tập lesson (`submitExercises`) **không** gọi streak — streak tính theo “ngày mở app/chơi game”, không phải mỗi lần nộp bài. Đừng trả lời sai điểm này.

### 3.3. Đọc / hiển thị — `StreakService`

```java
private Integer effectiveStreak(User user, LocalDate today) {
    if (lastStudyDate == null || ChronoUnit.DAYS.between(lastStudyDate, today) > 1) return 0;
    return streakOrZero(user);
}
```

Số thô trong DB có thể “treo” (user bỏ học cả tuần mà `currentStreak` vẫn 5) — `effectiveStreak` mới là nguồn sự thật cho UI/mail. Từ 16/09 header Profile cũng dùng số hiệu lực (`Profile.vue` đọc ref từ `/api/streak/current`, không đọc `localStorage.user` nữa). `GET /api/streak/current` → `{currentStreak, today}` (ngày ISO từ **server** để Profile đóng khung đúng ngày, không tin đồng hồ máy khách). `GET /api/streak/history?days=30` → ngày ISO tăng dần; Redis chết → `[]`, chuỗi rác parse-fail bị lọc + warn log.

### 3.4. Nhắc mail — `StreakReminderScheduler` + `RedisConstants`

- Cron `0 0 20 * * *`, zone `Asia/Ho_Chi_Minh` (20:00 mỗi ngày) + cơ chế catch-up khi app boot muộn (marker Redis `streak:reminder:<ngày>`, TTL 2 ngày — chạy rồi thì skip).
- `getUsersWithStreakAtRisk()`: active + học hôm qua, chưa học hôm nay → mail “cứu streak”.
- `getUsersWithBrokenStreak()`: active + nghỉ ≥ 2 ngày (không gồm người chưa từng học) → mail mời quay lại, chống spam bằng `streak:comeback:` 30 ngày.

**Trả lời 30 giây:** "Streak lưu 2 chỗ: số chuỗi và ngày học cuối trong DB, tập ngày trong Redis để vẽ lịch. Mỗi ngày tính một lần, học liên tục cộng một, nghỉ hai ngày gãy về không và học lại từ một. Tối 8 giờ có mail nhắc nhóm sắp gãy."

---

## 4. Tìm kiếm / Sắp xếp

**Thuộc 1 câu:** Lessons tìm bằng `q + level + page/size`, sort cố định `orderIndex`; Vocabulary 3 tầng (list cần login + search public LIKE + proxy từ điển có cache); Admin exercises lọc `lessonId/type/difficulty/q` đẩy hết xuống SQL.

### 4.1. Lessons — `GET /api/lessons?q=&level=&page=0&size=12`

`q` trim rồi LIKE tiêu đề/mô tả qua `findPublishedPageProjection` (chỉ bài published). Sort server fix cứng — không cho client truyền sort. Frontend `views/Lessons.vue`: ô search + nút chọn level + phân trang, gọi `lessonService.getPage()`.

### 4.2. Vocabulary — `controller/VocabularyController.java`

```java
@GetMapping                                   // CẦN LOGIN, Pageable mặc định size=20, sort=word
public ResponseEntity<Page<Vocabulary>> list(@PageableDefault(size = 20, sort = "word") Pageable pageable)

@GetMapping("/search")                        // PUBLIC
public ResponseEntity<List<Vocabulary>> search(@RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(defaultValue = "") String q) {
    String query = keyword.isBlank() ? q : keyword;   // keyword ưu tiên
    if (query.isBlank() || query.length() < 2) return ResponseEntity.ok(List.of()); // < 2 ký tự → rỗng
    return ResponseEntity.ok(vocabularyRepository.findByWordContainingIgnoreCase(query)); // LIKE %kw%
}

@GetMapping("/dictionary/{word}")              // PUBLIC: proxy dictionaryapi.dev
    String clean = word.replaceAll("[^a-zA-Z'-]", "").toLowerCase(); // lọc ký tự lạ
```

⚠️ Đính chính quan trọng: thứ tự fallback trong `vocabularyService.search()` là **proxy backend trước** (comment trong code: “Proxy backend là đường chính: Redis cache 1h dùng chung mọi user… Tiết kiệm ~8s chờ vô ích cho mỗi từ mới”), browser gọi thẳng `dictionaryapi.dev` (timeout 4s × 2 lần) chỉ khi proxy lỗi, cuối cùng mới tới DB Oxford3000 qua `/search`. Đừng nói ngược.

`DictionaryService.lookup()` tách riêng class để `@Cacheable("dictionary")` đi qua Spring proxy (self-invocation không kích hoạt cache); upstream từ mạng VN đo thực tế ~20s khi cache lạnh; 404 → `"[]"`; lỗi khác → warn log + `"[]"` (fail-soft, app không vỡ). `POST /api/vocabulary` cần login; `PUT/DELETE` cần ADMIN.

### 4.3. Admin exercises — `GET /api/admin/exercises?lessonId=&type=&difficulty=&q=` (ADMIN)

```java
Page<Exercise> page = exerciseRepository.findAdminPage(lessonId, exerciseType, exerciseDifficulty,
        search != null && !search.isBlank() ? search.trim() : null, pageable);
```

Filter đẩy hết xuống SQL (không load 43.7k rows lên memory — đường `findAll()` cũ 308ms đã bị xoá, C-03a). `q` LIKE leading-wildcard đo ~185ms: không thêm index vì vô ích với `%kw%`. `toAdminRow()` đọc tiêu đề lesson bằng 1 query batch (`findTitlesById`) thay vì hydrate entity `Lesson` — từng tốn 95k logical reads/trang vì kéo theo 2 cột NVARCHAR(MAX).

### Sắp xếp ở đâu (thuộc lòng)

- Server: lessons → `orderIndex`; vocab list → `word`; attempts → mới nhất trước; streak-history → tăng dần.
- Client không tự sort lại danh sách phân trang (chỉ sort mảng nhỏ đã load như meanings).

**Trả lời 30 giây:** "Tìm bài học theo từ khoá và trình độ, phân trang, thứ tự cố định theo lộ trình. Tra từ đi 3 tầng: proxy backend có cache trước, gọi thẳng từ điển nếu proxy lỗi, cuối cùng là kho từ local. Từ dưới 2 ký tự không tìm để đỡ nặng DB."

---

## Phụ lục thực hành — Chỉ dẫn demo từng bước

### A. Chuẩn bị môi trường (chạy trước giờ demo 10 phút)

```powershell
docker ps --format "{{.Names}} {{.Status}}"   # cần: engflow-backend (Up), engflow-sqlserver, engflow-minio
# Backend: http://localhost:8080 | Frontend: http://localhost:5173
Invoke-RestMethod http://localhost:8080/api/lessons?size=1 | ConvertTo-Json -Depth 3  # public, phải ra 200
```

Nếu backend vừa đổi code: `docker compose up -d --build backend` (code trong container chỉ đổi khi rebuild).

### B. Kiểm tra nhanh 4 chức năng bằng API (PowerShell)

```powershell
$base = 'http://localhost:8080'
# 1. Login → lấy token (token nằm ở .token hoặc .data.token)
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
  -ContentType 'application/json' -Body '{"email":"user@gmail.com","password":"123456"}'
$token = $login.token; if (-not $token) { $token = $login.data.token }
$H = @{ Authorization = "Bearer $token" }
# 2. Tìm + sắp xếp lessons
Invoke-RestMethod "$base/api/lessons?q=hello&size=3"
# 3. Streak hiện tại + lịch 7 ngày
Invoke-RestMethod -Headers $H "$base/api/streak/current"
Invoke-RestMethod -Headers $H "$base/api/streak/history?days=7"
# 4. Tra từ (public, không cần token)
Invoke-RestMethod "$base/api/vocabulary/search?keyword=hello"
Invoke-RestMethod "$base/api/vocabulary/dictionary/hello"
```

⚠️ PowerShell + curl JSON dễ vỡ quoting — dùng `Invoke-RestMethod` như trên, đừng `curl -d '...'` (ghi trong AGENTS.md).

### C. Kịch bản demo 5 phút trên UI

1. **(1') Đăng nhập:** mở DevTools → Application → Local Storage. Login `user@gmail.com` / `123456`, chỉ cho giám khảo thấy 2 key `token` + `user` xuất hiện. Mở `/admin/users` bằng tài khoản user → bị đá về `/` (guard `requiresAdmin`).
2. **(1') Bài học:** `/lessons` → gõ ô search + đổi level → list đổi + phân trang. Mở 1 bài → tab bài tập.
3. **(1.5') Bài tập:** bấm chấm thử → điểm hiện nhưng F5 + mở lịch sử thì **chưa có** (chứng minh `grade` không lưu). Bấm nộp bài → mở lịch sử attempts → thấy đúng lần nộp vừa rồi kèm chi tiết từng câu (chứng minh `submit` lưu `detailsJson`).
4. **(1') Tra từ:** `/search` gõ `hello` → phiên âm/audio/nghĩa/hyphen; gõ 1 ký tự → rỗng (chứng minh guard `< 2 ký tự`).
5. **(0.5') Streak:** `/profile` → số streak + lịch 30 ngày (số header = số lịch, đã thống nhất 16/09); nói: “tối 8 giờ hệ thống gửi mail cho nhóm sắp gãy”.

### D. Lỗi hay gặp khi demo (thuộc để đỡ bị hỏi gài)

| Câu hỏi / sự cố | Trả lời |
|---|---|
| JWT hết hạn giữa demo? | 15 phút; `api.js` tự bắt `exp` và đá về `/login`. Login lại là xong. |
| Sao sort lessons không đổi được? | Cố tình fix `orderIndex` để giữ lộ trình + query có index. |
| Bài không đáp án chấm sao? | Loại khỏi tử/mẫu, gắn `ungradeable=true`, không cho đúng oan. |
| Đổi giờ máy có gian lận streak? | Ngày lấy từ clock server (múi VN), không tin client. |
| Mất mạng tới dictionaryapi.dev? | Proxy fail-soft `[]` + fallback DB local, app không vỡ; cache Redis 1h. |
| Mic không chạy khi demo speaking? | Chuẩn bị quyền mic trước; mic giả headless trả im lặng → Whisper text rỗng → status FAILED là đúng thiết kế. |
| Sweep UI làm bẩn `payment_transactions`? | Mount `/premium/checkout` sinh row thật — dọn bằng `DELETE ... WHERE created_at >= '<ngày chạy>'` + `SET QUOTED_IDENTIFIER ON` đầu batch. |

## Bảng endpoint thuộc lòng

| Chức năng | Method + path |
|---|---|
| Register / Login / Me | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Lessons | `GET /api/lessons?q=&level=&page=&size=`, `GET /api/lessons/{id}` |
| Exercises | `GET /api/lessons/{id}/exercises`, `POST /grade`, `POST /submit`, `GET /attempts`, `GET /attempts/{id}` |
| Streak | `GET /api/streak/current`, `GET /api/streak/history?days=30` |
| Vocab | `GET /api/vocabulary`, `GET /api/vocabulary/search?keyword=`, `GET /api/vocabulary/dictionary/{word}` |
| Admin | `GET /api/admin/exercises?lessonId=&type=&difficulty=&q=` |
