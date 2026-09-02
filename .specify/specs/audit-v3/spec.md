# Spec: audit-v3 — Kiểm tra toàn diện lần 3 (chuẩn hơn)

## Problem
Hai vòng audit trước (audit-v1/v2, audit-redesign-v1) đã pass nhưng dự án vẫn còn lỗi
tiềm ẩn: backend test baseline RED khi chạy `mvnw.cmd test` trên máy mới (ApplicationContext
failure), và chưa lần nào chạy UI test có **human-in-the-loop cho payment premium**.

## Goal
Kiểm tra SÂU và TOÀN DIỆN hơn 2 vòng trước: API thật + UI thật + DB thật, lặp lại nhiều lượt,
sửa tận gốc mọi bug tìm thấy. Không giới hạn thời gian.

## Scope

### In scope
1. **Codebase scan**: backend (22 controller, 40 service), frontend (Vue 3, services layer),
   design system compliance.
2. **API testing**: chạy TẤT CẢ endpoints qua HTTP thật (:8080) — auth, lessons, exercises,
   admin, speaking, video, payment, games, decks, flashcards, SRS, vocab, progress, AI, snapshot,
   media. Verify status codes + payload shape + error paths (401/403/404/409/400).
3. **UI testing** (chrome-devtools-mcp + playwright-mcp): mọi chức năng chính user-facing:
   Home, Auth, Lessons, Lesson detail (content/exercises/submit), Videos (YouTube player),
   Decks, Flashcards, SRS, Games, Vocabulary, AI vocab, Dashboard, Profile, Premium/Payment,
   Admin (stats/users/lessons/exercises/speaking/ai-gen).
4. **DB trong Docker**: schema check, orphan check, index coverage, slow query, size/log growth,
   mojibake re-check.
5. **Hiệu năng**: đo latency trước/sau khi tối ưu; frontend bundle; DB indexes.
6. **Payment premium**: create-order → QR hiển thị → **DỪNG, nhắc user chuyển khoản thủ công
   bằng tay qua SePay** → poll status → verify premium activation. Đây là BẮT BUỘC
   human-in-the-loop.
7. **Prompt defect scan** (đã xong ở prompt-master skill): 5 lỗ hổng → 5 bản vá.
8. **Frontend tests + build**, backend tests — phải xanh sau mọi thay đổi.
9. **Taskstoissues**: sau khi xong, tạo GitHub issues cho bug còn tồn.

### Out of scope
- Deploy production, đổi cloud AI provider, thêm feature mới, migration Flyway.
- Đổi design system ngoài việc verify compliance (font Be Vietnam Pro đã áp dụng ở vòng trước).

## Acceptance criteria
- AC1: Backend `mvnw.cmd test` xanh, ≥ baseline 221+ tests, 0 context-failure.
- AC2: Frontend `npx vitest run` xanh 73+ tests; `npx vite build` clean.
- AC3: API audit: mọi endpoint có verify result (PASS/FAIL + note); mọi FAIL được fix hoặc
  có GitHub issue.
- AC4: UI: mỗi chức năng chính có byproduct (screenshot/console-log sạch) — 0 console error
  không được giải thích.
- AC5: Payment: user đã chuyển khoản thật, order status chuyển `PAID`, premium được activate,
  UI phản ánh đúng.
- AC6: DB: 0 orphan, FK index coverage đầy đủ, ghi nhận kích thước, mọi tối ưu có số đo
  trước/sau (constitution P5).
- AC7: REPORT.md chi tiết: đã làm / chưa làm / đã fix + cách fix / skills đã nạp.
- AC8: GitHub issues tạo cho bug còn tồn (taskstoissues).
- AC9: Font: grep 0 `Outfit`, 0 `Plus Jakarta Sans` trong frontend source.
