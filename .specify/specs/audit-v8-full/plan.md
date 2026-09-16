# Plan — audit-v8-full

Workflow: constitution → specify → clarify → checklist → plan → tasks → implement → converge (+ analyze giữa chừng).

## Strategy

Lặp lại toàn bộ quy trình audit-v7 nhưng ở mức sâu hơn: mỗi endpoint/route được xác minh bằng
**hai nguồn độc lập** (HTTP thật + trình duyệt thật), và chạy **hai vòng** (vòng 1 tìm lỗi,
vòng 2 tái xác minh sau khi fix trên bản dựng cuối).

## Tooling thực tế

- Chromium qua `playwright-core` (toàn cục) + Node 24 → harness `sweep/v8/` (lib.js + p1/p2/p3a/p3b + ui/*).
- `mvnw.cmd test` / `npx vitest run` / `npx vite build`.
- `docker exec engflow-sqlserver sqlcmd` (runner `sweep/v8/sqlrun.py`), `docker exec engflow-redis redis-cli`.
- Skill đã nạp: karpathy-guidelines, java-coding-standards, accessibility, prompt-master,
  speckit-*, frontend-testing-debugging, security-diff-scan/fix-finding (hướng tiếp cận).

## Fixes đã triển khai (mỗi cái có regression test)

| ID | Lỗi | Fix | Test |
|---|---|---|---|
| F81 | stored XSS: upload giữ phần mở rộng do client chọn, `/api/resources/**` permitAll + same-origin → đánh cắp JWT | `SafeUploadNames`: allowlist lúc ghi + pin Content-Type (octet-stream + `Content-Disposition: attachment`) lúc đọc | `AuditV8UploadXssTest` (9) |
| F82 | `POST /api/games/submit` cast mù sessionId/correctAnswers/answers → ClassCastException 500 | validate kiểu, trả 400 kèm thông báo tiếng Việt | `GameControllerSubmitTypeTest` (7) |
| F83 | bucket `:order` chết (prefix không khớp route thật) | `startsWith("/api/v1/payment")` | 2 test trong `RateLimitFilterTest` (5 → 7) |
| F84 | `generateAll` không coi count là trần; `count<5` sinh yêu cầuListening(-1) | per-type budget + trim vòng tròn; `Math.max(1, count - 4*perType)` | `AiExerciseServiceParsingTest` +2 |
| F85 | MC trùng đáp án được accept ("best","best") | guard "options must be distinct" | `AiExerciseServiceParsingTest` +1 |
| F86 | `ai-generate` prompt nhận topic rỗng → gọi Ollama, sinh nhảm | guard 400 như ai-generate-full | p3a (sống) |
| F87 | admin exercise page 95k logical reads/trang (kéo content/content_original) | bo JOIN FETCH + `LessonTitle` projection gộp | `ExerciseServiceAdminPaginationTest` cập nhật |
| UI-1 | header đăng nhập tràn ngang 1280–1535px (13–101px) | band nén `@media (min-width:1280px) and (max-width:1535.98px)` | đo lại 360→1920px, 0 overflow |
| UI-2 | ảnh trong lesson-content không alt (WCAG 1.1.1) | DOMPurify hook `afterSanitizeElements` (lazy, không phình entry) | `sanitize-a11y.test.js` (3) |

## Perf DB (đo trước/sau)

- PK `exercises`: 13.95% → 0.86% fragmentation, 1369 → 1283 pages.
- `idx_exercises_lesson_type_order`: 46.19% → 2.49%, 565 → 441 pages.
- Endpoint chậm nhất còn lại: dictionary proxy 19.9s cold → 0.01–0.07s warm (Redis cache, TTL 1h — thiết kế).
- `GET /api/admin/exercises`: 367ms/95k reads → 30.6ms/27k reads; 5 endpoint listing 1990→374→<30ms diện rộng.

## Boundaries đã tuân thủ

Không viết migration (schema do Hibernate); không seed/xóa dữ liệu demo; không thêm dependency;
`.env`/key không commit; backup-before-DML cho phiên mutate (chỉ REBUILD INDEX + DELETE row của chính mình).
