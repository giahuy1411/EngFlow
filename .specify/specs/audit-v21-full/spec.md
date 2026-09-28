# audit-v21-full — specification

**Ngày:** 2026-09-28 (+07) · **Nhánh:** `audit-v15-full` từ `b539d11`
**Tiền nhiệm:** `audit-v20-full` · **Artifact home:** `.specify/specs/audit-v21-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue** (`gh` chưa đăng nhập).

## Why this exists

Người dùng yêu cầu vòng quét toàn diện thứ 21: quét codebase + CSDL, chạy toàn bộ API, tương tác UI/UX
trên **2 MCP engine**, kiểm 5 vùng chính, kiểm DB trong Docker, tối ưu hiệu năng, verify design system,
**comment tiếng Việt toàn bộ code**, review code AI sinh, dọn rác.

**Phát hiện định hướng (đo phiên này):** font Be Vietnam Pro và design system Playful Geometric **đã
triển khai** (audit-v5; hiến pháp P6). Nên vòng này là **verify sâu + lấp khoảng trống**, KHÔNG làm lại từ đầu.

## Objective

1. **O1 — Comment tiếng Việt (ưu tiên #1):** bù comment cho **44 file Java + 33 file frontend** còn thiếu,
   chia **lô nhỏ**, **chạy full test sau MỖI lô** (bài học F-20-06: Javadoc từng phá build).
2. **O2 — API:** chạy toàn bộ 121 endpoint / 26 controller; 0 bỏ sót; contract + authz 2 chiều.
3. **O3 — UI/UX 2 MCP:** chrome-devtools + playwright, mọi route × 3 role × 5 viewport; deep hơn v20.
4. **O4 — DB:** parity, orphan FK, index coverage, query-plan/missing-index (chiều mới), validate drill.
5. **O5 — Hiệu năng:** bundle, Lighthouse/CWV, memory; mọi tuyên bố kèm số trước/sau (P5).
6. **O6 — Security (chiều mới):** claude-security scan + authz/rate-limit/upload-XSS/secret.
7. **O7 — Verify design system:** font, token, contrast, responsive, a11y — bằng browser thật (P8).
8. **O8 — 2 vòng lặp:** vòng 2 sâu hơn, bắt lỗi ngắt quãng (đường AI ×N≥10), sửa tận gốc.
9. **O9 — Review code AI sinh + mối liên kết**; **O10 — dọn rác** verify-trước-khi-xoá.

## Requirements

- **R1** mọi con số là đo của phiên này; kết luận cũ là giả thuyết.
- **R2** mọi kết luận có bằng chứng runtime (command output/log/screenshot/SQL/browser) — P8.
- **R3** giữ baseline xanh: backend **541/0/0/11**, frontend **194/1/32**, build **177.75 kB** (P1).
- **R4** comment: Javadoc TRƯỚC annotation; **full `mvnw test`** sau mỗi lô; kiểm "comment-only" bằng
  stripper hiểu Java text block; remap citation doc demo sau khi đổi số dòng.
- **R5** sweep có mutate phải tự dọn trong cùng run + assert parity (`STUDY_DAYS=4`, `PENDING_PAYMENTS=0`,
  `EXERCISE_ATTEMPTS=33`); payments **13** giữ nguyên (giao dịch thật).
- **R6** harness default trỏ `audit-v21-full` ở **CẢ HAI** nơi; `assert-harness.js` ALL CLEAN (8 check).
- **R7** không commit `.env`/secret/backup/PII/fixture; không seed demo data.
- **R8** chỉ tối ưu khi có đo (P5); không thêm dependency khi chưa cân nhắc size/license.
- **R9** service không chạy được ghi `BLOCKED`, không ghi `PASS`.
- **R10** 2 vòng lặp cùng kết luận mới kết luận; finding ngắt quãng cần repro + fix + regression test.

## Out of scope

- Thay font (đã xong) / dựng lại design system (đã xong).
- Tạo GitHub issue / push / PR (người dùng chọn commit tại chỗ).
- Migration timezone; xoá `content_original`; thêm index cho LIKE leading-wildcard (đã quyết không làm).
- Thêm polka/diagonal-stripe utility (OPTIONAL, không có sai lệch đo được).

## Success criteria

- 44 Java + 33 frontend file nhận comment tiếng Việt; 0 dòng code đổi; baseline vẫn xanh.
- api-sweep **145 pass/0 fail**, deep-probe **58 pass/0 fail**, search-sort đo thật.
- ui-sweep/design-v2/routes-all 0 lỗi; parity nguyên vẹn.
- 2 vòng lặp cùng kết luận; 0 finding CRITICAL tồn đọng.
- Báo cáo đủ bảng: đã kiểm tra / đã fix / chưa fix / BLOCKED / N/A / deferred / skill đã nạp.
