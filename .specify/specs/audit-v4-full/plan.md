# Plan — audit-v4-full

## Tech context (hiện trạng đã xác minh 2026-09-03)
- Docker: 7 container healthy. Backend :8080 (401 tại /api/health = JWT chặn — đúng),
  frontend :5173, SQL Server :1433, Redis :6379, MinIO :9000-9001, Whisper :9002.
- Backend Maven ở repo ROOT (`src/main/java/com/datn/engflow`, không phải backend/src).
- 26 controllers; payment: `POST /api/webhook/sepay`, `POST /api/v1/payment/create-order`,
  `GET /api/v1/payment/status` (`PaymentService`, `SePayApiService`).
- .env CÓ `SEPAY_API_TOKEN` (v3 ghi 401 → kiểm tra live lại; token có thể đã đổi).
- DB `english_learning`: 19 bảng; exercises 43.735, lessons 1.469, users 46,
  payment_transactions 109, vocabulary 127.

## Approach theo phase

### Phase A — Baseline & DB sweep (task T1–T3)
- A1 `mvnw.cmd test` (nền, đã chạy), A2 `vitest run` + `vite build`.
- A3 DB sweep bằng sqlcmd (`MSYS_NO_PATHCONV=1`): 
  1. Rà NVARCHAR: `SELECT ... FROM sys.columns WHERE ... collation_name='SQL_Latin1_General_CP1_CI_AS'` 
     trên các bảng text-heavy (lesson_blocks, lesson_sections, lessons, exercises, vocabulary...).
  2. Grep literal `?` bất thường trong lesson_blocks.content (pattern `% ? %` giữa từ).
  3. Orphan check: exercise_attempts→lessons, payment_transactions→users, deck_words→decks/vocabulary.
  4. Index hiện có: `sys.indexes` so với query nóng (đã có idx_exercises_lesson_type từ v3).
  5. Dump bảng cần ALTER ra file trước khi đổi (safeguard spec §5).

### Phase B — API full sweep (T4)
- Script PowerShell ghi file UTF-8 no-BOM + `--data-binary "@file"` (gotcha AGENTS.md).
- Login user + admin lấy JWT. Sau đó sweep theo nhóm controller (26):
  positive + negative (401/403/404/400). Ghi kết quả bảng.
- Đo thời gian `GET /api/v1/payment/status` trong cửa sổ poll (baseline 3,5s của v3).

### Phase C — Browser UI sweep (T5–T6)
- chrome-devtools-mcp (skill addyosmani-browser-testing-with-devtools) tại :5173.
- Views: user (Home, Lessons, LessonContent/Exercise, Video, Decks + 6 games,
  Vocabulary/Search/AiVocab, Speaking 3 bước, Leaderboard, Profile, Premium 2 trang)
  + admin (Dashboard, Users, Lessons, VideoLessons, Exercises, Speaking×2, Submissions,
  LessonBuilder, VideoAttempts). Mỗi view: render + network 4xx/5xx + console.
- Cross-check playwright-mcp 1 luồng.
- Verify design: computed font-family = "Be Vietnam Pro", grep tokens, screenshot 375/1440.

### Phase D — Payment thật (T7) ⚠ GATE USER
- Tạo order qua UI (PremiumPage → PremiumCheckout) → hiện đủ thông tin CK →
  **AskUserQuestion nhắc chuyển khoản** → chờ user xác nhận → poll status →
  verify DB `payment_transactions.status` + user premium + UI badge.
- Nếu 401 token: root-cause (token hết hạn/sai format), báo user hướng dẫn fix,
  verify tối đa đường hiện có.

### Phase E — Perf & fix (T8–T9)
- payment.status: đo before → root cause (poll chu kỳ, cache, N+1) → sửa → đo after.
- Mọi bug khác: root cause → fix → test đơn lẻ → commit.

### Phase F — Loop 2 + converge + report (T10–T12)
- Chạy lại full suite (backend + frontend + smoke UI vòng 2 mở rộng hơn vòng 1:
  thêm negative path trên UI, refresh mid-action, 2 role).
- converge: soát spec/plan/tasks vs thực tế, append task thiếu nếu có.
- REPORT.md theo mẫu v3.

## Contracts (API chính sẽ test)
- Auth: POST /api/auth/login|register, GET /api/auth/me
- Lessons: GET /api/v1/lessons, /{id}, structure, snapshot; POST /api/lessons/{id}/submit
- Exercises: GET/POST grade; Admin: /api/admin/exercises*, /api/admin/ai-exercises/generate
- Decks: /api/v1/decks CRUD; Flashcards: /api/flashcards/review
- Games: /api/games/* (quiz từ deck, Redis cap 10)
- Video: /api/v1/video-lessons (public + admin)
- Vocabulary: /api/vocabulary/dictionary/{w}, /api/v1/vocabulary/*
- Speaking: /api/v1/speaking-prompts, /api/v1/speaking-submissions
- Payment: create-order, status, webhook sepay
- Streak/Leaderboard/Progress/SRS/Dashboard: GET tương ứng

## Risk controls
- Không auto-accept confirm dialog. Data test nhãn "Audit v4". Dump trước ALTER.
