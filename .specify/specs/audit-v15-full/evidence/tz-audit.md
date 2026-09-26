# F-13-09 — ba đồng hồ (audit-v15 Phase 5, đo lại 2026-09-25)

**Kết luận: GIỮ NGUYÊN, không đổi code** (theo quyết định người dùng: "Chỉ verify + ghi docs").
Tài liệu ghi lại **số đo phiên này** (V4: kết luận cũ chỉ là giả thuyết).

## Số đo (không suy đoán)

| # | Đồng hồ | Bằng chứng đo 2026-09-25 |
|---|---|---|
| 1 | **Host** | `2026-09-25 03:00:35 +0700 (SEAST)` |
| 2 | **JVM backend** | `2026-09-25 03:00:36 +07` — `TZ=Asia/Ho_Chi_Minh`; ghi **naive +07** |
| 3 | **SQL Server** | `2026-09-24 20:00:36 +0000 (UTC)`; `SYSDATETIME()` off vs UTC = **0h** → engine **UTC**, lệch **7h** |
| 4 | **Browser** | không `dayjs`/`date-fns`; neo vào `today` server gửi, fallback `new Date()` |

## Writer đo lại

| Đo | Giá trị |
|---|---|
| `LocalDateTime.now()` trong `src/main` | **11** (7 file: ExerciseService, LessonService, PaymentService, ShadowingAiGradingService, SpeakingSubmissionService, SrsService, VideoLessonService) |
| `LocalDate.now()` trần (không qua `Clock`) | **0** ✅ |
| `Clock` bean | `EngflowApplication.clock()` = `Clock.systemDefaultZone()`; dùng bởi AdminService, GameService, PaymentService, PremiumExpiryScheduler, StreakReminderScheduler, StudyActivityService, UserService |
| `getdate_defaults_in_live_db` | **0** (Flyway disabled → default trong `db/migration/*.sql` chưa bao giờ áp) |

## Vì sao đường date-granular MIỄN NHIỄM (đo, không suy đoán)

- `StudyActivityService.today()` = `LocalDate.now(clock.withZone(STUDY_ZONE))`, `STUDY_ZONE = Asia/Ho_Chi_Minh` (ghim tường minh).
- `StreakReminderScheduler`: `@Scheduled(cron=..., zone="Asia/Ho_Chi_Minh")` + ghim zone.
- `study_days.study_date` là **`DATE`** (schema đo được: `id|bigint, study_date|date, user_id|bigint`) → không có phần giờ để lệch.
- Đo DB: `study_days` n=5, `min=2026-09-21`, `max=2026-09-25`; VN today = `2026-09-25`;
  **`FUTURE_STUDY_DAYS = 0`** → streak không bị "ngày tương lai".

## F-15-06 (MỚI, do chính v15 gây ra) — `GETDATE()` trong script migrate

**Phát hiện:** 3 hàng tôi migrate ở Phase 2 dùng `GETDATE()` (SQL container = UTC) → ghi
`created_at = 2026-09-24 19:48` trong khi thời điểm thật là `2026-09-25 02:48 +07`. **Đây đúng là
cái bẫy F-13-09 cảnh báo** — script SQL viết tay dễ trộn UTC của engine với naive-VN của ORM.

**Fix:** `UPDATE … SET created_at = DATEADD(HOUR, 7, created_at)` cho **đúng 3 hàng** đó
(điều kiện `exercise_id IN (787912,787913,787914)`), `BEGIN TRAN` + assert + `COMMIT`.
**Sau fix:** cả 3 hàng = `2026-09-25 02:48:50` (naive-VN) ✅; `@@TRANCOUNT = 0`.

**Bài học:** script SQL v15 từ nay dùng `DATEADD(HOUR, 7, GETUTCDATE())` hoặc literal naive-VN,
**không** `GETDATE()`. Đã ghi vào cleanup notes.

## Quyết định (giữ nguyên như v13)

**Không migrate.** DB có **một nguồn ghi duy nhất** (ORM, naive-VN) → tự nhất quán. Migrate sang UTC
chỉ nên làm khi có **consumer thứ hai** (service khác đọc DB / reporting UTC) — hiện chưa có.
`AGENTS.md:57` vẫn đúng; **không cần sửa** (số "11 writer" khớp đo phiên này).
