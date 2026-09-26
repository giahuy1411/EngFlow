# audit-v17-full — clarify (quyết định của người dùng)

Câu trả lời dưới đây là **ràng buộc** của phiên này.

---

## Q1 — Design system đã gần hoàn chỉnh; vòng này làm gì với giao diện?

**Bối cảnh đo được:** 0 `Outfit`/`Plus Jakarta Sans`; Be Vietnam Pro là font duy nhất; token `--geo-*` đầy đủ;
có lớp `*-ink` đạt WCAG AA; có `prefers-reduced-motion`; `design-v2.js` đã đo. Prompt người dùng dán vào có
**lỗ hổng đã kiểm chứng** (AAA sai, type-scale sai, "Lucide React" sai).

**Trả lời:** **Verify + fix lỗi thật** — KHÔNG thiết kế lại. Chỉ sửa khi **đo được** lệch chuẩn prompt. Prompt phải
được **kiểm lỗ hổng và sửa lại** trước khi làm chuẩn đối chiếu (R10).

---

## Q2 — Mức độ kiểm tra hiệu năng?

**Trả lời:** **Đo + fix các "win" rõ ràng**. Mọi tuyên bố tối ưu phải kèm số before/after (P5). Không "tối ưu"
khi chưa đo.

---

## Q3 — Nhánh git?

**Trả lời:** *"triển khai lựa chọn 1"* = **giữ nguyên nhánh `audit-v15-full`**, artifact mới
`.specify/specs/audit-v17-full/`. (Đo được: `git status` = cây sạch; v16 đã commit `baaa61f`.)

---

## Q4 — Xử lý khi phát hiện lỗi thật?

**Trả lời:** *"Apply fixes + commit (Recommended)"* → sửa tận gốc + test hồi quy + verify + **commit**
Conventional Commits.

---

## Q5 — Tài liệu?

**Trả lời:** *"All docs + prompt doc + demo doc (Recommended)"* → cập nhật README / AGENTS.md / CLAUDE.md /
`.agents/AGENTS.md` / `.gitignore` / `docs/**`; **regenerate prompt doc đã sửa lỗ hổng**; **verify + sửa**
`docs/demo-engflow-4-chuc-nang.md`; amend P1 counts trong `.specify/memory/constitution.md`.

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
→ Phase 0 chạy pipeline trước; `analyze` **giữa kỳ** (sau Phase D) + cuối kỳ. `taskstoissues` **bỏ qua**.

---

## Q9 — Báo cáo?

**Trả lời:** *"tạo báo cáo chi tiết chỗ nào đã làm, chỗ nào chưa làm, chỗ nào đã fix và fix làm sao, các
skill đã nạp khi test."*
→ REPORT.md bắt buộc có đủ 4 mục + danh sách file đã xoá + giới hạn.

---

## Q10 — Trọng tâm kiểm thử UI/UX (bổ sung giữa phiên)?

**Trả lời:** *"Tập trung vào kiểm thử toàn bộ chức năng thông qua ui/ux bằng cách sử dụng tool plugin và mcp hiện
có đi kèm với kiểm thử thủ công từng chức năng bằng chrome devtool mcp, playwright mcp."*
→ **Phase U là trọng tâm**: điều khiển **từng chức năng** qua UI thật, thủ công, bằng **cả** chrome-devtools MCP
và Playwright MCP; ghi ảnh + console + network; đối chiếu API↔UI.

---

## Q11 — Đi sâu 4 chức năng demo?

**Trả lời:** *"sẽ tập trung đi sâu vào toàn bộ chức năng thông qua ui/ux, review chéo toàn diện với nhau, đi sâu
vào kiểm tra logic của 4 chức năng được đề cập trong docs/demo-engflow-4-chuc-nang.md đã hoạt động ổn định hay chưa."*
→ Phase D: kiểm chứng **từng khẳng định** trong demo doc (file:line) + hành vi runtime; 2 lens độc lập mỗi chức năng.
