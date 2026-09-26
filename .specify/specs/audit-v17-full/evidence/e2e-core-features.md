# audit-v17-full — Phase 4 + D: E2E core features (API ↔ UI)

Bằng chứng từ **cả hai phía**: HTTP thật (:8080) và UI thật (chrome-devtools MCP, :5173). Chi tiết bấm từng bước:
`mcp-walkthrough.md`. Claim ledger 4 chức năng demo: `demo-claims.md`.

## 1. Đăng nhập / Đăng ký

| Claim | API | UI | Verdict |
|---|---|---|---|
| Đăng nhập đúng → token | `POST /api/auth/login` 200 + token | `/login` → `/lessons`; localStorage **đúng** `token`+`user` | ✅ |
| Token TTL 15 phút | `JwtTokenProvider` `jwt.expiration=900000` | (không đo được hết hạn trong phiên — giới hạn) | ✅ (source) |
| Sai mật khẩu 5 lần → khoá | `UserService:208` `fails >= MAX_LOGIN_FAILS(5)` → 400; `RedisConstants:45-46` 15 phút | (không khoá tài khoản demo — tránh tự gây khoá; verify bằng source + `deep-probe` role) | ✅ (source) |
| Guard admin | meta `requiresAdmin` | student `/admin/users` → **`/`** | ✅ |
| 401 giữa phiên → logout | `api.js:79-83`, `auth.js:112-123` | (có test `api.test.js` F7-BUG02) | ✅ |
| Chống open-redirect | `safeRedirect` | (test `safeRedirect`) | ✅ |

## 2. Bài học / Bài tập

| Claim | API | UI | Verdict |
|---|---|---|---|
| 3 tab (Nội dung/Bài tập/Lịch sử) | — | snapshot `/lessons/445` có **đúng 3 tab** | ✅ |
| Chấm thử **không lưu** | `POST .../grade` không tạo `ExerciseAttempt` | trả lời → "Kiểm tra" → **F5** → Lịch sử **"CHƯA CÓ LỊCH SỬ"** | ✅ |
| Nộp bài **có lưu** | `POST .../submit` → `ExerciseAttempt` | "Nộp bài (6 câu)" → Lịch sử **"1/6 câu đúng, 0 phút trước"** | ✅ |
| Máy chủ chấm lại (không tin client) | `submitExercises` gọi lại `gradeExercises` | — | ✅ (source `:473`) |
| Bài thiếu đáp án → `ungradeable` | `ExerciseService:263-270` | UI: "chưa có đáp án chuẩn nên không tính điểm" | ✅ |
| Không lộ `correctAnswer` | response có key nhưng **null** | Network reqid=325: mọi câu `correctAnswer:null` | ✅ (doc tinh chỉnh F-17-04) |
| `includeAnswers=true` non-admin → 403 | `LessonExerciseController:40-42` | — | ✅ (source + `deep-probe`) |
| Nộp bài → ghi streak | `ExerciseService:520` `recordStudy` | DB `study_days` +1 (đã verify + **dọn** id 40082) | ✅ |

## 3. Cơ chế Streak

| Claim | API | UI | Verdict |
|---|---|---|---|
| "Hôm nay" theo máy chủ (VN) | `/api/streak/current` → `today=2026-09-26`; `StudyActivityService.today():138` | — | ✅ |
| Ô Streak = lịch 30 ngày | `/api/streak/snapshot` `studiedDays=["2026-09-21","2026-09-22"]` | `/profile` Streak 0, lịch 1–27, "HÔM NAY CHƯA HỌC" | ✅ |
| Đếm chuỗi liền nhau, gãy → 0 | `currentStreak(List,LocalDate):200` | — | ✅ (source) |
| **5 đường** ghi `study_days` | `ExerciseService:520`, `FlashcardService:69`, `SrsService:116`, `SpeakingSubmissionService:134`, `StreakService:22` | flashcard 10/10 → `study_days` +1 (dọn 40083) | ✅ |
| **Login KHÔNG ghi** | `UserService.login` chỉ đọc | login → `study_days` không đổi | ✅ |
| Chống N+1 leaderboard | `currentStreaks(Collection)`:122 | `/leaderboard` render nhanh | ✅ |

## 4. Tìm kiếm / Sắp xếp

| Claim | API | UI | Verdict |
|---|---|---|---|
| Tìm bài học + trình độ | `GET /api/lessons?q=&level=` | gõ `grammar` → 1465 → **466** | ✅ |
| Thứ tự cố định `orderIndex` | `LessonService` | `search-sort.js`: `sort=title,asc==desc -> true` | ✅ |
| Tra từ → phiên âm/nghĩa/ví dụ | `/api/vocabulary/dictionary/hello` 200 | `/search` `hello` → `/həˈləʊ/`, NOUN/VERB/INTERJECTION, ví dụ | ✅ |
| **<2 ký tự không tìm** | `/api/vocabulary/search` guard `:46` | **TRƯỚC fix:** gõ `h` vẫn gọi `/dictionary/h` (reqid=475) → **SAU fix:** không gọi | ✅ (F-17-02 FIXED) |
| 3 tầng fallback | `vocabularyService.js`: proxy(`:152`) → local DB(`:117`) → direct(`:155-160`) | — | ✅ (doc sửa F-17-05/C-17-01) |
| `sort=notacolumn` → 400 | `search-sort.js` | — | ✅ |
| `/api/vocabulary` anon → 401 | `SecurityConfig anyRequest().authenticated()` | — | ✅ (deep-probe) |

## 5. CRUD

| Claim | API | UI | Verdict |
|---|---|---|---|
| Decks list/detail | `/api/decks`, `/api/decks/{id}` 200 | `/decks` 10 bộ; `/decks/10006` 10 từ, 6 chế độ | ✅ |
| Deck owner-scoped | `DeckController` POST/PUT/DELETE + owner check | (deep-probe F147: non-admin thiếu `deckId` → 400) | ✅ |
| Admin lessons/users CRUD | `/api/admin/lessons`, `/api/admin/users` | `/admin/users` 5 người + thao tác Khóa/Cấp Premium | ✅ |
| Admin stats khớp DB | `/api/admin/stats` | `/admin/dashboard`: 5 / 1470 / 43738 / 118 | ✅ |

## 6. AI

| Claim | API | UI | Verdict |
|---|---|---|---|
| Sinh bài tập (Ollama) | `POST /api/admin/exercises/ai/generate-async` 202 + poll | `/admin/exercises` AI panel | ✅ (api-sweep ai 5/0) |
| Quota free 5/ngày | `UserService` `AI_GENERATIONS_PER_DAY=5` → 403 | `/ai-vocab-generator` | ✅ (api-sweep) |
| `count` là trần (F84), MC không trùng (F85) | `deep-probe` mc-guard 18/0 | — | ✅ |
| Chấm speaking (Whisper + rubric) | `POST .../assess`; mic giả → `FAILED` đúng thiết kế | `/speaking` | ✅ (giới hạn media thật) |

## 7. Chức năng khác

| Chức năng | Verdict | Bằng chứng |
|---|---|---|
| Video lesson | ✅ | `/videos/1` iframe YouTube thật + transcript + quiz |
| Leaderboard | ✅ | 5 người, xếp hạng (student #1) |
| Flashcard / game | ✅ | 10/10 HOÀN THÀNH |
| SRS | ✅ | api-sweep srs 6/0 |
| Payment | ✅ (1 blocked) | create-order 200, webhook chữ ký sai → 200; chữ ký thật = **BLOCKED** (real-money) |

---

**Kết luận:** 4 chức năng demo + CRUD + AI **hoạt động ổn định** qua cả API lẫn UI; 2 defect thật đã fix
(F-17-01, F-17-02), 3 điểm doc sai đã sửa, 1 đặc tính UX ghi nhận (F-17-05).
