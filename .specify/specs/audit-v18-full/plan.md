# audit-v18-full — kế hoạch thực thi

**Nhánh:** `audit-v15-full` @ `c76cc1c` (giữ nguyên) · **Artifact:** `.specify/specs/audit-v18-full/`
**Tiền nhiệm:** `audit-v17-full` (đã xong) · **Ngày:** 2026-09-27 (+07)

## Nguyên tắc bất di bất dịch
P1 baseline từ run log · P2 phân lớp · P3 schema Hibernate `ddl-auto=update` · P4 AI local ·
P5 đo trước/sau · P6 Playful Geometric + Be Vietnam Pro · P7 UI tiếng Việt + WCAG 2.2 AA · P8 bằng chứng runtime.

**Ràng buộc phiên:** V1 dọn rác + liệt kê file đã xoá · V2 phạm vi = các nhóm đã chốt ·
V3 không GitHub issue · V4 kết luận cũ là giả thuyết · V5 probe thứ 2 mỗi finding · V6 parity phải khớp ·
**V7 Phase U (MCP thủ công, CẢ 2 engine cho MỌI luồng) là trọng tâm** · **V8 prompt phải sửa lỗ hổng trước khi dùng làm chuẩn** ·
**V9 cập nhật toàn bộ tài liệu kỹ thuật (README/.md/.gitignore) + demo doc.**

**Bẫy AGENTS.md:** flush `rate_limit:*` mỗi batch · `SET QUOTED_IDENTIFIER ON` mọi DELETE + quét `Msg \d+` ·
seed cả `token`+`user` + assert `page.url()` · `/premium/checkout` tạo row thật → dọn cùng run ·
`study_days` chỉ ghi bởi 5 đường (login KHÔNG ghi) · timezone naive-VN · backup trước DML · xoá theo ID liệt kê ·
đếm test từ run log (mvnw `-q` nuốt dòng tổng kết) · `.\mvnw.cmd`.

## Ground truth (đo phiên này)
- Docker: backend :8080, frontend :5173, sqlserver :1433, redis, minio, whisper :9002, tts :8001, tailscale — 8 up
- Ollama :11434 (qwen2.5:1.5b, 3b) · DB `english_learning`
- Backend 25 controller · 121 annotation / 137 expanded / 135 distinct (đo lại)
- Frontend 83 `.vue` · 37 route · 32 test file
- Parity `1470|43738|5|118|29|4|3|12|10`, STUDY_DAYS=4, PENDING_PAYMENTS=0, EXERCISE_ATTEMPTS=33
- Baseline backend **537/0/0/11** · frontend **194/1 (32)** · build **177.75 kB** (gzip 67.69)
- **Cả 2 MCP engine dùng được** (chrome-devtools + Playwright/Brave)

## Phases

### Phase 0 — SpecKit + baseline + prompt-flaw `[gate]`
T0.1 nhánh + cây sạch · T0.2 pipeline docs · T0.3 `assert-harness.js` · **T0.4 đổi default → audit-v18-full (2 chỗ)**
· T0.5 backend suite · T0.6 frontend suite + build · T0.7 container + parity + inventory · T0.8 kiểm lỗ hổng prompt
· T0.9 falsify ledger · T0.10 `prompt-rewritten-v18.md`.

### Phase 1 — API sweep (~121 endpoint) `[gate]`
T1.1 `api-sweep.js --audit audit-v18-full` · T1.2 reconciliation 100% · T1.3 deep-probe · T1.4 search-sort
· T1.5 rate-limit bucket · **T1.6 AI stress ×N≥10**.

### Phase 2 — DB audit + secret-scan `[gate]`
T2.1 parity · T2.2 orphan FK · T2.3 FK index coverage · T2.4 slow query · T2.5 timezone ·
T2.6 `ddl-auto=validate` drill · T2.7 secret-scan · T2.8 backup trước DML.

### Phase 3 — UI/UX automated sweep `[gate]`
T3.1 `ui-sweep.js` · T3.2 `design-v2.js` (BVP LOADED) · T3.3 contrast · T3.4 `routes-all.js` (guard 2 chiều)
· T3.5 `cls-probe.js` · T3.6 danger-tint + focused + f1302-a11y · T3.7 tự dọn + assertClean.

### Phase U — MCP thủ công MỌI chức năng, CẢ 2 ENGINE cho MỌI luồng `[gate]` — TRỌNG TÂM
TU.0 xác nhận 2 engine · TU.1–TU.12 mọi chức năng × 2 engine + đối chiếu 2 engine ↔ nhau
· TU.13 dọn residue + re-assert parity.

### Phase D — Đi sâu 6 nhóm chức năng (API↔UI) `[gate]`
TD.1 claim ledger · TD.2 verify từng claim (CONFIRMED/REFUTED) · TD.3 coverage guard code
· TD.4 `analyze` giữa kỳ.

### Phase 5 — Performance
T5.1 perf-probe before · T5.2 N+1/slow query · T5.3 bundle · T5.4 fix win rõ (hoặc "không win") · T5.5 Redis.

### Phase 6 — Fix + regression + review chéo
T6.1 root cause + fix + regression test + probe 2 · T6.2 review chéo đối kháng + đối chiếu lại code
· T6.3 mutation-test · T6.4 suite xanh.

### Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
T7.1 chạy lại toàn bộ trên build cuối · T7.2 case biên/adversarial · T7.3 dừng khi 2 vòng liên tiếp 0 finding.

### Phase 8 — Docs + Converge/Analyze + Report + Cleanup
T8.1 cập nhật README/AGENTS/CLAUDE/.agents-AGENTS/.gitignore/docs · **T8.2 verify + sửa demo doc + regenerate prompt doc**
· T8.3 converge + analyze · T8.4 REPORT.md · T8.5 cleanup manifest + thực thi · T8.6 commit.

## Definition of Done
- API 100% có probe, 0 fail · DB 0 orphan, parity khớp · UI 0 console/guard/contrast/overflow error
- **Phase U: MỌI chức năng có thao tác thật bằng CẢ 2 engine + ảnh + console/network**
- Design khớp prompt (đã sửa lỗ hổng) · Perf before/after · 6 nhóm chức năng verify từng khẳng định
- 2 vòng liên tiếp 0 finding mới · Suite xanh 0 regression · Docs cập nhật (gồm demo doc) · rác dọn sạch, `git status` sạch
- REPORT.md đủ: đã làm / chưa làm / đã fix & cách fix / skill đã nạp / file đã xoá / giới hạn

## Cleanup protocol
Giữ `.specify/specs/audit-v18-full/**` · xoá `target/**`, `frontend/dist/**`, `.mimosa/**`, `sweep/harness/_*.sql`,
`sweep/harness/*.json`, `sweep/harness/shots/**` · dọn probe residue theo ID · xác minh 2 chiều trước khi xoá
`scripts/figma-export/node_modules`, `.playwright-mcp/*` · KHÔNG đụng `uploads/**`, `.env*`, spec v8–v17, backup ngoài repo.
