# Spec: Kiểm thử & Tối ưu toàn diện EngFlow (Audit V1)

**Ngày:** 2026-09-01 | **Trạng thái:** specified

## Mục tiêu

Kiểm tra toàn diện dự án EngFlow hiện trạng: backend API, DB trong Docker,
design system frontend, hiệu năng, và tinh chỉnh nơi cần thiết — KHÔNG phá
baseline xanh và KHÔNG thêm tính năng mới.

## Yêu cầu (Requirements)

### R1 — Quét codebase
- R1.1 Kiểm kê controller (25), service (40), entity (21) đã nêu trong AGENTS.md.
- R1.2 Xác nhận không có module chết được tham chiếu (lessons, decks, games,
  speaking, video, payment).
- R1.3 Kiểm tra leak: `.env`, key, `.wav` public, secret trong code.

### R2 — Backend API smoke test toàn diện
- R2.1 Auth: login user + admin (`/api/auth/login`), token shape đúng
  `data.data.token || data.token`.
- R2.2 CRUD chính: lessons, exercises, decks, flashcards, vocabulary,
  video-lessons, speaking prompts/submissions, admin endpoints.
- R2.3 AI: `/api/ai/...` sinh bài (Ollama 1.5b), ai-vocab, speaking rubric
  không được chết cứng; async 202 + Redis progress được kiểm chứng.
- R2.4 Kiểm tra response shape từng API (v/v `.data` vs plain).

### R3 — DB trong Docker + hiệu năng
- R3.1 `engflow-sqlserver` healthy, kết nối được qua sqlcmd :1434.
- R3.2 Bảng, index, ràng buộc nguyên vẹn; các cột NOT NULL hợp lệ.
- R3.3 Chỉ số hiệu năng: thời gian API p50, query chậm (đọc Hibernate stats /
  logging), full-table-scan trên các query chính.
- R3.4 Tối ưu có đo: chỉ thêm index/sửa query khi có bằng chứng EXPLAIN hoặc
  thời gian đo được, giữ baseline test xanh.

### R4 — Frontend + design system verification
- R4.1 Font **Be Vietnam Pro** áp dụng toàn cục (đã thấy trong tailwind.config
  + design-system.css + index.html) — nhưng **vá 2 lỗ hổng**: duplicate
  `fontFamily`/`borderRadius` keys trong tailwind.config.js, và gỡ Plus
  Jakarta Sans khỏi index.html (font bị thay thế, nạp thừa = bandwidth).
- R4.2 Kiểm tra bằng chrome-devtools-mcp + playwright-mcp trên các trang chính:
  Home, Login, Lessons, LessonContent, Decks, Speaking, VideoLibrary,
  Premium, Admin Dashboard, Leaderboard.
- R4.3 A11y cơ bản: lang=vi, focus-visible, skip-link, contrast (audit qua
  Lighthouse accessibility trong chrome-devtools).
- R4.4 Responsive: mobile 375px kiểm tra các trang chính.
- R4.5 Console errors = 0 trên các trang đã kiểm; hiệu năng bundle/build.

### R5 — Vá lỗ hổng prompt
- R5.1 Prompt gốc yêu cầu "thay toàn bộ font bằng Be Vietnam Pro" — đã
  thực hiện đúng; các lỗ hổng còn lại được vá (R4.1).
- R5.2 Prompt gốc mô tả "Max 1 accent color" vs design system chơi màu
  rotationally (secondary/tertiary/quaternary) — đây là design system gốc,
  giữ nguyên (documented exception).

## Clarifications

### Session 2026-09-01
- Q: Cho rebuild backend container khi tối ưu cần sửa code? → A: **Có, rebuild** (downtime ~2-3 phút chấp nhận được, phải verify lại 196 tests).
- Q: Mức độ smoke AI? → A: **Real 1 lần mỗi loại** (1 exercise generation + 1 ai-vocab thật + endpoint health cho phần còn lại).

## Phạm vi KHÔNG làm

- Không rebuild container backend trừ khi sửa code backend (chỉ rebuild khi
  có thay đổi thật).
- Không seed data demo mới, không tái tạo achievements.
- Không thêm dependency mới, không migration file.
- Không tối ưu DB khi không có bằng chứng.

## Acceptance Criteria

1. Backend 196/196 + frontend 73/73 test xanh sau mọi thay đổi.
2. Báo cáo API smoke: mọi endpoint chính trả 2xx/đúng shape; lỗi được liệt kê.
3. Báo cáo DB: index/size/query-time của các bảng chính; tối ưu nếu có.
4. Báo cáo UI: screenshots, console errors=0, font Be Vietnam Pro verified,
   a11y pass cơ bản.
5. Diff code (nếu có) chỉ là vá lỗ hổng font/config + tối ưu có bằng chứng.
