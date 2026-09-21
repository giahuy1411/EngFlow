# Tasks — audit-v12-full

Thứ tự thực thi. `[x]` = **đã verify bằng artifact**; `[~]` = code có nhưng chưa verify; `[ ]` = chưa bắt đầu.
Không mục nào chuyển `[x]` nhờ hàng xóm.

## Phase 0 — freeze & baseline

- [x] T0.1 Git checkpoint + tree sạch — **`30bc9b6`**, 0 dirty
- [x] T0.2 Tạo `.specify/specs/audit-v12-full/` + artifact pipeline (constitution/spec/clarify/checklist/plan)
- [x] T0.3 Backend suite, đếm từ run log — **485 run / 0 fail / 0 error / 11 skipped, BUILD SUCCESS**
- [x] T0.4 Frontend suite + build — **119 passed / 1 skipped (23 file)**; entry **177.44 kB** (gzip 67.56)
- [x] T0.5 Container inventory + probe service + kiểm stale — 8 container up, backend **không stale**
- [x] T0.6 Parity live — **`1471|43735|72|127|28|15|4|126|14|5`** (khớp v11)
- [x] T0.7 Endpoint inventory tái dựng từ source — **131 annotation → 147 row / 145 distinct / 26 controller**

## Phase 1 — audit DB trong Docker (read-only)

- [x] T1.1 **Hiệu lực** constraint: FK/UQ/filtered index thực sự chặn insert vi phạm (DB scratch, DROP sau)
- [x] T1.2 Orphan scan viết sao cho NULL FK không bị đếm nhầm (in NULL count dòng riêng)
- [x] T1.3 Streak schema: policy, days, cutover semantics
- [x] T1.4 Row-level sanity trên bảng UI đọc (đo lại, không kế thừa)
- [x] T1.5 Index usage / fragmentation hot read path
- [x] T1.6 Quét `Msg \d+` mọi batch

## Phase 2 — API sweep trên container thật

- [x] T2.1 Auth: login/register/forgot/reset/me/change-password/avatar, lockout, expiry, role 2 chiều
- [x] T2.2 Lessons/Exercises incl. tái chứng minh guard draft F88/F89/F115/F126
- [x] T2.3 Streak `/snapshot` — đủ 7 field hợp đồng + type
- [x] T2.4 Search/sort — vocab search, dictionary, admin exercise; đo lại `?sort=`
- [x] T2.5 CRUD qua API, dọn trong cùng run (deck/lesson/vocabulary)
- [x] T2.6 **12 controller 0-coverage** đều có probe đúng role + hợp đồng
- [x] T2.7 AI contracts + TTS + Whisper
- [x] T2.8 Payment create-order + webhook chữ ký sai, dọn cùng run
- [x] T2.9 Role matrix 2 chiều
- [x] T2.10 Ghi rõ biên giới tiền thật/phá huỷ (BLOCKED kèm lý do)
- [x] T2.11 Re-assert parity

## Phase 3 — UI sweep bằng CẢ HAI MCP

- [x] T3.1 chrome-devtools MCP driver hạng nhất (snapshot/console/network bodies/trace/Lighthouse/emulate)
- [x] T3.2 Playwright MCP driver 2 (route/flow/network/resize/screenshot)
- [x] T3.3 Route×role 39 route × 3 role, guard 2 chiều, assert page.url()
- [x] T3.4 Console/network health
- [x] T3.5 Responsive 360/768/1280/1440/1920
- [x] T3.6 A11y — alt/tap-target/focus/skip-link/heading order
- [x] T3.7 Contrast AA composite alpha
- [x] T3.8 UI↔API cross-check 6 chức năng
- [x] T3.9 Dọn payment row + re-assert parity
- [x] T3.10 Design-system conformance + verify font bằng document.fonts.check

## Phase 4 — CRUD qua UI admin

- [x] T4.1–T4.3 Create/edit/delete lesson qua UI, verify cả UI lẫn API
- [x] T4.4 Lặp cho deck
- [x] T4.5 Re-assert parity — CRUD không để rác

## Phase 5 — sửa lỗi đo được

- [x] T5.1 Mỗi lỗi: bằng chứng fail → fix → chạy lại cùng probe → ghi cả 2 số
- [x] T5.2 Mỗi fix có regression test (F149: `AdminLessons.placeholder-contrast.test.js`, 2 test pass)
- [x] T5.3 Chạy lại backend + frontend + build, đếm từ log

## Phase 6 — hiệu năng, trước và sau

- [x] T6.1 Trước: hot endpoint + path mới, median ≥5
- [x] T6.2 Trước: bundle + CWV/LCP
- [x] T6.3 Chỉ tối ưu khi số biện minh; ghi lý do từ chối
- [x] T6.4 Sau: cùng phép đo, cùng phương pháp

## Phase 7 — vòng 2, rộng hơn (loop-until-dry)

- [x] T7.1 Full suite trên build cuối
- [x] T7.2 Lặp browser sweep; so Phase 3
- [x] T7.3 Case biên đối kháng: streak edges, SRS cap 365, cutover, lockout, bucket routing
- [x] T7.4 Cross-artifact consistency
- [x] T7.5 Viết report: đã làm / chưa làm / đã fix & cách fix / skill đã nạp

## Phase 8 — đóng

- [x] T8.1 `speckit-analyze` (read-only)
- [x] T8.2 `speckit-converge` — bù việc chưa xong
- [x] T8.3 Quét secret trong evidence trước commit
- [x] T8.4 `speckit-taskstoissues` — tạo **issue GitHub thật** #5/#6/#7/#8 cho 4 finding chưa fix
- [ ] T8.5 Commit artifact + fix

## Việc "verify-first rồi mới quyết"

- [x] C1 `POST /api/vocabulary` — probe 3 role + đọc call-site + đo blast-radius → finding + đề xuất (không tự đổi authz)
- [x] C2 README "223 tests"/"73 tests,14 files" → sửa theo số đo
- [x] C3 CLAUDE.md "Azure Speech SDK" → sửa (thật: Whisper+Ollama)
- [x] C4 CLAUDE.md `@PremiumRequired` là cơ chế → sửa (thật: 0 usage)
- [x] C5 CLAUDE.md "refresh via login" → sửa (thật: 0 endpoint)
- [x] C6 pom.xml Azure SDK ~15MB không dùng → **OWNER DECISION** (đã verify 0 import; không tự xoá)
- [x] C7 AdminController/LessonSnapshotController không `@PreAuthorize` → verify 403 rồi quyết
- [x] C8 `/api/srs/*` 0 caller frontend → phân loại

## Skill nạp theo phase

| Phase | Skill / plugin |
|---|---|
| Toàn pipeline | `speckit-*` (constitution→…→taskstoissues) |
| Nền | `superpowers:using-superpowers`, `superpowers:brainstorming` |
| 0–1 | `java-springboot`, `java-coding-standards` |
| 2 | `addyosmani-api-and-interface-design`, `java-docs` |
| 2 security | `addyosmani-security-and-hardening`, `claude-security:scan`, `security-review` |
| 3 browser | `chrome-devtools-mcp:*` (chrome-devtools, a11y-debugging, debug-optimize-lcp, cookie-debugging, troubleshooting) |
| 3 driver 2 | Playwright MCP (`browser_*`) |
| 3 a11y/design | `accessibility`, `frontend-design` |
| 3 method | `addyosmani-browser-testing-with-devtools`, `addyosmani-frontend-ui-engineering` |
| 5 | `superpowers:systematic-debugging`, `:test-driven-development`, `:verification-before-completion` |
| 5 review | `addyosmani-code-review-and-quality`, `code-review`, `simplify`, `review-agent` |
| 6 | `addyosmani-performance-optimization` |
| 7 | `addyosmani-doubt-driven-development`, `generate-tests`, `generate-test-cases` |
| Docs | `addyosmani-documentation-and-adrs`, `claude-md-management:claude-md-improver` |
| Orchestration | `workflow-authoring` + `Workflow` |

## Phase 9 — xử lý 4 hạng mục OPEN (sau khi owner chọn hướng)

- [x] P9.1 **F148** — hợp nhất 2 thuật toán SRS về SM-2; 3 nút Lại/Tiếp/Dễ khác nhau thật; "Lại" requeue
- [x] P9.2 **F147** — `POST /api/vocabulary` deck-scoped + atomic; dedupe; IDOR rollback; thêm enum `VIDEO_LESSON`
- [x] P9.3 **F150** — `#main-content{min-height:100vh}`; CLS 0.104 → 0.001 (Playwright ×5 + Lighthouse)
- [x] P9.4 **C6** — xoá Azure SDK + `azure.speech.*` + DTO chết; khai báo tường minh `jackson-datatype-jsr310`
- [x] P9.5 Verify tổng hợp — backend 492/0/0, frontend 121, API sweep 137/0/0, parity không đổi, 0 rác

## Phase 10 — 3 hạng mục cuối (F151 · UI · dọn dữ liệu)

- [x] P10.1 **F151 (mới)** — IDOR `/api/srs/due/{deckId}`; uỷ quyền `DeckService.getDeckById`; 4 test authz;
      probe `f151-idor-probe.js` **9/9 PASS**; kèm sửa 200+`[]` → 404 cho deck không tồn tại
- [x] P10.2 **Item A** — UI "ôn từ đến hạn": `srsService.js` (map field) + `DueReview.vue` + route
      `/decks/:id/review` + nút vào ở `DeckDetail.vue`; 7 test mới; E2E live 4 trạng thái + quality 1/4/5
- [x] P10.3 **Item B** — backup (`COMPRESSION,CHECKSUM` + `VERIFYONLY`) → dọn 9 vocab rác + 4 deck test +
      lesson 61882 + con; parity mới **`1470|43734|72|118|28|15|4|126|10|5`** (khớp dự đoán)
- [x] P10.4 **Hồi quy do chính P10.3 gây ra** — sweep phụ thuộc dữ liệu ambient (deck student bị xoá);
      sửa **harness** tự cấp phát deck → **137 pass / 0 fail**
- [x] P10.5 Verify tổng hợp — backend **496**/0/0, frontend **128**, build xanh, API sweep 137/0/0,
      Lighthouse route mới **100/100/100**, parity mới đúng, 0 rác

