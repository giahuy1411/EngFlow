# audit-v21-full — REPORT (báo cáo cuối)

**Ngày:** 2026-09-28 (+07) · **Nhánh:** `audit-v15-full` · **Nền:** commit `b539d11`
**Phạm vi:** quét codebase + CSDL, chạy toàn bộ API, test UI/UX trên **2 MCP engine**,
kiểm 5 vùng chính, verify design system, comment tiếng Việt toàn bộ code, review code AI sinh, dọn rác.

---

## 1. Tóm tắt điều hành

| Câu hỏi của người dùng | Trả lời |
|---|---|
| Font đã là Be Vietnam Pro chưa? | **RỒI** — font DUY NHẤT, đo runtime `distinctFonts = ["Be Vietnam Pro"]`. Yêu cầu "thay font" là **verify + guard**, không phải thay. |
| Giao diện đồng bộ design system chưa? | **RỒI** — 2 engine MCP: token khớp, nút 2px pill + hard shadow, h1 weight 900, decoration literal đủ (circle/blob/connector/squiggle/dot), featured card scale(1.1) + badge rotate(15deg). |
| Đã chạy toàn bộ API chưa? | **RỒI** — api-sweep **145 pass / 0 fail**, deep-probe **58 pass / 0 fail**, search-sort 0 finding. |
| Comment toàn bộ code chưa? | **RỒI** — Java: 44 file thiếu → **0**; Frontend: 33 file thiếu → **0**. 100% file có comment tiếng Việt. |
| Có lỗi nghiêm trọng không? | **CÓ 1 HIGH** — comment AI sinh làm vỡ component Vue (F-21-03). Đã fix + gia cố công cụ. |
| Dọn rác chưa? | **RỒI** — xoá 17 mục (file `tasks.md` 7 byte, 4 dir rỗng, pycache, 13 stub CSS). |

**Phát hiện quan trọng nhất:** quy trình "**test full sau MỖI lô comment**" (đúng yêu cầu "từ từ từng phần,
test sau mỗi phần" của người dùng) đã **bắt được lỗi thật** F-21-03 — 3 component Vue bị vỡ (7 test fail)
do comment HTML ở cấp gốc `<template>` tạo fragment. Nếu làm một lần rồi test cuối, lỗi này sẽ lọt.

---

## 2. Đã làm gì

### Phase 0 — SpecKit + baseline (đo thật)
- Runtime 8 container up; backend boot 0 ERROR.
- Baseline **khớp 100%**: backend **541/0/0/11**, frontend **194/1/32**, build **177.75 kB**, parity
  `1470|43738|5|118|29|4|3|13|10`.
- Fix F-21-01 (default vòng audit, 2 nơi) + F-21-02 (harness-restore.md) → `assert-harness` **ALL CLEAN 8/8**.
- Đo lại lỗ hổng prompt → 4 lỗi cũ + **2 lỗi mới** (F5, F6) có bằng chứng.
- Viết `spec.md`, `clarify.md`, `checklist.md`, `tasks.md`, `prompt-rewritten-v21.md`.

### Phase 1 — Comment tiếng Việt toàn bộ (ưu tiên #1, chia lô)
- Backend **44 file** (lô J1–J5): enums, projections, DTO, repository, config, security, controller, service.
- Frontend **33 file** (lô F1–F3): ui/common/layout/decor components, services, stores, composables, utils.
- **Mỗi lô chạy full suite.** Bắt được F-21-03 → fix → gate xanh lại.
- Kiểm chứng "comment-only" bằng `comment_only.py` (hiểu Java text block).
- **Kết quả: 0 file Java + 0 file frontend thiếu comment tiếng Việt.**

### Phase 2 — API sweep
- api-sweep **145 pass / 0 fail / 0 blocked / 2 n_a**; deep-probe **58 pass / 0 fail**
  (mc-guard 18, sort 2, contract 13, roles 25); search-sort **0 finding**.
- AI stress: `generate-vocab` **10/10 200, 0 CJK**; `enrich-word` **5/5 200** (sau flush bucket `:ai`).

### Phase 3 — DB + hiệu năng
- **0 orphan FK** (8 quan hệ), **0 cột FK thiếu index**, 2 index đã biết còn, **0 missing-index DMV**.
- `lessons` = **160 MB / 159 MB LOB** cho 1 472 dòng (bằng chứng số cho luật no-JOIN-FETCH).
- Timezone re-verify (SQL Server UTC vs VN +7). `ddl-auto=validate` drill: boot 15.868s, 0 schema error.
- Secret-scan: **0 secret bị commit**.

### Phase 4 — UI/UX automated sweep
- ui-sweep **ALL CLEAN** (contrast/alt/tap/console/api/overflow = 0), tự dọn, parity nguyên vẹn.
- design-v2: **0 non-BVP font**, 0 drift, 0 token thiếu (660 check), 0 lucide sai, 0 overflow/60 route.
- routes-all: **0 wrong landing**; CLS ≤ **0.00095**; danger-tint 5.28:1; g6/g8/l2 PASS.

### Phase 5 — MCP deep UI/UX (2 engine)
- chrome-devtools: font/token/decoration + Lighthouse **100/100/100** + LCP **434ms** / CLS **0.00**.
- playwright: 4 flow + 5 vùng, **0 console error**, guard admin 2 chiều đúng.

### Phase 6 — Vòng 2 (lặp)
- api-sweep + deep-probe + ui-sweep lặp lần 2 → **kết luận giống hệt** (145/58/ALL CLEAN).

### Phase 7 — Security
- Agent độc lập review: **0 CRITICAL/HIGH/MEDIUM**. Authz/upload-XSS/AI-sanitize/secret/rate-limit CLEAN.

### Phase 8 — Hiệu năng frontend
- Bundle **không đổi 177.75 kB**; LCP 434ms; CLS ~0. **Không cần tối ưu** (không có số chỉ điểm nghẽn).

### Phase 9 — `/simplify` + review code AI sinh (4 agent)
- Fix tận gốc F-21-06 (default vòng audit), F-21-07 (AdminLayout fragment ẩn), F-21-04/08 (công cụ).

### Phase 10 — Dọn rác
- Xoá 17 mục (verify tham chiếu trước); giữ `scripts/figma-export/node_modules`, `.env.bak-*`, fixture.

---

## 3. Kết quả kiểm thử (số đo thật)

| Harness / bộ test | Kết quả | Baseline |
|---|---|---|
| `mvnw.cmd test` | **541 / 0 fail / 0 err / 11 skipped — BUILD SUCCESS** | 541 ✓ |
| `npx vitest run` | **194 passed / 1 skipped (32 files)** | 194/1/32 ✓ |
| `npx vite build` | entry **177.75 kB** (gzip 67.68) | 177.75 ✓ |
| `assert-harness.js` | **ALL CLEAN 8/8** | ✓ |
| `api-sweep.js` | **145 pass / 0 fail / 0 blocked / 2 n_a** | ✓ |
| `deep-probe.js` | **58 pass / 0 fail** | ✓ |
| `ui-sweep.js` | **0** mọi loại lỗi + assertClean CLEAN | ✓ |
| `design-v2.js` | 0 non-BVP / 0 drift / 0 overflow / 0 lucide | ✓ |
| `routes-all.js` | 0 wrong landing / 0 console / 0 overflow | ✓ |
| `p16-parity.sql` | `1470\|43738\|5\|118\|29\|4\|3\|13\|10` + 3 marker | ✓ |
| Lighthouse `/` + `/lessons` | a11y/best-practice/SEO **100/100/100** | mới |
| Core Web Vitals `/lessons` | LCP **434ms**, CLS **0.00** | mới |
| `g6-sepay` / `g8-speaking` / `l2-cache` | **PASS / PASS / PASS** | ✓ |

---

## 4. Findings — đã fix gì và fix thế nào

| ID | Mức | Vấn đề | Fix |
|---|---|---|---|
| **F-21-03** | **HIGH** | Comment HTML cấp gốc `<template>` ở 3 file → **Vue fragment** → `wrapper.attributes()` undefined → **7 test fail**. | Bỏ comment cấp gốc (nội dung vào JSDoc `<script setup>`); gia cố `comment_only.py` coi comment template là code. |
| **F-21-06** | MED | Nguồn gốc lớp "default vòng cũ": `api-sweep.js` khai báo default riêng thay vì dùng `_config.js`. | `require("../harness/_config.js")` — chỉ còn 1 chỗ bump. |
| **F-21-07** | MED | Cùng lớp F-21-03 còn sót ở `AdminLayout.vue` (ẩn). | Dời comment ra trên `<template>`. |
| **F-21-01** | MED | Default audit round còn v20 (2 nơi). | → v21 cả hai; sau đó F-21-06 xử lý tận gốc. |
| **F-21-04** | LOW | `comment_only.py` che string → thay đổi chỉ-string lọt lưới. | Giữ nguyên string; mutation-test xác nhận. |
| **F-21-08** | LOW | 3 nhánh quote trùng + 2 danh sách ext song song. | Gộp 1 nhánh; bảng `EXT_KIND`; thêm `#`/`--`. |
| **F-21-02** | LOW | Thiếu `harness-restore.md`. | Tạo file; check 5 PASS. |
| **F-21-12** | MED | **5 trích dẫn dòng trong `docs/demo-engflow-4-chuc-nang.md` lệch** do comment đẩy dòng (lớp F-20-12). Công cụ remap neo-theo-block giữ nguyên 167 vì khối code trong doc bị lược trích. | Remap tay 5 chỗ: RedisConfig 52-87→55-90; LessonRepository 62-81→103-123, 66-81→108-123, 42-49→78-85; UserRepository 42-50→72-81. |

### Findings KHÔNG phải lỗi (đã đo, không suy đoán)

| ID | Kết luận |
|---|---|
| F-21-09 | `comment_only.py` spawn git mỗi file (~2s cho 69 file) — công cụ chạy tay, **không đáng tối ưu**. |
| F-21-10 | `_db-audit-v21.sql` trùng query FK-index với v16/v17 — artifact một lần, đúng chỗ. |
| F-21-11 | `enrich-word` 429 × 8 lần đầu — **không phải lỗi app**: bucket `:ai` 10/phút, đã tiêu hết bởi 10 lần `generate-vocab`. Sau flush: 5/5 200. |
| — | `/api/health` trả 401 — đúng thiết kế (sau `anyRequest().authenticated()`). |
| — | 1 tap target <24px trên Home — **inline text link** "đăng nhập", WCAG 2.5.8 miễn. |
| — | Dark mode vẫn nền sáng — **đúng thiết kế** (design system là Light Mode). |
| — | MC-fragment thiếu options → UI fallback "nhập đáp án" — đúng thiết kế (AGENTS.md). |

---

## 5. Còn lại / chưa làm

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| 2 method repository dead code (F-20-10) | **Giữ** | Có thể là API dự phòng; đã ghi trong Javadoc. |
| `PremiumRequired` 0 usage (F-20-11) | **Giữ** | Placeholder có chủ đích; đã ghi Javadoc. |
| `.env.bak-*` (4 file secret) | **Giữ** | gitignored, cần rollback. |
| `scripts/figma-export/node_modules` | **Giữ** | cls-probe.js cố ý dùng. |
| Polka/diagonal-stripe utility | **Không làm** | OPTIONAL, 0 sai lệch đo được. |
| GitHub issue (`taskstoissues`) | **Không làm** | `gh` chưa đăng nhập; người dùng không yêu cầu. |

### BLOCKED
**Không có mục nào BLOCKED.** Mọi hạng mục đều chạy được và có bằng chứng thật.

---

## 6. Skill / plugin đã nạp trong quá trình test

| Nhóm | Skill / plugin | Dùng vào việc gì |
|---|---|---|
| Browser/MCP | `chrome-devtools-mcp:chrome-devtools` (+ `a11y-debugging`, `debug-optimize-lcp`) | đo font/token/decoration, Lighthouse, perf trace, emulate |
| Browser/MCP | `playwright` (plugin) | 4 flow + 5 vùng, tương tác thật, screenshot |
| Pipeline | `speckit-constitution/specify/clarify/checklist/plan/tasks/implement/converge/analyze/taskstoissues` | workflow bắt buộc (yêu cầu #3) |
| Chất lượng | `review-agent`, `code-review`, `superpowers:verification-before-completion`, `superpowers:systematic-debugging` | review code AI sinh; truy F-21-03 |
| Điều phối | `superpowers:dispatching-parallel-agents` | 7 agent comment song song + 4 agent simplify |
| Java | `java-docs`, `java-coding-standards`, `java-springboot` | chuẩn Javadoc |
| Frontend | `frontend-design`, `redesign-existing-projects`, `accessibility` | hiểu design system |
| Bảo mật | `claude-security` (plugin), `addyosmani-security-and-hardening` | review bảo mật độc lập |
| Tài liệu | `docs/`, `AGENTS.md`, `.specify/memory/constitution.md` | nguồn sự thật |

> Ghi chú: `speckit-*` áp dụng như **khung workflow** (artifact ghi vào `.specify/specs/audit-v21-full/`),
> không gọi từng slash-command riêng vì vòng audit chạy trong một phiên.

---

## 7. Rủi ro cần lưu ý cho lần sau

1. **Comment template `.vue` = cấu trúc, không phải chú thích** (F-21-03): comment cấp gốc `<template>`
   là root node → fragment. Đặt comment trước `<template>` hoặc trong `<script setup>`.
2. **`comment_only.py` giữ comment template là code** → thay đổi cấu trúc bị bắt (đúng ý).
3. **Default vòng audit nay chỉ còn 1 chỗ** (`_config.js`) — F-21-06 đã xử lý tận gốc.
4. **Comment đẩy dòng → lệch citation doc**: công cụ remap có thể giữ nguyên khi khối code doc bị
   lược trích; **luôn hand-verify** vài trích dẫn.
5. **Đường AI cần lặp nhiều vòng** (đã lặp 15 lần); **bucket `:ai` 10/phút** — harness phải flush.
6. **Dictionary cold ~20s** — warm cache trước demo.

---

## 8. Bàn giao — file thay đổi chính

| Nhóm | Số file | Ghi chú |
|---|---|---|
| Java (comment) | 44 | Javadoc tiếng Việt |
| Frontend (comment) | 36 | JSDoc/comment + 3 fix fragment + AdminLayout |
| Harness | 5 | `comment_only.py` (mới + gia cố), `_db-audit-v21.sql` (mới), `_config.js`, `api-sweep.js` (fix tận gốc) |
| CSS | 1 | gỡ 13 stub rỗng |
| Docs | 1 | `docs/demo-engflow-4-chuc-nang.md` (5 citation remap) |
| Dọn | 1 | xoá `tasks.md` + 4 dir rỗng |
| Audit artifact | ~30 | `.specify/specs/audit-v21-full/` |

**Tổng diff vòng này:** ~133 file (phần lớn là Javadoc/comment).
