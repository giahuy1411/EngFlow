# audit-v15-full — BÁO CÁO

**Ngày:** 2026-09-25 (+07) · **Nhánh:** `audit-v15-full` (từ `audit-v14-full` @ `f39950b`)
**Tiền nhiệm:** `audit-v14-full` · **Artifact:** `.specify/specs/audit-v15-full/`
**Phạm vi người dùng chốt:** dọn nợ dữ liệu (C1/C2) + **gỡ toàn bộ Đường B (Lesson Builder)** + verify timezone.

---

## 1. Tóm tắt một đoạn

Người dùng xác nhận nội dung bài học lấy **hoàn toàn từ `english-practice.net`** (scrape), còn khối
**"Nội dung biên soạn / Tài liệu bổ sung cho bài học này"** — hiển thị từ đoạn đó trở xuống — **không**
phải nội dung của họ và phải **gỡ toàn bộ**. Đó chính là **Đường B (Lesson Builder)**: 16 file backend +
5 file frontend + 3 bảng DB + 2 test suite. Cùng phiên: xoá **67 tài khoản test/audit** + **114 payment
rác**, chuyển **3 câu hỏi** tự soạn sang bảng `exercises`, và verify timezone. Kết quả: trang bài học
giờ **chỉ còn nội dung scrape + bài tập** (tách hẳn), parity `1470|43738|5|118|29|4|3|12|10`, backend
512/0 fail, frontend 178/0 fail, **17 finding** (10 thật + 7 probe tự gây) đều FIXED.

---

## 2. Đã làm

### 2.1 Cleanup dữ liệu (C1 + C2)

| Việc | Kết quả |
|---|---|
| Backup mới + `RESTORE VERIFYONLY` | `engflow_2026-09-25-prec1c2.bak` (37 MB), **valid** |
| **Restore-drill** sang DB scratch | restore thành công, parity khớp `1470\|43735\|72\|126\|15\|10\|5`; drill DB đã DROP |
| Dry-run (ROLLBACK) | đúng 114 payment + 42 child + 67 user; DB không đổi |
| **Xoá thật** (ID liệt kê, `BEGIN TRAN` + assertion) | 0 `Msg`; **67 user**, **114 payment**, 42 row con |
| Parity sau cleanup | **`1470\|43735\|5\|118\|29\|4\|3\|12\|10\|5`** — khớp kế hoạch |
| 5 user thật + `admin@gmail.com` | còn nguyên; login **200**; leaderboard **200**; **0 orphan** |

### 2.2 Chuyển 3 câu hỏi → `exercises` (lesson 447)

| Block nguồn | → `exercises` | id mới |
|---|---|---|
| 4 `MULTIPLE_CHOICE` (`goes`, options go/goes/going/went) | `MULTIPLE_CHOICE` | **787912** |
| 5 `FILL_IN_BLANK` (`play`) | `FILL_BLANK` | **787913** |
| 7 `TRUE_FALSE` (`True`) | `MULTIPLE_CHOICE ["True","False"]` (enum không có TRUE_FALSE) | **787914** |
| 9 `SUBMISSION` | **không chuyển** (không có tương đương) | — |

Verify: lesson 447 = **8 exercises**; `POST /grade` đáp án đúng **3/3**, sai **0/3**;
learner **không** thấy `correctAnswer` trước khi trả lời.

### 2.3 Gỡ Đường B (Lesson Builder) — TOÀN BỘ

**Backend:** xoá 16 file (controller/service/entity/repo/enum/DTO của `LessonStructure*`, `LessonSnapshot*`,
`LessonBlock`, `LessonSection`, `BlockType`, `QuestionType`); `LessonService.deleteLesson` bỏ cascade tới
3 bảng đó. **Tách** 3 endpoint **dùng chung** (`/api/admin/upload`, `/api/resources/**`,
`/api/admin/audio-upload`) sang `AdminUploadController` mới — **không** gỡ chức năng.

**Frontend:** xoá `AdminLessonBuilder.vue`, `LessonBlocks.vue`, `lessonStructureService.js` (+2 test),
route `/admin/:id/build`, nút "Xây dựng" ở `AdminLessons.vue`, breadcrumb `AdminLayout.vue`,
`<LessonBlocks/>` trong `LessonLayout.vue`; `AdminExercises.vue` đổi sang `uploadService.js` mới;
sửa chuỗi stale ở `LessonPreview.vue`.

**DB:** `DROP TABLE lesson_blocks, lesson_sections, lesson_snapshots` (0 `Msg`).
Thêm `V10__drop_lesson_builder.sql` (theo tiền lệ V2 — Flyway disabled).

### 2.4 Verify sau gỡ (live, 3 tầng)

| Kiểm tra | Kết quả |
|---|---|
| `/lessons/447` tab "Nội dung" | **0** section "Nội dung biên soạn"; 3955 ký tự nội dung scrape còn |
| 8 lesson từng có Đường B | section **vắng** ở **cả 8** (`f15-route-b-removal-live.json` **18/18 PASS**) |
| Tab "Bài tập" | **8 câu**; 3 câu chuyển làm được |
| Tab "Lịch sử" | **200**; chuỗi stale đã sửa |
| `/admin/447/build` | redirect **`/`** (catch-all) |
| Admin lessons | **0** nút "Xây dựng", 0 icon Layers |
| anon `GET /api/lessons/447/structure` | **404** (F-15-01 — rò rỉ đáp án **tự khỏi**) |
| `POST /api/admin/upload` + `GET /api/resources/**` | **200**; path-traversal **400** |
| `deleteLesson` live | tạo lesson 114115 → DELETE **204** → GET **404** |
| a11y panel "Nội dung" | **5/5 PASS** (63 text node, min contrast **4.6**) |

### 2.5 F-13-09 timezone — verify + docs

Đo lại 3 đồng hồ: host/backend **+07**, SQL Server **UTC** (lệch 7h). **11** writer `LocalDateTime.now()`,
**0** `LocalDate.now()` trần; `study_days.study_date` là `DATE`; `FUTURE_STUDY_DAYS = 0` → đường streak
**miễn nhiễm**. **Giữ nguyên, không migrate** (theo quyết định người dùng). Phát hiện **F-15-06**: chính
script migrate của v15 dùng `GETDATE()` (UTC) → đã sửa 3 hàng về naive-VN.

---

## 2.6 Bản hardening 3 lớp điểm yếu harness (sau báo cáo gốc)

Người dùng yêu cầu sửa **tận gốc** 3 điểm yếu ở §8. Đã hoàn thành — chi tiết đầy đủ ở
`evidence/REPORT-HARDENING.md` + `evidence/hardening-summary.md`. Tóm tắt:

| Lớp | Sửa tận gốc |
|---|---|
| **L1** probe ghi dữ liệu thật, không dọn | `assertClean()` (throw) + gọi trong `finally`; `study_days` + `PENDING_PAYMENTS` vào parity; dọn `study_days` trong v12 sweep; tìm & dọn residue thật (id 30069) |
| **L2** harness dựng lại từ snapshot cũ → fix mất | `sweep/harness/**` **được commit** + `_config.js` tham số hoá `--audit/--out`; `harness-restore.md` không rỗng |
| **L3** harness/docs trỏ thứ đã gỡ | `HarnessDriftTest` (JUnit, trong `mvnw test`) + `assert-harness.js`; sửa 8 lỗi cụ thể; guard chứng minh **2 chiều** |

Kết quả: backend **515/0** (+3 drift test), frontend **178/1**, parity `1470|43738|5|118|29|4|3|12|10`,
`study_days=4`, `PENDING_PAYMENTS=0`, mọi harness tracked chạy **0 residue**.

---

## 3. Chưa làm (và lý do)

| Hạng mục | Lý do |
|---|---|
| Migrate toàn bộ timezone sang UTC | Người dùng chốt "chỉ verify + ghi docs"; chưa có consumer thứ hai |
| GitHub issues | Quyết định v14 giữ nguyên (V3) |
| Webhook SePay thật | Biên real-money (blocked như v11) |
| Optimisation hiệu năng | Không nằm trong phạm vi v15 (perf-probe chỉ đo "before") |
| `e2e-3tier` | Không có trong `sweep/v15`; regression phủ bằng api-sweep + deep-probe + ui-sweep |

---

## 4. Đã fix và cách fix (17 finding)

| ID | Mức | Vấn đề | Cách fix |
|---|---|---|---|
| **F-15-01** | HIGH | Endpoint public rò rỉ `correctAnswer` của block QUESTION cho anon | Gỡ Đường B → endpoint **404** |
| **F-15-02** | MED | `LessonStructureController` trộn 3 endpoint dùng chung | **Tách** → `AdminUploadController` |
| **F-15-03** | MED | `AdminExercises.vue` phụ thuộc service Đường B | Tạo `uploadService.js` |
| **F-15-04** | LOW | 6/8 lesson có block TEXT trùng `lesson.content` | Xoá (gỡ Đường B) |
| **F-15-05** | LOW | `lesson_snapshots` chưa từng dùng (5 hàng test) | DROP bảng |
| **F-15-06** | LOW | Script migrate dùng `GETDATE()` (UTC) → lệch 7h | `DATEADD(HOUR,7,…)` cho 3 hàng |
| **F-15-07** | MED | Harness còn khẳng định hành vi Đường B (6 chỗ) | Cập nhật sang hành vi mới |
| **F-15-08** | LOW* | 2 assertion v13 tái nhập (probe SAI) | Áp lại bản sửa v14 |
| **F-15-09** | MED | `ui-sweep` tạo payment thật không dọn (F-14-C2 tái diễn) | Xoá 2 hàng + **thêm cleanup** |
| **F-15-10** | LOW | Doc drift (21 bảng / test counts / 3 bảng đã gỡ) | Sửa README, AGENTS, ERD, docs |
| **F-15-11** | MED* | `ui-sweep` so URL đầy đủ với path trần → 27 fail giả | So pathname |
| **F-15-12** | MED* | `ui-sweep` kỳ vọng premium anon → `/login` | Đổi sang `/premium` |
| **F-15-13** | LOW* | `perf-probe` ghi artifact vào `sweep/v13` | `__dirname` |
| **F-15-14** | LOW* | `focused-probe` đo "builder" trên trang chủ | Gỡ block |
| **F-15-15** | LOW* | 2 probe v13 khẳng định section đã gỡ | Viết lại thành probe gỡ (18/18, 5/5) |
| **F-15-16** | LOW* | `cls-probe` ghi artifact vào `sweep/v13` | `__dirname` |
| **F-15-17** | MED | Harness **chuẩn** (tracked, AGENTS.md trỏ tới) vẫn dùng endpoint đã gỡ | Sửa `sweep/v8/ui/routes.js`, `sweep/v8/p16-parity.sql`, `sweep/v12/api-sweep.js` |

`*` = lỗi **probe tự gây** trong phiên, không phải lỗi sản phẩm. **10 finding thật** (F-15-01..07, 09, 10, 17) + **7 lỗi probe tự gây** (F-15-08, 11–16).

---

## 5. Kết quả kiểm thử (đếm từ run log)

| Bộ | Trước | Sau | Delta | Giải thích |
|---|---|---|---|---|
| Backend (`mvnw test`) | 525 / 0 fail / 0 err / 11 skip | **512 / 0 / 0 / 11** | −13 | 12 test Đường B xoá + 1 test F56 gộp |
| Frontend (`vitest`) | 194 pass / 1 skip (31 file) | **178 / 1 (29 file)** | −16 | 2 file test Đường B xoá |
| Build | 177.98 kB | **177.74 kB** | −0.24 kB | chunk Builder gỡ |
| `api-sweep` | — | **143 pass / 0 fail** (1 blocked, 2 n/a) | — | — |
| `deep-probe` | — | **58 pass / 0 fail** | — | — |
| `reconcile-inventory` | — | **137/137, 25 controller 100%** | — | — |
| `search-sort` | — | **0 finding** | — | — |
| `ui-sweep` | — | **0 guard fail, 0 api/page error, 0 contrast fail, 0 overflow** (1 cảnh báo trình duyệt `compute-pressure`) | — | — |
| Tap-target triage | 118 → 0 REAL (v14) | **115 → 0 REAL** | — | 52 inline + 21 spacing |
| CLS (3 route) | — | 0.00003–0.00095 | — | dưới ngưỡng |
| Perf (34 endpoint) | — | 12–195 ms median | — | — |
| Parity cuối | `…72…126…5` | **`1470\|43738\|5\|118\|29\|4\|3\|12\|10`** | — | +3 exercise migrate, −3 bảng |

---

## 6. Skill / plugin đã nạp

`speckit-*` (constitution/specify/clarify/checklist/plan/tasks/implement/converge/analyze) ·
`superpowers:using-superpowers`, `brainstorming`, `verification-before-completion`,
`requesting-code-review` · **chrome-devtools MCP** · **Playwright MCP / playwright-core** ·
`accessibility` (WCAG 2.2 AA) · `java-springboot`, `java-coding-standards`, `generate-tests` ·
`frontend-design`.

---

## 7. File đã xoá trong phiên (ràng buộc V1)

### 7.1 Xoá khỏi repo vì gỡ Đường B — **24 file**

**Backend `src/main` (16):** `controller/LessonStructureController.java`,
`controller/LessonSnapshotController.java`, `service/LessonStructureService.java`,
`service/LessonSnapshotService.java`, `model/entity/LessonBlock.java`,
`model/entity/LessonSection.java`, `model/entity/LessonSnapshot.java`,
`model/enums/BlockType.java`, `model/enums/QuestionType.java`,
`repository/LessonBlockRepository.java`, `repository/LessonSectionRepository.java`,
`repository/LessonSnapshotRepository.java`, `model/dto/request/BlockRequest.java`,
`model/dto/request/SectionRequest.java`, `model/dto/response/BlockResponse.java`,
`model/dto/response/SectionResponse.java`

**Backend `src/test` (2):** `service/LessonStructureBlockTypeGuardTest.java`,
`controller/AuditV10DraftLessonStructureGuardTest.java`

**Frontend (5):** `views/admin/AdminLessonBuilder.vue`, `views/admin/AdminLessonBuilder.f13.test.js`,
`views/lessons/LessonBlocks.vue`, `views/lessons/LessonBlocks.test.js`, `services/lessonStructureService.js`

**Docs (1):** `docs/lesson-builder-status.md`

### 7.2 File/folder rác sinh ra khi kiểm thử — **XOÁ cuối phiên**

| Đường dẫn | Nội dung | Số file |
|---|---|---|
| `tmp/v15/**` | scratch SQL/txt của phiên (**KHÔNG** gitignored → phải xoá) | 33 |
| `sweep/v15/**` | harness của phiên (gitignored) | 17 |
| `target/**` | build output backend | — |
| `frontend/dist/**` | build output frontend | — |

**Ngoài ra đã xoá trong phiên:** file upload probe `uploads/7828e7bf-c989-4543-9e3c-115a213ad8d0.txt`;
2 hàng payment residue (id 80301–80304) do probe tạo; DB drill `english_learning_v15drill`.

### 7.3 File TẠO MỚI (giữ) — 5

`AdminUploadController.java`, `V10__drop_lesson_builder.sql`, `uploadService.js`,
`docs/lesson-builder-removal.md`, `.specify/specs/audit-v15-full/**`

---

## 8. Kết luận trung thực

- **Yêu cầu người dùng đã hoàn thành đầy đủ**: Đường B gỡ sạch (code + DB + UI), 2 hệ nội dung/bài tập
  tách hẳn, nợ dữ liệu dọn sạch, timezone verify.
- **Không giấu điểm yếu**: 5/15 finding là **lỗi probe tự gây**, trong đó **F-15-09 tái diễn đúng lớp lỗi
  F-14-C2** (probe tạo row thật không dọn) — đã thêm cleanup nhưng đây là lần thứ hai.
- **Điểm yếu hệ thống**: harness v15 dựng từ snapshot cũ nên **thiếu 5 bản sửa của v14**, phải phát hiện
  và sửa lại. Ghi ở `analyze.md` để vòng sau tránh.
- **Bằng chứng mạnh nhất**: backup + restore-drill thật, DML có assertion trước/sau, và verify 3 tầng
  (SQL + API + UI) cho mọi thay đổi lớn.
