# Lesson Builder (Đường B) — ĐÃ GỠ (audit-v15)

**Cập nhật:** 2026-09-25 (+07) · **Nguồn:** audit-v15-full · **Tiền nhiệm:** `lesson-builder-status.md` (audit-v13 F-13-02)

> File này **thay thế** `docs/lesson-builder-status.md` cũ (đã xoá cùng lúc) và ghi lại vì sao
> Lesson Builder bị gỡ, bằng số đo thật.

## Quyết định của chủ sản phẩm

Nội dung bài học được lấy **hoàn toàn từ `https://english-practice.net/`** (scrape → `lessons.content`).
Khối hiển thị **"Nội dung biên soạn / Tài liệu bổ sung cho bài học này"** — tức Lesson Builder (Đường B) —
**không phải** nội dung chủ sản phẩm thêm vào, nên **gỡ toàn bộ**. Hai hệ thống được tách hẳn:

| | Nội dung bài học | Bài tập |
|---|---|---|
| Nguồn | scrape `english-practice.net` | `exercises` (admin "Quản lý bài tập") |
| Lưu | `lessons.content` | `exercises` |
| Hiển thị | tab **"Nội dung"** (`LessonContent.vue`) | tab **"Bài tập"** (`LessonExerciseTab.vue`) |
| Chấm điểm | không | `ExerciseService` (server-side) |

## Vì sao gỡ (số đo 2026-09-25)

| Sự thật | Số |
|---|---|
| Lesson từng hiện khối "Nội dung biên soạn" | **8** (446, 447, 567, 10889, 11300, 11301, 41881, 91900) |
| Trong đó block TEXT **trùng byte-for-byte** với `lesson.content` | **6** (446, 567, 10889, 11301, 41881 exact; 91900 gần) |
| Lesson có nội dung tự soạn thật | **1** (447 — khớp tiêu đề "Present simple") |
| Rò rỉ đáp án qua `GET /api/lessons/{id}/structure` cho anon | **3** `correctAnswer` (`goes`, `play`, `True`) |

→ Phần lớn là **nội dung lặp vô nghĩa**, và endpoint đọc nó còn **rò rỉ đáp án** cho người ẩn danh.

## Đã gỡ gì

**Code backend (16 file):** `LessonStructureController`, `LessonSnapshotController`,
`LessonStructureService`, `LessonSnapshotService`, `LessonBlock`/`LessonSection`/`LessonSnapshot`,
`LessonBlockRepository`/`LessonSectionRepository`/`LessonSnapshotRepository`, `BlockType`, `QuestionType`,
`BlockRequest`/`SectionRequest`/`BlockResponse`/`SectionResponse`.
`LessonService.deleteLesson` bỏ cascade tới 3 bảng đó.

**Code frontend (5 file):** `AdminLessonBuilder.vue`, `LessonBlocks.vue`, `lessonStructureService.js`
(+ 2 test của chúng), route `/admin/:id/build`, nút "Xây dựng" ở `AdminLessons.vue`, breadcrumb ở
`AdminLayout.vue`, `<LessonBlocks />` trong `LessonLayout.vue`.

**DB:** `DROP TABLE lesson_blocks, lesson_sections, lesson_snapshots` (18 bảng còn lại).

**Giữ lại có chủ ý:** `GET /api/admin/upload`, `GET /api/resources/**`, `POST /api/admin/audio-upload`
**không thuộc** Đường B — chúng phục vụ `AdminExercises` và speaking → **tách sang**
`AdminUploadController` + `frontend/src/services/uploadService.js`.

**Chuyển dữ liệu:** 3 câu hỏi của lesson 447 (block 4/5/7) → `exercises` (id 787912–787914), xem
`.specify/specs/audit-v15-full/evidence/migration-verify.md`. SUBMISSION (block 9) xoá (không có
tương đương trong `exercises`).

## Hệ quả

- Trang bài học tab "Nội dung" **chỉ** còn nội dung scrape.
- Endpoint `/api/lessons/{id}/structure` và `/api/admin/lessons/{id}/structure` → **404**.
- Đường bài tập (`/exercises`) **ẩn `correctAnswer`** cho tới khi chấm → an toàn hơn đường cũ.
