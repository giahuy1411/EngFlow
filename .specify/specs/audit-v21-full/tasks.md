# audit-v21-full — tasks

## Phase 0 — SpecKit + baseline `[gate]` ✅
- [x] T0.1 Nhánh `audit-v15-full` + cây sạch → `evidence/baseline.md`
- [x] T0.2 Runtime 8 container up
- [x] T0.3 Backend **541/0/0/11 BUILD SUCCESS** ✅
- [x] T0.4 Frontend **194/1 (32)** + build **177.75 kB** ✅
- [x] T0.5 Parity `1470|43738|5|118|29|4|3|13|10` + markers ✅
- [x] T0.6 Default v21 (2 nơi) + `harness-restore.md` → assert-harness **ALL CLEAN 8/8** ✅
- [x] T0.7 Prompt flaws đo lại → `evidence/prompt-flaws.md` ✅
- [x] T0.8 spec/clarify/checklist/plan/tasks ✅
- [x] T0.9 `prompt-rewritten-v21.md`

## Phase 1 — Comment tiếng Việt (ƯU TIÊN #1, lô nhỏ) `[gate]`
> Mỗi lô: Javadoc TRƯỚC annotation → **full `mvnw test`** (BE) / `vitest` (FE) → kiểm comment-only → commit.

### Backend (44 file thiếu)
- [x] T1.J1 `model/enums` (7) + `model/dto/projection` (3) + `model/dto/video` (1)
- [x] T1.J2 `repository` (10 file thiếu)
- [x] T1.J3 `config` (7 file thiếu)
- [x] T1.J4 `security` (3) + `controller` (5 thiếu)
- [x] T1.J5 `service` phần infra/AI (9 thiếu: AnswerKey, Cloudinary, DuckDuckGo, schedulers, Tts, assessment records)

### Frontend (33 file thiếu)
- [x] T1.F1 `components/ui` (12) + `components/decor` (2) + `layout` (1) + `common` (1) + `admin` (1)
- [x] T1.F2 `services` (7) + `store` (2) + `composables` (1)
- [x] T1.F3 `utils` (5)
- [x] T1.F4 CSS: giải thích khối token trong 4 file (chỉ comment, không đổi giá trị)

- [x] T1.V Kiểm chứng "comment-only" (stripper hiểu text block) + remap citation doc demo

## Phase 2 — API sweep `[gate]`
- [x] T2.1 `api-sweep.js --audit audit-v21-full` → 145 pass / 0 fail
- [x] T2.2 Reconciliation vs `api-inventory.js` → 0 bỏ sót
- [x] T2.3 `deep-probe.js` → 58 pass / 0 fail
- [x] T2.4 `search-sort.js` → đo `?sort=` thật
- [x] T2.5 Rate-limit bucket 0 tự-gây-429
- [x] T2.6 AI stress ×N≥10 → `evidence/ai-stress.md`

## Phase 3 — DB + hiệu năng `[gate]`
- [x] T3.1 Orphan FK + FK index coverage → `evidence/db-integrity.md`
- [x] T3.2 Query-plan / missing-index DMV (chiều mới) → `evidence/db-perf.md`
- [x] T3.3 Slow query đo thật (admin exercises/lessons)
- [x] T3.4 Timezone re-verify
- [x] T3.5 `ddl-auto=validate` drill
- [x] T3.6 Secret-scan → `evidence/secret-scan.md`

## Phase 4 — UI/UX automated sweep `[gate]`
- [x] T4.1 `ui-sweep.js` → 0 fail
- [x] T4.2 `design-v2.js` → 0 drift, font BVP, lucide 2.5
- [x] T4.3 `routes-all.js` → 0 wrong landing (2 chiều)
- [x] T4.4 `cls-probe.js` → CLS
- [x] T4.5 danger-tint + focused-probe + f1302-a11y + f1320
- [x] T4.6 Tự dọn + assertClean CLEAN

## Phase 5 — MCP deep UI/UX 2 ENGINE `[gate]`
- [x] T5.1 chrome-devtools: font/token/contrast/overflow (evaluate_script)
- [x] T5.2 chrome-devtools: Lighthouse Home/Lessons/Admin
- [x] T5.3 chrome-devtools: perf trace (LCP/INP/CLS) + heap snapshot
- [x] T5.4 chrome-devtools: emulate dark + reduced-motion + mobile
- [x] T5.5 playwright: 4 flow demo + 5 vùng chính, screenshot
- [x] T5.6 playwright: upload/mic/YouTube embed
- [x] T5.7 Đối chiếu chéo 2 engine

## Phase 6 — Vòng 2 (lặp sâu, bắt lỗi ngắt quãng) `[gate]`
- [x] T6.1 Lặp api-sweep + deep-probe + ui-sweep + design-v2 + routes-all
- [x] T6.2 AI path ×N≥10
- [x] T6.3 Kịch bản khắc nghiệt (token hết hạn, sai role, input hỏng, biên, outage, reduced-motion)
- [x] T6.4 Cleanup verification
- [x] T6.5 Cross-review mọi fix

## Phase 7 — Security scan `[gate]`
- [x] T7.1 claude-security scan → SARIF/CWE + report
- [x] T7.2 Authz/rate-limit/upload-XSS/error-leak/AI-output thủ công
- [x] T7.3 Đối chiếu 13 test `AuditV*` → lỗ hổng thiếu guard

## Phase 8 — Hiệu năng frontend
- [x] T8.1 Bundle size vs 177.75 kB
- [x] T8.2 Lighthouse CWV có số trước/sau
- [x] T8.3 Kiểm DOMPurify lazy / lucide tree-shake

## Phase 9 — Review code AI sinh + mối liên kết
- [x] T9.1 `/simplify` (4 agent) trên diff
- [x] T9.2 review-agent + cross-review
- [x] T9.3 Kiểm comment-only lần cuối + full `mvnw test`
- [x] T9.4 HarnessDriftTest + assert-harness

## Phase 10 — Dọn rác `[gate]`
- [x] T10.1 Verify-tham-chiếu các ứng viên (tasks.md, CSS stub, dir rỗng, .playwright-mcp)
- [x] T10.2 Xoá theo danh sách; liệt kê file + lý do
- [x] T10.3 Build + test vẫn xanh sau dọn

## Phase 11 — Báo cáo
- [x] T11.1 `findings.md` (đủ trường)
- [x] T11.2 `REPORT.md` (đủ bảng)
- [x] T11.3 `prompt-rewritten-v21.md`
- [x] T11.4 `converge.md` + `analyze.md` lần 2
- [x] T11.5 Cập nhật `docs/demo-engflow-4-chuc-nang.md` + `AGENTS.md` nếu đổi số
