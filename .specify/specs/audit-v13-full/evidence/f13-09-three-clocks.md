# F-13-09 — ba đồng hồ (audit-v13 Phase 4, đo lại 2026-09-22)

**Kết luận: GIỮ NGUYÊN, không đổi code.** Tài liệu này ghi lại số đo thật và lý do.

## Số đo (không suy đoán)

| # | Đồng hồ | Bằng chứng đo trong phiên này |
|---|---|---|
| 1 | **JVM backend** | `TZ=Asia/Ho_Chi_Minh` — `docker-compose.yml:52`, `backend.Dockerfile:14` + `-Duser.timezone=Asia/Ho_Chi_Minh` (`:17`). **11** lời gọi `LocalDateTime.now()` trong `src/main` (7 file: ExerciseService, LessonService, PaymentService, ShadowingAiGradingService, SpeakingSubmissionService, SrsService, VideoLessonService) → ghi **naive +07** vào `datetime2` |
| 2 | **SQL Server** | Container **KHÔNG có `TZ`** → `docker exec engflow-sqlserver date` = `Tue Sep 22 11:06 UTC 2026` trong khi giờ VN là 18:06 → engine chạy **UTC**, lệch 7h |
| 3 | **Browser** | không `dayjs`/`date-fns`; `new Date()` ở `StreakCalendar.vue:91` và `LessonPreview.vue:91`; `Profile.vue:103` hardcode `Date.now() + 7*3600000` |

### Sửa lại một chi tiết của báo cáo trước (đo lại mới thấy)

Báo cáo giữa kỳ ghi *"15 DEFAULT `GETDATE()` trong V1/V3/V9"*. **Đo lại: DB live có
`getdate_defaults_in_live_db = 0`.** Các default đó chỉ nằm trong file migration
`src/main/resources/db/migration/*.sql`, mà **Flyway đang disabled** (AGENTS.md) — file chỉ để
tham khảo, **chưa bao giờ được áp**. Nên lo ngại "default constraint ghi UTC lệch ORM" **không
xảy ra trên DB thật**; chỉ còn là rủi ro lý thuyết nếu sau này bật Flyway.

## Vì sao đường streak MIỄN NHIỄM

- `StudyActivityService.today()` (`:139`) = `LocalDate.now(clock.withZone(STUDY_ZONE))` với
  `STUDY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh")` (`:34`) — **ghim zone tường minh**, không tin
  `systemDefaultZone()`.
- `StreakReminderScheduler` cũng ghim (`:70, :77, :78, :179`) + `@Scheduled(zone="Asia/Ho_Chi_Minh")`.
- `study_days.study_date` là **`DATE`** (không sub-day) → không có phần giờ để lệch.
- Đo: **0** lời gọi `LocalDate.now()` trần trong `src/main` — mọi đường date-granular đều qua `Clock`.
- Frontend neo vào `today` do server gửi (`StreakCalendar.vue:80-93`), chỉ fallback `new Date()`
  khi prop vắng.

## Rủi ro còn lại (ghi rõ, không sửa)

Query nào **trộn** `datetime2` do ORM ghi (naive +07) với thời gian **do engine tính**
(`SYSDATETIME()`) sẽ lệch 7h, và gần nửa đêm có thể lệch cả ngày. Hiện **không có query nào như vậy**
trong app — nhưng script SQL viết tay dễ mắc. Quy ước đã có ở `AGENTS.md:57`:
dùng **cùng naive-VN clock** khi so timestamp; `SYSDATETIME()` trong sqlcmd sẽ lệch −7h.

## Quyết định

**Giữ nguyên.** DB có **một nguồn ghi duy nhất** (ORM), tự nhất quán theo giờ VN. Migrate toàn bộ
sang UTC chỉ nên làm khi có **consumer thứ hai** (service khác đọc DB, hoặc reporting dùng giờ UTC) —
hiện chưa có. Đổi bây giờ là rủi ro không đổi lại được lợi ích.

`AGENTS.md:57` vẫn đúng — không cần sửa.
