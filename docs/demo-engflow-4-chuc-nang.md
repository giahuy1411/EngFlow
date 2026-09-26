# EngFlow — Cẩm nang demo đồ án tốt nghiệp (4 chức năng)

> **Ai đọc cũng hiểu.** Tài liệu này viết cho **cả người không biết lập trình**: mỗi chức năng được giải thích
> bằng lời thường trước, rồi mới tới phần kỹ thuật (có **file + đoạn code thật** để đối chiếu khi bị hỏi sâu).
>
> **Đã kiểm chứng lại toàn bộ ngày 26/09/2026** bằng cách đọc trực tiếp source và chạy thử API/UI — không chép
> lại mô tả cũ. Tài khoản demo: `user@gmail.com` / `admin@gmail.com`, mật khẩu `123456`.

**Cách dùng tài liệu:** khi demo, chỉ cần thuộc mục **"Kịch bản bấm"** và **"Trả lời 30 giây"**. Phần
**"Kỹ thuật"** chỉ mở ra khi hội đồng hỏi sâu.

---

## 0. Chuẩn bị trước khi demo (làm trước 10 phút)

| Việc | Lệnh / cách làm |
|---|---|
| Bật hệ thống | Mở Docker Desktop, chờ tới khi 8 dịch vụ "Up": `docker ps` |
| Kiểm tra web sống | Mở `http://localhost:5173` — thấy trang chủ EngFlow |
| Kiểm tra máy chủ sống | `Invoke-RestMethod "http://localhost:8080/api/lessons?size=1"` → phải trả dữ liệu |
| Tài khoản | Học viên `user@gmail.com`, quản trị `admin@gmail.com` — mật khẩu `123456` |

> Nếu vừa sửa code backend: chạy `docker compose up -d --build backend` (code trong hộp Docker chỉ đổi khi dựng lại).

---

## 1. Đăng nhập / Đăng ký

### 1.1. Nó là gì (lời thường)

Giống như **làm thẻ ra vào một toà nhà**:
- **Đăng ký** = làm thẻ mới. Hệ thống kiểm tra email/tên đăng nhập chưa ai dùng, rồi cất mật khẩu đã **mã hoá**
  (không ai đọc được, kể cả người quản trị cơ sở dữ liệu).
- **Đăng nhập** = quẹt thẻ. Nếu đúng, hệ thống phát một **vé điện tử (JWT)** có hạn **15 phút** để bạn đi lại
  trong toà nhà mà không phải quẹt lại mỗi bước.
- **Chống dò mã** = gõ sai 5 lần thì cửa **tạm khoá 15 phút**.

### 1.2. Kịch bản bấm (trên UI)

1. Mở `/register` → nhập email, tên đăng nhập, mật khẩu → bấm **Đăng ký**. Hệ thống **tự đăng nhập luôn**.
2. Mở `/login` → nhập `user@gmail.com` / `123456` → bấm **Đăng nhập**.
3. Mở DevTools (F12) → tab **Application → Local Storage** → chỉ cho hội đồng thấy 2 mục `token` và `user` vừa xuất hiện.
4. (Điểm nhấn guard) Đang đăng nhập bằng tài khoản học viên, gõ thẳng `/admin/users` → **bị đá về trang chủ**
   (vì đây là khu vực quản trị).
5. (Điểm nhấn bảo mật) Đăng xuất, thử đăng nhập sai mật khẩu **5 lần** → **ngay lần thứ 5** báo **"tài khoản tạm khoá … phút"**.

### 1.3. Trả lời 30 giây

> "Đăng ký chặn trùng email và tên đăng nhập, mật khẩu mã hoá bằng BCrypt nên không đọc được. Đăng nhập sai 5 lần
> thì khoá 15 phút (lưu trong Redis). Khi đăng nhập thành công, hệ thống phát token JWT hết hạn sau 15 phút;
> giao diện lưu token đó và mỗi trang được bảo vệ bằng 4 lớp kiểm tra quyền."

### 1.4. Kỹ thuật — file + đoạn code thật

**a) Đăng ký chặn trùng + mã hoá mật khẩu** — `src/main/java/com/datn/engflow/service/UserService.java:141`

```java
public UserResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new ConflictException("Email đã tồn tại");          // → HTTP 409
    }
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new ConflictException("Tên đăng nhập đã tồn tại");  // → HTTP 409
    }
    User user = User.builder()
            .passwordHash(passwordEncoder.encode(request.getPassword())) // BCrypt
            .currentLevel(LessonLevel.ELEMENTARY)
            .build();
    User savedUser = userRepository.save(user);
    String jwt = tokenProvider.generateToken(savedUser.getEmail(),
            Boolean.TRUE.equals(savedUser.getIsAdmin()) ? "ADMIN" : "USER", savedUser.getIsPremium());
    return mapToUserResponse(savedUser, jwt);                      // tự đăng nhập luôn
}
```

**b) Đăng nhập có khoá tạm 5 lần sai** — `UserService.java:165`

```java
String normalizedEmail = request.getEmail().trim().toLowerCase();
String lockKey = RedisConstants.LOGIN_LOCK_PREFIX + normalizedEmail;
try {
    String locked = redisTemplate.opsForValue().get(lockKey);
    if (locked != null) {
        long ttl = redisTemplate.getExpire(lockKey);
        throw new BadRequestException("Tài khoản tạm khóa do đăng nhập sai nhiều lần. Thử lại sau "
                + Math.max(1, (ttl + 59) / 60) + " phút.");
    }
} catch (BadRequestException e) { throw e; }
  catch (Exception e) { log.warn("Redis unavailable ... fail-open"); }   // Redis chết → login vẫn chạy
```

**c) Vé JWT (15 phút, có vai trò)** — `src/main/java/com/datn/engflow/security/JwtTokenProvider.java:31`

```java
return Jwts.builder()
        .subject(email)                       // danh tính
        .claim("role", role)                  // "ADMIN" | "USER"
        .claim("isPremium", isPremium != null && isPremium)
        .expiration(new Date(now.getTime() + jwtExpirationInMs))  // jwt.expiration = 900000 ms = 15 phút
        .signWith(getSigningKey())            // ký HS256
        .compact();
```

**d) Phân quyền 3 lớp** — `src/main/java/com/datn/engflow/config/SecurityConfig.java:76`

```java
.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
.requestMatchers(HttpMethod.GET, "/api/lessons/*/exercises/attempts/**").authenticated()  // ← phải đặt TRƯỚC…
.requestMatchers(HttpMethod.GET, "/api/lessons/**").permitAll()                          // ← …rule rộng này
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

> ⚠️ **Bẫy thật đã từng sập (F54):** Spring chọn **rule khớp đầu tiên**. Rule hẹp "lịch sử làm bài cần đăng nhập"
> **phải đứng trước** rule rộng "bài học ai cũng xem được", nếu không lịch sử làm bài bị lộ ra công khai.

**e) Chuyển trang an toàn sau khi login** — `frontend/src/views/Login.vue:71`

```js
await auth.login({ email: email.value, password: password.value, remember: remember.value })
// audit-v13 F-13-20: quay lại đúng trang bị chặn trước đó, nhưng safeRedirect() từ chối mọi URL ngoài hệ thống
router.replace(safeRedirect(route.query.redirect))   // ?redirect=//evil.com KHÔNG thể lừa được
```

**Bảng endpoint thuộc lòng:** `POST /api/auth/register` · `POST /api/auth/login` · `GET /api/auth/me` ·
`POST /api/auth/forgot-password` · `POST /api/auth/reset-password` · `POST /api/auth/change-password` ·
`PUT /api/auth/avatar` · `POST /api/auth/avatar/upload`.

---

## 2. Bài học / Bài tập

### 2.1. Nó là gì (lời thường)

Giống như một **cuốn giáo trình có bài tập kèm theo**:
- **Bài học** = một chương (đọc lý thuyết). Nội dung bài học lấy từ nguồn có sẵn, hiển thị ở tab **"Nội dung"**.
- **Bài tập** = phiếu câu hỏi của chương đó, ở tab **"Bài tập"**. Hai phần này **tách hẳn** nhau.
- Khi làm bài, có 2 nút khác nhau:
  - **Chấm thử** = làm nháp, xem điểm ngay nhưng **không lưu**.
  - **Nộp bài** = chấm **và lưu vào sổ lịch sử**, để xem lại sau.
- **Chấm điểm do máy chủ làm**, không phải trình duyệt — nên không thể gian lận bằng cách sửa code trên máy khách.
  Đáp án chỉ **quản trị viên** mới xem được.

### 2.2. Kịch bản bấm (trên UI)

1. Mở `/lessons` → thấy danh sách chương, có ô tìm kiếm + nút chọn trình độ + phân trang.
2. Mở một bài (ví dụ `/lessons/445`) → thấy 3 tab: **Nội dung** · **Bài tập** · **Lịch sử**.
3. Tab **Nội dung**: chỉ có nội dung bài học (đã tách khỏi bài tập).
4. Tab **Bài tập**: làm vài câu → bấm **Chấm thử** → hiện đúng/sai ngay.
5. **Điểm nhấn:** bấm F5 tải lại → mở tab **Lịch sử** → **không thấy** lần chấm thử vừa rồi (chứng minh "chấm thử không lưu").
6. Bấm **Nộp bài** → mở tab **Lịch sử** → **thấy đúng lần nộp** kèm chi tiết từng câu.
7. (Điểm nhấn chống lộ đáp án) Mở DevTools → Network → xem response của `/exercises` khi đăng nhập bằng học viên
   → **không có** trường `correctAnswer`.

### 2.3. Trả lời 30 giây

> "Bài học và bài tập đều sắp theo thứ tự trong lộ trình. Máy chủ chấm điểm: chuẩn hoá chữ thường và khoảng trắng,
> bài nào thiếu đáp án thì bị loại khỏi điểm thay vì chấm oan. Nút 'chấm thử' không lưu, nút 'nộp bài' lưu lại
> lịch sử. Đáp án chỉ quản trị viên lấy được."

### 2.4. Kỹ thuật — file + đoạn code thật

**a) Chấm điểm + loại bài thiếu đáp án** — `src/main/java/com/datn/engflow/service/ExerciseService.java:232`

```java
// Bài thiếu đáp án: so "" với "" sẽ thành ĐÚNG (oan) → loại khỏi tử/mẫu, gắn cờ ungradeable
boolean ungradeable = (ex.getCorrectAnswer() == null || ex.getCorrectAnswer().isBlank())
        || isMatchingUngradeable(ex);
if (ungradeable) {
    results.add(ExerciseGradeItem.builder().exerciseId(ex.getId())
            .correct(false).ungradeable(true).build());
    continue;                                  // không tính vào score/total
}
correct = isCorrectAnswer(ex, item.getUserAnswer());
if (correct) score++;
total++;
```

**b) Chấm thử (không lưu) vs Nộp bài (lưu)** — cùng file, `gradeExercises()` (dòng 232) và `submitExercises()` (dòng 473).
`submitExercises` gọi lại `gradeExercises` để **chấm lại ở máy chủ** (không tin số điểm do trình duyệt gửi lên),
rồi lưu một bản ghi `ExerciseAttempt` kèm chi tiết từng câu.

**c) Tìm kiếm + chọn trình độ (có chờ gõ xong mới gọi)** — `frontend/src/views/Lessons.vue:180`

```js
const searchQuery = ref('')
if (selectedLevel.value !== 'ALL') params.level = selectedLevel.value
if (searchQuery.value.trim()) params.q = searchQuery.value.trim()
// gõ xong ~0.3s mới gọi API (debounce) — tránh bắn 1 request mỗi ký tự
let searchTimer = null
watch(searchQuery, () => { clearTimeout(searchTimer); searchTimer = setTimeout(/* gọi lại danh sách */) })
```

**d) Tab bài tập gọi API chấm** — `frontend/src/views/lessons/LessonExerciseTab.vue:295`

```js
const res = await lessonService.gradeExercises(lessonId, [ /* câu trả lời người dùng */ ])
// correctAnswer CHỈ xuất hiện sau khi chấm (với học viên), không có sẵn trong dữ liệu tải về
```

**Bảng endpoint thuộc lòng:** `GET /api/lessons?q=&level=&page=&size=` · `GET /api/lessons/{id}` ·
`GET /api/lessons/{id}/exercises` · `POST /api/lessons/{id}/exercises/grade` · `POST /api/lessons/{id}/exercises/submit` ·
`GET /api/lessons/{id}/exercises/attempts`.

---

## 3. Cơ chế Streak (chuỗi ngày học)

### 3.1. Nó là gì (lời thường)

Giống như **chuỗi ngày đi tập gym**:
- Học mỗi ngày → chuỗi tăng 1.
- Nghỉ 2 ngày liền → chuỗi **gãy về 0**, học lại tính từ 1.
- **Điểm mấu chốt:** "ngày hôm nay" do **máy chủ** quyết định (theo giờ Việt Nam), **không** theo đồng hồ máy bạn
  → không thể gian lận bằng cách đổi giờ máy.
- Mỗi tối 8 giờ, hệ thống **gửi email nhắc** cho nhóm sắp gãy chuỗi.

### 3.2. Kịch bản bấm (trên UI)

1. Đăng nhập → mở `/profile`.
2. Xem ô **Streak** (số chuỗi hiện tại) và **lịch học 30 ngày** bên dưới.
3. Nói với hội đồng: "số ở ô Streak và số trên lịch luôn khớp nhau — cả hai đọc từ cùng một nguồn".
4. (Nếu muốn minh hoạ) Làm 1 bài tập hoặc chơi 1 lượt game → tải lại `/profile` → ngày hôm nay được đánh dấu.

### 3.3. Trả lời 30 giây

> "Chuỗi ngày học được lưu trong một bảng điểm danh, mỗi ngày học ghi một dòng. Máy chủ đếm ngược từ hôm nay:
> liên tục thì cộng, nghỉ hai ngày thì chuỗi về 0. 'Hôm nay' lấy theo giờ máy chủ nên không gian lận được bằng
> cách đổi giờ máy. Tối 8 giờ có email nhắc nhóm sắp gãy chuỗi."

### 3.4. Kỹ thuật — file + đoạn code thật

**a) Ba endpoint** — `src/main/java/com/datn/engflow/controller/StreakController.java`

```java
@GetMapping("/snapshot")   // ảnh chụp đầy đủ cho lịch 30 ngày
    return ResponseEntity.ok(studyActivityService.snapshot(userPrincipal.getId(), 30));
@GetMapping("/history")    // danh sách ngày đã học (ISO)
    return ResponseEntity.ok(streakService.getLoginDays(userPrincipal.getId(), days));
@GetMapping("/current")    // { currentStreak, today } — "today" do MÁY CHỦ trả, không tin đồng hồ máy khách
    var snapshot = studyActivityService.snapshot(userPrincipal.getId(), 30);
    return ResponseEntity.ok(Map.of("currentStreak", snapshot.currentStreak(), "today", snapshot.today()));
```

**b) Nguồn sự thật = bảng `study_days` (không phải cột cũ)** — `StreakService.java` ghi chú rõ:
`nguồn sự thật là bảng SQL study_days`, và `StudyActivityService` mới là nơi tính.

**c) Thuật toán đếm chuỗi** — `src/main/java/com/datn/engflow/service/StudyActivityService.java:200`

```java
private int currentStreak(List<LocalDate> dates, LocalDate today) {
    var uniqueDates = new java.util.HashSet<>(dates);
    LocalDate cursor = uniqueDates.contains(today) ? today : today.minusDays(1);
    int streak = 0;
    while (uniqueDates.contains(cursor)) {   // đếm ngược từng ngày liền nhau
        streak++;
        cursor = cursor.minusDays(1);
    }
    return streak;
}
```

**d) "Hôm nay" theo giờ Việt Nam** — cùng file, `STUDY_ZONE` dòng 34, `today()` dòng 138

```java
private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
public LocalDate today() { return LocalDate.now(clock.withZone(STUDY_ZONE)); }  // clock inject được → test được
```

**e) Chống N+1 khi hiển thị nhiều người (leaderboard)** — cùng file, dòng 122

```java
/** Streak của nhiều user trong MỘT query, dùng cho các trang danh sách.
 *  Gọi currentStreak theo từng row là N+1: trang 20 dòng tốn 21 query. */
public Map<Long, Integer> currentStreaks(Collection<Long> userIds) { /* 1 query rồi tính trong bộ nhớ */ }
```

**f) Giao diện Profile đọc snapshot** — `frontend/src/views/Profile.vue:84`

```js
const snapshot = await streakService.getSnapshot()
// kiểm tra hợp lệ trước khi dùng (studiedDays là mảng, currentStreak là số nguyên, today có giá trị)
if (!Array.isArray(snapshot.studiedDays) || !Number.isInteger(snapshot.currentStreak)) throw new Error('Invalid study snapshot')
```

> ⚠️ **Điểm dễ trả lời sai:** streak tính theo **"ngày có hoạt động học"**, ghi vào bảng `study_days`. Có
> **5 đường** ghi ngày học (đều gọi `StudyActivityService.recordStudy`): **nộp bài tập lesson**
> (`ExerciseService.submitExercises` — có ít nhất 1 câu trả lời khác rỗng), **ôn flashcard**
> (`FlashcardService`), **ôn SRS** (`SrsService`), **nộp game** (`StreakService.checkin`), **nộp speaking**
> (`SpeakingSubmissionService`). Riêng **đăng nhập** (`UserService.login`) **chỉ ĐỌC** streak, **không** ghi.
> Đáp án đúng: **nộp bài tập CÓ tính streak** — nói "không tính" là SAI.

---

## 4. Tìm kiếm / Sắp xếp

### 4.1. Nó là gì (lời thường)

Giống như **mục lục của thư viện**:
- **Tìm bài học** = gõ từ khoá + chọn trình độ; thứ tự luôn theo **lộ trình học** (không đổi lung tung).
- **Tra từ vựng** = hệ thống thử **3 tầng** theo thứ tự:
  1. **Kho đệm của máy chủ** (nhanh nhất, nhớ sẵn 1 giờ).
  2. **Từ điển online** (khi kho đệm chưa có).
  3. **Kho từ vựng local** (khi mạng lỗi) — nên app **không bao giờ vỡ** vì mất mạng.
- Gõ **dưới 2 ký tự thì không tìm** (để đỡ nặng máy chủ).

### 4.2. Kịch bản bấm (trên UI)

1. Mở `/lessons` → gõ vào ô tìm kiếm (ví dụ `present`) → danh sách lọc lại; đổi nút trình độ → lọc tiếp; bấm phân trang.
2. Mở `/search` (tra từ) → gõ `hello` → hiện phiên âm, nghĩa, ví dụ.
3. Gõ **1 ký tự** (ví dụ `h`) → **không ra gì** (chứng minh guard "dưới 2 ký tự").
4. (Nếu muốn) Vào `/admin/exercises` bằng tài khoản admin → lọc theo bài học / loại / độ khó / từ khoá.

### 4.3. Trả lời 30 giây

> "Tìm bài học theo từ khoá và trình độ, thứ tự cố định theo lộ trình. Tra từ đi 3 tầng: kho đệm máy chủ trước,
> rồi từ điển online, cuối cùng là kho local — nên mất mạng vẫn tra được. Từ dưới 2 ký tự không tìm để đỡ nặng."

### 4.4. Kỹ thuật — file + đoạn code thật

**a) Tra từ 3 tầng + guard 2 ký tự** — `src/main/java/com/datn/engflow/controller/VocabularyController.java`

```java
@GetMapping                                   // CẦN ĐĂNG NHẬP, mặc định 20 dòng, sắp theo "word"
public ResponseEntity<Page<Vocabulary>> list(@PageableDefault(size = 20, sort = "word") Pageable pageable)

@GetMapping("/search")                        // CÔNG KHAI
public ResponseEntity<List<Vocabulary>> search(@RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(defaultValue = "") String q) {
    String query = keyword.isBlank() ? q : keyword;   // "keyword" ưu tiên
    if (query.isBlank() || query.length() < 2) return ResponseEntity.ok(List.of()); // < 2 ký tự → rỗng
    return ResponseEntity.ok(vocabularyRepository.findByWordContainingIgnoreCase(query)); // LIKE %kw%
}
```

> **Thứ tự fallback (đừng nói ngược):** `vocabularyService.search()` thử **proxy máy chủ trước** (có cache Redis 1 giờ),
> rồi mới tới từ điển online, cuối cùng là DB Oxford3000. `DictionaryService` tách riêng để cache hoạt động đúng.

**b) Tìm + sắp xếp bài học (server quyết định thứ tự)** — `LessonService` cố định `Sort.by("orderIndex")`;
`Lessons.vue` chỉ gửi `q` + `level` + trang.

**c) Admin lọc bài tập (đẩy hết xuống SQL)** — `ExerciseService`, endpoint `GET /api/admin/exercises?lessonId=&type=&difficulty=&q=`

```java
Page<Exercise> page = exerciseRepository.findAdminPage(lessonId, exerciseType, exerciseDifficulty,
        search != null && !search.isBlank() ? search.trim() : null, pageable);
// KHÔNG load 43.7k dòng lên bộ nhớ; tìm %kw% đo ~185ms → KHÔNG thêm index (vô ích với leading wildcard)
```

**d) Sắp xếp ở đâu (thuộc lòng):**
- Máy chủ: bài học → `orderIndex`; danh sách từ → `word`; lịch sử làm bài → mới nhất trước; lịch streak → tăng dần.
- Trình duyệt **không** tự sắp lại danh sách đã phân trang (chỉ sắp mảng nhỏ như danh sách nghĩa của một từ).

**Bảng endpoint thuộc lòng:** `GET /api/lessons?q=&level=&page=&size=` ·
`GET /api/vocabulary` (cần login) · `GET /api/vocabulary/search?keyword=` (công khai) ·
`GET /api/vocabulary/dictionary/{word}` (công khai) · `GET /api/admin/exercises?lessonId=&type=&difficulty=&q=`.

---

## Phụ lục A — Kịch bản demo 5 phút (thứ tự đề xuất)

| Phút | Việc làm | Câu nói kèm |
|---|---|---|
| 1' | Login `user@gmail.com`, mở DevTools chỉ 2 key `token`+`user`; thử `/admin/users` → bị đá về `/` | "Đăng nhập phát vé 15 phút, khu quản trị chặn học viên." |
| 1' | `/lessons` gõ tìm kiếm + đổi trình độ + phân trang; mở 1 bài | "Tìm theo từ khoá và trình độ, thứ tự theo lộ trình." |
| 1.5' | Tab **Bài tập** → **Chấm thử** → F5 → **Lịch sử** (trống) → **Nộp bài** → **Lịch sử** (có) | "Chấm thử không lưu, nộp bài mới lưu. Máy chủ chấm." |
| 1' | `/search` gõ `hello` → có kết quả; gõ `h` → trống | "Tra từ 3 tầng, dưới 2 ký tự không tìm." |
| 0.5' | `/profile` → ô Streak + lịch 30 ngày | "Chuỗi ngày học, tối 8 giờ gửi mail nhắc." |

---

## Phụ lục B — Câu hỏi hội đồng hay gài (thuộc để trả lời)

| Câu hỏi | Trả lời |
|---|---|
| Vé (JWT) hết hạn giữa demo thì sao? | 15 phút; giao diện tự phát hiện hết hạn và đưa về trang đăng nhập, đăng nhập lại là xong. |
| Sao không cho sắp xếp bài học tự do? | Cố định theo lộ trình (`orderIndex`) để giữ đúng thứ tự học. |
| Bài tập không có đáp án thì chấm thế nào? | Loại khỏi điểm (không cho "đúng oan"), gắn nhãn "không chấm được". |
| Đổi giờ máy có gian lận được streak không? | Không — "hôm nay" do máy chủ tính theo giờ Việt Nam. |
| Mất mạng tới từ điển online thì sao? | Hệ thống tự chuyển sang kho từ local, app không vỡ. |
| Vì sao gõ 1 ký tự không ra kết quả? | Cố ý — dưới 2 ký tự sẽ quét quá nhiều, nên chặn để đỡ nặng. |
| Tài khoản/mật khẩu demo? | `user@gmail.com` và `admin@gmail.com`, mật khẩu `123456`. |
