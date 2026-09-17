# Tasks — audit-v8-full

- [x] T01 Đọc goal-objective + constitution + audit-v7 REPORT (baseline 327→332, 79 tests)
- [x] T02 Backend suite vòng 1: 332/332 BUILD SUCCESS (43.8s)
- [x] T03 Frontend suite vòng 1: 79/79 (16 files)
- [x] T04 Sweep 42 endpoint v7 (42/42, slowest 374ms) + CRUD 43/43 → tái sử dụng làm baseline
- [x] T05 Enumerate 139 mapping annotations → thiết kế sweep mới (p1/p2/p3a/p3b)
- [x] T06 P1 public+role matrix: 124 probes → 0 fail (sau khi sửa 2 expectation sai của harness)
- [x] T07 P2 CRUD: 61 probes → 0 fail (lesson/exercise/vocab/prompt/deck/structure/snapshot+restore/SRS/flashcard/game submit UI-shape)
- [x] T08 P3a AI surface: 37 probes, 1 fail thật (F86) + 2 chất lượng (F84/F85); Ollama 1.5b/3b chạy thật
- [x] T09 P3b uploads/payment/speaking: 19 probes, 0 fail; Whisper+3b assess 14.6s; shadowing ai-grade 6.8s; SePay order QR
- [x] T10 Phát hiện + chứng minh stored XSS bằng Chromium thật (đánh cắp JWT 227 ký tự) → F81
- [x] T11 Fix F81 (2 tầng) + rebuild + PoC chạy lại: upload 400, file cũ trên đĩa = text/plain/attachment
- [x] T12 Fix F82 game submit (6 shape 500 → 400, đo sống)
- [x] T13 DB audit: 4.7GB/2 file, 24 bảng, 0 orphan, empty_answer 4848, LISTENING thiếu audio 9/367 (khớp AGENTS)
- [x] T14 REBUILD 2 index (đo trước/sau) + xác nhận count 43737 nguyên vẹn
- [x] T15 Top-query analysis → 2 hotspot (197k reads backfill one-shot; 95k reads admin page) → F87
- [x] T16 Fix F87 + đo lại: 95k→27k reads, 367→30.6ms; endpoint 60–105ms, titles đủ 20/20
- [x] T17 Browser UI sweep 39 route × admin/guest + mobile 375px → 0 console error, 100% Be Vietnam Pro
- [x] T18 Thiết kế audit: 0 font lạ, h1 weight 900, focus/skip-link, reduced-motion 0s; 0 target <24px
- [x] T19 Fix UI-1 overflow header (đo 360→1920px cả guest+admin: 0 overflow)
- [x] T20 Fix UI-2 alt + test; kiểm tra bundle: entry về 176.68kB baseline (hook lazy)
- [x] T21 Rate-limit burst đo thô: global đúng 100→429, AI 10→429, order không → F83 fix
- [x] T22 Suite cuối: 354/354 backend, 82/82 frontend, vite build OK
- [x] T23 Vòng 2 trên bản dựng cuối: P1 124/124, P2 61/61, XSS PoC safe, overflow 0, dup-option rejected, order bucket 10→429
- [x] T24 DB hygiene: xóa đúng row do tôi tạo (lessons/vocab/decks/prompts/payments/submissions/attempts + 2 file upload); parity về baseline
- [x] T25 Artifacts spec (constitution→converge) + REPORT + phân tích lỗ hổng prompt
- [x] T26 Xóa nghi vấn Cloudinary: key **có thật** trong `.env`, verify bằng file media thật → avatar + audio-upload **200 + URL Cloudinary** (2026-09-15)
- [x] T27 Đóng gap coverage còn sốt (authz→write, CRUD video, multipart, manual grade): p4a–p4e, 287 probe, DB về baseline
- [x] T28 Đính chính báo cáo: số probe 241→287, gỡ claim "demo creds", nhận 2 lần báo sối khi chưa có file bằng chứng ("21/21" p4c, "80 tests" frontend)
- [x] T29 Vòng 3 — xác minh lại baseline trên bản dựng cuối: backend `354/354` (fresh), frontend `82/82` (17 files), `vite build` entry 176.68 kB
- [x] T30 Vòng 3 — viết `sweep/v8/p5.js`: đăng ký/đăng nhập/lockout (15'), streak boundary trên DB thật (1 → +1 → no-op → đứt 3 ngày → reset 1), tìm kiếm/sắp xếp (lessons `q`/`level`, vocab, admin `q`/type/difficulty), tái xác minh F81/F83/F86 → **95 probe / 60 assert, 0 fail**
- [x] T31 Vòng 3 — viết `sweep/v8/ui/v3.js` (browser thật, fake mic): đăng ký qua form, lịch streak khớp server clock, ô search `/lessons`, **ghi âm thật → upload → assess → row DB** → **30 assert, 0 fail, 0 console error**
- [x] T32 Vòng 3 — cleanup + parity: xoá user tạm, submission + object MinIO (`mc rm`), file upload thử; parity về đúng baseline 1471/43 737/76/127/28/15/4/126/14/5/38, 0 orphan
- [x] T34 Vòng 3 — fix UI-3: 2 ảnh preview admin thiếu hẳn `alt` (`AdminExercises.vue`, `AdminLessonBuilder.vue`); sửa metric false-positive của `design.js` (`alt=""` không phải vi phạm) + tách ngưỡng 24 px (AA) / 44 px (AAA) → `imgNoAlt=0`, `small24=0` trên 13 trang, mobile 375 px cũng 0; frontend vẫn `82/82`, bundle không đổi. Verify sống bằng `ui/v3b.js` (tạo lesson/section/block IMAGE tạm + mở modal bài tập) → **17 assert 0 fail**, dọn sạch về baseline
- [x] T35 Vòng 3 — chạy lại 92 lượt route browser trên bản dựng hiện tại (admin 1440 + admin 375 + guest) → PROBLEM=0, 0 overflow, 0 font lạ
- [x] T33 Vòng 3 — cập nhật REPORT (§1.2/§1.3/§1.4/§1.7/§3/§4/§5) + ghi 3 phát hiện vận hành (không có DELETE submission; `/api/vocabulary` list cần auth; admin `q=` full-scan ~185 ms)
- [x] T36 Vòng 3 lần 2 — chạy lại TOÀN BỘ sweep trên bản dựng cuối: P1 124 · P2 61 · P3a 36 · P3b 19 · P4a 18 · P4b 16 · P4c 10 · P4d 6 · P4e 7 case · burst `hist` → **0 fail**; sửa expectation harness (enum `PRE_INTERMEDIATE`, update ≥ 2 dòng transcript, `transcript` phải có mặt, nháp → 404 guest) + `burst.js` in histogram
- [x] T37 Vòng 3 lần 2 — **F88**: admin không lưu được bài học video (3 tầng: thiếu nút Sửa; `lesson.youtubeUrl` undefined vì summary trả `youtubeVideoId`; `transcript:null` bị `@NotNull` chặn → guard "giữ nguyên phụ đề" thành dead code). Fix cả 3 + verify browser thật `ui/v4edit.js` **13/13** (PUT 200, toast, phụ đề giữ nguyên) + 5 test backend + 3 test frontend
- [x] T38 Vòng 3 lần 2 — **F89**: bài NHÁP `is_published=false` lộ công khai qua `/api/lessons/{id}`, `/api/v1/video-lessons/{id}`, `/api/lessons/{id}/exercises`, `/exercises/content` (đo thật: 6 bài nháp trả 200 + content cho guest). Fix bằng `assertLessonVisible` + guard `getDetail` → 404 cho guest/student, 200 cho admin; `p88live.js` **16/16** trên container đã rebuild + 10 test mới
- [x] T39 Vòng 3 lần 2 — xác minh `openrouter.*` **không phải cloud**: 0 hit `openrouter.ai` trong code; 1 call `ai-generate` sinh đúng 1 dòng mới trong log Ollama local (5.24 s khớp latency). Ghi nhận: `.env` giữ API key OpenRouter thật (73 ký tự) nhưng không có egress → secret chết, ghi ở REPORT §3.12
- [x] T40 Vòng 3 lần 2 — cleanup + parity: `clean_f88.sql` (1 user, 1 vocab, 3 submission, 1 video attempt, 1 lesson submission, 25 payment row) + `mc rm` 6 object MinIO mồ côi + move 5 `p*.json` về `sweep/v8`; parity về đúng baseline 1471/43737/76/127/28/15/4/126/14/5/38, 0 orphan
- [x] T41 Vòng 3 lần 2 — chốt cuối (2026-09-16): backend `369/369` (`t-final3.log`, exit 0), frontend `85/85` / 18 files (`t-fe-final.log`), `vite build` entry `176.68 kB` (gzip 67.34), parity DB **đúng** baseline + `orphan_u=4` (row `zzprobe*` 12/09) + 0 row `ZZ%` sót; sửa nốt số stale trong docs (REPORT §2 UI-3 → 85/85, checklist A8/E1 → 369/85); trả `sweep/v7-crud-r3.json` + `sweep/v7-sweep-r4.json` về bản committed (harness ghi đè do chạy từ CWD khác); sửa nhãn case trong `p4e.js` (nhãn cũ ghi sai, expect 201 cho admin create) rồi chạy lại → 6/6 FAIL=0

## Vòng audit-v8-full (2026-09-16) — lượt 1: F90–F94

- [x] T42 **F90** — nghi vấn `RateLimitFilter` `@Order(1)` chạy SAU Spring Security (`DEFAULT_FILTER_ORDER=-100`, xác nhận bằng `javap` trên `spring-boot-security-4.0.6.jar`). Đo được đúng hiện tượng (60× anonymous POST → 401×60, `keys=[]`) nhưng **bác bỏ** bằng falsification: quét **toàn bộ 17 surface `permitAll`** → **0/17 surface không tốn bucket**. Request bị security từ chối không chạy business logic ⇒ không có tài nguyên để bảo vệ. **Không sửa** (đổi thứ tự filter = rủi ro cao, lợi ích đo được = 0). Trạng thái `CLOSED-NOT-A-BUG`
- [x] T43 **F91** — bucket `:ai` chỉ khớp tiền tố literal `/api/ai/` → **12 endpoint thật gọi Ollama** rơi vào `:global`. Đo bằng đọc **key Redis thật** (probe path 404, 0 side effect). Fix: `AI_PATH_PREFIXES` + `AI_PATH_FRAGMENTS` + `isAiEndpoint(uri, method)` **có guard `METHOD=POST`**. `p8` 13/13 → `:ai`; `p8b` first429=**11** (trần 10/phút); +6 test `RateLimitFilterTest`
- [x] T44 **F92** — `:upload` không phủ `/api/auth/avatar/upload` và `/api/v1/admin/video-lessons/upload` → `:global`. Fix: +2 tiền tố. `p8` 2/2 → `:upload`; `p8b` first429=**16**; +test `avatarAndVideoLessonUploadsUseTheUploadBucket`
- [x] T45 **F93** — `POST /api/ai/enrich-word` trả 500 chung khi Ollama vượt timeout 30 s (bắt được khi `p3a` chạy song song; stack: `TimeoutException` tại `AiVocabController.enrichWord:95`). Đo lại nhát 10× sau đó: **10/10 200**, 1000–1274 ms ⇒ transient do contention. Fix: `GlobalExceptionHandler` dò **cause-chain** → **504**; thêm `@ExceptionHandler(TimeoutException)`; frontend đọc fallback `.detail`. 3 test mới; container verify `strings | grep "Gateway Timeout"` = **1**. Biên: đường 504 sống **chưa** ép được
- [x] T46 **F94** — 20 endpoint / 6 controller trả `{"error":"…"}` thay vì ProblemDetail. Fix: chuẩn hoá ở `api.js` (fallback `.detail`) + handler. Đo `p13`: **3/10 vẫn `LEGACY{error}`** (`save-vocab` mảng rỗng + 2 validation) — ghi rõ là biên, không làm tròn
- [x] T47 Regression round 1: `mvnw.cmd test` → **378/378** `BUILD SUCCESS`; `npx vitest run` → **89/89, 18 files**; `vite build` → **176.80 kB / gzip 67.39**; parity DB khớp baseline
- [x] T48 Đính chính `CLAIM-COUNT` + `BASELINE-DRIFT`: số cũ gõ tay (149 mapping / 27 controller / 32 route; 332/354/369 tests) → sinh bằng script (**144 / 26 / 39**) và đo lại suite; cập nhật `AGENTS.md` + `constitution` đề xuất PATCH

## Vòng audit-v8-full (2026-09-16) — lượt 2: Task 1–16 (comprehensive-audit-redesign)

- [x] T49 Hoàn thiện **Required Evidence Layout** — 8 tài liệu còn thiếu: `constitution.md`, `route-map.md`, `db-audit.md`, `performance.md`, `design-audit.md`, `security-audit.md`, `analyze.md`, `converge.md`. Mỗi tài liệu gắn với **một harness chạy lại được** (không viết văn xuôi suông). Kiểm lại: **16/16 mục + `evidence/` + `findings.json`**
- [x] T50 Task 2 — viết `prompt-rewritten.md` (bản prompt dán được nguyên khối) + sửa **11 lỗ hổng** của prompt gốc (Outfit/PJS, token shadcn, Lucide React, breakpoint thiếu, WCAG SC thiếu, headings 800 vs 900…). Đồng thời sửa **ký tự CJK lẫn** trong `prompt-gap.md` (`指令`, `已从`, `ảnh装饰`, `二元`) + thẻ đóng sai `</mition>`
- [x] T51 Task 3/4 — `checklist.md` + **2 lượt** `analyze.md`. Lượt 1: 6 tài liệu thiếu + 1 file bằng chứng **stale** (`p8.json` 16/16 status=404, sinh trước bản fix) + 1 bộ số chưa có generator → **0 thay đổi code cần thiết**. Lượt 2: suite xanh trên bản dựng cuối, 6/6 finding tái xác minh, `git diff HEAD` rỗng, 3 biên còn lại ghi rõ
- [x] T52 Task 5 — endpoint/route inventory **sinh bằng script**: `route_inventory.py` → **39 route**; `endpoint_inventory.py` → **144 mapping / 26 controller**; `route_verify.py` → `VERDICT: CONSISTENT` (không còn số gõ tay)
- [x] T53 Task 6 — browser sweep: `routes-all.js` **152 lượt** (38 route × 2 viewport × 2 role; `/admin` chỉ redirect) → **0** console error, **0** API ≥400, **0** overflow; `design-v2.js` **60 tổ hợp** → **0/7 685** element lệch font, **0/60** overflow; `design.js` → **0** vi phạm AA *(T67 nâng lên **228 lượt / 3 role**, xem T67)*
- [x] T54 Task 7–12 — `db-audit.md` (12 mục read-only, **0 lỗi SQL**: orphan 0, 0 FK disabled, 2 filtered index, 4 bảng `_bak_v5` deferred, ~9 index 0-seek deferred, 230 MB/64 MB log) + `performance.md` (N=7 × 15 endpoint, **105/105 200**; admin list **4 600 reads / 35 ms**, `projection only (F87 shape)`) + `security-audit.md` (10 admin surface × 3 role, 2 self-scoped, 4 public, 4 token case, **0 fail**, 0 leak / 10 pattern; 13 AI → `:ai`, 2 upload → `:upload`)
- [x] T55 Task 13 — `constitution.md`: P1–P8 **0 vi phạm**, **0 sửa đổi**; đề xuất PATCH v1.0.2 cho số P1 (221/73 → 378/89) — deferred vì là quyết định chủ sở hữu
- [x] T56 Task 14 — **vòng 2 adversarial**: `p14_adversarial.js` **38 case / 0 fail** (malformed JSON, dup register, empty search, pagination, stale, upload, slow network, service-down, 503 — dùng `route` interception, **không** dừng container thật)
- [x] T57 **Phát hiện 3 case của `p14` "pass vì lý do SAI"** — (1) upload gửi token **user** tới route admin-only → 403 che hết `SafeUploadNames`; (2) submit sai cả 2 tên field (`GradeRequest` là `{answers:[{exerciseId,userAnswer}]}`) → `answers` null → 200; (3) pagination chỉ assert "không 5xx" → sẽ pass cả khi trả 100 000 row. Viết `p15_adversarial_deep.js` với assert đã sửa → **21 case / 0 fail** (8/8 upload 400 dưới token admin; submit `total===0 && results.length===0` + positive control; clamp `100/1/20/0` row)
- [x] T58 **`xss-prove.js` không tự dọn dữ liệu** → parity từng bị đọc **SAI** (130 thay vì 127). Đưa cleanup vào trong-run; sửa **2 bug của chính cleanup**: (1) timestamp run-start tính **lúc cleanup** nên cửa sổ bắt đầu SAU khi ghi → khớp 0 row, no-op âm thầm; (2) coi "không có lỗi SQL" là thành công dù còn 3 row. Assert nay là **parity** (`remaining=0` VÀ `total=127`). Verify: `PARITY RESTORED`, verdict PASS
- [x] T59 Cleanup + parity: `p15-clean-residue.sql` (3 row XSSPROBE/`iframe` → **127**) + `p16-clean-payments.sql` (6 row PENDING do 3 sweep mount `/premium` → **126**, chỉ xoá `status <> 'SUCCESS'`). Xác nhận 10 bảng **khớp chính xác** baseline + orphan `0/0/0/0/0`
- [x] T60 Task 15 `taskstoissues` — kiểm pre-check: `extensions.yml` không tồn tại; remote **đúng** GitHub; `tasks.md` **0 task chưa tick**; `findings.json` **15/15 có status cuối**; `gh` CLI + GitHub MCP **không có**. ⇒ **"not run: no GitHub issue conversion required"** (không tạo `issues.md`)
- [x] T61 Task 16 — cập nhật `REPORT.md` (§6/§4/§5) + `findings.json` (10 → **15** finding, +5 lớp `AUDIT-HARNESS`/`AUDIT-ARTIFACT`) + `workflow-log.md` (§8 Round 2) + đính chính headline đếm lỗi (bảng 5 nhóm, đối chiếu `findings.json`)
- [x] T62 Quét placeholder + BOM + CJK trên toàn bộ artifact: `TODO/TBD/FIXME` = **0** (1 hit duy nhất là header bảng "Placeholder \| Id used" mô tả `:id` trong URL); BOM = **0/26 file**; CJK còn lại = **2** đều là **trích dẫn lỗi đang được ghi lại**; `git diff --check` **sạch**; 0 file `src/` untracked
- [x] T63 Task 5 (PLAN.md Phase 5) — **36 screenshot** cho flow bắt buộc: `p17_screenshots.js` chụp 30 desktop + 6 mobile → `evidence/screens/` (6,7 MB); verify bằng vision `22-admin-dashboard.png`
- [x] T64 PLAN.md Phase 2/4.4 — **backup trước DML**: `engflow_2026-09-16-audit-v8-full.bak` (197,1 MB thô / 35,3 MB nén), `RESTORE VERIFYONLY` valid, `is_damaged=0`, copy ra `C:\Users\ASUS\engflow-backups\`
- [x] T65 **Lỗi harness seed chỉ `token`** (HARNESS-TOKEN-ONLY-SEED): `p18_redirect_audit.js` phát hiện 9 route admin bị đá về `/`; vision xác nhận `22-admin-dashboard.png` là trang chủ; sửa bằng `loginFull()`+`mapUser()`+`seedAuth()`
- [x] T66 **Đo giới hạn thiệt hại bằng A/B, không suy đoán**: `p20_routes_all_old_seed_ab.js` seed cũ vs mới trên đúng thứ tự route → **cả hai 0/9 bounce**; `p19` case C xác nhận `design-v2` không bị ảnh hưởng ⇒ chỉ `p17` (9/36 ảnh) và `p18` bị, KHÔNG phải cả sweep 152 lượt
- [x] T67 `routes-all.js` nay chạy **3 role** (admin/user/anon) = **228 lượt**, ghi `finalPath` mọi lượt, assert hợp đồng landing guard × role **cả hai chiều** → 0 wrong landing, **18/18 lượt admin render trang admin thật**
- [x] T68 `cleanupAuditPayments()` trong `ui/lib.js` — sweep tự dọn row `payment_transactions` nó ghi (parity 126 → 134 → **126**), chỉ nhắm `status <> 'SUCCESS' AND transaction_id IS NULL`, assert parity chứ không tin exit code
- [x] T69 Sửa assertion sai của chính mình: `/login` là `guestOnly` nên admin ĐÚNG khi bị đá sang `/lessons` — 5 false alarm ở `design-v2` đã loại, `LANDED ON WRONG PAGE = 0`, exit 0
