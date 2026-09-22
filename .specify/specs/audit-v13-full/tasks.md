# audit-v13-full — tasks

`[x]` = đã verify bằng artifact · `[~]` = code có nhưng chưa verify · `[ ]` = chưa bắt đầu
Không mục nào chuyển `[x]` nhờ hàng xóm.

## Phase 0 — freeze & baseline

- [x] T0.1 Git checkpoint — `3c1c525`, branch `audit-streak-review`
- [x] T0.2 Tạo `.specify/specs/audit-v13-full/` + pipeline artifact
- [x] T0.3 Backend suite **499 / 0 fail / 0 error / 11 skipped, BUILD SUCCESS** (đếm từ run log)
- [x] T0.4 Frontend suite **127 passed / 1 skipped (25 file)**; build entry **177.44 kB** (gzip 67.56)
- [x] T0.5 Container inventory 8 up; backend **không stale**
- [x] T0.6 Parity live **`1470|43735|72|118|29|15|4|126|10|5`**
- [x] T0.7 Endpoint inventory — **132 annotation → 148 row / 146 distinct / 26 controller**
- [x] T0.8 Đồng bộ nhãn `sweep/v13/` — `AUDIT-V12` = **0** (F-13-06)

## Phase 1 — audit DB trong Docker (read-only)

- [x] T1.1 Hiệu lực constraint — scratch DB, chứng minh FK/UQ/filtered index **thật sự chặn**
- [x] T1.2 Orphan scan (NULL FK đếm riêng)
- [x] T1.3 Streak schema: policy, days, cutover → **phát hiện F-13-07 (HIGH)**
- [x] T1.4 Row-level sanity bảng UI đọc
- [x] T1.5 Index usage / fragmentation hot read path → F-13-10 (deferred, có số)
- [x] T1.6 Quét `Msg \d+`; timezone naive-VN → F-13-09
- [x] Artifact: `evidence/db-audit.md`

## Phase 2 — API sweep toàn bộ endpoint

- [x] T2.1–T2.11 — **143 pass / 0 fail / 1 blocked / 3 N/A**, 0 findings mới
- [x] Guard F-13-01 kiểm **cả hai chiều** trên container thật: payload lỗi → 400, hợp lệ → 200
- [x] Search/sort đo riêng (`evidence/search-sort.json`)
- [x] Re-assert parity sau mỗi lần ghi probe

## Phase 3 — UI sweep CẢ HAI MCP

- [~] T3.1 chrome-devtools MCP (Lighthouse đã chạy: `lh-home`, `lh-lessons`, `lh-login`)
- [~] T3.2 Playwright MCP (5 ảnh `shots/v13-*` tại 360/768/1440/1920)
- [~] T3.3–T3.9 (workflow đang hoàn tất)

## Phase 4 — E2E 3 tầng UI → API → DB

- [x] **T4.2b E2E khép kín F-13-01** — tạo MC thật → UI render lựa chọn → grade/submit 200 → `exercise_attempts` row + `study_days` (`evidence/e2e-flows.md`)
- [x] **T4.2c Chống regression khối lượng lớn** — lesson 567: 12 hàng MC options NULL **vẫn có ô nhập text trả lời được**
- [x] T4.1, T4.3, T4.4, T4.5, T4.6, T4.7 — đều PASS (`e2e-flows.md`)
- [~] T4.8 Premium checkout — chỉ chạm create-order + webhook sai chữ ký (biên giới tiền thật); row probe đã dọn

## Phase 5 — hiệu năng

- [~] T5.1–T5.3 (workflow đang chạy perf-probe + Lighthouse + CLS)
- [x] T5.4 Tối ưu **chỉ khi** số biện minh — F-13-15 (N+1) đã sửa; 5 quan sát khác **từ chối** kèm lý do (P5)

## Phase 6 — sửa lỗi tận gốc + test hồi quy

- [x] **T6.0 F-13-01** — repro dữ liệu thật → fix render + create FE/BE + AI path → verify live
- [x] **T6.0b Review chéo đối kháng** — tìm ra 6 vấn đề (gồm regression 32 814 hàng, guard bypassable qua AI path) → **đã sửa cả 6**
- [x] T6.1 Test hồi quy — vitest **8** + JUnit **11** + contrast **3**
- [x] T6.2 F-13-02 điều tra bằng dữ liệu thật (3 QUESTION block; không renderer học viên) + đính chính giả thuyết SAI
- [x] T6.3 F-13-03/04/05/14/16 contrast — fix 3+36+4 chỗ; **giữ nguyên** `AdminLayout` (đo 8.76:1, đúng)
- [x] T6.3b F-13-15 N+1 `srs/due` — batch query, chứng minh bằng query-stats
- [x] T6.4 Sửa prompt — `evidence/prompt-fixes.md` (H1 AAA sai + quy tắc `*-ink`/`*-strong`)
- [x] T6.5 Đóng/gia hạn F147/F148/F150/C6 bằng đo mới — F147/F148/F153/C6 **xác nhận FIXED**, F150 CLS 0.00069 (`t6.5-v12-open-items.md`)
- [x] T6.6 Re-run suite — backend **513 / 0 / 0 / 11**; frontend **144 passed / 1 skipped (27 file)**

## Phase 7 — vòng 2 (loop-until-dry)

- [x] T7.1–T7.4 — **2 vòng liên tiếp 0 lỗi mới** (`round-2.md`); adversarial verify: bác 1 artifact UI, xác nhận F-13-07/14/15

## Phase 8 — converge / analyze / báo cáo

- [x] T8.1 converge (`converge.md`) · [x] T8.2 analyze (`analyze.md` — tự bắt 4 lỗi đếm số) · [x] T8.3 checklist · [x] T8.4 REPORT.md


## Phase 9 — xử lý 5 hạng mục OPEN (quyết định của chủ sản phẩm)

- [x] T9.1 Đo lại độc lập cả 5 hạng mục → 2 kết luận probe SAI (F-13-08 một phần, F-13-10 hoàn toàn), 1 nặng hơn báo cáo (F-13-02 + bẫy "Xem trước"), 1 khuyến nghị của tôi SAI (`@Check` deprecated)
- [x] T9.2 **F-13-02/22** (phương án B) — sửa nút Xem trước `/lessons/{id}`, banner cảnh báo, vô hiệu hoá tạo QUESTION/SUBMISSION, `docs/lesson-builder-status.md`; test 2; verify live
- [x] T9.3 **F-13-07** — `@CheckConstraint` (JPA 3.2, không dùng `@Check` deprecated) + guard riêng trong deploy.sql + `fix-study-policy-check.sql`; verify `INSERT id=2` bị chặn Msg 547; test 1
- [x] T9.4 **F-13-08** — `recentUsers` chuyển sang `study_days` (query + service + facade); đánh dấu `countByLastStudyDateAfter` deprecated; test 2; verify live khớp DB
- [x] T9.5 **F-13-20** — `safeRedirect.js` + guard mang redirect + Login/Register đọc + Premium forward; test 24 (13 ca tấn công); verify live 2 chiều
- [x] T9.6 **F-13-10** đóng (kết luận probe SAI); **F-13-09** giữ nguyên
- [x] T9.7 Dọn bảng scratch `hb_check_probe`
- [x] T9.8 Full suites — backend **523 / 0 / 0 / 11**, frontend **175 / 1 skipped (30 file)**; build 177.73 kB; parity khớp
- [x] T9.9 Review chéo — agent adversarial hoàn tất, **7 finding đều đúng và đã sửa**: P1 untracked files, P2 premium mất đích, P2 test mock sai method, P3 safeRedirect decode, P3 backend block guard, P3 deploy.sql guard, P3 guestOnly; `evidence/review-open-items.md`
