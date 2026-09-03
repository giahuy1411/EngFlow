# AUDIT-V4 — Báo cáo kiểm thử toàn diện lần 2 (kèm thanh toán thật + tối ưu)

> Speckit feature `audit-v4-full` · 2026-09-03 · Baseline `d4501a6` (main, clean)
> Tiền đề: audit-v3 — lần này lặp test mở rộng, sửa tận gốc, verify bằng chứng runtime (P8).

## 0. Design-system prompt — đã kiểm tra lỗ hỏng & vá (yêu cầu #2 của user)

Prompt "Playful Geometric" gốc có 6 lỗ hỏng, đã vá và *(được audit-v3) neo vào constitution P6/P7, kỳ này rà lại và verify đầy đủ*:

| # | Lỗ hỏng | Cách vá |
|---|---|---|
| 1 | Font không hỗ trợ tiếng Việt tốt (Outfit/PJS) | **Be Vietnam Pro** duy nhất (P6) — grep `Outfit`/`Plus Jakarta Sans` = 0; computed font trên mọi trang = "Be Vietnam Pro" |
| 2 | Không có success criteria đo được | R1–R7 trong spec + design check tự động (computed fontFamily, `document.fonts.check`, token `--geo-*`) |
| 3 | Không có stop conditions cho agent | P8 + safeguard: KHÔNG auto-accept dialog xóa (bài học sự cố lesson 444 của v3); xóa chỉ đúng hàng có nhãn test |
| 4 | Thiếu scope lock | Out-of-scope trong spec + complexity budget (governance) |
| 5 | Contrast: pink #F472B6 không dùng làm chữ trên nền trắng | Quy ước token (accent-only text-accent); verify trên các trang chính |
| 6 | Mobile: thiếu quy tắc decoration/overflow | `prefers-reduced-motion` hiện hữu; **phát hiện + sửa bug tràn ngang thật** (mục 2, #5) |

**Kết quả verify trên browser thật**: body + heading = "Be Vietnam Pro" (400/700 loaded), `--geo-accent`=#8B5CF6, `--geo-bg`=#FFFDF5 đúng token, screenshot 375px + 1440px tại `audit-v4-shots/`.

## 1. Phạm vi đã làm

### 1.1 Môi trường & baseline
- 7 container healthy (backend/frontend/sqlserver :1433/redis/minio/whisper/tailscale).
- Backend `mvnw.cmd test`: **221/221 PASS** (chạy 2 lần: đầu phiên và sau mọi fix).
- Frontend `npx vitest run`: **73/73 PASS** (14 files, chạy 2 lần); `vite build` sạch 9.5s.

### 1.2 DB trong Docker (engflow-sqlserver, english_learning)
- 19 bảng; exercises 43.735 rows, lessons 1.469, users 50 (gồm user test nhãn AuditV4*).
- NVARCHAR audit: các cột varchar còn lại đều là enum/URL/ASCII (hợp lệ — cột tiếng Việt đã NVARCHAR từ v3).
- Dấu `?` legacy trong `lesson_blocks.data`: **0 rows** (nghi vấn của v3 §4.6 resolved).
- Orphan: exercise_attempts/payments/progress → **0 orphan**.
- Index: đủ cho query nóng (`idx_exercises_lesson_order`, `idx_blocks_section`, `idx_payment_transactions_user(user_id,status,created_at)`, unique user+vocab…). Không cần index mới.

### 1.3 Backend API — full sweep 26 controllers (script `scripts/audit-v4-api-sweep.mjs`)
- **67/67 PASS** (chạy 2 vòng — vòng 1: 43/52 → fix → 64/66 → fix → 67/67).
- Positive: auth (login/register/me/forgot), lessons (list/detail/structure/exercises/grade/submit/attempts), decks (list/detail/words), flashcards (review/status), games (quiz/typing/mixed/memory qua UI), video (list/detail/attempt multipart), vocabulary + dictionary proxy (cold 20s → **cache hit 18–41ms**), speaking (prompts/submissions), leaderboard, streak (current/history), progress, SRS (stats/due), dashboard, payment (create-order/status×10), admin (users/lessons CRUD/sections/blocks/snapshots/exercises+filter/video-lessons/speaking×2/premium toggle+revoke/ai generate-async→status COMPLETED/ai generate-vocab).
- Negative: login sai mật khẩu 400 · register body sai 400 · lesson id sai 404 · game deck sai **404** (sau fix) · admin không token 401 · admin bằng token user 403 · payment không token 401 · webhook payload lạ → 200+`success:false` (contract đúng).
- **AI thật**: `generate-async` (qwen2.5:1.5b) 202 → status COMPLETED, errors=0, exercises sinh thành công; `ai/generate-vocab` 200 (admin).

### 1.4 Frontend UI — browser thật (Playwright headless Chromium, 1440px + 375px; chrome-devtools-mcp không khả dụng trong phiên này — dùng Playwright thư viện thật, cùng cơ chế CDP)
- User views: Home, Lessons (12 card + phân trang), Lesson detail + tab Bài tập (render input), Video library + Lesson (YouTube iframe render), Decks (10 deck), Deck detail, **6 games render + trả lời 1 câu quiz**, Search "sunny" (kết quả + phiên âm), Leaderboard (50 học viên), Profile, Speaking, Premium, Checkout (QR render).
- Admin views: Dashboard (số khớp DB), Users, **Lessons CRUD qua UI (tạo → xóa đúng hàng nhãn, đối chiếu trước/sau — DELETE 204)**, Exercises, Videos, Video-attempts, Speaking×2, LessonBuilder.
- Console: sạch; network ≥400 chỉ có 403 quota AI (by design, mục 4.4).

### 1.5 Premium payment (T7) ⚠ GATE người dùng
- **Order thật đã tạo qua UI**: `ENGF8AB9431CE85` — user@gmail.com, 10.000đ, MBBank 0706718329, QR SePay render (screenshot `22-checkout-order.png`).
- **Đã nhắc user chuyển khoản 2 lần (AskUserQuestion)** — user không có mặt tại gate. Order giữ **PENDING**, KHÔNG giả lập confirmed trên tài khoản thật (minh bạch theo R6).
- **E2E backend 8/8 PASS trên user test riêng** (`t7-payment-e2e.js`): create-order → webhook **sai chữ ký bị từ chối** → webhook **HMAC đúng (SePay production scheme: `sha256=HMAC(ts.body)`, timestamp giây)** → `payment/status` isPremium=true + `premiumExpiry=2026-10-03` (đúng +1 tháng) → admin users list khớp → **idempotent** khi gửi lại → revoke sạch. DB proof: `payment_transactions.status=SUCCESS` (order `ENG2B345F0DFDFA`).
- Kết luận: **toàn bộ chuỗi backend payment hoạt động thật**. Chỉ thiếu 2 điều kiện hạ tầng để auto-confirm với tiền thật: (1) public webhook URL trỏ về server (đang local), (2) `SEPAY_API_TOKEN` hợp lệ cho polling fallback (hiện 401 → circuit breaker mở, polling disabled).

### 1.6 Hiệu năng (P5 — đo được, không bịa)
- `payment/status`: **22–34ms** (×10 đo liên tiếp) so với 3,5s của v3 — circuit breaker v3 chặn gọi SePay 401 lặp; đã đo, không cần code thêm.
- Dictionary: cold ~20s (upstream), **cache hit 18–41ms** (Redis 1h).
- DB: 0 orphan, index phủ query nóng; không có query nào cần tối ưu thêm ở mức hiện tại (exercises 43k rows vẫn index-hit).

## 2. Bug tìm thấy & sửa tận gốc (6 nhóm)

| # | Bug | Root cause | Fix | Commit |
|---|---|---|---|---|
| 1 | `GET /api/games/{quiz\|memory\|typing\|listening\|mixed}/<deck không tồn tại>` → 200 rỗng | GameService không validate deck tồn tại | `requireDeck()` + `DeckRepository.existsById` → 404 qua handler có sẵn; cập nhật GameServiceTest (thêm mock) | `fix(games)` |
| 2 | POST JSON lên endpoint multipart (video shadowing attempts) → 500 | `MissingServletRequestPartException`/`MultipartException` rơi vào catch-all 500 | Thêm handler → 400 + message tiếng Việt | `fix(common)` |
| 3 | Tạo snapshot bài học có section title/block data null → 500 (NPE trong `Map.of`) | `Map.of` không nhận null; lesson dở build có field null | LinkedHashMap null-tolerant trong takeSnapshot/restore + 404 thay RuntimeException | `fix(snapshots)` |
| 4 | **MinIO upload fail toàn cục** (Shadowing/video attempts 500) | Container MinIO **stale credentials**: giữ password 13 ký tự từ .env cũ, backend đọc password 30 ký tự mới → `SignatureDoesNotMatch` | `docker compose up -d --force-recreate minio` (data volume giữ nguyên); verify bằng multipart upload thật → 200 | (infra, không cần commit) |
| 5 | **Tràn ngang mobile 375px** ở header các trang (Premium: scrollWidth 515 > 375) | `&nbsp;` giữa 2 span của H1 biến "EngFlow Premium" thành 1 token không wrap (491px); space thường trong template bị Vue compiler trim | Space nằm trong **chuỗi runtime** (`{{ prefix + ' ' }}`) + `break-words` + `min-w-0` → scrollWidth = 375 đúng viewport | `fix(ui)` |
| 6 | **Premium user bị chặn /speaking*** | Router guard đọc `auth.isPremium` nhưng auth store không định nghĩa (luôn undefined) — backend `UserResponse` có field này | Thêm `isPremium`/`premiumExpiry` vào mapping login/register/fetchUser + expose computed; verify: premium user thấy list 6 đề, detail + record studio mở | `fix(auth)` |

Cộng thêm (hardening do hook Mimosa bắt buộc, đã fix thật):
- `crawler/crawl.js`: SSRF allowlist guard (`assertSafeUrl` — chỉ http/https + host trong allowlist + chặn loopback/private), whitelist + slug-map literal cho level/skill, bỏ JSON.parse dữ liệu không tin cậy (resume qua sidecar text), path join không `..`.
- `PaymentServiceTest`: webhook secret test đổi từ literal sang `env hoặc UUID runtime`.

## 3. Sự cố / điểm chặn minh bạch

**Hook Mimosa chặn git commit**: quick-check trước commit flag `crawler/crawl.js` (heuristic taint: `fetchUrl` là SSRF entry, `cheerio.load` là "deserialize") — là **chức năng cốt lõi của crawler** có sẵn từ trước, không thuộc code của phiên. Tôi đã: fix thật mọi finding khả thi (mục 2), chạy **official deep scan → findings: 0** (seal `sha256:e2a39978…`), nhưng quick-check vẫn chặn mọi commit kể cả khi file không nằm trong commit. 5 commit đầu đã vào repo; phần còn lại nằm trong working tree chờ quyết định của user (tắt tạm hook / thêm ngoại lệ crawler/ hoặc tiếp tục loop với heuristic):
- `crawler/crawl.js` (hardening), `src/test/.../PaymentServiceTest.java` (secret env/UUID), `.specify/specs/audit-v4-full/*`, `.specify/memory/constitution.md` (v1.0.1), `scripts/audit-v4-api-sweep.mjs`, REPORT.md, tasks.md.

## 4. Chưa làm / còn tồn

1. **Chuyển khoản thật cho order `ENGF8AB9431CE85`**: chờ user — khi đã chuyển, nói tôi một câu là tôi confirm qua webhook HMAC (hoặc cấp `SEPAY_API_TOKEN` + webhook URL công khai thì auto).
2. **SePay infra**: token hợp lệ + webhook public URL — điều kiện để polling/webhook tự nhiên hoạt động.
3. **89/449 listening thiếu audio**: giữ nguyên từ v3 (fallback giọng máy đang chạy; re-seed cần MCP + GPU, out-of-scope phiên này theo spec).
4. **Quota AI của user@gmail.com đã cạn** (5/5 free) — UI hiện 403 có thông báo; hành vi by design (đã test sinh vocab bằng admin: PASS).
5. Test users còn lại trong DB (nhãn `AuditV4*`, disabled): 150015–150026 + order test SUCCESS — đã revoke/disable, không dọn xóa cứng (giữ audit trail).
6. Commit còn lại bị chặn (mục 3).

## 5. Loop 2 — kết quả sau toàn bộ fix (GREEN)

| Suite | Kết quả |
|---|---|
| Backend `mvnw.cmd test` | **221/221 PASS** (BUILD SUCCESS 46s) |
| Frontend `npx vitest run` | **73/73 PASS** (14 files) |
| `npx vite build` | sạch 9.5s |
| API sweep (67 checks) | **67/67 PASS** (2 vòng) |
| Payment E2E | **8/8 PASS** |
| UI regression | mobile premium 375px ✓ · speaking premium unlock ✓ (list→detail→record) · admin CRUD ✓ (create + delete 204) · AI vocab admin ✓ · games 6/6 ✓ |
| Docker | backend + frontend rebuild & verify sau từng fix |

## 6. Skills đã nạp khi test

- `prompt-master` — audit + vá 6 lỗ hỏng design-prompt (mục 0)
- `speckit-workflow`, `speckit-constitution`, `speckit-specify`, `speckit-clarify`, `speckit-checklist`, `speckit-plan`, `speckit-tasks`, `speckit-analyze` (giữa chừng, PASS) — pipeline constitution v1.0.1 → spec → clarify → checklist → plan → tasks → implement; converge = tasks.md đã cập nhật kết quả
- `browser-use:control-browser` — browser testing (thực thi bằng Playwright thư viện qua Node; chrome-devtools-mcp/playwright-mcp không có tool trong phiên này)
- Hook bảo mật Mimosa (plugin) — chặn code nguy hiểm khi ghi file/commit; dẫn tới hardening crawler + PaymentServiceTest + deep scan chính thức
- Kỹ năng sẵn có của harness: chạy Docker/sqlcmd/Playwright, AskUserQuestion cho gate payment

## 7. Kết luận

Audit-v4 đạt spec R1–R7: **backend 221/221, frontend 73/73, API 67/67 (2 vòng), payment E2E 8/8, UI verified trên browser thật 2 vòng, design system Be Vietnam Pro đồng bộ và đã vá 1 bug tràn ngang mobile thật + 5 bug backend/auth thật**. Mọi fix đều sửa tận gốc (root cause) và verify lại bằng HTTP/UI thật. Còn tồn duy nhất mang tính hạ tầng/người dùng: chuyển khoản thật cho order pending + SePay token/webhook, và commit bị hook Mimosa chặn (đã harden + official scan 0 findings, chờ user quyết).
