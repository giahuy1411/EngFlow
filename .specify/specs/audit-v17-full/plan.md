# audit-v17-full — kế hoạch thực thi

**Nhánh:** `audit-v15-full` @ `baaa61f` (giữ nguyên) · **Artifact:** `.specify/specs/audit-v17-full/`
**Tiền nhiệm:** `audit-v16-full` (đã xong) · **Ngày:** 2026-09-26 (+07)

## Nguyên tắc bất di bất dịch
P1 baseline từ run log · P2 phân lớp · P3 schema Hibernate `ddl-auto=update` · P4 AI local ·
P5 đo trước/sau · P6 Playful Geometric + Be Vietnam Pro · P7 UI tiếng Việt + WCAG 2.2 AA · P8 bằng chứng runtime.

**Ràng buộc phiên:** V1 dọn rác + liệt kê file đã xoá · V2 phạm vi = các nhóm đã chốt ·
V3 không GitHub issue · V4 kết luận cũ là giả thuyết · V5 probe thứ 2 mỗi finding · V6 parity phải khớp ·
**V7 Phase U (MCP thủ công) là trọng tâm · V8 prompt phải sửa lỗ hổng trước khi dùng làm chuẩn**.

**Bẫy AGENTS.md:** flush `rate_limit:*` mỗi batch · `SET QUOTED_IDENTIFIER ON` mọi DELETE + quét `Msg \d+` ·
seed cả `token`+`user` + assert `page.url()` · `/premium/checkout` tạo row thật → dọn cùng run ·
`study_days` chỉ ghi bởi 5 đường (login KHÔNG ghi) · timezone naive-VN · backup trước DML · xoá theo ID liệt kê ·
đếm test từ run log · `.\mvnw.cmd` (shell không tự tìm cwd).

## Ground truth (đo phiên này)
- Docker: backend :8080, frontend :5173, sqlserver :1433, redis, minio, whisper :9002, tts :8001, tailscale — 8 up
- Ollama :11434 (qwen2.5:1.5b, 3b) · DB `english_learning`
- Backend 25 controller · 121 annotation / 137 expanded / 135 distinct (đo lại)
- Frontend 83 `.vue` · 39 route · 29 test file
- Parity `1470|43738|5|118|29|4|3|12|10`, STUDY_DAYS=4, PENDING_PAYMENTS=0 (khớp v16)
- Baseline backend (đang đo) · frontend **178/1 (29)** · build **177.74 kB** (gzip 67.67)

## Phases

### Phase 0 — SpecKit + baseline `[gate]`
T0.1 nhánh + cây sạch · T0.2 pipeline docs (spec/clarify/checklist/plan/tasks) · T0.3 `assert-harness.js` PASS ·
T0.4 backend suite · T0.5 frontend suite + build · T0.6 container + parity + inventory · T0.7 falsify ledger ·
**T0.8 kiểm lỗ hổng prompt + sửa**.

### Phase 1 — API sweep (~121 endpoint) `[gate]`
T1.1 `api-sweep.js --audit audit-v17-full` · T1.2 reconciliation 100% · T1.3 contract · T1.4 error-path role ·
T1.5 rate-limit bucket · T1.6 `deep-probe.js` + `search-sort.js`.

### Phase 2 — DB audit + secret-scan `[gate]`
T2.1 parity · T2.2 orphan FK · T2.3 FK index coverage · T2.4 slow query · T2.5 timezone ·
T2.6 `ddl-auto=validate` drill · T2.7 secret-scan.

### Phase 3 — UI/UX automated sweep `[gate]`
T3.1 `ui-sweep.js` · T3.2 `design-v2.js` · T3.3 contrast · T3.4 tap-target/alt · T3.6 CLS · T3.7 responsive.

### Phase U — Interactive MCP UI/UX walkthrough của MỌI chức năng `[gate]` — TRỌNG TÂM
U1–U23: từng chức năng một, thủ công, bằng **chrome-devtools MCP + Playwright MCP**, kèm ảnh + console + network
+ đối chiếu API↔UI. Chi tiết matrix xem `plan.md` §Phase U.

### Phase D — Deep 4-function logic audit (API↔UI) `[gate]`
Claim ledger từ demo doc + source; pipeline-over-claims, 2 lens khác nhau mỗi chức năng; coverage guard tính bằng
code. CRUD + AI cũng được phủ sâu.

### Phase 5 — Performance
T5.1 perf-probe before/after · T5.2 N+1/slow query · T5.3 bundle · T5.4 fix win rõ · T5.5 Redis.

### Phase 6 — Fix + regression + review chéo
T6.1 root cause + fix tận gốc + regression test · T6.2 review chéo đối kháng (REFUTE + REGRESSION) · T6.3 suite xanh.

### Phase 7 — Vòng 2 (loop-until-dry) `[gate]`
T7.1 chạy lại toàn bộ trên build cuối · T7.2 case biên/adversarial · T7.3 dừng khi 2 vòng liên tiếp 0 finding.

### Phase 8 — Docs + Converge/Analyze + Report + Cleanup
T8.1 cập nhật docs · T8.2 sửa demo doc + regenerate prompt doc · T8.3 converge + analyze · T8.4 REPORT.md ·
T8.5 cleanup manifest + thực thi.

## Definition of Done
- API 100% có probe, 0 fail · DB 0 orphan, parity khớp · UI 0 console/guard/contrast/overflow error
- **Phase U: mọi chức năng có script bấm thật + ảnh + console/network; 2 engine cho luồng trọng yếu**
- Design khớp prompt (đã sửa lỗ hổng) · Perf before/after · 4 chức năng demo verify từng khẳng định
- 2 vòng liên tiếp 0 finding mới · Suite xanh 0 regression · Docs cập nhật + demo doc sửa · rác dọn sạch, `git status` sạch
- REPORT.md đủ: đã làm / chưa làm / đã fix & cách fix / skill đã nạp / file đã xoá / giới hạn

## Cleanup protocol
Giữ `.specify/specs/audit-v17-full/**` · xoá `sweep/v17/**`, `tmp/**`, `target/**`, `frontend/dist/**` ·
dọn probe residue · xác minh 2 chiều trước khi xoá `scripts/figma-export/node_modules`, `.playwright-mcp/*` ·
KHÔNG đụng `uploads/**`, `.env*`, spec v8–v16, backup ngoài repo.
