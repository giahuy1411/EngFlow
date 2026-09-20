# audit-v12-full — Kế hoạch kiểm thử & audit toàn diện EngFlow

**Ngày lập:** 2026-09-21 (+07) · **Nhánh:** `audit-streak-review` · **Checkpoint:** `30bc9b6` (tree sạch)
**Tiền nhiệm:** `audit-v11-full` (đã commit) · **Artifact home:** `.specify/specs/audit-v12-full/`
**Remote:** `https://github.com/giahuy1411/EngFlow.git`

---

## Context — vì sao cần v12

Người dùng yêu cầu: quét toàn bộ codebase + CSDL, chạy **toàn bộ API**, tương tác **toàn bộ UI** tương ứng từng API,
kiểm sát 6 chức năng chính (Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI), audit DB trong
Docker, tối ưu hiệu năng, test UI/UX bằng **chrome-devtools-mcp + playwright-mcp**, verify UI khớp design-system
"Playful Geometric" (font **Be Vietnam Pro**), dùng skill/plugin trong `C:\Users\ASUS\.claude`, kiểm prompt có lỗ hổng
rồi sửa, theo workflow `constitution → specify → clarify → checklist → plan → tasks → implement → converge →
analyze → taskstoissues`, **chạy 2 vòng** (vòng 2 toàn diện hơn, sửa tận gốc), báo cáo trung thực.

**v11 đã làm nhiều, nhưng tôi đo lại và tìm ra khoảng trống thật, có bằng chứng:**

| Sự thật đo trong phiên này | Bằng chứng |
|---|---|
| Source có **131 annotation / 26 controller** (56 GET, 49 POST, 15 PUT, 2 PATCH, 9 DELETE) | `grep -rhoE "@(Get\|Post\|Put\|Patch\|Delete)Mapping" .../controller/` = 131 |
| `sweep/v11/api-sweep.js` chỉ probe **~27 endpoint** | 27 path `/api/...` trong file |
| **10 controller KHÔNG có probe nào** | Flashcard(2), Game(6), Leaderboard(1), LessonSubmission(3), LessonSnapshot(3), Progress(1), SpeakingPrompt(9), AdminExerciseSeed(1), LessonStructure(11), AdminAnswerBackfill(2) |
| Probe cũ v10 gọi endpoint **không tồn tại** | `deep-ai.js` gọi `/api/ai/grade-writing`, `/api/ai/translate` — grep source = 0 |
| **Font đã là Be Vietnam Pro 100%** | Đo DOM live: `distinctFonts = ["\"Be Vietnam Pro\", system-ui, sans-serif"]` (90 text node); source: `index.html`, `tailwind.config.js` (sans/heading/mono), `design-system.css --geo-font`; 0 hit Outfit/Plus Jakarta |
| **Design system đã triển khai đủ** | token `--geo-*`, shadow cứng, border 2px, hero sun, blob, dashed connector, pricing scale(1.1)+badge 15°, squiggle, marquee, arrow button |
| Backend container **KHÔNG stale** | probe live `POST /api/ai/save-vocab` trả `X-AI-Saved-Count: 1` (fix F145 có trong container); 0 file `.java` mới hơn container start |
| Parity live | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` |
| Baseline | `.p0-v11-backend-test-rerun.log`: `Tests run: 483, Failures: 0`; frontend 23 file `.test.js` |
| Router có **39 route** (inventory v8) / 30 path tuyệt đối | `frontend/src/router/index.js` |
| Deck public thật để probe game: **10006** | `SELECT deck_id,name,is_public FROM decks` |

→ v12 **không** "phát hiện lại" font/design-system (làm thế là bịa). v12 **đóng khoảng trống API coverage**, chạy UI
sweep sâu hơn bằng **cả hai** MCP, và kiểm chứng lại mọi con số bằng chính phiên này.

### Quyết định đã chốt (clarify — người dùng trả lời)
1. **Phạm vi:** làm lại từ đầu, toàn diện — phủ hết 131 endpoint + UI sweep 2 MCP + DB + perf + vòng 2.
2. **Doc drift:** sửa README/CLAUDE.md cho đúng thực tế, kèm bằng chứng đo được.
3. **`POST /api/vocabulary`:** điều tra kỹ (probe 3 role + đọc call-site + cân nhắc ngữ nghĩa sản phẩm) rồi mới quyết.
4. **Vòng 2:** chạy đến khi **2 vòng liên tiếp không tìm thêm lỗi mới** (loop-until-dry).
5. **taskstoissues:** tạo issue **thật** trên GitHub cho finding chưa fix.
6. **Commit:** commit fix + artifact (Conventional Commits, tách commit).
7. **AI vòng 2:** chạy pipeline AI sinh nội dung **đến xong**.

---

## Nguyên tắc bất di bất dịch (constitution P1–P8 + AGENTS.md)

- **P1** baseline xanh: đếm test **từ run log**, không đếm `target/surefire-reports` XML (từng ảo +8).
- **P3** schema do Hibernate `ddl-auto=update`; Flyway disabled; đổi schema = SQL trực tiếp + entity + verify trên container.
- **P5** hiệu năng phải **đo trước/sau**; từ chối tối ưu khi số không biện minh (ghi lý do).
- **P6** chỉ một font **Be Vietnam Pro**; cấm Outfit/Plus Jakarta.
- **P7** UI tiếng Việt; WCAG 2.2 AA bắt buộc (focus-visible, skip-link, contrast, `prefers-reduced-motion`).
- **P8** mỗi thay đổi phải có **bằng chứng runtime** (HTTP thật :8080 + UI thật qua MCP).
- **Bằng chứng:** mỗi khẳng định trỏ tới artifact; chưa verify = **BLOCKED/PARTIAL** kèm lý do; **lỗi của probe cũng là lỗi**;
  không "cap im lặng" — sweep có giới hạn phải ghi rõ đã bỏ gì.

### Bẫy thao tác (AGENTS.md — bắt buộc tuân thủ)
- Flush `rate_limit:*` **trước mỗi batch** HTTP (global 100/phút/IP) — không thì harness tự tạo 429 giả (F109).
- `SET QUOTED_IDENTIFIER ON` cho **mọi** batch DELETE; **quét `Msg \d+`** (sqlcmd exit 0 kể cả khi batch lỗi).
- Seed **cả** `localStorage.token` **và** `localStorage.user`; assert `page.url()` sau điều hướng (`HARNESS-TOKEN-ONLY-SEED`).
- Contrast probe phải **composite alpha bottom-up**.
- `alt=""` hợp lệ → dùng `!el.hasAttribute('alt')`.
- Tap-target AA = **24px** (24–44 chỉ là AAA).
- Overflow = `scrollWidth − clientWidth` (Chromium chừa ~15px scrollbar → 13–15px KHÔNG phải overflow).
- `/premium/checkout` tạo row payment thật lúc mount → loại khỏi route sweep hoặc dọn + re-assert parity.
- Backup trước DML hàng loạt; xoá theo **ID liệt kê**, cấm `LIKE 'zz%'`.
- Timezone: mọi `datetime2` = **naive giờ VN (+07)**; `SYSDATETIME()` trong sqlcmd lệch −7h.

---

## Phases & Tasks

### Phase 0 — Freeze & baseline (không ghi)
| # | Việc | Lệnh / harness | Artifact | Tiêu chí |
|---|---|---|---|---|
| T0.1 | Xác nhận checkpoint + tree sạch | `git status --short`, `git log -1` | `evidence/baseline.md` | 0 dirty |
| T0.2 | Tạo `.specify/specs/audit-v12-full/` + seed artifact pipeline | Write | thư mục tồn tại | có constitution/spec/clarify/checklist/plan/tasks/findings/evidence |
| T0.3 | Backend suite, đếm **từ run log** | `cmd /c "mvnw.cmd -o test"` | `evidence/baseline-backend.log` | ghi số + exit code |
| T0.4 | Frontend suite + build | `npx vitest run`, `npx vite build` | `baseline-frontend.log`, `baseline-build.log` | test count + entry kB/gzip |
| T0.5 | Container inventory + probe service + kiểm stale | `docker ps`, curl :8080/:5173/:9000/:9002/:8001; so epoch `.java` vs container start | `evidence/containers.md` | 8 container up, không stale |
| T0.6 | Parity live | `python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql` | `evidence/parity-before.txt` | `1471\|...\|5` |
| T0.7 | **Endpoint inventory tái dựng từ source** | script mới `sweep/v12/api-inventory.js` | `evidence/endpoint-inventory.json` | đếm = **131**, reconcile |

**Gate:** không đóng finding nào, không sửa code nào trước khi T0.3–T0.4 có số thật.

### Phase 1 — Audit DB trong Docker (read-only)
| # | Việc | Artifact |
|---|---|---|
| T1.1 | **Hiệu lực** constraint: FK/UQ/filtered index có **thực sự chặn** insert vi phạm — chứng minh trong DB scratch `english_learning_v12probe` rồi DROP | `evidence/db-audit.txt` |
| T1.2 | Orphan scan viết sao cho **NULL FK không bị đếm nhầm**; in NULL count **dòng riêng** | nt |
| T1.3 | Streak schema: `study_policy`(1 row, effective_from=2026-09-20), `study_days`, FK `fk_study_days_user` enabled, UQ `uq_study_days_user_date` | nt |
| T1.4 | Row-level sanity bảng UI đọc; **đo lại** LISTENING thiếu audio (đừng kế thừa con số 9) | nt |
| T1.5 | Index/fragmentation hot path; **từ chối tối ưu kèm số** | nt |
| T1.6 | Quét `Msg \d+` mọi batch | nt |

### Phase 2 — API sweep trên container thật (**đóng khoảng trống v11**)
Harness `sweep/v12/api-sweep.js` (dựng trên primitive của v11: flush bucket bằng `execFileSync`, **serial**).
**Kiểm hợp đồng (field + type), không chỉ 2xx.** Mỗi inventory entry phải có status.

| # | Vùng | Endpoint phải phủ | Artifact |
|---|---|---|---|
| T2.1 | Auth | register/login/forgot/reset/me/change-password/avatar; sai mk 401, email sai 400, JWT rác 401, **expiry 900s**, role 2 chiều. **Bỏ "refresh"** (không có endpoint) | `evidence/api-sweep.md` |
| T2.2 | Lessons/Exercises | list/detail/structure/content/grade/submit/attempts; tái chứng minh guard draft **F88/F89/F115/F126** 2 chiều | nt |
| T2.3 | **Streak** | `/snapshot` đủ **7 field** hợp đồng + type, `/current`, `/history` | nt |
| T2.4 | **Search/Sort** | vocab search (<2 ký tự → 200+`[]` theo thiết kế), dictionary, admin exercise `q=`; **đo lại `?sort=`** (v11: bị bỏ qua — phân loại product gap) | nt |
| T2.5 | CRUD qua API | deck (gửi `isPublic:false` tường minh), lesson, **`POST /api/vocabulary`** (mục C1); dọn cùng run | nt |
| T2.6 | **10 controller 0-coverage** | Game(6), Flashcard(2), Leaderboard(1), LessonSubmission(3), LessonSnapshot(3), Progress(1), SpeakingPrompt(9), AdminExerciseSeed(1), LessonStructure(11), AdminAnswerBackfill(2) — đúng role, đúng hợp đồng | nt |
| T2.7 | AI | generate-vocab, enrich-word, save-vocab (có/không deckId — F145); admin ai/status, generate-async (count là trần — F84), speaking-prompts ai-generate, video-attempts ai-grade, translate-transcript, fetch-youtube; TTS :8001, Whisper :9002 | `evidence/api-ai.md` |
| T2.8 | Payment | create-order (probe 1 lần + dọn), status, webhook **chữ ký sai → reject**, replay → idempotent | `evidence/api-payment.md` |
| T2.9 | Role matrix | mỗi path admin × 3 role, assert **cả 2 chiều** | nt |
| T2.10 | **Biên giới ghi rõ** | webhook chữ ký **hợp lệ** = giao dịch thật → **BLOCKED by design**; transfer ngân hàng thật → BLOCKED; restore/seed/backfill/generate-all chỉ trên row audit tự tạo hoặc dry-run | nt |
| T2.11 | Re-assert parity | | nt |

### Phase 3 — UI sweep bằng **cả hai** MCP (sâu hơn v11)
v11 dùng chrome-devtools gần như chỉ `list_pages`+1 `lighthouse_audit`; v12 dùng **thật** cả hai, ghi rõ driver nào ra số nào (không trộn — engine khác nhau).

| # | Việc | Artifact |
|---|---|---|
| T3.1 | **chrome-devtools MCP** làm driver hạng nhất: `new_page`/`navigate_page`, `take_snapshot`, `take_screenshot`, `list_console_messages`, `list_network_requests`+`get_network_request` (headers+bodies), `evaluate_script`, `performance_start_trace`+`performance_analyze_insight` (LCP/INP/CLS), `lighthouse_audit` (a11y/SEO/best-practices) ≥4 route, `emulate` (Slow 4G, reduced-motion) | `evidence/ui-chrome-devtools.md` |
| T3.2 | **playwright MCP** driver 2: `browser_navigate/snapshot/find/click/type/fill_form/select_option/press_key/evaluate`, `browser_network_requests/network_request`, `browser_console_messages`, `browser_resize` (5 width), `browser_take_screenshot`, `browser_emulate_media` | `evidence/ui-playwright.md` |
| T3.3 | Route×role: **39 route × 3 role**, guard assert **2 chiều** (stay vs bounce), assert `page.url()`; inventory tái dựng từ router | nt |
| T3.4 | Console/network health: 0 console error, 0 unhandled rejection, 0 API ≥400 **do UI gây ra** | nt |
| T3.5 | Responsive 360/768/1280/1440/1920, số thô, loại element có ancestor overflow | nt |
| T3.6 | A11y: `alt` qua `hasAttribute`, tap-target **2 ngưỡng**, focus-visible, skip-link, heading order (F135), label-in-name (F136), colour-not-only (F142) | nt |
| T3.7 | **Contrast AA** trang thật, **composite alpha bottom-up** (F138 lộ ra vì vòng 1 composite sai) | nt |
| T3.8 | **UI↔API cross-check** 6 chức năng: bắt network call UI phát ra, so URL+method+status+field với hợp đồng Phase 2 | nt |
| T3.9 | Dọn payment row sweep tạo + re-assert parity | nt |
| T3.10 | Design-system conformance vs prompt: token, border 2px, shadow cứng, hero sun, blob, dashed connector, pricing scale+badge, squiggle, marquee, arrow button; **verify font bằng `document.fonts.check`**, không chỉ khai báo | `evidence/ui-design.md` |

### Phase 4 — CRUD qua UI admin (end-to-end)
Tạo → thấy trong UI → sửa → thấy sửa → xoá → 404 → mất khỏi UI, cho **lesson + deck**; verify **cả UI lẫn API** mỗi bước; re-assert parity. → `evidence/ui-crud.md`

### Phase 5 — Sửa lỗi đo được (gate theo đo lường, TDD)
T5.1 mỗi lỗi: chụp bằng chứng fail → fix → chạy lại **cùng probe** → ghi cả 2 số.
T5.2 mỗi fix có **regression test fail trước** (F146+).
T5.3 chạy lại backend+frontend+build, đếm từ log. → `findings.md`

### Phase 6 — Hiệu năng, đo trước/sau (P5)
T6.1 `sweep/v12/perf-probe.js` (port v11): hot endpoint + path mới (leaderboard, progress, game session, SRS stats), **median ≥5**, flush bucket, consume body. T6.2 bundle + CWV/LCP qua chrome-devtools trace. T6.3 chỉ tối ưu khi số biện minh, **ghi lý do từ chối**. T6.4 đo lại cùng phương pháp. → `evidence/performance.md`

### Phase 7 — Vòng 2, rộng hơn, **loop-until-dry**
T7.1 suite trên build cuối. T7.2 lặp browser sweep, so Phase 3. T7.3 case biên đối kháng:
streak (login≠ngày học, 2 hoạt động cùng ngày = 1, missed day, cutover, midnight); SRS cap 365 (F106) + quality ngoài 0–5;
rate-limit bucket (burst tới đúng ngưỡng: `:auth`20, `:ai`10, `:upload`15, `:order`10, `:global`100); lockout 5 lần; JWT expiry;
pagination ngoài biên; JSON hỏng; register trùng; search rỗng. **Dừng khi 2 vòng liên tiếp không lỗi mới.**
T7.4 cross-artifact (speckit-analyze). T7.5 viết report. → `evidence/second-pass.md`

### Phase 8 — Đóng (đuôi pipeline)
T8.1 `speckit-analyze` (read-only) → `analyze.md`. T8.2 `speckit-converge` → bù việc chưa xong vào `tasks.md` rồi làm.
T8.3 **quét secret** trong evidence trước commit. T8.4 `speckit-taskstoissues` → **tạo issue thật trên GitHub** cho finding chưa fix.
T8.5 commit (Conventional Commits, author `giahuy1411`). → `REPORT.md`

---

## Ma trận kịch bản kiểm thử (~60 case)

| # | Vùng | Case | Actor | Expected |
|---|---|---|---|---|
| A1–A3 | Login | happy / sai mk / email sai định dạng | anon | 200+token / 401 / 400 |
| A4 | Login | JWT hết hạn (TTL 900s) → /me | student | 401 |
| A5–A6 | Register | happy / trùng email | anon | 200-201 / 400-409 |
| A7 | Auth | lockout 5 lần sai → chờ → đúng | anon | 429 rồi 200 |
| L1–L4 | Lessons | list / detail / draft guard / draft admin | anon·student·admin | 200 / 200 / 404 / 200 |
| L5 | Structure | draft guard (F126) | student | 404/403 |
| E1–E6 | Exercises | list / ẩn đáp án anon / admin thấy / grade anon / grade student / submit | anon·student·admin | 200 / không lộ `correctAnswer` / 200 / 401 / 200 / persist |
| S1–S2 | Streak | 7 field typed / anon | student·anon | 200 / 401 |
| S3–S7 | Streak | login≠ngày học / hoạt động → +1 / 2 hoạt động 1 ngày / missed / cutover | user mới | `studiedToday` false→true, streak 0→1, không double, reset, legacy đúng |
| Q1–Q5 | Search/Sort | vocab ≥2 / 1 ký tự / dictionary / admin q / `?sort=` | anon·admin | 200 / 200+`[]` / 200 hoặc 5xx-upstream (INFO) / 200 / ghi rõ ignored |
| C1–C4 | CRUD | deck create/read/update/delete / private hidden / lesson chain | student·anon·admin | 200→persist→204→404 / 400-404 / 200→404 |
| C5 | CRUD vocab | `POST /api/vocabulary` | student | xem mục C1 |
| G1–G4 | Games | session quiz / 5 mode / submit blank / answers sai kiểu | student | 200 shape / 200 / 400 / 400 |
| F1–F2 | Flashcards | review / status | student | 200 / 200 int |
| R1–R3 | SRS | due / review quality 3 / quality 9 | student | 200 list / 200 / 400 |
| P1–P2 | Progress/Dashboard | summary / stats | student·anon | 200 / 200·401 |
| B1 | Leaderboard | list | anon | 200 |
| AI1–AI6 | AI | gen anon / gen student / save no-deck / save with-deck / admin status anon / TTS+Whisper | anon·student | 401 / 200 / header not-linked (F145) / word trong deck / 401 / reachable |
| AU1 | Authz | mỗi path admin × 3 role | anon·student·admin | 401 / 403 / 200 |
| U1–U6 | UI | UI↔API / route×role / responsive / a11y / contrast / console | 3 role | khớp hợp đồng / guard 2 chiều / 0 overflow / 0 vi phạm / 0 fail AA / 0 error |
| D1–D3 | DB | constraint effect / orphan / parity | n/a | raise error / 0 / `1471\|...\|5` |
| PERF1 | Perf | hot paths median 5 | n/a | before/after ghi số |

---

## Xử lý "lỗ hổng của prompt" (prompt holes)

Kế thừa H1–H8 v11 (đã phân tích kèm bằng chứng) + mới:

| # | Lỗ hổng | Xử lý trong v12 |
|---|---|---|
| H1 | Dùng `secondary/tertiary/quaternary` cho "chữ nhấn mạnh" → trượt AA (2.65:1) | v11 đã tách token `*-ink`/`*-strong`; v12 **đo lại = 0 vi phạm** bằng probe composite đúng. Prompt sửa thành "hình khối/icon; dùng ink cho chữ" |
| H2 | Ghi "Lucide **React**" | Dự án Vue dùng `lucide-vue-next` → sửa **prompt** |
| H3 | Bắt dùng tên utility Tailwind literal nhưng lại cấm viết lại component | Sửa prompt; ghi nhận, không đụng code |
| H4 | "shadow 4px, không ngoại lệ mobile" mâu thuẫn "mobile 2px" | Sửa prompt; v12 verify mobile shadow |
| H5 | Không dark mode / không `prefers-color-scheme` | Ghi là biên giới có chủ ý |
| H6 | "hero image có blob mask" — không có ảnh | Sửa prompt; verify hero geometry |
| H7 | "pricing card giữa scale" — chỉ 2 gói | Sửa prompt; verify layout 2 gói |
| H8 | "Features grid 3" — Home có 4 | Sửa prompt; verify |
| **H9 (mới)** | "chạy **toàn bộ** API" bất khả thi: webhook chữ ký hợp lệ = tiền thật; create-order ghi row thật; restore/seed/backfill mutate nội dung | Ghi **biên giới**: probe đường an toàn (từ chối chữ ký / dry-run / row audit), dọn cùng run, re-assert parity; phần còn lại **BLOCKED(real-money/real-data)** kèm lý do. Sửa prompt, không phải lỗi sản phẩm |
| **H10 (mới)** | "tối ưu hiệu năng" không nêu mục tiêu | Đo trước; chỉ tối ưu khi số biện minh; ghi lý do từ chối (P5) |
| **H11 (mới)** | v11 dùng chrome-devtools MCP gần như chỉ `list_pages` → yêu cầu "test UI/UX bằng chrome-devtools-mcp" chưa được đáp ứng đủ | v12 biến chrome-devtools thành driver hạng nhất (T3.1) |
| **H12 (mới)** | "thay toàn bộ font" là **việc đã xong** | v12 **verify** (DOM `document.fonts.check` + source), không làm lại |

**Nguyên tắc:** prompt sai về sản phẩm → sửa **prompt**; có hậu quả thực tế (a11y) → sửa **code** kèm số đo.

---

## Hạng mục "verify-first rồi mới quyết"

| # | Ứng viên | Cách verify | Phân loại |
|---|---|---|---|
| C1 | `POST /api/vocabulary` cho mọi user tạo từ **GLOBAL** (PUT/DELETE yêu cầu ADMIN); caller: `VideoLesson.vue:473 saveWordToDeck` | probe 3 role live + đọc call-site + đo blast-radius (row có hiện với mọi người qua `/search`?) | **quyết định sản phẩm** — nêu finding kèm số, đề xuất fix, **không tự đổi authz** |
| C2 | README "223 tests" / "73 tests, 14 files" (thật 483 / 118·23 file) | so log thật T0.3/T0.4 | **safe fix doc** (đã chốt) |
| C3 | CLAUDE.md "Azure Speech SDK" (thật: Whisper :9002 + Ollama; 0 import) | grep source | **safe fix doc** |
| C4 | CLAUDE.md `@PremiumRequired` là cơ chế premium (thật: 0 usage, gating thủ công) | grep source | **safe fix doc** |
| C5 | CLAUDE.md "refresh via login" (thật: 0 endpoint refresh) | grep AuthController | **safe fix doc** |
| C6 | `pom.xml` khai báo Azure Speech SDK (~15MB) không dùng | grep import = 0 | **quyết định owner** — xoá cần rebuild + full test |
| C7 | `AdminController` + `LessonSnapshotController` không `@PreAuthorize` (chỉ URL rule) | probe student → 403 | **defense-in-depth**, verify rồi quyết |
| C8 | `/api/srs/*` (3 endpoint) **0 caller frontend** | grep `frontend/src` = 0; SrsService chỉ dùng nội bộ qua `FlashcardService` | **bề mặt mồ côi** — verify rồi phân loại |

---

## Rủi ro & giảm thiểu

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| "Chạy mọi API" → probe mutate data/tiền thật | Med/High | T2.10 biên giới; dry-run/từ chối chữ ký; dọn cùng run + parity; còn lại BLOCKED |
| 429 tự gây đọc nhầm là lỗi app | High/Med | flush bucket mỗi batch; sweep serial; quy đúng nguyên nhân |
| Seed chỉ token → trang admin render thành trang chủ mà báo PASS | High/High | `loginFull`+`mapUser`+`seedAuth`; assert `page.url()` |
| Parity trôi do harness (payment row) | High/Med | `cleanupAuditPayments`+`dbParity` cùng run; regex theo marker |
| `sqlcmd` fail batch mà exit 0 | Med/High | quét `Msg \d+`; `QUOTED_IDENTIFIER ON` |
| Probe bug → finding giả | High/Med | falsify bằng probe thứ 2 độc lập; A/B; ghi lỗi probe |
| Contrast probe composite sai | Med/Med | composite bottom-up (gốc F138) |
| Ollama cold-start đọc nhầm là regression | Med/Low | tách latency AI riêng; `OLLAMA_MAX_LOADED_MODELS=1` |
| Vượt phạm vi: "sửa" code theo prompt sai | Med/High | sửa prompt; code chỉ khi có lỗi đo được |
| Backend container stale | Low/High | kiểm T0.5 |
| Lộ secret/PII vào evidence | Low/High | quét T8.3 trước commit |

---

## Definition of Done
- Backend + frontend suite xanh trên build cuối, số đếm **từ run log**.
- Build frontend không tăng quá baseline mà không ghi lý do.
- Inventory reconcile = **131**, **mọi entry có status** (PASS/FAIL/BLOCKED/N/A) — 0 endpoint bỏ sót im lặng.
- 10 controller 0-coverage đều có ≥1 probe đúng role + hợp đồng.
- Role check **2 chiều** cho mọi path admin.
- UI sweep bằng **cả 2 MCP**: 39 route × 3 role, guard 2 chiều, 5 width, a11y, contrast composite, console/network sạch.
- UI↔API cross-check 6 chức năng: khớp URL+method+status+field.
- DB: constraint **có hiệu lực** thật, 0 orphan (NULL FK tách riêng), `Msg` = 0.
- Perf before/after (median ≥5), ghi lý do từ chối tối ưu.
- Vòng 2 tới khi cạn lỗi mới; mỗi fix có regression test.
- Doc-drift/dependency/authz có quyết định verify-first + nhãn safe-fix vs owner.
- Parity `1471|43735|72|127|28|15|4|126|14|5` trước/sau **mọi** run ghi.
- `REPORT.md`: đã làm / chưa làm / đã fix & cách fix / skill đã nạp / giới hạn.
- `analyze.md`, `converge.md`, issue GitHub cho finding chưa fix.

---

## Skill & plugin sẽ nạp (theo phase)
| Phase | Skill / plugin | Vì sao |
|---|---|---|
| Toàn pipeline | `speckit-constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze → taskstoissues` | Chuỗi bắt buộc người dùng nêu |
| Nền | `superpowers:using-superpowers`, `superpowers:brainstorming` | Luật nạp skill trước khi hành động; chốt phạm vi |
| Phase 0–1 | `java-springboot`, `java-coding-standards` | Đọc backend đúng; convention khi fix |
| Phase 2 | `addyosmani-api-and-interface-design`, `java-docs` | Kỷ luật hợp đồng API |
| Phase 2 security | `addyosmani-security-and-hardening`, `claude-security:scan`, `security-review` | Authz 2 chiều, rate-limit, upload XSS, AI output |
| Phase 3 browser | `chrome-devtools-mcp:chrome-devtools`, `:a11y-debugging`, `:debug-optimize-lcp`, `:cookie-debugging`, `:troubleshooting` | Driver 1 + Lighthouse + LCP |
| Phase 3 driver 2 | Playwright MCP (`browser_*`) | Engine độc lập, luồng tương tác |
| Phase 3 a11y/design | `accessibility`, `frontend-design` | WCAG 2.2; conformance design system |
| Phase 3 method | `addyosmani-browser-testing-with-devtools`, `addyosmani-frontend-ui-engineering` | Phương pháp bằng chứng runtime |
| Phase 5 | `superpowers:systematic-debugging`, `:test-driven-development`, `:verification-before-completion` | Truy gốc; test fail trước; bằng chứng trước tuyên bố |
| Phase 5 review | `addyosmani-code-review-and-quality`, `code-review`, `simplify`, `review-agent` | Review đa trục |
| Phase 6 | `addyosmani-performance-optimization` | Đo rồi mới tối ưu |
| Phase 7 | `addyosmani-doubt-driven-development`, `generate-tests`, `generate-test-cases` | Kiểm chứng đối kháng; phủ test |
| Docs | `addyosmani-documentation-and-adrs`, `claude-md-management:claude-md-improver` | Sửa doc drift đúng |
| Orchestration | `workflow-authoring` + `Workflow` | Fan-out có kiểm chứng đối kháng |
| Khi bị chặn quyền | `update-config` | Allowlist, không phải workaround |

*Ghi chú:* `docs/superpowers/plans/2026-09-16-...-redesign.md` nhắc skill `.dsh` — **không tồn tại** thư mục `.dsh`
(đã verify). Skill thật nằm ở `C:\Users\ASUS\.claude\skills` + plugin đã cài.

---

## Câu hỏi còn mở cho owner (chỉ chặn nếu cần quyết trước khi fix)
1. **C1** — `POST /api/vocabulary` mở cho mọi user: giữ (mọi người đóng góp) hay siết ADMIN + chuyển lưu từ student sang deck-scoped?
2. **C6** — Xoá dependency Azure Speech SDK (~15MB, không dùng) hay giữ?
3. **`?sort=`** — bổ sung UI sắp xếp hay bỏ tham số?
4. **C8** — `/api/srs/*` 0 caller frontend: giữ làm API nội bộ, bổ sung UI, hay retire?

*(Ba câu 1–3 và câu 4 sẽ được **đo rồi đề xuất**, không tự quyết; nếu không chặn tiến độ thì v12 vẫn chạy và ghi finding.)*
