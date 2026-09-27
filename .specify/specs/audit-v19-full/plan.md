# audit-v19-full — kế hoạch thực thi

**Nhánh:** `audit-v15-full` @ `0dbd66c` (giữ nguyên) · **Artifact:** `.specify/specs/audit-v19-full/`
**Tiền nhiệm:** `audit-v18-full` (đã commit) · **Ngày:** 2026-09-27 (+07)

## Nguyên tắc bất di bất dịch
P1 baseline từ run log · P2 phân lớp · P3 schema Hibernate `ddl-auto=update` · P4 AI local ·
P5 đo trước/sau · P6 Playful Geometric + Be Vietnam Pro · P7 UI tiếng Việt + WCAG 2.2 AA · P8 bằng chứng runtime.

**Ràng buộc phiên:** V1 dọn rác + liệt kê · V2 phạm vi = 5 mục + defect mới · V3 không GitHub issue ·
V4 kết luận cũ là giả thuyết · V5 probe 2 mỗi finding · V6 parity phải khớp ·
**V7 Phase U (CẢ 2 engine MỌI luồng, gồm Premium)** · **V8 W1–W4 sửa tận gốc hoặc kết luận có số** ·
**V9 W4 không tự xoá row tiền thật**.

**Bẫy AGENTS.md:** flush `rate_limit:*` mỗi batch · `SET QUOTED_IDENTIFIER ON` mọi DELETE + quét `Msg \d+` ·
seed cả `token`+`user` + assert `page.url()` · `/premium/checkout` tạo row thật → dọn cùng run ·
`study_days` chỉ ghi bởi 5 đường (login KHÔNG ghi) · timezone naive-VN · backup trước DML · xoá theo ID liệt kê ·
đếm test từ run log (`mvnw -q` nuốt dòng tổng kết) · `.\mvnw.cmd`.

## Ground truth (đo phiên này)
- Docker 8 up + Ollama :11434 · DB `english_learning`
- Backend 25 controller · 121 annotation / 137 expanded / 135 distinct
- Parity `1470|43738|5|118|29|4|3|12|10`, STUDY_DAYS=4, PENDING=0, EX_ATTEMPTS=33
- Baseline backend **537/0/0/11** · frontend **194/1 (32)** · build **177.75 kB**
- Cả 2 MCP engine dùng được (v18 đã chứng minh)

## Phases

### Phase 0 — SpecKit + baseline + prompt-flaw `[gate]`
T0.1 nhánh/cây sạch · T0.2 pipeline docs · T0.3 assert-harness · **T0.4 đổi default → audit-v19-full** ·
T0.5 backend · T0.6 frontend + build · T0.7 container + parity + inventory · T0.8 kiểm lỗ hổng prompt ·
T0.9 falsify ledger · T0.10 `prompt-rewritten-v19.md`.

### Phase 1 — API sweep `[gate]`
T1.1 `api-sweep.js` · T1.2 reconciliation · T1.3 `deep-probe.js` · T1.4 `search-sort.js` · T1.5 rate-limit ·
T1.6 AI stress ×N≥10.

### Phase 2 — DB audit + secret-scan `[gate]`
T2.1 parity · T2.2 orphan FK · T2.3 FK index · T2.4 slow query · T2.5 timezone · T2.6 validate drill ·
T2.7 secret-scan · T2.8 backup trước DML.

### Phase 3 — UI/UX automated sweep `[gate]`
T3.1 `ui-sweep.js` · T3.2 `design-v2.js` · T3.3 contrast · T3.4 `routes-all.js` · T3.5 CLS ·
T3.6 danger-tint + focused + f1302-a11y · **T3.7 FIX harness smallTargets (W2)** · T3.8 tự dọn.

### Phase U — MCP thủ công MỌI chức năng, CẢ 2 ENGINE `[gate]` — TRỌNG TÂM
TU.0 xác nhận engine · TU.1–TU.12 mọi chức năng × 2 engine · **TU.13 Premium × 2 engine** ·
TU.14 dọn residue + re-assert parity.

### Phase D — Đi sâu 6 nhóm chức năng `[gate]`
TD.1 claim ledger · TD.2 verify · TD.3 coverage guard · TD.4 analyze giữa kỳ.

### Phase W — 4 WORKSTREAM đóng nốt mục "còn lại" `[gate]` — TRỌNG TÂM MỚI
**W1** AI gloss (prompt + guard CJK) · **W2** smallTargets harness + a11y word-chip video ·
**W3** perf admin projection · **W4** SePay chữ ký thật (chuyển khoản thật).

### Phase 5 — Performance
T5.1 perf before · T5.2 N+1/slow · T5.3 bundle · **T5.4 [W3] fix + đo before/after** · T5.5 Redis.

### Phase 6 — Fix + regression + review chéo `[gate]`
T6.1 fix + regression + probe 2 · T6.2 review chéo · T6.3 mutation-test · T6.4 suite xanh.

### Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
T7.1 chạy lại toàn bộ · T7.2 case biên · T7.3 dừng khi 2 vòng liên tiếp 0 finding mới.

### Phase 8 — Docs + Converge/Analyze + Report + Cleanup
T8.1 docs · T8.2 demo doc + prompt doc · T8.3 converge + analyze · T8.4 REPORT.md · T8.5 cleanup · T8.6 commit.

## Definition of Done
- API 100% probe 0 fail · DB 0 orphan parity khớp · UI 0 lỗi + smallTargets thật
- **Phase U: MỌI chức năng × CẢ 2 engine (gồm Premium)**
- **W1 0 CJK · W2 smallTargets thật · W3 before/after · W4 chữ ký thật SUCCESS**
- Design khớp prompt · 2 vòng 0 finding mới · suite xanh 0 regression · docs cập nhật · rác dọn · REPORT.md 4 mục
