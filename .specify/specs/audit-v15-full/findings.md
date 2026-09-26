# audit-v15-full — findings

Quy ước: `F-15-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy ·
**probe thứ 2 độc lập**.

Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`

---

## F-15-01 — Endpoint public rò rỉ `correctAnswer` của block QUESTION cho người ẩn danh — **HIGH** — `FIXED (bằng cách gỡ Đường B)`

**Nguồn:** phát hiện khi khảo sát A2 (không nằm trong danh sách OPEN của v14).

**Bằng chứng:** `GET /api/lessons/447/structure` (public, `LessonStructureController.java:135`) trả về raw
`data` JSON của mỗi block; block QUESTION chứa `correctAnswer`. Đo DB (3 block):
`{"questionType":"MULTIPLE_CHOICE","correctAnswer":"goes",...}` (block 4),
`{"questionType":"FILL_IN_BLANK","correctAnswer":"play",...}` (block 5),
`{"questionType":"TRUE_FALSE","correctAnswer":"True",...}` (block 7).

**Root cause:** `LessonStructureService.toSectionResponse` map `data` verbatim, không lọc field theo vai trò.

**Fix:** gỡ toàn bộ Đường B → endpoint biến mất. **Probe 2:** anon `GET /api/lessons/447/structure` → **404**.

---

## F-15-02 — `LessonStructureController` trộn endpoint dùng chung với Đường B — **MEDIUM** — `FIXED (tách)`

**Nguồn:** phát hiện khi lập kế hoạch gỡ.

**Bằng chứng:** cùng class chứa 3 endpoint **không** thuộc Đường B:
- `POST /api/admin/upload` — `AdminExercises.vue:483` gọi để upload ảnh/audio bài tập.
- `GET /api/resources/{filename}` — `LessonSubmissionService.java:119` sinh URL cho file speaking;
  `SecurityConfig.java:117` permitAll; `AuditV8UploadXssTest` phủ 6 assertion (XSS/path-traversal).
- `POST /api/admin/audio-upload` — không còn consumer frontend nào (chỉ `RateLimitFilterTest`).

**Root cause:** endpoint dùng chung bị đặt nhầm trong controller của Đường B.

**Fix:** tách 3 endpoint sang `AdminUploadController` (+ service nếu cần) trước khi xoá Đường B.
**Probe 2:** `AuditV8UploadXssTest` + `RateLimitFilterTest` xanh; `AdminExercises` upload được ảnh.

---

## F-15-03 — `AdminExercises.vue` (ở lại) phụ thuộc service của Đường B — **MEDIUM** — `FIXED`

**Bằng chứng:** `AdminExercises.vue:356` `import lessonStructureService`, dùng `uploadFile` (dòng 483).

**Fix:** tạo `frontend/src/services/uploadService.js` (chỉ `uploadFile`) và đổi import.

---

## F-15-04 — 6/8 lesson có block TEXT **trùng byte-for-byte** với `lesson.content` — **LOW (dữ liệu)** — `FIXED (xoá)`

**Bằng chứng** (`LEN(block.data)` vs `LEN(lesson.content)`):

| lesson | block_len | content_len | kết luận |
|---|---|---|---|
| 446 | 5986 | 5986 | **trùng byte-for-byte** |
| 567 | 7248 | 7248 | **trùng byte-for-byte** |
| 10889 | 7652 | 7652 | **trùng byte-for-byte** |
| 11301 | 7689 | 7689 | **trùng byte-for-byte** |
| 41881 | 2621 | 2621 | **trùng byte-for-byte** |
| 91900 | 32275 | 33657 | gần trùng (chênh 1382) |
| 447 | 42/204/306/259 | 6621 | **tự soạn thật** (khớp tiêu đề "Present simple") |
| 11300 | — | NULL | section rỗng (Unicode Test 2, draft) |

**Kết luận:** 6 lesson lặp nội dung scrape dưới dạng block → hiển thị **trùng** cho người học.
**Fix:** xoá toàn bộ (gỡ Đường B). Chỉ lesson 447 có nội dung tự soạn, và 3 câu hỏi của nó được
**chuyển sang `exercises`** trước khi xoá.

---

## F-15-05 — Bảng chưa từng dùng ở prod (`lesson_snapshots` chỉ 5 hàng, toàn test) — **LOW** — `FIXED`

**Bằng chứng:** `lesson_snapshots` = 5 hàng (`snapshot_id` 30004–30008); `created_by` không trỏ user rác nào;
không có UI nào ngoài `AdminLessonBuilder` gọi snapshot. **Fix:** DROP bảng.

---

## F-15-06 — Script migrate của v15 dùng `GETDATE()` (UTC) thay vì naive-VN — **LOW** — `FIXED`

**Nguồn:** tự phát hiện ở Phase 5 khi đo lại F-13-09 (chính v15 gây ra).

**Bằng chứng:** 3 hàng migrate ở Phase 2 dùng `GETDATE()` (SQL container chạy **UTC**) →
`created_at = 2026-09-24 19:48`, trong khi thời điểm thật là `2026-09-25 02:48 +07`. **Đúng cái bẫy
F-13-09 cảnh báo.**

**Fix:** `UPDATE exercises SET created_at = DATEADD(HOUR,7,created_at), updated_at = …` cho đúng
3 hàng (787912–787914), `BEGIN TRAN` + `COMMIT`. Sau fix: `2026-09-25 02:48:50` naive-VN, `@@TRANCOUNT=0`.

---

## F-15-07 — Harness v15 còn khẳng định hành vi Đường B (6 chỗ) — **MEDIUM** — `FIXED`

**Bằng chứng:** sau khi gỡ, các probe báo fail/`Msg 208`:
`api-sweep.js:160,172,412-416` (structure 200, snapshots 200/403/401), `perf-probe.js` (target structure),
`coverage-sweep.js` (parity đếm `lesson_snapshots` → `Msg 208`), `ui-sweep.js:78` (`/build` cho admin).

**Fix:** cập nhật mọi khẳng định sang hành vi mới (structure → **404**; snapshots admin → **404**,
student **403**, anon **401**; `/build` → **`/`**; bỏ target + cột đã gỡ).
**Probe 2:** `api-sweep` **143/0**; `deep-probe` **58/0**.

---

## F-15-08 — 2 assertion stale từ v13 tái nhập vào harness v15 — **LOW (probe SAI)** — `FIXED`

**Bằng chứng:** `deep-probe.js` khẳng định LISTENING bare-letter options → 400 và copy row 651717 → 400;
đo thật **200** cả hai. Đây là 2 assertion **đã sửa ở audit-v14** (F-13-13 guard là MC-scoped theo thiết kế;
F-13-12 "A - Salad" là option hợp lệ) nhưng bản harness v15 khôi phục lại bản cũ.

**Fix:** áp lại bản sửa v14. **Probe 2:** `deep-probe` mc-guard 18/0.

---

## F-15-09 — `ui-sweep` tạo payment PENDING thật rồi không dọn (F-14-C2 tái diễn) — **MEDIUM** — `FIXED`

**Bằng chứng:** ghé `/premium/checkout` (student) → tạo 2 hàng `payment_transactions` PENDING
(id 80301, 80302, `transaction_id IS NULL`, `created_at` hôm nay); parity đọc `payments=14` thay vì 12.
Script **không có bước cleanup** nào.

**Fix:** xoá 2 hàng theo **ID liệt kê**; thêm block cleanup vào `ui-sweep.js` (xoá PENDING
`transaction_id IS NULL` trong ngày + assert 0 còn lại). **Probe 2:** `payments=12`, `PENDING_TODAY=0`.

---

## F-15-10 — Doc drift sau khi gỡ Đường B — **LOW** — `FIXED`

**Bằng chứng (đo phiên này):** `README.md` ghi 21 bảng / 194 test / 523 test / 48 view;
`AGENTS.md` baseline 525 / 194; `docs/erd-sql-guide.md` mô tả `lesson_sections`/`lesson_blocks`/
`lesson_snapshots` + 21 bảng; `docs/lesson-builder-status.md` mô tả tính năng đã gỡ;
`db/migration/V1` vẫn CREATE 3 bảng đã DROP; `.specify/feature.json` trỏ v14.

**Fix:** README **18 bảng / 178 test / 512 test / 46 view**; AGENTS **512 / 178**; ERD **18 bảng, 22 FK**
(bỏ DDL/DBML 3 bảng, cập nhật row counts: users 5, study_days 5, user_progress 22, uvp 47);
xoá `lesson-builder-status.md` → thêm `lesson-builder-removal.md`; thêm `V10__drop_lesson_builder.sql`;
sửa comment `api.js`/`api.test.js`; feature.json → v15.

---

## Tổng kết findings v15

| ID | Mức | Trạng thái |
|---|---|---|
| F-15-01 rò rỉ `correctAnswer` anon | HIGH | `FIXED` (gỡ Đường B → 404) |
| F-15-02 controller trộn endpoint dùng chung | MEDIUM | `FIXED` (tách `AdminUploadController`) |
| F-15-03 `AdminExercises` phụ thuộc service Đường B | MEDIUM | `FIXED` (`uploadService`) |
| F-15-04 6/8 lesson block trùng `content` | LOW (data) | `FIXED` (xoá) |
| F-15-05 `lesson_snapshots` chưa từng dùng | LOW | `FIXED` (DROP) |
| F-15-06 `GETDATE()` UTC trong migrate | LOW | `FIXED` |
| F-15-07 harness khẳng định hành vi Đường B | MEDIUM | `FIXED` |
| F-15-08 2 assertion stale v13 | LOW (probe SAI) | `FIXED` |
| F-15-09 ui-sweep tạo payment không dọn | MEDIUM | `FIXED` |
| F-15-10 doc drift | LOW | `FIXED` |

---

## F-15-11 — `ui-sweep` so sánh URL đầy đủ với path trần → 27 guard "fail" giả — **MEDIUM (probe SAI)** — `FIXED`

**Bằng chứng:** 27 dòng `!anon` báo fail vì anon bị đẩy tới `/login?redirect=<path>` (F-13-20) nhưng
harness so `page.url()` **đầy đủ** với `/login` trần. Đây là bản sửa **đã áp ở audit-v14** (P1) nhưng
bản harness v15 khôi phục lại bản cũ.

**Fix:** so **pathname** (cắt `?`) cho cả `landed` và `want`.
**Probe 2:** chạy lại → **0 guard fail**.

---

## F-15-12 — `ui-sweep` kỳ vọng anon trên route premium → `/login` — **MEDIUM (probe SAI)** — `FIXED`

**Bằng chứng:** router kiểm `requiresPremium` **TRƯỚC** `requiresAuth` (`router/index.js:205`), nên anon
trên `/videos`, `/speaking*` → **`/premium?redirect=…`** chứ không phải `/login`. Harness kỳ vọng `/login`.
Đây là bản sửa **đã áp ở audit-v14** (P2).

**Fix:** 6 dòng `expect.anon` của route premium đổi sang `/premium`.
**Probe 2:** chạy lại → 0 guard fail (đã verify router đọc dòng 205).

---

## F-15-13 — `perf-probe` ghi artifact vào `sweep/v13` — **LOW (probe SAI)** — `FIXED`

**Bằng chứng:** `perf-probe.js:142` `const dir = path.join("sweep","v13")` → ghi `sweep/v13/perf-before.json`
dù chạy từ `sweep/v15`, và log in ra "wrote sweep/v15/…" (sai). Bản sửa **đã áp ở audit-v14** (D3).

**Fix:** `const dir = __dirname`. **Probe 2:** ghi đúng `sweep/v15/perf-before.json`.

---

## F-15-14 — `focused-probe` đo "builder contrast" trên trang chủ (nhãn sai) — **LOW (probe SAI)** — `FIXED`

**Bằng chứng:** block "2. AdminLessonBuilder contrast" ghé `/admin/445/build` — nay redirect về `/` — nên
nó đo contrast **trang chủ** dưới nhãn "builder" (`builder logoB=[]`). **Fix:** gỡ block (contrast admin
vẫn được phủ bởi `adminDashboard`/`adminSidebar`).

---

## F-15-15 — 2 probe a11y/blocks của v13 khẳng định section đã gỡ — **LOW (probe SAI)** — `FIXED`

**Bằng chứng:** `f1302-a1-blocks-live.js` khẳng định section "Tài liệu bổ sung" **render** trên 8 lesson;
`f1302-a1-a11y.js` đo contrast **trong** section đó. Cả hai đều test tính năng đã gỡ.

**Fix:** viết lại thành probe **gỡ** (mạnh hơn):
- `f1302-a1-blocks-live.js` → khẳng định section **VẮNG** trên cả 8 lesson + nội dung scrape **còn**:
  **18/18 PASS**.
- `f1302-a1-a11y.js` → đo a11y panel "Nội dung" còn lại: **5/5 PASS** (63 text node, min ratio 4.6).

---

## F-15-16 — `cls-probe` ghi artifact vào `sweep/v13` — **LOW (probe SAI)** — `FIXED`

**Bằng chứng:** `cls-probe.js:136-138` `fs.writeFileSync(path.join("sweep","v13","cls-before.json"))` nhưng
log in ra "wrote sweep/v15/cls-before.json" → file thật nằm ở `sweep/v13` (cùng lớp lỗi F-15-13).

**Fix:** ghi bằng `__dirname`. **Probe 2:** `sweep/v15/cls-before.json` tồn tại (4521 B); CLS median
`/`=0.00069, `/lessons`=0.00095, `/login`=0.00003 — đều dưới ngưỡng.

---

## F-15-17 — Harness CHUẨN (tracked, AGENTS.md trỏ tới) vẫn dùng endpoint đã gỡ — **MEDIUM** — `FIXED`

**Nguồn:** phát hiện cuối phiên khi chạy lại harness theo đúng AGENTS.md.

**Bằng chứng:**
- `sweep/v8/ui/routes.js:15` — route list còn `/admin/447/build` (đã gỡ).
- `sweep/v8/p16-parity.sql` — `COUNT(lesson_snapshots)` → `Msg 208` (bảng đã DROP). AGENTS.md §22 trỏ
  trực tiếp script này để đo parity.
- `sweep/v12/api-sweep.js:160,172,412-416` — 3 khẳng định `/structure` 200 + `/snapshots` 200/403/401.
  AGENTS.md §27 gọi đây là "API sweep chuẩn (131 endpoint)".

**Vì sao MEDIUM:** đây **không phải** harness phiên (v15) mà là **tooling được tài liệu hoá** — người
sau chạy theo AGENTS.md sẽ nhận fail/`Msg 208` và tưởng sản phẩm hỏng.

**Fix:** cập nhật cả 3 file sang hành vi mới (bỏ route; bỏ cột `snapshots`; structure → 404,
snapshots admin 404 / student 403 / anon 401). **Probe 2:** `sweep/v12/api-sweep.js` chạy lại →
**143 pass / 0 fail**; `p16-parity.sql` → `1470|43738|5|118|29|4|3|12|10`.

**Ghi chú kỷ luật:** khi chạy lại, `sweep/v12/api-sweep.js` ghi đè artifact lịch sử
`.specify/specs/audit-v12-full/evidence/api-sweep.json` (thư mục gitignored) → **đã khôi phục bản gốc**
từ `git show 2590caf^:…`.
