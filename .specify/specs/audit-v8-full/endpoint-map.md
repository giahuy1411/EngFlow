# Endpoint map — audit-v8-full (Round 1)

> Sinh tự động từ SOURCE: `sweep/v8/endpoint_inventory.py` → `evidence/endpoint-inventory.json`.
> Auth/role suy ra bằng cách áp **đúng thứ tự khai báo** của `SecurityConfig.java:76-118` (Spring chọn rule KHỚP ĐẦU TIÊN — đây chính là bug class của audit-v7 F54).
> Bucket suy ra bằng cách áp **đúng thứ tự if/else** của `RateLimitFilter.java:47-75`.

**Tổng số mapping**: 144 · **controller**: 26 · **đã probe**: 144/144

**Phân bố HTTP method**: `DELETE`=10, `GET`=61, `PATCH`=3, `POST`=54, `PUT`=16


## Bảng mapping

| # | Method | Path | Controller | Auth rule | Rate-limit bucket | Probed |
|---|---|---|---|---|---|---|
| 1 | `POST` | `/api/admin/exercises/ai/generate` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 2 | `POST` | `/api/admin/exercises/ai/generate-all` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 3 | `POST` | `/api/admin/exercises/ai/generate-async` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 4 | `POST` | `/api/admin/exercises/ai/generate-batch` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 5 | `GET` | `/api/admin/exercises/ai/status` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 6 | `POST` | `/api/admin/exercises/ai/validate` | AdminAiExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 7 | `POST` | `/api/admin/exercises/ai/backfill-answers` | AdminAnswerBackfillController | ROLE_ADMIN | `:global` (100/min) | yes |
| 8 | `GET` | `/api/admin/exercises/ai/backfill-answers/status` | AdminAnswerBackfillController | ROLE_ADMIN | `:global` (100/min) | yes |
| 9 | `GET` | `/api/admin/lessons` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 10 | `POST` | `/api/admin/lessons` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 11 | `DELETE` | `/api/admin/lessons/{id}` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 12 | `GET` | `/api/admin/lessons/{id}` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 13 | `PUT` | `/api/admin/lessons/{id}` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 14 | `PUT` | `/api/admin/lessons/{id}/toggle-publish` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 15 | `GET` | `/api/admin/stats` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 16 | `GET` | `/api/admin/users` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 17 | `PUT` | `/api/admin/users/{id}/revoke-premium` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 18 | `PUT` | `/api/admin/users/{id}/toggle-active` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 19 | `PUT` | `/api/admin/users/{id}/toggle-admin` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 20 | `PUT` | `/api/admin/users/{id}/toggle-premium` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 21 | `GET` | `/api/admin/vocabulary` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 22 | `POST` | `/api/admin/vocabulary` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 23 | `DELETE` | `/api/admin/vocabulary/{id}` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 24 | `PUT` | `/api/admin/vocabulary/{id}` | AdminController | ROLE_ADMIN | `:global` (100/min) | yes |
| 25 | `GET` | `/api/admin/exercises` | AdminExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 26 | `POST` | `/api/admin/exercises` | AdminExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 27 | `DELETE` | `/api/admin/exercises/{id}` | AdminExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 28 | `GET` | `/api/admin/exercises/{id}` | AdminExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 29 | `PUT` | `/api/admin/exercises/{id}` | AdminExerciseController | ROLE_ADMIN | `:global` (100/min) | yes |
| 30 | `POST` | `/api/admin/exercises/seed` | AdminExerciseSeedController | ROLE_ADMIN | `:global` (100/min) | yes |
| 31 | `POST` | `/api/ai/enrich-word` | AiVocabController | authenticated | `:ai` (10/min) | yes |
| 32 | `POST` | `/api/ai/generate-vocab` | AiVocabController | authenticated | `:ai` (10/min) | yes |
| 33 | `POST` | `/api/ai/save-vocab` | AiVocabController | authenticated | `:ai` (10/min) | yes |
| 34 | `PUT` | `/api/auth/avatar` | AuthController | authenticated | `:global` (100/min) | yes |
| 35 | `POST` | `/api/auth/avatar/upload` | AuthController | authenticated | `:global` (100/min) | yes |
| 36 | `POST` | `/api/auth/change-password` | AuthController | authenticated | `:global` (100/min) | yes |
| 37 | `POST` | `/api/auth/forgot-password` | AuthController | permitAll | `:mail` (5/min) | yes |
| 38 | `POST` | `/api/auth/login` | AuthController | permitAll | `:auth` (20/min) | yes |
| 39 | `GET` | `/api/auth/me` | AuthController | authenticated | `:global` (100/min) | yes |
| 40 | `POST` | `/api/auth/register` | AuthController | permitAll | `:auth` (20/min) | yes |
| 41 | `POST` | `/api/auth/reset-password` | AuthController | permitAll | `:mail` (5/min) | yes |
| 42 | `GET` | `/api/dashboard/stats` | DashboardController | authenticated | `:global` (100/min) | yes |
| 43 | `GET` | `/api/decks` | DeckController | permitAll | `:global` (100/min) | yes |
| 44 | `POST` | `/api/decks` | DeckController | authenticated | `:global` (100/min) | yes |
| 45 | `GET` | `/api/decks/my` | DeckController | permitAll | `:global` (100/min) | yes |
| 46 | `DELETE` | `/api/decks/{id}` | DeckController | authenticated | `:global` (100/min) | yes |
| 47 | `GET` | `/api/decks/{id}` | DeckController | permitAll | `:global` (100/min) | yes |
| 48 | `PUT` | `/api/decks/{id}` | DeckController | authenticated | `:global` (100/min) | yes |
| 49 | `POST` | `/api/decks/{id}/words` | DeckController | authenticated | `:global` (100/min) | yes |
| 50 | `POST` | `/api/flashcards/review` | FlashcardController | authenticated | `:global` (100/min) | yes |
| 51 | `GET` | `/api/flashcards/status/{vocabularyId}` | FlashcardController | authenticated | `:global` (100/min) | yes |
| 52 | `GET` | `/api/games/listening/{deckId}` | GameController | authenticated | `:global` (100/min) | yes |
| 53 | `GET` | `/api/games/memory/{deckId}` | GameController | authenticated | `:global` (100/min) | yes |
| 54 | `GET` | `/api/games/mixed/{deckId}` | GameController | authenticated | `:global` (100/min) | yes |
| 55 | `GET` | `/api/games/quiz/{deckId}` | GameController | authenticated | `:global` (100/min) | yes |
| 56 | `POST` | `/api/games/submit` | GameController | authenticated | `:global` (100/min) | yes |
| 57 | `GET` | `/api/games/typing/{deckId}` | GameController | authenticated | `:global` (100/min) | yes |
| 58 | `GET` | `/api/leaderboard` | LeaderboardController | permitAll | `:global` (100/min) | yes |
| 59 | `GET` | `/api/lessons` | LessonController | permitAll | `:global` (100/min) | yes |
| 60 | `POST` | `/api/lessons` | LessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 61 | `DELETE` | `/api/lessons/{id}` | LessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 62 | `GET` | `/api/lessons/{id}` | LessonController | permitAll | `:global` (100/min) | yes |
| 63 | `PUT` | `/api/lessons/{id}` | LessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 64 | `GET` | `/api/lessons/{lessonId}/exercises` | LessonExerciseController | permitAll | `:global` (100/min) | yes |
| 65 | `GET` | `/api/lessons/{lessonId}/exercises/attempts` | LessonExerciseController | authenticated | `:global` (100/min) | yes |
| 66 | `GET` | `/api/lessons/{lessonId}/exercises/attempts/{attemptId}` | LessonExerciseController | authenticated | `:global` (100/min) | yes |
| 67 | `GET` | `/api/lessons/{lessonId}/exercises/content` | LessonExerciseController | permitAll | `:global` (100/min) | yes |
| 68 | `POST` | `/api/lessons/{lessonId}/exercises/grade` | LessonExerciseController | authenticated | `:global` (100/min) | yes |
| 69 | `POST` | `/api/lessons/{lessonId}/exercises/submit` | LessonExerciseController | authenticated | `:global` (100/min) | yes |
| 70 | `GET` | `/api/admin/lessons/{lessonId}/snapshots` | LessonSnapshotController | ROLE_ADMIN | `:global` (100/min) | yes |
| 71 | `POST` | `/api/admin/lessons/{lessonId}/snapshots` | LessonSnapshotController | ROLE_ADMIN | `:global` (100/min) | yes |
| 72 | `POST` | `/api/admin/lessons/{lessonId}/snapshots/{snapshotId}/restore` | LessonSnapshotController | ROLE_ADMIN | `:global` (100/min) | yes |
| 73 | `POST` | `/api/admin/audio-upload` | LessonStructureController | ROLE_ADMIN | `:upload` (15/min) | yes |
| 74 | `DELETE` | `/api/admin/blocks/{blockId}` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 75 | `PUT` | `/api/admin/blocks/{blockId}` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 76 | `POST` | `/api/admin/lessons/{lessonId}/sections` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 77 | `GET` | `/api/admin/lessons/{lessonId}/structure` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 78 | `DELETE` | `/api/admin/sections/{sectionId}` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 79 | `PUT` | `/api/admin/sections/{sectionId}` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 80 | `POST` | `/api/admin/sections/{sectionId}/blocks` | LessonStructureController | ROLE_ADMIN | `:global` (100/min) | yes |
| 81 | `POST` | `/api/admin/upload` | LessonStructureController | ROLE_ADMIN | `:upload` (15/min) | yes |
| 82 | `GET` | `/api/lessons/{lessonId}/structure` | LessonStructureController | permitAll | `:global` (100/min) | yes |
| 83 | `GET` | `/api/resources/{filename:.+}` | LessonStructureController | permitAll | `:global` (100/min) | yes |
| 84 | `GET` | `/api/lesson-submissions/my/lesson/{lessonId}/skill/{skillType}` | LessonSubmissionController | authenticated | `:global` (100/min) | yes |
| 85 | `POST` | `/api/lesson-submissions/submit` | LessonSubmissionController | authenticated | `:global` (100/min) | yes |
| 86 | `POST` | `/api/lesson-submissions/upload-audio` | LessonSubmissionController | authenticated | `:global` (100/min) | yes |
| 87 | `GET` | `/api/v1/media/**` | MediaProxyController | permitAll | `:global` (100/min) | yes |
| 88 | `POST` | `/api/v1/payment/create-order` | PaymentController | authenticated | `:order` (10/min) | yes |
| 89 | `GET` | `/api/v1/payment/status` | PaymentController | authenticated | `:global` (100/min) | yes |
| 90 | `POST` | `/api/webhook/sepay` | PaymentController | permitAll | `:global` (100/min) | yes |
| 91 | `GET` | `/api/users/progress` | ProgressController | authenticated | `:global` (100/min) | yes |
| 92 | `GET` | `/api/v1/admin/speaking-prompts` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 93 | `POST` | `/api/v1/admin/speaking-prompts` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 94 | `POST` | `/api/v1/admin/speaking-prompts/ai-generate` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 95 | `POST` | `/api/v1/admin/speaking-prompts/ai-generate-full` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 96 | `DELETE` | `/api/v1/admin/speaking-prompts/{id}` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 97 | `PUT` | `/api/v1/admin/speaking-prompts/{id}` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 98 | `GET` | `/api/v1/admin/video-prompts` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 99 | `POST` | `/api/v1/admin/video-prompts` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 100 | `POST` | `/api/v1/admin/video-prompts/ai-generate` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 101 | `POST` | `/api/v1/admin/video-prompts/ai-generate-full` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 102 | `DELETE` | `/api/v1/admin/video-prompts/{id}` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 103 | `PUT` | `/api/v1/admin/video-prompts/{id}` | SpeakingPromptController | ROLE_ADMIN | `:global` (100/min) | yes |
| 104 | `GET` | `/api/v1/speaking-prompts` | SpeakingPromptController | permitAll | `:global` (100/min) | yes |
| 105 | `GET` | `/api/v1/speaking-prompts/{id}` | SpeakingPromptController | permitAll | `:global` (100/min) | yes |
| 106 | `GET` | `/api/v1/video-prompts` | SpeakingPromptController | permitAll | `:global` (100/min) | yes |
| 107 | `GET` | `/api/v1/video-prompts/{id}` | SpeakingPromptController | permitAll | `:global` (100/min) | yes |
| 108 | `GET` | `/api/v1/admin/speaking-submissions` | SpeakingSubmissionController | ROLE_ADMIN | `:global` (100/min) | yes |
| 109 | `PATCH` | `/api/v1/admin/speaking-submissions/{id}/grade` | SpeakingSubmissionController | ROLE_ADMIN | `:global` (100/min) | yes |
| 110 | `GET` | `/api/v1/admin/video-submissions` | SpeakingSubmissionController | ROLE_ADMIN | `:global` (100/min) | yes |
| 111 | `PATCH` | `/api/v1/admin/video-submissions/{id}/grade` | SpeakingSubmissionController | ROLE_ADMIN | `:global` (100/min) | yes |
| 112 | `GET` | `/api/v1/speaking-prompts/{promptId}/submissions` | SpeakingSubmissionController | permitAll | `:global` (100/min) | yes |
| 113 | `GET` | `/api/v1/speaking-submissions` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 114 | `POST` | `/api/v1/speaking-submissions/upload` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 115 | `GET` | `/api/v1/speaking-submissions/{id}` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 116 | `POST` | `/api/v1/speaking-submissions/{id}/assess` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 117 | `GET` | `/api/v1/video-prompts/{promptId}/submissions` | SpeakingSubmissionController | permitAll | `:global` (100/min) | yes |
| 118 | `GET` | `/api/v1/video-submissions` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 119 | `POST` | `/api/v1/video-submissions/upload` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 120 | `GET` | `/api/v1/video-submissions/{id}` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 121 | `POST` | `/api/v1/video-submissions/{id}/assess` | SpeakingSubmissionController | authenticated | `:global` (100/min) | yes |
| 122 | `GET` | `/api/srs/due/{deckId}` | SrsController | authenticated | `:global` (100/min) | yes |
| 123 | `POST` | `/api/srs/review` | SrsController | authenticated | `:global` (100/min) | yes |
| 124 | `GET` | `/api/srs/stats` | SrsController | authenticated | `:global` (100/min) | yes |
| 125 | `GET` | `/api/streak/current` | StreakController | authenticated | `:global` (100/min) | yes |
| 126 | `GET` | `/api/streak/history` | StreakController | authenticated | `:global` (100/min) | yes |
| 127 | `GET` | `/api/v1/admin/video-attempts` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 128 | `POST` | `/api/v1/admin/video-attempts/{id}/ai-grade` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 129 | `PATCH` | `/api/v1/admin/video-attempts/{id}/grade` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 130 | `GET` | `/api/v1/admin/video-lessons` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 131 | `POST` | `/api/v1/admin/video-lessons` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 132 | `POST` | `/api/v1/admin/video-lessons/fetch-youtube` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 133 | `POST` | `/api/v1/admin/video-lessons/translate-transcript` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 134 | `POST` | `/api/v1/admin/video-lessons/upload` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 135 | `DELETE` | `/api/v1/admin/video-lessons/{id}` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 136 | `PUT` | `/api/v1/admin/video-lessons/{id}` | VideoLessonController | ROLE_ADMIN | `:global` (100/min) | yes |
| 137 | `GET` | `/api/v1/video-attempts` | VideoLessonController | authenticated | `:global` (100/min) | yes |
| 138 | `GET` | `/api/v1/video-lessons` | VideoLessonController | permitAll | `:global` (100/min) | yes |
| 139 | `GET` | `/api/v1/video-lessons/{id}` | VideoLessonController | permitAll | `:global` (100/min) | yes |
| 140 | `POST` | `/api/v1/video-lessons/{id}/attempts` | VideoLessonController | authenticated | `:global` (100/min) | yes |
| 141 | `GET` | `/api/vocabulary` | VocabularyController | authenticated | `:global` (100/min) | yes |
| 142 | `POST` | `/api/vocabulary` | VocabularyController | authenticated | `:global` (100/min) | yes |
| 143 | `GET` | `/api/vocabulary/dictionary/{word}` | VocabularyController | permitAll | `:global` (100/min) | yes |
| 144 | `GET` | `/api/vocabulary/search` | VocabularyController | permitAll | `:global` (100/min) | yes |

## Negative cases bắt buộc (PLAN.md Task 5)

| Case | Cách đo | Trạng thái |
|---|---|---|
| missing auth | probe với `as=none` trên toàn bộ endpoint không permitAll | p1/p4a/p6 role matrix |
| wrong role | probe `as=user` trên mọi endpoint `/api/admin/**`, `/api/v1/admin/**` | p1/p4a/p6 |
| malformed body | JSON sai kiểu / thiếu field bắt buộc | p1/p4b/p6 (exercise, prompt, lesson, game submit) |
| missing resource | id không tồn tại (999999999) | p6 + p4c-fleet |
| duplicate resource | register trùng email/username → 409 | p5 |
| invalid enum/type | `level=FOO`, `skill=NOT_A_SKILL`, `level=B1` trên video | p1/p5/p6 |
| upload extension/content mismatch | `.html/.svg/.js` → 400; octet-stream + attachment khi serve | `AuditV8UploadXssTest` + `xss_poc.js` |
