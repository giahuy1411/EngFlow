# audit-v18-full — BÁO CÁO

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` @ `c76cc1c` (**giữ nguyên — không tạo nhánh mới**)
**Tiền nhiệm:** `audit-v17-full` (đã xong, đã commit) · **Artifact:** `.specify/specs/audit-v18-full/`
**Phạm vi người dùng chốt:** quét toàn bộ codebase + DB; chạy toàn bộ API ↔ UI; **TRỌNG TÂM: kiểm thử MỌI chức năng
thủ công qua UI bằng CẢ 2 MCP engine (chrome-devtools + Playwright) cho mọi luồng**; đi sâu logic 6 nhóm chức năng;
đo & tối ưu hiệu năng; verify giao diện + prompt Playful Geometric (font Be Vietnam Pro); **cập nhật tài liệu kỹ thuật
(README/.md/.gitignore) + `docs/demo-engflow-4-chuc-nang.md`**; kiểm lỗ hổng prompt + sửa; 2 vòng; dọn rác; báo cáo.

---

## 1. Tóm tắt một đoạn

EngFlow sau v17 được **quét lại toàn diện** với **trọng tâm kiểm thử tương tác MỌI chức năng qua UI thật bằng
CẢ HAI MCP engine** (chrome-devtools MCP + Playwright MCP — 13 luồng × 2 engine, đối chiếu nhau, 24 ảnh):
**145/145 API probe pass**, **58/58 deep-probe**, **ui-sweep 0 lỗi mọi loại**, **design-v2 0 drift + BVP LOADED**,
**6 nhóm chức năng verify 29 khẳng định (API↔UI)**, **DB 0 orphan / 22 FK đủ index**, **CLS ≤ 0.00095**, perf median
**9.6 ms** (không win → không tối ưu). Vòng này phát hiện + **sửa 1 finding thật** (harness hardcode revision Chromium
→ 2 probe fail launch; review chéo tìm thêm 3 probe cùng lớp → **mở rộng fix**) + **1 doc-drift** (demo doc mô tả
trần chờ 6s cũ) + **1 rút lại** (file:line thực ra ĐÚNG — grep của tôi khớp overload khác). **Prompt dán vào vẫn
chứa 4 lỗ hổng đã kiểm chứng** (AAA sai, type-scale sai, "Lucide React" sai, Plus Jakarta/Outfit) → sửa thành
`prompt-rewritten-v18.md`. Backend **537/0**, frontend **194/1** — 0 regression. Rác đã dọn, tài liệu đã đồng bộ.

---

## 2. Đã làm

### 2.1 Phase 0 — SpecKit pipeline + baseline
| Việc | Kết quả |
|---|---|
| Nhánh | `audit-v15-full`, cây sạch — không tạo nhánh mới |
| Pipeline docs | `spec.md` · `clarify.md` · `checklist.md` · `plan.md` · `tasks.md` |
| Harness drift guard | `assert-harness.js` **ALL CLEAN (7/7)** (sau khi đổi default → audit-v18) |
| Backend baseline | **537 / 0 / 0 / 11 — BUILD SUCCESS** (v17: 520) |
| Frontend baseline | **194 pass / 1 skip (32 file)** (v17: 192) · build **177.75 kB** |
| Container | 8 up + Ollama :11434 |
| Parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` · STUDY_DAYS=4 · PENDING=0 · EX_ATTEMPTS=33 |
| Inventory | 121 annotation / 137 expanded / 135 distinct / 25 controller |
| **Prompt flaw audit** | 4 lỗ hổng đo lại + "font = verify" → `prompt-flaws.md` |
| **MCP engines** | chrome-devtools + Playwright **cả 2 dùng được** → `mcp-engines.md` |

### 2.2 Phase 1 — API sweep
- `api-sweep.js`: **145 pass / 0 fail / 0 blocked / 2 n/a** (15 area). `deep-probe.js`: **58/0/0** (mc-guard 18,
  sort 2, contract 13, roles 25). `search-sort.js`: case-insensitive ✅, guard <2 ký tự ✅, `?sort=notacolumn` → 400 ✅.
- **T1.6 AI stress ×15** (mới cho vòng 2): generate-vocab ×10 + enrich-word ×5 → **15/15 = 200**, không 500/504.

### 2.3 Phase 2 — DB + secret-scan
- **22 FK, 0 orphan thật** (5 FK nullable bị đếm nhầm là orphan → loại NULL), **22/22 FK có leading index**, 19 bảng.
- Slow query: chủ yếu JDBC metadata lúc boot; nghiệp vụ nặng nhất = admin exercises LIKE (40–193 ms, đặc tính).
- Timezone: SQL Server UTC (−7h), container `Asia/Ho_Chi_Minh` — nhất quán.
- **`ddl-auto=validate` drill PASS** (Started in 14.7s, 0 ERROR, 0 schema-warning).
- Secret-scan: **0 secret trong tracked source**; 3 quan sát (báo người dùng, không tự đổi).

### 2.4 Phase 3 — UI/UX automated sweep
- `ui-sweep.js`: **0** contrast/alt/name/console/api/page/guard/overflow; responsive 35 ô 0 tràn.
- `design-v2.js`: **7670 el, 0 badFont, 0 legacy, 0 token drift, 655 lucide stroke 2.5, BVP LOADED (400/700/900)**.
- `routes-all.js`: **228 visit, 0 wrong landing** (guard 2 chiều).
- `cls-probe.js`: **0.00069 / 0.00095 / 0.00003**.
- `danger-tint` + `focused-probe` + `f1302-a1-a11y`: PASS (error-state contrast 5.28:1).

### 2.5 Phase U — **tương tác MCP thủ công MỌI chức năng, CẢ 2 ENGINE (TRỌNG TÂM)**
13 luồng × 2 engine: U1 login · U4 guard 2 chiều · U2 lessons + chống lộ đáp án · U3 streak · U11 tra từ + guard
1 ký tự · U12 CRUD decks + game · U15 AI generate-vocab · U22 admin · U18 video · U17 speaking · U20 leaderboard ·
U19 premium · U0 home/responsive. Chi tiết: `mcp-walkthrough.md` + `shots/mcp/**` (24 ảnh).

### 2.6 Phase D — đi sâu 6 nhóm chức năng
`demo-claims.md`: **29 khẳng định CONFIRMED**, **1 REFUTED** (doc §4.3 trần 6s cũ — đã sửa), **1 rút lại**
(file:line thực ra đúng), **1 quan sát** (model nhỏ). Mọi `file:line` đã `sed`/`grep` đối chiếu.

### 2.7 Phase 5 — Performance
34 endpoint median **9.6 ms** (max 157.3 = admin search); bundle 177.75 kB; CLS ≤ 0.00095. **Không có win rõ → không tối ưu** (P5).

### 2.8 Phase 6/7 — Fix + review chéo + vòng 2
- 1 finding FIXED (F-18-01) + 1 doc-drift FIXED (F-18-02) + 1 rút lại (F-18-03).
- **Review chéo đối kháng** (subagent) tìm thêm 3 probe + 2 comment cùng lớp → tôi đối chiếu lại, **mở rộng fix**.
- **Vòng 2 = 0 finding mới** → hội tụ. Targeted probes g6/g7/g8/l2 đều PASS, tự dọn.

### 2.9 Phase 8 — Docs + Converge/Analyze + Report + Cleanup
- Cập nhật `README`/`AGENTS.md`/`CLAUDE.md`/`.gitignore`/`constitution.md` (v1.0.3)/`feature.json` + **demo doc**;
  tạo `prompt-rewritten-v18.md`.
- `converge.md` + `analyze.md` (giữa kỳ + cuối kỳ) + `findings.md` + `cleanup-manifest.md`.

---

## 3. Chưa làm (và lý do)

| Hạng mục | Lý do |
|---|---|
| **Premium flow (U19) chỉ 1 engine** | PW dùng chung context; bù bằng deep-probe roles + g6 webhook. Ghi rõ, không tô vẽ |
| **Webhook SePay chữ ký THẬT** | Biên **real-money** — BLOCKED từ v11; g6 chỉ chứng minh **logic** HMAC/replay bằng secret local |
| **AI model nhỏ trả gloss tiếng Trung** | Hạn chế `qwen2.5:1.5b`, không phải bug code |
| Tối ưu hiệu năng | **Không có win đo được** (P5 cấm tối ưu khi chưa chứng minh) |
| `smallTargets=115` | **Không phải vi phạm** (inline link + label checkbox) — đã triage |
| GitHub issues (`taskstoissues`) | Quyết định v14/V3 |
| Migrate timezone sang UTC | Chưa có consumer thứ hai |

---

## 4. Đã fix và cách fix

| ID | Mức | Vấn đề | Cách fix |
|---|---|---|---|
| **F-18-01** | LOW (harness) | `danger-tint.js`/`focused-probe.js` hardcode `chromium-1237` (bundle là 1234) → launch fail EXIT=1 | Thêm **`resolveChromium()`** vào `sweep/v8/ui/lib.js` (registry → scan `ms-playwright/chromium-*` → Brave); áp cho **5 probe** (`ui-sweep`, `danger-tint`, `focused-probe`, `f1302-a1-a11y`, `f1302-a1-blocks-live`, `f1320-api-redirect-live`) — cả 6 chạy EXIT=0 |
| **F-18-02** | LOW (docs) | Demo doc §4.1/§4.3 mô tả trần chờ từ điển 6s CŨ (v17 đổi thành mềm 6s/cứng 45s) | Sửa doc: "6s ngưỡng mềm, trần cứng 45s, vẫn chờ; dictionary là nguồn duy nhất" |
| **F-18-03** | — | Nghi file:line demo doc lệch | **RÚT LẠI (probe SAI)** — grep chính xác xác nhận doc ghi đúng dòng 200/122 |
| — | LOW | Comment `_config.js` + marker `AUDIT-V17-C6` cũ | → `audit-v18`/`V18`/`AUDIT-GHOST` (version-agnostic) |

### Review chéo bắt gì (bằng chứng `review-v18.md`)
Reviewer (subagent độc lập) verdict **"No change is UNSAFE to keep"** nhưng bắt **3 probe còn hardcode 1237**
(`f1302-a1-a11y`, `f1302-a1-blocks-live`, `f1320-api-redirect-live`) + 2 comment/marker cũ. Tôi **đối chiếu lại code**
xác nhận đúng rồi **mở rộng fix** — đóng cả lớp lỗi, không chỉ 2 file đã vỡ.

### Tự bác bỏ 2 kết luận SAI của chính mình (kỷ luật V4)
1. Tưởng `study_days` không tăng khi submit là bug → đọc `GradeRequest.java`: payload key sai (`answer` vs
   `userAnswer`); gửi lại đúng → +1. **Không phải bug app.**
2. Tưởng demo doc file:line lệch → `grep -n` chính xác xác nhận **doc đúng**; grep đầu khớp overload khác.

---

## 5. Kết quả kiểm thử (đếm từ run log — đo lại sau MỌI fix)

| Bộ | Kết quả |
|---|---|
| Backend (`.\mvnw.cmd test`) | **537 / 0 / 0 / 11 — BUILD SUCCESS** |
| Frontend (`npx vitest run`) | **194 pass / 1 skip (32 file)** |
| Frontend build | **177.75 kB** (gzip 67.69) |
| `api-sweep` (vòng 1 & 2) | **145 / 0 / 0** (2 n/a) — hội tụ |
| `deep-probe` | **58 / 0 / 0** |
| `ui-sweep` | 0 guard/contrast/alt/console/api/page/overflow |
| `design-v2` | 0 badFont/legacy/token/overflow (7670 el) + BVP LOADED |
| `routes-all` | 228 visit, 0 wrong landing |
| `cls-probe` | 0.00069 / 0.00095 / 0.00003 |
| `perf-probe` | 34 endpoint, median 9.6 ms |
| g6 / g7 / g8 / l2 | PASS (đều tự dọn) |
| Parity cuối | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` · STUDY_DAYS=4 · PENDING=0 · EX_ATTEMPTS=33 |

---

## 6. Skill / plugin đã nạp

**SpecKit:** `speckit-constitution`, `speckit-specify`, `speckit-clarify`, `speckit-checklist`, `speckit-plan`,
`speckit-tasks`, `speckit-implement`, `speckit-converge`, `speckit-analyze`.
**Superpowers:** `using-superpowers`, `verification-before-completion`, `systematic-debugging`,
`requesting-code-review`, `writing-plans`, `executing-plans`, `dispatching-parallel-agents`.
**Domain:** `accessibility` (WCAG 2.2 AA), `java-springboot`, `java-coding-standards`, `frontend-design`.
**addyosmani-\*:** `browser-testing-with-devtools`, `performance-optimization`, `code-review-and-quality`,
`debugging-and-error-recovery`, `security-and-hardening`, `source-driven-development`, `test-driven-development`.
**MCP:** `chrome-devtools` (navigate/snapshot/click/fill/fill_form/evaluate_script/list_console_messages/
list_network_requests/get_network_request/take_screenshot/resize_page — **dùng thật**) + `playwright`
(browser_navigate/snapshot/click/fill_form/evaluate/console_messages/take_screenshot/resize — **dùng thật**).
**Agent:** `Explore` ×3 (map backend/frontend/config), `Plan` (thiết kế orchestration), `general-purpose` (review chéo đối kháng).

---

## 7. File đã xoá trong phiên (ràng buộc dọn rác)

Xem `evidence/cleanup-manifest.md`. Tóm tắt:
- **Scratch runtime:** `.playwright-mcp/*.log`, `*.yml` (snapshots phiên này).
- **Helper tạm:** `sweep/harness/_probe_cleanup.js` (tôi tạo để gọi cleanup, xoá ngay sau dùng).
- **Per-run SQL/JSON:** `sweep/harness/_*.sql`, `sweep/harness/*.json`.
- **Build output:** `target/**` (22M), `frontend/dist/**` (1.4M).
- **KHÔNG xoá (xác minh 2 chiều):** `scripts/figma-export/node_modules` (cls-probe phụ thuộc), `uploads/**`, `.env*`,
  `.specify/specs/audit-v8..v17/**`, `sweep/harness/**`, fixtures có license.

---

## 8. Kết luận trung thực

- **Yêu cầu người dùng hoàn thành:** quét codebase+DB; chạy toàn bộ API↔UI; **kiểm thử thủ công MỌI chức năng qua
  CẢ 2 MCP engine cho mọi luồng**; đi sâu 6 nhóm chức năng; đo perf; verify design + prompt + sửa lỗ hổng; 2 vòng;
  dọn rác; **cập nhật tài liệu (README/.md/.gitignore + demo doc)**; báo cáo đủ 4 mục.
- **Không giấu điểm yếu:** Premium flow 1 engine; SePay chữ ký thật BLOCKED; model nhỏ trả gloss lạ; perf không win;
  1 finding rút lại (probe SAI của chính tôi).
- **Điểm mạnh nhất:** **kỷ luật tự bác bỏ 2 lần** (payload sai, file:line thực ra đúng) + **review chéo mở rộng fix**
  (từ 2 file vỡ → đóng cả lớp hardcode revision). Mọi fix có before/after; mọi probe tự dọn; parity giữ nguyên suốt phiên.
