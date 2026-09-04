# Tasks — audit-v6-full

## Phase 0 — Nền tảng (DONE)
- [x] T0.1 Baseline: backend 247/247, frontend 73/73
- [x] T0.2 GET sweep 53 endpoint (admin+user token) — phân loại 2xx/404/405
- [x] T0.3 Spec artifacts: spec/clarify/checklist/plan (file này)
- [x] T0.4 Sub-agent static code audit (chờ kết quả)
- [x] T0.5 Sub-agent DB audit (chờ kết quả)
- [x] T0.6 Sub-agent design-system gap audit (chờ kết quả)

## Phase 1 — Tổng hợp phát hiện
- [x] T1.1 Reconcile 3 báo cáo sub-agent + api-sweep → danh sách F20–F31 có priority
- [x] T1.2 analyze.md — cross-check spec↔tasks↔code (PASS, 2 mục DoD ghi "chưa làm" có lý do)

## Phase 2 — DB + Perf
- [x] T2.1 Áp index `idx_exercises_lesson_type_order` + đo 7ms→0ms
- [x] T2.2 Data-quality: 5.434 thiếu correctAnswer → pipeline backfill tồn tại, chạy ngoài phiên (ghi REPORT §3.2); mồ côi FK = 0
- [x] T2.3 Config perf: hot endpoints <100ms warm, Redis 74/74 TTL, blocking 0

## Phase 3 — UI verify (browser thật)
- [x] T3.1 Font BVP: 22/22 trang nonBvp=0 (computed style)
- [x] T3.2 Design checklist: button/card/input/icon khớp spec (computed)
- [x] T3.3 Console 0 error/warning; mobile 375px không overflow
- [x] T3.4 prefers-reduced-motion media query toàn cục xác minh

## Phase 4 — E2E chức năng ↔ API
- [x] T4.1 CRUD lesson qua UI + verify DB (1469→1470→1469)
- [x] T4.2 CRUD exercise + speaking-prompt + video-lesson qua UI/API
- [x] T4.3 AI: ai-generate-full ✓, assess ✓ (9.0), ai-grade ✓, translate ✓, generate-async ✓, fetch-youtube ✗→F30 (giới hạn kiến trúc, REPORT §3.1)
- [x] T4.4 User flow: lesson submit 4/5 server-grade, deck quiz, shadowing view, premium checkout, record page
- [x] T4.5 Authz: user→403 admin, noauth→401, public→200

## Phase 5 — Fix
- [x] T5.1 Fix F20–F31 + phụ (null-guard update, @PreAuthorize, timeout, rate-limit mail, contrast, modal a11y, mobile shadow, media URL relative, chunking, quota enrich-word, aria-labels)
- [x] T5.2 Tái kiểm: 247/247 BE, 73/73 FE, build sạch, verify live từng fix

## Phase 6 — Converge + Báo cáo
- [x] T6.1 converge: REPORT §3 liệt kê rõ chưa làm + lý do; không task orphan
- [x] T6.2 REPORT.md tiếng Việt (đã làm / chưa / fix+cách / skill đã nạp)
- [ ] T6.3 Commit Conventional Commits
