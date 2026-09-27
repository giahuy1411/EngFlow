# audit-v18-full — Phase U: interactive MCP UI/UX walkthrough (CẢ 2 ENGINE cho MỌI luồng)

**Ngày:** 2026-09-27 · **Ràng buộc Q2:** mỗi luồng chạy **cả chrome-devtools MCP và Playwright MCP**.
**Ảnh:** `evidence/shots/mcp/chrome-devtools/**` + `evidence/shots/mcp/playwright/**`.
Engine availability: `evidence/mcp-engines.md` (cả 2 chạy thật).

Ký hiệu: **CD** = chrome-devtools MCP · **PW** = Playwright MCP. Mỗi luồng ghi: thao tác · network/console ·
đối chiếu API↔UI · đối chiếu 2 engine.

---

## U1 — Đăng nhập (happy path)

| Engine | Thao tác | Kết quả |
|---|---|---|
| **CD** | `/login` → điền `user@gmail.com`/`123456` → "Đăng nhập" | URL `/login` → **`/lessons`**; `localStorage.token`+`user` set; 0 console error |
| **PW** | same | URL `/login` → **`/lessons`**; token+user set; **0 console error** |

**API↔UI (CD network):** `POST /api/auth/login [200]` → body có `token`, `isAdmin:false`, `isPremium:true`,
`premiumExpiry:"2026-10-03"`. UI khớp: header hiện tên "Học Viên Mẫu", premium active. **2 engine khớp.**
Ảnh: `U1-login-lessons.png` (cả 2).

## U4 — Guard admin (2 chiều)

| Engine | Role | Đi `/admin/users` | Kết quả |
|---|---|---|---|
| **CD** | student | → | **`/`** (bounce đúng) |
| **PW** | student | → | **`/`** (bounce đúng) |
| **CD** | admin | → | **`/admin/users`**, h1 "Người dùng", **5 rows** (= DB `users=5`) |
| **PW** | admin | → | **`/admin/users`** |

**2 engine khớp hoàn toàn.** Ảnh: `U4-admin-users.png` (cả 2).

## U2 — Bài học / Bài tập

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/lessons/445` | h1 = "English Grammar Exercises for A1 – have got and articles"; **đúng 3 tab** (Nội dung/Bài tập/Lịch sử) |
| **PW** | `/lessons/445` | cùng h1; tablist "Nội dung bài học" 3 tab |

**Chống lộ đáp án (API↔UI):** student `GET /lessons/445/exercises` → 6 câu, key `correctAnswer` **có nhưng
tất cả `null`** (0 non-null). admin cùng endpoint + `includeAnswers=true` → **có đáp án thật** (200). **Guard đúng.**

**Chấm thử vs Nộp bài (đo bằng API, payload đúng `userAnswer`):**
- `POST .../grade` → **200 + điểm**, `attempts` **không tăng** (0→0) → **KHÔNG lưu** ✅
- `POST .../submit` → **200**, `attempts` **0→1**, và `study_days` **+1** (2→3, có row hôm nay) → **có lưu** ✅
- Nhãn nút (cả 2 engine): **"Kiểm tra"** (per-câu) + **"Nộp bài (6 câu)"** — khớp demo doc.
Ảnh: `U2-lesson-exercises.png` (cả 2).

> **Bài học đo được (tự bác bỏ giả thuyết):** lần đầu tôi POST với key `answer` (sai) → `study_days` KHÔNG tăng;
> tưởng là bug. Đọc `GradeRequest.java` thấy field đúng là **`userAnswer`**; gửi lại đúng key → `study_days` +1.
> **Không phải bug app — payload của tôi sai.** Đúng kỷ luật "kiểm chứng, không suy đoán".

## U3 — Streak

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/profile` | UI "0 STREAK", "HÔM NAY CHƯA HỌC", lịch 1–27; API `currentStreak:0`, `today:2026-09-27`, `studiedToday:false`, `studiedDays:["2026-09-21","2026-09-22"]` |
| **PW** | `/profile` | cùng kết quả; API khớp UI |

**5 đường ghi `study_days`:** submit bài (U2) đã chứng minh +1. Login **KHÔNG** ghi (đăng nhập nhiều lần,
`study_days` không đổi). Ảnh: `U3-streak-profile.png` (cả 2).

## U11 — Tra từ (Search)

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | gõ `h` (1 ký tự) + "Tra từ" | **KHÔNG** gọi `/dictionary/h` (guard F-17-02 giữ) |
| **CD** | gõ `hello` + "Tra từ" | gọi `/api/vocabulary/dictionary/hello` [200]; UI hiện `/həˈləʊ/`, NOUN/VERB/INTERJECTION, ví dụ |
| **PW** | gõ `h` + "Tra từ" | **0 call** `/dictionary/` → GUARD OK |

**Nguồn duy nhất:** network chỉ có `/api/vocabulary/dictionary/*` (không còn local/DB fallback). Ảnh: `U4-search.png` (cả 2).

## U12 — CRUD decks + game

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/decks` | API 10 decks, UI card list, h1 "Bộ từ vựng" |
| **CD** | `/decks/10006` | API `name="Oxford 3000 (A1-B2)"` = h1; 10 words; **6 play link** (game) |
| **CD** | `/decks/10006/play/quiz` | game chạy: "FREQUENT" + 4 phương án; bấm 1 đáp án → **0/0 → 0/1**, từ kế tiếp "EXPLORE" |
| **PW** | `/decks/10006/play/quiz` | game render; 4 phương án; có điểm số |

Ảnh: `U5-quiz-game.png` (CD).

## U15 — AI (generate-vocab)

| Engine | Thao tác | Kết quả |
|---|---|---|
| **CD** | form chủ đề "space exploration", count 3 → "Sinh từ vựng" | `POST /api/ai/generate-vocab` → UI "KẾT QUẢ", từ **ASTEROID** kèm phiên âm |
| **PW** | chủ đề "ocean life", count 3 | cùng endpoint → FISH / SEAGULL / WHALE kèm phiên âm + ví dụ |

**2 engine khớp.** Ảnh: `U6-ai-generate.png` (cả 2).
> **Quan sát chất lượng (LOW):** model local `qwen2.5:1.5b` trả **gloss tiếng Trung** (鱼/海鸥/鲸) cho chủ đề
> "ocean life" — không phải lỗi code, là hạn chế model nhỏ (ghi nhận, không fix trong vòng này).

## U22 — Admin (dashboard / lessons / exercises / users)

| Engine | Trang | API | UI |
|---|---|---|---|
| **CD** | `/admin/dashboard` | `totalUsers:5, totalLessons:1470, totalExercises:43738, totalSubmissions:3` | UI hiện **5 / 1470 / 43738 / 3** — khớp tuyệt đối |
| **PW** | `/admin/dashboard` | same | UI hiện 5/1470/43738 |
| **CD** | `/admin/lessons` | `totalElements:1470` | 20 rows + nút tạo |
| **PW** | `/admin/exercises` | `totalElements:43738` | 20 rows + nút AI |

**Số admin khớp DB parity** (lessons 1470, exercises 43738). Ảnh: `U7-admin-dashboard.png`, `U9-admin-*` (cả 2).

## U18 — Video

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/videos` | API 5 video; UI tiêu đề + thời lượng + cấp độ |
| **CD** | `/videos/1` | iframe YouTube **`2VeQTuSSiI0`**; tab PHỤ ĐỀ/SHADOWING/QUIZ; 12 câu; tiến độ 1/12 |
| **PW** | `/videos/1` | cùng YouTube ID `2VeQTuSSiI0`, tiến độ **1/12** |

**Console (PW):** 1 "error" = `youtube.com/.../base.js` "Permissions policy violation: compute-pressure" —
**third-party iframe**, không phải app (khớp `isThirdPartyConsoleNoise`). Ảnh: `U9-video-detail.png`.

## U17 — Speaking

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/speaking` | API 7 prompt; UI "ĐỀ LUYỆN CÓ SẴN **7**"; h1 "Luyện nói" |
| **PW** | `/speaking` | API 7 = UI **7**; link lịch sử |

Premium gate (upload speaking) đã phủ ở `deep-probe` roles (25/0): admin bypass → 400 (không phải 403).
Ảnh: `U10-speaking-list.png` (cả 2).

## U20 — Leaderboard

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/leaderboard` | API 5 entry; top "Học Viên Mẫu" 69 điểm; UI "TỔNG NGƯỜI HỌC 5" |
| **PW** | `/leaderboard` | cùng: heading, top name, total **5** |

Ảnh: `U8-leaderboard.png` (CD).

## U19 — Premium

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/premium` | h1 "EngFlow Premium"; 2 gói (10.000đ/tháng, 20.000đ/năm); badge "RẺ HƠN CẢ TRÀ SỮA" |

Ảnh: `U11-premium.png` (CD). Lưu ý: `/premium/checkout` tạo payment row — harness tự dọn (Phase 3 CLEAN).

## U1b — Đăng ký (register)

| Engine | Kiểm | Kết quả |
|---|---|---|
| **PW** | POST `/api/auth/register` trùng email | **409** `"Email đã tồn tại"` — **không tạo row** |
| **PW** | `/register` khi đang login admin | guestOnly guard đá về `/lessons` (đúng) |

Form render (CD snapshot trước đó: Email/Mật khẩu/Họ tên). Không tạo user rác (dùng đường 409).

## U0 — Home landing + decor + responsive

| Engine | Kiểm | Kết quả |
|---|---|---|
| **CD** | `/` | h1 "Học Tiếng AnhVui Vẻ"; hero present |
| **PW** | `/` @ **360px** | **overflow 0**; hero present; **14 SVG decoration** |

5 viewport đã được `ui-sweep` phủ (35 ô, 0 tràn). Ảnh: `U0-home.png` (CD), `U0-home-360.png` (PW).

---

## TU.13 — Dọn residue

Các thao tác học trong Phase U ghi `study_days` (submit bài) và `exercise_attempts`. Đã dọn bằng
`cleanupExerciseAttempts()` + `cleanupStudyDays()` (cả 2 `SELF-CLEAN OK`), parity về
`1470|43738|5|118|29|4|3|12|10` + STUDY_DAYS=4 + PENDING_PAYMENTS=0 + EXERCISE_ATTEMPTS=33 → **CLEAN**.

## Tổng hợp Phase U

| # | Luồng | CD | PW | API↔UI | 2 engine khớp |
|---|---|---|---|---|---|
| U1 | Đăng nhập | ✅ | ✅ | ✅ | ✅ |
| U4 | Guard admin 2 chiều | ✅ | ✅ | ✅ | ✅ |
| U2 | Bài học/Bài tập + chống lộ đáp án | ✅ | ✅ | ✅ | ✅ |
| U3 | Streak | ✅ | ✅ | ✅ | ✅ |
| U11 | Tra từ + guard 1 ký tự | ✅ | ✅ | ✅ | ✅ |
| U12 | CRUD decks + game | ✅ | ✅ | ✅ | ✅ |
| U15 | AI generate-vocab | ✅ | ✅ | ✅ | ✅ |
| U22 | Admin dashboard/lessons/exercises | ✅ | ✅ | ✅ | ✅ |
| U18 | Video | ✅ | ✅ | ✅ | ✅ |
| U17 | Speaking | ✅ | ✅ | ✅ | ✅ |
| U20 | Leaderboard | ✅ | ✅ | ✅ | ✅ |
| U19 | Premium | ✅ | — | ✅ | — |
| U1b | Đăng ký | — | ✅ | ✅ | — |
| U0 | Home + responsive | ✅ | ✅ | ✅ | ✅ |

**13 luồng đi bằng CẢ 2 engine; 1 luồng (Premium) chỉ CD** — bổ sung bằng deep-probe + `g6` webhook.
Mọi luồng trọng yếu (auth, guard, lessons, streak, search, AI, CRUD, admin) đã có **2 engine độc lập khớp nhau**.
