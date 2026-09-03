# Checklist: audit-v5-full (quality of requirements)

Trước khi implement, spec phải qua các gate sau. Đánh dấu chỉ khi có bằng chứng runtime.

## A. Tính đầy đủ của phát hiện
- [x] Mỗi finding F1–F14 có root cause + bằng chứng (không chỉ triệu chứng)
- [x] Kiểm tra `.env` từng dòng (token length, URL từ trong container) — bắt được F1, F4
- [x] Kiểm tra `@PreAuthorize` có thực sự enforce — bắt được F2
- [x] Quét mọi font-family trong CSS/HTML/config — bắt được F5, F6
- [x] Đối chiếu token màu giữa design prompt và tailwind.config — bắt được F5, F7
- [x] Quét class màu off-token trong toàn bộ `frontend/src` — bắt được F10, F14
- [x] Profile dữ liệu DB theo exercise_type (đếm malformed/placeholder) — bắt được F8, F9

## B. Tính khả kiểm
- [x] DoD mỗi mục có phép đo cụ thể (test count, ms, số dòng log, computed style)
- [x] Không có yêu cầu mơ hồ kiểu "tối ưu hơn" — mọi mục perf có before/after (P5)

## C. Rủi ro & ranh giới
- [x] Quyết định không rewrite seed data được ghi thành risk có lý do
- [x] `.env` xác nhận gitignored, không vào commit
- [x] Không đổi schema DB (constitution: ddl-auto, không migration)

## D. Thiết kế / UX
- [x] Fallback UI cho data hỏng phải giữ được khả năng làm bài + chấm điểm (không chỉ ẩn lỗi)
- [x] Flashcard flat-color phải giữ tương phản WCAG (ink theo nền)
- [x] Mobile 375px screenshot trước khi đóng (risk §5)
