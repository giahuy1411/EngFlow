# Tasks — audit-v4-full (KẾT QUẢ — converge 2026-09-03)

## Phase A — Baseline & DB sweep
- [x] T1 Backend `mvnw.cmd test` → **221/221 PASS** (chạy lại sau mọi fix: PASS)
- [x] T2 Frontend `npx vitest run` **73/73** + `vite build` sạch 9.5s (chạy lại sau fix auth store: PASS)
- [x] T3 DB sweep: varchar còn lại = enum/URL (hợp lệ); `?` legacy = 0 rows; orphan 0; index phủ đủ query nóng

## Phase B — API full sweep
- [x] T4 **67/67 PASS** (chạy 2 lần: lần đầu 43/52 → fix → 64/66 → fix → 67/67; loop 2: 67/67)
  - Script bug đã sửa: path `/api/lessons` (không phải /api/v1), Page `.content`, LessonLevel enum, FlashcardReviewRequest {vocabularyId,isKnown}, video attempts là multipart, webhook contract 200+success:false
  - **Bug app tìm thấy & fix**: games 200-rỗng cho deck không tồn tại; video attempt JSON → 500; snapshot NPE; MinIO stale credentials (infra)

## Phase C — Browser UI sweep
- [x] T5 User views: home/lessons/lesson+exercises/video+embed/decks/6 games/search/leaderboard/profile/speaking — PASS (design check Be Vietnam Pro mọi trang)
- [x] T6 Admin views: dashboard/users/lessons CRUD/exercises/videos/video-attempts/speaking×2 — PASS (delete verified: DELETE 204, chỉ đúng hàng nhãn)
  - **Bug app tìm thấy & fix**: H1 `&nbsp;` gây tràn ngang mobile 375px (scrollWidth 515→375)
  - **Bug app tìm thấy & fix**: auth store thiếu isPremium → premium user bị chặn /speaking (guard đọc undefined)

## Phase D — Premium payment ⚠ GATE
- [x] T7 E2E backend **8/8 PASS** (user test): create-order → webhook sai chữ ký bị chặn → webhook HMAC đúng → status isPremium=true + expiry đúng 1 tháng → admin list khớp → idempotent → revoke sạch. DB proof: transaction SUCCESS.
  - Order thật `ENGF8AB9431CE85` (user@gmail.com, 10.000đ MBBank) giữ **PENDING** chờ user chuyển thật — đã nhắc 2 lần qua AskUserQuestion, user không có mặt; KHÔNG giả lập confirmed trên tài khoản thật.
  - Infra còn thiếu để auto-confirm: public webhook URL + SEPAY_API_TOKEN hợp lệ.

## Phase E — Perf & bugfix
- [x] T8 payment.status: **22–34ms** (v3 đo 3.5s) — circuit breaker v3 đã chặn gọi SePay 401; đo before/after đủ bằng chứng P5
- [x] T9 Fix tận gốc (6 nhóm): GameService 404 · GlobalExceptionHandler 400 · LessonSnapshotService NPE/404 · MinIO stale creds (recreate container) · UserPageHeader mobile overflow · auth store isPremium
  - Kèm hardening cộng thêm: crawler SSRF allowlist + argv whitelist + bỏ JSON.parse resume; PaymentServiceTest secret qua env/UUID

## Phase F — Loop 2 + converge + report
- [x] T10 Loop 2: backend 221/221 · frontend 73/73 · API 67/67 · UI regression (speaking premium unlock 6 anchors, detail+record OK, AI vocab admin OK, mobile premium không tràn)
- [x] T11 converge: mọi R1–R7 của spec có bằng chứng; task phát hiện thêm: MinIO creds, crawler hardening, auth isPremium (đã liệt kê ở trên)
- [x] T12 REPORT.md — xem `.specify/specs/audit-v4-full/REPORT.md`
  - ⚠ Commit bị hook Mimosa chặn cho crawler/crawl.js + PaymentServiceTest + artifacts (official deep scan: 0 findings, seal `e2a39978...`; quick-check heuristic vẫn flag chức năng cốt lõi của crawler) — các thay đổi nằm an toàn trong working tree chờ quyết định của user.
