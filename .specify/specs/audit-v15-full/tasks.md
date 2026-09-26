# audit-v15-full — tasks

`[x]` = đã verify bằng artifact · `[ ]` = chưa bắt đầu

## Phase 0 — freeze, baseline, harness
- [x] T0.1 Nhánh `audit-v15-full` từ `audit-v14-full` @ `f39950b` (tree sạch)
- [x] T0.2 Artifact home + pipeline docs
- [x] T0.3 Harness `sweep/v15/` (17 file) — `assert-namespace.js` PASS (0 foreign ns, 8 writers)
- [x] T0.4 Backend baseline `Tests run: 525, Failures: 0, Errors: 0, Skipped: 11` — `evidence/baseline-backend.log`
- [x] T0.5 Frontend baseline `194 passed | 1 skipped (31 files)` — `evidence/baseline-frontend.log`
- [x] T0.6 Container 8 up; parity `1470|43735|72|118|29|15|4|126|10|5` — `evidence/parity-before.txt`
- [x] T0.7 Ground-truth re-verify (schema thật: `section_id`/`block_id`/`snapshot_id`/`order_index`)
- [ ] T0.8 Falsify-first ledger — `evidence/prior-hypotheses.md`

## Phase 1 — cleanup C1 + C2
- [ ] T1.1 Backup mới + `RESTORE VERIFYONLY`
- [ ] T1.2 Liệt kê 67 user_id (đã có: 4,5,6,7,10006,…,160040)
- [ ] T1.3 Liệt kê 114 payment id (109 PENDING ∪ 5 SUCCESS của 60009/110010/110011)
- [ ] T1.4 Dry-run + ROLLBACK (đếm)
- [ ] T1.5 Xoá thật con→cha (`QUOTED_IDENTIFIER ON` + `XACT_ABORT ON` + `BEGIN TRAN`)
- [ ] T1.6 Parity sau cleanup = `1470|43735|5|118|29|4|3|12|10|5`
- [ ] T1.7 Probe 2: 5 user thật + `admin@` login 200; leaderboard 200

## Phase 2 — migrate 3 câu hỏi → `exercises`
- [ ] T2.1 Ánh xạ: block 4→`MULTIPLE_CHOICE`(4 opts); 5→`FILL_BLANK`; 7→`MULTIPLE_CHOICE`(`["True","False"]`)
- [ ] T2.2 Insert 3 hàng `exercises` (lesson 447, order_index 6/7/8)
- [ ] T2.3 Verify: lesson 447 = 8 exercises; parity exercises 43735→43738
- [ ] T2.4 Verify UI tab "Bài tập" (không lộ đáp án trước khi trả lời)

## Phase 3 — gỡ Đường B khỏi CODE
- [ ] T3.1 Tách endpoint dùng chung → controller/service mới (upload/resources/audio)
- [ ] T3.2 `LessonService`: bỏ cascade sections/blocks/snapshots + bỏ 3 field repo
- [ ] T3.3 Xoá `LessonStructureController`/`Service`, `LessonBlock`/`Section`/`Snapshot` + repos + `BlockType`
- [ ] T3.4 Xoá `AdminLessonBuilder.vue`, `LessonBlocks.vue`, `lessonStructureService.js`; route `:id/build`;
      nút "Xây dựng" ở `AdminLessons.vue`; breadcrumb `AdminLayout.vue`
- [ ] T3.5 `LessonLayout.vue`: bỏ `<LessonBlocks />`
- [ ] T3.6 Sửa chuỗi stale `LessonPreview.vue` ("tab Nội dung" → "tab Bài tập")
- [ ] T3.7 `AdminExercises.vue`: đổi sang `uploadService`
- [ ] T3.8 Cập nhật/xoá test Đường B
- [ ] T3.9 DROP `lesson_blocks` → `lesson_sections` → `lesson_snapshots`
- [ ] T3.10 Rebuild backend container + verify boot

## Phase 4 — verify trang bài học sau gỡ
- [ ] T4.1 `/lessons/447` tab "Nội dung" = 0 "Nội dung biên soạn"
- [ ] T4.2 Tab "Bài tập" = 8 câu
- [ ] T4.3 Tab "Lịch sử" 200
- [ ] T4.4 Admin: không nút "Xây dựng"; `/admin/447/build` → catch-all
- [ ] T4.5 anon `GET /api/lessons/447/structure` → 404 (F-15-01 tự khỏi)

## Phase 5 — F-13-09 timezone verify + docs
- [ ] T5.1 Đo 3 đồng hồ + đếm writer
- [ ] T5.2 Kiểm date-granular miễn nhiễm
- [ ] T5.3 Ghi docs

## Phase 6 — regression + review chéo
- [ ] T6.1 Backend suite
- [ ] T6.2 Frontend suite + build
- [ ] T6.3 Review chéo

## Phase 7 — vòng 2
- [ ] T7.1–T7.3

## Phase 8 — converge / analyze / report / cleanup
- [ ] T8.1–T8.5

---

## Cập nhật cuối phiên (2026-09-25)

- [x] Phase 1 cleanup C1+C2 — parity `1470|43735|5|118|29|4|3|12|10|5`
- [x] Phase 2 migrate 3 câu hỏi — id 787912–787914, grade 3/3 & 0/3
- [x] Phase 3 gỡ Đường B — 16 file backend + 5 file frontend + DROP 3 bảng
- [x] Phase 4 verify sau gỡ — 18/18 removal-live, 5/5 a11y, deleteLesson 204→404
- [x] Phase 5 F-13-09 — verify + docs (giữ nguyên); F-15-06 sửa timestamp migrate
- [x] Phase 6 regression — backend 512/0/0/11, frontend 178/1 (29 file), build OK
- [x] Phase 7 vòng 2 — api-sweep 143/0, deep-probe 58/0, ui-sweep 0 guard fail, 17 finding (10 thật + 7 probe)
- [x] Phase 8 converge / analyze / review chéo / REPORT / cleanup-manifest
- [x] **Cleanup thực thi** — `rm -rf tmp/ sweep/v15/ target/ frontend/dist/` + `sweep/v13` (rỗng) + ảnh builder; danh sách 58 file ở `evidence/deleted-files.txt`

---

## audit-v15-hardening (2026-09-25) — bịt 3 lớp điểm yếu harness

### H0 — Baseline & mốc study_days
- [x] H0.1 Baseline: backend 515/0, frontend 178/1, parity `1470|43738|5|118|29|4|3|12|10`
- [x] H0.2 Truy vết residue: `study_days` id=30069 (user 2, 2026-09-25) — v14 kết thúc ở 4
- [x] H0.3 Xoá theo ID liệt kê → `study_days` = **4**

### H1 — Harness được commit (L2)
- [x] H1.1 15 file vào `sweep/harness/` từ `a627569:sweep/v13` (trừ api-sweep)
- [x] H1.2 Áp lại fix v14 + v15 (D8 ngày động, F-13-12/13, F-15-13/14/15/16)
- [x] H1.3 `_config.js` tham số hoá `--audit/--out` (verify: `--audit audit-v16-full` → MARKER AUDIT-V16)
- [x] H1.4 `.gitignore`: `sweep/*` + `!sweep/harness/`; 14 file source tracked, output ignored
- [x] H1.5 `harness-restore.md` không rỗng (3.473 ký tự) + guard

### H2 — Drift guard (L3)
- [x] H2.1 `HarnessDriftTest` 3 test — PASS cây sạch, FAIL khi chèn drift (2 chiều)
- [x] H2.2 `assert-harness.js` 4 check — ALL CLEAN, FAIL khi chèn route/baseline drift
- [x] H2.3 Whitelist assertion "đã gỡ → 404" cố ý

### H3 — Ép dọn residue (L1)
- [x] H3.1 `study_days` + `PENDING_PAYMENTS` vào `p16-parity.sql`
- [x] H3.2 `assertClean()` trong `sweep/v8/ui/lib.js` (throw khi residue; 2 chiều đã chứng minh)
- [x] H3.3 `v12/api-sweep.js` dọn `study_days` + ngày động → `AUDIT_STUDY_DAYS=0`
- [x] H3.4 `routes-all` cleanup vào `finally`; `design-v2` gate exit; `design` thêm cleanup
- [x] H3.5 Static check harness-ghi-phải-có-cleanup (một phần của assert-harness)

### H4 — 8 lỗi L3 + docs
- [x] H4.1 baseline 1 nguồn (`PARITY_BASELINE`/`PAYMENTS_BASELINE` trong lib.js)
- [x] H4.2 `routes-all` sinh route từ router (`routes-from-router.js`), bỏ file untracked
- [x] H4.3 `AGENTS.md` inventory + baseline; `CLAUDE.md` bỏ `BlockType`
- [x] H4.4 `v12/api-sweep.js` `--out`, không ghi đè `audit-v12-full` (verify hash không đổi)

### H5 — Verify + regression
- [x] H5.1 Backend **515/0/0/11**
- [x] H5.2 Frontend **178/1**
- [x] H5.3 Chạy lại mọi harness tracked — 0 residue, parity khớp
- [x] H5.4 Guard 2 chiều (endpoint/route/baseline)
- [x] H5.5 `assertClean` 2 chiều

### H6 — Report + cleanup
- [x] H6.1 `REPORT-HARDENING.md` + `hardening-summary.md`
- [ ] H6.2 Cleanup manifest + xoá `tmp/`
