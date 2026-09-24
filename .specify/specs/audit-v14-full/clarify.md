# audit-v14-full — clarify (quyết định đã chốt với người dùng)

**Ngày:** 2026-09-25 (+07)

Các câu hỏi dưới đây được hỏi **trước khi lập kế hoạch**; câu trả lời là ràng buộc của phiên này.

## Q1 — Phạm vi?

> **Người dùng:** "Lựa chọn 1 tuy nhiên cần làm tốt hơn so với các bản trước đó làm gắt gao và tỉ mỉ hơn.
> Có 1 lưu ý bạn cần phải nhớ và các file/folder rác được sinh ra khi triển khai kiểm thử cần được xoá
> bỏ sau khi hoàn thành và báo cáo chi tiết các file đã xoá luôn."

→ v14 là bản **mới hoàn toàn**: không kế thừa kết luận, chỉ kế thừa phương pháp và giả thuyết để kiểm
chứng. Gắt hơn qua 12 delta đo được (D1–D12, xem `plan.md`). **Ràng buộc mới:** dọn rác + liệt kê file đã xoá.

## Q2 — Xử lý harness sweep đã bị xoá khỏi working tree (còn trong git)?

> **Người dùng:** "Khôi phục từ git thành sweep/v14."

→ Khôi phục 16 file từ `a627569:sweep/v13/*` → `sweep/v14/*`, đổi namespace `AUDIT-V13`→`AUDIT-V14`,
trỏ output vào `audit-v14-full/evidence/`. **Cuối phiên xoá `sweep/v14/` và liệt kê từng file** (đúng Q1).

## Q3 — Chính sách sửa lỗi và vòng 2?

> **Người dùng:** "Fix tận gốc + regression test + loop-until-dry."

→ Mọi lỗi đo được: viết **test hồi quy fail trước → fix → chạy lại cùng probe**, ghi cả hai số.
Vòng 2 **loop-until-dry**: dừng khi 2 vòng liên tiếp không thêm finding mới; ghi rõ đã dừng ở vòng nào.

## Q4 — Có tạo GitHub issues không?

> **Người dùng:** "Không, giữ tasks.md."

→ **Bỏ bước taskstoissues.** `tasks.md` trong `.specify/specs/audit-v14-full/` là nguồn duy nhất.

## Q5 — Harness lifecycle khi kết thúc?

> **Người dùng:** "Xoá cuối phiên, liệt kê."

→ `sweep/v14/**` bị xoá cuối phiên; `cleanup-manifest.md` liệt kê từng file.

## Q6 — Bằng chứng của audit-v14 có commit không?

> **Người dùng:** "Commit evidence (như v13)."

→ Thêm whitelist `.gitignore` cho `.specify/specs/audit-v14-full/evidence/`; REPORT link được artifact.

## Q7 — "Fix tận gốc" có bao gồm A2/A3 Lesson Builder không?

> **Người dùng:** "Chỉ lỗi đo được."

→ F-13-02 A2/A3 và F-13-09 giữ là quyết định chủ sản phẩm; v14 chỉ **đo lại** và ghi verdict.

## Q8 — Nhánh làm việc?

> **Người dùng:** "Tạo nhánh audit-v14-full."

→ Đã tạo `audit-v14-full` từ `main` @ `e729b2c` (tree sạch).

## Mặc định đã chốt cho các câu còn lại (ghi để minh bạch)

- **Q9 GPU cho AI E2E:** thử `generate-async` thật nếu GPU rảnh; nếu không → **BLOCKED-time** ghi rõ.
- **Q10 Payment biên giới:** giữ như v13 (create-order + sai chữ ký + replay; dọn trong run).
- **Q11 `tmp/`:** dùng `tmp/v14/` ở repo root (KHÔNG ignored → phải xoá + liệt kê).
- **Q12 Lighthouse:** thử lấy category Performance; nếu host không phát → ghi giới hạn.
- **Q13 Concurrency:** UI/perf **serial** trên build đóng băng, không chạy song song với việc sửa code.
- **Q14 Ngôn ngữ report:** tiếng Việt + thuật ngữ kỹ thuật tiếng Anh (P7).
