# audit-v18-full — clarify (quyết định của người dùng)

Câu trả lời dưới đây là **ràng buộc** của phiên này.

---

## Q1 — Nhánh git?

**Trả lời:** *"Giữ audit-v15-full"* → giữ nguyên nhánh `audit-v15-full` @ `c76cc1c`; artifact mới
`.specify/specs/audit-v18-full/`. (Đo được: `git status` = cây sạch.)

---

## Q2 — Dùng 2 MCP engine thế nào ở Phase U?

**Trả lời:** *"Cả 2 engine cho MỌI luồng"* → **chrome-devtools MCP + Playwright MCP chạy LẦN LƯỢT cho MỌI luồng**;
đối chiếu kết quả 2 engine với nhau. Không có luồng nào chỉ chạy 1 engine.

---

## Q3 — Phạm vi design (font đã là Be Vietnam Pro — chỉ cần verify)?

**Trả lời:** *"Verify + fix lỗi thật"* → chỉ sửa khi **đo được** lệch chuẩn prompt; KHÔNG thiết kế lại.

---

## Q4 — Tài liệu (nhấn mạnh lại)?

**Trả lời:** *"cập nhật lại các tài liệu kỹ thuật như readme hay các file md và gitignore. cập nhật lại tài liệu
docs/demo-engflow-4-chuc-nang.md luôn."*
→ Phase 8: cập nhật `README.md`, `AGENTS.md`, `CLAUDE.md`, `.agents/AGENTS.md`, **`.gitignore`**, mọi `.md` trong
`docs/**`; **verify + sửa `docs/demo-engflow-4-chuc-nang.md`** (từng file:line).

---

## Q5 — Xử lý khi phát hiện lỗi thật?

**Kế thừa v17:** sửa tận gốc + test hồi quy + verify live + **commit** Conventional Commits.

---

## Q6 — Chạy 2 vòng?

**Trả lời:** *"sau khi chạy và hoàn thành lần 1 thì chạy lại thêm lần nữa với mức độ toàn diện hơn không quan
trọng về thời gian cứ chạy test lặp toàn bộ chức năng để tìm ra lỗi và sửa tận gốc."*
→ Phase 7 loop-until-dry: chạy lại toàn bộ probe trên build cuối; dừng khi **2 vòng liên tiếp 0 finding mới**.

---

## Q7 — Dọn file rác?

**Trả lời:** *"xoá các file/folder rác được sinh ra quá trình triển khai."*
→ Phase 8.5: cleanup manifest + thực thi; liệt kê trong REPORT.

---

## Q8 — Workflow?

**Trả lời:** *"Tuân thủ workflow này trước khi kiểm tra: constitution → specify → clarify → checklist → plan
→ tasks → implement → converge → analyze (kiểm tra giữa chừng) → taskstoissues (nếu cần)."*
→ Phase 0 chạy pipeline trước; `analyze` **giữa kỳ** (sau Phase D) + cuối kỳ. `taskstoissues` **bỏ qua**.

---

## Q9 — Báo cáo?

**Trả lời:** *"tạo báo cáo chi tiết chỗ nào đã làm, chỗ nào chưa làm, chỗ nào đã fix và fix làm sao, các
skill đã nạp khi test."*
→ REPORT.md bắt buộc có đủ 4 mục + danh sách file đã xoá + giới hạn.

---

## Q10 — Kiểm lỗ hổng prompt?

**Trả lời:** *"Kiểm tra toàn bộ đoạn prompt này có lỗ hổng nào hay không sau đó sửa lại và triển khai."*
→ Phase 0 T0.8 đo lại 4 lỗ hổng (AAA sai, type-scale sai, "Lucide React" sai, Plus Jakarta/Outfit) + ghi
"thay font = verify" + khoảng trống literal tùy chọn; T0.10 viết `prompt-rewritten-v18.md`.

---

## Q11 — Đối chiếu, kiểm chứng thông tin?

**Trả lời:** *"luôn đối chiếu, kiểm chứng thông tin thường xuyên, kiểm tra thật không suy đoán tránh xảy ra sai
sót ... (kể cả test ui/ux hay quét codebase, csdl, tài liệu dự án)."*
→ R1: kết luận cũ là giả thuyết; mỗi finding có probe 2 độc lập; review chéo code AI sinh; đối chiếu lại code.
