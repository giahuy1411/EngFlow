# audit-v13-full — findings

Quy ước: `F-13-NN` (tiếp số v12 kết thúc ở F153). Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy.

Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `N-A`

---

## F-13-01 — Chọn "Trắc nghiệm" nhưng học viên thấy ô điền từ — **HIGH** — `FIXED`

**Người dùng báo trực tiếp.**

### Bằng chứng ban đầu (dữ liệu THẬT)

- Hàng thật: `exercises.exercise_id=777434`, `lesson_id=91920` ("thầy bình"), `exercise_type=MULTIPLE_CHOICE`, `options=["a","b","c","d"]`, `correct_answer='d'`, tạo `2026-09-21 22:01`.
- UI trước fix (Playwright MCP, live): badge **"TRẮC NGHIỆM"** nhưng `mc-option buttons = 0`, `input[type=text] = 2`, placeholder "Nhập câu trả lời...". Ảnh `f13-01-repro-before.png`.

### ⚠️ Đính chính quan trọng — giả thuyết ban đầu của tôi SAI, review chéo bắt được

Bản fix **đầu tiên** của tôi giả định "32 814 hàng MC có options là placeholder chữ cái". **Sai.** SQL đúng cho thấy:

| Đo lại (2026-09-22) | Số |
|---|---|
| `MULTIPLE_CHOICE` tổng | 33 556 |
| **`options IS NULL`** | **32 814 (97.8%)** |
| `options = '[]'` | 101 |
| **options chỉ có chữ cái** | **0** |

→ Hàng 777434 với `["a","b","c","d"]` là **ngoại lệ hiếm**, không phải đa số. Đa số là **options NULL**, và những hàng đó **vốn luôn trả lời bằng cách nhập text** rồi được server chấm theo `correctAnswer` — tức là **hoạt động đúng**.

**Hệ quả:** fix đầu tiên biến 32 814 hàng MC (options NULL) + 319 hàng LISTENING thành **thẻ chết, không trả lời được** — một regression **lớn gấp 44 lần** bug gốc. Review chéo đối kháng đã bắt được trước khi commit.

### Root cause (đúng)

`LessonExerciseTab.vue:170` coi options chỉ chữ cái là placeholder → `null` → `hasOptionChoices()` false → render `<input type=text>`. Với hàng 777434, **loại bài tập hiển thị khác loại admin chọn** — đó là bug thật. Nhưng cơ chế "thiếu options ⇒ nhập text" **không phải bug**: nó là đường trả lời hợp lệ cho hàng legacy.

### Fix cuối cùng (sau review)

- **Render (`LessonExerciseTab.vue`):** `hasOptionChoices()` giữ option chữ cái làm **lựa chọn** (không đổi loại); khi **thiếu options**, vẫn render **ô nhập text** (giữ hàng legacy trả lời được) kèm **cảnh báo nội dung** `role="note"` — **không bao giờ xoá control duy nhất**. `rawOptions()` lọc blank nên không render nút rỗng.
- **Create (FE):** `AdminExercises.vue saveExercise()` từ chối MC mới khi <2 lựa chọn hoặc toàn chữ cái.
- **Create (BE):** `ExerciseService.assertNewChoiceOptionsUsable()` chỉ chạy khi **tạo mới**, hoặc khi **update có gửi options**; **không** validate hàng đang lưu (nếu không sẽ chặn mọi sửa đổi hợp lệ của 32 814 hàng legacy).
- **AI path:** `AiExerciseService.validateSchema()` thêm guard "phải có ít nhất 1 lựa chọn nội dung thật" — đóng lỗ mà model local sinh `["a","b","c","d"]` lọt qua (distinct + size 4).

### Bằng chứng pass

- UI live sau fix: hàng 777434 render **4 nút lựa chọn**, `text input = 0`.
- API thật: payload chữ cái → **400**; payload options thật → **200** (`f13-01-api-verify.json`).
- Hàng legacy `options NULL` → **vẫn có ô nhập text** (test khẳng định).
- `evidence/f13-01.json`, `evidence/f13-01-api-verify.json`, `evidence/e2e-flows.md`.

### Test hồi quy

- `LessonExerciseTab.f13.test.js` — **8 test**, gồm 4 test chống regression (MC options NULL, LISTENING no options, `[]`, blank options) mà bản fix đầu **không có**.
- `ExerciseServiceMultipleChoiceOptionsTest.java` — **11 test**, gồm test "sửa câu hỏi của hàng legacy options NULL **không** bị chặn".
- `contrast-ink-tokens.test.js` (3) cho F-13-03/04/05.

### Review chéo (bắt buộc theo yêu cầu người dùng)

Adversarial review độc lập đã tìm ra: (1) regression 32 814 hàng — **HIGH**, đã sửa; (2) guard bypassable qua AI path — **HIGH**, đã sửa; (3) update path chặn sửa đổi hợp lệ — **MEDIUM**, đã sửa; (4) nút option rỗng — **MEDIUM**, đã sửa; (5) `role="alert"` gây ồn screen-reader → đổi `role="note"`; (6) thông điệp hướng sai đối tượng (nói với học viên "sửa trong trang quản trị") → đổi thành lời nhắc trung tính. **6/6 điểm đã xử lý.**

---

## F-13-03/04/05 — Residual contrast: token vivid dùng làm màu chữ/icon — **MEDIUM** — `FIXED`

**Nguồn:** đo live DOM bằng Playwright MCP (composite alpha bottom-up), không suy đoán.

| # | Vị trí | Trước | Tỷ lệ | Sau |
|---|---|---|---|---|
| F-13-03 | `Profile.vue:44` — icon Flame "Lịch học" | `text-tertiary` #FBBF24 trên trắng | **1.67:1 FAIL** | `text-tertiary-ink` #B45309 → 5.02:1 |
| F-13-04 | `AdminDashboard.vue:19` — StatCard "Bài tập" | `text-tertiary` trên `bg-tertiary/10` | FAIL | `text-tertiary-ink` |
| F-13-05 | `AdminDashboard.vue:20` — StatCard "Bài nộp" | `text-quaternary` trên `bg-quaternary/10` | FAIL | `text-quaternary-ink` |

- **Bằng chứng pass:** `/admin/dashboard` sau fix — **0 contrast failure** (trước: 2). `evidence/f13-contrast.json`.
- **KHÔNG sửa cái đang đúng:** `AdminLayout.vue:28,66` `text-tertiary` nằm trên sidebar **tối** `bg-foreground` #1E293B → đo live **8.76:1 PASS**. Giữ nguyên; "sửa" chỗ này sẽ là false-positive.
- **Test hồi quy:** `frontend/src/views/__tests__/contrast-ink-tokens.test.js` (3 test) — chặn token vivid quay lại 2 call-site đã sửa.

---

## F-13-07 — `study_policy` thiếu CHECK singleton (cùng lớp lỗi F124) — **HIGH** — `FIXED`

**Nguồn:** Phase 1 DB audit (workflow) — **tôi đã verify độc lập lại.**

- **Bằng chứng (đo lại bằng query riêng):**
  ```
  sys.check_constraints trên study_policy = 0
  study_policy rows = 1 (id=1, effective_from=2026-09-20)
  ```
- **Root cause (đọc `tasks/streak-study/deploy.sql:27-33`):** `CONSTRAINT ck_study_policy_singleton CHECK (id = 1)` nằm **bên trong** khối `IF OBJECT_ID(N'dbo.study_policy', N'U') IS NULL`. Trong môi trường thật, Hibernate `ddl-auto=update` đã tạo bảng trước khi script chạy → điều kiện false → **cả khối bị bỏ qua → constraint không bao giờ được tạo**.
- **Đây đúng là lớp lỗi F124** — chính comment ngay dưới (`deploy.sql:42-45`) đã ghi lại bài học đó cho bảng `study_days` ("bảng và ràng buộc phải có GUARD RIÊNG… FK KHÔNG BAO GIỜ được tạo… FK_COUNT = 0"), nhưng `study_policy` **chưa được sửa theo**.
- **Hệ quả:** không có gì chặn ở tầng DB việc chèn hàng `id <> 1`; tính "singleton" của policy (nền tảng của `effectiveFrom()` — thứ `StreakService` ném `IllegalStateException` nếu thiếu) chỉ được bảo vệ bởi quy ước.
### Fix đã áp (tra tài liệu chính thức, KHÔNG dùng trí nhớ)

**Đính chính khuyến nghị ban đầu:** tôi từng đề xuất `@Check` — **SAI**. Tài liệu Hibernate 7 ghi rõ:
> `@Deprecated(since="7")` — *"Prefer `Table.check()`, `Column.check()`, etc., with `@CheckConstraint`"*
> (nguồn: `docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/Check.html`)

API đúng là **`jakarta.persistence.CheckConstraint`** + `@Table(check=…)`. Đã verify trong jar thật:
`jakarta.persistence-api-3.2.0.jar` có `CheckConstraint{name,constraint,options}` và `Table.check()`.

**Và một sự thật quan trọng khác:** `ddl-auto=update` **KHÔNG thêm CHECK constraint lên bảng đã tồn tại**
(docs Hibernate 7.0: `update` = *"only export what's missing in the schema"*; maintainer Hibernate trên
discourse: *"hbm2ddl UPDATE is not recommended for production"*). ⇒ **Annotation một mình KHÔNG đủ.**

| # | Việc | File |
|---|---|---|
| 1 | Khai báo `@Table(check=@CheckConstraint(name="ck_study_policy_singleton", constraint="id = 1"))` | `model/entity/StudyPolicy.java` |
| 2 | Tách CHECK ra guard riêng trong deploy.sql (đúng bài học F124) | `tasks/streak-study/deploy.sql` |
| 3 | Script vá DB đang chạy (idempotent, có THROW nếu có row id<>1) | `tasks/streak-study/fix-study-policy-check.sql` |

### Bằng chứng pass

```
Pre-check:  rows_with_id_not_1=0   existing_check=0
Apply:      study_policy_checks=1   (0 Msg errors)
Verify:     is_disabled=0  is_not_trusted=0  definition=([id]=(1))
Effectiveness: INSERT id=2 → BLOCKED msg=547   rows_after=1 (rollback sạch)
```
- Test hồi quy: `StudyPolicySingletonGuardTest` (1 test) — chặn annotation bị gỡ âm thầm.

**Về backup:** đây là `ALTER TABLE ADD CONSTRAINT` (không phải DML dữ liệu). Đã kiểm `rows_with_id_not_1=0`
trước khi chạy; rollback được bằng `DROP CONSTRAINT`. Không cần backup toàn DB.

---

## F-13-08 — `users.last_study_date` cũ: dashboard admin đọc dữ liệu chết — **MEDIUM** — `FIXED`

**Nguồn:** Phase 1 DB audit, **đo lại độc lập trong phiên quyết định** (audit ban đầu nói đúng một phần).

### Đo lại — audit ban đầu nói SAI một nửa

| Kiểm tra | Kết quả |
|---|---|
| `grep .getCurrentStreak()` trên entity `User` | **0 hit** → `current_streak` là **cột CHẾT**, không ai đọc |
| `UserService.java:322` | dùng `streakService.getCurrentStreak()` (streak THẬT từ `study_days`) |
| `DashboardService.java:35` | dùng `streakService.getCurrentStreak()` (THẬT) |
| Reminder (`StudyActivityService.reminderCandidates`) | dùng `findStudyReminderAtRisk/Broken` → query trên **`StudyDay`**, KHÔNG dùng `lastStudyDate` |
| 3 query cũ trên `lastStudyDate` | comment ghi rõ *"legacy, giữ cho tương thích"*, **0 caller** |
| **`AdminService.java:51`** `countByLastStudyDateAfter(...)` | **ĐỌC THẬT** cột `last_study_date` → dashboard "Học trong 7 ngày" (`recentUsers`) |

→ Audit ban đầu nói "AdminService đọc cột cũ" là **đúng** — nhưng tôi suýt bỏ sót vì đã grep `getCurrentStreak()` mà không grep `countByLastStudyDateAfter`. **Một agent khảo sát dừng giữa chừng kịp chỉ ra `recentUsers`.**

### Lệch thật (đo 2026-09-22)

```
user 2: last_study_date = 2026-09-19   | học thật (study_days) = 2026-09-22   → lệch 3 ngày
user 3: last_study_date = 2026-09-19   | học thật              = 2026-09-21   → lệch 2 ngày
```
`recentUsers` hiện trùng (2 = 2) **do may mắn**; sẽ sai khi cửa sổ 7 ngày trôi qua.

### Fix

- `StudyDayRepository.countDistinctUsersBetween(start, end)` — query mới trên `study_days`.
- `StudyActivityService.countActiveLearnersInLastDays(windowDays)` — bọc lại.
- `StreakService.countActiveLearnersInLastDays(days)` — facade.
- `AdminService.getDashboardStats()` — `recentUsers` nay lấy từ `study_days`, **không** đọc cột cũ; xoá biến `sevenDaysAgo` không còn dùng.

### Bằng chứng pass

- Test hồi quy: `AdminServiceDashboardStreakSourceTest` (2 test) — khẳng định `recentUsers` đến từ `streakService`, và `countByLastStudyDateAfter` **không bao giờ** được gọi.
- **Còn lại (ghi rõ, KHÔNG xoá):** `users.current_streak` + 3 query `lastStudyDate` cũ là **dead code**; xoá cột = đổi schema → cần quyết định riêng + backup (P3).

---

## F-13-09 — Ba đồng hồ trong stack lệch nhau (SQL Server chạy UTC) — **LOW** — `OPEN` (đã biết)

- `SYSDATETIME()` trong container SQL Server trả **UTC**, JVM trả **+07**, dữ liệu `datetime2` là **naive +07**. Đây là **hệ quả đã được ghi nhận từ audit-v7** trong AGENTS.md ("SYSDATETIME() trong sqlcmd sẽ lệch -7h").
- Không phải lỗi mới; v13 xác nhận lại. Giữ nguyên (không migrate timezone khi chưa có consumer thứ hai) — đúng như AGENTS.md.

---

## F-13-10 — "3/5 index trên `exercises` dư leading column" — **LOW** — `CLOSED (kết luận probe SAI)`

**Đo lại độc lập trong phiên quyết định — kết luận ban đầu SAI.**

`sys.indexes` + `sys.dm_db_index_usage_stats` (uptime 12h):

| index | key columns | seeks | scans |
|---|---|---|---|
| idx_exercises_type | (exercise_type) | 29 | 0 |
| idx_exercises_difficulty | (difficulty) | 0 | 147 |
| idx_exercises_lesson_order | (lesson_id, order_index) | 255 | 11 |
| idx_exercises_lesson_type_order | (lesson_id, exercise_type, order_index) | 12 | 1 |
| IX_exercises_lesson_type_diff_order | (lesson_id, exercise_type, difficulty, order_index) | 1485 | 1 |

**Phân tích prefix (script độc lập): KHÔNG index nào là strict prefix của index khác.**
- `lesson_order` = `(lesson_id, order_index)` **không** là prefix của `lesson_type_order` = `(lesson_id, exercise_type, …)` — khác cột thứ 2.
- `type` = `(exercise_type)` không phải prefix của cái nào (các index khác bắt đầu bằng `lesson_id`).

→ **Mọi index đều đang được dùng** (seeks hoặc scans > 0). Tuyên bố "3/5 dư leading column" là **kết luận của probe, không phải vấn đề sản phẩm**. **Đóng — không hành động.**

---

## F-13-06 — `sweep/v13/` mang nhãn v12 (coverage bị gán nhầm) — **LOW** — `FIXED`

- **Bằng chứng:** `sweep/v13/api-sweep.js` header ghi `audit-v12-full Phase 2`, ghi output vào `.specify/specs/audit-v12-full/evidence/api-sweep.json`, tạo/xoá `AUDIT-V12-API-%`.
- **Rủi ro:** một lần chạy v13 sẽ ghi đè bằng chứng v12 → "đã phủ toàn bộ" bị gán nhầm phiên bản.
- **Fix:** đổi namespace `AUDIT-V13-`, output → `audit-v13-full/evidence/`, tiêu đề log; giữ tham chiếu lịch sử "audit-v12 F148/F153" trong comment.
- **Bằng chứng pass:** `grep AUDIT-V12 sweep/v13/*` = **0**; `AUDIT-V13` = 9 (js) + 4 (sql).

---

## F-13-02 — Lesson builder: block không bao giờ tới học viên — **MEDIUM** — `FIXED (phương án B)`

**Nguồn:** điều tra nhánh "tạo bài tập" khi truy F-13-01. **Đã verify bằng dữ liệu thật + đọc code** (không suy đoán).

- **Bằng chứng DB thật:** `lesson_sections = 10`, `lesson_blocks = 15` (TEXT 10, QUESTION **3**, SUBMISSION 1, TABLE 1). Ví dụ `block_id=4`: `{"questionType":"MULTIPLE_CHOICE","options":["go","goes","going","went"],"correctAnswer":"goes",...}`; `block_id=5`: `FILL_IN_BLANK`; `block_id=7`: `TRUE_FALSE`. → admin **có** dựng bài qua builder và **có** lưu.
- **Nhưng không có renderer cho học viên:** `getStructure()` (public, `GET /api/lessons/{id}/structure`) chỉ được **định nghĩa** trong `frontend/src/services/lessonStructureService.js:4` và **không nơi nào gọi**; `grep 'blockType'` ngoài `AdminLessonBuilder.vue` = 0. `LessonContent.vue:27` chỉ render `lesson.content` (HTML), không đọc blocks.
- **Hệ quả:** nội dung admin dựng trong Lesson Builder **không xuất hiện** cho học viên. Học viên chỉ thấy bài tập dạng `exercises` (bảng khác, đường admin tạo riêng ở `AdminExercises.vue`).
- **Đính chính một giả thuyết SAI của tôi khi lập kế hoạch (giữ lại để minh bạch):** tôi từng nghi `AdminLessonBuilder.vue:256` dùng `FILL_IN_BLANK` "không có trong enum". **Sai** — đó là `QuestionType` (đúng là có `FILL_IN_BLANK`), không phải `ExerciseType` (mới là `FILL_BLANK`). Hai enum khác nhau, cả hai đều đúng. Không có bug mismatch.
- **Quan sát phụ (LOW):** `QuestionType` là **dead code** — `grep` trong `src/main` cho thấy nó không được tham chiếu ở đâu ngoài chính file định nghĩa; builder lưu `questionType` dưới dạng **chuỗi JSON thô** trong `lesson_blocks.data`, không qua enum. Không gây lỗi (chuỗi khớp giá trị enum), nhưng là nợ kỹ thuật.
### Quyết định: **phương án B** — sửa cái bẫy + ghi rõ giới hạn

Người dùng chốt phương án B (rẻ, không mất tính năng, không API mới, đúng AGENTS.md).

### ⚠️ Phát hiện THÊM khi đo lại — bẫy admin chưa được audit thấy

Nút **"Xem trước"** (`AdminLessonBuilder.vue:29`) trỏ `/lessons/{id}/preview` — route **KHÔNG tồn tại** (`router/index.js` chỉ có `/lessons` và `/lessons/:id`), nên catch-all (`:192`) đá admin về **trang chủ**.

**Bằng chứng live:** `GET /lessons/446/preview` → URL cuối `http://localhost:5173/`.

→ Admin bấm "Xem trước" thấy trang chủ và tưởng nội dung đã publish. **Đây là F-13-22.**

### Fix đã áp

| # | Việc | File |
|---|---|---|
| 1 | Sửa "Xem trước" → `/lessons/{id}` | `AdminLessonBuilder.vue:29` |
| 2 | Banner cảnh báo `role="note"` nói rõ giới hạn + chỉ sang "Quản lý bài tập" | `AdminLessonBuilder.vue` |
| 3 | Vô hiệu hoá tạo mới QUESTION/SUBMISSION (vẫn hiển thị blocks cũ) | `AdminLessonBuilder.vue` (`blockTypeOptions`) |
| 4 | Tài liệu trạng thái | `docs/lesson-builder-status.md` |

### Bằng chứng pass

- Test hồi quy: `AdminLessonBuilder.f13.test.js` (2 test) — nút preview trỏ `/lessons/447` và **không** kết thúc `/preview`; banner cảnh báo tồn tại.
- **Live:** `/admin/447/build` → `previewHref=/lessons/447`, `hasWarningBanner=true`.
- Ảnh: `evidence/f13-02-builder-fixed.png`, `evidence/f13-02-learner-cannot-see-blocks.png`.

### CHƯA làm (ghi rõ)

Nội dung builder **vẫn chưa tới học viên** — nhưng nay **đã được nói rõ** trong UI + docs. Muốn nối thật: A1 (render TEXT/IMAGE/AUDIO/TABLE) → A2 (QUESTION tự-kiểm-tra) → A3 (grading + submission). Xem `docs/lesson-builder-status.md`.

---

## F-13-11 — `?sort=` không hợp lệ trên endpoint phân trang trả **500** thay vì 400 — **LOW** — `FIXED`

**Nguồn:** Phase 2 API sweep (workflow). **Tôi verify độc lập lại.**

- **Bằng chứng (HTTP thật):**
  ```
  GET /api/vocabulary?sort=nonexistentProperty&page=0&size=3
  -> HTTP 500 {"detail":"Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.","status":500}
  ```
- **Root cause:** Spring Data ném `PropertyReferenceException` khi resolve `Pageable` với property không tồn tại; `GlobalExceptionHandler` không có handler cho nó → rơi vào catch-all `RuntimeException` → 500.
- **Đây là lỗi của CLIENT**, phải là 400.
- **Fix:** thêm `@ExceptionHandler(PropertyReferenceException.class)` → **400 Bad Request** (`GlobalExceptionHandler.java`).
- **Test hồi quy:** `GlobalExceptionHandlerProblemDetailTest.invalidSortPropertyMapsTo400Not500`.
- **Ghi chú phụ (từ sweep, chưa hành động):** `?sort=` bị **bỏ qua âm thầm** ở 3 endpoint phân trang khác (không sort theo tham số). Cần quyết định: hoặc hỗ trợ, hoặc ghi rõ trong tài liệu API. **Chưa sửa** vì chưa đo được tác động người dùng.

---

## F-13-12 — Guard F-13-01 **chặn nhầm** option dạng `"A - <nội dung>"` — **HIGH** — `FIXED`

**Nguồn:** Phase 2 API sweep (workflow). Đây là **lỗi do chính fix của tôi gây ra**, bắt bởi review chéo.

- **Bằng chứng (hàng THẬT):** `exercise_id=651717`, `exercise_type=MULTIPLE_CHOICE`, `options=["A - Salad","B - Cheeseburger","C - Pizza","D - Bread"]`, `correct_answer="A - Salad"`.
- **Vấn đề:** regex `^[A-D](\s*-\s*.*)?$` coi `"A - Salad"` là placeholder → guard sẽ **từ chối** một bài tập hoàn toàn hợp lệ. (Tôi đã copy nguyên regex này từ logic render cũ sang guard mới.)
- **Fix:** tách 2 khái niệm khác nhau:
  - `isBareLetterOption` = `^[a-d]$` — chỉ một chữ cái đơn độc mới là placeholder (dùng cho guard + nhánh MULTIPLE_CHOICE).
  - `isPlaceholderOption` = `^[a-d](\s*-\s*.*)?$` — dùng **chỉ** cho FILL_BLANK/TRANSLATION, nơi đáp án nằm ở `correctAnswer` nên dạng `"A - noisy"` đúng là placeholder.
- **Sửa ở cả backend** (`ExerciseService`, `AiExerciseService`) **và frontend** (`LessonExerciseTab.vue`).
- **Test hồi quy:** `createAcceptsMultipleChoiceWithLetterDashContentOptions` (JUnit) + `renders "A - Salad" style options as choices, not a text input` (vitest).

---

## F-13-13 — LISTENING giữ nguyên defect "options chữ cái vô nghĩa" — **MEDIUM** — `FIXED`

**Nguồn:** Phase 2 API sweep (workflow).

- **Bằng chứng (đo thật):** `LISTENING` tổng **358**, trong đó **39** hàng có options `["A","B","C","D"]` và **319** hàng không có options.
- **Vấn đề:** guard F-13-01 chỉ áp cho `MULTIPLE_CHOICE`, nên LISTENING vẫn có thể lưu options chữ cái → nếu render thành nút thì học viên thấy 4 lựa chọn vô nghĩa (đáp án thật là cả câu transcription).
- **Fix:** nhánh `LISTENING` trong `hasOptionChoices()` chỉ hiện lựa chọn khi options **có nội dung thật** (`hasRealOptions`); ngược lại giữ ô nhập text (đúng bản chất "nghe rồi viết lại").
- **Test hồi quy:** `keeps a text answer input for LISTENING whose options are bare letters` + `still offers choices for LISTENING with real options`.

---

## F-13-14 — Token `danger` dùng làm màu chữ chỉ đạt **4.00:1** (dưới AA) — **MEDIUM** — `FIXED`

**Nguồn:** Phase 3 UI sweep (workflow). **Tôi verify độc lập lại bằng tính toán.**

- **Bằng chứng (đo + tính):**
  - Sweep bắt live: `/leaderboard` — `rgb(225,29,72)` trên `rgb(252,231,228)` = **3.95:1** (cần 4.5).
  - Tính lại độc lập: `#E11D48` trên `bg-danger/10` composite = **4.00:1**; trên `bg-danger/10` phủ cream = **3.96:1**.
- **Phạm vi:** **10 call-site thật** dùng `text-danger` làm màu chữ (Leaderboard, AdminDashboard, AdminSpeakingPrompts/Submissions, AdminVideoAttempts/Lessons, AiGeneratePanel, AiVocabGenerator, SpeakingRecord...).
- **Đây là cùng lớp lỗi F-13-03/04/05** (token vivid dùng làm chữ) nhưng ở họ màu `danger` — lớp `*-ink` của v11 **chưa bao phủ `danger`**.
- **Fix:** thêm token `danger-ink` = **#BE123C** (5.29–6.29:1 trên mọi nền), mirror ở `tailwind.config.js` + `design-system.css`; đổi **36 chỗ** `text-danger` → `text-danger-ink` trong 23 file.
- **Giữ nguyên:** `bg-danger` (fill) và `border-danger` (border) — đúng vai trò "vivid = fill/border".
- **Test hồi quy:** `contrast-ink-tokens.test.js` (+3 test) — chặn `text-danger` quay lại.

### Lưu ý về probe artifact (không phải bug)

Sweep cũng báo `/leaderboard` và `/profile` có `ERR_EMPTY_RESPONSE` và trạng thái lỗi. **Đây là artifact do chính tôi gây ra**: tôi rebuild container backend nhiều lần trong lúc sweep chạy, làm request rơi vào lúc container restart. Đo lại sau: `/api/leaderboard` → **200** bình thường. Ghi lại để không ai "sửa" lỗi không tồn tại.

---

## F-13-15 — `/api/srs/due/{deckId}` có **N+1 query** — **MEDIUM** — `FIXED`

**Nguồn:** Phase 5 perf sweep (workflow). Chứng minh bằng **delta query-stats**, không phải đọc code.

- **Bằng chứng:** sweep chứng minh bằng `sys.dm_exec_query_stats` delta rằng endpoint phát **1 query cho mỗi từ** trong deck.
- **Root cause (đọc code xác nhận):** `SrsService.getDueWords()` gọi `progressRepository.findByUserIdAndVocabularyId(userId, vocab.getId())` **bên trong vòng lặp** qua từng `DeckWord`.
- **Fix:** thêm `findByUserIdAndVocabularyIdIn(userId, vocabIds)` vào repository và **batch 1 query** cho cả deck, build `Map<vocabId, progress>` rồi tra cứu trong vòng lặp.
- **Test hồi quy:** `SrsDueWordsAuthzTest.dueWordsUsesOneBatchedProgressQueryNotOnePerWord` — khẳng định `findByUserIdAndVocabularyIdIn` gọi **đúng 1 lần** và `findByUserIdAndVocabularyId` **không bao giờ** được gọi.
- **Hành vi giữ nguyên:** F151 (authorize trước khi đọc) và F106 (cap interval) đều còn nguyên; 4 test cũ vẫn pass.

### Ghi chú: các quan sát perf KHÁC — KHÔNG hành động (P5)

| Quan sát | Quyết định |
|---|---|
| `admin exercise search q=the` = 185 ms | **Từ chối tối ưu** — LIKE `%kw%` trên 43 735 hàng, không index nào cứu leading wildcard (đã biết từ v11). |
| `admin exercise list` = 72 ms | Ghi nhận; chưa đo được lợi ích rõ. |
| Lighthouse không phát category Performance trên host này | Ghi nhận là **giới hạn của host**, không phải lỗi app. |
| Webfont payload 125 376 bytes | Ghi nhận; **không đề xuất cắt** (font là ràng buộc thiết kế P6). |
| Deep-pagination chậm | **Không tái hiện** — ghi là **kết quả âm** (negative result). |

---

## F-13-16 — `/admin/:id/build` có 3 chỗ contrast dưới AA — **MEDIUM** — `FIXED`

**Nguồn:** Phase 3 UI sweep (117 route×role). Đây là **route duy nhất** còn contrast fail sau khi sửa F-13-14.

- **Bằng chứng (live DOM, composite alpha):**

| Chỗ | Trước | Tỷ lệ | Sau | Tỷ lệ mới |
|---|---|---|---|---|
| Badge "B" — `bg-accent` + `text-white` | 4.23:1 FAIL | → | `bg-accent-strong` | **5.70:1** |
| Nút × trên header section tối — `text-white/40` | 3.62:1 FAIL | → | `text-white/60` | **6.21:1** |
| Nút × trắng — `text-foreground/40` | 2.36:1 FAIL | → | `text-muted-foreground` | **6.38:1** |
| Header modal "Xóa Section" — `bg-accent` + white | 4.23:1 FAIL | → | `bg-accent-strong` | **5.70:1** |

- **Bằng chứng pass:** đo live lại `/admin/445/build` → **contrastFails = 0** (trước: 3).
- **Đây là cùng lớp lỗi F-13-03/04/05/14** — token vivid + opacity dùng làm chữ. Đã xử lý nốt.

---

## F-13-17 — `sweep/v12/api-inventory.js` ghi đè bằng chứng v12 (lớp lỗi F-13-06) — **LOW** — `FIXED`

**Nguồn:** phát hiện khi soát `git status` ở bước cuối. Đây là **cùng lớp lỗi với F-13-06** nhưng ở một file khác.

- **Bằng chứng:** chạy `node sweep/v12/api-inventory.js` trong phiên v13 đã **ghi đè** `.specify/specs/audit-v12-full/evidence/endpoint-inventory.json` — đổi `131/147/145` (số thật của v12) thành `132/148/146` (số của v13). `git diff` xác nhận 4 dòng đổi.
- **Root cause:** `sweep/v12/api-inventory.js:20` **hardcode** đường dẫn output vào `audit-v12-full/evidence/`, không nhận tham số.
- **Rủi ro:** mỗi lần chạy lại inventory cho phiên mới sẽ **âm thầm viết lại bằng chứng của phiên cũ** → báo cáo cũ mất tính trung thực.
- **Fix:**
  1. `git checkout` khôi phục nguyên trạng `audit-v12-full/evidence/endpoint-inventory.json` (đã verify `git diff` sạch).
  2. Tạo `sweep/v13/api-inventory.js` trỏ output vào `audit-v13-full/evidence/`.
  3. Chạy lại → ghi đúng chỗ, **v12 không bị chạm** (verify `git diff` sạch lần 2).
- **Bài học:** harness phải **tham số hoá đường dẫn output**, không hardcode theo phiên — nếu không, "bằng chứng" của phiên cũ có thể bị viết lại mà không ai biết. Đây là lần thứ **hai** trong cùng phiên một script v12 ghi vào namespace v12 (lần đầu là `api-sweep.js`, F-13-06).

---

## F-13-18 — `/lessons/:id` **không có `h1`** (heading bắt đầu ở h3) — **LOW** — `FIXED`

**Nguồn:** Phase 3 UI sweep (workflow). Vi phạm WCAG 2.4.6 / 1.3.1.

- **Bằng chứng (live DOM):** `/lessons/445` render `h3,h3,h3,h3,h3,h3` và **0 `h1`**.
- **Root cause:** `LessonLayout.vue` không có `h1`; `LessonContent.vue` render HTML bài học qua `v-html` và `h2` duy nhất nó sở hữu là phần "Luyện nói".
- **Fix:** thêm `<h1>` vào `LessonLayout.vue`, nạp tiêu đề bài học qua `lessonService.getById()` (có fallback "Bài học" nếu lỗi).
- **Bằng chứng pass (live):** `/lessons/445` → **1 `h1`** = *"English Grammar Exercises for A1 – have got and articles"* (đúng tiêu đề thật).
- **Test hồi quy:** `LessonLayout.h1.test.js` (2 test).
- **Lỗi tự gây trong lúc fix:** lần đầu tôi gọi `lessonService.getLessonById()` — **method không tồn tại** (đúng là `getById`), nên `h1` hiện fallback. Phát hiện bằng cách **đo lại DOM** (không tin "code nhìn đúng"). Đã sửa và đo lại.

---

## F-13-19 — Leaderboard render avatar hỏng, không có fallback `onerror` — **LOW** — `FIXED`

**Nguồn:** Phase 3 UI sweep (workflow).

- **Bằng chứng (live DOM + DB thật):** `users.avatar_url` của `testaudit2026` (id 150011) = `https://example.com/avatar.png`. `Leaderboard.vue:45` dùng `:src="user.avatarUrl || dicebear…"` — vì URL **không rỗng** nên fallback không bao giờ chạy → ảnh hỏng (`naturalWidth=0`, `net::ERR_BLOCKED_BY_ORB`).
- **Fix:** thêm `@error="onAvatarError"` để đổi sang ảnh dự phòng khi load lỗi.
- **Bằng chứng pass (live):** `/leaderboard` → **21 ảnh, 0 ảnh hỏng** (trước: 1).
- **Ghi chú:** hàng `avatar_url` rác là dữ liệu test cũ, **không xoá** (không phải việc của v13); fix ở tầng UI là đúng vì URL hỏng có thể xảy ra với bất kỳ user nào.

---

## F-13-20 — Tham số `?redirect=` được tạo nhưng **không ai đọc** — **INFO** — `FIXED`

**Nguồn:** Phase 3 UI sweep (workflow).

- **Bằng chứng:** `router/index.js:206` sinh `next('/premium?redirect=' + encodeURIComponent(to.fullPath))`; `LearningPath.vue:95` link `/premium?redirect=%2Fspeaking`. **Grep toàn `frontend/src` = 0 chỗ đọc `route.query.redirect`**.
- **Hệ quả:** người dùng bị đá từ `/speaking` sang `/premium`, sau khi đăng nhập **không được quay lại** `/speaking`.
- **Đo thêm:** `requiresAuth` bounce (`router/index.js:210`) về `/login` **cũng không kèm redirect** → cả trường hợp thường cũng mất đích.

### Fix đã áp

| # | Việc | File |
|---|---|---|
| 1 | Helper chống open-redirect | `frontend/src/utils/safeRedirect.js` (mới) |
| 2 | `requiresAuth` bounce mang `redirect` | `router/index.js:210` |
| 3 | Login đọc `redirect` sau khi đăng nhập | `views/Login.vue` |
| 4 | Register xử lý tương tự (nhất quán) | `views/Register.vue` |

`safeRedirect()` từ chối: `//evil.com`, `http(s)://`, `javascript:`, `data:`, `\evil`, `%2F%2F`, khoảng trắng/control chars.

### Bằng chứng pass

- Test hồi quy: `safeRedirect.test.js` — **24 test**, gồm **13 ca tấn công** đều fallback `/lessons`.
- **Live (Playwright):**
  - `?redirect=%2Fspeaking` → đăng nhập → URL cuối **`/speaking`** ✅
  - `?redirect=%2F%2Fevil.com` → đăng nhập → URL cuối **`/lessons`** (KHÔNG rời site) ✅

---

## F-13-21 — Harness `sweep/v13/api-sweep.js` còn 2 đường dẫn v12 — **LOW** — `FIXED`

**Nguồn:** workflow báo cáo (harness correction). **Cùng lớp F-13-06/17.**

- **Bằng chứng:** dòng 554 chạy `sweep/v12/_cleanup.sql` (thay vì file nó vừa ghi) → deck `AUDIT-V13-*` sẽ **rò rỉ**; dòng 569 ghi JSON kết quả vào `.specify/specs/audit-v12-full/evidence/`.
- **Fix:** trỏ cả hai về `sweep/v13/` và `audit-v13-full/evidence/`. Đã verify: `sweep/v13/_cleanup.sql` được dùng, `api-sweep.json` nằm đúng chỗ, **v12 evidence sạch** (`git status` rỗng).


---

## F-13-22 — Nút "Xem trước" của Lesson Builder trỏ route **không tồn tại** — **MEDIUM** — `FIXED`

**Nguồn:** phát hiện khi đo lại F-13-02 (audit ban đầu chưa thấy).

- **Bằng chứng:** `AdminLessonBuilder.vue:29` trỏ `/lessons/{id}/preview`. Router chỉ có `/lessons` và `/lessons/:id`; catch-all (`router/index.js:192`) đá về `/`.
- **Bằng chứng live:** `GET /lessons/446/preview` → URL cuối `http://localhost:5173/` (trang chủ).
- **Hệ quả:** admin bấm "Xem trước" thấy trang chủ và **tưởng nội dung đã publish** — cùng gốc với F-13-02.
- **Fix:** trỏ `/lessons/{id}` (route thật).
- **Bằng chứng pass:** live `/admin/447/build` → `previewHref=/lessons/447`; test `AdminLessonBuilder.f13.test.js` khẳng định không href nào kết thúc `/preview`.
