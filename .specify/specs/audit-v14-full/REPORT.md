# audit-v14-full — BÁO CÁO CUỐI

**Ngày:** 2026-09-25 (+07) · **Nhánh:** `audit-v14-full` · **Checkpoint đầu:** `e729b2c` (tree sạch)
**Tiền nhiệm:** `audit-v13-full` (2026-09-22) · **Artifact home:** `.specify/specs/audit-v14-full/`
**Người dùng yêu cầu:** một bản **hoàn toàn mới, gắt gao và tỉ mỉ hơn** — e2e, đào sâu; fix tận gốc + test
hồi quy; lặp đến khi cạn lỗi; **không mock data**; review chéo mọi fix; **xoá file rác + liệt kê**; không tạo
GitHub issues.

---

## 1. TL;DR

| Hạng mục | Kết quả |
|---|---|
| **Tổng finding** | **5** (F-14-01, 02, 03, C1, C2) |
| **Đã fix** | **2** — F-14-01 (harness data-loss), F-14-02 (500→503) |
| **CLOSED (triage)** | **1** — F-14-03 (118 tap-target → 0 REAL) |
| **OPEN (quyết định owner)** | **2** — F-14-C1 (67/72 user test), F-14-C2 (109 payment PENDING) |
| **Lỗi probe tự gây** (KHÔNG phải finding sản phẩm) | **6** — P1–P3 (ui-sweep), E1–E2 (e2e), adversarial content-type |
| **Backend suite** | **525 / 0 fail / 0 error / 11 skipped, BUILD SUCCESS** (từ 523, **+2**) |
| **Frontend suite** | **194 passed / 1 skipped (31 file)** |
| **Build entry** | **177.98 kB** (gzip 67.76) — không đổi |
| **API sweep** | **143 pass / 0 fail / 1 blocked / 3 N/A** |
| **Reconcile** | **148/148 accounted, unaccounted=0, 26/26 controller 100%** |
| **UI sweep** | **117 route×role, 0 guardFail, 0 console/page/api error, 0 contrast fail, 0 overflow** |
| **Tap-target** | **118 đo → triage 0 REAL** (50 spacing + 68 inline except) |
| **E2E 3 tầng** | **7/7 PASS** (đẳng thức DB) |
| **Perf** | 35 endpoint, **stackStable=true**, chỉ 1 >100ms |
| **CLS** | `/` 0.00069 · `/lessons` 0.00095 · `/login` 0.00003 |
| **Parity** | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — **baseline** |
| **Font / design system** | Be Vietnam Pro xác nhận (`document.fonts.check`=true); 0 hit Outfit/Plus Jakarta |
| **Vòng 2** | **2 vòng liên tiếp 0 finding sản phẩm mới** |
| **Rác đã xoá** | `tmp/` (29 file) + `sweep/v14/` (25 file) — xem §6 |

**Điều quan trọng nhất về phương pháp:** một harness **do chính tôi viết** (`coverage-sweep.js`) đã **xoá
dữ liệu thật** (lesson 445 + cascade). Nó **được phát hiện** nhờ re-assert parity ngay sau sweep, **được
khôi phục** từ backup, và **được sửa tận gốc** (destructive method → ghost id). Đây là lý do người dùng yêu
cầu kỷ luật bằng chứng — và nó đã hoạt động.

---

## 2. Đã làm

### Phase 0 — freeze + khôi phục harness + baseline (đo bằng phiên này)
- Backend **523/0/0/11** → sau fix **525/0/0/11**; frontend **194/1 (31 file)**; build **177.98 kB**.
- 8 container up; backend **không stale**.
- Parity `1470|43735|72|118|29|15|4|126|10|5`.
- Khôi phục **16 file** harness từ `a627569` → `sweep/v14/`, đổi namespace `AUDIT-V13→V14`, tham số hoá
  ngày cleanup (`VN_RUN_DATE`), thêm `assert-namespace.js` (guard D4).
- **Phát hiện tiền đề:** host `frontend/node_modules` **RỖNG** (container dùng named volume) → `npm install`.

### Phase 1 — DB trong Docker (read-only)
- **Hiệu lực constraint** chứng minh trong scratch DB: FK 547, CHECK 547, UNIQUE 2627, filtered 2601; scratch DROP.
- **0 orphan / 16 FK**, NULL FK đếm riêng.
- F-13-07 CHECK còn hiệu lực; F-13-08 2 cột chết **đã xoá**.
- LISTENING thiếu audio **0/358** (lệch AGENTS.md "9/367" → doc-drift).
- **Nợ dữ liệu đo được:** 67/72 user là test (F-14-C1); 109 payment PENDING (F-14-C2).

### Phase 2 — API sweep toàn bộ
- **143 pass / 0 fail / 1 blocked / 3 N/A** (Ollama bật; ban đầu Ollama tắt → 2 endpoint AI 500 → F-14-02).
- **Bảng reconcile MỚI: 148/148, unaccounted=0, 26/26 controller 100%** — đóng khoảng trống v13 (D1/D10).
- Deep-probe **58/0**; role matrix **25/25** 2 chiều; search/sort đo (case-insensitive; `?sort=badprop`→400).
- Guard F-13-01/12/13 verify 2 chiều.
- **Sự cố F-14-01:** coverage-sweep xoá dữ liệu thật → khôi phục + fix harness.

### Phase 3 — UI sweep bằng CẢ HAI MCP
- **117 route×role, 0 guardFail** (sửa 3 assertion sai của v13: `?redirect=`, premium+auth, payment cleanup).
- **0 contrast fail**, 0 missing alt, 0 no-name, 0 overflow (5 width).
- **Tap-target triage (D2):** 118 đo → **0 REAL** (50 spacing + 68 inline except) — đóng khoảng trống v13.
- chrome-devtools MCP: token/font/Lighthouse a11y **100** trên `/`, `/lessons`, `/login`.

### Phase 4 — E2E 3 tầng UI → API → DB
- **7/7 PASS** với **đẳng thức** DB (streak, search id); MC closure F-13-01; CRUD 1→1→0.

### Phase 5 — Hiệu năng (đo trước, P5)
- 35 endpoint, **stackStable=true** (D3 — v13 đo khi stack động); chỉ 1 >100ms (LIKE đã biết).
- **Từ chối tối ưu** — số không biện minh. CLS rất tốt.

### Phase 6 — Fix tận gốc + test hồi quy
- **F-14-01** (harness): destructive → ghost id; 13 POST mutate → BLOCKED; integrity check cuối script.
- **F-14-02** (503): `@ExceptionHandler(WebClientRequestException)` + `containsConnectionRefused`; **+2 test**.
- Doc-drift sửa: README (194 test, 48 .vue), AGENTS.md (525 test, 0/358 listening), feature.json.
- Review chéo: `review-f14.md` (agent độc lập chết vì lỗi API → tự review inline, ghi rõ).

### Phase 7 — Vòng 2 (loop-until-dry)
- **14/14 adversarial PASS**; chạy lại mọi probe → **0 finding sản phẩm mới** ở 2 vòng liên tiếp.

### Phase 8–9 — converge / analyze / report / cleanup + đóng OPEN cũ
- 20/20 prior finding có verdict v14 (`prior-hypotheses.md`).

---

## 3. Chưa làm / OPEN (nói thẳng)

| # | Vấn đề | Trạng thái |
|---|---|---|
| **F-14-C1** | 67/72 tài khoản user là test/audit; 4 là admin | **OPEN** — nợ dữ liệu, không lộ ra UI (leaderboard sạch); cần quyết định owner |
| **F-14-C2** | 109 `payment_transactions` PENDING không `transaction_id` (07-24→09-12) | **OPEN** — checkout bỏ dở tích tụ; cần quyết định owner |
| **F-13-02 A2/A3** | Lesson Builder QUESTION/SUBMISSION chưa render + chưa chấm | **OPEN** (ngoài phạm vi V2) — A1 xác nhận done |
| **F-13-09** | SQL Server UTC, 3 đồng hồ lệch | **OPEN (đã biết)** — không consumer thứ hai |
| **AI generate-async E2E** | Ghi exercise thật qua Ollama | **BLOCKED** — dùng đường `validate` thay thế |

**Giới hạn đã gặp (ghi để không ai tưởng đã phủ):**
- **Lighthouse không phát category Performance** trên host này → perf đo trực tiếp bằng `perf-probe.js`.
- **Không có unit test end-to-end cho F-14-02** (service ném → handler 503); live test thay thế.
- **A11y keyboard-traversal / modal focus-trap** chưa đo tự động lại (chỉ heading/lang/skip-link qua 2 MCP).
- **Agent review độc lập cho F-14-02 chết vì lỗi API** → review do người thực hiện tự làm.

---

## 4. Đã fix & fix thế nào

| # | Fix | Bằng chứng |
|---|---|---|
| **F-14-01** | `coverage-sweep.js`: DELETE/PUT/PATCH → ghost id `999999999`; 13 POST mutate → BLOCKED kèm lý do; integrity check cuối script assert parity **+ `study_days`** | `parityOk=true`; dữ liệu khôi phục (lesson 445 content 5948, 6 ex; section 5; parity baseline); `incident-f14-01-data-loss.md` |
| **F-14-02** | `GlobalExceptionHandler`: `@ExceptionHandler(WebClientRequestException.class)`→503; `containsConnectionRefused()` trong `handleRuntimeTimeout`→503 | TDD: compile fail trước → **8/8 test pass**; live: Ollama tắt→**503**, bật→**200** |
| Doc-drift | README 194 test / 48 .vue; AGENTS.md 525 test / 0-358 listening; `.specify/feature.json`→v14 | `doc-drift.md` |

---

## 5. Skill / plugin đã nạp trong phiên

| Skill / plugin | Dùng để làm gì |
|---|---|
| `speckit-*` (constitution→specify→clarify→checklist→plan→tasks→implement→converge→analyze) | Pipeline bắt buộc |
| `superpowers:using-superpowers` | Luật nền: nạp skill trước khi hành động |
| `superpowers:brainstorming` | Chốt phạm vi + 4 quyết định với người dùng |
| `superpowers:systematic-debugging` | Truy gốc F-14-01 (data-loss) + F-14-02 (500→503) |
| `superpowers:test-driven-development` | F-14-02: compile fail trước → pass sau |
| `superpowers:verification-before-completion` | Mọi tuyên bố "pass" có output lệnh đứng sau |
| `workflow-authoring` + `Workflow` tool | Tham chiếu orchestration (Plan agent thiết kế delta D1–D12) |
| **chrome-devtools MCP** (`evaluate_script`, `list_console_messages`, `lighthouse_audit`, `navigate_page`) | Driver UI 1 + Lighthouse |
| **Playwright MCP / playwright-core** (`browser_*`) | Driver UI 2: route×role, tap-target, CLS |
| `accessibility` (WCAG 2.2) | Contrast, tap-target triage, exception Inline/Spacing |
| `frontend-design` | Xác nhận design system + prompt holes |
| `java-springboot`, `java-coding-standards` | Đọc backend, xác nhận gốc exception/constraint |
| `generate-tests` | Sinh test hồi quy F-14-02 |
| `prompt-master` | Phân tích lỗ hổng prompt (H1–H11) |

---

## 6. Cleanup — file đã xoá (ràng buộc mới của người dùng)

**Xem `evidence/cleanup-manifest.md` để có danh sách đầy đủ + verbatim.**

| Nhóm | Đã xoá |
|---|---|
| `tmp/` (KHÔNG được .gitignore) | **cả thư mục**, 29 file scratch |
| `sweep/v14/` (harness) | **25 file** (đã chuyển `perf-before.json`+`cls-before.json` sang evidence trước khi xoá) |
| Row DB probe | payment PENDING hôm nay; `study_days` 30067; `lesson_snapshots` 30030; vocab `zzburst%` |
| Scratch DB | `english_learning_v14probe`, `english_learning_v14restore` (DROP) |
| Build output | `target/`, `frontend/dist/` (tái tạo được) |

**KHÔNG đụng:** `uploads/**` (runtime data), spec v8–v13, `.env*`, backup ngoài repo, `node_modules/`.

**Verify sau dọn:** `git status --short` sạch (chỉ deliverable + 6 file sửa); parity = baseline.

---

## 7. Kiểm chứng end-to-end (cách chạy lại)

```bash
# Backend
cmd /c "mvnw.cmd -o test"                              # 525 / 0 / 0 / 11
# Frontend (npm install trước vì host node_modules rỗng)
cd frontend && npm install && npx vitest run && npx vite build   # 194 / 1 (31) ; 177.98 kB
# API (cần Ollama chạy: ollama serve)
node sweep/v14/api-sweep.js && node sweep/v14/deep-probe.js && node sweep/v14/reconcile-inventory.js
# UI
NODE_PATH=%APPDATA%\npm\node_modules node sweep/v14/ui-sweep.js && node sweep/v14/tap-target-triage.js
# E2E + adversarial
node sweep/v14/e2e-3tier.js && node sweep/v14/adversarial.js
# Perf (stack phải tĩnh)
node sweep/v14/perf-probe.js
# DB parity
python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql
# Rebuild container sau khi sửa backend
docker compose up -d --build backend
```
> **Lưu ý:** `sweep/v14/` đã bị xoá theo cleanup — muốn chạy lại thì khôi phục từ git:
> `git show a627569:sweep/v13/<file>` (hoặc từ chính commit v14 nếu đã commit).

---

## 8. Kết luận trung thực

1. **Một sự cố tự gây được phát hiện và khôi phục:** harness của tôi xoá lesson 445 + cascade; **re-assert
   parity** bắt được, **backup** cứu dữ liệu, **fix tận gốc** phòng tái diễn. Ghi công khai, không che.
2. **Khoảng trống thật của v13 đã đóng:** reconcile 148/148 (v13 không chứng minh được); tap-target triage
   118→0 REAL (v13 đo rồi bỏ); perf trên stack tĩnh (v13 đo khi stack động).
3. **Không tô hồng "đã xong":** font BVP + design system **đã có từ trước** — v14 chỉ **verify**.
4. **6 lỗi probe tự gây** (P1–P3, E1–E2, content-type) **không** tính là finding sản phẩm — ghi riêng.
5. **5 finding** (2 FIXED, 1 CLOSED, 2 OPEN-nợ-dữ-liệu); **không** finding nào bị bỏ mà không ghi.
6. **Không tối ưu mò:** chỉ 1 endpoint chậm (LIKE đã biết) — **từ chối** tối ưu có lý do (P5).
7. **Mọi con số trong báo cáo này do phiên v14 đo**, không sao chép. Chỗ trùng ghi "xác nhận", chỗ lệch
   ghi "phát hiện" (LISTENING 0/358, README 47→48 .vue, 193→194 test).
8. **Rác đã xoá + liệt kê** theo đúng ràng buộc người dùng (§6 + `cleanup-manifest.md`).
