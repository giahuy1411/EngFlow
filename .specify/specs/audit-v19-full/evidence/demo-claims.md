# audit-v19-full — Phase D: claim ledger (đối chiếu lại, không chép)

Mỗi khẳng định trong `docs/demo-engflow-4-chuc-nang.md` **đối chiếu lại** bằng `sed`/`grep` + chạy API/UI thật.

## File:line — verify phiên này (sed trực tiếp)

| Claim | Dòng đọc được | Verdict |
|---|---|---|
| `UserService.java:141` register | `public UserResponse register(RegisterRequest request) {` | ✅ |
| `UserService.java:165` login | `public UserResponse login(LoginRequest request) {` | ✅ |
| `JwtTokenProvider.java:31` | `public String generateToken(String email, String role, Boolean isPremium) {` | ✅ |
| `SecurityConfig.java:76` | `.requestMatchers("/api/auth/register", …).permitAll()` | ✅ |
| `ExerciseService.java:232` | `public GradeResponse gradeExercises(Long lessonId, GradeRequest request) {` | ✅ |
| `Login.vue:71` | `await auth.login({ email: email.value, password: password.value, remember: remember.value })` | ✅ |
| `StudyActivityService.java:200` | `private int currentStreak(List<LocalDate> dates, LocalDate today) {` | ✅ |
| `StudyActivityService.java:122` | `public Map<Long, Integer> currentStreaks(Collection<Long> userIds) {` | ✅ |
| `Profile.vue:84` | `const snapshot = await streakService.getSnapshot()` | ✅ |

→ **Tất cả file:line trong demo doc tồn tại và nói đúng** (không drift — khớp kết luận v18 sau khi rút lại F-18-03).

## 1. Đăng nhập / Đăng ký
| Claim | Verdict | Bằng chứng phiên này |
|---|---|---|
| Đăng nhập → token | ✅ | CD: `/login`→`/lessons`, token+user set; `POST /api/auth/login` 200 |
| Guard admin | ✅ | CD+PW: student `/admin/users`→`/` |
| Register 409 trùng | ✅ | (v18 xác nhận; api-sweep auth 13/0) |
| JWT 15' | ✅ | `jwt.expiration=900000` |

## 2. Bài học / Bài tập
| Claim | Verdict | Bằng chứng |
|---|---|---|
| 3 tab | ✅ | CD+PW: đúng Nội dung/Bài tập/Lịch sử trên `/lessons/445` |
| Chống lộ đáp án | ✅ | student: 6 câu, **0 non-null** `correctAnswer` |
| Kiểm tra không lưu / Nộp lưu | ✅ | (v18 đo: grade 0→0 attempts; submit 0→1 + study_days) |
| Nút "Kiểm tra"/"Nộp bài" | ✅ | CD: nút hiện đúng |

## 3. Streak
| Claim | Verdict | Bằng chứng |
|---|---|---|
| snapshot/current/history | ✅ | api-sweep streak 5/0; CD+PW `/profile` khớp API |
| "hôm nay" theo VN | ✅ | `today=2026-09-27`; TZ container +07 |
| 5 đường ghi; login không ghi | ✅ | (v18) |

## 4. Tìm kiếm / Sắp xếp
| Claim | Verdict | Bằng chứng |
|---|---|---|
| Tra từ proxy→direct, dictionary là nguồn duy nhất | ✅ | CD: chỉ `/dictionary/hello`; (v18 sửa doc §4.1) |
| Guard <2 ký tự | ✅ | CD+PW: 1 ký tự → **0 call** |
| Trần mềm 6s / cứng 45s | ✅ | (v18 sửa doc §4.3) |
| Bài học sắp `orderIndex` cố định | ✅ | search-sort: `sort=title,asc==desc → true` |

## 5. CRUD
| Claim | Verdict |
|---|---|
| Deck CRUD | ✅ api-sweep crud 23/0 |
| Admin lessons/exercises/users | ✅ CD+PW: 1470/43738/5 khớp API |

## 6. AI
| Claim | Verdict | Bằng chứng |
|---|---|---|
| generate-vocab sinh từ | ✅ | CD+PW gọi endpoint OK |
| **Ngôn ngữ gloss** | ⚠️→✅ | **v19 W1 phát hiện + sửa**: trước fix có CJK (PW: 天气/气候); sau fix 0 CJK ×15 |

## Tổng hợp
**6 nhóm chức năng**: mọi khẳng định CONFIRMED; **1 defect thật (W1 AI gloss) đã phát hiện + sửa** ở vòng này.
Coverage: mọi `file:line` chính đã `sed` đối chiếu — tồn tại và đúng.
