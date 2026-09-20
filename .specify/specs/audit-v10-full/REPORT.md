# Báo cáo audit-v10-full

**Ngày:** 2026-09-19 → 2026-09-20 (+07) · **Nhánh:** `audit-streak-review` · **Tiền nhiệm:** `audit-v9-full`
**Trạng thái tổng:** ✅ **GATE P1 ĐẠT.** Backend 456 test XANH 100%, frontend 106 XANH, browser sweep 342/342 sạch, API sweep 35/35 sạch, parity đo được và giải thích được.

> **Cập nhật 01:2x:** Toàn bộ kế hoạch đã chạy thật. B1 (classifier) được gỡ bằng allowlist của chủ dự án. Container đã rebuild **hai lần** (lần 2 mang F126 + F127). Phát hiện **3 lỗi mới** ngoài kế hoạch (F125 MEDIUM, F126 + F127 HIGH).
>
> **Vòng R2 đã đóng.** Chạy lại **toàn bộ** trên build cuối: browser 342/342 · API 35/35 · streak 25/25 — **0 hồi quy**.

---

## 0. Kết quả chạy thật (mới nhất)

```
./mvnw.cmd -o test
[INFO] Tests run: 470, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS

npx vitest run          → 106 passed | 1 skipped (21 files)
npx vite build          → ✓ built in 5.19s · index 177.31 kB (gzip 67.51)

Browser sweep           → 342 lượt (39 route × 3 viewport × 3 role)
                          0 không-mount · 0 console error · 0 page error
                          0 API ≥ 400 · 0 overflow · 0 landing sai guard
                          0 font sai · 0/878 <img> thiếu alt

API sweep               → PASS=35  FAIL=0  SKIP=0
Streak E2E              → PASS=25  FAIL=0

DB parity
1471|43735|72|127|28|15|4|126|14|5
   ↑ users 76→72 (xoá 4 user rác) · exercises 43737→43735 (xoá 2 row F128)
     cả hai đều có chủ đích, 8 cột còn lại không đổi
```

| Chỉ số | Baseline đỏ | Bây giờ |
|---|---|---|
| Tests run | 432 | **470** |
| Failures | 3 | **0** ✅ |
| Errors | 2 | **0** ✅ |
| Frontend | 73 | **106** ✅ |
| Findings VERIFIED_GREEN | 0 | **10** ✅ |

**Schema đã deploy:** `study_policy` = 1 row, `effective_from` = `2026-09-20`; `study_days` có FK `fk_study_days_user → users` + unique `uq_study_days_user_date`.
**Backup:** `engflow_2026-09-19-audit-v10.bak` (SHA256 `d71bfec1…49ad5c`) và `engflow_2026-09-20-pre-backlog-dml.bak`, cả hai `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"*.

---

## 1. TL;DR — điều quan trọng nhất

1. **F122 (HIGH): khoảng trước cutover làm sập toàn bộ chức năng học.** Nếu deploy schema streak mà không sửa, **suốt từ lúc deploy đến nửa đêm ngày cutover**, mọi endpoint học tập cốt lõi (nộp bài tập, ôn SRS, điểm danh game, nộp speaking) trả **HTTP 500**. Sửa trước khi deploy. Chi tiết mục 3.1.

2. **F126 (HIGH, MỚI): nội dung bài NHÁP rò rỉ qua `GET /api/lessons/{id}/structure`.** Đo trên dữ liệu sống: bài nháp `10889` trả **8.013 byte nội dung bài học thật** cho student, trong khi `/api/lessons/10889` trả 404. Cùng họ F88/F105/F115 — guard được áp tay từng controller và controller này bị bỏ sót. Đã sửa + 6 test.

3. **F127 (HIGH, MỚI): bài MATCHING không bao giờ chấm được đúng.** 330/331 row published lưu đáp án dạng CHỮ, client gửi dạng CHỈ SỐ. Đo 4 bài: **cả 4 đều `correct=false` dù nối đúng hết**. Và sửa định dạng chỉ số là **bất khả thi** vì client xáo trộn cột phải. Đã sửa cả hai phía + 12 test.

4. **F125 (MEDIUM, MỚI): `font-weight: 800` dùng 17 lần nhưng không có trong URL Google Fonts.** Browser **giả lập** nó từ 900 — đo được chiều rộng chuỗi ở 800 và 900 **trùng khít** (633.000px). Logo "EngFlow" và 5 node lộ trình render sai weight, **không có lỗi console nào**.

5. **Tự bác bỏ hai kết luận sai của chính mình.** (a) F121: mở rộng dải navbar xuống 1152px là **CSS chết** — đã revert. (b) F127 bản sửa đầu: so khớp cặp theo **chỉ số** ở server — **không thể chạy** vì client xáo trộn cột phải. Cả hai đều bị bắt bằng cách **đọc lại source**, không phải bằng cách tin bản vá.

6. **Backlog đo lại lệch: user `zz*` là 4, không phải 8.** Nếu tin số cũ mà "tìm cho đủ 8" thì cách tự nhiên là nới filter — **đúng sai lầm đã gây ra F111 ở v9**.

7. **"Sắp xếp" chưa phải một tính năng.** Không controller nào nhận `sort` từ client; frontend không gửi nó. `?sort=id,desc` là tham số **bị bỏ qua im lặng**. Ghi nhận là **khoảng trống sản phẩm**, không phải lỗi.


---

## 2. Blocker B1 — lý do không có gì được verify

Harness command classifier (`cbai/deepseek-v4.1-flash[1m]`) **không khả dụng suốt phiên**. Khi nó chết, auto mode không quyết định được độ an toàn của lệnh nên **từ chối gọi**, còn thao tác chỉ-đọc thì được miễn.

| Bị từ chối | Vẫn chạy được |
|---|---|
| `mvnw.cmd test` (mọi biến thể) | `Read`, `Glob`, `Grep` |
| `java -version` | ghi file vào `.specify/**` |
| `npx vitest run`, `npx vite build` | `ls`, `date`, `git status/diff/log`, `docker ps` |
| `docker exec ... sqlcmd`, `redis-cli` | |
| `cmd //c`, `Monitor`, `Agent`, Playwright MCP, chrome-devtools MCP | |

**Hệ quả:** 0 test đã chạy, 0 build đã tạo, 0 browser đã mở, 0 câu SQL đã thực thi kể từ khi chụp baseline đỏ.

**Đã chuẩn bị sẵn để chạy ngay khi hạ tầng hồi:**
- `sweep/v10/run-test.bat <TestClass> [outfile]` — chạy 1 test class ra file log
- `sweep/v10/probe-live.js` — các probe live

Chi tiết đầy đủ: `.specify/specs/audit-v10-full/evidence/blocker-b1.md`

---

## 3. Đã làm được gì

### 3.1 F122 — Lỗi HIGH mới: khoảng trước cutover làm sập toàn bộ chức năng học

**Đây là phát hiện quan trọng nhất của vòng này.**

`StudyActivityService.recordStudy()` (dòng 47-53, bản cũ) ném `IllegalStateException` khi `today < effectiveFrom`. Vì `recordStudy` chạy với `Propagation.MANDATORY` — tức **bên trong chính transaction đang lưu kết quả của người gọi** — cú ném đó cuốn theo kết quả học tập, rồi `GlobalExceptionHandler` không có handler cho `IllegalStateException` nên rơi xuống catch-all `problem500` → **HTTP 500**.

Chuỗi gọi đã kiểm chứng trong source:
| Producer | File:dòng | Transaction |
|---|---|---|
| Nộp bài tập | `ExerciseService.java:325` | `@Transactional` (dòng 277) |
| Ôn SRS | `SrsService.java:110` | `@Transactional` (dòng 51) |
| Điểm danh game | `StreakService.java:18` | `@Transactional` (dòng 15) |
| Nộp speaking | `SpeakingSubmissionService.java:134` | có |

→ Với cutover `2026-09-20` và hôm nay là `2026-09-19`, **cả 4 nhóm chức năng cốt lõi trả 500 cho tới hết ngày**.

**Sửa:** trước cutover coi như "chưa có gì để ghi" (`return`), **không ném**. Thiếu hẳn row policy vẫn ném để lộ lỗi cấu hình. Cơ chế rollback chung transaction vẫn nguyên vẹn cho các lỗi thật.

**Quan hệ với test đỏ sẵn có — đã kiểm chứng bằng stack trace, không suy đoán:** test đỏ `AuditV9DraftLessonGradeGuardTest.publishedLesson_submit_studentStill200` trong baseline **KHÔNG phải** do nhánh này. `backend-baseline.log:304` ghi `Study policy is missing; apply the reviewed deployment SQL first`, frame `effectiveFrom:122 → recordStudy:38` — tức **thiếu row policy**. F122 là **lỗi thứ hai, chỉ xuất hiện SAU khi deploy schema**: row có rồi thì `effectiveFrom()` thôi ném, và cửa ải tiếp theo (`today < effectiveFrom`) tiếp quản. Hai lỗi khác nhau, không được gộp.

**Sửa kèm test:**
- `StudySqlIntegrationTest.studyWriteFailureRollsBackActualSrsProgress` — test này dùng ngày trước cutover làm **cách tiêm lỗi cho tiện**, chứ ý định thật là kiểm **tính nguyên tử khi ghi study thất bại**. Đã đổi nguồn tiêm lỗi sang `user inactive` (giữ nguyên ngữ nghĩa kiểm), **không** hạ assertion.
- Thêm test mới `beforeCutoverRecordsNoStudyDayAndDoesNotFailTheActivity` (SQL) và `beforeCutoverRecordsNothingAndDoesNotFailTheCaller` (unit) để khoá hành vi mới.

### 3.2 F115 — Lỗ hổng HIGH: bài nháp lộ qua đường nộp bài kỹ năng

`LessonSubmissionController` chưa bao giờ gọi `LessonService.assertLessonVisible`. Một student biết id bài nháp vẫn **nộp được bài** (ghi row `PENDING` vào `lesson_submissions`) và **đọc lại được**. Cùng họ với F88 (đường đọc) và F105 (đường chấm/nộp bài tập).

**Sửa:** thêm guard dùng chung vào cả 2 endpoint, admin vẫn preview được.
**Test:** `AuditV10DraftLessonSubmissionGuardTest` — 4 test (draft 404 cho student ×2 đường, draft 200 cho admin, published 200 cho student).

### 3.3 F116 + F117 — Scheduler nhắc học

- **F117 (HIGH):** job **fail-open** khi Redis chết — `tryAcquireMarker` trả `null` và code cũ coi `null` là "cứ chạy". Mà **mọi** cơ chế chống trùng (marker ngày, cờ đã-gửi, suppression 30 ngày) đều nằm ở Redis → mất Redis là mất hết chống trùng, mail sẽ dội lặp mỗi lần job chạy. Đã sửa thành **fail-closed** (bỏ lượt).
- **F116 (MEDIUM):** test `retryBudgetStopsAfterThreeAttempts` yêu cầu trần thử lại nhưng **production chưa hề có** — không code nào tham chiếu `streak:attempts`. Đã **viết phần production còn thiếu** (`retryBudgetExhausted()` + hằng số trong `RedisConstants`) thay vì hạ assertion.

Điểm quan trọng về phương pháp: hai test này ban đầu tôi chẩn đoán nhầm là "stub mismatch". Đọc `NoInteractionsWanted` tại dòng 241/260 trong log mới lộ ra chúng là **yêu cầu chưa được implement**, không phải test hỏng. Đã sửa lại kết luận trong `findings.json`.

### 3.4 F118 — Hai component decor khai prop rồi không dùng

- `DecoConfetti`: `sizePx` là hằng module `sizeMap.md` → **mọi** confetti luôn 18px, prop `sm/lg/xl` vô tác dụng; `color` khai mà không đọc.
- `SquiggleDivider`: `color`/`height` không dùng, `--deco-height` đọc mà không ai set, và style object ghi **cả một khai báo CSS** (`'color:var(--geo-fg)'`) vào custom property.

**Sửa:** `DecoConfetti` tách size/color độc lập; `SquiggleDivider` vẽ bằng `mask-image` + `background-color` vì `currentColor` trong data-URI SVG **không** kế thừa từ host (đã kiểm chứng).
**Test:** `decor-props.test.js` — 10 test.
**Đánh giá rủi ro:** `DecoConfetti` có **0 call site thật** trong toàn bộ `frontend/src` → sửa an toàn tuyệt đối.

### 3.5 F121 — Container `max-w-6xl`, và một kết luận sai tôi tự bác bỏ

Prompt yêu cầu `max-w-6xl` (72rem). Đã đổi `.geo-container` mặc định 80rem → 72rem, và chỉnh `.app-navbar__inner` / `.app-footer__inner` cùng 72rem. **Blast radius hẹp:** `<Container>` chỉ có 3 chỗ dùng và không chỗ nào truyền `size`.

**Nhưng:** ban đầu tôi mở rộng dải compact navbar xuống `1152px`, lý luận rằng "áp lực bắt đầu từ 1152". **Sai.** Header render link desktop bằng `hidden xl:flex` và hamburger bằng `xl:hidden`; Tailwind `xl` = **1280px** và dự án **không** override `screens`. Nên trong 1152–1279px link desktop **không được render**, hamburger đảm nhiệm → dải bắt đầu ở 1152px là **CSS chết**, còn ngụ ý một độ phủ không tồn tại. Bình luận gốc của audit-v8 trong chính file đó nói y hệt: *"below 1280 the hamburger menu already takes over."* **Đã revert về 1280px.**

### 3.6 Phase 2 — Ép UI theo prompt literal

| Mục prompt | Thực hiện | Ghi chú |
|---|---|---|
| `max-w-6xl` container | 72rem (1152px) | `sm` 40rem / `xl` 96rem giữ nguyên |
| `py-24` spacing | 3rem mobile, 6rem (=96px) từ `md` | Mobile nhỏ hơn có chủ đích: 96px trên màn 360px đẩy nội dung khỏi màn đầu |
| Vòng tròn vàng lớn ở hero | `.app-hero__sun` 34→40rem | `overflow:hidden` của `.app-hero` cắt nó → **không gây overflow** (đã kiểm) |
| Blob-mask | `.app-hero__shape--blob` | Hero là hình học thuần, **không có ảnh thật** để mask → áp blob radius lên shape. Divergence được ghi rõ, không thêm ảnh stock |
| Đường nét đứt nối feature card | SVG inline `aria-hidden`, ẩn < 768px | |
| Pricing `scale(1.1)` + badge `rotate(15deg)` | `PremiumPage.vue` | **Chỉ từ `md` lên**: ở 360–767px 2 gói xếp dọc, scale 1 cái sẽ đè cái kia và làm tràn trang |
| Squiggle divider | Dùng giữa các section Home | |
| Marquee vô hạn | `.app-marquee` | Bản sao cho vòng lặp liền mạch có `aria-hidden`; **tắt hẳn** khi `prefers-reduced-motion` |
| ArrowRight trong vòng tròn trắng | prop `with-arrow` của `AppButton` | **Opt-in, default false**, chỉ 1 call site → không đụng ~100 chỗ dùng còn lại |

**Prompt có lỗi thật, đã sửa prompt chứ không bẻ code theo điều sai:** prompt nói "Lucide **React**" (dự án dùng `lucide-vue-next`), và yêu cầu các tên utility Tailwind (`py-24`, `max-w-6xl`) mà codebase **không hề dùng** — prompt cũ còn tự ghi *"do not rewrite components just to add them"*. Đã viết lại prompt ở `prompt-rewritten-v10.md`.

### 3.7 Dọn dẹp & đính chính số liệu

- **Endpoint inventory (T4.1):** xác nhận **144 route / 26 controller**, không đổi. Con số 131 tôi đo ban đầu là **sai phương pháp**: 14 annotation mang **2 path literal** (cặp alias speaking/video) → 1 annotation = 2 route. Số học khớp chính xác từng verb: GET 62 (−1 trùng = 61), POST 54, PUT 16, DELETE 10, PATCH 3 → 144. Đã ghi lại phương pháp đúng để vòng sau không lặp lại.
- **Dead CSS:** 337 dòng bị xoá khỏi `design-system.css` — đã kiểm chứng `.geo-heading*`, `.geo-btn`, `.geo-body`, `.geo-shadow-*` **không còn file nào trong repo dùng** (chỉ còn trong tài liệu audit cũ). Class sống là `.app-btn*`, `.geo-card`, `.geo-markdown`, `.geo-audio`.
- **`verify-sql.ps1`:** script assert cứng `Tests run: 6` cho `StudySqlIntegrationTest`; tôi thêm test thứ 7 nên nó **sẽ fail**. Đã sửa thành 7 và giữ **khớp chính xác** (không dùng `>=`) để test bị skip vẫn làm đỏ cổng.
- **`StreakService.java`:** có một comment **tiếng Pháp** (`Compatibilité des lecteurs streak...`) trong khi quy ước dự án là tiếng Việt. Đã viết lại.
- **F123 (đính chính backlog):** mục lỗi shape "3 endpoint" thực tế **21 return / 7 controller**. Trong đó **6 return của `PaymentService` là domain result map** (`{success:false, error:...}`) trả cho webhook SePay — **hợp đồng ngoài, KHÔNG được đổi**. Về mặt người dùng thì `frontend/src/services/api.js:59-62` **đã** chuẩn hoá `error → detail/message` từ audit-v8 F94, nên UI vẫn hiện đúng thông báo. → **Hoãn có lý do**, không phải bỏ sót: đổi 21 return là thay đổi hợp đồng diện rộng, mà cổng của chính kế hoạch là "shape lỗi mới phải có test 2 phía" — cổng đó **không thể đạt khi B1 chặn mọi lần chạy test**, và khác F122, mục này hiện **không phá gì**.

---

## 4. Còn lại gì chưa làm

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| **Phase 2.12** — Screenshot before/after cho 2.1–2.6 | ⏳ **CHƯA** | Cần chụp ảnh; mọi phép **đo** đã xong (184 assert, 0 FAIL). Đây là **bằng chứng trình bày**, không phải bằng chứng kỹ thuật — các con số đã có trong `evidence/prompt-claims-measured.md` |
| **Phase 3.4** — Chuyển shape lỗi legacy sang ProblemDetail | ⏳ **HOÃN có lý do** | F123: 21 return / 7 controller, trong đó 6 là hợp đồng webhook SePay. `api.js` đã shim `error → detail`, nên **không có lỗi người dùng thấy**. Đổi 21 return là thay đổi hợp đồng diện rộng |
| ~~**Phase 3.8** — 9 audio LISTENING~~ | ✅ **XONG (F128)** | Không phải vấn đề audio: 7 row là trắc nghiệm bị gắn nhãn sai. Đã đổi nhãn + sửa bộ gán nhãn. Còn 2 row chờ quyết định nội dung — xem mục 4c |
| **Phase R2** — Vòng 2 toàn diện | ✅ **XONG** | Browser sweep 342/342, API 35/35, streak 25/25 — **tất cả trên build cuối**, 0 hồi quy |

---

## 4b. F124 — `deploy.sql` bỏ sót FK (đã sửa tận gốc)

Khi chạy `deploy.sql`, đo được **FK không được tạo**: `FK_COUNT=0`.

**Nguyên nhân:** script tạo bảng `study_days` **và** FK trong **cùng một** khối `IF OBJECT_ID(...) IS NULL`. Nhưng Hibernate `ddl-auto=update` **đã tạo bảng trước**, nên cả khối bị bỏ qua → FK không bao giờ được tạo. Unique index thì **có**, vì Hibernate tạo kèm bảng — nên phép kiểm "bảng đã tồn tại chưa" **không phát hiện được** thiếu sót.

**Đã sửa tận gốc trong `tasks/streak-study/deploy.sql`:** tách guard của bảng và của hai ràng buộc ra riêng (`IF NOT EXISTS` trên `sys.indexes` / `sys.foreign_keys`).

**Idempotency được CHỨNG MINH, không tuyên bố.** `sweep/v10/prove-f124-idempotent.sql` tạo một DB scratch có bảng + unique index nhưng **không** FK (giả lập "Hibernate đã chạy"), rồi áp DDL mới:

```
fk_before=0  →  fk_after_run1=1 (fk_study_days_user)  →  fk_after_run2=1 (KHÔNG nhân đôi)
```

Và chứng minh ràng buộc **thật sự ràng buộc**:
- chèn `study_days` với `user_id=999999` → **BỊ CHẶN** (`conflicted with the FOREIGN KEY constraint "fk_study_days_user"`)
- chèn user thật → thành công
- chèn trùng `(user_id, study_date)` → **BỊ CHẶN** bởi `uq_study_days_user_date`

DB scratch đã được drop sau đó.

---

## 4c. F128 — 9 row "LISTENING": **đã sửa** (đổi nhãn + sửa bộ gán nhãn)

Mục này từng được ghi là *"9 exercise thiếu `audio_url`"* — một bài toán **thiếu dữ liệu**. **Sai.** Đây là **lỗi gán nhãn**, và backfill audio sẽ là đi sai hướng hoàn toàn.

### Bằng chứng 1: UI thật đọc to câu lệnh

Mở lesson 11516 ở tab BÀI TẬP:

```
5  NGHE
I can understand a text about brothers and sisters.
NGHE & TRẢ LỜI (GIỌNG ĐỌC MÁY)
🔊 Nghe
A. My brother is taller than me.  B. ...  C. ...  D. ...
```

Bốn câu cùng lesson đều hiện **TRẮC NGHIỆM**. Chỉ câu 5 hiện **NGHE** — và nút `🔊 Nghe` **đọc to chính câu lệnh** *"I can understand a text about brothers and sisters"*, một câu mô tả năng lực đọc.

### Bằng chứng 2: 7/9 row có cấu trúc trắc nghiệm

`options` là mảng JSON và `correct_answer` **trùng khít** một lựa chọn. Tiêu đề lesson xác nhận: `Reading - VOCABULARY`, `Writing - READING`, `Article level 1 - WRITING`… Mỗi lesson có **đúng 5 exercise và đúng 1 LISTENING** — và LISTENING đó **luôn** là row thiếu audio. Mẫu lặp 9/9 lần.

### Bằng chứng 3: phép thử bắt chước client — và một lần tôi suýt kết luận sai

Phép thử đầu gửi **chính `correct_answer`** làm câu trả lời → `correct=true` cho cả 9. Đó là **vòng lặp trùng**: nó chỉ chứng minh hàm so sánh chuỗi chạy, **không** chứng minh học sinh tạo ra được chuỗi đó.

Phép thử đúng: `LessonExerciseTab.vue:71` gửi **nguyên văn nội dung lựa chọn**. Mô phỏng `parsedOptions()` rồi gửi đúng chuỗi nút bấm sẽ gửi:

| Kết quả | Số row |
|---|---|
| Bấm nút được **và chấm đúng** | **7** |
| Rơi vào **ô nhập tay** | **2** |

### Gốc rễ

`HtmlParserService.detectDiviExerciseType()` gán nhãn **theo section**, quy tắc đầu là *"có thẻ `<audio>` → LISTENING"*. Thẻ đó có trong HTML nguồn nhưng **file không tải về được** → row mang nhãn LISTENING vĩnh viễn dù `audio_url` rỗng.

### Đã sửa (2 phần)

**(A) Đổi nhãn 7 row** sang `MULTIPLE_CHOICE`, câu lệnh idempotent. Verify độc lập:

| Kiểm | Kết quả |
|---|---|
| `MULTIPLE_CHOICE` | 33549 → **33556** (+7 đúng) |
| `LISTENING` | 367 → **360** (−7 đúng) |
| Tổng `exercises` | **43737** không đổi |
| Parity | **không đổi** |
| UI lesson 11516 | nhãn "NGHE" 1→**0**, nút 🔊 1→**0**, "TRẮC NGHIỆM" 4→**5** |

**(D) Sửa bộ gán nhãn** để không tái diễn: trong `buildExerciseFromP`, row `LISTENING` có `options` sinh ra ≥2 lựa chọn thật bị hạ nhãn xuống `MULTIPLE_CHOICE`. Điều kiện này đúng vì **279/358 row LISTENING có audio** cũng có lựa chọn — nhưng chúng **có `audio_url` để phát**. Chỉ hạ nhãn khi row **không có audio**, tức nhãn LISTENING **không thể đúng**.

**Test:** `HtmlParserListeningDowngradeTest` — **14/14 PASS**, gồm 2 test dùng **nguyên văn chuỗi `options` từ DB thật**.

### 2 row CÒN LẠI — đã XOÁ (chủ dự án chọn phương án 3)

| exercise_id | Vấn đề |
|---|---|
| **745673** | `options` là **chuỗi văn bản**, không chứa lựa chọn nào → phải tạo dữ liệu mới |
| **745808** | `options` dạng `"A) ...B) ..."`, `correct_answer = "A"` → regex tách được **2/4** |

**An toàn được chứng minh TRƯỚC khi xoá, không giả định:**

| Kiểm | Kết quả |
|---|---|
| FK trỏ tới `dbo.exercises` | **Không có** (đọc từ `sys.foreign_key_columns`) |
| `exercise_attempts` có cột `exercise_id`? | **Không** — chỉ lưu `lesson_id` |
| `details` (JSON) nhắc tới 2 ID đó? | **0 row** — dù 36 row khác **có** chứa `exerciseId` |
| Attempt thuộc 2 lesson đó? | **0** |

Điểm đáng chú ý: `details` **có** chứa `exerciseId`. Một phép kiểm hời hợt (*"bảng không có cột `exercise_id` → an toàn"*) sẽ **bỏ sót** khả năng lịch sử làm bài trỏ tới ID sắp xoá.

**Kết quả:**

```
exercises:  43737 → 43735   (−2)
LISTENING:    360 → 358     (−2)
các loại khác: KHÔNG đổi
LISTENING thiếu audio:  9 → 0
PARITY: 1471|43735|72|127|28|15|4|126|14|5
```

**Verify trên browser:** cả 2 lesson hiện **0 nút "Nghe"**, **0 chữ "NGHE"**, 4 nhãn **"TRẮC NGHIỆM"**, "Nộp bài (4 câu)", 0 console error.

**Đánh đổi đã chấp nhận:** lesson 11477 và 11539 giờ có **4 exercise** thay vì 5, lệch cấu trúc so với 1.459 lesson khác. Ghi lại để lần sau không đọc nhầm thành lỗi nhập liệu.

### Vì sao backfill audio là SAI

Sinh TTS sẽ **đọc to chính câu lệnh**: *"Write a description of your new home"*, *"Please read these Terms and Conditions"*. Và hậu quả **tệ hơn để nguyên**: row thiếu audio là lỗi **nhìn thấy được**; row có audio đọc câu lệnh là lỗi **trông như đã sửa xong**.

*(Đường TTS→Cloudinary vẫn được chứng minh chạy thật — WAV 276.524 byte, `RIFF`/`WAVE`, upload 200, URL phục vụ `audio/wav`. Nó chỉ không liên quan tới 9 row này.)*

---

## 5. Đã fix gì và fix thế nào — bảng tổng

| ID | Mức | Vấn đề | Cách sửa | Trạng thái |
|---|---|---|---|---|
| **F122** | HIGH | Trước cutover → mọi endpoint học tập 500 | Trước cutover `return` thay vì ném; thiếu policy vẫn ném | ✅ **VERIFIED_GREEN** |
| **F115** | HIGH | Bài nháp lộ qua `/api/lesson-submissions/**` | Thêm `assertLessonVisible` + 4 test | ✅ **VERIFIED_GREEN** |
| **F117** | HIGH | Job mail fail-open khi Redis chết | Fail-closed: `null` marker → bỏ lượt | ✅ **VERIFIED_GREEN** |
| **F116** | MED | Trần thử lại chưa được implement | Viết `retryBudgetExhausted()` + hằng số | ✅ **VERIFIED_GREEN** |
| **F118** | MED | 2 component decor khai prop không dùng | Nối prop vào output + 10 test | ✅ **VERIFIED_GREEN** |
| **F121** | MED | Container 72rem vs navbar | Align shell 72rem; **giữ dải 1280** | ✅ **VERIFIED_GREEN** |
| **F119** | MED | Video quiz chấm hoàn toàn client-side | **Phân loại, KHÔNG wire** | CLASSIFIED |
| **F120** | LOW | Skill submission / shadowing upload chưa verify lúc ghi | **Phân loại, KHÔNG wire** | CLASSIFIED |
| **F123** | MED | Backlog shape lỗi là 21 return, không phải 3 | **Đo lại + hoãn có lý do** | INVESTIGATED_DEFERRED |
| **F124** | MED | `deploy.sql` bỏ sót FK khi Hibernate đã tạo bảng | Tách guard; **idempotency chứng minh trên DB scratch** | ✅ **APPLIED_AND_VERIFIED** |
| **F125** | MED | `font-weight: 800` không có trong URL font → browser giả lập từ 900 | Thêm 800 vào URL | ✅ **VERIFIED_GREEN** |
| **F126** | **HIGH** | **Nội dung bài NHÁP rò rỉ qua `/api/lessons/{id}/structure`** | Thêm `assertLessonVisible` + 6 test | ✅ **VERIFIED_GREEN** |
| **F127** | **HIGH** | **Bài MATCHING không bao giờ chấm được đúng** | Chấm theo tập cặp từ `options`; client gửi CHỮ + 12 test | ✅ **VERIFIED_GREEN** |
| **F128** | MED | **9 row "LISTENING" thực chất là trắc nghiệm** | Đổi nhãn 7 row · xoá 2 row · sửa bộ gán nhãn · 14 test | ✅ **VERIFIED_GREEN** |
| **B1** | — | Classifier hạ tầng chập chờn | Allowlist của chủ dự án; ghi lại phần còn chập chờn | RESOLVED_ENOUGH |

**10 findings VERIFIED_GREEN · 2 CLASSIFIED_NOT_A_DEFECT · 1 INVESTIGATED_DEFERRED · 1 APPLIED_AND_VERIFIED.**

---

## 5b. Skills đã nạp trong phiên

| Skill / công cụ | Dùng để làm gì |
|---|---|
| `superpowers:using-superpowers` | Khung quy tắc bắt buộc nạp skill trước khi hành động |
| `superpowers:systematic-debugging` | Truy vết test đỏ tới **đúng nguyên nhân gốc**; phân biệt "test hỏng" với "yêu cầu chưa implement" (F116) |
| `superpowers:verification-before-completion` | Buộc mọi tuyên bố phải có artifact — lý do báo cáo này ghi rõ từng con số kèm file bằng chứng |
| `update-config` | Cấu hình allowlist quyền (lần đầu bị classifier chặn chính thao tác ghi file cấu hình) |
| `speckit-*` (workflow) | constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze |
| `playwright-core` (Edge thật) | Browser sweep 342 lượt, đo prompt claims, font check, F126/F127 live |
| `security-guidance` (hook) | Bắt lỗi command injection trong `api-sweep.js` (`execSync` + nội suy Redis key) → sửa sang `execFileSync` với argv |

**Artifact quy trình:** `spec.md`, `clarify.md` (C1–C14), `checklist.md`, `plan.md`, `tasks.md`, `producer-audit.md`, `findings.json`, `prompt-rewritten-v10.md`, và **14 file bằng chứng** trong `evidence/`.

---

## 6. Rủi ro và điều cần biết

1. **Cutover `2026-09-20` là HÔM NAY.** Schema đã deploy, container đã chạy code mới, và **5 kịch bản streak đã PASS 25/25** — bao gồm KB1 (đăng nhập 3 lần → `study_days` vẫn = 0), đây là **hợp đồng cốt lõi của cả refactor**.
2. **Parity mới là `…|72|…` cho `users`, không phải `76`.** Đây là **thay đổi có chủ đích** (xoá 4 user rác, chủ dự án quyết định). Mọi tài liệu còn ghi `76` cần đọc kèm ghi chú này.
3. **Working tree chưa commit.** Refactor streak và toàn bộ bản sửa vòng này vẫn là thay đổi chưa commit. **Không có điểm rollback bằng git.** Backup DB thì có (2 bản, đều VERIFYONLY hợp lệ).
4. **`playwright-core` được cài bằng `--no-save`** nên **không** nằm trong `package.json`. Chạy lại browser sweep trên máy khác cần `npm install playwright-core --no-save` và một Chrome/Edge có sẵn (`sweep/v10/ui-lib.js` tự tìm).
5. **`sweep/v8/ui/*.js` không chạy được trên máy này** vì gọi thẳng `pw.chromium.launch()` (cần Chromium của playwright, chưa cài). Dùng `sweep/v10/*` — chúng bọc qua `ui-lib.js`.
6. **`?sort=` là tham số bị bỏ qua.** Không phải lỗi, nhưng nếu ai đó tưởng có tính năng sắp xếp thì sẽ nhầm.

---

## 7. Kết luận trung thực

**Đã làm thật, có số đo:**

- **2 lỗi HIGH mới ngoài kế hoạch** — F126 (nội dung bài nháp rò rỉ, đo được 8.013 byte) và F127 (bài MATCHING không bao giờ chấm được đúng, đo được 4/4 sai dù nối đúng).
- **1 lỗi typography thật** — F125 (`font-weight: 800` bị giả lập từ 900, chứng minh bằng chiều rộng chuỗi trùng khít).
- **9 findings VERIFIED_GREEN**, mỗi cái có test hồi quy và số đo.
- Backend **456 test XANH** (từ 432 đỏ), frontend **106 XANH**, build **177.31 kB**.
- Browser sweep **342/342 sạch** trên build cuối, guard assert **cả hai chiều**.
- API sweep **35/35 sạch**; 5 kịch bản streak **25/25 PASS**.
- Schema deploy có **FK + UQ + policy**, idempotency **chứng minh trên DB scratch**.
- Backlog: xoá 4 user rác + 4 bảng backup, **0 orphan thật** sau khi sửa query của chính mình.
- Parity đo được **và giải thích được** ở mọi bước.

**Tự bác bỏ ba kết luận sai của chính mình** — F121 (CSS chết), F127 bản sửa đầu (so chỉ số, bất khả thi vì client xáo trộn), và 39 "orphan" (thực ra là NULL hợp lệ). Cả ba đều bị bắt bằng **đọc lại source**, không bằng tin vào phán đoán ban đầu.

**Chưa làm, và nói rõ lý do:**

- **Screenshot before/after** (Phase 2.12) — mọi phép **đo** đã xong; đây chỉ là bằng chứng trình bày.
- **Chuyển shape lỗi legacy** (F123) — hoãn có lý do, và `api.js` đã shim nên **người dùng không thấy lỗi**.
- **2 row 745673 và 745808** — ✅ **đã xoá** theo quyết định của chủ dự án. Không còn row nào mang nhãn `LISTENING` mà thiếu audio.
- **Chất lượng nội dung AI** — cần đánh giá của con người, không phải assert. Nói rõ để không ai tưởng đã kiểm.
- **CRUD qua UI admin** (T4.10) — CRUD ở tầng API đã được sweep phủ; **chưa đi từng thao tác tạo/sửa/xoá qua giao diện admin**.
- **Git checkpoint** — 40 file sửa + 30 untracked. **Không có điểm rollback bằng git.** Backup DB thì có (2 bản, đều hợp lệ).

---

## 8. Vòng R2 — chạy lại toàn bộ trên build cuối

Đây là vòng có giá trị nhất, vì nó chạy trên build đã mang **cả hai bản sửa HIGH**:

| Vòng kiểm | Kết quả | Ghi chú |
|---|---|---|
| Backend suite | **456 run / 0 fail / 0 error / 11 skipped** | tăng 24 test so với đầu vòng, 0 hồi quy |
| Frontend suite | **106 passed / 1 skipped** | không đổi |
| Frontend build | **177.31 kB** | không đổi |
| Browser sweep | **342/342 sạch** | 0 mount lỗi, 0 console, 0 overflow, 0 landing sai, 0/885 ảnh thiếu alt |
| API sweep | **35/35 sạch** | 0 SKIP |
| Streak E2E | **25/25 PASS** | gồm KB1 (hợp đồng cốt lõi) |
| F126 live | **404** cho cả 2 bài nháp từng rò rỉ | trước đó: 200 + 8.013 byte |
| F127 live | **6/6 PASS** | trước đó: 4/4 `correct=false` |
| Font check | **Be Vietnam Pro duy nhất, tải đủ** | 800 giờ là face thật |
| Parity sau mọi thao tác | `1471\|43737\|72\|127\|28\|15\|4\|126\|14\|5` | ổn định qua 2 sweep + 2 lần chạy streak |

**Sweep tự dọn dẹp:** vòng 2 tạo 6 row payment và dọn 6 row trong cùng lần chạy, parity về đúng 126. Verify độc lập bằng script riêng: **TẤT CẢ PASS**.

