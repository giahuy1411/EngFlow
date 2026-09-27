# audit-v18-full — checklist (unit test cho requirements)

Mỗi dòng là một tiêu chí kiểm chứng được cho requirement tương ứng.

## R1 — Số liệu đo trong phiên, không chép số cũ
- [ ] Mọi con số trong REPORT/evidence đều có file log/JSON tương ứng sinh trong phiên này
- [ ] Chỗ trùng với v17 ghi "xác nhận"; chỗ lệch ghi "phát hiện" (vd baseline 520→537, 192→194)

## R2 — Probe thứ 2 + review chéo
- [ ] Mỗi finding trong `findings.md` có mục "probe thứ 2 độc lập"
- [ ] `review-v18.md` tồn tại, phủ mọi fix do AI sinh
- [ ] Mọi verdict của reviewer được **đối chiếu lại code** trước khi sửa (reviewer cũng có thể sai)
- [ ] Test hồi quy mới **mutation-tested** (fail khi fix bị tắt)

## R3 — Bằng chứng runtime
- [ ] Mọi kết luận API có HTTP status + body thật
- [ ] Mọi kết luận UI có ảnh/JSON từ MCP hoặc harness headless

## R4 — DML an toàn
- [ ] Backup mới + `RESTORE VERIFYONLY` valid trước mọi DML hàng loạt
- [ ] Mọi DELETE theo ID liệt kê; dry-run + ROLLBACK trước khi commit

## R5 — Probe tự dọn
- [ ] Parity sau mỗi sweep khớp `1470|43738|5|118|29|4|3|12|10`
- [ ] `PENDING_PAYMENTS=0`, `study_days` không tăng do probe

## R6 — Design chỉ fix lỗi thật
- [ ] Mọi thay đổi design có số đo before/after chứng minh lệch chuẩn
- [ ] `design-v2.js`: 0 badFont, 0 legacy, 0 tokenMismatch, 0 overflow; **BVP LOADED**

## R7 — Perf có before/after
- [ ] `perf-before.json` + `perf-after.json` tồn tại (hoặc kết luận "không win")
- [ ] Mọi fix perf có cặp số trước/sau

## R8 — Dọn rác
- [ ] `cleanup-manifest.md` liệt kê mọi file/folder đã xoá + keep 2 chiều
- [ ] `git status` sạch sau cleanup

## R9 — Docs khớp
- [ ] README/AGENTS/CLAUDE/.agents-AGENTS/.gitignore/docs cập nhật khớp thực tế
- [ ] `docs/demo-engflow-4-chuc-nang.md` verify lại; mỗi mục có file + đoạn code đã kiểm chứng tồn tại

## R10 — Prompt đã sửa lỗ hổng
- [ ] Prompt doc mới (`prompt-rewritten-v18.md`) tồn tại
- [ ] Mọi lỗ hổng (AAA sai, type-scale sai, "Lucide React" sai, Plus Jakarta/Outfit) đã sửa + có số đo
- [ ] Không còn khẳng định sai trong prompt doc (grep "AAA", "Lucide React", "Plus Jakarta")

## R11 — Phase U hai engine cho MỌI luồng
- [ ] `mcp-walkthrough.md` có mục cho **mọi** chức năng trong matrix
- [ ] **MỌI luồng chạy CẢ chrome-devtools MCP VÀ Playwright MCP**; đối chiếu 2 engine
- [ ] Engine không dùng được ghi **BLOCKED + lỗi thật**, không thay thế im lặng

## R12 — Docs cập nhật (nhấn mạnh người dùng)
- [ ] `README.md` cập nhật (số test, inventory, tính năng)
- [ ] `AGENTS.md` cập nhật (baseline, parity, bẫy mới)
- [ ] `CLAUDE.md`, `.agents/AGENTS.md` cập nhật
- [ ] `.gitignore` cập nhật (whitelist log audit-v18)
- [ ] `docs/demo-engflow-4-chuc-nang.md` verify + sửa lệch

## O2b / O4 — Trọng tâm UI/UX + 6 nhóm chức năng
- [ ] Mỗi chức năng có ảnh `shots/mcp/<engine>/**` + console/network đối chiếu
- [ ] `demo-claims.md`: mọi khẳng định trong demo doc có verdict (CONFIRMED/REFUTED + fix)
- [ ] Coverage guard tính bằng code: `unprobed[]`/`missing[]` rỗng

## O7 — 2 vòng
- [ ] `round-2.md` ghi kết quả vòng 2
- [ ] Điều kiện dừng: 2 vòng liên tiếp 0 finding mới
