# audit-v19-full — specification

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` @ `0dbd66c` (cây sạch — KHÔNG tạo nhánh mới)
**Tiền nhiệm:** `audit-v18-full` (đã commit `0dbd66c`) · **Artifact home:** `.specify/specs/audit-v19-full/`

## Why this exists

Sau `audit-v18-full`, báo cáo liệt kê **5 mục "Còn lại"**. Người dùng yêu cầu **vòng audit-v19-full đầy đủ** để
đóng nốt, đồng thời khảo sát lại phát hiện **2 kết luận v18 CHƯA ĐẦY ĐỦ/SAI**:

1. **Premium flow** chỉ 1 engine → vòng này **cả 2 engine**.
2. **SePay chữ ký THẬT**: khung "real-money" của v18 **SAI** — SePay có Test-mode simulator miễn phí; người dùng
   chọn **chuyển khoản thật**.
3. **AI gloss tiếng Trung**: **CÓ THỂ FIX** — prompt không nêu ngôn ngữ; pattern guard đã có ở `AiPromptService`.
4. **`smallTargets=115`**: triage v18 **SAI/THIẾU** — harness cắt list nên giấu **12 button word-chip** trên
   `/videos/1` (ứng viên WCAG 2.5.8 THẬT).
5. **Perf "không win"**: có candidate admin-exercise projection; người dùng chọn **theo đuổi, đo before/after**.

## Objective

| # | Mục tiêu | Cách đo |
|---|---|---|
| O1 | Mọi endpoint có probe | `api-sweep.js` + reconciliation |
| O2 | Mọi route × role × viewport sạch | `ui-sweep.js`, `routes-all.js`, `design-v2.js` |
| O2b | **MỌI chức năng × CẢ 2 MCP engine (gồm Premium)** | Phase U + `mcp-walkthrough.md` + `shots/mcp/<engine>/**` |
| O3 | DB toàn vẹn | parity + 0 orphan + validate drill |
| O4 | 6 nhóm chức năng verify API↔UI | Phase D + `demo-claims.md` |
| **O5** | **W1: 0 CJK trong output AI** | 10× generate-vocab + test guard |
| **O6** | **W2: `smallTargets` chỉ còn vi phạm thật; word-chip ≥24px** | ui-sweep before/after |
| **O7** | **W3: logical reads admin-exercise giảm (hoặc rút lại có số)** | `sys.dm_exec_query_stats` |
| **O8** | **W4: webhook chữ ký SePay THẬT → SUCCESS** | log + row `id`/`gateway` thật |
| O9 | 2 vòng; docs; rác; báo cáo | `round-2.md`, `docs-drift.md`, `cleanup-manifest.md`, `REPORT.md` |

## Requirements

- **R1** Số liệu đo phiên này; kết luận v1–v18 là giả thuyết.
- **R2** Mỗi finding có probe thứ 2; review chéo code AI sinh; đối chiếu lại code.
- **R3** Bằng chứng runtime (HTTP thật + UI thật MCP).
- **R4** DML an toàn: backup trước; xoá theo ID.
- **R5** Probe tự dọn; parity là cổng chặn.
- **R6** Design chỉ fix khi đo được lệch chuẩn.
- **R7** Perf chỉ fix win rõ (before/after) — P5.
- **R8** Dọn rác + liệt kê.
- **R9** Docs khớp; demo doc verify file:line.
- **R10** Prompt kiểm lỗ hổng + sửa.
- **R11** Phase U **CẢ 2 engine MỌI luồng** (gồm Premium).
- **R12** **W1–W4 sửa tận gốc hoặc kết luận có số** (không bỏ lửng).
- **R13** W4 **không tự xoá row tiền thật**; ghi rõ xử lý.

## Out of scope
Migrate timezone UTC · GitHub issues · SePay Test-mode (người dùng chọn chuyển khoản thật; ghi fallback).

## Success criteria
- [ ] Toàn bộ ~121 endpoint có probe; 0 fail.
- [ ] Mọi route × role × viewport: 0 lỗi.
- [ ] **Phase U: MỌI chức năng × CẢ 2 engine (gồm Premium) + ảnh + console/network.**
- [ ] **W1 0 CJK · W2 smallTargets thật · W3 before/after · W4 chữ ký thật SUCCESS.**
- [ ] Design 0 drift + BVP loaded; perf có số; 2 vòng 0 finding mới.
- [ ] Suite xanh 0 regression; docs cập nhật; rác dọn; REPORT.md đủ 4 mục.
