# audit-v17-full — Phase U: interactive MCP UI/UX walkthrough (chrome-devtools MCP)

**Engine:** chrome-devtools MCP (primary). Playwright MCP = BLOCKED (missing Chrome channel) → second engine is
the headless `playwright-core` harness (see `mcp-engines.md`).
**Cách chạy:** mở `http://localhost:5173`, đăng nhập thật, bấm từng bước; mỗi bước ghi snapshot + console + network.
Ảnh/snapshot lưu ở `evidence/shots/mcp/**`.

---

## U3 — Đăng nhập (happy path) ✅

1. `/login` → snapshot: h1 "ĐĂNG NHẬP", skip-link "BỎ QUA ĐIỀU HƯỚNG" là focusable đầu tiên, form có EMAIL/MẬT KHẨU/
   GHI NHỚ, nút "Đăng nhập".
2. Điền `user@gmail.com` / `123456` → bấm Đăng nhập → **điều hướng `/lessons`**.
3. `evaluate_script`: `localStorage` = **đúng 2 key** `["token","user"]`; `user.isAdmin=false`; `isPremium=true`.
4. `list_network_requests`: `GET /api/auth/me` **200**; `list_console_messages`: **0 error**.

→ Khớp demo doc §1.2 bước 2–3.

## U4 — Guard admin (điểm nhấn bảo mật) ✅

1. Đang là student, gõ thẳng `/admin/users`.
2. Kết quả: `page.url()` = **`http://localhost:5173/`** → **bị đá về trang chủ**.

→ Khớp demo doc §1.2 bước 4. Ảnh: `U4-guard-bounce-home.png`.

## U6 — Danh sách bài học + tìm kiếm ✅

1. `/lessons` → 12 bài/trang, tổng **1465 bài**, nút trình độ (Tất cả/Elementary/Pre-Int./Intermediate/Upper-Int.),
   ô "Tìm bài học", phân trang ("1 - 12 / 1465").
2. Gõ `grammar` vào ô tìm kiếm → (debounce) → danh sách còn **466 bài**, toàn bộ khớp "grammar".

→ Khớp demo doc §2.2 bước 1 (tìm + chọn trình độ + phân trang). Snapshot: `U6-lessons-snapshot.txt`.

## U7 — Tab bài học ✅

`/lessons/445` → snapshot có **đúng 3 tab**: `NỘI DUNG` (selected) · `BÀI TẬP` · `LỊCH SỬ`.
Tab Nội dung render nội dung bài (6 heading bài tập ngữ pháp).

→ Khớp demo doc §2.2 bước 2–3. Snapshot: `U7-lesson445.txt`.

## U9 — Chống lộ đáp án ✅ (kèm tinh chỉnh)

1. Bấm tab `BÀI TẬP` → `GET /api/lessons/445/exercises` (**reqid=325**, 200).
2. Đọc response body: **mọi exercise có key `correctAnswer` nhưng giá trị = `null`** — không lộ đáp án.
3. Kiểm tra source: `LessonExerciseController:28-48` — `includeAnswers=true` mà không phải ADMIN → **403**;
   draft lesson bị `assertLessonVisible` chặn.

→ **Kết luận an ninh ĐÚNG** (không lộ giá trị đáp án). **Tinh chỉnh doc:** demo doc viết *"không có trường
`correctAnswer`"* — thực tế trường **có mặt nhưng null**. Xem finding F-17-04.

## U8 — Chấm thử (không lưu) vs Nộp bài (lưu) ✅

1. Tab Bài tập: 6 câu, mỗi câu có nút **"Kiểm tra"** + cuối trang nút **"Nộp bài (6 câu)"**.
2. Trả lời câu 1 → bấm "Kiểm tra" → alert **"✅ ĐÚNG"** (chấm ngay).
3. **F5 tải lại** → tab **Lịch sử** → **"CHƯA CÓ LỊCH SỬ"** → **chấm thử KHÔNG lưu** ✅.
4. Trả lời câu 1 → bấm **"Nộp bài (6 câu)"** → **"✅ ĐÃ LƯU KẾT QUẢ! XEM TAB LỊCH SỬ."**
5. Tab Lịch sử → **"17% · CHƯA ĐẠT · 1 / 6 câu đúng · 0 phút trước"** → **nộp bài CÓ lưu** ✅.

→ Khớp demo doc §2.2 bước 4–6 (chỉ khác **nhãn nút**: UI là "Kiểm tra"/"Nộp bài", doc ghi "Chấm thử" — xem F-17-03).
Ảnh: `U8-history-persisted.png`.

## U10 — Streak (Profile) ✅

1. `/profile` → "HỒ SƠ HỌC TẬP", ô **STREAK = 0**, lịch học 1–27, "HÔM NAY CHƯA HỌC", "🔥 0 ngày".
2. Gọi API trực tiếp từ tab: `GET /api/streak/current` → `{currentStreak:0, today:"2026-09-26"}`;
   `GET /api/streak/snapshot` → `today=2026-09-26, currentStreak=0, studiedToday=false,
   studiedDays=["2026-09-21","2026-09-22"]`.
3. Đối chiếu DB `study_days` user 2: **đúng 2 dòng** `2026-09-21`, `2026-09-22` → **UI khớp API khớp DB**.

→ Khớp demo doc §3.2 bước 2–3 ("số ô Streak và lịch luôn khớp — cùng một nguồn"). `today` do **máy chủ** trả
(Asia/Ho_Chi_Minh), không theo đồng hồ máy khách.

## U11 — Tìm kiếm từ ✅ (kèm tinh chỉnh)

1. `/search` → gõ `hello` → bấm "Tra từ" → render **HELLO /həˈləʊ/**, nghĩa NOUN/VERB/INTERJECTION, ví dụ, SYN/ANT.
   Network: `GET /api/vocabulary/dictionary/hello` **200** (cache cold ~20s → xem F-17-05).
2. Gõ `h` (1 ký tự) → bấm "Tra từ" → Network **CÓ gọi `GET /api/vocabulary/dictionary/h`** (không có guard `<2`
   ở frontend); kết quả **rỗng** (kết quả cũ bị xoá, không hiện gì).

→ **Kết quả quan sát ĐÚNG** ("1 ký tự → không ra gì") nhưng **cơ chế KHÁC doc**: guard `<2` chỉ ở backend
`/api/vocabulary/search` (`VocabularyController:46`), **không** ở frontend và **không** ở `/dictionary/{word}`.
Xem finding F-17-02.

## U12 — CRUD decks ✅

- `/decks` → "BỘ TỪ VỰNG", 10 bộ, tab CỘNG ĐỒNG / BỘ CỦA TÔI / ĐƯỢC CHIA SẺ, nút "Tạo bộ từ" + "Tạo bằng AI",
  phân trang.
- `/decks/10006` → deck detail: 10 từ (AMBITIOUS…), 6 chế độ chơi (FLASHCARD/QUIZ/NGHE/GÕ TỪ/GHÉP CẶP/TỔNG HỢP).

## U13 — Flashcard / game ✅

`/decks/10006/play/flashcard` → lật thẻ + "Tiếp theo" ×10 → **"10/10 · 100% · HOÀN THÀNH!"**.
→ Ghi `study_days` thật (đã verify DB + **dọn** id 40083; parity về 4).

## U18 — Video ✅

`/videos/1` → iframe YouTube **thật** (`2VeQTuSSiI0`, title "English Conversation at a Café"), transcript,
tab PHỤ ĐỀ / SHADOWING / QUIZ, "3/12" tiến độ.
Console: **1 `[warn]`** — `postMessage` origin mismatch từ **YouTube iframe** (third-party, không phải lỗi app;
AGENTS.md ghi nhận lớp này). Xem F-17-06 (ui-sweep đếm nó là "console error").

## U20 — Leaderboard ✅

`/leaderboard` → 5 người học, xếp hạng + điểm + streak. Student **hạng 1** (69 điểm), admin hạng 2 (31).

## U22 — Admin ✅ (dashboard / exercises / users)

- `/admin/dashboard` → stats **khớp DB**: NGƯỜI DÙNG 5 · BÀI HỌC 1470 · BÀI TẬP 43738 · TỪ VỰNG 118 · BÀI NỘP 3.
- `/admin/exercises` → bộ lọc loại bài + ô "Tìm bài tập..." + phân trang.
- `/admin/users` → "5 NGƯỜI DÙNG", bảng Email/Vai trò/Trạng thái/Premium/Thao tác (Khóa, Cấp Premium).
- Các trang admin còn lại (speaking-prompts/submissions, videos, video-attempts) do `ui-sweep` phủ:
  mỗi route render, `con=0 api=0 err=0`.

---

## Tổng hợp Phase U

| # | Chức năng | Verdict | Bằng chứng |
|---|---|---|---|
| U3 | Đăng nhập | ✅ PASS | 2 key localStorage, /api/auth/me 200, 0 console |
| U4 | Guard admin | ✅ PASS | /admin/users → / |
| U6 | Lessons + search | ✅ PASS | 1465 → 466 (grammar) |
| U7 | 3 tab bài học | ✅ PASS | NỘI DUNG/BÀI TẬP/LỊCH SỬ |
| U8 | Chấm thử vs Nộp | ✅ PASS | reload → trống; nộp → "1/6 câu đúng" |
| U9 | Chống lộ đáp án | ✅ PASS (giá trị null) | response 325: correctAnswer=null mọi câu |
| U10 | Streak | ✅ PASS | UI = API = DB (09-21, 09-22) |
| U11 | Tìm từ | ✅ PASS (cơ chế khác doc) | hello có kết quả; `h` gọi /dictionary/h, rỗng |
| U12 | Decks | ✅ PASS | 10 bộ, 6 chế độ |
| U13 | Flashcard | ✅ PASS | 10/10 HOÀN THÀNH |
| U18 | Video | ✅ PASS (1 warn YouTube) | iframe thật + transcript |
| U20 | Leaderboard | ✅ PASS | 5 người, xếp hạng |
| U22 | Admin | ✅ PASS | stats khớp DB |

**Residue đã dọn:** `study_days` 40082 (U8 submit), 40083 (U13 flashcard), `exercise_attempts` 70147 (U8).
Parity sau dọn: `1470|43738|5|118|29|4|3|12|10`, STUDY_DAYS=4.
