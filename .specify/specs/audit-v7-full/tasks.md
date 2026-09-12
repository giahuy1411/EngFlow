# Tasks — audit-v7-full

> Trạng thái: `[x]` = đã làm + có bằng chứng trong plan.md/checklist.md/subreports.
> `[ ]` = còn lại ở thời điểm generate (xem tiến độ cập nhật trong REPORT.md).

## Phase 1 — Quét (Scan)

- [x] T001 Static audit backend bằng subagent → `subreports/backend-static-audit.md` (82 findings, phân P1/P2/P3)
- [x] T002 Live DB audit qua sqlcmd trong Docker → `subreports/db-audit.md` (24 bảng, 46.705 rows, fragmentation, FK, data quality)
- [x] T003 Design audit frontend bằng Playwright (computed styles, token spot-check, 375px) → `subreports/design-audit.md`
- [x] T004 Sweep 53 endpoint × 3 vai trò (noauth/user/admin) → `sweep/v7-sweep-r1.ps1`, `r2.ps1`
- [x] T005 CRUD harness (lesson/exercise/prompt/deck/auth/streak/search/grade) → `sweep/v7-crud-r3.ps1`

## Phase 2 — Vá wave-1 (security + perf)

- [x] T010 F34: `GET /api/decks/my` 500 → 401 khi guest + regression test `DeckControllerMyDecksTest` (3 tests)
- [x] T011 F41/B-06: `correct_answer` nvarchar(500)→2000 (SQL + entity khớp, tránh ddl-auto co lại)
- [x] T012 F51: REBUILD PK `exercises` (frag 95.11%→0.70%, pages 2330→1277, scan 19ms→1ms)
- [x] T013 Create indexes M-1 `IX_uvp_due`, M-2 `IX_exercises_lesson_type_diff_order`, M-3 `IX_decks_owner_name` (verify sys.indexes + timing)
- [x] T014 C-03a: xóa dead `getAllExercises()` (findAll 43.7k rows = 57.8% logical reads) — grep 2 phía trước khi xóa
- [x] T015 F47: gỡ rule permitAll shop dead; F55/F59 comments giải thích bề mặt media/resources

## Phase 3 — Vá wave-2 (P1 security/data)

- [x] T020 F54: attempts endpoints — rule `.authenticated()` đặt trước permitAll + guard `isRealUser()` 2 lớp; test guest→401 (2 tests)
- [x] T021 F55: `MediaSigner` (HMAC-SHA256 exp/sig, TTL 6h) + `MediaProxyController` verify ticket, fallback owner-auth; signed URL trong 3 service paths; tests: signed 200 / tamper 403 / expired 403 / owner-fallback
- [x] T022 F56: `getLessonStructure` bỏ @Transactional + INSERT → section ảo read-only; test 0 rows sau GET
- [x] T023 F53: `wrapAnswerSections()` — bọc khối ĐÁP ÁN/ANSWER vào `<details><summary>`; wired `getCleanContent`; live 6 blocks + browser verified
- [x] T024 F57: `processSePayTransaction` chặn underpayment (amount < order) + test reject, không kích premium
- [x] T025 F60: `saveVocab` — @Valid list, empty→400, >50→400, quota check + consume; 2 tests mới
- [x] T026 F61: RateLimitFilter thêm bucket `:ai` 10/phút, `:upload` 15/phút (POST only), `:order` 10/phút; live 429 sau 10 request
- [x] T027 F62: `IllegalArgumentException` → 400 ProblemDetail tiếng Việt (hết 500 cho enum rác); live probe 400/200
- [x] T028 F58: DatabaseSeeder log SECURITY warning khi fallback password; decision: giữ dev seed (AGENTS.md) + document
- [x] T029 Wave-2 suite: `AuditV7SecurityWaveTest` 10/10; full `mvnw test` **327/327 BUILD SUCCESS**

## Phase 4 — Vá wave-3 (design/FE)

- [x] T030 F63: AdminDashboard donut/bars `hsl(var(--accent))`→`var(--geo-*)` (computed rgb(139,92,246) — hết đen)
- [x] T031 F64: mobile hover/active shadow-pop patch đủ 20 lượt lọt lưới F27 (`[class*="hover:shadow-pop"]` selectors)
- [x] T032 F65: reduced-motion giết `hover:-translate-*` + `scroll-behavior:auto`
- [x] T033 F66: retract — không còn self-inject `<link href="/src/assets/main.css">` (grep 0 match)
- [x] T034 F67: AdminLayout off-canvas sidebar 375px (burger + overlay + auto-close on navigate; verified DOM)
- [x] T035 F68: overflow-x-auto cho 4 bảng admin (computed verified)
- [x] T036 F69: index.html nạp đúng weights có usage (bỏ 800 = 0 lượt dùng)
- [x] T037 F70: contrast text trạng thái → `text-success`/`text-danger` (LessonPreview, TypingGame ×2, PremiumCheckout ×2, Register, AiVocabGenerator ×3, DeckCreate)
- [x] T038 vitest 79/79 sau wave-3

## Phase 5 — Khép kín (Round 2)

- [x] T040 Rebuild container `docker compose up -d --build backend` + toàn bộ live probes ở plan.md “Verification evidence”
- [x] T041 Rerun sweep r2 (53 endpoint × 3 vai trò) trên container mới — `sweep/v7-sweep-r4.ps1` 42/42 PASS + F56 SQL row-check = 0
- [x] T042 Rerun CRUD r3 với token mới (login → CRUD → cleanup) — 43/43 đúng expect
- [x] T043 Playwright sanity các route chính sau FE wave + console-error scan — 20+ route, 0 console errors; `vite build` production OK
- [x] T044 Screenshot route chính desktop + admin mobile — `audit-v7-shots/` (5 file)
- [x] T045 Cập nhật checklist.md + spec/clarify nếu phát hiện mới — checklist all-green; spec §2/§4 hiệu chỉnh theo analyze

## Phase 6 — Báo cáo

- [x] T050 REPORT.md tiếng Việt: đã làm / chưa làm / đã fix + cách / skill đã nạp / khuyến nghị cần user quyết
- [x] T051 SpecKit analyze + converge: analyze → 5 findings (I1 baseline 319→314/327 đã sửa trong spec; I2 supplement F53–F70 đã thêm; C1 DoD checkboxes đã tick; A1 weights stale đã chú thích; 0 constitution violation). converge → đánh giá F43/F45/F46: tất cả dead code/comment/formLogin KHÔNG CÒN TỒN TẠI trong code (grep verified) → 0 actionable findings → **converged, tasks.md giữ nguyên**

## Follow-ups ghi nhận nhưng không làm (lý do)

- Drop `content_original` (~69MB, 34.7% DB) + 4 bảng `*_bak*`: ràng buộc v6 không xóa + DB chưa từng backup (`msdb.backupset`=0) → chỉ khuyến nghị trong REPORT
- Backfill 5.434 `correct_answer` rỗng (12.42% exercises): cần data source, không phải code fix → REPORT khuyến nghị
- Normalized timezone cho naive-local writes: đổi lớn, cần user duyệt → REPORT khuyến nghị
- `srs_interval` outlier 1914d: data fix một dòng, không khẩn cấp → REPORT
