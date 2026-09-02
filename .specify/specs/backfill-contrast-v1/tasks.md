# Tasks: backfill-contrast-v1

## 1. Accent AA (hoàn thành)

- [x] T.1 Grep toàn bộ `#8B5CF6` (9 file) → swap `#7C3AED` (tailwind.config.js 6 chỗ, design-system.css, main.css focus outline, 4 skill views, Lessons.vue + lessonLevels.js fallbacks)
- [x] T.2 Thêm `--geo-secondary-text #DB2777` + `--geo-tertiary-text #B45309`; stat-num dùng biến text-safe
- [x] T.3 Verify computed styles + contrast tính tay (5.70 white-on-accent, 4.51/4.93 stat text trên cream) + Lighthouse a11y 100
- [x] T.4 vitest 73/73 xanh sau swap

## 2. Backfill backend

- [x] T.5 Sample data shapes: MC rỗng = mảnh đề (392 letter-prefix / 562 placeholder / 386 slash / 4601 khác), FB hint `(verb)` 191/1171, placeholder ≤8 chars 13
- [x] T.6 Khám phá: answer key nằm trong `lesson.content` (details ANSWER) — chiến lược 3 lớp: deterministic parse → AI backfill → unfillable report
- [ ] T.7 `ExerciseRepository` + 2 method: `findByCorrectAnswerEmpty(Pageable)`, `countByCorrectAnswerEmpty()` (JPQL `correctAnswer IS NULL OR TRIM = ''`)
- [ ] T.8 `AiAnswerBackfillService`: per-lesson grouping ASC, checkpoint Redis-ish (in-memory map), BatchProgress riêng, lớp 1 parse answer key từ content (regex `<details>…ANSWER…</details>` → per-exercise mapping bằng position/order), lớp 2 Ollama `qwen2.5:1.5b` prompt grounded (question + lesson title), validation (FB ≤120 chars không placeholder; MC nếu có options thì answer ∈ options — corpus này options NULL nên MC ghi đáp án text), dry-run flag, retry 2, tx per lesson
- [ ] T.9 Endpoint `POST /api/admin/exercises/ai/backfill-answers` (dryRun, limit) + poll qua `/status?batchId=` tái dùng; unit tests cho service (parse answer key, validation, dry-run không save)
- [ ] T.10 `mvnw test` 197/197 xanh → rebuild container → pilot limit=50 → verify DB + đo thời gian
- [ ] T.11 Full run 7,076 bài → poll → verify: count rỗng giảm đúng số backfilled; sample 20 bài random đọc hợp lệ; unfillable report rõ
- [ ] T.12 Baseline re-test (197 + 73) + analyze + converge + báo cáo tiếng Việt

## Notes

- MC rỗng options NULL → UI render text input, chấm so text correctAnswer (LessonExerciseTab.checkAnswer) → backfill MC = điền text đáp án, không phá UI.
- Lesson 11795 minh họa: content có ANSWER key "1 b 2 a 3 b 4 b 5 a 6 a" + options "a ..." "b ..." inline trong content → lớp 1 deterministic đủ mạnh cho phần lớn lesson có ANSWER block.
- FB nhiều bài đáp án đã inline trong question (`…have been used…… (use)`) hoặc hint ngữ pháp `(get)` — lớp 2 AI tận dụng; hint `(verb)` → đáp án phải là dạng chia của verb trong ngoặc (validation: không chứa "(", dài ≤120).
- Không đụng MATCHING rỗng.
