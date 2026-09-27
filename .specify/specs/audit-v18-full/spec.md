# audit-v18-full — specification

**Ngày:** 2026-09-27 (+07) · **Nhánh:** giữ nguyên `audit-v15-full` @ `c76cc1c` (cây sạch — đã verify; KHÔNG tạo nhánh mới)
**Tiền nhiệm:** `audit-v17-full` (đã xong, đã commit) · **Artifact home:** `.specify/specs/audit-v18-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue** (quyết định v14/V3/v17).

## Why this exists

Người dùng yêu cầu vòng kiểm tra toàn diện tiếp theo trên EngFlow (đã xong v17):

1. Quét **toàn bộ codebase + cơ sở dữ liệu** (Docker `engflow-sqlserver`).
2. Chạy **toàn bộ API** backend và **tương tác UI tương ứng** cho các chức năng chính:
   **Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI**.
3. **TRỌNG TÂM (user chốt):** kiểm thử **toàn bộ chức năng thông qua UI/UX** bằng **CẢ HAI MCP engine**
   (**chrome-devtools MCP + Playwright MCP**) cho **MỌI luồng**, thủ công, từng chức năng một.
4. **Đi sâu logic 6 nhóm chức năng** (demo doc 4 chức năng + CRUD + AI) — verify từng khẳng định API↔UI.
5. **Tối ưu hiệu năng** (chỉ khi đo được win — Hiến pháp P5).
6. **Verify giao diện đồng bộ prompt** Playful Geometric + font **Be Vietnam Pro**; **kiểm lỗ hổng prompt rồi sửa**.
7. **Cập nhật tài liệu kỹ thuật** (README, các `.md`, `.gitignore`) + **`docs/demo-engflow-4-chuc-nang.md`**.
8. **Chạy 2 vòng** — vòng 2 toàn diện hơn, không giới hạn thời gian, sửa tận gốc.
9. **Dọn file rác** sinh ra trong quá trình triển khai.
10. **Báo cáo chi tiết**: đã làm / chưa làm / đã fix & cách fix / **skill đã nạp**.

**Tuân thủ workflow:** constitution → specify → clarify → checklist → plan → tasks → implement → converge →
analyze (giữa kỳ + cuối kỳ) → taskstoissues (bỏ qua).

## Objective

| # | Mục tiêu | Cách đo |
|---|---|---|
| O1 | Mọi endpoint backend có probe (happy + error path) | `api-sweep.js` + reconciliation 100% |
| O2 | Mọi route × role × 5 viewport render không lỗi | `ui-sweep.js`, `routes-all.js`, `design-v2.js` |
| **O2b** | **MỌI chức năng được điều khiển thủ công qua UI thật bằng CẢ 2 MCP engine (mọi luồng)** | **Phase U matrix + `mcp-walkthrough.md` + `shots/mcp/<engine>/**`** |
| O3 | DB toàn vẹn: 0 orphan, parity khớp, slow query có số | `sqlrun.py p16-parity.sql` + db-audit SQL |
| O4 | **6 nhóm chức năng xác minh từng khẳng định, API↔UI** | Phase D + `demo-claims.md` |
| O5 | Design khớp prompt (đã sửa lỗ hổng) | `design-v2.js` + contrast probe |
| O6 | Hiệu năng: before/after có số; fix win rõ | `perf-probe.js`, `cls-probe.js`, bundle size |
| O7 | 2 vòng; dừng khi 2 vòng liên tiếp 0 finding mới | `round-2.md` |
| O8 | Docs cập nhật + prompt doc đã sửa + demo doc verify | `docs-drift.md` |
| O9 | Rác dọn sạch, liệt kê | `cleanup-manifest.md` |

## Requirements

- **R1** Mọi con số là **đo của phiên này**; kết luận v1–v17 là **giả thuyết để kiểm chứng**.
- **R2** Mỗi finding có **probe thứ 2 độc lập**; review chéo **mọi code AI sinh**; reviewer cũng có thể sai → **đối chiếu lại code**.
- **R3** Bằng chứng runtime: API thật (:8080) + UI thật (MCP), không dừng ở "code nhìn có vẻ đúng".
- **R4** DML hàng loạt: **backup + restore-drill trước**; xoá theo **ID liệt kê**; dry-run + ROLLBACK.
- **R5** Probe tự dọn trong cùng run (`assertClean()`); parity là cổng chặn.
- **R6** Design: **chỉ fix khi đo được lệch chuẩn** — không thiết kế lại.
- **R7** Perf: **chỉ fix win rõ**, mọi thay đổi có before/after.
- **R8** Dọn rác + **liệt kê file đã xoá**; grep tham chiếu 2 chiều trước khi xoá.
- **R9** Cập nhật docs khớp thực tế; demo doc kèm **file/đoạn code đã kiểm chứng tồn tại**.
- **R10** **Prompt Playful Geometric** phải được kiểm lỗ hổng (a11y/type-scale/lucide/font) và **sửa lại** trước khi
  dùng làm chuẩn đối chiếu.
- **R11** **Phase U** phải chạy **CẢ HAI engine** (chrome-devtools MCP + Playwright MCP) cho **MỌI luồng**;
  engine nào không dùng được → ghi **BLOCKED kèm lỗi thật**, không thay thế im lặng.
- **R12** Cập nhật lại **toàn bộ tài liệu kỹ thuật**: `README.md`, `AGENTS.md`, `CLAUDE.md`, `.agents/AGENTS.md`,
  `.gitignore`, `docs/**`, và **`docs/demo-engflow-4-chuc-nang.md`** (verify từng file:line + sửa lệch).

## Out of scope

- Migrate timezone toàn bộ sang UTC (V2 v15 — chỉ verify).
- GitHub issues (V3 — không tạo).
- Webhook SePay với chữ ký thật của SePay (biên real-money — BLOCKED như v11+; chỉ chứng minh logic HMAC bằng secret local).
- Thiết kế lại giao diện (Q3: verify + fix lỗi thật).
- Tính năng mới ngoài phạm vi trên.

## Success criteria

- [ ] Toàn bộ ~121 endpoint có probe; 0 fail (hoặc fix tận gốc).
- [ ] Mọi route × role × 5 viewport: 0 console error, 0 guard fail, 0 contrast fail, 0 overflow.
- [ ] **Phase U: MỌI chức năng có thao tác thật bằng CẢ 2 engine + ảnh + console/network; 2 engine đối chiếu nhau.**
- [ ] Design conformance: 0 badFont, 0 legacy family, 0 token mismatch; prompt đã sửa lỗ hổng; BVP LOADED.
- [ ] Perf before/after có số; win rõ đã fix (hoặc kết luận "không win").
- [ ] 6 nhóm chức năng verify API↔UI có bằng chứng từng khẳng định.
- [ ] 2 vòng liên tiếp 0 finding mới.
- [ ] Suite backend + frontend + build xanh; 0 regression.
- [ ] Docs cập nhật (gồm README/.md/.gitignore/demo doc); prompt doc regenerated.
- [ ] Rác dọn sạch; REPORT.md đầy đủ.
