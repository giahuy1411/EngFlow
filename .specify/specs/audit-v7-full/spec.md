# Spec — audit-v7-full
**Feature ID:** audit-v7-full · **Ngày:** 2026-09-12 · **Kế thừa:** audit-v6-full (2026-09-04)
**Bất biến:** audit READ-ONLY-with-fix; không phá UI hiện có (AGENTS.md); mọi finding phải verify bằng code:line TRƯỚC khi fix; mọi fix phải có bằng chứng runtime (P8 constitution).

## 1. Mục tiêu

1. Quét toàn bộ codebase (backend + frontend) và DB (SQL Server + Redis trong Docker).
2. Test backend bằng cách chạy TOÀN BỘ API: CRUD Bài học/Bài tập, streak, đăng nhập/đăng ký, tìm kiếm/sắp xếp, AI.
3. Verify frontend bằng trình duyệt thật (chrome-devtools/playwright MCP) tương ứng từng API; kiểm tra design system Playful Geometric + font Be Vietnam Pro đã đồng bộ với prompt yêu cầu chưa.
4. Kiểm tra DB trong Docker + tối ưu hiệu năng (đo trước/sau — P5).
5. Vá lỗ hổng (prompt + code + DB).
6. Chạy lại lần 2 khép kín: test suite + sweep + UI walkthrough.
7. Báo cáo chi tiết tiếng Việt.

## 2. Trạng thái đầu (đã đo 2026-09-12 — hiệu chỉnh sau analyze I1)

- Backend tests: **314 pass / 0 fail** (surefire run `audit-v7-backend-test4.log`; baseline AGENTS.md 311 + 3 `DeckControllerMyDecksTest` wave-1. Con số "319" ghi đầu kỳ là đọc nhầm từ XML stale `target/surefire-reports` — đã loại trừ theo đúng note AGENTS.md). Kết thúc kỳ: **327** (thêm 10 `AuditV7SecurityWaveTest` + 1 underpayment + 2 save-vocab).
- Frontend tests: **79 pass / 16 files** (khớp baseline AGENTS.md).
- Container: backend, sqlserver(healthy), redis, minio, whisper, tts, frontend — tất cả Up.
- Ollama: 2 models OK. Login `user@gmail.com`/`admin@gmail.com` + `123456` OK.
- Quét prompt-design: **0** lần Outfit/Plus Jakarta Sans trong frontend; Be Vietnam Pro đã là font duy nhất → yêu cầu "thay font" = ĐÃ đồng bộ. (Ghi chú F69: URL weights đã nạp 400–900; round 2 cắt còn 5 weight đúng usage + italic, 800 = 0 lượt dùng.)

## 3. Findings cần fix (đã VERIFY code:line — nguồn: static audit + kiểm chứng trực tiếp; xóa các false positive)

### P1 (bảo mật / crash / mất dữ liệu) — sau bước clarify, chỉ F34 còn sống

| ID | Lỗ hổng | Trạng thái |
|---|---|---|
| **F32** | IDOR đọc submission người khác | ~~P1~~ → **FALSE POSITIVE** (clarify Q1): `SpeakingSubmissionService.getSubmissionForViewer:223-229` có owner-check + admin-bypass; controller gọi đúng hàm này |
| **F33** | IDOR xóa attempt `deleteAttempt(id)` | **FALSE POSITIVE**: endpoint không tồn tại (ProgressController.java chỉ có `/progress` GET, 31 dòng) — finding từ static audit hallucinate |
| **F34** | NPE → **HTTP 500** khi gọi `/api/decks/my` không token | **ĐÃ FIX + regression test** (`DeckControllerMyDecksTest`, 3 case; stack trace docker logs xác nhận DeckController:43). Verify live: chờ rebuild |

### P2 (thiếu phòng thủ / hiệu năng) — phân loại lại sau clarify + db-audit

| ID | Lỗ hổng | Trạng thái |
|---|---|---|
| **F35** | SecurityConfig thiếu rule video-lessons mutations | → P3 **no-op**: mọi route admin nằm dưới `/api/v1/admin/**`=hasRole ADMIN (line 101) + `@EnableMethodSecurity` đang bật + `requireAdmin()` inline. 3 tầng phòng thủ, không gap khai thác |
| **F36** | MediaProxy allowlist rộng + SSRF/OOM | **FALSE POSITIVE**: proxy chỉ đọc MinIO internal qua `getObject(objectKey)`, không fetch URL user đưa; `video-uploads/` không có ở MediaProxy. Ghi chú P3: objectKey không prefix-check (risk thấp, key chứa UUID) |
| **F37** | LessonController `@AuthenticationPrincipal Object` | no-op — authz URL rule ADMIN đã chắn (probe L4=403, L5=401) |
| **F38** | ExerciseService.getExercisesByLesson không authz tầng service | controller check inline đã chắn (probe: user includeAnswers → 403) |
| **F39** | Admin tự demote | chấp nhận được ở app nội bộ; recovery bằng SQL; không sửa |
| **F40** | VideoLessonService viewCount race | **FALSE POSITIVE**: `setViewCount` không tồn tại trong file (grep 0 kết quả) |
| **F41** | `exercises.correct_answer` nvarchar(4000) | db-audit: 100% giá trị ≤100 ký tự → resize nvarchar(500) an toàn; lợi ích page-scan nhỏ (cột 4000 varchar không chiếm dung lượng nếu dữ liệu ngắn) → làm nếu rẻ, không gây rủi ro |
| **F42** | api.js retry + idempotency key | **FALSE POSITIVE**: grep `Idempotency|retry|retries` frontend/src = 0 kết quả; api.js 67 dòng không có retry-interceptor |

### P2-mới (từ db-audit, thay chỗ các false positive)

| ID | Phát hiện | Hành động |
|---|---|---|
| **F49** | `lessons.content_original` ~72.5 MB = **45% toàn DB** (avg 24,827 chars, đọc bởi không ai) | KHÔNG DROP — ràng buộc v6 (giữ backup); ghi report khuyến nghị user quyết |
| **F50** | 4 bảng backup `exercises_bak_v5/v5b/v5c/v5d` (2,144 KB, 100% overlap id với exercises live) + `sysdiagrams` (408 KB) | Không drop (ràng buộc v6); report khuyến nghị |
| **F51** | PK clustered `exercises` **95.11% fragmentation**, stats 9 ngày cũ | REBUILD ONLINE index + UPDATE STATS hai bảng lớn → đo lại scan time trước/sau (P5) |
| **F52** | 629 nhóm / **2,534 exercise trùng lặp** (5.8%); 5,434 bài rỗng correct_answer (12.42%) | Ghi report; dedup/backfill ngoài phạm vi phiên (đã chốt v6) |

### P3 (vệ sinh)

| ID | Lỗ hổng |
|---|---|
| **F43** | Dead code: `VideoLessonService.generateQuiz` (0 call-site), `DashboardService.getLevelStats` (0 call-site), `LeaderboardService.getWeeklyTop`, `AdminController.getStats` (FE không gọi), bucket `ai-generate` trong `RateLimitFilter.java:75-78` (path cũ không còn) |
| **F44** | `LocalDateTime.now()` không-múi: LessonService:197, DeckService:49, GameService:140, DashboardService:69,92; `LocalDate.now()` StreakService.java:57 (P6-F1 mới only) |
| **F45** | Comment sai: `AdminLessonBuilder.vue:192` ghi "API login tra token o `data.token`" nhưng `authService.login:7` đọc `response.data.token` |
| **F46** | `SecurityConfig.java:107` `.formLogin(withDefaults())` còn bật → attack surface /login HTML không cần trên stateless API |
| **F47** | `/api/shop/items` permitAll nhưng **controller không tồn tại** → rule chết (dọn) |
| **F48** | `speaking-submissions` bảng có **0 index FK** (static audit claim) — verify + thêm index FK nếu thiếu |

### Lỗ hổng trong chính prompt yêu cầu (yêu cầu #2 — ghi để sau vá prompt, không đổi thiết kế đã chốt)

| ID | Lỗ hổng prompt | Xử lý |
|---|---|---|
| **G8** | Prompt design-system ghi "Outfit/Plus Jakarta Sans" nhưng user lại yêu cầu thay **toàn bộ** bằng Be Vietnam Pro → mâu thuẫn nội tại; constitution P6 đã chốt BVP. | Verify runtime (mục 4.3); không quay lại Outfit |
| **G9** | Prompt là thiết kế cho **React + Tailwind** ("Lucide React", `bg-[url(...)]`) nhưng stack thật là **Vue 3**; không nêu gì về dark mode, i18n, và "py-24/max-w-6xl" là spec landing page, không phải app UI nhiều màn. | Ghi nhận; áp dụng token, không áp dụng landing-page layout vào app |
| **G10** | Prompt không định nghĩa tiêu chí "đã đồng bộ" đo được → không thể verify khách quan. | Fix bằng tiêu chí đo được: computed font-family == "Be Vietnam Pro" trên MỌI element của mọi route; shadow/border/radius đúng token trên component chính |
| **G11** | Workflow prompt (`constitution→…→converge`) không nêu rõ artifact mới đè hay kế thừa artifact v6 → rủi ro mất lịch sử. | Tạo dir `audit-v7-full` riêng, constitution giữ nguyên vì không đổi principle |

### P1/P2 phát hiện THÊM trong quá trình implement (supplement I2 — live-verified trước khi sửa)

| ID | Lỗ hổng | Trạng thái cuối |
|---|---|---|
| **F54** | `GET /lessons/*/exercises/attempts**` noauth → 500 NPE; permitAll `/api/lessons/**` che authz | FIXED 2 lớp (rule + guard) + 2 tests; live 401 |
| **F55** | Media proxy mở cho mọi object key đã biết; `<audio>` không gửi header | FIXED signed HMAC URL (MediaSigner) + owner-fallback; 4 tests; live 200/403 |
| **F56** | Public structure GET INSERT rows (write qua read-endpoint) | FIXED virtual section read-only; 2 tests; live 0 rows |
| **F53** | Đáp án lộ công khai trong lesson content cho learner | FIXED wrapAnswerSections `<details>`; live 6 blocks + browser |
| **F57** | SEPay webhook không đối chiếu amount (underpayment vẫn kích Premium) | FIXED + underpayment-reject test |
| **F58** | Seed fallback password không dấu vết khi env thiếu | FIXED warn-log (giữ dev fixture theo AGENTS.md) |
| **F60** | save-vocab: thiếu @Valid, batch vô hạn, không tốn quota | FIXED + 2 tests (empty/oversize) |
| **F61** | Rate-limit thiếu bucket cho AI/upload/order | FIXED 3 bucket; live 429 đúng ngưỡng |
| **F62** | Enum query rác → 500 (6 sites valueOf) | FIXED 400 ProblemDetail; live |
| **F63–F70** | Design wave (donut đen, mobile shadow/motion, admin drawer/tables, font weights, contrast) | FIXED F63/64/65/67/68/69/70; **F66 = false positive** retract |

## 4. Phạm vi kiểm tra (Definition of Test)

### 4.1 Backend API (live, port 8080)
- [x] GET sweep 53 endpoint (r1) — phân loại lại 404 do ID sai → rerun với ID thật (r2 33/33, r4-khép-kín 42/42)
- [x] CRUD qua API: lesson ✅; exercise ✅; admin speaking-prompt ✅; video-lesson ✅ (admin UI + API); deck ✅; vocabulary ✅ (admin vocab 200 + search 200)
- [x] Auth: register 201/409/400, login sai pw 400 (validation), forgot/reset 200/200 anti-enumeration + 400 bogus token
- [x] Streak: `/current` {today:2026-09-12,currentStreak} + `/history` list; noauth 401 (date-tương-lai = false-positive có chủ đích do TZ drift, xem §5/R4)
- [x] Tìm kiếm/sắp xếp: lessons q/level/page/size (clamp 200/400), decks q, vocabulary search, leaderboard ALL/WEEKLY
- [x] AI: quota-status + generate paths qua sweep; save-vocab validation 5 tests; AI pipeline nặng verify ở UI v6 (không đổi prompt kỳ này)
- [x] Authz matrix: user→admin 403; noauth→401; includeAnswers 403/200; attempts 401 guest (F54)

### 4.2 DB trong Docker
- [x] Inventory + size 24 bảng/46.705 rows; backup tables (`*_bak_v5*`, sysdiagrams) còn sót — keep per §5
- [x] Index: idx_exercises_lesson_type_order usage stats; missing → tạo M-1/M-2/M-3; FK index speaking_submissions — verify FK 18, 0 orphan
- [x] Data quality: 5.434 correct_answer rỗng; 9/367 LISTENING thiếu audio (số AGENTS.md stale — đã đo lại); future dates = TZ artifact
- [x] Redis: 49 keys, 100% TTL, không leak vĩnh viễn
- [x] Tối ưu có đo được (P5): F51 rebuild 95.11%→0.70%, scan 19→1ms; 3 index apply + measure; C-03a xóa findAll 43.7k

### 4.3 Frontend bằng browser thật (chrome-devtools + playwright)
- [x] Walkthrough login (user + admin): /, /lessons, /lessons/445, /videos, /videos/1, /decks, /decks/10006, /decks/10006/play/flashcard, /speaking (+playback), /profile, /search, /leaderboard, /premium, /ai-vocab-generator, /login, /register, /admin/dashboard, /admin/users, /admin/speaking-submissions, admin@375px
- [x] Font: computed == "Be Vietnam Pro" trên MỌI element/iframe route (G10) — 0 non-BVP
- [x] Design token spot-check: app-btn--primary border 2px/radius 9999px/pop-shadow rgb(30,41,59) 4px ✓; donut/bars dùng đúng --geo-* (F63)
- [x] CRUD flows kèm API: create/xóa deck + lesson + prompt + exercise qua r3, grade qua UI, streak render, search render
- [x] Console errors = 0 trên mọi trang quét (lesson 445, decks, videos/1, admin, speaking)
- [x] Mobile 375px: không overflow-x trang user; admin có drawer (F67) + tables scroll (F68); shadow 2px cả hover/active (F64)
- [x] prefers-reduced-motion hoạt động (kể cả translate jump — F65)

## 5. Ràng buộc kế thừa từ v6 (KHÔNG tái phạm / KHÔNG tái tạo)
- Không xóa `content_original`, không drop bảng `*_bak*`/`sysdiagrams` (quyết định user).
- Fetch-Youtube sync translation: giới hạn kiến trúc — không fix trong kỳ này nếu không đổi sang async.
- Mic E2E headless: đã biết fail-soft — verify qua API upload.
- Backfill 5.4k correct_answer: ngoài phạm vi phiên.

## 6. Tiêu chí hoàn thành (DoD)
1. Backend 319 + frontend 79 tests xanh SAU khi merge mọi fix.
2. Mọi P1 có regression test; mọi fix có bằng chứng live (HTTP status / DOM computed / screenshot).
3. Rerun sweep + UI pass 2 không còn lỗi P1/P2 mới.
4. Báo cáo REPORT.md: đã làm / chưa làm / đã fix + cách fix / skill đã nạp.
