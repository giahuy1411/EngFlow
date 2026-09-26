# audit-v16-full — specification

**Ngày:** 2026-09-26 (+07) · **Nhánh:** giữ nguyên `audit-v15-full` @ `6f046b6` (cây sạch — đã verify; KHÔNG tạo nhánh mới theo yêu cầu người dùng)
**Tiền nhiệm:** `audit-v15-full` (đã xong, đã commit) · **Artifact home:** `.specify/specs/audit-v16-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue** (quyết định v14/V3).

## Why this exists

Người dùng yêu cầu một vòng kiểm tra **toàn diện** trên EngFlow (đã hoàn tất v15 — gỡ Đường B, 17 finding FIXED):

1. Quét **toàn bộ codebase + cơ sở dữ liệu** (Docker `engflow-sqlserver`).
2. Chạy **toàn bộ API** backend và **tương tác UI tương ứng** cho các chức năng chính:
   **Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI**.
3. **Tối ưu hiệu năng** cho dự án.
4. Kiểm tra frontend bằng **chrome-devtools-mcp + playwright-mcp**: UI/UX toàn bộ, responsive.
5. **Kiểm chứng giao diện đã đồng bộ với prompt Playful Geometric + font Be Vietnam Pro hay chưa**.
6. **Chạy 2 vòng** — vòng 2 toàn diện hơn, không giới hạn thời gian, sửa tận gốc.
7. **Dọn file rác** sinh ra trong quá trình triển khai.
8. **Cập nhật tài liệu kỹ thuật** (README.md, các `.md`, `.gitignore`, …), bao gồm **viết lại
   `docs/demo-engflow-4-chuc-nang.md`** cho **người không biết lập trình** hiểu và demo đồ án tốt nghiệp —
   **mỗi giải thích kèm file/đoạn code thực tế**.
9. **Báo cáo chi tiết**: đã làm / chưa làm / đã fix & cách fix / **skill đã nạp**.

## Objective

| # | Mục tiêu | Cách đo |
|---|---|---|
| O1 | Mọi endpoint backend có probe (happy + error path) | `api-sweep` + reconciliation 100% |
| O2 | Mọi view frontend render không lỗi, đúng theo role | `ui-sweep` route × role |
| O3 | DB toàn vẹn: 0 orphan, parity khớp, slow query có số | sqlcmd + `p16-parity.sql` |
| O4 | Design system khớp prompt (font/token/shadow/border/radius/motion/contrast) | `design-v2.js` + contrast probe |
| O5 | Hiệu năng: before/after có số; fix win rõ | `perf-probe.js` |
| O6 | 6 nhóm chức năng verify **API↔UI** | Phase 4 E2E |
| O7 | 2 vòng; dừng khi 2 vòng liên tiếp 0 finding mới | `round-2.md` |
| O8 | Docs cập nhật khớp; `demo-engflow-4-chuc-nang.md` viết lại kèm code | `docs-drift.md` |
| O9 | Rác dọn sạch, liệt kê | `cleanup-manifest.md` |

## Requirements

- **R1** Mọi con số là **đo của phiên này**; kết luận v1–v15 là **giả thuyết để kiểm chứng**.
- **R2** Mỗi finding có **probe thứ 2 độc lập**; review chéo **mọi code AI sinh**.
- **R3** Bằng chứng runtime: API thật (:8080) + UI thật (MCP), không dừng ở "code nhìn có vẻ đúng".
- **R4** DML hàng loạt: **backup + restore-drill trước**; xoá theo **ID liệt kê**; dry-run + ROLLBACK.
- **R5** Probe tự dọn trong cùng run (`assertClean()`); parity là cổng chặn.
- **R6** Design: **chỉ fix khi đo được lệch chuẩn** — không thiết kế lại.
- **R7** Perf: **chỉ fix win rõ**, mọi thay đổi có before/after.
- **R8** Dọn rác + **liệt kê file đã xoá**; grep tham chiếu 2 chiều trước khi xoá.
- **R9** Cập nhật docs khớp thực tế; demo doc kèm **file/đoạn code đã kiểm chứng tồn tại**.

## Out of scope

- Migrate timezone toàn bộ sang UTC (V2 v15 — chỉ verify).
- GitHub issues (V3 — không tạo).
- Webhook SePay với chữ ký thật (biên real-money — BLOCKED như v11).
- Tính năng mới ngoài phạm vi trên.

## Success criteria

- [ ] Toàn bộ ~121 endpoint có probe; 0 fail (hoặc fix tận gốc).
- [ ] Mọi route × role × 5 viewport: 0 console error, 0 guard fail, 0 contrast fail, 0 overflow.
- [ ] Design conformance: 0 badFont, 0 legacy family, 0 token mismatch.
- [ ] Perf before/after có số; win rõ đã fix.
- [ ] 6 nhóm chức năng verify API↔UI có bằng chứng.
- [ ] 2 vòng liên tiếp 0 finding mới.
- [ ] Suite backend + frontend + build xanh; 0 regression.
- [ ] Docs cập nhật; demo doc viết lại cho người không biết lập trình (kèm code thật).
- [ ] Rác dọn sạch; REPORT.md đầy đủ.
