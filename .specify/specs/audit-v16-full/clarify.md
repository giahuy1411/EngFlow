# audit-v16-full — clarify (quyết định của người dùng)

Câu trả lời dưới đây là **ràng buộc** của phiên này.

---

## Q1 — Design system đã gần hoàn chỉnh; vòng này làm gì với giao diện?

**Bối cảnh đo được:** 0 `Outfit`/`Plus Jakarta Sans`; Be Vietnam Pro là font duy nhất; token `--geo-*` đầy đủ;
có lớp `*-ink` đạt WCAG AA; có `prefers-reduced-motion`; có component trang trí; harness `design-v2.js` đã đo.

**Trả lời:** **Chỉ verify + fix lỗi thật** — KHÔNG thiết kế lại. Chỉ sửa khi **đo được** là lệch chuẩn prompt.

---

## Q2 — Mức độ kiểm tra hiệu năng?

**Trả lời:** **Đo + fix các "win" rõ ràng**. Mọi tuyên bố tối ưu phải kèm số before/after (Hiến pháp P5).
Chỉ fix regression đo được / win rõ; không "tối ưu" khi chưa đo.

---

## Q3 — Nhánh git?

**Trả lời:** *"Không cần tạo nhánh mới, sử dụng nhánh v15 nếu đã commit hết rồi; nếu chưa thì tạo nhánh mới."*

**Đo được:** `git status` = `nothing to commit, working tree clean`; v15 đã commit (`6f046b6`).
→ **GIỮ NGUYÊN nhánh `audit-v15-full`**, không tạo nhánh mới.

---

## Q4 — Tài liệu?

**Trả lời:** *"Cập nhật lại các tài liệu kỹ thuật bao gồm file readme.md, .md, gitignore, ... Cập nhật lại
luôn file `docs/demo-engflow-4-chuc-nang.md` giúp người không hiểu gì về lập trình cũng có thể hiểu được
nội dung và truyền đạt thông qua việc demo sản phẩm dự án tốt nghiệp."*

**Bổ sung (Q5):** *"mỗi giải thích đi kèm với file/đoạn code thực tế."*

→ Phase 8: cập nhật README/AGENTS/CLAUDE/.agents-AGENTS/.gitignore/docs; **viết lại demo doc cho người
không biết lập trình**, mỗi mục kèm **đường dẫn file + đoạn code ngắn** (đã kiểm chứng tồn tại).

---

## Q6 — Chạy 2 vòng?

**Trả lời:** *"chạy lại thêm lần nữa với mức độ toàn diện hơn không quan trọng về thời gian cứ chạy test lặp
toàn bộ chức năng để tìm ra lỗi và sửa tận gốc."*

→ Phase 7 loop-until-dry: chạy lại toàn bộ probe trên build cuối; dừng khi **2 vòng liên tiếp 0 finding mới**.

---

## Q7 — Dọn file rác?

**Trả lời:** *"xoá các file/folder rác được sinh ra quá trình triển khai."*

→ Phase 8.5: cleanup manifest + thực thi; liệt kê trong REPORT.

---

## Q8 — Workflow?

**Trả lời:** *"Tuân thủ workflow này trước khi kiểm tra: constitution → specify → clarify → checklist → plan
→ tasks → implement → converge → analyze (kiểm tra giữa chừng) → taskstoissues (nếu cần)."*

→ Phase 0 chạy pipeline trước; `analyze` **giữa kỳ** (sau Phase 4) + cuối kỳ.
`taskstoissues` **bỏ qua** (V3 — không tạo GitHub issue).

---

## Q9 — Báo cáo?

**Trả lời:** *"tạo báo cáo chi tiết chỗ nào đã làm, chỗ nào chưa làm, chỗ nào đã fix và fix làm sao, các
skill đã nạp khi test."*

→ REPORT.md bắt buộc có đủ 4 mục + danh sách file đã xoá + giới hạn.
