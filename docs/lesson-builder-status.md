# Lesson Builder — trạng thái và giới hạn (audit-v13 F-13-02)

**Cập nhật:** 2026-09-22 (+07) · **Nguồn:** audit-v13-full, đo bằng dữ liệu thật
**Trạng thái:** A1 **ĐÃ LÀM** (TEXT/IMAGE/AUDIO/TABLE hiển thị cho học viên). A2/A3 chưa.

## Tóm tắt một câu

Khối **Text / Image / Audio / Table** admin dựng ở đây **nay đã tới học viên** (tab "Nội dung",
mục *Tài liệu bổ sung*, ngay sau nội dung gốc). Khối **Question / Submission** vẫn chưa hiển thị
và chưa chấm được — dùng **Quản lý bài tập** cho câu hỏi.

## Đo được gì (không suy đoán)

| Kiểm tra | Kết quả |
|---|---|
| Bảng lưu | `lesson_sections` (10 hàng), `lesson_blocks` (15 hàng) |
| Lesson có blocks | **8** (446, 447, 567, 11301, 41881, 91900, …) |
| Learner đọc blocks? | **CÓ** (từ A1) — qua `LessonBlocks.vue` gọi `GET /api/lessons/{id}/structure` |
| Trang học viên hiển thị gì? | `lesson.content` (HTML **scraped**) **+** blocks đã biên soạn |
| Blocks có sinh `lesson.content`? | **Không** — hai nguồn độc lập, hiển thị nối tiếp |
| Câu hỏi trong block có chấm điểm? | **Không** — chưa có endpoint grading cho block QUESTION |

### Ví dụ thật — lesson 447 ("Present simple")

Admin đã soạn trong Builder:

| block | loại | hiển thị cho học viên? |
|---|---|---|
| 3 | TABLE | **CÓ** — bảng chia động từ |
| 4 | QUESTION | Không (A2/A3) |
| 5 | QUESTION | Không (A2/A3) |
| 7 | QUESTION | Không (A2/A3) |
| 9 | SUBMISSION | Không (A2/A3) |

Kiểm live 2026-09-22: `sweep/v13/f1302-a1-blocks-live.js` → **12/12 PASS**; ảnh
`evidence/f13-02-a1-blocks-after.png`.

## ⚠️ Bẫy đã xử lý — chống hiển thị trùng nội dung

`GET /api/lessons/{id}/structure` **tự sinh một section ảo** (`id === null`) dựng từ `lesson.content`
khi bài **chưa** có section nào. Nếu render mọi thứ endpoint trả về, **~1.462 bài** sẽ hiện nội dung
**HAI LẦN**. `LessonBlocks.vue` bỏ qua section ảo (`s.id != null`), và render **rỗng** khi không có
block thật. Test khoá hành vi này: `frontend/src/views/lessons/LessonBlocks.test.js`
(2 ca "virtual section" — đã mutation-check: bỏ filter ⇒ 2 ca đỏ).

## Hai đường tạo bài tập — dùng đúng đường

| | Đường A — **Quản lý bài tập** | Đường B — **Lesson Builder** |
|---|---|---|
| Truy cập | Admin → Bài tập (`AdminExercises.vue`) | Admin → Bài học → nút "Xây dựng" |
| Lưu vào | bảng `exercises` | `lesson_sections` + `lesson_blocks` |
| **Học viên thấy?** | **CÓ** (tab "Bài tập") | **CÓ** với TEXT/IMAGE/AUDIO/TABLE (tab "Nội dung") |
| Chấm điểm? | **CÓ** (server-side) | **KHÔNG** (block QUESTION chưa chấm) |

→ **Muốn tạo câu hỏi có chấm điểm: dùng đường A.**

## Việc CHƯA làm

- **A2** — render `QUESTION` dạng tự-kiểm-tra (hiện đáp án khi bấm, không lưu điểm).
- **A3** — grading server-side + lưu submission + tích điểm/streak.

Cả hai cần endpoint mới; chưa làm ở A1.
