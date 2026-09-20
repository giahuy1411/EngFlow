# Gate P1 — ĐÃ ĐẠT (verified 2026-09-19 23:2x +07)

## Kết quả chạy thật

```
./mvnw.cmd -o test
[INFO] Tests run: 438, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS
```

Chạy lặp lần 2 để loại trừ flakiness: **68 test class, 0 dòng `Failures: [1-9]` hoặc `Errors: [1-9]`.**

## So với baseline đỏ lúc vào audit

| Chỉ số | Baseline | Sau khi sửa | Ghi chú |
|---|---|---|---|
| Tests run | 432 | **438** | +6 test mới (4 F115 + 1 F122 SQL + 1 F122 unit) |
| Failures | 3 | **0** | ✅ |
| Errors | 2 | **0** | ✅ |
| Skipped | 10 | 11 | +1: `StudySqlIntegrationTest` cần `ENGFLOW_STUDY_TEST_PASSWORD` |

**Counts đọc từ run log, KHÔNG đọc từ `target/surefire-reports` XML** (theo C13 — XML cũ từng gây sai lệch +8).

## 5 test đỏ ban đầu — kết quả từng cái

| Test | Trạng thái | Xác nhận bằng |
|---|---|---|
| `ExerciseServiceGradingTest.submitSavesAttemptWhenGradeable` | ✅ PASS | `@Mock StudyActivityService` bổ sung |
| `LeaderboardServicePaginationTest.assignsGlobalRanksAcrossPages` | ✅ PASS | `@Mock StreakService` bổ sung |
| `StreakReminderSchedulerTest.redisFailureMustNotSendWithoutDeduplication` | ✅ PASS | F117 fail-closed đã implement |
| `StreakReminderSchedulerTest.retryBudgetStopsAfterThreeAttempts` | ✅ PASS | F116 `retryBudgetExhausted()` đã viết |
| `AuditV9DraftLessonGradeGuardTest.publishedLesson_submit_studentStill200` | ✅ PASS | **Sau khi deploy schema** — xem bên dưới |

## Test F122 — xác nhận bằng stack trace, không suy đoán

**Trước khi deploy schema**, test `AuditV9DraftLessonGradeGuardTest.publishedLesson_submit_studentStill200` vẫn đỏ với:

```
java.lang.IllegalStateException: Study policy is missing; apply the reviewed deployment SQL first
	at StudyActivityService.lambda$effectiveFrom$0(StudyActivityService.java:135)
	at StudyActivityService.effectiveFrom(StudyActivityService.java:134)
	at StudyActivityService.recordStudy(StudyActivityService.java:50)   ← dòng 50
	at ExerciseService.submitExercises(ExerciseService.java:325)
```

**Điểm mấu chốt:** frame là **dòng 50** (`effectiveFrom()`), **KHÔNG phải dòng 51** (nhánh `today.isBefore(start)` mà F122 đã sửa). Nghĩa là:

- **F122 đã sửa đúng** — nhánh before-cutover không còn ném.
- Lỗi còn lại là **thiếu row `study_policy`** — đúng cái deployment gate mà `deploy.sql` giải quyết.

Sau khi deploy: **5/5 test class này PASS.**

## Deploy schema — đã thực hiện

### Trạng thái phát hiện (Phase 0.3)

Hai bảng **đã tồn tại** nhưng **trống**:

```
STUDY_TABLES=study_days,study_policy
POLICY_ROWS=0
STUDY_DAYS_ROWS=0
```

Đúng như `tasks/streak-study/README.md` ghi: *"Hibernate update can create tables, but deliberately does not create the policy row."* → Đây chính là lý do test cuối cùng đỏ.

### Backup (bắt buộc trước DML)

```
BACKUP DATABASE english_learning TO DISK='/var/opt/mssql/backup/engflow_2026-09-19-audit-v10.bak'
  WITH COMPRESSION, CHECKSUM, INIT
→ BACKUP DATABASE successfully processed 25066 pages in 0.389 seconds

RESTORE VERIFYONLY FROM DISK='...' WITH CHECKSUM
→ The backup set on file 1 is valid.
```

**SHA256:** `d71bfec163d29aa768c49847f0475160815290ce83deff8424e19a4c2d49ad5c`
**Kích thước:** 36,999,168 bytes

### Deploy cutover

```
STUDY_POLICY_EFFECTIVE_FROM=2026-09-20
```

### Verify schema độc lập (Phase 1.10)

```
POLICY=2026-09-20
FK=fk_study_days_user -> users
UQ=uq_study_days_user_date
COLS=id,study_date,user_id
```

## ⚠️ F124 — `deploy.sql` bỏ sót FK khi bảng đã tồn tại (finding mới)

**Vấn đề:** `deploy.sql` dòng 40-47 chỉ tạo FK **bên trong** khối `IF OBJECT_ID(N'dbo.study_days', N'U') IS NULL`. Khi Hibernate đã tạo bảng trước (đúng tình huống thực tế ở đây), khối đó bị bỏ qua → **FK không bao giờ được tạo**.

Bằng chứng đo được:
```
FK_COUNT=0        ← sau khi chạy deploy.sql
```

**Đã bổ sung thủ công:**
```sql
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('dbo.study_days'))
    ALTER TABLE dbo.study_days ADD CONSTRAINT fk_study_days_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
→ FK_ADDED
```

**Verify lại:** `FK=fk_study_days_user -> users` ✅

**Ý nghĩa:** đây là lỗi idempotency của script deploy — script không đạt được mục tiêu của chính nó trong trường hợp phổ biến nhất (Hibernate đã dựng bảng). Cần sửa `deploy.sql` để tách phần FK ra ngoài khối `IF`.

## Frontend — đã chạy

```
npx vitest run
→ Test Files  21 passed | 1 skipped (22)
→ Tests      106 passed | 1 skipped (107)

npx vitest run src/components/decor/__tests__/decor-props.test.js
→ 10 passed (10)          ← F118 xác nhận

npx vite build
→ ✓ built in 8.35s
→ index-DKADIgBc.js  177.31 kB │ gzip: 67.50 kB
```

Bundle: baseline cũ 176.80 kB → **177.31 kB** (+0.51 kB, do icon ArrowRight + component divider mới). Mức tăng hợp lý, không bất thường.

## Parity line — KHỚP CHÍNH XÁC 100%

Đo bằng đúng 10 cột của `sweep/v8/p16-parity.sql` (định nghĩa chuẩn, không phải tự nghĩ ra):

```
1471|43737|76|127|28|15|4|126|14|5
```

**Giống hệt baseline công bố.** Ý nghĩa: việc deploy `study_policy` (thêm 1 row) + thêm FK `fk_study_days_user` **không làm thay đổi** bất kỳ bảng nào trong 10 bảng theo dõi. Không có dữ liệu nào bị mất hay thêm ngoài dự kiến.

Chi tiết 10 cột: lessons=1471, exercises=43737, users=76, vocabulary=127, speaking_submissions=28, video_attempts=15, lesson_submissions=4, payment_transactions=126, decks=14, lesson_snapshots=5.

> Ghi chú phương pháp: lần đo đầu tôi tự chọn 10 bảng khác (dùng `speaking_prompts`, `video_lessons`, `study_days`…) nên không so trực tiếp được với baseline. Phải đọc `p16-parity.sql` để lấy đúng định nghĩa. Ghi lại đây để vòng sau không lặp lại sai sót này.

## Việc còn lại sau Gate P1

| Việc | Trạng thái | Lý do |
|---|---|---|
| Copy backup ra host | ⏳ CHƯA | `docker cp` không khớp allowlist |
| `docker compose up -d --build backend` | ⏳ CHƯA | `docker compose` không khớp allowlist |
| `StudySqlIntegrationTest` (7 test) | ⏳ SKIPPED | Cần `ENGFLOW_STUDY_TEST_PASSWORD`; chạy qua `verify-sql.ps1` |
| Browser sweep 5 viewport | ⏳ CHƯA | MCP bị classifier chặn |
| Phase 3/4/5/R2 | ⏳ CHƯA | — |

**Lệnh cần thêm vào allowlist để hoàn tất:**
```
"Bash(docker cp *)"
"Bash(docker compose up -d --build backend)"
"Bash(docker compose *)"
"Bash(powershell.exe -NoProfile -ExecutionPolicy Bypass -File *)"
```
