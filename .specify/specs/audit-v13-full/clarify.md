# audit-v13-full — clarify (quyết định đã chốt với người dùng)

**Ngày:** 2026-09-22 (+07)

Bốn câu hỏi được hỏi trước khi lập kế hoạch; câu trả lời dưới đây là ràng buộc của phiên này.

## Q1 — Phạm vi?

> **Người dùng:** "làm lại thành 1 bản hoàn toàn mới khi so với các bản audit trước đó bản này cần toàn diện hơn đào sâu hơn và e2e"

→ v13 là bản mới: **không kế thừa kết luận**, chỉ kế thừa phương pháp và giả thuyết để kiểm chứng. Bổ sung tầng **E2E UI → API → DB** mà v12 chưa làm.

## Q2 — Xử lý các hạng mục OPEN của v12 (F147/F148/F150/C6) và các lỗi tìm thấy?

> **Người dùng:** "Fix tận gốc + test hồi quy"

→ Mọi lỗi đo được: viết **test hồi quy fail trước → fix → chạy lại cùng probe**, ghi cả hai số. Đã áp cho F-13-01 (3 vitest + 6 JUnit) và F-13-03/04/05 (3 vitest).

## Q3 — Mức độ vòng 2?

> **Người dùng:** "Lặp đến khi cạn lỗi mới"

→ **Loop-until-dry**: dừng khi **2 vòng liên tiếp không thêm finding mới**; ghi rõ đã dừng ở vòng nào.

## Q4 — Có tạo GitHub issues không?

> **Người dùng:** "Không, giữ tasks.md"

→ **Bỏ bước taskstoissues.** `tasks.md` trong `.specify/specs/audit-v13-full/` là nguồn duy nhất.

## Ràng buộc bổ sung (người dùng nêu trực tiếp trong lúc làm)

> "không được mock data linh tinh chỉ được dùng dữ liệu thực tế để chứng minh kết quả. sử dụng thêm các skill plugin liên quan"

→ **R8 (không mock)** + **R9 (review chéo)** được thêm vào spec. Mọi bằng chứng dùng hàng DB thật / HTTP thật / UI thật.

## Bổ sung phạm vi trong phiên

> "gắn thêm 1 việc cần phải fix ngay vào plan là trong lúc test chức năng tạo bài tập thì tôi chọn trắc nghiệm nhưng lúc vào làm thì lại hiển thị dạng bài điền từ vào ô trống"

→ Trở thành **F-13-01, ưu tiên cao nhất**, và đã được **repro bằng dữ liệu thật** (hàng `exercise_id=777434`) rồi fix trong phiên này.
