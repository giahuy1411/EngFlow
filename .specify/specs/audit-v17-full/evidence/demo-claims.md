# audit-v17-full — TD.1/TD.3: claim ledger 4 chức năng demo (API↔UI)

Mọi khẳng định trong `docs/demo-engflow-4-chuc-nang.md` được rút ra và kiểm chứng. Cột **Verdict**:
`CONFIRMED` (đúng) · `REFUTED` (sai → sửa) · `IMPRECISE` (đúng ý nhưng câu chữ sai → sửa doc) · `OPEN`.

## §1 Đăng nhập / Đăng ký

| Claim (doc) | Nguồn | Verdict | Bằng chứng |
|---|---|---|---|
| Đăng ký chặn trùng email/tên → 409 | `UserService.java:141-147` | CONFIRMED | `UserService:142` `ConflictException`; `api-sweep` auth 13/0 |
| Mật khẩu mã hoá BCrypt | `SecurityConfig:46-47` | CONFIRMED | `new BCryptPasswordEncoder()` |
| Sai 5 lần → khoá 15 phút | `UserService:208`, `RedisConstants:45-46` | CONFIRMED | `fails >= MAX_LOGIN_FAILS(5)`; lock TTL 15 phút |
| **Khoá ở lần thứ 5** (không phải 6) | `UserService:208` | CONFIRMED | (đã sửa ở v16 F-16-09; xác nhận lại) |
| JWT TTL 15 phút | `JwtTokenProvider` `jwt.expiration=900000` | CONFIRMED | `application.properties` |
| Đăng nhập → localStorage `token`+`user` | `main.js`, `auth.js` | CONFIRMED | MCP: đúng 2 key |
| Student `/admin/users` → đá về `/` | `router/index.js` meta | CONFIRMED | MCP: url → `/` |
| Phân quyền rule hẹp TRƯỚC rule rộng (F54) | `SecurityConfig:83-84` | CONFIRMED | `attempts/**` ở `:83` trước `/api/lessons/**` ở `:84` |
| Safe redirect chặn `//evil.com` | `Login.vue:71` | CONFIRMED | `safeRedirect` + test |

## §2 Bài học / Bài tập

| Claim | Nguồn | Verdict | Bằng chứng |
|---|---|---|---|
| 3 tab Nội dung/Bài tập/Lịch sử | `LessonLayout.vue:71-73` | CONFIRMED | MCP snapshot đúng 3 tab |
| Nút **"Chấm thử"** | doc §2.2 | **REFUTED** | UI là **"Kiểm tra"**; `grep "Chấm thử"` = 0 → **F-17-03** (sửa doc) |
| Chấm thử không lưu | `gradeExercises:232` | CONFIRMED | F5 → Lịch sử trống |
| Nộp bài lưu | `submitExercises:473` | CONFIRMED | Lịch sử "1/6 câu đúng" |
| Máy chủ chấm lại | `submitExercises` gọi `gradeExercises` | CONFIRMED | source `:473` |
| Bài thiếu đáp án → `ungradeable` | `:263-270` | CONFIRMED | UI "không tính điểm" |
| **"không có trường `correctAnswer`"** | doc §2.2 b7 | **IMPRECISE** | có key nhưng **null** → **F-17-04** (sửa doc) |
| Nộp bài tính streak | `ExerciseService:520` | CONFIRMED | DB +1 (dọn 40082) |

## §3 Streak

| Claim | Nguồn | Verdict | Bằng chứng |
|---|---|---|---|
| "Hôm nay" theo máy chủ VN | `StudyActivityService:34,138` | CONFIRMED | `/api/streak/current` today=2026-09-26 |
| Ô Streak = lịch (cùng nguồn) | `Profile.vue:84` | CONFIRMED | snapshot studiedDays = DB |
| Đếm liền nhau, gãy → 0 | `currentStreak:200` | CONFIRMED | source |
| 5 đường ghi `study_days` | 5 file (Exercise/Flashcard/Srs/Speaking/Streak) | CONFIRMED | `grep recordStudy` = 5 writer |
| **Login KHÔNG ghi** | `UserService.login` | CONFIRMED | (v16 F-16-01 sửa attribution) |
| Tối 8h gửi mail nhắc | `StreakReminderScheduler:56` | CONFIRMED | cron `0 0 20 * * * zone=Asia/Ho_Chi_Minh` |
| Chống N+1 leaderboard | `currentStreaks:122` | CONFIRMED | source |

## §4 Tìm kiếm / Sắp xếp

| Claim | Nguồn | Verdict | Bằng chứng |
|---|---|---|---|
| Tìm bài học + trình độ; thứ tự lộ trình | `LessonService` | CONFIRMED | 1465 → 466; `sort` bị bỏ qua (cố định) |
| Tra từ → phiên âm/nghĩa/ví dụ | `vocabularyService` | CONFIRMED | `/dictionary/hello` 200 |
| **"gõ 1 ký tự → không ra gì"** | doc §4.2 | **IMPRECISE** | quan sát ĐÚNG nhưng **cơ chế khác**: UI vẫn gọi `/dictionary/h` (không guard `<2`); đã thêm guard → **F-17-02 FIXED** |
| **Fallback: cache → online → local** | doc §4.1 | **REFUTED** | code: cache(proxy) → **local DB**(`:117`) → **direct online**(`:155-160`) — 2/3 đảo → **C-17-01** (sửa doc) |
| `<2` ký tự không tìm (backend) | `VocabularyController:46` | CONFIRMED | `search-sort.js` `"a"` → `[]` |
| `/api/vocabulary` cần login | `SecurityConfig` | CONFIRMED | deep-probe roles |
| Admin LIKE `%kw%` ~185ms, không thêm index | `findAdminPage` | CONFIRMED | perf-probe 190/199ms |

## Tổng hợp

- **CONFIRMED:** 24 khẳng định.
- **REFUTED / IMPRECISE (đã sửa doc):** 4 — F-17-02 (thêm guard), F-17-03 (nhãn nút), F-17-04 (correctAnswer null),
  C-17-01 (thứ tự fallback). Cộng F-17-05 (đặc tính chậm ~20s) đã ghi vào doc.
- **Không khẳng định nào của doc bị bỏ sót** — coverage guard: mọi `file:line` trong doc đều đã đối chiếu.
