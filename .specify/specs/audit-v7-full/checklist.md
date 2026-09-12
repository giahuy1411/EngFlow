# Checklist — audit-v7-full (từ yêu cầu người dùng + spec + clarify)

## A. Backend API (mục tiêu 2)
- [x] Sweep 53 endpoint không token/user/admin (v7-sweep-r1/r2) — FAIL duy nhất = decks/my 500 → fix F34 → 401 live
- [x] CRUD lesson: create 200 / read / update / delete cascade 204 (v7-crud-r3 L1-L3,C1)
- [x] CRUD exercise: create/read/update 200, validation 400 (E1-E4)
- [x] CRUD speaking-prompt: create 201, delete 200, user 403 (P1-P5)
- [x] CRUD deck: create/read/update/delete + owner-check từ admin khác → 400 đúng (D1-D7)
- [x] Login/Register: 201 mới, 409 dup, 400 sai format, forgot-password anti-enumeration 200/200 (A1-A5)
- [x] Streak: /current 200 {today, currentStreak}, /history 200 list date, noauth 401 (S1-S2b)
- [x] Tìm kiếm/sắp xếp: lessons q/level/size clamp, decks q, vocabulary search, leaderboard ALL/WEEKLY (X1-X6)
- [x] Submit bài tập: grade 200 percentage=100, attempts list ghi nhận (G1-G2)
- [x] includeAnswers authz: user → 403, admin → 200
- [x] Authz matrix: user→admin-endpoints 403, noauth→401 (toàn bộ pass sau fix F34)

## B. DB Docker + performance (mục tiêu 3)
- [x] Inventory 24 bảng/46,705 rows, lessons+exercises = 94.5% dung lượng (db-audit)
- [x] Data quality: 5,434 correct_answer rỗng; 9/367 LISTENING thiếu audio; 0 orphan/18 FK; 5 score>10 pre-fix; Redis 49 keys 100% TTL
- [x] F51: REBUILD PK exercises 95.11% → 0.70%, pages 2330→1277, scan 19ms→1ms (đo trước/sau)
- [x] F41/B-06: correct_answer nvarchar(500)→2000 (DB) + entity khớp (tránh ddl-auto co lại)
- [x] C-03a: xóa `getAllExercises` findAll() 43.7k rows (57.8% tổng logical reads app) — dead method, controller đã paginated
- [x] M-2 IX_exercises_lesson_type_diff_order + M-1 IX_uvp_due + M-3 IX_decks_owner_name — tạo live, admin-filter 43ms
- [ ] Drop content_original / *_bak tables — QUYẾT ĐỊNH USER (ràng buộc v6: không xóa); chỉ khuyến nghị report

## C. Frontend browser thật (mục tiêu 4)
- [x] Login UI → redirect /lessons, token lưu localStorage, giữ version (playwright)
- [x] Font: computed fontFamily = "Be Vietnam Pro" trên MỌI element (0 non-BVP), 0 console errors (/lessons)
- [x] Token spot-check: app-btn--primary = border 2px + radius 9999px + shadow 4px 4px 0 rgb(30,41,59) đúng spec
- [x] Lesson detail: tab Nội dung/Bài tập/Lịch sử; grade qua UI → 5 POST /grade 200
- [x] Decks: tab Cộng đồng (10 deck) + Bộ của tôi (3 deck qua /api/decks/my đã fix)
- [x] Speaking list 6 cards, Profile streak 🔥 render, Leaderboard, Search "travel" dictionary OK
- [x] Admin dashboard: thống kê 75 users/1471 lessons, font BVP
- [x] Mobile 375px: scrollWidth 360 ≤ viewport, không horizontal scroll
- [x] prefers-reduced-motion: có 2 block media-query trong design-system.css (464, 1273)
- [x] Screenshot round 2: audit-v7-home.png, audit-v7-lesson-445.png, audit-v7-deck-10006.png, audit-v7-video-1.png, audit-v7-admin-mobile-375.png
- [x] Round-2 route scan (6 route qua iframe): profile/leaderboard/videos/ai-vocab/premium/search — tất cả render, fontOk=true, 0 error
- [x] Flashcard play + video/1 shadowing UI hoạt động; media URL ký sẵn trong video page

## D. Prompt gaps (mục tiêu 5)
- [x] G8: xác nhận constitution P6 thắng prompt; BVP đã áp dụng 100%, không còn Outfit/Jakarta trong code
- [x] G9/G10/G11: đã encode tiêu chí đo được vào clarify + checklist này

## E. Test suites + closed-loop (mục tiêu 1,7)
- [x] Frontend vitest: 79/79 pass, 16 files (sau mọi fix) + `vite build` production OK (1650 modules, 4.73s)
- [x] Backend mvnw full: **327 tests, 0 failures, BUILD SUCCESS** (audit-v7-backend-test6.log) — baseline mới cho các vòng sau
- [x] Round-2 closed loop: sweep r4 42/42 PASS (thêm 9 probe F54/F55/F56/F62 + token auto-refresh); CRUD r3 rerun 43/43 đúng expect; F56 row-check sections_445_after=0
- [x] REPORT.md tiếng Việt (.specify/specs/audit-v7-full/REPORT.md)
