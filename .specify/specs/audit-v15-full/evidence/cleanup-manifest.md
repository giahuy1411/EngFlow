# audit-v15-full — cleanup manifest

**Ngày:** 2026-09-25 (+07) · Ràng buộc V1: xoá mọi file/folder rác sinh ra khi kiểm thử + **liệt kê chi tiết**.

## Quy tắc

| Nhóm | Đường dẫn | Xử lý |
|---|---|---|
| Artifact pipeline + evidence | `.specify/specs/audit-v15-full/**` | **GIỮ** (deliverable) |
| Harness v15 | `sweep/v15/**` | **XOÁ cuối** — gitignored, là scratch của phiên |
| Scratch SQL/txt | `tmp/v15/**` | **XOÁ** — **KHÔNG** gitignored (sẽ làm bẩn repo) |
| Build output | `target/`, `frontend/dist/` | **XOÁ** (đã gitignored) |
| Backup DB | `C:\Users\ASUS\engflow-backups\engflow_2026-09-25-prec1c2.bak` | **GIỮ ngoài repo** (chứa PII) |
| Không đụng | `uploads/**` (file thật), spec v8–v14, `.env*`, backup cũ | — |

## A. File ĐÃ XOÁ khỏi repo (code Đường B) — 24 file

**Backend `src/main/java/` (16):**
`controller/LessonStructureController.java`, `controller/LessonSnapshotController.java`,
`service/LessonStructureService.java`, `service/LessonSnapshotService.java`,
`model/entity/LessonBlock.java`, `model/entity/LessonSection.java`, `model/entity/LessonSnapshot.java`,
`model/enums/BlockType.java`, `model/enums/QuestionType.java`,
`repository/LessonBlockRepository.java`, `repository/LessonSectionRepository.java`,
`repository/LessonSnapshotRepository.java`,
`model/dto/request/BlockRequest.java`, `model/dto/request/SectionRequest.java`,
`model/dto/response/BlockResponse.java`, `model/dto/response/SectionResponse.java`

**Backend `src/test/java/` (2):**
`service/LessonStructureBlockTypeGuardTest.java`, `controller/AuditV10DraftLessonStructureGuardTest.java`

**Frontend (5):** `views/admin/AdminLessonBuilder.vue`, `views/admin/AdminLessonBuilder.f13.test.js`,
`views/lessons/LessonBlocks.vue`, `views/lessons/LessonBlocks.test.js`, `services/lessonStructureService.js`

**Docs (1):** `docs/lesson-builder-status.md`

→ **Tổng: 24 file xoá.**

## B. File TẠO MỚI (giữ trong repo) — 5 file

`src/main/java/com/datn/engflow/controller/AdminUploadController.java` (tách endpoint dùng chung),
`src/main/resources/db/migration/V10__drop_lesson_builder.sql`,
`frontend/src/services/uploadService.js`, `docs/lesson-builder-removal.md`,
`.specify/specs/audit-v15-full/**` (pipeline + evidence).

## C. File SỬA (20 file)

`.specify/feature.json`, `AGENTS.md`, `README.md`, `docs/erd-sql-guide.md`,
`frontend/src/layouts/AdminLayout.vue`, `frontend/src/router/index.js`,
`frontend/src/services/api.js`, `frontend/src/services/api.test.js`,
`frontend/src/views/admin/AdminExercises.vue`, `frontend/src/views/admin/AdminLessons.vue`,
`frontend/src/views/lessons/LessonLayout.vue`, `frontend/src/views/lessons/LessonLayout.h1.test.js`,
`frontend/src/views/lessons/LessonPreview.vue`,
`src/main/java/com/datn/engflow/service/LessonService.java`,
`src/test/java/com/datn/engflow/controller/AiVocabSaveVocabValidationTest.java`,
`src/test/java/com/datn/engflow/controller/AuditV7SecurityWaveTest.java`,
`src/test/java/com/datn/engflow/service/LessonServicePaginationTest.java`,
`sweep/v8/ui/routes.js` (harness tracked — bỏ route `/admin/447/build` đã gỡ),
`sweep/v8/p16-parity.sql` (harness tracked — bỏ cột `snapshots` của bảng đã DROP),
`sweep/v12/api-sweep.js` (harness chuẩn AGENTS.md §27 — 3 khẳng định structure/snapshots → hành vi mới)

## D. Xoá cuối phiên — **ĐÃ THỰC THI**

```bash
rm -rf tmp/                 # 33 file scratch (KHÔNG gitignored) — ĐÃ XOÁ
rm -rf sweep/v15/           # 25 file harness (gitignored) — ĐÃ XOÁ
rm -rf target/              # build output backend — ĐÃ XOÁ
rm -rf frontend/dist/       # build output frontend — ĐÃ XOÁ
rmdir  sweep/v13/           # thư mục RỖNG do probe lỗi tạo ra — ĐÃ XOÁ
rm     .specify/.../shots/v13-admin-builder-contrast.png  # ảnh chụp trang Builder đã gỡ — ĐÃ XOÁ
```

**Danh sách 58 file đã xoá lưu tại** `evidence/deleted-files.txt` (25 harness + 33 scratch).

**Kiểm chứng sau xoá:**
- `git status --porcelain` = **24 deleted + 20 modified + 5 untracked** (đúng deliverable, không còn rác).
- `ls -d tmp sweep/v15 target frontend/dist` → **ALL GONE**.
- `sweep/` còn: `backfill-export.ps1`, `v8/`, `v12/` (đúng như đầu phiên).

`tmp/v15/**` (33 file) gồm: `backup.sql`, `child-counts.sql`, `drill-check.sql`, `drill-drop.sql`,
`dup-check.sql`, `ex447.sql`, `fk-children.sql`, `fk-users.sql`, `focused-probe-orig.js`, `lesson447.sql`,
`mc-sample.sql`, `pay-cols.sql`, `pay-ids.sql`, `pay.sql`, `post-cleanup-check.sql`, `probe.txt`,
`q-data.sql`, `restore-drill.sql`, `schema.sql`, `stale-ids.sql`, `t1-dryrun.sql`, `tz-out.txt`, `tz.sql`,
`tz2.sql`, `tz3.sql`, `tz3.txt`, `ui-sweep-run.txt`, `ui-sweep-run2.txt`, `users.sql`, `users2.sql`,
`users3.sql`, `verify-blocks-data.sql`, `verify-blocks.sql`.

`tmp/` **không** nằm trong `.gitignore` → nếu để lại sẽ xuất hiện trong `git status`. Đã xoá.

## D2. Artifact của harness cũ bị ghi đè trong phiên — ĐÃ KHÔI PHỤC

`sweep/v12/api-sweep.js` (harness chuẩn AGENTS.md §27) ghi kết quả vào
`.specify/specs/audit-v12-full/evidence/api-sweep.json` (thư mục **gitignored**, artifact lịch sử).
Khi chạy để verify, file này bị ghi đè → **đã khôi phục bản gốc** từ `git show 2590caf^:…`
(19.405 bytes, `n_a=3` như cũ). Không sửa nội dung lịch sử.

## E. Kiểm tra cuối

- `git status` sạch (chỉ còn file thuộc deliverable: code + docs + `.specify/specs/audit-v15-full/`).
- Không commit `.env`, key, hay `.bak` (chứa PII).
- `uploads/**` (file thật) **không** bị đụng; file probe `7828e7bf-….txt` đã xoá riêng trong phiên.
