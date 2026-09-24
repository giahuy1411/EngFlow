# audit-v14-full — Kế hoạch kiểm thử & audit toàn diện EngFlow

**Ngày lập:** 2026-09-25 (+07) · **Nhánh:** tạo mới `audit-v14-full` từ `main` · **Checkpoint đầu:** `e729b2c` (tree sạch)
**Tiền nhiệm:** `audit-v13-full` (2026-09-22) · **Artifact home:** `.specify/specs/audit-v14-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue** (người dùng chốt)

---

## Context — vì sao có v14

Người dùng yêu cầu một bản **hoàn toàn mới, gắt gao và tỉ mỉ hơn mọi bản trước**: quét toàn bộ
codebase + CSDL, chạy **toàn bộ API**, tương tác **toàn bộ UI** tương ứng từng API, kiểm sát 6
chức năng chính (Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI), audit DB
trong Docker, tối ưu hiệu năng, test UI/UX bằng **cả hai** MCP (`chrome-devtools-mcp` +
`playwright`), verify giao diện đã khớp prompt **Playful Geometric + Be Vietnam Pro**, dùng
skill/plugin trong `C:\Users\ASUS\.claude`, tìm lỗ hổng của chính prompt rồi sửa, tuân thủ pipeline
`constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze`,
chạy **vòng 2 rộng hơn** fix tận gốc, và báo cáo trung thực.

**Ràng buộc mới (người dùng nêu trong phiên):** mọi file/folder rác sinh ra khi kiểm thử **phải
được xoá** sau khi hoàn thành, và báo cáo cuối **liệt kê chi tiết file đã xoá**.

### Bốn quyết định đã chốt với người dùng (clarify)

1. **Phạm vi:** audit-v14 **mới hoàn toàn, gắt hơn** v13 — coi kết luận cũ là **giả thuyết để kiểm
   chứng**, không kế thừa số. Đo lại mọi con số trong phiên này.
2. **Harness:** khôi phục từ git (`a627569`) thành `sweep/v14/`, đổi namespace `AUDIT-V13`→`AUDIT-V14`,
   trỏ output vào `audit-v14-full/evidence/` — và **xoá cuối phiên + liệt kê từng file đã xoá**.
3. **Sửa lỗi:** fix tận gốc + regression test (fail trước → pass sau) + **loop-until-dry** vòng 2.
   Phạm vi fix = **chỉ lỗi đo được trong v14**; F-13-02 A2/A3 và F-13-09 giữ là quyết định chủ sản phẩm.
4. **GitHub issues:** KHÔNG. `tasks.md` là nguồn duy nhất. **Evidence được commit** (như v13).

### Ba sự thật KHÔNG được "phát hiện lại" (đã đúng từ trước — v14 chỉ verify)

| Sự thật | Bằng chứng đo trong phiên khảo sát này |
|---|---|
| **Be Vietnam Pro đã là font DUY NHẤT** | `frontend/index.html:54`; `tailwind.config.js:80-85` (sans/heading/mono đều BVP); `design-system.css` `--geo-font`; grep Outfit/Plus Jakarta = 0 |
| **Playful Geometric đã triển khai đủ** | `--geo-*` tokens + `shadow-pop-*` + border 2px + hero sun/blob + dashed connector + pricing scale(1.1)+badge + marquee + reduced-motion (`design-system.css`) |
| **Lỗ hổng contrast của prompt đã được vá ở v11/v12** | tầng `--geo-*-ink` / `--geo-*-strong` với tỷ lệ đo được ghi ngay trong CSS |

v14 **đo lại bằng probe của chính nó**; khớp = *xác nhận*, lệch = *finding*. Không tính việc đã xong
là việc mới (bài học B1 của v12).

### Ground truth đo trong phiên khảo sát (không phải giả thuyết)

| Sự thật | Giá trị |
|---|---|
| Git | `main` @ `e729b2c`, tree **sạch** |
| Containers | **8 up** (backend:8080, frontend:5173, sqlserver:1433, redis:6379, minio:9000-9001, whisper:9002, tts:8001, tailscale) |
| Backend staleness | started `2026-09-24T16:25:54Z`; `.java` mới nhất 2026-09-22 → **KHÔNG stale** |
| Parity live | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — khớp v13 baseline |
| Endpoint inventory | **132 annotation → 148 rows / 146 distinct / 26 controllers** (GET64 POST50 PUT15 DELETE9 PATCH2) |
| Router | **40 dòng `path:` / 39 path tuyệt đối** (gồm 1 catch-all) |
| DB | **22 base table** (21 app + `sysdiagrams`), **25 FK**, **9 CHECK**; `study_policy` CHECK singleton **còn hiệu lực** (`ck_study_policy_singleton`, is_disabled=0, is_trusted=0) |
| v13 harness | **16 file** recoverable ở `a627569`; `git diff a627569 6b2836f -- sweep/v13/` = **rỗng** (byte-identical) |
| Môi trường host | `frontend/node_modules` **RỖNG** (container dùng named volume cho `/app/node_modules`) → Phase 0 phải `npm install` |

### Ba khoảng trống THẬT của v13 (v14 phải đóng — không phải ý kiến)

1. **Inventory không reconcile.** `endpoint-inventory.json.expandedRows = 148`, nhưng `api-sweep.json`
   = **143 pass + 1 blocked + 3 N/A = 147**. **1 hàng không có status**; v13 không hề xuất bảng join
   theo hàng → tuyên bố R2 ("mọi entry có status") **không chứng minh được từ artifact của chính nó**.
2. **118 vi phạm tap-target AA bị đo rồi bỏ khỏi báo cáo.** `ui-sweep.json.totals.smallTargets = 118`
   (`<24px` = WCAG 2.5.8 **AA fail**), `distinctSmallTargets = 18` — nhưng `REPORT.md` §1 chỉ liệt kê
   "0 guardFails, 0 console/page/api error, 0 contrast fail, 0 overflow". **118 vi phạm AA chưa từng được triage.**
3. **Số perf của v13 đo khi stack đang động.** `performance.md` §11-28 ghi backend bị **rebuild+restart
   2 lần** trong cửa sổ perf; `perf-before.json` ghi rõ "rebuilt+restarted by a concurrent audit phase".
   Cùng endpoint cho median **47.1 ms** (có tranh chấp) vs **14.4 ms** (sạch) — lệch >3×.

Thêm: `ui-sweep.js`/`perf-probe.js`/`focused-probe.js` **không mang chuỗi phiên bản nào** nhưng ghi vào
`audit-v13-full/evidence`; `_cleanup.sql` **hardcode `'2026-09-21'`** → chạy lại ngày khác sẽ rò rỉ hàng.

### Ứng viên finding đã đo được (đưa vào Phase 1/9 để xác minh)

| Ứng viên | Số đo | Cách xử lý |
|---|---|---|
| **Rác tài khoản audit cũ** | `users` = **72**, trong đó **45** là `auditv4.*/auditv5.*@test.local` tạo 2026-09-03/04 | Đo tác động (leaderboard: **0 entry rác** → không phải bug người dùng, là **nợ dữ liệu**); nêu finding + đề xuất, **KHÔNG tự xoá** (cần quyết định owner) |
| **109 hàng `payment_transactions` PENDING không `transaction_id`** | created 2026-07-24 → 2026-09-12 | Điều tra nguồn (checkout bỏ dở vs rác sweep); nêu finding + đề xuất, không tự xoá |
| **`frontend/node_modules` host rỗng** | chỉ có `.vite-temp` | Tiền đề Phase 0 |
| **`tmp/` KHÔNG được `.gitignore`** | grep `.gitignore` không có `tmp` | Đưa vào cleanup manifest |

---

## Nguyên tắc bất di bất dịch (constitution P1–P8 + AGENTS.md)

- **P1** baseline xanh: đếm test **từ run log**, không đếm `target/surefire-reports` XML (từng ảo +8).
- **P2** kiến trúc phân lớp: Controller → Service → Repository; Vue `<script setup>` + `services/*.js`.
- **P3** schema do Hibernate `ddl-auto=update`; Flyway disabled; đổi schema = SQL trực tiếp + entity + verify container.
- **P4** AI local: Ollama `qwen2.5:1.5b`/`3b`, Whisper :9002; không thêm cloud AI trả phí.
- **P5** hiệu năng **đo trước/sau**; từ chối tối ưu khi số không biện minh (ghi lý do).
- **P6** chỉ một font **Be Vietnam Pro**; cấm Outfit/Plus Jakarta.
- **P7** UI tiếng Việt; WCAG 2.2 AA bắt buộc (focus-visible, skip-link, contrast, `prefers-reduced-motion`).
- **P8** mỗi thay đổi có **bằng chứng runtime** (HTTP thật :8080 + UI thật qua MCP + hàng DB thật).

### Bẫy thao tác (AGENTS.md — bắt buộc tuân thủ)

- Flush `rate_limit:*` trong redis **trước mỗi batch** HTTP (global 100/phút/IP) — không thì harness tự tạo 429 giả.
- `SET QUOTED_IDENTIFIER ON` cho **mọi** batch DELETE; **quét `Msg \d+`** (sqlcmd exit 0 kể cả khi batch lỗi).
- Seed **cả** `localStorage.token` **và** `localStorage.user`; assert `page.url()` sau điều hướng (`HARNESS-TOKEN-ONLY-SEED`).
- Contrast probe phải **composite alpha bottom-up**.
- `alt=""` hợp lệ → dùng `!el.hasAttribute('alt')`.
- Tap-target AA = **24px** (24–44 chỉ là AAA).
- Overflow = `scrollWidth − clientWidth` (Chromium chừa ~15px scrollbar → 13–15px KHÔNG phải overflow).
- `/premium/checkout` tạo row payment thật lúc mount → dọn cùng run + re-assert parity.
- Timezone: mọi `datetime2` = **naive giờ VN (+07)**; `SYSDATETIME()` trong sqlcmd lệch −7h.
- Backup trước DML hàng loạt; xoá theo **ID liệt kê**, cấm `LIKE 'zz%'`.
- `speaking_submissions` không có API xoá → SQL DELETE + xoá object MinIO.

---

## Phases & Tasks

Artifact nằm dưới `.specify/specs/audit-v14-full/evidence/`. Acceptance = file máy-đọc được tồn tại
**và** một lệnh chạy lại tái tạo nó. `[gate]` = phase sau bị chặn tới khi đạt.

### Phase 0 — Freeze, khôi phục harness, baseline `[gate]`

| # | Việc | Lệnh / harness | Artifact | Tiêu chí |
|---|---|---|---|---|
| T0.1 | Freeze checkpoint + tạo nhánh `audit-v14-full` | `git rev-parse HEAD`, `git status --short`, `git switch -c audit-v14-full` | `baseline.md` | tree sạch; branch ghi lại |
| T0.2 | Tạo artifact home + seed pipeline docs | Write `constitution/spec/clarify/checklist/plan/tasks/findings.md` | thư mục tồn tại | đủ 7 file trước khi đọc code |
| T0.3 | **Khôi phục harness** `sweep/v13/*` → `sweep/v14/` | `git show a627569:sweep/v13/<f> > sweep/v14/<f>` cho 16 file | `harness-restore.md` | 16 file; sha256 khớp `a627569` |
| T0.4 | **Đổi namespace + tham số hoá output** | sed `AUDIT-V13`→`AUDIT-V14`; `audit-v13-full`→`audit-v14-full`; inject `--out` argv | `harness-restore.md` | `grep -r 'AUDIT-V13\|audit-v13-full' sweep/v14` = 0 |
| T0.5 | **Tham số hoá ngày cleanup** (v13 hardcode 2026-09-21) | thay literal bằng `VN_RUN_DATE` | `_cleanup.sql`, `_deep-cleanup.sql` | `grep "2026-09" sweep/v14` = 0 |
| T0.6 | Backend suite, đếm **từ run log** | `cmd /c "mvnw.cmd -o test"` | `baseline-backend.log` | tests/fail/error/skip; KHÔNG từ surefire XML |
| T0.7 | Frontend suite + build (**phải `npm install` trước** — host `node_modules` rỗng) | `cd frontend; npm install; npx vitest run; npx vite build` | `baseline-frontend.log`, `baseline-build.log` | test count/files; entry kB+gzip |
| T0.8 | Container inventory + staleness | `docker ps`; epoch `.java`/`.vue` vs `StartedAt` | `containers.md` | 8 up; không stale (hoặc rebuild + ghi lại) |
| T0.9 | Parity + endpoint inventory | `python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql`; `node sweep/v14/api-inventory.js` | `parity-before.txt`, `endpoint-inventory.json` | parity = baseline; inventory tự nhất quán |
| T0.10 | **Falsify-first ledger** — mỗi finding v13 là giả thuyết để test lại | Write | `prior-hypotheses.md` | mỗi F-13-* có hypothesis / cách test lại / verdict v14 để trống |

**Gate:** không đóng finding, không sửa code trước khi T0.6–T0.7 có số thật.

### Phase 1 — Audit DB trong Docker (read-only, chứng minh hiệu lực constraint)

| # | Việc | Lệnh | Artifact | Tiêu chí |
|---|---|---|---|---|
| T1.1 | Parity re-measure + giải thích mọi delta | `sqlrun.py p16-parity.sql` | `db-audit.md` §parity | delta vs T0.9 = 0 hoặc do writer có tên |
| T1.2 | **Hiệu lực constraint trong DB scratch** | tạo `english_learning_v14probe`; INSERT vi phạm (FK/UQ/CHECK/filtered index) kỳ vọng `Msg 547/2627/2601/515`; DROP | `db-audit.md` §constraints | ≥1 block mỗi họ constraint; scratch DB đã DROP (assert absent) |
| T1.3 | Orphan scan, NULL FK in **dòng riêng** | SQL | `db-audit.md` §orphans | orphan=0; `null_fk=<n>` tách riêng |
| T1.4 | Re-verify F-13-07 (study_policy CHECK) + F-13-08 (cột chết) | `sys.check_constraints`; grep entity | `db-audit.md` | khớp thực tế; nếu bị revert → finding mới |
| T1.5 | Index usage/fragmentation hot read | `sys.indexes` + `dm_db_index_usage_stats` | `db-audit.md` §index | test lại F-13-10 kèm uptime |
| T1.6 | Timezone (F-13-09): đo lại defaults + đếm writer | SQL + `grep LocalDateTime.now()` | `db-audit.md` §tz | ghi giá trị + số writer; không dùng SYSDATETIME làm mốc |
| T1.7 | Row-level sanity; đo lại LISTENING thiếu `audio_url` | SQL | `db-audit.md` | số đo phiên này |
| T1.8 | Quét `Msg \d+` mọi batch | harness helper | `db-audit.md` | `Msg` count = 0 hoặc giải thích từng cái |

### Phase 2 — API sweep toàn bộ (mọi entry inventory có status) `[gate]`

| # | Việc | Lệnh / harness | Artifact | Tiêu chí |
|---|---|---|---|---|
| T2.1 | Base sweep, cả 148 row, đúng role, kiểm **hợp đồng** (field+type) | `node sweep/v14/api-sweep.js` | `api-sweep.json` | per-row join (T2.2) |
| T2.2 | **BẢNG RECONCILE MỚI** 148 row → 148 status | `sweep/v14/reconcile-inventory.js` | `endpoint-reconciliation.md` | `unaccounted = 0`; exit != 0 nếu khác |
| T2.3 | Deep probe: hợp đồng + hành vi đo được cho 6 chức năng | `node sweep/v14/deep-probe.js` | `deep-probe.json` | mỗi chức năng ≥1 assert hợp đồng |
| T2.4 | Role matrix: mọi path admin × 3 role, **cả 2 chiều** | in-sweep | `api-sweep.json` §roles | 0 assert một chiều |
| T2.5 | Search/sort đo, không giả định | `node sweep/v14/search-sort.js` | `search-sort.json` | `?sort=` nêu rõ per endpoint |
| T2.6 | Re-assert guard F-13-01/12/13 **cả 2 chiều** | in deep-probe | `deep-probe.json` | từ chối bare-letter; chấp nhận `"A - Salad"` |
| T2.7 | AI: validate-only + BLOCKED cho `generate-async` | in-sweep | `api-sweep.json` | BLOCKED kèm lý do, không skip im |
| T2.8 | Payment biên giới: create-order + sai chữ ký + replay | in-sweep | `api-payment.md` | đường tiền thật = BLOCKED by design |
| T2.9 | Re-assert parity sau mọi write probe | `sqlrun.py` | `parity-after-api.txt` | bằng T0.9 |

**Gate:** `endpoint-reconciliation.md` cho `148/148`, `unaccounted=0`, **26 controller đều 100%**.

### Phase 3 — UI sweep bằng **cả hai** MCP

| # | Việc | Lệnh / harness | Artifact | Tiêu chí |
|---|---|---|---|---|
| T3.1 | Playwright driver: 39 route × 3 role, guard **2 chiều** + `page.url()` | `node sweep/v14/ui-sweep.js` | `ui-sweep.json` | 0 guardFails; URL cuối asserted |
| T3.2 | chrome-devtools MCP driver 2: snapshot, console, network, `lighthouse_audit`, `emulate` | MCP calls | `ui-chrome-devtools.md` | driver ghi rõ per số (không trộn) |
| T3.3 | **TRIAGE TAP-TARGET MỚI** real/decorative/probe-false-positive | `node sweep/v14/tap-target-triage.js` | `tap-targets.md` | mọi small target có verdict + selector + size |
| T3.4 | Contrast AA, **composite alpha bottom-up** | in ui-sweep | `ui-sweep.json` §contrast | 0 fail chưa giải thích |
| T3.5 | Responsive 360/768/1280/1440/1920; overflow = scrollWidth−clientWidth trừ scrollbar | in ui-sweep | `ui-sweep.json` §responsive | 0 overflow thật |
| T3.6 | Design-system conformance vs P6 + font `document.fonts.check` | in ui-sweep | `ui-sweep.json` §design | font true; token/border/shadow ghi lại |
| T3.7 | Heading order, label-in-name, colour-not-only, skip-link, focus-visible, **keyboard traversal, modal focus-trap** | in ui-sweep | `ui-sweep.json` §a11y | mỗi vi phạm có selector |
| T3.8 | Payment row dọn **trong run** + parity re-assert | `cleanupAuditPayments` + `dbParity` | `ui-sweep.json` | parse marker `AUDIT_CLEAN_TOTAL` |

### Phase 4 — E2E 3 tầng UI → API → DB (6 chức năng chính)

Mỗi flow: thao tác UI thật → bắt network call → đối chiếu API contract → **đọc hàng DB đổi** (assert **đẳng thức**, không chỉ "row đổi").

| # | Flow | Artifact | Tiêu chí |
|---|---|---|---|
| T4.1 | Lessons/Exercises: UI action → network call → API contract → DB row | `e2e-flows.md` | 3 tầng, hàng thật |
| T4.2 | **F-13-01 khép kín (re-proven, không kế thừa)**: MC thật render lựa chọn → grade/submit → `exercise_attempts` + `study_days` | `e2e-flows.md` + PNG | hàng DB đổi |
| T4.3 | Streak: snapshot 7 field typed; `currentStreak` **==** số hàng `study_days` | `e2e-flows.md` | assert đẳng thức |
| T4.4 | Login/Register: register→login→`/me`→hàng `users` | `e2e-flows.md` | id hàng captured |
| T4.5 | Search/Sort: `q=habitat` → id kết quả **==** id DB | `e2e-flows.md` | đẳng thức id |
| T4.6 | CRUD: create→read→update→delete, DB row 1→1→0 | `e2e-flows.md` | mọi bước 2 tầng |
| T4.7 | AI: validate cả 2 chiều; `generate-async` BLOCKED(lý do) | `e2e-flows.md` | BLOCKED nêu rõ |
| T4.8 | Premium checkout: create-order + sai chữ ký; cleanup + parity | `e2e-flows.md` | row dọn, parity bằng |

### Phase 5 — Hiệu năng trước/sau (P5: đo rồi mới biện minh)

| # | Việc | Lệnh | Artifact | Tiêu chí |
|---|---|---|---|---|
| T5.1 | Perf probe, median ≥5, flush bucket mỗi batch, **stack tĩnh** | `node sweep/v14/perf-probe.js` | `perf-before.json` | `backendStartedAt` ghi lại; **không restart** trong cửa sổ (assert `docker inspect` không đổi) |
| T5.2 | CLS probe | `node sweep/v14/cls-probe.js` | `cls-before.json` | 3 route |
| T5.3 | Lighthouse (hoặc ghi giới hạn host) | chrome-devtools MCP | `perf-lighthouse.json` | có category hoặc ghi giới hạn |
| T5.4 | Tối ưu **chỉ khi** số biện minh; ngược lại từ chối + lý do | — | `performance.md` | mọi hot endpoint có before/after hoặc refusal |
| T5.5 | Đo lại **cùng phương pháp** | `perf-probe.js` (after) | `perf-after.json` | method string khớp T5.1 |

### Phase 6 — Fix tận gốc + test hồi quy (TDD)

| # | Việc | Tiêu chí |
|---|---|---|
| T6.1 | Mỗi finding: bằng chứng fail → root-cause (đọc code + SQL) → fix → chạy lại **cùng probe** → ghi cả 2 số | `findings.md` before/after |
| T6.2 | Regression test **fail trước → pass sau** | test name trích per finding |
| T6.3 | Backend + frontend + build; đếm từ log | `backend-after.log`, `frontend-after.log` |
| T6.4 | **Review chéo đối kháng mọi fix** + mọi liên kết UI-API-DB mới (R9) | `review-<finding>.md` |
| T6.5 | Phân tích prompt-hole: claim của prompt vs thực tế đo | `prompt-fixes.md` |
| T6.6 | Doc-drift: README/CLAUDE.md vs thực tế đo | `doc-drift.md` |

### Phase 7 — Vòng 2, rộng hơn, **loop-until-dry**

| # | Việc | Tiêu chí |
|---|---|---|
| T7.1 | Case biên đối kháng (§5) trên **build cuối** | `round-2.md` |
| T7.2 | Chạy lại mọi probe Phase 1–5 trên build cuối | `round-2.md` |
| T7.3 | **Dừng khi 2 vòng liên tiếp không thêm finding mới** | `round-2.md` chứng minh 2 vòng dry |
| T7.4 | Adversarial verify: bác/xác nhận mỗi finding bằng **probe thứ 2 độc lập** | `adversarial-verify.json` |

### Phase 8 — Converge / Analyze / Báo cáo / Cleanup

| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T8.1 | `speckit-converge` | `converge.md` | mỗi R# của spec map tới evidence |
| T8.2 | `speckit-analyze` (read-only) | `analyze.md` | tự bắt lỗi đếm số |
| T8.3 | Quét secret/PII toàn bộ evidence trước khi giữ | `secret-scan.md` | 0 secret; chỉ WARN seed-password đã biết |
| T8.4 | `REPORT.md` (đã làm/chưa làm/fix cách nào/skill/giới hạn/**file đã xoá**) | `REPORT.md` | mọi số trỏ artifact |
| T8.5 | **Cleanup manifest + thực thi** (§Cleanup) | `cleanup-manifest.md` | liệt kê đường dẫn xoá + giữ |

### Phase 9 — Đóng hạng mục OPEN cũ (đo lại, không giả định)

| # | Việc | Tiêu chí |
|---|---|---|
| T9.1 | Đo lại độc lập mọi OPEN/DEFERRED của v13 (F-13-02/07/08/09/10) | verdict điền vào `prior-hypotheses.md` |
| T9.2 | "FIXED" bị revert → finding mới; "OPEN" mà ổn → đóng kèm bằng chứng | ghi vào `findings.md` |

---

## v14 GẮT HƠN v13 ở đâu (delta đo được — yêu cầu "gắt gao, tỉ mỉ hơn")

| # | Điểm yếu v13 (kèm bằng chứng) | Cách v14 làm khác | Acceptance đo được |
|---|---|---|---|
| **D1** | **Inventory không reconcile**: 148 row vs 147 status → R2 không chứng minh được | `sweep/v14/reconcile-inventory.js` join inventory ↔ status **theo method+path**, in ra diff | `unaccounted=0`, exit!=0 nếu khác; artifact `endpoint-reconciliation.md` |
| **D2** | **Tap-target AA đo rồi bỏ**: `smallTargets:118`, vắng trong `REPORT.md` §1 | Triage từng small target: real/decorative/probe-false-positive, kèm selector+size+ratio | `tap-targets.md` liệt kê N/N verdict; mỗi cái real là finding |
| **D3** | **Perf đo khi stack động**: backend restart 2 lần trong cửa sổ perf | Freeze stack: assert `StartedAt` không đổi suốt run; abort+retry nếu đổi | `perf-before.json.stackStable=true`; StartedAt before==after |
| **D4** | **Nhãn phiên bản harness trôi**: 3 file không mang version string; v12 inventory ghi đè evidence v12 (F-13-06/17/21) | Mọi writer nhận `--out`; guard `sweep/v14/assert-namespace.js` grep namespace lạ | `grep -r 'audit-v1[0-3]-full\|AUDIT-V1[0-3]' sweep/v14` = 0; guard exit!=0 nếu hit |
| **D5** | **Probe false-negative bị báo thành finding** (F-13-10; F-13-14 ERR_EMPTY_RESPONSE tự gây) | Luật: mỗi finding cần **probe thứ 2 độc lập** bác-hoặc-xác-nhận trước khi đặt status | `adversarial-verify.json`: 100% finding có 2 probe |
| **D6** | **Kết luận cũ được kế thừa làm nguồn** | Falsify-first ledger (`prior-hypotheses.md`) viết ở Phase 0; mọi F-13-* re-test | mỗi finding cũ có verdict v14 |
| **D7** | **A11y scope hẹp** (alt/tap/contrast/heading) | Thêm keyboard traversal, modal focus-trap, aria-live async, form error association, reduced-motion | `ui-sweep.json` §a11y có thêm key; vi phạm liệt kê |
| **D8** | **Cleanup SQL hardcode ngày** (2026-09-21) → chạy ngày khác rò rỉ | Tham số hoá `VN_RUN_DATE`; assert leftover `AUDIT_*` = 0 | `grep "2026-09" sweep/v14` = 0; leftover marker 0 |
| **D9** | **UI sweep chạy khi code đang sửa** (v13 REPORT §3 tự nhận) | UI/perf sweep chỉ chạy trên **build hash đóng băng**; ghi hash | `ui-sweep.json.buildHash` có và khớp REPORT |
| **D10** | **Chỉ 143/148 status, không chứng minh coverage theo controller** | Bảng coverage 26 controller (rows/statuses) | `endpoint-reconciliation.md` cho 26 controller đều 100% |
| **D11** | **Nợ dữ liệu tích tụ qua nhiều kỳ chưa từng đo** | Đo 45 user audit cũ + 109 payment PENDING; phân loại | finding kèm số + đề xuất |
| **D12** | **Không có quy trình dọn rác** | Cleanup manifest liệt kê từng file xoá (ràng buộc mới) | `cleanup-manifest.md` + REPORT liệt kê verbatim |

---

## Cleanup protocol (ràng buộc mới của người dùng)

### Nhóm artifact phiên này sinh ra

| Nhóm | Đường dẫn | Bản chất | Giữ / Xoá |
|---|---|---|---|
| Pipeline docs | `.specify/specs/audit-v14-full/*.md` | deliverable | **GIỮ** (tracked, như v13) |
| Evidence | `.specify/specs/audit-v14-full/evidence/**` | deliverable | **GIỮ**; thêm whitelist `.gitignore` (như v13 L46-48) |
| Shots | `evidence/shots/**` | bằng chứng | **GIỮ** ảnh trỏ từ findings; xoá ảnh thừa (liệt kê) |
| Harness khôi phục | `sweep/v14/**` (16 file + script mới) | tooling sinh trong run | **XOÁ cuối phiên + liệt kê** (quyết định người dùng) |
| Harness scratch | `sweep/v14/_cleanup.sql`, `_deep-cleanup.sql` | scratch | **XOÁ** (cùng sweep/v14) |
| Logs | `*.log` (root / `target/` / `frontend/`) | scratch, ignored | **XOÁ**; chỉ giữ `evidence/*.log` |
| Temp dir | `tmp/` (chưa có; sẽ tạo) | scratch, **KHÔNG ignored** | **XOÁ cả thư mục** |
| Browser/MCP cache | `.playwright-mcp/`, `.playwright/`, `.ua/`, `.ua-screens/`, `.mimosa/` | scratch, ignored | **XOÁ** |
| Uploads | `uploads/**` (7 file hiện có, untracked) | runtime data | **KHÔNG xoá file có sẵn**; chỉ xoá file run này tạo (theo tên) |
| Build output | `target/`, `frontend/dist/` | ignored | **XOÁ** (tái tạo được) |
| Network dumps | `*.network-request`, `*.network-response` | ignored | **XOÁ** |
| DB rows | `AUDIT-V14-*` decks/lessons/vocab, payment probe, speaking rows | data | xoá **trong run**; assert leftover=0 |
| MinIO objects | `speaking-uploads/<key>` do run tạo | data | xoá **trong run** |
| Scratch DB | `english_learning_v14probe` | scratch | **DROP trong run**, assert absent |

### Quy trình xoá an toàn

1. **Trước hết:** `git status --short` + `git status --ignored --short` → snapshot `evidence/pre-cleanup-tree.txt`.
2. Lập `evidence/cleanup-manifest.md` với 3 danh sách: **GIỮ** (deliverable), **XOÁ** (đường dẫn tuyệt đối liệt kê, KHÔNG glob gốc), **KHÔNG ĐỤNG** (`uploads/*` có sẵn).
3. Xoá **chỉ** đường dẫn đã liệt kê, theo thứ tự:
   a. DB trong run trước (`_cleanup.sql` + `_deep-cleanup.sql`, đã tham số hoá) → assert `AUDIT-V14-*` leftover = 0.
   b. MinIO trong run → assert object mất.
   c. `DROP DATABASE english_learning_v14probe` → assert absent.
   d. Scratch filesystem: `tmp/`, `*.log` không thuộc evidence, `.playwright-mcp/`, `.playwright/`, `.ua*/`, `.mimosa/`, `target/`, `frontend/dist/`, `*.network-*`, `sweep/v14/`.
4. **Verify:** `git status --short` (chỉ deliverable dự định) + `git status --ignored --short` (scratch đã mất).
5. **Báo cáo cuối liệt kê verbatim từng đường dẫn đã xoá** (lấy từ `cleanup-manifest.md`).

**Luật an toàn:** không bao giờ `rm -rf` glob gốc; không xoá cả `uploads/`; không xoá `.specify/specs/audit-v8..v13`; không đụng `.env*` backup; backup-before-DML theo AGENTS.md nếu có bulk DML.

---

## Ma trận kịch bản kiểm thử (~72 case)

Vai: **anon / student / admin**. DB = assert DB; UI = assert UI.

| Nhóm | Case | Kỳ vọng |
|---|---|---|
| **A. Auth (A1–A10)** | login happy / sai mk / email sai định dạng / lockout 5 lần / JWT hết hạn 900s→/me / register happy / register trùng / `/me` shape / avatar real media / avatar bytes giả | 200+token / 401 / 400 / 429 rồi 200 / 401 / 200-201+row / 400-409 / 200 typed / 200+Cloudinary URL / 500·400 (bất đối xứng đã ghi) |
| **B. Lessons (L1–L5)** | list / detail / draft guard / draft admin / structure draft guard F126 | 200 / 200 / 404 / 200 / 404-403 |
| **C. Exercises (E1–E8)** | list / ẩn đáp án anon / admin thấy / grade anon / grade student / submit / MC chữ cái→400 / `"A - Salad"`→200 / LISTENING letter-only→ô text / legacy options NULL→trả lời được / `POST /api/vocabulary` / IDOR deck private | đúng guard F-13-01/12/13; 200/401/403/404 đúng |
| **D. Streak (S1–S7)** | 7 field typed / anon / login≠ngày học / hoạt động→+1 / 2 hoạt động 1 ngày / missed / cutover | 200 / 401 / `studiedToday` false→true, streak 0→1, không double, reset |
| **E. Search/Sort (Q1–Q5)** | vocab ≥2 / 1 ký tự / dictionary / admin q / `?sort=` | 200 / 200+`[]` / 200 / 200 / ghi rõ ignored |
| **F. AI (AI1–AI8)** | gen anon / gen student / save no-deck / save with-deck / admin status anon / generate-async / TTS :8001 / Whisper :9002 | 401 / 200 / 400 F147 / word trong deck / 401 / BLOCKED(GPU) ghi rõ / reachable documented |
| **G. Games/SRS/Flashcard/Progress/Leaderboard (G1–G8)** | game session quiz / 6 mode / submit blank / answers sai kiểu / SRS due (F-13-15 batch) / quality −1·6·999 / cap 365 / leaderboard anon | 200 shape / 200 / 400 / 400 / 200 + query-stats delta / 400 / capped / 200 |
| **H. Payment (H1–H4)** | create-order / webhook sai chữ ký / replay / webhook hợp lệ | 200+row dọn / rejected / idempotent / BLOCKED (tiền thật) |
| **I. Authz matrix (I1–I3)** | mọi path admin × anon/student/admin | 401 / 403 / 200 |
| **J. UI/A11y/Design (J1–J12)** | 39 route×3 role guard 2 chiều / `page.url()` / console-page-network 0 / contrast composite / tap-target triage / alt hasAttribute / heading order / keyboard traversal / modal focus-trap / reduced-motion / responsive 5 width / font `document.fonts.check` | 0 guardFails / no silent bounce / 0 UI-caused / 0 fail / N/N verdict / 0 real missing / no skip / reachable / trapped / honoured / 0 overflow / true |
| **K. DB (K1–K5)** | constraint effectiveness / orphan / parity / Msg scan / timezone writers | Msg 547·2627·2601·515 / 0, NULL FK riêng / baseline / 0 / số ghi lại |
| **L. Adversarial (L1–L8)** | pagination size=100000 / JSON hỏng / XSS `<script>` trong q / open-redirect `//evil.com` / midnight streak / burst đúng ngưỡng bucket / Unicode-RTL-chuỗi dài / concurrent same-user writes | cap 100 / 400 / không phản chiếu raw / ở lại site / đúng ngày / 200×N rồi 429 / không crash / không hỏng |

---

## Xử lý "lỗ hổng của prompt" (prompt holes)

Nguyên tắc: prompt sai về **sản phẩm** → sửa **prompt**; có hậu quả **thực tế** (a11y) → sửa **code** kèm số đo.

| # | Lỗ hổng | Xử lý trong v14 |
|---|---|---|
| H1 | "slate-800 on off-white = AAA" nhưng bảo dùng vivid cho "emphasized words" → trượt AA | Đo lại 0 vi phạm bằng probe composite; giữ tầng `*-ink`/`*-strong` |
| H2 | Prompt ghi Outfit/Plus Jakarta | Đã thay bằng Be Vietnam Pro; verify grep = 0 |
| H3 | "Lucide **React**" | Dự án Vue dùng `lucide-vue-next`; sửa prompt |
| H4 | "shadow 4px không ngoại lệ mobile" mâu thuẫn "mobile 2px" | Sửa prompt; verify mobile shadow |
| H5 | Không dark mode / `prefers-color-scheme` | Ghi biên giới có chủ ý |
| H6–H8 | "hero image blob" (không có ảnh) / "pricing 3 gói" (thật 2) / "features grid 3" (thật 4) | Sửa prompt; verify layout thật |
| H9 | "chạy **toàn bộ** API" bất khả thi (webhook hợp lệ = tiền thật) | Ghi biên giới; BLOCKED(real-money) kèm lý do |
| H10 | "tối ưu hiệu năng" không nêu mục tiêu | Đo trước; từ chối tối ưu kèm số (P5) |
| H11 | yêu cầu 2 MCP nhưng bản trước dùng 1 MCP hình thức | v14 biến **cả hai** thành driver hạng nhất |
| H12 | "thay toàn bộ font" là việc đã xong | v14 **verify**, không làm lại |
| **H13 (mới)** | "chạy test lặp toàn bộ chức năng" không nêu tiêu chí dừng | v14 ghi rõ **loop-until-dry** (2 vòng liên tiếp 0 lỗi mới) |
| **H14 (mới)** | "xoá file rác" không nêu cái gì là rác | v14 định nghĩa **Cleanup protocol** + manifest |
| **H15 (mới)** | "verify giao diện đã đồng bộ với prompt hay chưa" không nêu oracle đo | v14 dùng token live + `document.fonts.check` + contrast composite + tap-target triage làm oracle |

---

## Rủi ro & giảm thiểu

| # | Rủi ro | Mức | Giảm thiểu | Tín hiệu phát hiện |
|---|---|---|---|---|
| R1 | **Concurrent writer** (sweep khi process khác đang ghi) | High/High | Freeze stack khi perf (D3); ghi StartedAt before/after; single-writer cho API write | `stackStable` flag; restart = abort+retry |
| R2 | **429 tự gây** (global 100/phút/IP) | High/Med | flush `rate_limit:*` mỗi batch; chạy serial; prefix bucket đúng (`:auth`20, `:mail`5, `:global`100, `:ai`10, `:upload`15, `:order`10) | 429 khi bucket đã flush = finding thật |
| R3 | **Container stale** | Med/High | T0.8; rebuild + ghi nếu `.java`/`.vue` mới hơn | `find -newermt` vs StartedAt |
| R4 | **Parity trôi** do harness ghi | High/Med | Dọn trong run + `dbParity()` assert với marker `AUDIT_CLEAN_TOTAL` (không "số cuối"); re-assert mọi phase ghi | parity != baseline → dừng, dọn, giải thích |
| R5 | **Probe bug báo thành finding** | High/Med | Probe thứ 2 độc lập per finding (D5); A/B harness nghi sai trước khi tuyên bố sweep vô hiệu | `adversarial-verify.json` bất đồng |
| R6 | **Xoá nhầm thứ cần** | Low/High | Manifest liệt kê; KEEP/UNTOUCHED; không xoá cả `uploads/` hay spec cũ | review `git status` sau dọn |
| R7 | **Lộ secret/PII vào evidence** | Med/High | Quét toàn evidence trước khi giữ (T8.3); redact sa password trong log; không copy `.bak` (36MB PII) | `secret-scan.md` |
| R8 | **Inventory/sweep mismatch** (khoảng trống D1) | High/Med | Script reconcile; exit!=0 khi unaccounted>0 | `endpoint-reconciliation.md` |
| R9 | **JWT hết hạn giữa sweep** (TTL 900s) | Med/Low | Harness refresh token; sweep dài re-login | bão 401 ở vùng xanh |
| R10 | **Tranh chấp GPU/Ollama** đọc nhầm thành regression | Med/Low | Tách latency AI; `OLLAMA_MAX_LOADED_MODELS=1`; ghi model-swap | outlier chỉ ở AI |
| R11 | **UI sweep trên build trung gian** (lỗi v13 tự nhận) | Med/High | Freeze build hash trước UI/perf; ghi hash vào evidence (D9) | `buildHash` mismatch |
| R12 | **Contrast false-positive** (alpha compositing) | Med/Med | Composite bottom-up; `alt=""` hợp lệ (`hasAttribute`); tap-target AA=24px | cross-check phương pháp 2 |
| R13 | **sqlcmd exit 0 khi batch lỗi** | Med/High | `SET QUOTED_IDENTIFIER ON` mọi batch DELETE; quét `Msg \d+` | có `Msg` → coi là fail |
| R14 | **Scope creep** (fix cái prompt không hỏi) | Med/High | Complexity budget constitution; chỉ fix lỗi đo được; prompt-hole → sửa prompt | review mỗi fix vs spec |

---

## Definition of Done

- Backend + frontend suite xanh trên build cuối, số đếm **từ run log**.
- Build frontend không tăng quá baseline mà không ghi lý do.
- `endpoint-reconciliation.md`: **148/148** status, `unaccounted=0`, **26 controller đều 100%**.
- Role check **2 chiều** cho mọi path admin.
- UI sweep bằng **cả 2 MCP**: 39 route × 3 role, guard 2 chiều, 5 width, a11y **gồm tap-target triage**, contrast composite, console/network sạch.
- E2E 3 tầng cho 6 chức năng; mỗi cái có assert **hàng DB thật** (đẳng thức, không chỉ "row đổi").
- Design-system + font verify bằng `document.fonts.check` + token live.
- Mọi finding có ID, severity, evidence, root cause, fix hoặc disposition, regression test, status cuối; **probe thứ 2 độc lập** per finding.
- **Cleanup manifest** đầy đủ; `git status` sạch; parity = baseline; 0 rác.
- REPORT.md trung thực: đã làm / chưa làm / đã fix & cách fix / skill đã nạp / **file đã xoá** / giới hạn.

---

## Quyết định đã chốt (mặc định cho các câu còn lại)

| # | Câu hỏi | Chốt |
|---|---|---|
| Q1 | Harness lifecycle | **Xoá cuối phiên + liệt kê** (người dùng) |
| Q2 | Evidence commit | **Commit** (như v13) |
| Q3 | Fix scope OPEN cũ | **Chỉ lỗi đo được**; A2/A3 + F-13-09 giữ cho chủ sản phẩm |
| Q6 | Branch | **`audit-v14-full`** từ `main` |
| Q4 | GPU cho AI E2E | Thử `generate-async` thật nếu GPU rảnh; nếu không → **BLOCKED-time** ghi rõ (không skip im) |
| Q5 | Payment biên giới | Giữ như v13: create-order + sai chữ ký + replay; dọn trong run |
| Q7 | `tmp/` | Dùng `tmp/v14/` ở repo root (KHÔNG ignored → phải xoá + liệt kê) |
| Q8 | Lighthouse | Thử lấy category Performance; nếu host không phát → ghi giới hạn (không cố bằng mọi giá) |
| Q9 | Concurrency | UI/perf **serial** trên build đóng băng, không chạy workflow song song ghi code |
| Q10 | Ngôn ngữ report | **Tiếng Việt + thuật ngữ kỹ thuật tiếng Anh** (P7, như v13) |

---

## Kiểm chứng end-to-end (cách chạy lại)

```bash
# Backend
cmd /c "mvnw.cmd -o test"
# Frontend (npm install trước vì host node_modules rỗng)
cd frontend && npm install && npx vitest run && npx vite build
# API
node sweep/v14/api-sweep.js && node sweep/v14/deep-probe.js && node sweep/v14/reconcile-inventory.js
# UI (Playwright driver)
node sweep/v14/ui-sweep.js && node sweep/v14/tap-target-triage.js
# Perf (stack phải tĩnh)
node sweep/v14/perf-probe.js
# DB parity
python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql
# Rebuild container sau khi sửa backend
docker compose up -d --build backend
```

---

## Skill / plugin dự kiến nạp

| Phase | Skill / plugin | Dùng để làm gì |
|---|---|---|
| Pipeline | `speckit-constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze` | Pipeline bắt buộc |
| Nền | `superpowers:using-superpowers` | Luật: nạp skill trước khi hành động |
| Clarify | `superpowers:brainstorming` | Chốt phạm vi + câu hỏi với người dùng |
| 0–1 | `java-springboot`, `java-coding-standards` | Đọc backend, xác nhận gốc constraint/query |
| 2 | `addyosmani-api-and-interface-design`, `java-docs`, `claude-security:scan` | Kiểm hợp đồng API + quét bảo mật |
| 3 | `accessibility` (WCAG 2.2), `frontend-design`, `addyosmani-browser-testing-with-devtools`, `addyosmani-frontend-ui-engineering` | a11y, tap-target triage, design conformance |
| MCP | chrome-devtools MCP + Playwright MCP | Driver 1 + 2 |
| 4–6 | `superpowers:systematic-debugging`, `superpowers:test-driven-development`, `superpowers:verification-before-completion`, `superpowers:requesting-code-review`, `superpowers:receiving-code-review`, `generate-tests`, `addyosmani-code-review-and-quality` | Truy gốc, TDD, review chéo |
| 5 | `addyosmani-performance-optimization` | Đo rồi mới tối ưu (P5) |
| 7 | `superpowers:dispatching-parallel-agents`, `workflow-authoring` + `Workflow` tool | Vòng 2 fan-out + adversarial verify |
| 8 | `addyosmani-documentation-and-adrs`, `claude-md-management` | Report + doc-drift |
| Prompt | `prompt-master` | Phân tích + sửa lỗ hổng prompt |
| Đọc hiểu | `understand` (knowledge graph) | Bản đồ kiến trúc trước khi sửa liên kết |
