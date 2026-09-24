# audit-v14-full — tasks

`[x]` = đã verify bằng artifact · `[~]` = code có nhưng chưa verify · `[ ]` = chưa bắt đầu
Không mục nào chuyển `[x]` nhờ hàng xóm.

## Phase 0 — freeze, khôi phục harness, baseline

- [x] T0.1 Freeze checkpoint + tạo nhánh — `e729b2c`, branch `audit-v14-full`, tree sạch
- [x] T0.2 Tạo `.specify/specs/audit-v14-full/` + pipeline artifact (constitution/spec/clarify/checklist/plan/tasks/findings)
- [x] T0.3 Khôi phục harness `sweep/v13/*` → `sweep/v14/` (16 file, sha256 khớp `a627569`)
- [x] T0.4 Đổi namespace `AUDIT-V13`→`AUDIT-V14` + tham số hoá output (`assert-namespace.js` PASS)
- [x] T0.5 Tham số hoá ngày cleanup (`VN_RUN_DATE`)
- [x] T0.6 Backend suite — **523 / 0 / 0 / 11, BUILD SUCCESS**
- [x] T0.7 Frontend suite + build — **194 passed / 1 skipped (31 file)**; entry **177.98 kB** (gzip 67.76)
- [x] T0.8 Container inventory + staleness — 8 up, backend **không stale**
- [x] T0.9 Parity + endpoint inventory — `1470|43735|72|118|29|15|4|126|10|5`; 132→148/146/26
- [x] T0.10 Falsify-first ledger (`prior-hypotheses.md`)

## Phase 1 — audit DB trong Docker (read-only)

- [x] T1.1 Parity re-measure + giải thích delta — khớp baseline, 0 delta
- [x] T1.2 Hiệu lực constraint (scratch DB, DROP sau) — FK 547, CHECK 547, UQ 2627, filtered 2601; `SCRATCH_DB_LEFT=0`
- [x] T1.3 Orphan scan (NULL FK riêng dòng) — **0 orphan / 16 FK**, null_fk = 0
- [x] T1.4 Re-verify F-13-07 + F-13-08 — CHECK còn hiệu lực; 2 cột chết **đã xoá** (XÁC NHẬN)
- [x] T1.5 Index usage/fragmentation — không finding (usage stats non-stationary, kế thừa F-13-10)
- [x] T1.6 Timezone (F-13-09) — SQL Server UTC, 0 default GETDATE(); XÁC NHẬN đã biết
- [x] T1.7 Row-level sanity + LISTENING audio — LISTENING thiếu audio **0/358** (lệch AGENTS.md "9/367" → doc-drift)
- [x] T1.8 Quét `Msg \d+` mọi batch — 0 (batch audit sạch)

## Phase 2 — API sweep toàn bộ

- [x] T2.1 Base sweep, 148 row, kiểm hợp đồng — **143 pass / 0 fail / 1 blocked / 3 N/A** (Ollama bật)
- [x] T2.2 **Bảng reconcile 148/148** — `unaccounted=0`, 26/26 controller 100% (D1/D10 đóng)
- [x] T2.3 Deep probe 6 chức năng — **58 pass / 0 fail**
- [x] T2.4 Role matrix 2 chiều — 25/25 pass
- [x] T2.5 Search/sort đo — case-insensitive; `?sort=notacolumn`→400 (F-13-11 giữ)
- [x] T2.6 Re-assert guard F-13-01/12/13 — giữ; sửa 2 assertion sai của v13
- [x] T2.7 AI validate + BLOCKED generate-async — validate 2 chiều OK; generate* BLOCKED
- [x] T2.8 Payment biên giới — create-order 200 + dọn; sai chữ ký từ chối; hợp lệ BLOCKED
- [x] T2.9 Re-assert parity — khớp baseline
- [x] T2.10 **Sự cố F-14-01** — coverage sweep xoá dữ liệu thật → khôi phục + fix harness

## Phase 3 — UI sweep bằng cả hai MCP

- [x] T3.1 Playwright driver 39 route × 3 role — **117 cell, 0 guardFails** (sửa 3 assertion sai của v13)
- [x] T3.2 chrome-devtools MCP driver 2 — token/font/Lighthouse a11y 100 trên 3 route
- [x] T3.3 Triage tap-target — **118 đo → 0 REAL** (50 spacing + 68 inline except) — D2 đóng
- [x] T3.4 Contrast composite — **0 fail**
- [x] T3.5 Responsive 5 width — **0 overflow**
- [x] T3.6 Design-system + font — `checkBeVietnam=true`, token khớp
- [x] T3.7 A11y mở rộng — alt 0 thiếu, 0 no-name, heading order OK
- [x] T3.8 Dọn payment row + parity — **ok=true**, parity baseline

## Phase 4 — E2E 3 tầng (6 chức năng)

- [x] T4.1 Lessons/Exercises — `listHasExercise=true`, grade 100%
- [x] T4.2 F-13-01 khép kín (re-proven) — MC tạo → render options → grade 100% → DB
- [x] T4.3 Streak (đẳng thức) — `currentStreak` == computed từ `studiedDays`; 7 field
- [x] T4.4 Login/Register — hàng `users` tạo + dọn
- [x] T4.5 Search (đẳng thức id) — `resultId=10051` == DB
- [x] T4.6 CRUD — DB row 1→1→0
- [x] T4.7 AI validate + BLOCKED — q1 false, q2 true
- [x] T4.8 Premium checkout — orderCode ENG, sai chữ ký từ chối, dọn + parity
- [x] T4.9 Sửa 2 lỗi probe tự gây (streak assertion, payment cleanup) → **7/7 PASS, parity baseline**

## Phase 5 — hiệu năng (stack tĩnh)

- [x] T5.1 Perf probe (**stackStable=true**) — 35 endpoint, 1 >100ms
- [x] T5.2 CLS probe — `/` 0.00069, `/lessons` 0.00095, `/login` 0.00003
- [x] T5.3 Lighthouse — a11y 100/100/100; host không phát Performance (ghi giới hạn)
- [x] T5.4 Tối ưu chỉ khi số biện minh — **từ chối tối ưu** (chỉ 1 endpoint >100ms, LIKE đã biết)
- [x] T5.5 Đo lại cùng phương pháp — không áp dụng (không sửa gì cho perf)

## Phase 6 — fix tận gốc + test hồi quy

- [x] T6.1 Mỗi finding: fail → root cause → fix → pass — F-14-01 (harness), F-14-02 (503), F-14-03 (triage)
- [x] T6.2 Regression test fail trước → pass sau — `GlobalExceptionHandlerProblemDetailTest` 8/8
- [x] T6.3 Backend **525/0/0/11**; frontend **194/1 (31 file)**; build **177.98 kB**
- [x] T6.4 Review chéo đối kháng mọi fix — xem `review-f14.md`
- [x] T6.5 Prompt-hole analysis — `prompt-fixes.md` (H1–H11)
- [x] T6.6 Doc-drift — `doc-drift.md` (D-1..D-9)

## Phase 7 — vòng 2, loop-until-dry

- [x] T7.1 Case biên đối kháng trên build cuối — **14/14 PASS** (`adversarial.json`)
- [x] T7.2 Chạy lại mọi probe — API 143/0, deep 58/0, reconcile 148/148, parity baseline
- [x] T7.3 Dừng sau 2 vòng liên tiếp 0 lỗi mới — xem `round-2.md`
- [x] T7.4 Adversarial verify (probe thứ 2) — F-14-01/02/03 đều có probe 2 (`review-f14.md`)

## Phase 8 — converge / analyze / report / cleanup

- [ ] T8.1 converge
- [ ] T8.2 analyze (read-only)
- [ ] T8.3 Secret/PII scan
- [ ] T8.4 REPORT.md
- [ ] T8.5 Cleanup manifest + thực thi

## Phase 9 — đóng hạng mục OPEN cũ

- [x] T9.1 Đo lại F-13-02/07/08/09/10 — verdict điền vào `prior-hypotheses.md` (20/20)
- [x] T9.2 FIXED bị revert → không có; OPEN ổn → giữ (F-13-09); A1 xác nhận, A2/A3 vẫn chưa
