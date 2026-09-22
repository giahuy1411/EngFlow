# Lesson Builder — trạng thái và giới hạn (audit-v13 F-13-02)

**Cập nhật:** 2026-09-22 (+07) · **Nguồn:** audit-v13-full, đo bằng dữ liệu thật

## Tóm tắt một câu

**Lesson Builder là công cụ soạn thảo chưa nối vào trang học.** Nội dung admin dựng ở đây
được lưu vào DB nhưng **học viên không thấy**.

## Đo được gì (không suy đoán)

| Kiểm tra | Kết quả |
|---|---|
| Bảng lưu | `lesson_sections` (10 hàng), `lesson_blocks` (15 hàng) |
| Lesson có blocks | **7**, trong đó **6 đã publish** (446, 447, 567, 11301, 41881, 91900) |
| Learner đọc blocks? | **Không** — `getStructure()` (public API) có **0 caller** trong frontend |
| Trang học viên hiển thị gì? | `lesson.content` — HTML **scraped** từ english-practice.net |
| Blocks có sinh `lesson.content`? | **Không** — không có code nào chuyển blocks thành content |
| Câu hỏi trong block có chấm điểm? | **Không** — không có endpoint grading cho block QUESTION |

### Ví dụ thật — lesson 447 ("Present simple")

Admin đã soạn trong Builder:

| block | loại | nội dung |
|---|---|---|
| 3 | TABLE | Bảng chia động từ (`I work`, `He/She/It works`…) |
| 4 | QUESTION | `She ___ to school every day.` (trắc nghiệm, đáp án `goes`) |
| 5 | QUESTION | `They ___ (play) football on Sundays.` (điền từ, đáp án `play`) |
| 7 | QUESTION | `The writer wakes up at 6 AM.` (đúng/sai, đáp án `True`) |
| 9 | SUBMISSION | `Nhap bai viet cua ban o day:` |

Đo DOM trang học viên `/lessons/447`: **cả 4 nội dung trên đều không xuất hiện**.

## Hai đường tạo bài tập — dùng đúng đường

| | Đường A — **Quản lý bài tập** | Đường B — **Lesson Builder** |
|---|---|---|
| Truy cập | Admin → Bài tập (`AdminExercises.vue`) | Admin → Bài học → nút "Xây dựng" |
| Lưu vào | bảng `exercises` | `lesson_sections` + `lesson_blocks` |
| **Học viên thấy?** | **CÓ** (tab "Bài tập" trong bài học) | **KHÔNG** |
| Chấm điểm? | **CÓ** (server-side) | **KHÔNG** |

→ **Muốn tạo câu hỏi cho học viên làm: dùng đường A.**

## Thay đổi từ audit-v13

1. **Nút "Xem trước"** trong Builder từng trỏ `/lessons/{id}/preview` — route **không tồn tại**
   → catch-all đá admin về **trang chủ**. Đã sửa thành `/lessons/{id}` (route thật).
2. **Banner cảnh báo** thêm vào đầu trang Builder, nói rõ giới hạn.
3. **Không tạo mới** được khối `QUESTION` / `SUBMISSION` nữa (vẫn hiển thị nếu đã có sẵn,
   để không làm hỏng nội dung cũ).

## Việc CHƯA làm (cần quyết định sản phẩm)

Để nối Builder vào trang học cần:

- **A1** — render `TEXT/IMAGE/AUDIO/TABLE` cho học viên (không cần grading). *Rẻ.*
- **A2** — render `QUESTION` 3 loại dạng tự-kiểm-tra (hiện đáp án khi bấm, không lưu điểm). *Vừa.*
- **A3** — grading server-side + lưu submission + tích hợp điểm/streak. *Lớn.*

Hoặc **bỏ Builder** (cần xử lý 7 lesson có dữ liệu). Hoặc **giữ nguyên** như hiện tại
(đã có banner cảnh báo nên không còn gây hiểu nhầm).
