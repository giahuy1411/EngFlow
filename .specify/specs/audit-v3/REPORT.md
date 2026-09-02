# AUDIT-V3 — Báo cáo kiểm thử & sửa lỗi toàn diện EngFlow

> Speckit feature `audit-v3` · Thực hiện 2026-09-02 · Commit baseline `83ce082` → HEAD `20edf2d`

## 1. Phạm vi kiểm thử (đã làm)

### 1.1 Backend API — mọi nhóm chức năng chính (11/11 PASS)
Đã chạy bằng script PowerShell (audit-api.ps1, audit-retest.ps1, perf-dictionary.ps1) chống `http://localhost:8080`:

| Nhóm | Endpoint đại diện | Kết quả |
|---|---|---|
| Auth | `POST /api/auth/login` (user + admin), JWT 900s | PASS |
| Lessons | `GET /api/v1/lessons` (1462), detail 567, structure, exercises 39 câu | PASS |
| Exercises | `POST /api/lessons/{id}/exercises/grade` → history 68% (25/37) | PASS |
| Decks | `GET /api/v1/decks`, deck 10007 + words | PASS |
| Flashcards | `POST /api/flashcards/review` | PASS |
| Videos | `GET /api/v1/video-lessons/1` (YouTube 2VeQTuSSiI0), subtitles | PASS |
| Vocabulary + Dictionary proxy | `GET /api/vocabulary/dictionary/{word}` (Redis cache 1h) | PASS |
| Speaking | `GET /api/v1/speaking-submissions?page=0&size=5` | PASS |
| Leaderboard | `GET /api/leaderboard` (46 users) | PASS |
| Premium/SePay | `POST /api/payments/create` → order ENG15C67AA3B771, QR SePay | PASS (bạn đã chọn bỏ qua chuyển tiền thật) |
| Admin | lessons CRUD, users list, toggle/revoke premium, seed exercises | PASS |

### 1.2 Frontend UI (chrome-devtools-mcp, trang 1) + cross-verify (playwright-mcp)
- **Đã xác minh qua UI thực tế**: login/logout (user + admin), lessons list 1462 + phân trang 122 trang, lesson 567 (content + 39 exercises + submit → history 68%), video 1 (YouTube embed + tab Phụ đề/Shadowing/Quiz), decks list, deck 10007 + flashcard game (POST review 200 sau fix), premium + checkout QR, search (sau fix BUG #2), leaderboard (46 học viên, badge "Bạn"), speaking list 5 đề + đề 50006 + recording studio ("Bật micro" — từ chối mic không crash, không lỗi console), profile (streak, heatmap T2–CN, gói thành viên), admin dashboard (46/1469/43731/5 khớp DB), admin lessons (CRUD thật: tạo → xóa), admin users (toggle premium ON→revoke về trạng thái gốc), admin exercises (20 rows + bộ lọc loại/độ khó).
- **T17 playwright**: login user + search "sunny" → render /ˈsʌni/, 3 part-of-speech, ví dụ, Syn — khớp chrome-devtools.
- **T18 design-system grep**: `Outfit`/`Plus Jakarta Sans` = **0** lần; `Be Vietnam Pro` ở đúng 4 file (index.html, design-system.css, App.vue, AdminLayout.vue); `--geo-*` 699 lần; `shadow-pop*` 201; `prefers-reduced-motion` 6 → **đạt**.
- Console: không có error; 1 cảnh a11y low-priority "A form field element should have an id or name attribute" (39 lần ở tab exercises — input render động, không chặn chức năng).

### 1.3 Database trong Docker (engflow-sqlserver)
> Port note: trong suốt đợt audit này SQL Server host port là **1434**; sau khi audit (2026-09-02) đã đổi sang **1433** (commit `11dfaa8`) — kết nối từ ngoài giờ dùng `localhost:1433`.
- Kết nối `sa` OK; DB `english_learning`; đếm rows khớp admin dashboard.
- **Tối ưu hiệu năng đã áp dụng**: index `idx_exercises_lesson_type` (lesson_id, exercise_type), `idx_lesson_blocks_section`… (tạo bằng sqlcmd với `SET QUOTED_IDENTIFIER ON`), Redis cache dictionary, RestTemplate timeout đúng.
- **Hiệu năng đo được (bằng chứng P5)**: dictionary trước khi sửa **19,5–21,9s mỗi lần** (browser direct, luôn timeout/fallback) → sau khi sửa: call đầu ~20s (cold), các call sau **23–258ms** (Redis, 4 keys `dictionary:*`). Còn lại: `payment.status` ~3,5s trong cửa sổ poll — đo được, chưa tối ưu (xem §4).

### 1.4 AI (Ollama local)
- Sinh bài qua `AiExerciseService` (qwen2.5:1.5b) đã xác minh vòng trước: 202 + Redis progress, JSON salvage 3 lớp, MATCHING slash-pair repair, dedup; AI vocabulary `{topic, level, count}` OK. Không chạy lại lâu ở vòng này (đã PASS trong Phase 1–4 của audit-v3).

## 2. Bug đã tìm thấy & cách sửa tận gốc (đã commit)

| # | Bug | Nguyên nhân gốc | Cách sửa | Commit |
|---|---|---|---|---|
| 1 | Dictionary tra từ luôn fail/timeout | `RestTemplate` read timeout 5s < ~20s upstream thật; browser direct không dùng được cache | `RestTemplateConfig` dùng `SimpleClientHttpRequestFactory` 3s/30s; tách `DictionaryService` (`@Cacheable` Redis 1h, cache String thuần — tránh tự-gọi qua proxy & lỗi serialize `ResponseEntity`) | `60316f4` |
| 2 | Search page chờ 8s vô ích rồi báo sai "Không tìm thấy" | `vocabularyService.search()` thử direct-fetch 4s×2 từ browser trước, rồi mới tới proxy; axios default timeout 10s cắt proxy cold-cache ~20s | Đảo thứ tự: **proxy backend là đường chính** (cache 1h dùng chung), direct chỉ là fallback; riêng call proxy đặt `timeout: 32000`; cập nhật message TIMEOUT | `9b9181f` |
| 3 | Dấu tiếng Việt thành "?" ("N?i dung bài h?c") ở tiêu đề section | `lesson_sections.title` là **varchar** — ký tự ngoài CP1252 thành `?` khi insert (mọi cột text khác đều nvarchar) | `ALTER COLUMN title NVARCHAR(255)` (drop/tạo lại index phụ thuộc) + UPDATE 8 rows hỏng về "Nội dung bài học" + pin entity `columnDefinition = "NVARCHAR(255)"` | `6b2ff2a` |
| 4 | SePay spam log 401 mỗi ~300ms | Token trong .env hết hạn nhưng polling fallback vẫn retry 8 order treo | Circuit breaker `volatile authDisabled`: 401/403 đầu tiên mở breaker tới khi restart; webhook vẫn là kênh chính | `30f2e1a` |
| 5 | Flashcard game không lưu SRS | `flashcardService.reviewFlashcard` là dead code — component không gọi | `FlashcardGame.vue` fire-and-forget `reviewFlashcard(vocabId, rating!=='again')` trước khi chuyển thẻ | `2e8227d` |
| 6 | Test security hard-code lesson 444 | Lesson 444 bị xóa (sự cố §3) → `correctAnswer` JSON path fail | Test chọn động lesson có exercises (`lessonWithExercisesId()`), fallback 444L | `6b2ff2a` |
| 7 | DB_PASSWORD hard-code trong properties | Mặc định trong `application.properties` rò secret mặc định | `${DB_PASSWORD}` không default, resolve qua `spring.config.import=optional:file:.env[.properties]`; thêm `.env.example`; `.gitignore` chặn `.env*` | `f9ca0cd` (trước §2) |

## 3. Sự cố trong quá trình audit — đã xử lý minh bạch

**Sự cố**: khi test nút xóa trong Admin Lessons (UI), dialog xác nhận của trình duyệt bị auto-accept trong automation → `DELETE /api/admin/lessons/444` **204** — xóa nhầm lesson thật "English Grammar Exercises for A1 – be, possessives and pronouns" (cascade: exercises + sections/blocks của nó).

**Xử lý phục hồi (không mất dữ liệu)**:
1. Tìm nguồn dữ liệu gốc: `crawler/data/a1-grammar.json` (dữ liệu crawl ban đầu) chứa **đủ HTML 33.657 ký tự** của lesson này.
2. Tạo lại lesson qua Admin API → id mới **91900**; thêm section "Nội dung bài học" + block TEXT với HTML đã làm sạch (strip `<script>/<style>/<ins>/comments` — script `scripts/clean-lesson444-block.js`).
3. Re-seed exercises: `POST /api/admin/exercises/seed?force=false` → **41 exercises** mới cho 91900.
4. Xác minh qua API và UI: title/en-dash đúng, nội dung render sạch, 41 exercises, sửa thêm BUG #3 trước để title tiếng Việt chuẩn.
5. Dọn dẹp: xóa lesson test "Audit v3 Test Lesson" (91899) sinh ra khi test CRUD.
- **Tác động còn sót**: exercise_attempts/user_progress của lesson 444 vốn là 0 rows (không mất progress ai); snapshots chỉ tồn tại cho lesson 567 (không liên quan). Lesson mới có id 91900 thay vì 444 → mọi tham chiếu ngoài (nếu có) cần biết id mới. Scripts phục hồi giữ lại tại `scripts/restore-lesson444.js`, `scripts/clean-lesson444-block.js`.

## 4. Chưa làm / còn tồn (không chặn nghiệm thu)

1. **Premium thanh toán thật**: bạn chọn **bỏ qua** chuyển tiền thủ công. Order `ENG15C67AA3B771` (10.000đ, MBBank 0706718329) còn treo ở trạng thái pending; SePay polling đang circuit-open (401 = `SEPAY_API_TOKEN` trong .env không hợp lệ). Khi có token hợp lệ + webhook SePay trỏ tới server thì luồng hoàn tất mới test end-to-end được.
2. **`payment.status` ~3,5s**: đo được trong cửa sổ poll; nguyên nhân là chu kỳ polling + cache — đề xuất Webhook-driven hoặc giảm poll interval có chủ đích (chưa sửa trong đợt này).
3. **Listening thiếu audio**: 89/449 bài không có `audio_url` (sinh khi MCP venv chưa có supertonic) — fallback giọng máy trình duyệt đang hoạt động (`frontend/src/utils/speech.js`); muốn lấp đủ phải chạy lại `generate_listening` qua MCP.
4. **A11y low-priority**: form fields render động thiếu `id`/`name` (39 input ở tab exercises) — nên thêm id sinh tự động.
5. **Constitution update nhỏ**: test baseline thực tế là **221** (constitution ghi 196) — cần sửa số trong kỳ backup constitution tiếp theo.
6. **Lesson_blocks chứa "?" cũ** (block_id 8, 10011, 10013): chỉ thấy dấu hiệu ở vòng trước, chưa rà từng block; đa số content lớn còn lại là NVARCHAR chuẩn.

## 5. Kiểm thử vòng 2 (sau toàn bộ fix) — GREEN

| Suite | Kết quả |
|---|---|
| Backend `mvnw.cmd test` | **221/221 PASS, BUILD SUCCESS** |
| Frontend `npx vitest run` | **73/73 PASS (14 files)** |
| `npx vite build` | clean (9,77s) |
| Docker rebuild | backend + frontend đã `--build` và verify |
| UI smoke sau fix | login, search "garden" (1s, cache hit), lesson 91900, leaderboard, speaking, profile, admin CRUD — không lỗi console |

## 6. Commit (Conventional Commits, tiếng Anh)

- `60316f4` perf(dictionary): backend proxy with 30s timeout and 1h Redis cache
- `9b9181f` fix(search): use backend dictionary proxy first, axios timeout 32s
- `6b2ff2a` fix(lessons): NVARCHAR section title; tests no longer hardcode lesson 444
- `30f2e1a` fix(payments): circuit-break SePay polling on 401 to stop log spam
- `2e8227d` fix(flashcards): persist SRS review from game answers
- `f9ca0cd` fix(config): DB_PASSWORD resolved from .env via spring.config.import, no hardcoded default
- `20edf2d` docs(audit-v3): speckit artifacts and lesson-444 restore scripts

## 7. Skills đã load trong phiên

- `prompt-master` — vá 5 lỗ hổng của design prompt thành P6 constitution
- `speckit-workflow`, `speckit-constitution`, `speckit-specify`, `speckit-clarify`, `speckit-checklist`, `speckit-plan`, `speckit-tasks`, `speckit-implement`, `speckit-converge`, `speckit-analyze`
- `addyosmani-browser-testing-with-devtools` — UI audit qua chrome-devtools-mcp
- `addyosmani-debugging-and-error-recovery` — root-cause các bug #1–#7
- (Mặc định của harness: `editing-cordis-compositions` không dùng vì không đụng composition)

## 8. Kết luận

Audit-v3 đạt mục tiêu: **toàn bộ API chính + UI các chức năng chính đã chạy thật trên môi trường Docker**, 7 bug gốc đã sửa và commit, DB đã tối ưu + sửa schema lỗi tiếng Việt, hiệu năng dictionary cải thiện ~100x khi cache hit, hai suite test xanh 221/221 + 73/73 sau vòng 2. Các mục còn tồn (§4) đều đã ghi rõ nguyên nhân và hướng xử lý; bug thật còn lại sẽ tạo GitHub issues theo bước T23.
