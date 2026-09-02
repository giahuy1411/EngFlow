# Endpoint Map — Audit V2 (Phase A, Task 1)

Tổng: 25 controllers (21 root + 4 subpackage). Dấu (x2) = 1 mapping, 2 đường dẫn.

## Root controllers (21)
### AdminAiExerciseController /api/admin/exercises/ai
POST generate-async | POST generate | POST generate-all | POST generate-batch | POST save | POST validate | GET status
### AdminController /api/admin
GET stats | GET users | PUT users/{id}/toggle-active | PUT users/{id}/toggle-admin | PUT users/{id}/toggle-premium | PUT users/{id}/revoke-premium | GET lessons | GET lessons/{id} | POST lessons | PUT lessons/{id} | DELETE lessons/{id} | PUT lessons/{id}/toggle-publish | GET vocabulary | POST vocabulary | PUT vocabulary/{id} | DELETE vocabulary/{id}
### AdminExerciseController /api/admin/exercises
GET | GET {id} | POST | PUT {id} | DELETE {id}
### AdminExerciseSeedController /api/admin/exercises
POST seed
### AiVocabController /api/ai
POST generate-vocab | POST enrich-word | POST save-vocab
### AuthController /api/auth
POST register | POST login | GET me | POST change-password | PUT avatar | POST avatar/upload
### DashboardController /api/dashboard
GET stats
### DeckController /api/decks
GET | GET my | GET {id} | POST | PUT {id} | DELETE {id} | POST {id}/words
### FlashcardController /api/flashcards
POST review | GET status/{vocabularyId}
### GameController /api/games
GET quiz/{deckId} | GET memory/{deckId} | GET typing/{deckId} | GET listening/{deckId} | GET mixed/{deckId} | POST submit
### LeaderboardController /api/leaderboard
GET
### LessonController /api/lessons
GET | GET {id} | POST | PUT {id} | DELETE {id}
### LessonExerciseController /api/lessons/{lessonId}/exercises
GET | POST grade | POST submit | GET attempts | GET attempts/{attemptId} | GET content
### LessonSnapshotController (base rỗng)
GET /api/admin/lessons/{lessonId}/snapshots | POST .../snapshots | POST .../snapshots/{snapshotId}/restore
### LessonStructureController (base rỗng)
GET /api/admin/lessons/{lessonId}/structure | POST /api/admin/lessons/{lessonId}/sections | PUT /api/admin/sections/{sectionId} | DELETE /api/admin/sections/{sectionId} | POST /api/admin/sections/{sectionId}/blocks | PUT /api/admin/blocks/{blockId} | DELETE /api/admin/blocks/{blockId} | POST /api/admin/upload | GET /api/resources/{filename} | POST /api/admin/audio-upload | GET /api/lessons/{lessonId}/structure
### LessonSubmissionController /api/lesson-submissions
POST submit | POST upload-audio | GET my/lesson/{lessonId}/skill/{skillType}
### MediaProxyController
GET /api/v1/media/**
### ProgressController /api/users
GET progress
### SrsController /api/srs
POST review | GET due/{deckId} | GET stats
### StreakController /api/streak
GET history | GET current
### VocabularyController /api/vocabulary
GET | GET search | POST

## Subpackage controllers (4)
### payment/PaymentController
POST /api/webhook/sepay | POST /api/v1/payment/create-order | GET /api/v1/payment/status
### speaking/SpeakingPromptController
GET admin list | GET public list (q / !q) | GET public {id} | POST admin | PUT admin {id} | DELETE admin {id} | POST admin ai-generate (mỗi mapping x2 speaking/video)
### speaking/SpeakingSubmissionController
POST upload | POST {id}/assess | GET admin list | PATCH admin {id}/grade | GET my list | GET by prompt | GET {id} (mỗi mapping x2 speaking/video)
### video/VideoLessonController
GET /api/v1/video-lessons | GET /{id} | POST /{id}/attempts | GET /api/v1/video-attempts | GET admin list | POST admin | POST admin upload (multipart) | PUT admin {id} | DELETE admin {id} | GET admin video-attempts | PATCH admin video-attempts/{id}/grade
