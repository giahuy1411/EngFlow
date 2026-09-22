# audit-v13 — Documentation drift pass (2026-09-22)

**Trigger:** câu hỏi của người dùng "còn cần làm gì nữa không". `/code-review` không chạy được
(repo có **0 pull request**, `gh` chưa cài) → chuyển sang rà **drift tài liệu** — lớp lỗi
đã lặp lại nhiều kỳ audit (xem commit `93d44e5 docs(agents): correct two stale claims`).

**Phương pháp:** mỗi con số trong tài liệu được **đo lại** từ nguồn thật (DB live / `find` / grep
entity / `vitest` / `vite build`) trước khi sửa. Không sửa theo trí nhớ.

---

## README.md — 10 claim sai, đã sửa

| # | Claim cũ | Đo thật | Nguồn đo |
|---|---|---|---|
| 1 | `19 tables` (liệt kê 22 tên) | **21 bảng** | `INFORMATION_SCHEMA.TABLES` |
| 2 | có `flashcards`, `flashcard_reviews`, `leaderboard_entries` | **không tồn tại** | `INFORMATION_SCHEMA.TABLES` |
| 3 | thiếu `study_days`, `study_policy` | **có thật** | như trên |
| 4 | `87 .vue files` (dưới `views/`) | **47** | `find frontend/src/views -name '*.vue'` |
| 5 | `Vitest (119 tests / 23 files)` | **175 / 30** | `npx vitest run` |
| 6 | `requires JDK 17+` | **JDK 25** | `pom.xml` `<java.version>25</java.version>` |
| 7 | `Swagger: .../swagger-ui.html` | **không tồn tại** | không có `springdoc` trong `pom.xml` |
| 8 | `POST /api/admin/ai/generate-exercise` | **`/api/admin/exercises/ai/generate`** | `AdminAiExerciseController:26,51` |
| 9 | `4 CEFR levels` | **Elementary/Pre-Int/Int/Upper-Int** | `LessonLevel.java` |
| 10 | `SePay VNPAY gateway` | **chỉ SePay** — 0 ref VNPAY | grep `VNPAY` toàn `src/main` |
| 11 | `Z_AI_API_KEY`, `OLLAMA_BASE_URL`, `CLOUDINARY_URL`, `REDIS_PASSWORD` | **không app nào đọc** | `application.properties` `${...}` |
| 12 | Games thiếu `Listening` | có `ListeningGame.vue` | `ls views/luyentu/` |

## docs/erd-sql-guide.md — 5 claim sai, đã sửa

| # | Claim cũ | Đo thật |
|---|---|---|
| 1 | `19 bảng` (bỏ sót `study_days`, `study_policy`) | **21 bảng** |
| 2 | `24 foreign keys` | **25** |
| 3 | `exercises_bak_v5*` (4 bảng backup) | **không còn tồn tại** |
| 4 | `user_streaks` là bảng streak | **legacy** — 0 entity, 0 reader |
| 5 | Số liệu §7 (users 70, vocab 127, decks 14, …) | users 72, vocab 118, decks 10, study_days 3 … |

Bổ sung: DDL + dbdiagram cho `study_days` / `study_policy`; đánh dấu `user_streaks` legacy;
mẫu attempt sai (`30045` không tồn tại, PK là `attempt_id`) → **40102**.

## .gitignore — rò rỉ tiềm ẩn, đã chặn

`tmp/` **không** nằm trong `.gitignore` và chứa **15 file có JWT sống**
(`admin.token`, `user.token`, `*_login.json`). Đã thêm `tmp/` + comment giải thích.
→ untracked 477 → **132** (phần còn lại là artifact audit cố ý).

> Đã kiểm: **0 file đang tracked** chứa JWT (`git grep 'eyJ[A-Za-z0-9_-]\{20,\}'` = rỗng),
> và 9 file `tmp/*.sql` đã tracked từ trước đều là SQL vô hại (không secret).

## Cổng kiểm chứng lại sau mọi sửa

| Cổng | Kết quả |
|---|---|
| Backend `mvnw.cmd -o test` | **523 / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| Frontend `vitest run` | **175 passed / 1 skipped (30 files)** |
| `vite build` | `index-B1jcJfQd.js` **177.76 kB** (gzip 67.67) — đo 2 lần, ổn định |
| Parity `p16-parity.sql` | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — khớp |
| `ck_study_policy_singleton` | còn (1); `dbo.hb_check_probe` = 0 (đã dọn) |

**AGENTS.md:** cập nhật build size 177.73 → **177.76 kB**.

---

## ⚠️ Cảnh báo commit (P1, chưa xử lý — cần người dùng quyết định)

**12 file untracked**, trong đó `frontend/src/utils/safeRedirect.js` được
`router/index.js`, `Login.vue`, `Register.vue`, `PremiumCheckout.vue` import.
`git commit -am` **bỏ sót** → bản clone sạch `npm run build` **hỏng**.

```
docs/lesson-builder-status.md
frontend/src/utils/safeRedirect.js                     ← import bởi 3 file
frontend/src/utils/safeRedirect.test.js
frontend/src/views/__tests__/contrast-ink-tokens.test.js
frontend/src/views/admin/AdminLessonBuilder.f13.test.js
frontend/src/views/lessons/LessonExerciseTab.f13.test.js
frontend/src/views/lessons/LessonLayout.h1.test.js
src/test/java/.../model/entity/StudyPolicySingletonGuardTest.java
src/test/java/.../service/AdminServiceDashboardStreakSourceTest.java
src/test/java/.../service/ExerciseServiceMultipleChoiceOptionsTest.java
src/test/java/.../service/LessonStructureBlockTypeGuardTest.java
tasks/streak-study/fix-study-policy-check.sql
```
