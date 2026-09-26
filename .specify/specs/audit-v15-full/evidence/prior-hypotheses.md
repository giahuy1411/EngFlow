# audit-v15-full — falsify-first ledger (V4: kết luận cũ là GIẢ THUYẾT)

Mỗi dòng: giả thuyết kế thừa từ v14 · cách **bác bỏ** · kết quả đo phiên này.

| # | Giả thuyết (từ v14 / kế hoạch) | Cách bác bỏ | Kết quả đo 2026-09-25 | Trạng thái |
|---|---|---|---|---|
| H1 | Parity `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` | chạy `p16-parity.sql` | **khớp** | ✅ xác nhận |
| H2 | 67 user rác (72 − 5 thật) | `SELECT user_id FROM users WHERE user_id NOT IN (2,3,70009,120010,150040)` | **đúng 67** | ✅ xác nhận |
| H3 | Row con 67 user = 42 hàng / 6 bảng | đếm qua 12 FK (gồm cả `graded_by`) | exercise_attempts 1, lesson_sub 1, payments 23, user_progress 2, uvp 4, video_attempts 11 = **42**; `graded_by` = 0 | ✅ xác nhận |
| H4 | Payments xoá = 114 (109 PENDING ∪ stale-owner) | `GROUP BY user_id,status` | PENDING 109 + SUCCESS của 60009/110010(2)/110011(3) = 5 → **114**; giữ **12** | ✅ xác nhận |
| H5 | Cột Route B là `id`/`position` | `INFORMATION_SCHEMA.COLUMNS` | **SAI** — thật là `section_id`/`block_id`/`snapshot_id`/`order_index` | ⚠️ **đã sửa** |
| H6 | 7 lesson có Đường B | `SELECT lesson_id FROM lesson_sections` | **8** (446,447,567,10889,**11300**,11301,41881,91900) | ⚠️ **đã sửa** |
| H7 | 3 block QUESTION + 1 SUBMISSION, chỉ ở 447 | dump `data` | **đúng** (block 4/5/7 QUESTION, 9 SUBMISSION; cả 3 section đều của 447) | ✅ xác nhận |
| H8 | `LessonStructureController` chỉ phục vụ Đường B | grep endpoint trong class | **SAI** — còn `/api/admin/upload`, `/api/resources/**`, `/api/admin/audio-upload` dùng chung | ⚠️ **đã sửa (F-15-02)** |
| H9 | Chỉ `AdminLessonBuilder` phụ thuộc `lessonStructureService` | grep import | **SAI** — `AdminExercises.vue:356` cũng dùng (`uploadFile`) | ⚠️ **đã sửa (F-15-03)** |
| H10 | `QuestionType` là dead code | grep `src/main` | **đúng** — 0 usage | ✅ xác nhận |
| H11 | `LessonService.deleteLesson` cascade tới sections/blocks/snapshots | đọc dòng 310–325 | **đúng** — phải sửa trước DROP | ✅ xác nhận |
| H12 | FK tới `lesson_sections` chỉ từ `lesson_blocks` | `sys.foreign_keys` | **đúng** — 1 FK | ✅ xác nhận |
| H13 | Tab "Lịch sử" = snapshot history | đọc `LessonPreview.vue` | **SAI** — là **attempt history** (`getAttempts`) → GIỮ | ⚠️ **đã sửa** |
| H14 | `/api/admin/audio-upload` còn được dùng | grep `frontend/src` | **SAI** — 0 consumer frontend (chỉ test rate-limit) | ⚠️ ghi nhận |
| H15 | 6 lesson có block trùng `lesson.content` | `LEN(data)` vs `LEN(content)` | **đúng** (446/567/10889/11301/41881 exact; 91900 gần) | ✅ xác nhận |
