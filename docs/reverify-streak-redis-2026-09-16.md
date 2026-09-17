# Re-verify: Streak + Mail nhắc học + Lịch Profile + Redis + Liên kết chức năng (16/09/2026)

Phương pháp: đọc source trực tiếp, grep toàn repo, chạy test mục tiêu.
Kết quả test sau sửa: backend **26/26 PASS** (`RateLimitFilterTest` 14 gồm test B4 mới,
`StreakReminderSchedulerTest` 11 gồm 3 test B2 mới, `DashboardServiceTest` 1 mới; `BUILD SUCCESS`),
frontend **90/90 PASS** (gồm `Profile.test.js` mới — đã kiểm đỏ với code cũ, xanh với fix).

**TẤT CẢ 6 MỤC ĐÃ SỬA XONG (16/09/2026, xem chi tiết từng mục).**
B3 là ops (giám sát, không code). B6 ghi nhận, không sửa.

## Kết luận nhanh

| # | Vấn đề | Mức | Trạng thái |
|---|---|---|---|
| B1 | Header Profile hiện streak thô vs lịch hiệu lực | Trung bình (hiển thị) | ✅ ĐÃ SỬA: `Profile.vue:19` dùng ref `currentStreak` từ `/api/streak/current`; test `Profile.test.js` (đỏ→xanh đã kiểm) |
| B2 | Retry scheduler gửi trùng mail | Thấp | ✅ ĐÃ SỬA: key `streak:sent:<ngày>:<userId>` (TTL 2 ngày), check trước + mark sau mỗi lần gửi thành công; 3 test mới trong `StreakReminderSchedulerTest` |
| B3 | Redis `allkeys-lru` đuổi được key TTL | Thấp (hạ tầng) | ⚠️ OPS: theo dõi `used_memory` (alert > 70%), tăng maxmemory khi cần; không đổi code |
| B4 | Rate-limit key rò rỉ mất TTL | Rất thấp | ✅ ĐÃ SỬA: `RateLimitFilter` tự xoá key best-effort khi `EXPIRE` lỗi (giữ nguyên fail-open); test mới trong `RateLimitFilterTest` |
| B5 | Dashboard đếm cả bài nháp | Nhỏ | ✅ ĐÃ SỬA: `DashboardService` dùng `countByIsPublishedTrue()`; test mới `DashboardServiceTest` |
| B6 | 2 overload `isComebackSuppressed` trùng nhau; mail comeback dùng streak thô làm số hiển thị | Nhỏ | Ghi nhận, không cần sửa gấp |
| — | Thuật toán streak, query at-risk/broken, cron, catch-up, fail-open Redis, lịch Profile, a11y | — | ĐÚNG từ đầu, không đụng |

## B1. Lệch số streak trên trang Profile (đã sửa 16/09)

`UserService.mapToUserResponse()` dòng 326 dùng giá trị **thô** trong DB:

```java
.currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
```

Nguyên nhân: `UserService.mapToUserResponse()` trả giá trị **thô** trong DB (chỉ cập nhật khi user quay lại nên “treo” sau nhiều ngày nghỉ), `Profile.vue:19` từng hiện thẳng số này từ store, trong khi `StreakCalendar` (`Profile.vue:44`) dùng ref `currentStreak` từ `GET /api/streak/current` = **effective** (gap > 1 ngày → 0). Kịch bản trước fix: streak 5, nghỉ 3 ngày, mở Profile → header vẫn “5”, lịch hiện “0”, cùng trang 2 số khác nhau.

Đã sửa 16/09 (1 dòng, không đụng backend): `Profile.vue:19` đổi `user.currentStreak` → ref `currentStreak` đã có sẵn từ `/api/streak/current`. Test `frontend/src/views/Profile.test.js` bao regression (đỏ với code cũ, xanh với fix). `Lessons.vue` và `DashboardService` vốn đã dùng endpoint effective nên đúng từ đầu; `LeaderboardService:66` và `AdminService:124` vẫn dùng số thô — leaderboard xếp bằng điểm, admin xem nội bộ, chấp nhận được.

## B2–B4. Scheduler, Redis hạ tầng, rate-limit

**B2 — gửi trùng khi retry.** `runReminderJob()` xoá marker ngày khi SMTP lỗi 1 user bất kỳ để “retry cùng ngày”,
nhưng lần chạy lại gửi cho **toàn bộ** danh sách (nhóm at-risk không có suppression nào; nhóm broken chỉ
chặn theo 30 ngày *sau khi gửi thành công*). Xác suất thấp (cần SMTP lỗi đúng ngày + restart/catch-up cùng ngày),
hậu quả là user nhận 2 mail. Đã sửa đúng hướng này: key `streak:sent:<yyyy-MM-dd>:<userId>` (TTL 2 ngày, `STREAK_SENT_PREFIX`),
check trước mỗi `sendStreakReminder` / `sendStreakComebackReminder`.

**B3 — `allkeys-lru` đuổi key TTL.** `docker-compose.yml`: `redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru`.
Mọi key quan trọng đều có TTL (`user:login_days:*` 90 ngày, `streak:reminder:*` 2 ngày, `streak:comeback:*` 30 ngày,
`otp:*`, `login_fail/lock:*`, `rate_limit:*`, `game:session:*` 24h, `game:points:*` 25h) nên đều là ứng viên bị đuổi
khi đầy 256MB → mất lịch học, gửi mail 2 lần, OTP vô hiệu, bypass rate-limit, mất session game giữa chừng.
Hiện tại data nhỏ nên chưa đau. Hành động: theo dõi `used_memory` (alert > 70%), cân nhắc tăng maxmemory;
đổi policy cần thiết kế riêng (không đổi bừa `volatile-lru` vì cache dictionary `@Cacheable` có thể không TTL
và sẽ không bao giờ bị đuổi → phản tác dụng).

**B4 — INCR/EXPIRE không nguyên tử** (`RateLimitFilter.java:161-164`): `expire` chỉ gọi khi `count == 1`;
nếu lệnh expire lỗi sau khi INCR thành công, key tồn tại vĩnh viễn → IP đó kẹt 429 ở bucket đó tới khi flush tay.
Đã sửa theo hướng giữ nguyên call-pattern (không đụng test F91/F92): `EXPIRE` lỗi → xoá key best-effort + fail-open, thay vì Lua.

## B5. Dashboard đếm cả bài nháp

`DashboardService.getDashboardStats()`: `lessonRepository.count()` (tất cả) vs `ProgressService`: `countByIsPublishedTrue()`.
Khi có bản nháp, “tổng số bài” ở Dashboard ≠ “tổng số bài” ở trang tiến độ. Đã sửa: dùng `countByIsPublishedTrue()`.

## Phần ĐÚNG (đã kiểm, khỏi sửa)

- **Thuật toán streak** (`StreakService.recordAccess`): idempotent trong ngày (no-op, không đụng Redis);
  null → 1; cách đúng 1 ngày → +1; còn lại → 1. `checkin()` chỉ delegate (tham số cũ giữ cho tương thích).
- **Query mail**: at-risk = `lastStudyDate == hôm qua` (đúng user, đúng active);
  broken = `IS NOT NULL AND < hôm qua` (loại never-studied). Chính xác theo spec.
- **Scheduler**: cron `0 0 20 * * *` zone `Asia/Ho_Chi_Minh`; marker ngày chống double-send khi restart;
  catch-up sau boot muộn (compose đã bật `ENGFLOW_SCHEDULER_CATCHUP_ENABLED=true`; mặc định code `false` cho test/dev).
- **Fail-open Redis toàn diện**: login, streak, history, scheduler marker/suppression, rate-limit, OTP đều try/catch —
  Redis chết thì app vẫn chạy, chỉ mất tính năng phụ. Không chỗ nào NPE vì Redis null.
- **Múi giờ nhất quán**: `Clock` bean `systemDefaultZone()` + container `TZ=Asia/Ho_Chi_Minh` → naive-VN đúng quy ước repo.
- **`@EnableScheduling`** có mặt (`EngflowApplication.java:18`); cron đang sống.
- **App mount → `/api/auth/me` → `getProfile` → `recordAccess`** (`App.vue:52-55`, comment trong code ghi rõ) —
  vào app là tính ngày học. 3 điểm kích hoạt duy nhất (đã grep): login, `/me`, nộp game (`GameService.java:258`,
  gọi sau khi chốt điểm + cap 100/ngày, trước khi lưu điểm — thứ tự đúng).
- **Lịch Profile** (`StreakCalendar.vue`): neo ngày bằng `today` server (fix lỗi parse UTC lệch 1 ngày),
  lưới 4 tuần T2–CN, `role=grid/gridcell` + `aria-label` từng ô (không chỉ truyền bằng màu, WCAG 1.4.1),
  `history` 30 ngày vào `Set` tra O(1). Không lỗi logic.
- **Mail**: `MAIL_USERNAME/PASSWORD` đã set trong `.env` (kiểm tra presence, không in giá trị);
  streak-mail throw để scheduler retry, OTP-mail fail-soft để không lộ trạng thái email — phân biệt đúng.
- **Nộp bài lesson không chạm streak** (đúng thiết kế “ngày mở app/chơi game”, tránh cày streak bằng spam nộp bài).
- **AI progress là `ConcurrentHashMap` in-memory** (`AiExerciseService.java:57`), KHÔNG phải Redis như AGENTS.md/memory
  từng ghi — single-container ổn, multi-instance sẽ mất tiến độ poll. Đây là **lệch tài liệu**, không phải bug app;
  đề xuất sửa dòng tương ứng trong AGENTS.md thay vì sửa code.

## Bản đồ Redis toàn app (đã grep hết `redisTemplate.*` + injections)

| Prefix / key | TTL | Dùng ở | Mất khi Redis chết / bị đuổi |
|---|---|---|---|
| `user:login_days:<id>` (Set ngày ISO) | 90 ngày | `StreakService` ghi + đọc lịch | Lịch Profile trống (`[]`), streak số vẫn đúng (DB) |
| `login_fail:<email>` / `login_lock:<email>` | 15 phút | `UserService.login` | Mất chống brute-force tạm thời (fail-open) |
| `otp:reset:<email>` / `otp:rate:<email>` | 10 / 15 phút | Quên mật khẩu | OTP vô hiệu / mất giới hạn 3 OTP |
| `rate_limit:<ip><bucket>` | 1 phút | `RateLimitFilter` | Mất giới hạn tốc độ tạm thời |
| `game:session:<id>` | 24h | `GameService` tạo quiz | Mất ván game đang chơi dở |
| `game:points:today:<id>` | 25h | Cap 100 điểm/ngày | Cày quá cap trong ngày |
| `streak:reminder:<ngày>` | 2 ngày | Scheduler chống double-send | Có thể gửi mail 2 lần |
| `streak:comeback:<userId>` | 30 ngày | Chống spam mail quay lại | Spam mail comeback |
| Spring cache `dictionary` | (xem `RedisConfig`) | `DictionaryService` proxy từ điển | Tra từ chậm lại (~20s cache lạnh) |

## Bản đồ liên kết chức năng (luồng → chạm nhau ở đâu)

```
Đăng nhập ──recordAccess──→ Streak (số+lịch)
   │  └─ fail/lock (Redis)      ▲
   │                            │  App mount → GET /me ──recordAccess──┘
   └─ JWT → guard mọi route authenticated (nộp bài, attempts, streak, vocab POST)

Nộp game ──checkin──→ Streak + cộng totalPoints (cap 100/ngày, Redis) ──→ Leaderboard (xếp bằng điểm)
Nộp bài lesson ──→ ExerciseAttempt (KHÔNG chạm streak) ──→ Progress/isCompleted ──→ Dashboard/Trang tiến độ
Tra từ ──→ dictionary cache (Redis) ──→ fallback DB
20:00 cron ──→ query streak ──→ Gmail SMTP ──→ user quay lại app ──→ recordAccess ──→ streak +1
```

Điểm gãy liên kết duy nhất đáng kể là **B1** (số thô vs số hiệu lực hiển thị khác nhau).
Còn lại các biên đều có test hoặc guard: submit đòi auth (F54), đáp án chỉ admin (403), nháp trả 404,
upload chặn extension thực thi (F81), mail streak/mã OTP tách chính sách lỗi.

## Thứ tự đã sửa (16/09)

1. ✅ B1 — `Profile.vue:19` (xong, có test).
2. ✅ B5 — `DashboardService` (xong, có test).
3. ✅ B2 — idempotency key theo user (xong, 3 test).
4. ✅ B4 — tự dọn key khi EXPIRE lỗi (xong, 1 test).
5. ⚠️ B3 — alert memory Redis + tăng maxmemory khi cần (ops, không code).
6. ✅ AGENTS.md: AI progress là in-memory (xong).

## Phụ lục verify live bằng browser + API (17/09, sau rebuild backend)

Backend rebuild `docker compose up -d --build backend` (image `64758cd6524d`), boot OK 00:46.
Test: backend targeted **26/26 PASS** (`RateLimitFilterTest` 14 + `DashboardServiceTest` 1 +
`StreakReminderSchedulerTest` 11, log `sweep/v8/t-verify-fixes3.log` đã dọn),
frontend full **19 files / 90/90 PASS** (gồm `Profile.test.js` mới).

Script: `sweep/v8/ui/verify-fixes.js`, `sweep/v8/ui/verify-b1-stale.js`;
evidence: `sweep/v8/evidence/verify-fixes/` (`run.log`, `run-drift.log`, `run-stale.log`,
`verify-fixes.json`, `profile.png`, `profile-stale.png`, `check.sql`/`drift.sql`/`restore.sql`).

| Check | Kết quả live |
|---|---|
| B1 steady-state (`user@gmail.com`, streak 6) | Header `["6","55","N/A"]` == effective 6 == lịch 6, 28 gridcells, 0 console error — PASS |
| B1 stale-tab (seed `localStorage.user.currentStreak=99`, DB lùi 3 ngày, không login lại) | Header hiện số từ API, **không** hiện 99 — PASS (code cũ sẽ hiện 99) |
| B5 dashboard | API `totalLessons=1465` == `COUNT is_published=1` (tổng 1471, 6 nháp bị loại) — PASS |
| Drift 3 ngày | `GET /api/streak/current` → 0, DB raw giữ 6 (chỉ heal khi có request ghi) — đúng thiết kế |
| Heal-on-visit | Mở `/profile` → `App.vue` mount → `fetchUser` → `/api/auth/me` → `recordAccess` → streak reset 1; header hiện 1 (đã heal), không hiện 99 — đúng thiết kế |
| Redis | `used 1.72M/256M` (0.7%, B3 chưa đau); `rate_limit:*` 0 key rò; `streak:sent:*` 0 key (cron 20:00 chưa chạy trên bản mới) |

Boundary trung thực: B2/B4 chỉ bao bằng unit test (cron 20:00 và lỗi EXPIRE ép buộc không tái hiện live);
DB `user@gmail.com` đã restore `streak=6, last_study_date=2026-09-17`; JWT tạm trong `token.txt` đã xóa.
