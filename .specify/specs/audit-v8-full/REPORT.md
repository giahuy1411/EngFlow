# REPORT — audit-v8-full (kiểm toán toàn diện vòng 1–3, EngFlow)

**Ngày**: 2026-09-14, cập nhật vòng 3 **2026-09-16**, cập nhật vòng audit-v8-full **2026-09-16** (2 lượt: F90–F94, rồi Task 1–16) · **Phạm vi**: toàn bộ codebase + DB SQL Server trong Docker + API thật + browser thật

**Kết luận**: 7 mục tiêu của yêu cầu đều hoàn thành có bằng chứng. Baseline backend **332 → 378 tests xanh**
(+46 regression mới qua 4 vòng), frontend **79 → 89 tests (18 files)**, `vite build` OK (entry **176.80 kB** / gzip 67.39).
Quét **144 mapping / 26 controller** (sinh bằng script, tái lập được — xem §2b CLAIM-COUNT),
**499 probe HTTP chủ động** + **228 lượt route runtime** (38 route × 2 viewport × 3 role; `/admin` chỉ redirect) + **60 tổ hợp
route×viewport** (12 route × 5 viewport: 360/768/1280/1440/1920) + **59 case adversarial** + **36 screenshot** → **0 console error**,
font Be Vietnam Pro 100% (**7 685 element, 0 element lệch font**).

**Đếm lỗi chính xác** (không gộp nhóm khác bản chất):

| Nhóm | Số | Chi tiết |
|---|---:|---|
| Lỗi thật đã fix (vòng 1–3) | **12** | F81–F89 (9) + UI-1/UI-2/UI-3 (3) — §2 |
| Lỗi thật đã fix (vòng audit-v8-full) | **4** | F91 (bucket `:ai`), F92 (bucket `:upload`), F93 (timeout→504), F94 (error shape) — §2b |
| Nghi vấn đã **bác bỏ** bằng đo | **1** | F90 (thứ tự filter) — `CLOSED-NOT-A-BUG`, **không** tính là lỗi |
| Lỗi artifact/harness của chính cuộc audit | **6** | HARNESS-FALSE-PASS-3, HARNESS-NO-CLEANUP, PAYMENT-SWEEP-DELTA, MISSING-EVIDENCE-LAYOUT-6, STALE-P8-EVIDENCE, **HARNESS-TOKEN-ONLY-SEED** — §6 |
| Verification pass (không có gì để sửa) | **1** | PLANB-VERIFY (Be Vietnam Pro) |

⇒ **16 lỗi ứng dụng thật đã fix tận gốc** + **1 nghi vấn bác bỏ** + **6 lỗi của chính bộ audit đã fix**.
Trong 16 lỗi có **1 chuỗi stored-XSS → đánh cắp JWT đã chứng minh end-to-end trước khi sửa** (F81);
F88 = admin không lưu được bài học video, F89 = bài NHÁP lộ công khai (cả hai tìm ở vòng 3).
*(Bản trước ghi "8 lỗi" trong khi bảng §2 đã liệt kê 9 dòng — đếm sai; các bản sau ghi "17 lỗi thật" khi
đã gộp cả F90 vốn không phải lỗi. Bảng trên là con số đã đối chiếu `findings.json` + §2.)*

**Lỗi harness mới nhất (vòng 3) — HARNESS-TOKEN-ONLY-SEED**: harness seed chỉ `localStorage.token`
mà không seed `localStorage.user`, nên `auth.isAdmin` là `undefined` và guard đá `/admin/*` về `/`.
Lần chạy screenshot đầu báo **36/36 PASS** trong khi **9 ảnh admin thật ra là ảnh trang chủ**. Phạm vi
**đã đo bằng A/B**, không suy đoán: chỉ các harness mở **context mới cho từng case** bị ảnh hưởng
(`p17`, `p18`); `routes-all.js` và `design-v2.js` đi tuần tự trong 1 context nên **không** bị (app tự
hydrate `user` qua `/api/auth/me`). Chi tiết §6.1b.

---

## 1. ĐÃ LÀM (theo từng mục tiêu)

### 1.1 Quét toàn bộ codebase + database
- Enumerate mọi `@*Mapping` trong 26 controller → **139 annotation / 97 handler**; dựng sweep mới phủ hết (kể cả route alias
  `video-prompts`/`speaking-prompts`, `media/**`, 10 endpoint video AI).
- Static pass: `SecurityConfig` (thứ tự rule), `RateLimitFilter` (`getClientIP`), `GameController`, `LessonStructureController`,
  `LessonSubmissionService`, `AiExerciseService`, `AiVocabController`, `DictionaryService`, `MediaProxyController`.
- DB (`sqlcmd` trong `engflow-sqlserver`): 4.7 GB / 2 file, 24 bảng; lessons 1471, exercises 43 737, users 76, vocabulary 127;
  **0 orphan** (exercises→lessons, snapshots→lessons); `correct_answer` rỗng **4848** và LISTENING thiếu audio **9/367** —
  khớp chính xác AGENTS.md (xác nhận số liệu audit-v7 không stale).

### 1.2 Test backend bằng CHẠY TOÀN BỘ API + tương tác UI tương ứng
| Vòng | Nội dung | Kết quả |
|---|---|---|
| reused v7 | 42 endpoint × 3 vai trò | **42/42**, slowest 374 ms |
| reused v7 | CRUD 43 bước | **43/43** |
| P1 (mới) | public reads, media-proxy IDOR (ticket/bad-sig/traversal), auth surface, **role matrix** 16 admin endpoint × user/noauth, enum/validation | **124/124** (vòng cuối) |
| P2 (mới) | Lesson/Exercise/Vocabulary/Speaking-prompt/Deck CRUD + cascade delete, attempts, SRS/flashcard/game submit **UI-shape**, structure section/block CRUD, **snapshot create→list→restore** | **61/61**, mọi id thật |
| P3a (mới, vòng 3 chạy lại **36/36**) | AI: generate-async 202 + poll status, generate-all, validate, backfill dry-run deterministic, generate-vocab/enrich-word (Ollama thật), prompt ai-generate/-full, translate-transcript, fetch-youtube | 36 probes → **1 lỗi thật** (F86) + 2 vấn đề chất lượng (F84/F85) → đã fix |
| P3b (mới) | multipart uploads (speaking/video/lesson/admin/audio-upload), **Whisper + 3b assess 14.6 s**, shadowing **ai-grade 6.8 s**, manual grade, SePay **create-order → QR**, avatar | **19/19** trong contract; 2 phát hiện (F83 bucket chết; lệch sửa kết luận Cloudinary — xem §3.1) |
| P4a (mới) | đổi mật khẩu (sai / ngắn / đúng), forgot → OTP trong Redis → reset (OTP **consumed** sau dùng), avatar PUT, **6 admin user-toggle** × role matrix (admin/user/noauth/unknown) | **18/18** |
| P4b → P4c (mới) | speaking manual-grade validation (`score 11`, `7.55`, blank feedback → 400; valid → 200 `GRADED 8.5`; non-admin → 403) rồi **video-lesson CRUD** create 201 → public detail → update → delete → 404 | **16/16** (vòng 3: harness sửa expectation — enum đúng là `PRE_INTERMEDIATE`, update cần ≥ 2 dòng, `transcript` phải có mặt) ; thêm 404-cho-nháp (F89) |
| P4d → P4e (mới) | multipart `video-lessons/upload`: meta+SRT và meta+transcriptText → **201** (SRT parse đúng 2 dòng, `youtube_video_id` extract từ URL), YouTube lỗi → 400, user → 403 | **6/6** (vòng 3) + 1 check `non-admin (user token)` = 7 case, 0 fail; harness dọn sạch `leftover-ZZ=0` |
| **P5 (vòng 3)** | **Đăng ký/Đăng nhập/lockout** (register 201 + token, trùng email/username 409, email sai/short pw/short username 400, khoá sau 5 lần sai 15'); **streak** boundary trên DB thật; **tìm kiếm/sắp xếp** (lessons `q`+`level`, vocab search, admin `q`/type/difficulty); tái xác minh F81/F83/F86 | **95 probe / 60 assert — 0 fail** (`sweep/v8/p5.json`) |
| **V3-UI (vòng 3)** | browser thật: đăng ký qua form `/register` (201 + token + redirect), lịch streak trong `/profile` khớp `/api/streak/current`, ô tìm kiếm `/lessons` (request `q=` + mọi tiêu đề hiển thị khớp), **ghi âm THẬT bằng fake mic → upload blob webm → 200 + assess 200 + row DB** | **30 assert — 0 fail, 0 console error** (`sweep/v8/ui/v3ui.json`) |
- AI chạy **local thật** (`qwen2.5:1.5b` exercises, `qwen2.5:3b` rubric qua `AI_SPEAKING_OLLAMA_MODEL`), đã kiểm
  `host.docker.internal:11434` thông từ trong container.
- Mọi thao tác mutate tạo **lesson/prompt/deck/vocab tạm** rồi xóa; dữ liệu seed không bị chạm (đã kiểm `created_at`).

### 1.3 Database trong Docker + tối ưu hiệu năng (đo trước/sau — P5)
| Việc | Trước | Sau |
|---|---|---|
| `ALTER INDEX ALL ON exercises REBUILD` (PK) | 13.95 % frag, 1369 pages | **0.86 %, 1283 pages** (43 737 rows nguyên vẹn) |
| `idx_exercises_lesson_type_order` | **46.19 %**, 565 pages | **2.49 %, 441 pages** |
| `GET /api/admin/exercises` (F87) | 367 ms, **95 053** logical reads | **30.6 ms, 27 293** reads (−12×) |
| 1 trang 20 rows: kéo Lesson entity vs projection | **320 lob reads, 32 ms** | **0 lob reads, 1 ms** |
| 5 endpoint listing (lessons/decks/vocab/leaderboard/attempts) | — | **60–105 ms**, tất cả < 300 ms |
| `GET /api/admin/exercises?q=the&size=20` (vòng 3, LIKE `%…%`) | — | **184–193 ms** (min 3 lần) — không index hoá được leading wildcard |
| `sp_columns`/`sp_fkeys` metadata (JDBC) | 84 ms/5858 reads | ghi nhận, không đổi (driver) |
- Không viết migration (Hibernate `ddl-auto=update`); index tạo/thu bằng SQL trực tiếp + entity khớp — đúng boundary.
- Đo lại 2026-09-16 (sau khi container restart): `GET /api/admin/exercises` **không filter** vẫn < 100 ms; bản **có `q=`** (LIKE `%…%` trên 43 737 row)
  là **184–193 ms** (min 3 lần) — chậm hơn bản không filter nhưng vẫn dưới 300 ms và không có index nào cứu được leading-wildcard; ghi nhận là đặc tính, không phải regression.
- Index chưa dùng (`user_seeks=0`) được liệt kê nhưng **không xóa**: `sys.dm_db_index_usage_stats` reset khi restart, không đủ bằng chứng.

### 1.4 Frontend bằng browser thật + verify design system
- Harness `sweep/v8/ui/` (Chromium qua `playwright-core`), đăng nhập **bằng form thật** (Pinia + localStorage).
- **39 route × (admin 1440 px, admin 375 px) + 14 route guest** = 92 lượt: **0 console error, 0 API ≥400**, `#app` mount 100 %.
- **Typography**: `fontFamily` = **Be Vietnam Pro trên mọi element được sample** (785 element/route) → **0 font lạ**;
  `h1` weight **900** trên 11/11 route có h1; URL font chỉ nạp 400;500;600;700;900 + italic (800 đã bỏ từ v7).
- **Playful Geometric**: border 2 px, hard `pop-*` shadow (shadowed 1→66/button mỗi trang), radius, palette `--geo-*` đủ 10 token;
  signature check: polka ✓ grid ✓ squiggle ✓ marquee ✓ blob ✓ wiggle ✓ star ✓ bounce `cubic-bezier(0.34,1.56,0.64,1)` ✓ — **stripe ✗ (khoảng trống của prompt gốc — G4 trong `prompt-gap.md`; design system hiện có không dùng pattern stripe)**.
- **A11y**: skip-link có thật, `btnNoName=0`, contrast token (v7 F70) còn nguyên, `prefers-reduced-motion` → `transition-duration: 0s`,
  **0 target < 24 px** (WCAG 2.5.8). 2 lỗi thật về `alt` (WCAG 1.1.1): **ảnh trong lesson content** → fix ở vòng 1 (UI-2),
  **2 ảnh preview trong admin** → fix ở vòng 3 (UI-3). Vòng 3 cũng phát hiện metric `imgNoAlt` của harness là false positive với `alt=""` (xem §2 UI-3).
- **Responsive**: lỗi tràn ngang thật (không phải artifact): header khi đăng nhập tràn **13 px @1440, 101 px @1280, 65 px @1366**
  (đo raw `scrollWidth`, đã loại scrollbar 15 px) → fix band nén 1280–1535 px → **0 overflow ở 360→1920 px cả guest lẫn admin**.
- Luồng nghiệp vụ bấm thật: search dictionary (`/api/vocabulary/dictionary/travel` 200 + IPA hiện ra), lesson tabs,
  quiz game click, flashcard SRS (`/api/flashcards/review` 200), speaking history → media signed 200, admin filter/builder,
  logout (token cleared), **video /videos/1**: YouTube iframe + 113 dòng phụ đề + tab SHADOWING/QUIZ, 0 error.
- **AI generator qua UI thật**: `POST /api/ai/generate-vocab` **200 trong 12.6 s**, từ vựng hiển thị đúng.
- **Vòng 3 chạy lại toàn bộ 92 lượt route trên bản dựng hiện tại**: `routes.js admin` (39 route × 1440 px), `routes.js admin mobile` (39 × 375 px), `routes.js guest` (14) → **PROBLEM=0**, 0 font ngoài Be Vietnam Pro, **0 tràn ngang**;
  `design.js` 13 trang: `badFont=0`, `imgNoAlt=0`, `btnNoName=0`, `noFocus=0`, `h1` weight **900** trên 11/11, `prefers-reduced-motion` → `transition-duration: 0s`,
  **target < 24 px (WCAG 2.5.8 AA) = 0** ở cả desktop lẫn 375 px (ngưỡng AAA 44 px còn 138 desktop / 7 mobile — ghi nhận, không phải vi phạm).
- **Vòng 3 bổ sung 4 luồng chưa từng bấm thật** (xem §1.7): đăng ký bằng form → 201 + token + redirect `/lessons`;
  lịch streak ở `/profile` hiện đúng chuỗi server (1 ô `ring-accent` = hôm nay, nhãn `16/9/2026: đã học (hôm nay)`);
  ô tìm kiếm `/lessons` phát request `page=0&size=12&q=Grammar` và mọi tiêu đề hiển thị đều chứa từ khóa;
  **ghi âm thật** bằng `--use-fake-device-for-media-stream` (MediaRecorder tạo blob webm ~48 kB) → `POST /api/v1/speaking-submissions/upload` **200** → `/{id}/assess` **200** → row `speaking_submissions` status `FAILED` (mic giả = im lặng, Whisper trả text rỗng — đúng thiết kế, xem §3.6).

### 1.5 CRUD + AI
- CRUD: tạo→đọc→sửa→xóa có cascade và kiểm tra quyền theo owner (`D3/D4/D5` admin khác owner vẫn 400 như v7),
  snapshot restore thật (`snapshotId=30020` → 200), attempt detail theo owner.
- AI: 6 pipeline surface (exercises async/sync/batch-validate, vocab generate/enrich/save, prompt gen, translate, shadowing grade,
  backfill) đều probe bằng payload đúng DTO; output được inspect thật (2 MC câu hỏi, 30 rows khi count=3 → phát hiện F84).

### 1.6 Kiểm tra lỗ hổng prompt + viết lại (prompt-master)
→ `prompt-gap.md`: 10 gaps (G1–G10) + rewritten prompt. Điểm chính: font Be Vietnam Pro **mâu thuẫn nội tại** với
typography scale 1.25; thiếu breakpoint/mobile; `Lucide React` ≠ stack Vue; **không có acceptance criteria**;
thiếu màu warning/danger; thiếu rule upload/resource (chuỗi XSS nằm ngay trong vùng prompt mô tả "icon enclosed in shapes").

### 1.7 Chạy lại toàn bộ ở mức toàn diện hơn (vòng 2) + cleanup
- Vòng 2 chạy **trên bản dựng cuối** (đã rebuild container 5 lần): `354/354`, `82/82`, P1 124/124, P2 61/61,
  XSS PoC safe, 0 overflow, dup-option bị từ chối, order bucket 10 → 429, `/generate-all` count=5 → **đúng 5** và đa dạng type.
- **Vòng 3 lần 1 (2026-09-16), không sửa `src/`**: `354/354` backend (fresh run), `82/82` frontend (17 files),
  `vite build` entry **176.68 kB / gzip 67.34 kB** (= baseline); `p5.js` **95 probe/60 assert 0 fail**; `ui/v3.js` **30 assert 0 fail, 0 console error**;
  parity DB sau vòng 3 khớp **chính xác** baseline (§1.3): 1471/43 737/76/127/28/15/4/126/14/5/38, 0 orphan, 4 `zzprobe*` còn lại là row của phiên 12/09 (không phải của vòng này).
- DB hygiene: mọi row do probe tạo bị xóa đúng phạm vi (lessons/vocab/decks/prompts/payments/submissions/attempts + 2 file upload);
  parity về baseline: 1471/43 737/76/127/14/5/38/28/15/4/126, **0 orphan**, `uploads/` còn đúng 4 file pre-existing.
  (`DELETE` cần `SET QUOTED_IDENTIFIER ON` do filtered index `IX_uvp_due` — đã xử lý.)

- **Vòng 3 lần 2 (2026-09-16, sau khi đã rebuild container)** — đọt này **có sửa `src/main` + `frontend/src`** (khác vòng 3 lần 1):
  - chạy lại **toàn bộ** P1–P5 trên bản dựng cuối: P1 124/124, P2 61/61, P3a 36/36 (generate-all 86 s), P3b 19/19, P4a 18/18, P4b 16/16, P4c 10/10, P4d 6/6, P4e 6/6 (+1 case riêng), burst (đo thô `hist`), **0 fail**;
  - phát hiện + fix **F88** (admin không lưu được bài học video — 3 tầng) và **F89** (bài NHÁP `is_published=false` lộ công khai qua 4 endpoint detail/list con);
  - vòng 3 lần 2 cũng xác minh `openrouter.*` **không phải cloud**: 1 call `ai-generate` sinh đúng **1 dòng mới** trong `%LOCALAPPDATA%\Ollama\server.log` (`POST /v1/chat/completions` 02:07:16, 5.24 s ≈ latency 5.3 s của API) — xem §3.12;
  - dọn dẹp xong: parity về **đúng** baseline, `mc rm` 6 object MinIO mồ côi, xoá 25 row payment probe.
  - **chốt cuối (cùng ngày, sau khi rà docs)**: backend **369/369** (sweep/v8/t-final3.log, exit 0), frontend **85/85** (18 files), `vite build` entry **176.68 kB** (gzip 67.34), parity DB **đúng** baseline (1471/43 737/76/127/28/15/4/126/14/5/38, 0 orphan trừ 4 row `zzprobe*` của phiên 12/09), 0 row `ZZ%` sót lại.

---

## 2. ĐÃ FIX — 12 lỗi thật (F81–F89 + UI-1…UI-3), fix tận gốc + regression test

| ID | Lỗi (cách tìm ra) | Fix tận gốc | Bằng chứng |
|---|---|---|---|
| **F81** | **Stored XSS → đánh cắp JWT.** `uploadFile` + `saveAudioFile` giữ extension do client chọn; `GET /api/resources/**` permitAll, same-origin với SPA. Bất kỳ user đăng nhập nào POST `lesson.html` là chạy JS first-party. | `SafeUploadNames`: (1) allowlist extension lúc **ghi** (ảnh/audio/video + txt/srt/vtt), (2) **pin Content-Type theo tên** lúc **đọc** thay cho `Files.probeContentType`, (3) `Content-Disposition: attachment` cho thứ không hiển thị được. | Trước: Chromium thật lấy `localStorage.token` (227 ký tự) từ `localhost:5173`. Sau: upload `.html/.svg/.js/.xhtml` → **400 "Loại tệp không được hỗ trợ"**; file `.html` còn trên đĩa → `text/plain` + `attachment`, browser từ chối render; `evil.HTML`/`a.cDn`/`a.tar.gz`/`a%00.html` → 400; `rec.webm`/`pic.png`/`notes.txt` → 200. 9 test `AuditV8UploadXssTest` |
| **F82** | `POST /api/games/submit` cast mù → **500 + stack trace** với payload tự chọn (4/6 shape đo được). | Validate kiểu thật (`sessionId` String, `correctAnswers` Number, `answers` List, mỗi phần tử là object) → 400 thông báo tiếng Việt; không đổi hành vi hợp lệ. | 6 shape 500 → **400**; 2 luồng hợp lệ vẫn 200 (count-only `1/10`, UI-shape server-scored). 7 test `GameControllerSubmitTypeTest` |
| **F83** | Bucket rate-limit `:order` **chết** — prefix `/api/payments/create-order`, `/api/premium` không tồn tại (route thật `/api/v1/payment/create-order`). | Matcher → `startsWith("/api/v1/payment")`. | Burst thô trước: 13/13 200, key `:order` không sinh; sau: **10 × 200 rồi 429** + key `rate_limit:…:order` tồn tại. +3 test `RateLimitFilterTest` (4→7) |
| **F84** | `generateAll` không coi `count` là trần; `perType + remainder` **âm** khi `count<5` (đo: `count=3` sinh **30 rows**). | Per-type budget + trim vòng tròn theo type; `Math.max(1, count − 4·perType)`. | `count=5` → **generated=5**, types {MC:2, FB:1, TR:1, LS:1}; DB row khớp 0 sau cleanup |
| **F85** | MC **trùng phương án** vẫn được accept ("most expensive/more expensive/best/**best**") → bài tập mơ hồ, 2 đáp án đúng. | Guard deterministic `MULTIPLE_CHOICE options must be distinct` (case/space-normalised, Unicode-safe). | `POST /ai/validate`: 1 accepted + 1 rejected đúng thông báo; +3 test parse (17→20) |
| **F86** | `POST /api/v1/admin/speaking-prompts/ai-generate` chấp nhận **topic rỗng** → vẫn gọi Ollama, trả nội dung bịa. | Guard 400 `Cần nhập chủ đề` (đồng bộ với `ai-generate-full`). | topic rỗng → **400 trong 0.1 s** (không gọi model); topic thật → 200, 7 s |
| **F87** | Admin exercise list **95 053 logical reads/trang** (`JOIN FETCH e.lesson` kéo `content` + `content_original` cho 20 row). | `JOIN` phẳng + **1 batch `LessonTitle` projection**; `toAdminRow` không initialise lazy proxy; `@Transactional(readOnly=true)`. | 367 ms/95 k reads → **30.6 ms/27 k**; lob 320→0; endpoint 60–105 ms, **20/20 row vẫn có lessonTitle**; 3 test pagination xanh |
| **F88** | **Admin không lưu được bài học video** — 3 tầng chồng nhau: (1) bảng admin **không có nút "Sửa"** (chỉ Xem/Xóa) nên `openForm(lesson)` là code chết; (2) sau khi thêm nút, ô "Link YouTube" (required) **trống** vì form đọc `lesson.youtubeUrl` còn `VideoLessonSummary` chỉ trả `youtubeVideoId` → trình duyệt chặn submit, **không có request nào và không có toast**; (3) khi submit được thì payload mang `transcript: null` → **400 Validation Failed** do `VideoLessonRequest.transcript` có `@NotNull`, trong khi `VideoLessonService.update` có guard "null/rỗng = giữ nguyên phụ đề cũ" (audit-v6 F21) — guard thành **dead code**. | (1) thêm nút `Sửa` (`openForm(l)`); (2) dựng lại URL từ `youtubeVideoId` khi thiếu; (3) bỏ `@NotNull` ở `transcript` — create vẫn bị chặn bởi `validateTranscript` với thông báo rõ ("Transcript cần ít nhất 2 dòng"). | Browser thật (`ui/v4edit.js`, **13/13**): bấm `Sửa` → form có URL `...watch?v=2VeQTuSSiI0`, ô phụ đề trống → `PUT` **200**, payload `{"transcript":null}`, toast "ĐÃ LƯU BÀI HỌC VIDEO", phụ đề **giữ nguyên 2 dòng**, 0 console error. +5 test `AuditV8VideoLessonUpdateTranscriptTest`, +3 test `AdminVideoLessons.test.js` |
| **F89** | **Bài NHÁP (`is_published=false`) đọc được công khai**: `GET /api/lessons/{id}` trả **200 + content đầy đủ** cho guest (6 bài nháp thật trong DB), `GET /api/v1/video-lessons/{id}` trả **200 + transcript**, `GET /api/lessons/{id}/exercises` + `/exercises/content` cũng không kiểm `is_published` — trong khi endpoint **list** đã lọc `isPublished=true`. Guard "nháp" chưa từng tồn tại. | Thêm `assertLessonVisible(lessonId, requesterIsAdmin)` trong `LessonService` (dùng cho `/lessons/{id}`, `/exercises`, `/exercises/content`) + guard tương ứng trong `VideoLessonService.getDetail`; **404** (không phải 403) để không xác nhận sự tồn tại; admin được bỏ qua để preview/review. | `p88live.js` **16/16 trên container đã rebuild**: nháp → guest 404 / student 404 / **admin 200**; bài đã publish → guest 200; xuất bản nháp → guest 200; 4 endpoint đo riêng. +10 test (`AuditV8DraftLessonVisibilityTest` 7, `AuditV8VideoLessonDraftVisibilityTest` 3); P4b/P4c đã được cập nhật expectation theo contract mới |
| **UI-1** | Header khi đăng nhập **tràn ngang 13–101 px** ở 1280–1535 px (8 nav label 978 px + cluster 222 px vào 1225 px khả dụng). | Band nén có chủ đích `@media (min-width:1280px) and (max-width:1535.98px)`: link `0.75rem`, gap 1.5→0.75rem, username 120→84 px. | Đo 360→1920 px × {guest, admin}: **0 overflow**; ≥1536 px trở lại kiểu gốc (linkMaxRight 1255/1447 px = bản gốc) |
| **UI-3** | **2 ảnh xem trước trong trang admin thiếu HẲN attribute `alt`** (`AdminExercises.vue` preview `imageUrl`, `AdminLessonBuilder.vue` preview ảnh khối nội dung) — phát hiện khi chạy lại `design.js` ở vòng 3. | Thêm `alt` (ảnh câu hỏi; ảnh khối nội dung lấy `caption` làm alt, fallback mô tả). Đồng thời **sửa metric sai của harness**: `design.js` cũ dùng `!getAttribute("alt")` nên đếm cả `alt=""` (ảnh trang trí — HỢP LỆ) thành vi phạm → 12 ảnh ở `/lessons`, 5 ở `/videos`, 10 ở `/admin/users` là **false positive**; nay dùng `!hasAttribute("alt")`. | `design.js` sau fix: `imgNoAlt=0` trên 13/13 trang; probe chéo `ui/altcheck.js` (đếm `hasAttribute`) = 0 thiếu attribute; frontend `82/82` (số lúc đó; sau F88 là `85/85`), entry bundle 176.68 kB không đổi. **Verify sống cả 2 ảnh** (`ui/v3b.js`, 17 assert 0 fail): tạo lesson→section→block `IMAGE` có `imageUrl` → builder render `<img alt="ZZ v3 caption ảnh minh hoạ">` (alt lấy từ `caption`); mở modal "Thêm bài tập" + điền URL → `<img alt="Xem trước hình ảnh của câu hỏi">`; dọn lesson tạm, `lesson_sections`/`lesson_blocks` về 10/15 |
| **UI-2** | Ảnh trong lesson content/exercise question **không `alt`** (WCAG 1.1.1) — scraped HTML. | 1 DOMPurify hook `afterSanitizeElements` (singleton → phủ mọi call site), chỉ điền khi thiếu, giữ alt tác giả; nạp **lazy** để không phình entry bundle. | `#0a201717…` giờ `alt="Hình minh họa trong bài học"` trong Chromium; 3 unit test; **entry 205.52 → 176.68 kB (= baseline)**, markdown chunk +0.13 kB |

**Cộng**: 37 test backend mới (332→369: 22 ở vòng 1–2 + **15 của F88/F89**), 6 test frontend mới (79→85: 3 của UI-2/sanitize-a11y + **3 của F88**), 0 test cũ phải bỏ; UI-3 chỉ sửa template admin (không cần test mới, đã verify bằng browser thật + `85/85` vẫn xanh); chỉ 1 test
(`ExerciseServiceAdminPaginationTest`) cập nhật sang contract mới — vẫn assert `lessonTitle`.

---

## 2b. VÒNG audit-v8-full (2026-09-16) — 5 lỗi mới F90–F94 + xác minh Plan B

Vòng này chạy lại từ đầu theo 3 file kế hoạch (`PLAN.md`, audit agent prompt, comprehensive
audit redesign) trên nền baseline đã có. **Không tin số cũ — đo lại hết.** Kết quả: baseline
cũ tái lập được, nhưng tìm thêm **5 lỗi thật** (F91–F94) + **1 kết luận SAI cần bác bỏ** (F90)
+ **1 rò rỉ dữ liệu do harness** (SWEEP-LEAK). Chi tiết máy đọc: `findings.json`.

| ID | Lỗi (cách tìm ra) | Fix tận gốc | Bằng chứng |
|---|---|---|---|
| **F90** | **KHÔNG phải lỗ hổng — bác bỏ.** Nghi vấn: `RateLimitFilter` `@Order(1)` chạy SAU Spring Security (`DEFAULT_FILTER_ORDER = -100`, xác nhận bằng `javap` trên `spring-boot-security-4.0.6.jar`) nên flood ẩn danh không tốn bucket. Đo được đúng hiện tượng: 60× POST `/api/ai/enrich-word` ẩn danh → `{401:60}`, `keys=[]`. | **Không sửa gì.** Bác bỏ bằng đo: quét **toàn bộ 17 surface `permitAll`** của `SecurityConfig` → **0 surface không tốn bucket** (`:auth` tốn ở request 21, `:mail` ở request 6, còn lại `:global`). Request bị security từ chối thì **không thực thi business logic** ⇒ không có tài nguyên nào bị tiêu thụ để bảo vệ. Đổi thứ tự filter là thay đổi rủi ro cao không có lợi ích đo được. | `sweep/v8/p7_ratelimit_order.js`, `p9_f90_scope.js`, `p10_f90_falsify.js` → **0/17 surface bypass**. Ghi lại vì đây là câu hỏi hợp lý, rất dễ bị phân loại nhầm thành HIGH. |
| **F91** | **Bucket `:ai` (10/phút) chỉ khớp tiền tố literal `/api/ai/`** → **12 endpoint THỰC SỰ gọi Ollama** nằm ngoài tiền tố đó rơi hết vào `:global` 100/phút. Đo bằng cách đọc **key Redis thật** sau mỗi prefix (probe path 404, 0 side effect): `/api/admin/exercises/ai/generate*`, `/speaking-prompts/ai-generate*`, `/video-prompts/ai-generate`, `/video-lessons/translate-transcript`, `/fetch-youtube`, `/video-attempts/{id}/ai-grade` → **`:global` (SAI)**. | Danh sách tiền tố + fragment tường minh (`AI_PATH_PREFIXES`/`AI_PATH_FRAGMENTS`), tách hàm `isAiEndpoint(uri, method)` **có guard `METHOD=POST`** để GET status/polling không tiêu budget generation. | `p8_bucket_coverage.js`: 13/13 endpoint AI → **`:ai`** (trước: 1/13). `p8b_ceiling.js`: first429=**11** (đúng trần 10/phút) trên cả 4 endpoint trước đây bị hở; GET status vẫn `:global` first429=101. +6 test `RateLimitFilterTest` |
| **F92** | **Bucket `:upload` (15/phút) không phủ 2 đường upload thật**: `/api/auth/avatar/upload` (proxy Cloudinary) và `/api/v1/admin/video-lessons/upload` (ghi file + parse SRT) → `:global`. | Bổ sung 2 tiền tố vào `UPLOAD_PATH_PREFIXES`; giữ nguyên logic `/submissions` + `/video-attempts`. | `p8`: cả 2 → **`:upload`**; `p8b`: first429=**16** (đúng trần 15/phút). +test `avatarAndVideoLessonUploadsUseTheUploadBucket` |
| **F93** | **`POST /api/ai/enrich-word` trả 500 chung khi Ollama local vượt timeout 30 s** dưới tải GPU (bắt được khi `p3a` chạy song song). Stack gốc: `reactor.core.Exceptions$ReactiveException: TimeoutException: … within 30000ms` tại `AiVocabController.enrichWord:95` (`.block()`). Đo lại nhát 10× **sau đó**: 10/10 **200**, latency 1000–1274 ms ⇒ lỗi **transient do contention**, không phải lỗi code. | `GlobalExceptionHandler`: handler `RuntimeException` dò **cause-chain** tìm `TimeoutException` → **504 Gateway Timeout** + thông báo retryable; mọi lỗi khác giữ 500. Thêm `@ExceptionHandler(TimeoutException)`. Frontend `AiVocabGenerator` đọc fallback `.detail`. | 3 test mới (`GlobalExceptionHandlerProblemDetailTest` 2→5): wrapped-timeout→504, plain-timeout→504, **connection-refused→500 (không bị ăn nhầm)**. Container verify: `unzip -p app.jar … GlobalExceptionHandler.class \| strings \| grep "Gateway Timeout"` = **1**. Đường 504 phủ bằng unit test, **chưa** ép được timeout sống (race phụ thuộc tải GPU) — ghi rõ là `FIXED-VERIFIED-PARTIAL`. |
| **F94** | **20 endpoint / 6 controller trả lỗi shape CŨ `{"error":"…"}`** thay vì RFC 7807 ProblemDetail (đo 10 response lỗi: 3/10 là `LEGACY{error}`). Interceptor `api.js` chỉ sao `detail`→`message` ⇒ call-site đọc `.detail`/`.message` hiện chuỗi generic ("Sinh từ thất bại") thay vì "topic không được để trống". | Chuẩn hoá tại **MỘT chỗ** (`api.js`): nếu body có `error` dạng string thì backfill vào `detail` **và** `message` (chỉ khi trống). **Giữ nguyên backend** vì **13 call-site frontend đang đọc `.error`** — đổi backend sẽ là breaking change không cần thiết. | `p13_error_contract.js`; `api.test.js` 4→8 test. Toàn bộ frontend **89/89**. |
| **SWEEP-LEAK** | **Rò rỉ dữ liệu do harness**: `p3a` bị cắt ở mốc timeout 120 s **giữa chừng** nên bước cleanup của chính nó không chạy → 1 lesson + 4 exercise lọt vào DB thật (phát hiện vì parity lệch `1472/43741` so với baseline `1471/43737`). | `sweep/v8/clean_r2.sql` — xoá **đúng phạm vi** (theo `lesson_id` + `title LIKE 'ZZ%'`), `SET QUOTED_IDENTIFIER ON`, kiểm tra **0 phụ thuộc** trước khi xoá. | Parity về **khớp baseline CHÍNH XÁC**: lessons 1471, exercises 43737, users 76, vocabulary 127, speaking_submissions 28, video_attempts 15, lesson_submissions 4, payment_transactions 126, decks 14, lesson_snapshots 5; orphan ex/uvp_vocab/uvp_user/snap/sub = **0/0/0/0/0**; `zz_leftover=0`. |
| **GAP-COVERAGE-16** | **16/144 endpoint chưa từng được probe** dù REPORT cũ khẳng định "139 mapping phủ hết" — toàn bộ họ route **ALIAS** (`video-prompts`, `video-submissions`) + write path của `LessonController`. | Thêm `sweep/v8/p6.js` (52 probe) phủ đủ 16 gap. | `coverage_check.py` → **covered=144/144, uncovered=0**; `p6.js` **52 probe / 0 FAIL** |
| **CLAIM-COUNT** | Số liệu inventory cũ ("139 mapping/97 handler") **không tái lập được từ source** (không có script sinh ra nó). | Sinh số bằng script + ghi rõ định nghĩa đếm. | `endpoint_inventory.py` → **144 unique mapping / 26 controller** (GET 61, POST 54, PUT 16, DELETE 10, PATCH 3), deterministic |
| **BASELINE-DRIFT** | Baseline trong `AGENTS.md`/`REPORT.md` **đã cũ** sau khi thêm test mới. | Cập nhật `AGENTS.md` + REPORT theo **số đo mới**. | Backend **369 → 378** (`t-r1-baseline.log` → `t-r1-after-fix.log` → `t-r1-after-f93.log`), frontend **85 → 89** (`t-fe-r1.log` → `t-fe-r2.log`), build **176.68 → 176.80 kB** (`build-r1.log` → `build-r2.log`) |
| **PLANB-VERIFY** | **Xác minh Plan B (Be Vietnam Pro) — ĐẠT.** Không phải defect. | Không sửa. Đo thay vì tin: **"font được khai báo" ≠ "font được load"** (khai báo sai vẫn fallback `system-ui` im lặng). | `sweep/v8/ui/design-v2.js`: **12 route × 5 viewport (360/768/1280/1440/1920) = 60 tổ hợp**, Chromium thật. `document.fonts.check()` 400/700/900 = **true/true/true** (15 face); **7 685 element → 0 element không phải Be Vietnam Pro**; tìm `Outfit/Plus Jakarta Sans/Inter/Roboto/Poppins` trong cascade = **0 hit**; horizontal overflow = **0/60** (ngưỡng nhiễu scrollbar 16 px). Chi tiết: `evidence/design-audit-r1.md` |

**Cộng vòng này**: backend **+9 test** (369→378: 6 của F91/F92 + 3 của F93),
frontend **+4 test** (85→89, của F94), 0 test cũ phải bỏ, **1 test cũ cập nhật**
(`aiEndpointUsesTheAiBucket` — thêm stub `getMethod()` vì `:ai` nay chỉ tính POST).
Không thêm dependency nào.

**Bài học phương pháp (ghi lại để vòng sau không lặp)**:

1. **Đo key Redis thật, đừng suy luận từ tên bucket.** F83 (`:order` chết) và F91/F92 là
   **cùng một lớp lỗi**: matcher so khớp chuỗi trong khi route đổi tên. Cách duy nhất phát hiện
   là đọc **key thật** sinh ra sau mỗi prefix.
2. **Sửa cho đúng có thể TỰ GÂY regression cho harness.** Siết `:ai` về đúng 10/phút làm `p3a`
   (35 probe AI) tự chặn chính nó → 8 FAIL giả. Đã sửa `sweep/v8/lib.js` để harness **tự xoá
   bucket của chính nó** trước mỗi probe rơi vào bucket chặt (chỉ xoá `rate_limit:*`, không
   đụng dữ liệu nghiệp vụ). Sau fix: `p3a` **35/0 FAIL**.
3. **Một hiện tượng, hai cách giải thích — phải phân biệt.** F93 nhìn như "AI hỏng" nhưng đo
   lại 10/10 xanh ⇒ là **contention transient**, và fix đúng là **map lỗi cho đúng** (504),
   không phải "sửa AI".
4. **Job bị cắt giữa dòng để lại rác trong DB thật.** Mọi sweep tạo row tạm phải **đo lại parity
   sau khi chạy**, kể cả khi job báo lỗi/timeout.

---

## 3. CHƯA LÀM / CÒN MỞ (có lý do)

1. **Cloudinary đang dùng thật, không phải demo.** `.env` có `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` thật (mask-verify 2026-09-15) và container nhận đủ ba biến. Với file media THẬT: `POST /api/auth/avatar/upload` → **200** + URL `.../engflow/avatars/<public_id>.png`; `POST /api/admin/audio-upload` (WAV thật 563 kB) → **200** + URL `.../engflow/audio/<public_id>.wav` — đây chính là đường sinh listening audio của MCP. Kết luận "demo creds" ở bản nháp trước của audit-v8 là **SAI**, sinh ra do fixture giả (bytes không phải ảnh/audio thật); đã sửa AGENTS.md + REPORT.
   Lệch nhỏ còn lại: với cùng nội dung giả, audio-upload trả **400** `{"error":"Unsupported video format or file"}` (bắt được từ chối của Cloudinary), còn avatar trả **500** (`AuthController` chỉ map `IllegalArgumentException` → 400, số còn lại → 500) — API trả lỗi không đồng nhất khi Cloudinary từ chối, không phải mất năng lực upload.
2. **Không xóa index chưa dùng** (`idx_lessons_level`, `idx_prompts_level`, `idx_prompts_published`, `decks.idx_decks_public`,
   `IX_uvp_due` seeks=0): usage stats reset khi container restart → không đủ bằng chứng; xóa là rủi ro asymmetric.
3. **4848 `correct_answer` rỗng** để nguyên — theo gate 3.4-B (AGENTS.md), đa số là MC-fragment scrape lỗi, ungradeable-by-design.
4. **Không "sửa" chất lượng nội dung AI** vượt ngoài các guard: `1.5b` vẫn sinh phát biểu sai ngữ pháp ("superlative of *most expensive* → *best*").
   Đã thêm guard trùng-đáp án (F85); còn lại thuộc review loop + reviewer model — đã ghi nhận là giới hạn mô hình local.
5. **4 user `zzprobe*` tạo ngày 12/09** (của audit-v7/v8-P3 trước tôi) để nguyên — không phải row của vòng này.
6. **Mic/permission flow — ĐÃ ĐÓNG ở vòng 3**: bấm thật `Bật micro → Bắt đầu ghi → Dừng ghi → Gửi bài` trong Chromium với fake device;
   blob webm thật được upload (`200`), assess tự chạy (`200`), row ghi vào DB. Vì fake device phát **im lặng**, Whisper trả transcript rỗng →
   trạng thái cuối là `FAILED` (đúng thiết kế "không chấm được"). Muốn thấy điểm thật thì phải nói vào micro thật — không tự động hoá được trong CI/headless.
7. **Thanh toán vẫn không simulate webhook thành công** (`create-order` → 200 + `orderCode` + QR, dừng ở đó) để không đổi trạng thái premium của user seed.
8. **Không có API xoá `speaking_submissions`** (không có `DELETE /api/v1/admin/speaking-submissions/{id}` → **404**, và `AdminSpeakingSubmissions.vue` cũng không có nút xoá):
   dọn row thử nghiệm phải làm bằng SQL + `mc rm` object trong MinIO (`speaking-uploads/<media_object_key>`). Không phải bug (tính năng xoá chưa từng tồn tại),
   nhưng là điều cần biết khi test — vòng 3 đã dọn đúng cách và parity về baseline.
9. **`GET /api/vocabulary` (list) yêu cầu đăng nhập** (`401` khi ẩn danh) — chỉ `/api/vocabulary/search` và `/api/vocabulary/dictionary/*` là `permitAll`.
   Ghi nhận là hành vi có chủ đích của `SecurityConfig`; frontend chỉ gọi 2 route public.
10. **Sweep UI đi qua `/premium/checkout` sẽ tạo row THẬT trong `payment_transactions`** (`PremiumCheckout.vue` gọi `POST /api/v1/payment/create-order` lúc mount để dựng QR).
    Vòng 3 phát hiện sau khi chạy `design.js`/`routes.js`: parity lệch **126 → 128** (2 row PENDING lúc 01:34, 01:36) → đã xoá theo `created_at` của ngày chạy và parity về **126**.
    Đây là side effect cần nhớ cho mọi sweep UI sau này (không phải bug, nhưng bắt buộc dọn).
11. **Không commit**: working tree để nguyên cho bạn review (theo rules, chỉ commit khi được yêu cầu). Vòng 3 **có** sửa `frontend/src` (UI-3: 2 file admin template) + thêm
    `sweep/v8/p5.js` + `p5.json`, `sweep/v8/ui/v3.js` + `v3ui.json`, `sweep/v8/ui/v3b.js` + `v3b.json`, `sweep/v8/ui/altcheck.js` (sửa metric `ui/design.js`), và cập nhật `REPORT.md`/`tasks.md`/`AGENTS.md`.
    `src/main` và `src/test` của backend **không bị chạm** ở vòng 3.
12. **`openrouter.*` chỉ là TÊN CŨ, không phải cloud** — đã đo lại 2026-09-16 vì tên config gây nghi ngờ: `AiPromptService`/`AiVocabService` dùng `WebClient` với `openrouter.base-url`, nhưng giá trị thực trong container là `http://host.docker.internal:11434/v1` (Ollama local). Bằng chứng 2 chiều: (a) `rg "openrouter\.ai|api\.openai\.com"` = **0 hit** trong `src/`+`frontend/`; (b) 1 call `ai-generate` → `%LOCALAPPDATA%\Ollama\server.log` tăng đúng **1** dòng `POST /v1/chat/completions` (02:07:16, 5.24 s, client `127.0.0.1`) và toàn bộ 58 call trong log đều từ loopback. **Còn lại**: `.env` giữ một **API key OpenRouter thật** (`sk-or-v1-`, 73 ký tự) được gửi kèm `Authorization` tới Ollama local — Ollama bỏ qua nên **không có egress**, nhưng là secret chết nên để lại hay đổi tên đều được (không tự xoá: có thể là key bạn còn dùng ở nơi khác).
13. **AI thỉnh thoảng lẫn ký tự CJK (đọt, chưa tái hiện)** — `sweep/v8/cjkcheck.js` chạy 15 lần `ai-generate` (5 chủ đề × 3) = **0/15 có CJK**; nhưng một run `p3a` cùng ngày có `"description": "...cho người mới bắt đầu học英语并..."`. Không thêm guard vì chưa đủ bằng chứng tái hiện; nếu cần thì guard deterministic `[一-鿿]` ở tầng parse/validate (chưa làm để tránh chặn oan nội dung hợp lệ có Hán tự trong ví dụ ngôn ngữ học).
14. **Không có API xoá `speaking_submissions` → row xoá bằng SQL thì object MinIO thành mồ côi.** Vòng 3 lần 2 tạo 6 object mồ côi (5 `speaking-submissions/*.wav` + 1 `video-attempts/.../c6381dd5...`) và đã dọn bằng `mc rm --force` sau khi đối chiếu `media_object_key` trong DB. Là hệ quả của việc không có API xoá, không phải bug mới.
15. **Harness ghi JSON theo CWD** — `sweep/v8/*.js` `lib.dump()` và `ui/routes.js` ghi file tương đối, nên phải chạy **từ trong `sweep/v8`** (chạy từ repo root sẽ để lại `p*.json` ở root — đã mắc và đã dọn). Lần sau chạy sweep: `Set-Location sweep\v8` trước.

---

## 4. SKILL / PLUGIN ĐÃ NẠP & DÙNG

| Skill | Dùng cho |
|---|---|
| `superpowers:verification-before-completion` | vòng 3: mỗi kết luận phải kèm output vừa chạy (suite/probe/parity), không dùng "chắc là pass" |
| `build-web-apps:frontend-testing-debugging` | xác định Browser plugin **không có** trong phiên → dùng Playwright và ghi lại lý do (đúng contract của skill) |
| `karpathy-guidelines` | thay đổi tối thiểu, không gold-plate; surface trade-off (band nén vs đổi breakpoint; không xóa index) |
| `java-coding-standards` | `SafeUploadNames`/`LessonTitle` — naming, immutability, Optional/streams, exception |
| `accessibility` | audit WCAG: alt, focus, reduced-motion, target size ≥24 px, skip-link |
| `prompt-master` | phân tích 10 lỗ hổng prompt + viết lại prompt (`prompt-gap.md`) |
| `speckit-constitution → specify → clarify → checklist → plan → tasks → implement → converge` | khung workflow bắt buộc; artifacts trong `.specify/specs/audit-v8-full/` |
| `frontend-testing-debugging` (plugin Build Web Apps) | phương pháp test browser: console/network/computed-style, false-positive YouTube |
| `security-diff-scan` / `fix-finding` (plugin Codex Security, hướng tiếp cận) | truy vết source→sink chuỗi XSS, fix + verify nhiều tầng |
| Plugin CircleCI | không có CI config trong repo → không áp dụng (đã kiểm `.circleci/`) |
| Computer Use / Visualize | không cần: kiểm chứng bằng Playwright Chromium + SQL + HTTP trực tiếp (bằng chứng mạnh hơn screenshot) |

**Nạp thêm ở vòng Task 1–16 (2026-09-16)**:

| Skill | Dùng cho |
|---|---|
| `speckit-taskstoissues` | Task 15 — kiểm pre-check (`extensions.yml`), xác nhận remote là GitHub, đọc `tasks.md` + `findings.json` để quyết định có cần chuyển issue không (kết luận: **không**, xem §6.6) |
| `speckit-analyze` / `speckit-converge` | 2 lượt analyze (trước/sau converge) — `analyze.md` ghi cả 2; `converge.md` ghi 13 mục **cố ý không** converge kèm lý do |
| `accessibility` (dùng lại) | falsification WCAG: tách ngưỡng **AA 24 px** vs **AAA 44 px**, `prefers-reduced-motion`, `alt=""` hợp lệ — `design-audit.md` §4 |
| `java-coding-standards` (dùng lại) | đọc `SafeUploadNames` / `LessonController` để xác minh biên upload + clamp phân trang |
| `superpowers:verification-before-completion` (dùng lại) | nguyên tắc chủ đạo của vòng: 3 harness pass-sai + 2 bug cleanup bị bắt **vì đọc lại dữ liệu thật**, không tin output của chính harness |

---

## 5. BẰNG CHỨNG / REPRODUCE

```powershell
# suites
cmd /c "mvnw.cmd test"                                     # 378/378 BUILD SUCCESS (369 + 6 F91/F92 + 3 F93)
Set-Location frontend; cmd /c "npx vitest run"             # 89/89 (18 files)
cmd /c "npx vite build"                                    # entry 176.80 kB / gzip 67.39 kB

# API sweeps (Node 24, tự refresh JWT, tự flush rate-limit bucket của chính nó)
Set-Location sweep\v8; node p1.js; node p2.js; node p3a.js; node p3b.js; node burst.js

# browser (cần NODE_PATH tới playwright-core toàn cục)
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\routes.js admin"
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\routes.js guest"
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\design.js"
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\overflow3.js"
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node xss_poc.js"

# vòng 3 lần 2 — F88 (admin sửa bài học video) + F89 (nháp không lộ)
Set-Location sweep\v8; node p88live.js                                   # 16/16 trên container đã rebuild
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\v4edit.js"  # 13/13: bấm Sửa -> PUT 200 -> toast -> phụ đề giữ nguyên
python sweep\v8\sqlrun.py sweep\v8\clean_f88.sql                        # dọn row probe (users/vocab/submissions/attempts/payments)
python sweep\v8\sqlrun.py sweep\v8\parity.sql                           # phải về đúng baseline

# vòng 3 (2026-09-16) — CHẠY TỪ TRONG sweep\v8 (harness ghi JSON theo CWD)
Set-Location sweep\v8; node p5.js                                  # 95 probe / 60 assert (auth + streak + search/sort + re-verify)
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\v3.js"  # 30 assert: register form, streak calendar, search, GHI ÂM THẬT
# lưu ý: p5.js cần `SET QUOTED_IDENTIFIER ON` cho mọi DELETE (filtered index IX_uvp_due) — helper `sql()` đã thêm

# vòng audit-v8-full (2026-09-16) — F90 bác bỏ · F91/F92 bucket routing · F93 timeout · F94 error shape · Plan B
Set-Location sweep\v8
python endpoint_inventory.py      # 144 mapping / 26 controller -> evidence/endpoint-inventory.json
python coverage_check.py          # 144/144 covered, 0 uncovered
node p6.js                        # 52 probe / 0 FAIL (đóng 16 gap ALIAS + LessonController write)
node p7_ratelimit_order.js        # thứ tự filter: request bị 401 không tốn bucket (hiện tượng)
node p9_f90_scope.js              # :auth tốn ở request 21, :mail ở request 6 (bucket permitAll VẪN chạy)
node p10_f90_falsify.js           # 0/17 surface permitAll bypass -> F90 KHÔNG phải lỗ hổng
node p8_bucket_coverage.js        # 13/13 endpoint AI -> :ai; 2 upload -> :upload (TRƯỚC: 1/13, 0/2)
node p8b_ceiling.js               # first429=11 (:ai 10/phút) và 16 (:upload 15/phút)
node p11_enrich_reliability.js 10 # 10/10 200, 1000-1274 ms -> F93 là contention transient, không phải lỗi code
node p12_f93_live.js              # handler mới CÓ trong container đang chạy + đường thành công không bị ăn
node p13_error_contract.js        # 3/10 response lỗi dùng shape cũ {"error"} -> F94
python sqlrun.py clean_r2.sql     # dọn 1 lesson + 4 exercise rò rỉ do p3a bị timeout giữa dòng
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\design-v2.js"  # Plan B: 12 route x 5 viewport = 60 tổ hợp

# DB
python sweep\v8\sqlrun.py sweep\v8\parity.sql
python sweep\v8\sqlrun.py sweep\v8\db_frag.sql
```

```powershell
# ===== VÒNG audit-v8-full Task 1–16 (2026-09-16) — CHẠY TỪ TRONG sweep\v8 =====
Set-Location sweep\v8

# route inventory + reconcile (Task 5)
python route_inventory.py            # 39 route -> .specify/specs/audit-v8-full/evidence/route-inventory.json
python route_verify.py               # VERDICT: CONSISTENT (đối chiếu với path: literal)

# evidence documents (Task 7/8/9/10/11/12)
python sqlrun.py db-audit.sql        # 12 mục read-only -> db-audit-out.txt
node perf.js                         # N=7 x 15 endpoint -> perf.txt + perf.json
node security.js                     # 0 failures -> security.txt + security.json
node xss-prove.js                    # PASS + tự dọn row -> xss-prove.txt + xss-prove.json
node p8_bucket_coverage.js           # 13 :ai + 2 :upload -> p8-bucket-coverage.txt

# browser (Task 6/11) — cần NODE_PATH tới playwright-core toàn cục
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\routes-all.js"   # 228 lượt, 38 route x 2 viewport x 3 role
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\design-v2.js"    # 60 tổ hợp route x viewport
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\design.js"       # a11y: alt / tap target / reduced-motion

# Task 14 — vòng 2 adversarial
node p9_f90_scope.js                 # F90: bucket permitAll vẫn chạy
node p10_f90_falsify.js              # 0/17 surface bypass
node p11_enrich_reliability.js       # 10/10 200 -> contention transient
node p12_f93_live.js                 # handler trong container + đường thành công
node p13_error_contract.js           # 3/10 LEGACY{error}
node p14_adversarial.js              # 38 case (malformed JSON, dup register, upload, slow net, 503)
node p15_adversarial_deep.js         # 21 case — assert ĐÃ SỬA cho 3 case pass-sai của p14

# Task 14 — dọn dẹp + parity (BẮT BUỘC sau mọi sweep có ghi row)
python sqlrun.py p15-clean-residue.sql      # 3 row XSSPROBE/iframe sót -> vocabulary 127
python sqlrun.py p16-clean-payments.sql     # 6 row PENDING do mount /premium -> 126 (chỉ xoá status <> SUCCESS)
python sqlrun.py p16-parity.sql             # 10 bảng phải khớp baseline
python sqlrun.py p16-orphans.sql            # orphan ex/uvp_vocab/uvp_user/snap/sub = 0

# Task 16 — suite cuối trên bản dựng đóng băng
Set-Location ..\..
cmd /c "mvnw.cmd test"                                   # 378/378 BUILD SUCCESS
Set-Location frontend; cmd /c "npx vitest run"; cmd /c "npx vite build"
Set-Location ..; git diff --check; git status --short
```

Bằng chứng vòng này: `.specify/specs/audit-v8-full/evidence/round-2/` (12 file) +
`sweep/v8/{t-final-backend.log, t-final-frontend.log, t-final-build.log, db-audit-out.txt,
perf.txt, perf-reads-out.txt, security.txt, xss-prove.txt, p8-bucket-coverage.txt,
p14-adversarial.txt, p15-adversarial-deep.txt}`.
Artifact bổ sung: `{constitution, route-map, db-audit, performance, design-audit, security-audit,
analyze, converge}.md`.
```powershell
# vòng 3 — a11y alt thật (phân biệt thiếu attribute vs alt="") + verify UI-3 bằng browser
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\altcheck.js"
cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\v3b.js"     # 17 assert: preview alt ở builder + modal bài tập
```
Json kết quả: `sweep/v8/p1.json`, `p2.json`, `p3a.json`, `p3b.json`, `p4a…p4d.json`, **`p5.json`**, `ui/routes-*.json`, **`ui/v3ui.json`**, **`ui/v3b.json`**, **`ui/v4edit.js` log** (`ui/routes-*.log`), `r-p4*.log`, `t-f88.log`, **`t-final3.log`** (369 backend), **`t-fe-final.log`** (85 frontend).
Artifact: `.specify/specs/audit-v8-full/{spec,clarify,checklist,plan,tasks,prompt-gap,REPORT}.md`.

---

## 6. VÒNG audit-v8-full Task 1–16 (2026-09-16) — hoàn thiện bộ artifact + vòng 2 adversarial

Vòng này chạy theo `2026-09-16-engflow-comprehensive-audit-redesign.md` (Task 1–16). Vòng trước
đã **sửa** lỗi (F90–F94) nhưng **bộ bằng chứng chưa đủ layout mà plan yêu cầu**: 8 tài liệu bắt buộc
chưa tồn tại. Đó là phát hiện lớn nhất của vòng này — và nó nằm ở **artifact**, không phải ở code.

### 6.1 Tám tài liệu bắt buộc còn thiếu → đã viết, mỗi cái có harness chạy lại được

| Tài liệu | Harness sinh bằng chứng | Kết quả |
|---|---|---|
| `route-map.md` | `route_inventory.py` · `route_verify.py` · `ui/routes-all.js` | **39 route** (CONSISTENT), **228 lượt** (38 route × 2 viewport × 3 role; `/admin` chỉ redirect) → **0** console error, **0** API ≥400, **0** overflow, **0** font lạ, **0 wrong landing**, **18/18** admin render thật |
| `db-audit.md` | `db-audit.sql` (12 mục, **read-only**) | 0 lỗi SQL; orphan **0**; 0 FK disabled/untrusted; 2 filtered index; parity **khớp tuyệt đối** |
| `performance.md` | `perf.js` + `perf-reads.sql` | **N=7** mỗi endpoint; 105/105 call `200`; đọc 12–74 ms; F87 shape xác nhận **4 600 reads / 35 ms**, **0** statement chạm `content_original` |
| `design-audit.md` | `ui/design-v2.js` + `ui/design.js` | Be Vietnam Pro **0/7 685** element lệch; 15 face; 400/700/900 = true; **0** vi phạm AA (kể cả tap target <24 px) |
| `security-audit.md` | `security.js` · `xss-prove.js` · `p8_bucket_coverage.js` | **0 fail**: 10 admin surface role-gated, 0 rò rỉ body, 4/4 token xấu bị chặn, XSS **không thực thi** |
| `constitution.md` | — (bản ghi tuân thủ) | P1–P8 **0 vi phạm**, **0** sửa đổi, 1 đề xuất PATCH (P1: 221/73 → 378/89) |
| `analyze.md` | — (phân tích 3 lượt) | Lượt 1: 6 tài liệu thiếu + 1 bằng chứng stale + 1 bộ số thiếu generator → **0** code change. Lượt 2: suite xanh, 6/6 finding tái xác minh. Lượt 3: **5 câu của chính audit đã hết đúng** → sửa |
| `converge.md` | — (đánh giá hội tụ) | 9/9 spec requirement đạt; **0** task do converge thêm; 13 mục **cố ý không** hội tụ kèm lý do; §4.1 ghi lại quyết định đã **đảo ngược** |

**Kiểm lại bộ layout bắt buộc** (Task 7): **16/16 tài liệu** + `evidence/` (**4 file** ở gốc +
`round-2/` **21 file** + `screens/` **36 ảnh PNG**) + `findings.json`
(**16** finding) — đủ theo danh sách plan.

### 6.1b Task 5 (screenshots) + vòng 3 — harness seed SAI, và giới hạn ĐÃ ĐO của nó

Hai việc còn thiếu của plan được làm nốt ở vòng này, và việc thứ hai lộ ra một lỗi harness nghiêm trọng.

**(a) Backup trước DML (PLAN.md Phase 2 mặc định + Phase 4.4).** Điểm khôi phục mới nhất trên đĩa là
**2026-09-12**, trong khi vòng này đã chạy DELETE. Đã tạo `engflow_2026-09-16-audit-v8-full.bak`
(`BACKUP ... WITH COMPRESSION, CHECKSUM` → **197,1 MB thô / 35,3 MB nén**, `RESTORE VERIFYONLY` →
*The backup set on file 1 is valid*, `is_damaged=0`, `has_backup_checksums=1`, recovery FULL) và copy ra
ngoài volume `C:\Users\ASUS\engflow-backups\`. Ghi thẳng: điểm khôi phục này phủ cho DML **về sau**,
không hồi tố 9 row đã xoá — nhưng từ nay lệnh `BACKUP` nằm trong chính script nên chạy lại được.

**(b) 36 screenshot cho các flow bắt buộc (PLAN.md Phase 5).** Trước vòng này evidence có **0 ảnh**.
`p17_screenshots.js` nay chụp 36 flow (30 desktop + 6 mobile), tổng **6,7 MB**, vào `evidence/screens/`.

**(c) Lỗi harness: seed chỉ `token`.** Lần chạy đầu báo **36/36 PASS** — nhưng 9 shot admin đều có
`text=1524`, đúng bằng độ dài trang chủ (1513). Kiểm bằng vision: ảnh `22-admin-dashboard.png` là
**trang chủ**, không phải dashboard.

Nguyên nhân: `store/modules/auth.js:22` tính `isAdmin = user.value?.isAdmin`, `user` khởi tạo từ
`localStorage.user`, và `main.js` **không** gọi `fetchUser()` lúc boot. Harness chỉ ghi `token` ⇒
`isAdmin === undefined` ⇒ guard `index.js:220` đá `/admin/*` về `/`. Trang chủ là trang hoàn toàn
khỏe mạnh nên `mounted=Y, err=0, api4xx=0, ovf=-15` — **không chỉ số nào phân biệt được "render đúng
route" với "bị đá sang trang khác"**.

**Giới hạn đã ĐO — không nói quá.** Ban đầu tôi định báo "cả sweep 152 lượt vô hiệu". Đã kiểm lại
bằng A/B (`p20_routes_all_old_seed_ab.js`: seed cũ vs seed mới, đi đúng thứ tự route của `routes-all.js`):

| Seed | Route admin bounce | text của 9 route admin |
|---|---|---|
| cũ (chỉ token) | **0/9** | 721/2800/2471/1151/921/4167/588/942/365 |
| mới (token+user) | **0/9** | y hệt |

Hai seed cho **kết quả giống hệt nhau**, vì app tự hydrate `user` qua `/api/auth/me` trong lúc đi các
route trước đó. `p19` case C xác nhận điều tương tự cho `design-v2` (60 tổ hợp vẫn hợp lệ).

**Vậy thiệt hại thật, đúng phạm vi:**

| Harness | Kiểu context | Bị ảnh hưởng? | Bằng chứng |
|---|---|---|---|
| `p17_screenshots.js` | **context mới mỗi shot** | **CÓ — 9/36 ảnh là trang chủ đội lốt admin** | `p17_screenshots.txt` (text=1524 × 9) |
| `p18_redirect_audit.js` | **context mới mỗi case** | CÓ (chính là probe phát hiện ra) | `p18_redirect_audit.txt` (9 redirect → `/`) |
| `ui/routes-all.js` | 1 context / role, đi tuần tự | **KHÔNG** (A/B 0/9 bounce) | `p20_ab.json` |
| `ui/design-v2.js` | 1 context / viewport, đi tuần tự | **KHÔNG** (p19 case C) | `p19.txt` |

Sửa: `loginFull()` + `mapUser()` + `seedAuth()` trong `ui/lib.js`; `routes-all.js` nay chạy **3 role**
(admin/user/anon) thay vì 2, ghi `finalPath` cho **mọi** lượt và assert hợp đồng landing guard × role
**cả hai chiều** (stay vs bounce); `design-v2.js` ghi `wrongPage`; `p17` từ chối chạy nếu
`adminS.user.isAdmin` falsy. Kết quả sau sửa: `routes-all` **228 lượt, 0 wrong landing, 18/18 lượt
admin render trang admin thật**; `p17` **36/36, 0 landed-on-wrong-page**, shot admin nay `text=721/2800/...`.

**(d) Sweep lại ghi row `payment_transactions`.** Chạy `routes-all.js` (có `/premium/checkout`) đẩy
parity **126 → 134**. 8 row đều `PENDING`, `transaction_id IS NULL` — đã kiểm từng row trước khi xoá.
Thêm `cleanupAuditPayments()` vào `ui/lib.js`: xoá **trong cùng run**, chỉ nhắm
`status <> 'SUCCESS' AND transaction_id IS NULL` (không thể chạm đơn đã trả tiền), và **assert parity**
chứ không tin exit code. Verify: sweep chạy xong tự trả về **126/126 → PARITY OK**, exit 0.


### 6.2 Ba harness "pass vì lý do SAI" — tệ hơn cả fail

Đây là phần quan trọng nhất của vòng này. `p14_adversarial.js` báo **38 case / 0 fail**, nhưng **3
case trong đó không kiểm tra thứ nó tuyên bố**. Một harness pass sai chỗ nguy hiểm hơn harness fail,
nên từng cái được kiểm lại bằng probe đã sửa (`p15_adversarial_deep.js`):

| Case | Vì sao pass SAI | Sửa thành |
|---|---|---|
| **Invalid upload** | Gửi token **user** tới `/api/admin/audio-upload` (admin-only) → **403 hết mọi case**, `SafeUploadNames` chưa từng chạy. Assert `status < 500` pass vô nghĩa. | Dùng token **admin** (có baseline chứng minh token qua được role gate) → cả 8 case trả **400** `Unsupported video format or file`, `dangerousUrl=false`. Test thêm `.HTML` hoa, double extension, path traversal, null byte. |
| **Absent exercise submit** | Gửi `{exerciseId, answer}` nhưng `GradeRequest` là `{answers:[{exerciseId, userAnswer}]}` — **sai cả 2 tên field** → `answers` null → không có gì để chấm → 200. Case chưa từng chạm tới id thiếu. | Gửi đúng shape; assert `total === 0 && results.length === 0` (không "phantom result") **và** positive control: id thật → `total = 1`. |
| **Pagination** | Chỉ assert "không 5xx" — sẽ pass cả khi server trả về **100 000 row**. | Assert **số row thực tế bị clamp**: `size=100000` → **100 row**; `size=-5`/`0` → **1 row**; `page=-1` → 20; `page=99999` → 0. Khớp `LessonController.java:36`. |

**Kết quả sau khi sửa**: `p15` **21 case / 0 fail**, và lần này mỗi assert đều kiểm tra đúng hành vi.

### 6.3 `xss-prove.js` KHÔNG tự dọn dữ liệu nó ghi → parity từng SAI

Phát hiện nghiêm trọng nhất về mặt vận hành: **một con số "127" trước đó là SAI**.

`xss-prove.js` ghi row thật vào `vocabulary` (chứng minh stored-XSS thì phải lưu thứ gì đó). Việc dọn
là **bước thủ công riêng**, không nằm trong harness → rác tích lũy, lần đọc sau ra **130** row thay vì
127. Chỉ phát hiện được vì harness adversarial **đọc lại bảng** thay vì tin con số cũ.

Đã đưa cleanup vào **trong cùng run**, và **2 bug của chính cleanup** được tìm ra bằng cách test nó
chứ không phải tin nó:

1. **Cửa sổ thời gian bắt đầu quá muộn.** Timestamp run-start được tính **lúc cleanup** thay vì lúc
   process khởi động → `created_at >= start` **loại trừ đúng những row vừa ghi** → batch báo thành công
   và khớp **0 row** (no-op âm thầm). Sửa: chốt `RUN_START_NAIVE` ở module load.
2. **Coi "không có lỗi SQL" là thành công.** Lần chạy đầu in `cleanup ran without SQL error: true`
   nhưng **để lại 3 row**. Assert nay là **parity**: phải `remaining = 0` **và** `vocabulary_total = 127`,
   nếu không thì run fail.

Sửa thêm: bộ lọc từ nay khớp `word LIKE 'XSSPROBE%'` **hoặc** đúng các từ đã lưu, vì model có lúc
**trích xuất danh từ ngắn từ payload** (lưu literal `iframe`) — không mang marker nên sẽ sót.

Kiểm chứng end-to-end sau khi sửa:
```
cleanup: no SQL error=true remaining=0 total=127 (baseline 127) -> PARITY RESTORED
rows created by this run cleaned   : true (vocabulary now 127, baseline 127)
VERDICT: PASS - stored AI output cannot execute
```

**Bài học**: harness có ghi row nghiệp vụ **phải tự dọn trong cùng run** và **phải verify parity**,
không verify exit code của chính nó.

### 6.4 Delta `payment_transactions` 126 → 132 (đã dọn về 126)

3 sweep UI (`design.js`, `design-v2.js`, `routes-all.js`) mount `/premium`, mà `PremiumCheckout.vue`
gọi `POST /api/v1/payment/create-order` lúc mount để dựng QR — đúng cái trap đã ghi trong `AGENTS.md`
(2 sweep = 126 → 128; vòng này 3 sweep nên delta lớn hơn). **6 row đều được kiểm trước khi xoá**:
tất cả `PENDING` với `transaction_id IS NULL` = đơn tạo ra nhưng **chưa từng thanh toán**. Cleanup
(`p16-clean-payments.sql`) vì vậy còn yêu cầu `status <> 'SUCCESS'` → **không thể** xoá đơn đã trả tiền thật.

### 6.5 Task 14 — vòng 2 adversarial (đủ danh sách plan yêu cầu)

Plan liệt kê 13 case bắt buộc. Tất cả đều có, chia giữa các harness:

| Case | Ở đâu | Kết quả |
|---|---|---|
| expired token | `security.js` §3 (4 dạng: không token / rác / sai chữ ký / Bearer rỗng) | 4/4 **401** |
| wrong role | `security.js` §1 (10 surface × 3 role) | 0 fail (admin 200 · user 403 · anon 401) |
| AI malformed output | `security.js` §4 + `xss-prove.js` | không thực thi |
| reduced-motion | `ui/design.js` | transition `0s`, transform `none` |
| narrow viewport | `ui/design-v2.js` (360) + `routes-all.js` | 0 overflow |
| malformed JSON | `p14` §1 (5 dạng) | 5/5 **400** + ProblemDetail |
| duplicate registration | `p14` §2 | **400**, không 500 |
| empty search | `p14` §3 + `p15` §4 | 0 row, không dump bảng (127 row) |
| out-of-range pagination | `p15` §3 (6 case, **assert clamp**) | 100/1/20/0 row đúng |
| stale resource | `p14` §5 (6 case) | 404/400, không 500 |
| invalid upload | `p15` §1 (8 case, **token admin**) | 8/8 **400** |
| slow network | `p14` §7 (delay 4 s qua route interception) | app vẫn mount, 0 pageerror |
| service-unavailable | `p14` §7 (abort + 503 giả) | shell vẫn mount, 0 pageerror |

**So sánh vòng 1 vs vòng 2**: không finding nào "biến mất" mà không có status. F90–F94 giữ nguyên
kết luận sau khi chạy lại (`p9`–`p13` đều exit 0). Vòng 2 **rộng hơn** vòng 1: thêm 38 + 21 = **59
case adversarial** mà vòng 1 chưa có.

### 6.6 Task 15 — taskstoissues: **KHÔNG chạy (không cần thiết)**

- Remote **có** và **đúng** GitHub: `https://github.com/giahuy1411/EngFlow.git` → bước này *áp dụng được*.
- `.specify/extensions.yml` **không tồn tại** → không có hook trước/sau, bỏ qua im lặng theo skill.
- **`tasks.md`: 0 task chưa tick** (T01–T62 xong hết; T42–T62 ghi lại vòng F90–F94 + Task 1–16) → không có task nào để chuyển thành issue.
- **`findings.json`: 10/10 finding đã có status cuối** — `FIXED-VERIFIED` (F91–F94, GAP-COVERAGE-16,
  CLAIM-COUNT, SWEEP-LEAK, BASELINE-DRIFT), `CLOSED-NOT-A-BUG` (F90), `VERIFIED-PASS` (PLANB-VERIFY).
  **Không có "unresolved actionable finding" nào.**
- Môi trường phiên này **không có** `gh` CLI và **không có** GitHub MCP server → không có đường tạo issue.

⇒ Theo acceptance của Task 15: **"not run: no GitHub issue conversion required."** Không tạo
`issues.md` vì plan nói chỉ tạo khi thực sự cần chuyển đổi.

Các mục **deferred** (8 user audit cũ, 6 lesson content rỗng, 4 bảng `_bak_v5`, ~9 index 0 seek,
số P1 trong constitution) **không** được chuyển thành issue vì chúng là **quyết định của chủ sở hữu**,
không phải finding hành động được của vòng này — mỗi cái đã có lý do ghi rõ trong tài liệu tương ứng.

### 6.7 Task 16 — bảng trạng thái cuối cùng

| Nhóm | Nội dung |
|---|---|
| **Đã kiểm tra** | 144/144 endpoint · 39 route inventory / 38 lượt runtime (**× 2 viewport × 3 role = 228**) · 12 mục DB audit · 15 endpoint × N=7 perf · 10 admin surface × 3 role · 13 AI + 2 upload bucket · 59 case adversarial · 7 685 element font · 212 tổ hợp route×viewport · **36 screenshot** · **backup restore point verified** |
| **Đã fix** | F91, F92, F93, F94 (code) + GAP-COVERAGE-16, CLAIM-COUNT, SWEEP-LEAK, BASELINE-DRIFT, **HARNESS-TOKEN-ONLY-SEED** (artifact/harness) + 3 harness false-pass + 2 bug cleanup của `xss-prove.js` + 2 bug cleanup của `cleanupAuditPayments()` |
| **Chưa fix** | **0** |
| **Blocked** | **0** — không dịch vụ nào outage trong suốt vòng |
| **Not applicable** | 4 848 `correct_answer` rỗng (ungradeable-by-design) · 9 LISTENING thiếu `audio_url` (có speech fallback) · 11 NULL `content_original` (giữ theo P5.1) · 138 tap target 24–44 px (AA đạt, 44 px là AAA) · `noShadow=46`/`thinBorder=5` · `q=` leading wildcard 210 ms (không index nào cứu được) · redirect `/login` → `/lessons` khi đã đăng nhập (guard `guestOnly` chạy ĐÚNG, không phải lỗi) |
| **Deferred** | 8 user audit cũ · 6 lesson content rỗng · 4 bảng `_bak_v5` · ~9 index 0 seek · số P1 trong constitution (đề xuất PATCH v1.0.2) |
| **Skills đã nạp** | xem §4 |
| **Command-evidence-exit code** | xem §6.8 |

### 6.8 Bằng chứng lệnh cuối cùng (chạy lại trên bản dựng đóng băng)

| Lệnh | Kết quả | Exit | Log |
|---|---|---:|---|
| `cmd /c "mvnw.cmd test"` | **378/378**, `Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` | 0 | `sweep/v8/t-r3-final.log` (vòng 3 đo lại; khớp `t-final-backend.log`) |
| `npx vitest run` | **89/89, 18 files** | 0 | `sweep/v8/t-final-frontend.log` |
| `npx vite build` | **176.80 kB / gzip 67.39**, 8.16 s | 0 | `sweep/v8/t-final-build.log` |
| `node p9_f90_scope.js` | F90: bucket permitAll vẫn chạy | 0 | `p9_f90_scope.txt` |
| `node p10_f90_falsify.js` | **0/17** surface bypass | 0 | `p10_f90_falsify.txt` |
| `node p11_enrich_reliability.js` | 10/10 `200`, 969–4757 ms | 0 | `p11_enrich_reliability.txt` |
| `node p12_f93_live.js` | handler trong container + 200/1021 ms + 401 | 0 | `p12_f93_live.txt` |
| `node p13_error_contract.js` | 3/10 `LEGACY{error}` (đúng biên đã ghi) | 0 | `p13_error_contract.txt` |
| `node security.js` | **0 failures**, 0 leak | 0 | `security.txt` |
| `node xss-prove.js` | **PASS** + PARITY RESTORED | 0 | `xss-prove.txt` |
| `node p14_adversarial.js` | 38 case / 0 fail | 0 | `p14-adversarial.txt` |
| `node p15_adversarial_deep.js` | **21 case / 0 fail** (assert đã sửa) | 0 | `p15-adversarial-deep.txt` |
| `node p8_bucket_coverage.js` | 13 `:ai` + 2 `:upload` | 0 | `p8-bucket-coverage.txt` |
| `node perf.js` | N=7 × 15 endpoint, 105/105 `200` | 0 | `perf.txt`, `perf-reads-out.txt` |
| `node ui/routes-all.js` | **228 lượt** (38 × 2 viewport × 3 role), 0 lỗi, **0 wrong landing**, 18/18 admin thật | 0 | `ui/routes-all.txt` |
| `node ui/design-v2.js` | 0/60 overflow, 0/7 685 lệch font, **0 wrong landing** | 0 | `ui/design-v2.txt` |
| `node ui/design.js` | 0 vi phạm AA | 0 | `ui/design.txt` |
| `node p17_screenshots.js` | **36/36 ảnh**, 0 landed-on-wrong-page, parity OK | 0 | `p17_screenshots.txt` |
| `node p18_redirect_audit.js` | **14/18 redirect** — chính là probe phát hiện lỗi seed | 0 | `p18_redirect_audit.txt` |
| `node p18b_admin_after_seed_fix.js` | **9/9** route admin render thật | 0 | `p18b.txt` |
| `node p19_why_design_v2_was_fine.js` | case C: `design-v2` không bị ảnh hưởng | 0 | `p19.txt` |
| `node p20_routes_all_old_seed_ab.js` | seed cũ vs mới: **cả hai 0/9 bounce** | 0 | `p20.txt`, `p20_ab.json` |
| `python sqlrun.py audit-v8-full-r2-backup.sql` | `BACKUP` + `RESTORE VERIFYONLY` valid | 0 | `audit-v8-full-r2-backup.sql` |
| `python sqlrun.py db-audit.sql` | 0 lỗi SQL | 0 | `db-audit-out.txt` |
| `python route_verify.py` | `VERDICT: CONSISTENT` (39) | 0 | `route-verify.txt` |

**Parity DB cuối cùng — khớp baseline CHÍNH XÁC**:
`lessons 1471 · exercises 43737 · users 76 · vocabulary 127 · speaking_submissions 28 ·
video_attempts 15 · lesson_submissions 4 · payment_transactions 126 · decks 14 · lesson_snapshots 5`;
orphan `ex/uvp_vocab/uvp_user/snap/sub = 0/0/0/0/0`. Row do vòng này tạo còn sót: **0**
(sweep tự ghi 8 row `payment_transactions`, tự dọn trong cùng run → 126/126 PARITY OK).

### 6.9 Ba biên chưa verify được — ghi rõ, không làm tròn thành "pass"

1. **Đường 504 sống của F93.** Unit test chứng minh handler map timeout → 504, và container đang chạy
   **có** handler (`p12`: 1 hit `Gateway Timeout`). Ép timeout sống cần Ollama vượt 30 s — phụ thuộc tải
   GPU, không tất định. Ghi là `verificationBoundary` trong `findings.json`.
2. **Before/after của F87.** Con số "trước" (95k reads / 367 ms) là đo lịch sử của vòng F87; đo lại phải
   revert fix + rebuild. Cái **đo mới** là code hiện tại giữ đúng shape (4 600 reads / 35 ms, 0 statement
   chạm `content_original`). `performance.md` §2 nói rõ điều này.
3. **`noShadow=46` / `thinBorder=5`.** Báo là informational: hệ thống áp shadow cứng + border 2 px cho
   **container**, không phải mọi node con.
