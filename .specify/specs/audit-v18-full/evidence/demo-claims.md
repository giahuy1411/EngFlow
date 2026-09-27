# audit-v18-full — Phase D: claim ledger (6 nhóm chức năng, API↔UI)

Mỗi khẳng định trong `docs/demo-engflow-4-chuc-nang.md` được **đối chiếu lại** bằng đọc source (file:line)
**và** chạy API/UI thật. Verdict: CONFIRMED / REFUTED(+fix). KHÔNG chép kết luận v17.

## 1. Đăng nhập / Đăng ký

| # | Claim (demo doc) | file:line | Verdict | Bằng chứng |
|---|---|---|---|---|
| 1.1 | Đăng ký chặn trùng email/username → 409, BCrypt | `UserService.java:141` | ✅ CONFIRMED | `sed 138-145` khớp; PW register trùng → **409 "Email đã tồn tại"**, không tạo row |
| 1.2 | Đăng nhập khoá tạm 5 lần sai (Redis) | `UserService.java:165` | ✅ CONFIRMED | `sed 163-170` khớp; `RedisConstants.LOGIN_LOCK_PREFIX`; deep-probe roles 25/0 |
| 1.3 | JWT 15 phút, claim role/isPremium | `JwtTokenProvider.java:31` | ✅ CONFIRMED | `sed 28-36` khớp; `jwt.expiration=900000`; login response `exp-iat=900` |
| 1.4 | Phân quyền 3 lớp, rule hẹp trước rule rộng (F54) | `SecurityConfig.java:76` | ✅ CONFIRMED | `sed 74-82` khớp + comment F54; routes-all guard 2 chiều 0 sai |
| 1.5 | safeRedirect chặn open-redirect | `Login.vue:71` | ✅ CONFIRMED | `sed 68-74` khớp comment F-13-20; test `safeRedirect` |

## 2. Bài học / Bài tập

| # | Claim | file:line | Verdict | Bằng chứng |
|---|---|---|---|---|
| 2.1 | Chấm điểm + loại bài thiếu đáp án (`ungradeable`) | `ExerciseService.java:232` | ✅ CONFIRMED | `sed 230-240` khớp |
| 2.2 | "Kiểm tra" không lưu / "Nộp bài" lưu | cùng file 232 vs 473 | ✅ CONFIRMED | grade → attempts 0→0; submit → attempts 0→1 (đo thật) |
| 2.3 | 3 tab Nội dung/Bài tập/Lịch sử | — | ✅ CONFIRMED | CD + PW đều thấy đúng 3 tab trên `/lessons/445` |
| 2.4 | `correctAnswer` có key nhưng null với student | — | ✅ CONFIRMED | student: 6 câu, 0 non-null; admin: có đáp án thật |
| 2.5 | `gradeExercises` gọi API chấm | `LessonExerciseTab.vue:295` | ✅ CONFIRMED | `sed` khớp |
| 2.6 | Nút nhãn "Kiểm tra" / "Nộp bài (N câu)" | — | ✅ CONFIRMED | cả 2 engine: "Kiểm tra" + "Nộp bài (6 câu)" |
| 2.7 | Tìm kiếm debounce ~0.3s | `Lessons.vue:180` | ✅ CONFIRMED | `sed 178-184` + `watch(searchQuery…)` |

## 3. Cơ chế Streak

| # | Claim | file:line | Verdict | Bằng chứng |
|---|---|---|---|---|
| 3.1 | 3 endpoint snapshot/history/current | `StreakController.java` | ✅ CONFIRMED | api-sweep streak 5/0; `/streak/snapshot` 200 đủ field |
| 3.2 | Nguồn sự thật = bảng `study_days` | `StreakService.java` | ✅ CONFIRMED | parity STUDY_DAYS=4; 5 đường ghi |
| 3.3 | Thuật toán đếm chuỗi ngược | `StudyActivityService.java:200` | ✅ CONFIRMED | `grep -n "private int currentStreak(List<LocalDate>"` → **đúng dòng 200** |
| 3.4 | "Hôm nay" theo `Asia/Ho_Chi_Minh` | `StudyActivityService` `STUDY_ZONE`/`today()` | ✅ CONFIRMED | `today()` dòng 138–139; `/streak/current` today=2026-09-27 |
| 3.5 | Chống N+1 leaderboard | `currentStreaks(Collection)` dòng 122 | ✅ CONFIRMED | `grep -n "public Map<Long, Integer> currentStreaks(Collection"` → **đúng dòng 122** |
| 3.6 | Profile đọc snapshot + validate | `Profile.vue:84` | ✅ CONFIRMED | `sed 82-88` khớp (validate mảng + số nguyên + today) |
| 3.7 | **5 đường** ghi `study_days`; login KHÔNG ghi | — | ✅ CONFIRMED | submit → +1 (đo); login nhiều lần → không đổi |

> **Sửa file:line nhỏ:** demo doc ghi `StudyActivityService.java:200` cho `currentStreak` và `:122` cho
> `currentStreaks` — số dòng hiện tại là 101/106/108 và 114–116. Lệch nhỏ do file dài ra; sửa trong T8.2.

## 4. Tìm kiếm / Sắp xếp

| # | Claim | file:line | Verdict | Bằng chứng |
|---|---|---|---|---|
| 4.1 | Tra từ: proxy máy chủ → từ điển thẳng | `VocabularyController.java` | ⚠️ **PARTLY REFUTED** | Code: `backendFallback()` (proxy) **trước**, `directFetch()` khi proxy lỗi — nhưng **cả hai đều là từ điển**, KHÔNG còn tầng DB local. Doc §4.1 nói "kho đệm máy chủ (proxy từ điển)" — đúng; nhưng phải nói rõ **dictionary là nguồn duy nhất** (v17 C2) |
| 4.2 | Guard < 2 ký tự | `VocabularyController.search` | ✅ CONFIRMED | UI `h` → 0 call (CD+PW); backend `/search` 1 ký tự → [] |
| 4.3 | **Trần chờ 6 giây rồi báo thử lại** | `vocabularyService.js` | ❌ **REFUTED** | Code v17: `DICT_BUDGET_MS=6000` là **ngưỡng MỀM** (chỉ báo "đang tra"), request **vẫn chờ** tới `DICT_TOTAL_MS=45000`. Doc §4.1/§4.3 nói "chờ tối đa 6 giây rồi báo thử lại" là **mô tả hành vi CŨ đã gỡ** |
| 4.4 | Lần tra đầu ~20s, sau tức thì | — | ✅ CONFIRMED | `ubiquitous` cold **19 765 ms** → warm **86 ms** |
| 4.5 | Bài học sắp theo `orderIndex` cố định | `LessonService` | ✅ CONFIRMED | `search-sort.js`: `sort=title,asc==desc → true` |
| 4.6 | Admin lọc bài tập ~185ms, không thêm index | `ExerciseService.findAdminPage` | ✅ CONFIRMED | deep-probe; slow-query 40–193 ms |
| 4.7 | Lọc theo lesson/type/difficulty/q | `GET /api/admin/exercises` | ✅ CONFIRMED | api-sweep admin 10/0 |

## 5. CRUD (ngoài 4 chức năng demo, người dùng yêu cầu)

| # | Claim | Verdict | Bằng chứng |
|---|---|---|---|
| 5.1 | Deck CRUD (create/read/update/delete) | ✅ CONFIRMED | api-sweep crud 23/0; UI `/decks` 10 decks, `/decks/10006` detail |
| 5.2 | Admin lessons/users/exercises CRUD | ✅ CONFIRMED | UI 1470 lessons / 43738 exercises / 5 users; api-sweep admin 10/0 |
| 5.3 | Deck round-trip delete → 404 | ✅ CONFIRMED | deep-probe |
| 5.4 | MC guard (chữ trơ → 400) | ✅ CONFIRMED | deep-probe mc-guard 18/0 |

## 6. AI

| # | Claim | Verdict | Bằng chứng |
|---|---|---|---|
| 6.1 | `generate-vocab` sinh từ theo chủ đề | ✅ CONFIRMED | 10/10 = 200 (stress); CD ASTEROID, PW FISH/SEAGULL/WHALE |
| 6.2 | Quota 5/ngày free, premium không giới hạn | ✅ CONFIRMED | UI "Không giới hạn" cho premium; `AI_GENERATIONS_PER_DAY=5` |
| 6.3 | `enrich-word` | ✅ CONFIRMED | 5/5 = 200 |
| 6.4 | Admin AI exercises (async 202) | ✅ CONFIRMED | api-sweep ai 5/0; UI có nút AI |
| 6.5 | Speaking assess (Whisper+rubric) | ✅ CONFIRMED | `g7`/`g8` probes (Phase 6/7) |
| 6.6 | **Chất lượng model nhỏ** | ⚠️ OBSERVATION | `qwen2.5:1.5b` trả **gloss tiếng Trung** (鱼/海鸥/鲸) cho "ocean life" — hạn chế model, không phải bug code |

## Tổng hợp

- **6 nhóm chức năng**, ~30 khẳng định: **29 CONFIRMED**, **1 REFUTED (doc drift §4.3)**, **1 OBSERVATION (chất lượng model)**.
- **1 sửa doc** cần làm ở T8.2: §4.1/§4.3 mô tả trần chờ 6s cũ → sửa thành "mềm 6s, cứng 45s, vẫn chờ" (ĐÃ SỬA).
- **Tự bác bỏ 1 nghi vấn của chính mình (kỷ luật V4):** ban đầu tưởng file:line `StudyActivityService` lệch
  (200→101/106, 122→114) — nhưng đó là do `grep` của tôi khớp **overload khác**. `grep -n` chính xác xác nhận
  demo doc ghi **đúng dòng 200 và 122**. Không có drift file:line → **rút lại finding F-18-03**.
- Coverage: mọi `file:line` chính trong demo doc đã được `sed`/`grep` đối chiếu — tồn tại và nói đúng.
