# audit-v20 — Danh sách file cần bổ sung Javadoc tiếng Việt

## Backend — 20 file (74 public method thiếu Javadoc)

| # | File (dưới `src/main/java/com/datn/engflow/`) | Số method thiếu |
|---|---|---|
| 1 | `service/AiExerciseService.java` | 13 |
| 2 | `controller/speaking/SpeakingPromptController.java` | 8 |
| 3 | `service/ExerciseService.java` | 6 |
| 4 | `service/AiAnswerBackfillService.java` | 5 |
| 5 | `security/UserPrincipal.java` | 5 |
| 6 | `controller/GameController.java` | 5 |
| 7 | `controller/AdminAiExerciseController.java` | 5 |
| 8 | `controller/LessonController.java` | 4 |
| 9 | `controller/AdminExerciseController.java` | 4 |
| 10 | `controller/LessonSubmissionController.java` | 3 |
| 11 | `service/StreakReminderScheduler.java` | 2 |
| 12 | `controller/payment/PaymentController.java` | 2 |
| 13 | `controller/VocabularyController.java` | 2 |
| 14 | `controller/SrsController.java` | 2 |
| 15 | `controller/AiVocabController.java` | 2 |
| 16 | `controller/AdminUploadController.java` | 2 |
| 17 | `security/RateLimitFilter.java` | 1 |
| 18 | `controller/StreakController.java` | 1 |
| 19 | `controller/FlashcardController.java` | 1 |
| 20 | `config/WebConfig.java` | 0 (class đã comment-out — CHỈ thêm comment giải thích, KHÔNG sửa code) |

## Frontend — file thiếu comment

### Services (36 .js, 10 thiếu)
`services/authService.js`, `services/dashboardService.js`, `services/deckService.js`,
`services/leaderboardService.js`, `services/paymentService.js`, `services/speakingService.js`,
`services/streakService.js`, `composables/useSpeakingRecorder.js`, `main.js`,
`components/decor/index.js`

### Views/components không có comment (30 .vue)
`App.vue`, `components/common/GuestCtaCard.vue`, `components/common/Pagination.vue`,
`components/common/UserPageHeader.vue`, `components/decor/DecoShape.vue`,
`components/decor/DotBackground.vue`, `components/layout/PageHeader.vue`,
`components/layout/PageSection.vue`, `components/layout/StatCard.vue`,
`components/ui/AppEmptyState.vue`, `components/ui/AppErrorState.vue`,
`components/ui/AppSkeleton.vue`, và 18 file khác (liệt kê đầy đủ bằng `fe_doc_gap.py`).

## Quy tắc comment (bắt buộc)

1. **Tiếng Việt dễ hiểu**, giải thích *tại sao* và *luồng hoạt động*, không dịch máy móc.
2. Javadoc/JSDoc cho **mỗi class + method public**.
3. Comment cho **block logic không hiển nhiên**: guard, công thức, ràng buộc thứ tự, gotcha.
4. **KHÔNG** comment kiểu `i++; // tăng i`.
5. **TUYỆT ĐỐI KHÔNG đổi code** — chỉ thêm comment. (Bài học F-20-06: một lần thêm comment đã xoá mất dòng class.)
6. Với `.vue`: comment ngay sau `<script setup>` mô tả mục đích view + guard + luồng dữ liệu.
7. Giữ đúng vị trí: Javadoc **TRƯỚC** annotation, không phải sau.
