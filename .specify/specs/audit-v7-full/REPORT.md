# REPORT — audit-v7-full (kiểm toán khép kín toàn diện EngFlow)

**Ngày**: 2026-09-12 · **Phạm vi**: codebase + DB SQL Server Docker + frontend browser thật
**Kết luận tổng**: toàn bộ 7 mục tiêu của yêu cầu đã hoàn thành với bằng chứng đo được.
Backend **327/327 tests xanh** (baseline cũ 314 → +13 regression mới), frontend **79/79**,
production build OK, sweep khép kín vòng 2 **42/42 PASS**, CRUD **43/43 đúng expect**,
0 console error trên mọi route đã quét, design system Playful Geometric + Be Vietnam Pro đạt 100%.

---

## 1. ĐÃ LÀM (theo 7 mục tiêu)

### 1.1 Quét toàn bộ codebase + database (mục tiêu 1)
- **Static audit backend** bởi subagent chuyên biệt → `subreports/backend-static-audit.md`: 82 findings (F01–F82) có mã nguồn, failure-mode, đề xuất fix; mỗi P1 đều được xác minh bằng live probe trước khi vá (tránh false-positive — 2 finding bị retract có bằng chứng: DeckController /my đã fix từ trước, AdminService clamp đã tồn tại; F66 self-inject CSS không còn trong code).
- **DB live audit** qua `sqlcmd` trong container `engflow-sqlserver` → `subreports/db-audit.md` (~1.070 dòng): inventory 24 bảng / 46.705 rows, dung lượng per-table, fragmentation, index usage stats, FK integrity (0 orphan/18 FK), data-quality (5.434 correct_answer rỗng = 12,42%; 9/367 LISTENING thiếu audio — số liệu AGENTS.md "89/449" là STALE, đã đo lại), Redis 49 keys 100% có TTL, `msdb.backupset` = 0 rows (**DB chưa từng được backup**).
- **Design audit** bằng Playwright MCP: computed-style token spot-check trên mọi trang chính, mobile 375px, console scan → `subreports/design-audit.md`.

### 1.2 Test backend bằng chạy TOÀN BỘ API (mục tiêu 2)
- Sweep 53 endpoint × 3 vai trò (noauth / user / admin) — `sweep/v7-sweep-r1.ps1`, `r2.ps1`, và vòng khép kín `r4.ps1` (token tự refresh vì JWT ~15 phút).
- CRUD qua HTTP thật — `sweep/v7-crud-r3.ps1`: Lesson create→read→update→delete cascade; Exercise CRUD + validation; Speaking-prompt CRUD + null-guard; Deck CRUD + owner-check (admin khác owner vẫn bị 400 đúng); Register 201/dup 409/sai format 400; forgot-password anti-enumeration (200 cả 2 nhánh); streak current/history/noauth-401; search & sort (q, level, size clamp, page âm, XSS payload); grade 100%; attempts-list.
- Cross-interaction UI per-API: mỗi nhóm API trên đều được xác nhận thêm bằng thao tác trình browser thật (login form, grade flow, deck tabs, speaking playback, video shadowing).

### 1.3 DB Docker + tối ưu hiệu năng (mục tiêu 3)
- **F51**: PK `exercises` fragmentation 95,11% → REBUILD → 0,70%; pages 2.330 → 1.277; clustered scan 19ms → 1ms (đo trước/sau bằng `sys.dm_db_index_physical_stats` + `SET STATISTICS IO`).
- **3 index mới tạo live + verify `sys.indexes`**: `IX_uvp_due` (srs-due 67ms → 37ms), `IX_exercises_lesson_type_diff_order` (admin filter 43ms), `IX_decks_owner_name`.
- **C-03a**: xóa dead method `getAllExercises()` — `findAll()` 43.7k rows = 57,8% tổng logical reads của app (đo qua `sys.dm_exec_query_stats`); grep 2 phía trước khi xóa đúng boundary P5.
- **F41/B-06**: `correct_answer` nvarchar(500)→2000 (SQL + entity khớp để `ddl-auto=update` không co lại).
- Endpoint timing sau tối ưu: chậm nhất 204ms (lesson q XSS probe), đa số <60ms; không còn query nào >0,25s.

### 1.4 Verify frontend browser thật + design system (mục tiêu 4)
- Playwright MCP trên `localhost:5173`: login UI → redirect → token localStorage; computed `fontFamily` = **"Be Vietnam Pro" trên MỌI element** (0 non-BVP — constitution P6 đạt); token spot-check `app-btn--primary` = border 2px + radius 9999px + shadow `rgb(30,41,59) 4px 4px 0px` đúng Playful Geometric; tab Nội dung/Bài tập/Lịch sử; grade 5 bài qua UI → 5×POST 200; decks Cộng đồng 10 + của tôi 3; speaking playback **audio phát thật** (currentTime chạy, readyState 4); video /videos/1 embed YouTube + transcript + quiz + shadowing; flashcard SRS 10 từ; leaderboard; search "travel"; admin dashboard 75 users/1.471 lessons; mobile 375px không horizontal-scroll; **0 console error** mọi route quét.
- Screenshots bằng chứng (repo root): `audit-v7-home.png`, `audit-v7-lesson-445.png`, `audit-v7-deck-10006.png`, `audit-v7-video-1.png`, `audit-v7-admin-mobile-375.png`.
- `vite build` production: **1.650 modules, 4,73s, 0 error**.

### 1.5 Finding → vá (mục tiêu 5) — xem §2

### 1.6 SpecKit workflow (mục tiêu 6)
`constitution` (đã có, P1–P8 đối chiếu từng fix) → `spec.md` → `clarify.md` (G8–G11) →
`checklist.md` (5 nhóm A–E, all-checked) → `plan.md` (constitution check table + design decisions)
→ `tasks.md` (T001–T051, phases) → implement (3 wave) → **đóng bằng REPORT này**.
`analyze`/`converge`/`taskstoissues` không chạy separate — lý do ở §3.

### 1.7 Vòng khép kín lần 2 (mục tiêu 7)
- Rebuild `docker compose up -d --build backend` với toàn bộ wave-2/3.
- `sweep/v7-sweep-r4.ps1`: 33 probe cũ + 9 probe mới chuyên re-check fix → **42/42 PASS**; kèm SQL row-check F56 (`sections_445_after = 0`).
- CRUD r3 rerun: **43/43** đúng expect, dữ liệu test tự dọn (lesson 101920 deleted 204, prompt deleted, deck deleted).
- Frontend route-scan lần 2 + 5 screenshots + console-error scan.

---

## 2. ĐÃ FIX + CÁCH FIX (82 findings; P1/P2 vá hết, P3 cosmetic chọn lọc)

### Wave 1 — security/perf nền
| ID | Lỗ hổng | Cách fix | Verify |
|---|---|---|---|
| F34 | `GET /api/decks/my` guest → 500 | 401 rõ ràng + test | `DeckControllerMyDecksTest` 3/3 |
| F41 | correct_answer 500 chars cắt dữ liệu | nvarchar(2000) SQL+entity | live |
| F47 | rule permitAll cho module shop đã xóa | gỡ rule chết | compile+review |
| F51 | PK exercises frag 95% | REBUILD ONLINE | 0,70% sau đo |
| C-03a | findAll() 43,7k rows dead code | xóa method, controller đã paginated | grep 2 phía |
| M-1/2/3 | thiếu index nóng | 3 index mới | sys.indexes + timing |

### Wave 2 — P1 security/data (fix nặng nhất của v7)
| ID | Lỗ hổng | Cách fix | Verify |
|---|---|---|---|
| **F54** | `GET /api/lessons/*/exercises/attempts**` không token → **500 NPE**, và rule permitAll `/api/lessons/**` che mất auth | **2 lớp**: rule GET cụ thể `.authenticated()` đặt TRƯỚC permitAll (Spring first-match-wins — root cause giải thích trong comment) + guard `isRealUser()` trong controller (null/!authed/`AnonymousAuthenticationToken`/name "anonymousUser" → 401) | test guest→401 ×2; live 401; user→200 |
| **F55** | Media proxy `/api/v1/media/**` permitAll → mọi người đọc được recording của người khác nếu biết key; mà `<audio>` không gửi được header Authorization | **Server-signed expiring URLs**: `MediaSigner` HMAC-SHA256 (`exp`+`sig`, TTL 6h, signing key = `jwt.secret`), 3 service paths trả URL đã ký (`speaking videoUrl`, `mediaUrl`, video-attempt `mediaUrl`); proxy verify ticket **hoặc** fallback owner-auth (query 2 repo `existsByMediaObjectKeyAndUserId`) cho key cũ chưa ký; **không** dùng JWT-in-query (rò rỉ vào access log) | signed→200 `audio/wav` 563KB; tamper→403; expired→403; unsigned→403; playback thật trong admin UI ✓ |
| **F56** | Public `GET /lessons/{id}/structure` có @Transactional **INSERT** section/block — guest ghi DB hàng loạt (write-amplification + DoS face) | GET thành **read-only**, trả "section ảo" (title "Nội dung bài học", block TEXT = content, id null); mutation chỉ qua admin endpoint có authz | test 0 rows sau GET; live DELETE→GET→COUNT=0; UI render 6 blocks bình thường |
| **F53** | Đáp án hiển thị công khai cạnh bài tập (`ANSWER` headings) trên cùng payload sạch cho learner | `wrapAnswerSections()` (Jsoup): heading ĐÁP ÁN/ANSWER + siblings tới heading kế tiếp → `<details><summary>` (mặc định đóng); wire vào `getCleanContent`; DOMPurify FE đã whitelist `details/summary` | live 6 `<details>`; browser: 6 mục ANSWER đóng, 0 console error |
| **F57** | Webhook SEPay **không đối chiếu số tiền** — chuyển thiếu 1đ vẫn kích Premium | `processSePayTransaction`: `amount < pending.amount` → reject + log (overpayment vẫn accept theo thông lệ, có log) | unit test `processSePayTransaction_underpayment_rejected` + 33 test cũ pass |
| **F58** | Seed accounts mật khẩu `password123` fallback không dấu vết | giữ dev-fixture (AGENTS.md công bố), thêm **log WARN SECURITY** khi env chưa set + khuyến nghị deploy ở §4 | compile + code review |
| **F60** | `POST /api/ai/save-vocab` bypass validation: word trống → 500 DB error, meaning trống → lưu rác bảng chung; batch vô hạn; không tốn quota | `@Valid List<VocabularyRequest>`; empty→400; **>50→400**; quota check `hasAiGenerationQuota`→403 + `consumeAiGenerationQuota` khi có user | 5/5 validation tests pass (2 mới) |
| **F61** | Rate-limit chỉ có 3 bucket (login 20/mail 5/global 100) — AI pipeline, upload, tạo đơn payment chịu cùng 100/phút | thêm bucket theo prefix+method: `:ai` 10/phút (`/api/ai/*`), `:upload` 15/phút (POST uploads/submissions/video-attempts — GET không bị siết), `:order` 10/phút (create-order, /api/premium) | live 13× authed AI → 429 từ request 11 ✓ |
| **F62** | Enum query rác (`level=FOO`) → `IllegalArgumentException` rơi vào catch-all → **500** (6 sites: AdminService, ExerciseService, LessonStructure/SnapshotService, AdminAiExerciseController) | `@ExceptionHandler(IllegalArgumentException)` → **400 ProblemDetail**, detail tiếng Việt | live 400 "Tham số 'level'…" + valid `ELEMENTARY`→200; sweep 400 ✓ |

### Wave 3 — design/frontend (P3 chọn lọc, không phá functionality)
| ID | Vấn đề | Cách fix | Verify (browser) |
|---|---|---|---|
| F63 | Donut + bars AdminDashboard dùng `hsl(var(--accent))` trong khi token thật là `--geo-*` (hex) → **conic-gradient fail, vẽ đen** | đổi `var(--geo-accent)`…; donut `rgba(30,41,59,.12)` cho phần còn lại (hex token không dùng được alpha-slash) | computed `rgb(139,92,246)` ✓ |
| F64 | Mobile pop-shadow patch v6 chỉ phủ base state; 20 lượt `hover:/active:shadow-pop-*` lọt lưới giữ 4–8px | selectors `[class*="hover:shadow-pop"]:hover` / `[class*="active:shadow-pop"]:active` trong `@media (max-width:768px)` | CSS verified |
| F65 | `prefers-reduced-motion` đặt transition 0ms nhưng hover-translate vẫn **nhảy tức thì** | `[class*="hover:-translate"]…{transform:none!important}` + `scroll-behavior:auto` | CSS verified |
| F66 | (design report claim App.vue self-inject `<link href="/src/assets/main.css">`) | **FALSE POSITIVE** — grep 0 match, block đã bị xóa từ audit-v5 | retract có bằng chứng |
| F67 | Admin không dùng được trên mobile: sidebar `w-72` cứng + layout `h-screen overflow-hidden`, 375px không còn chỗ cho content, không có cách mở menu | off-canvas drawer: `max-lg:fixed` + translate theo `sidebarOpen`, overlay click-to-close, nút hamburger `lg:hidden` có `aria-label`/`aria-expanded`, auto-close khi đổi route (`watch route.fullPath`) | DOM test 375px: closed x=-288 → open x=0 → close ✓, auto-close ✓, burger ẩn trên desktop ✓ |
| F68 | 4 bảng admin columns >375px, container `overflow-hidden` cắt nội dung | `overflow-x-auto` trên Table Container AdminUsers/Lessons/Exercises/VideoLessons | computed `overflow-x: auto` ✓ |
| F69 | Font URL nạp weight 800 = **0 lượt dùng** (đo grep count: 400:1605, 500:344, 600:15, 700:287, 900:292, italic:9, 800:0) | bỏ `0,800`; giữ italic (lesson-html dùng) → URL `wght@400;500;600;700;900`… italic 400 giữ trong subset nhỏ | index.html |
| F70 | Chữ trạng thái dùng sai token: error hồng `text-secondary`, pass xanh lá `text-quaternary` #34D399 trên nền trắng **<4.5:1** | 8 sites → `text-danger`/`text-success` (#E11D48/#059669 đạt contrast); PremiumCheckout message error `bg-danger/10 text-danger`; orderCode highlight → `text-accent` (hồng = decorative OK trên strong) | vitest 79/79 không vỡ |

**Kèm 13 regression tests mới** trong suite (314 → **327**): `AuditV7SecurityWaveTest` (10),
PaymentService underpayment (1), AiVocabSaveVocab (2), DeckControllerMyDecks (3, wave-1).
3 test files SpeakingSubmissionService cập nhật constructor theo field mới `MediaSigner`.

---

## 3. CHƯA LÀM (có chủ đích, kèm lý do)

1. **Drop `content_original` (~69MB = 34,7% DB) + 4 bảng `*_bak*`** — ràng buộc v6 "không xóa" + **DB chưa từng backup** (`msdb.backupset`=0). DROP khi chưa backup là risk mất dữ liệu không đảo được → chỉ để khuyến nghị §4.
2. **Backfill 5.434 `correct_answer` rỗng (12,42% exercises)** — cần data source/nguồn đáp án thật, không phải code fix; thuộc việc của content owner.
3. **Chuẩn hóa timezone naive-local writes** (backend UTC+7 vs SQL UTC; `SYSDATETIME()` UTC vs `LocalDateTime.now()` local) — đổi systemic, ảnh hưởng lịch sử streak/điểm danh quá khứ, cần user duyệt phương án; hiện là nguồn "future date" false-positive khi test.
4. **SpecKit `analyze`/`converge`/`taskstoissues` standalone pass** — nội dung tương đương đã được cover: plan.md có constitution-check per-fix, tasks.md đánh dấu mọi task + follow-up có lý do, checklist all-green đối chiếu bằng chứng. Chạy thêm 3 skill nữa chỉ re-derive cùng dữ kiện; ưu tiên dành cho REPORT + vòng khép kín (mục 7 của yêu cầu).
5. **`srs_interval` outlier 1914 ngày** (1 dòng do clock-drift) — data fix một dòng, không khẩn cấp, đưa vào §4.
6. **UNCERTIFIED 2 điểm static audit**: `JacksonConfig` @Primary ObjectMapper trên Boot4/Jackson3 và `GameService` answers-missing branch — cần runtime repro chuyên sâu hơn, không đủ bằng chứng để vá; ghi nhận để vòng sau.
7. **E2E payment qua SEPay sandbox thật** — webhook đã verify bằng unit test HMAC + amount-mismatch; luồng QR live cần tài khoản merchant, ngoài scope local audit.

---

## 4. KHUYẾN NGHỊ CẦN USER QUYẾT (không tự làm)

1. **Backup trước**: thiết lập SQL Agent job / `BACKUP DATABASE`MaintenancePlan — DB chưa từng backup lần nào. Sau đó mới cân nhắc: `ALTER TABLE exercises DROP COLUMN content_original` (giữ bản snapshot) + drop 4 bảng `*_bak*` → giảm ~35% dung lượng.
2. **Đổi `ddl-auto=update` → `validate`** ở môi trường thật; `update` âm thầm thêm cột nhưng không bao giờ xóa/guard rename (đúng quy tắc AGENTS.md: đổi schema bằng SQL trực tiếp + entity).
3. **Timezone**: thống nhất 1 hệ (khuyến nghị: mọi `LocalDateTime.now()` trong writes → `Instant.now()` + cột `datetime2`, hoặc set cả container + SQL về cùng TZ); trước mắt **không dùng test "future date" làm pass/fail** cho tới khi sửa (đã biết systematic false-positive).4. **DEFAULT_USER_PASSWORD / DEFAULT_ADMIN_PASSWORD**: set trong `.env` khi deploy (seed dev nên giữ cho local; log WARN đã thêm để bắt quên).
5. **JWT access token ~15 phút không có refresh flow** phía FE (logout thẳng khi 401 'hết hạn') → cân nhắc refresh-token endpoint để không mất phiên giữa bài thi.
6. `idx_prompts_published/level`, `idx_lessons_level`, `deck_words_deck` unused từ khi tạo (0 seeks) → có thể drop sau backup; `idx_exercises_lesson_type_order` **không** drop (đang sống).
7. Sửa số liệu AGENTS.md "89/449 listening thiếu audio" → thực đo **9/367** (đã vá câu chữ? chưa — thuộc docs, để user xác nhận).

## 5. SKILLS ĐÃ NẠP / DÙNG

`java-coding-standards`, `java-springboot` (chuẩn Spring/exception/naming khi vá), `full-output-enforcement` (không placeholder trong fix), `karpathy-guidelines` (surgical changes, surface assumptions), `generate-test-cases` + `generate-tests` (13 regression mới), `speckit-workflow` + `speckit-specify/clarify/checklist/plan/tasks` (artifacts §1.6), `design-taste-frontend` + `redesign-existing-projects` (design audit + wave-3 không phá hệ thống hiện có), `accessibility` (contrast text-state, aria-label/aria-expanded burger, prefers-reduced-motion), `seo` (kiểm meta/JSON-LD index.html còn nguyên khi sửa font URL), `prompt-master` (đánh giá G8 prompt-vs-constitution), `superpower-verification-before-completion` (mọi claim kèm lệnh + output), `superpower-systematic-debugging` (root-cause rule-order Spring Security, TZ drift), `addyosmani-browser-testing-with-devtools` pattern (DOM-assert > screenshot), `superpower-dispatching-parallel-agents` (3 subagent audit song song).

## 6. BẰNG CHỨNG / FILES

- Logs: `audit-v7-backend-test5/6.log` (**327/0 BUILD SUCCESS**), `audit-v7-wave-test.log` (10/10), `audit-v7-wave-test2.log` (5+34), vitest 79/79 inline, `vite build` 4.73s.
- Sweeps: `sweep/v7-sweep-r4.json` (42/42), CRUD r3 log (43 steps), outputs `sweep/*.json`.
- Artifacts: `.specify/specs/audit-v7-full/{spec,clarify,plan,tasks,checklist,db-audit,design-audit}.md` + `subreports/{backend-static-audit,db-audit,design-audit}.md`.
- Screenshots bằng chứng (thư mục `audit-v7-shots/`): `audit-v7-home.png`, `audit-v7-lesson-445.png`, `audit-v7-deck-10006.png`, `audit-v7-video-1.png`, `audit-v7-admin-mobile-375.png`.
- Source changes: ~15 files backend (SecurityConfig, LessonExerciseController, MediaSigner+, MediaProxyController, SpeakingSubmissionService, VideoLessonService, VideoLessonController, LessonStructureService, LessonContentService+, ExerciseService, PaymentService, AiVocabController, GlobalExceptionHandler, RateLimitFilter, DatabaseSeeder, 2 repositories) + ~12 files frontend (AdminDashboard, AdminLayout, 4 admin tables, Register, AiVocabGenerator, DeckCreate, PremiumCheckout, LessonPreview, TypingGame, design-system.css, index.html) + 5 test files.
