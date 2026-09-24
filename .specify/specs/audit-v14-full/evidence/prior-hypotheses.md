# audit-v14-full — Falsify-first ledger (T0.10)

Mỗi finding của v13 được coi là **giả thuyết để kiểm chứng**, không phải nguồn. Cột "Verdict v14" điền
khi phase tương ứng chạy xong. Quy tắc (spec R1): khớp = **xác nhận**, lệch = **finding mới**.

| ID | Tuyên bố v13 | Trạng thái v13 | Cách v14 test lại | Verdict v14 |
|---|---|---|---|---|
| F-13-01 | MC chọn "Trắc nghiệm" render ô điền từ — đã fix cả render + create + AI path | FIXED | E2E T4.2 trên hàng MC thật; deep-probe guard 2 chiều; UI render | **XÁC NHẬN** — T4.2 `listHasExercise=true`, grade 100%; deep-probe 18/18 mc-guard |
| F-13-02 | Lesson Builder block QUESTION không tới học viên (A1 done, A2/A3 chưa) | FIXED (B) | Đếm `lesson_blocks`; grep renderer; đo `GET /structure` | **XÁC NHẬN** — A1: live `/lessons/447` render TABLE (tableCount=1); A2/A3 **vẫn chưa** (`LessonBlocks.vue` chỉ TEXT/IMAGE/AUDIO/TABLE) |
| F-13-03/04/05 | Residual contrast token vivid làm chữ | FIXED | Contrast composite trên route tương ứng | **XÁC NHẬN** — ui-sweep 117 cell, contrastFails=**0** |
| F-13-06 | `sweep/v13` mang nhãn v12 | FIXED | `assert-namespace.js` | **XÁC NHẬN** (guard PASS ở T0.4) |
| F-13-07 | `study_policy` thiếu CHECK singleton | FIXED | `sys.check_constraints` | **XÁC NHẬN** — `ck_study_policy_singleton` enabled+trusted (T1.4) |
| F-13-08 | dashboard admin đọc cột `last_study_date` chết | FIXED | grep entity + query | **XÁC NHẬN** — 2 cột đã **xoá** khỏi `users` (T1.4) |
| F-13-09 | SQL Server chạy UTC, 3 đồng hồ lệch | OPEN (đã biết) | Đo lại SYSDATETIME vs JVM vs naive | **XÁC NHẬN (đã biết)** — SQL Server UTC, 0 default GETDATE() (T1.6); giữ nguyên |
| F-13-10 | "3/5 index dư" — kết luận probe SAI | CLOSED | Đo lại `sys.indexes` + usage stats | **XÁC NHẬN (đóng)** — không index nào là prefix; usage stats non-stationary (T1.5) |
| F-13-11 | `?sort=` không hợp lệ → 500 thay vì 400 | FIXED | Probe `?sort=badprop` | **XÁC NHẬN** — live 400 (search-sort + adversarial) |
| F-13-12 | Guard F-13-01 chặn nhầm `"A - Salad"` | FIXED | deep-probe accept `"A - Salad"` | **XÁC NHẬN** — deep-probe 200 (sửa assertion sai của v13) |
| F-13-13 | LISTENING letter-only giữ defect | FIXED | deep-probe + UI render | **XÁC NHẬN** — guard MC-scoped; render path xử lý (sửa assertion sai của v13) |
| F-13-14 | `danger` làm chữ chỉ 4.00:1 | FIXED | Contrast composite | **XÁC NHẬN** — 0 contrast fail |
| F-13-15 | `/api/srs/due/{deckId}` N+1 | FIXED | query-stats delta | **XÁC NHẬN** — `findByUserIdAndVocabularyIdIn` (batch) còn trong code |
| F-13-16 | `/admin/:id/build` 3 chỗ contrast dưới AA | FIXED | Contrast composite route đó | **XÁC NHẬN** — 0 fail (route trong matrix) |
| F-13-17 | `sweep/v12/api-inventory.js` ghi đè evidence v12 | FIXED | `assert-namespace.js` | **XÁC NHẬN** (guard PASS) |
| F-13-18 | `/lessons/:id` không có `h1` | FIXED | UI a11y heading order | **XÁC NHẬN** — `/lessons` có 1 h1 (chrome-devtools); `LessonLayout.vue` có `<h1>` |
| F-13-19 | Leaderboard avatar hỏng không fallback | FIXED | UI: đếm ảnh hỏng | **XÁC NHẬN** — `@error` handler còn trong `Leaderboard.vue` |
| F-13-20 | `?redirect=` không ai đọc | FIXED | UI: login với `?redirect=` | **XÁC NHẬN** — router mang `?redirect=` (27 bounce đúng); `safeRedirect.js` tồn tại |
| F-13-21 | `sweep/v13/api-sweep.js` còn 2 đường dẫn v12 | FIXED | `assert-namespace.js` + grep | **XÁC NHẬN** — đã sửa `_cleanup.sql` path trong v14 |
| F-13-22 | Nút "Xem trước" builder trỏ route chết | FIXED | UI: href preview | **XÁC NHẬN** — `AdminLessonBuilder.vue` trỏ `/lessons/{id}` |

## Khoảng trống v13 mà v14 phải đóng (không phải finding cũ — là lỗi phương pháp)

| # | Khoảng trống | Cách v14 đóng |
|---|---|---|
| D1 | Inventory 148 row vs 147 status → R2 không chứng minh được | `reconcile-inventory.js` → `unaccounted=0` |
| D2 | 118 tap-target AA đo rồi bỏ khỏi REPORT | `tap-target-triage.js` → triage từng cái |
| D3 | Perf đo khi stack động (restart 2 lần) | `stackStable` assert |
