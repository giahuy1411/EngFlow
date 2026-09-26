# audit-v15-full — clarify (quyết định đã chốt với người dùng)

**Ngày:** 2026-09-25 (+07)

Câu trả lời dưới đây là **ràng buộc** của phiên này.

## Q1 — Xử lý hạng mục OPEN nào?
> **Người dùng:** "Cả 3 hạng mục, về quy trình thực hiện thì cứ như đoạn prompt trước đó là được."

→ Cả 3 nhóm; quy trình giữ nguyên như v14 (pipeline speckit + bằng chứng runtime + dọn rác + liệt kê +
không GitHub issue).

## Q2 — Dọn tài khoản test/audit (C1)?
> **Người dùng:** "Xoá 67 user rác + row con."

→ **Xoá** 67 user rác + row con theo thứ tự FK; giữ 5 user thật. Backup trước.

## Q3 — Lesson Builder A2/A3 tới đâu?
> **Người dùng (ban đầu):** "A2 + A3 đầy đủ."

→ Sau đó **đổi hướng hoàn toàn** (Q5/Q6): thay vì làm A2/A3, **gỡ toàn bộ Đường B**.

## Q4 — Timezone F-13-09?
> **Người dùng:** "Chỉ verify + ghi docs."

→ Verify + ghi docs; **KHÔNG migrate**.

## Q5 — Đường B dùng để làm gì?
> **Người dùng:** "kiểm tra lại đường b xem nó có tác dụng là gì / dùng để tạo bài tập hay nội dung bài học"

→ Kiểm chứng: Đường B tạo **nội dung** (section/block), không phải bài tập. Xác nhận qua code
(`LessonBlocks.vue`), ERD (`docs/erd-sql-guide.md:15` — "Cấu trúc bài học 3 tầng"), DOM live, và DB.

## Q6 — Quyết định cuối về Đường B
> **Người dùng:** "bỏ hoàn toàn tách ra hệ thống nội dung bài học và bài tập riêng biệt"
> **Người dùng:** "tôi muốn bạn loại bỏ hoàn toàn hướng b bạn đã nói"
> **Người dùng:** "nội dung bài học hiện tại tôi lấy hoàn toàn từ https://english-practice.net/ còn phần bài
> tập tôi muốn bạn loại bỏ là bài tập kèm theo ngoài nội dung tôi đã thêm trong nguồn / ví dụ là 'Nội dung
> biên soạn / Tài liệu bổ sung cho bài học này' từ đoạn này đi xuống không phải nội dung tôi đã thêm cần loại bỏ"

→ **GỠ TOÀN BỘ Đường B.** Nội dung bài học = scrape. Khối "Nội dung biên soạn / Tài liệu bổ sung" = Đường B
= gỡ.

## Ba lựa chọn cuối (AskUserQuestion)
| # | Câu hỏi | Trả lời |
|---|---|---|
| 1 | Phạm vi Đường B | **A: Gỡ toàn bộ Đường B** |
| 2 | 4 block cũ (3 QUESTION + 1 SUBMISSION) ở lesson 447 | **A: Chuyển 3 câu hỏi → `exercises`, rồi xoá** |
| 3 | `lesson_snapshots` | **A: Xoá luôn snapshot** |

## Phát hiện bổ sung trong phiên khảo sát (không nằm trong danh sách OPEN)

- **F-15-01 (HIGH)** — endpoint public `GET /api/lessons/{id}/structure` rò rỉ `correctAnswer` cho anon.
  **Tự khỏi** khi gỡ Đường B (endpoint biến mất) → verify 404.
- **`LessonStructureController` chứa endpoint dùng chung** (`/api/admin/upload`, `/api/resources/{filename}`,
  `/api/admin/audio-upload`) → phải **tách** trước khi xoá, không xoá cả class.
- **`AdminExercises.vue`** (ở lại) dùng `lessonStructureService.uploadFile` → cần service upload riêng.
