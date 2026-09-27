# audit-v19-full — checklist (unit test cho requirements)

## R1 — Số đo phiên này
- [ ] Mọi số có file log/JSON sinh phiên này; chỗ trùng v18 ghi "xác nhận"

## R2 — Probe 2 + review chéo
- [ ] Mỗi finding có "probe thứ 2 độc lập"
- [ ] `review-v19.md` phủ mọi fix AI sinh; verdict reviewer đối chiếu lại code
- [ ] Test hồi quy mới **mutation-tested**

## R3 — Bằng chứng runtime
- [ ] Kết luận API có status + body thật; kết luận UI có ảnh/JSON MCP

## R4 — DML an toàn
- [ ] Backup + RESTORE VERIFYONLY trước DML hàng loạt; xoá theo ID

## R5 — Probe tự dọn
- [ ] Parity `1470|43738|5|118|29|4|3|12|10` sau mỗi sweep; PENDING=0

## R6 — Design chỉ fix lỗi thật
- [ ] `design-v2`: 0 badFont/legacy/token/overflow + BVP LOADED

## R7 — Perf có before/after
- [ ] W3: `sys.dm_exec_query_stats` before/after (hoặc rút lại có số)

## R8 — Dọn rác
- [ ] `cleanup-manifest.md` liệt kê file xoá + keep 2 chiều; `git status` sạch

## R9 — Docs khớp
- [ ] README/AGENTS/CLAUDE/.gitignore/docs cập nhật; demo doc verify file:line

## R10 — Prompt đã sửa lỗ hổng
- [ ] `prompt-rewritten-v19.md` tồn tại; 4 lỗ hổng đã sửa + có số đo

## R11 — Phase U 2 engine MỌI luồng
- [ ] MỌI chức năng chạy CẢ chrome-devtools MCP VÀ Playwright MCP (gồm **Premium**)
- [ ] Engine fail → BLOCKED + lỗi thật

## R12 — W1–W4 đóng nốt
- [ ] **W1:** 0 CJK trong output AI ×10; test guard + mutation-test
- [ ] **W2:** harness `smallTargets` chỉ vi phạm thật; word-chip video ≥24px; `smallList` không cắt
- [ ] **W3:** logical reads before/after (hoặc rút lại có số)
- [ ] **W4:** webhook chữ ký SePay THẬT → SUCCESS + fixture field-shape; row thật xử lý rõ

## R13 — W4 không tự xoá row tiền thật
- [ ] Row thật giữ lại (hoặc hoàn nguyên premium nếu người dùng chọn); ghi rõ

## O9 — 2 vòng / docs / rác / báo cáo
- [ ] `round-2.md`: 2 vòng liên tiếp 0 finding mới
- [ ] `docs-drift.md`, `cleanup-manifest.md`, `REPORT.md` (4 mục) tồn tại
