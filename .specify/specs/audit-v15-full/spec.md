# audit-v15-full — specification

**Ngày:** 2026-09-25 (+07) · **Nhánh:** `audit-v15-full` từ `audit-v14-full` @ `f39950b`
**Tiền nhiệm:** `audit-v14-full` · **Artifact home:** `.specify/specs/audit-v15-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue**.

## Why this exists

audit-v14 kết thúc với 4 hạng mục OPEN. Người dùng yêu cầu xử lý **3 nhóm**, giữ nguyên quy trình v14.

**Quyết định định hướng (chốt qua nhiều lần kiểm chứng):** nội dung bài học lấy **hoàn toàn** từ
`english-practice.net` (scrape → tab "Nội dung"). Khối **"Nội dung biên soạn / Tài liệu bổ sung cho bài học
này"** — hiển thị **từ đoạn đó trở xuống** — **KHÔNG** phải nội dung người dùng thêm → **gỡ toàn bộ Đường B
(Lesson Builder)**. Mục tiêu: tách hẳn **nội dung bài học** (scrape) và **bài tập** (`exercises`).

## Objective

1. **Cleanup C1** — xoá **67** tài khoản test/audit + **42** row con (backup trước, thứ tự FK, ID liệt kê).
2. **Cleanup C2** — xoá **114** `payment_transactions` (109 PENDING ∪ 5 SUCCESS của owner rác), giữ **12** SUCCESS thật.
3. **Migrate** — chuyển **3** câu hỏi (block 4/5/7) của lesson 447 → `exercises`; SUBMISSION (block 9) xoá.
4. **Gỡ Đường B** — xoá UI + API + entity/repo; DROP `lesson_blocks`, `lesson_sections`, `lesson_snapshots`.
5. **F-13-09** — verify + ghi docs timezone (KHÔNG migrate).

## Requirements

- **R1** mọi con số là đo của phiên này; kết luận cũ là giả thuyết.
- **R2** DML hàng loạt: backup + restore-drill trước; xoá theo ID liệt kê.
- **R3** 5 user thật + `admin@gmail.com` nguyên vẹn; 12 payment SUCCESS còn.
- **R4** trang bài học tab "Nội dung" chỉ còn nội dung scrape; **0** section "Nội dung biên soạn".
- **R5** 3 câu hỏi cũ làm được ở tab "Bài tập"; tab "Lịch sử" (attempt history) vẫn chạy.
- **R6** `deleteLesson` vẫn chạy sau khi DROP bảng (không nổ FK).
- **R7** kỷ luật bằng chứng; probe thứ 2 mỗi finding; review chéo mọi fix.
- **R8** dọn rác + liệt kê file đã xoá.

## Out of scope

- Migrate timezone (V2).
- GitHub issues (V3).
- Xoá `/api/resources/**`, `/api/admin/upload` — **dùng chung** với speaking/AdminExercises → chỉ tách khỏi
  controller Đường B, **không** gỡ chức năng (xem plan R10/R11).
