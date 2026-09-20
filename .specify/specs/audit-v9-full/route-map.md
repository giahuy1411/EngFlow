# Route map — audit-v9-full

Source: `frontend/src/router/index.js` (parsed, not inferred). 39 routes, 37 named.

Guard classes: {"public": 8, "guestOnly": 4, "auth+premium": 6, "auth": 11, "admin-redirect": 1, "admin": 9}

Guard semantics asserted live by `sweep/v8/ui/routes-all.js` in BOTH directions
(a `guestOnly` route MUST bounce a logged-in visitor; a `public` route must stay).

| # | path | name | guard | requiresAuth | requiresPremium | guestOnly | component |
|---|---|---|---|---|---|---|---|
| 1 | `/` | Home | public | - | - | - | `@/views/Home.vue` |
| 2 | `/login` | Login | guestOnly | - | - | True | `@/views/Login.vue` |
| 3 | `/register` | Register | guestOnly | - | - | True | `@/views/Register.vue` |
| 4 | `/forgot-password` | ForgotPassword | guestOnly | - | - | True | `@/views/ForgotPassword.vue` |
| 5 | `/reset-password` | ResetPassword | guestOnly | - | - | True | `@/views/ResetPassword.vue` |
| 6 | `/lessons` | Lessons | public | False | - | - | `@/views/Lessons.vue` |
| 7 | `/lessons/:id` | LessonDetail | public | False | - | - | `@/views/lessons/LessonLayout.vue` |
| 8 | `/videos` | VideoLibrary | auth+premium | True | True | - | `@/views/videos/VideoLibrary.vue` |
| 9 | `/videos/:id` | VideoLesson | auth+premium | True | True | - | `@/views/videos/VideoLesson.vue` |
| 10 | `/profile` | Profile | auth | True | - | - | `@/views/Profile.vue` |
| 11 | `/search` | Search | public | - | - | - | `@/views/SearchVocabulary.vue` |
| 12 | `/leaderboard` | Leaderboard | public | - | - | - | `@/views/Leaderboard.vue` |
| 13 | `/decks` | Decks | public | False | - | - | `@/views/luyentu/Decks.vue` |
| 14 | `/decks/:id` | DeckDetail | auth | True | - | - | `@/views/luyentu/DeckDetail.vue` |
| 15 | `/decks/:id/play/flashcard` | FlashcardGame | auth | True | - | - | `@/views/luyentu/FlashcardGame.vue` |
| 16 | `/decks/:id/play/quiz` | QuizGame | auth | True | - | - | `@/views/luyentu/QuizGame.vue` |
| 17 | `/decks/:id/play/memory` | MemoryMatchGame | auth | True | - | - | `@/views/luyentu/MemoryMatchGame.vue` |
| 18 | `/decks/:id/play/typing` | TypingGame | auth | True | - | - | `@/views/luyentu/TypingGame.vue` |
| 19 | `/decks/:id/play/listening` | ListeningGame | auth | True | - | - | `@/views/luyentu/ListeningGame.vue` |
| 20 | `/decks/:id/play/mixed` | MixedGame | auth | True | - | - | `@/views/luyentu/MixedGame.vue` |
| 21 | `/ai-vocab-generator` | AiVocabGenerator | auth | True | - | - | `@/views/luyentu/AiVocabGenerator.vue` |
| 22 | `/decks/create` | DeckCreate | auth | True | - | - | `@/views/luyentu/DeckCreate.vue` |
| 23 | `/speaking` | Speaking | auth+premium | True | True | - | `@/views/speaking/SpeakingList.vue` |
| 24 | `/speaking/history` | SpeakingHistory | auth+premium | True | True | - | `@/views/speaking/SubmissionHistory.vue` |
| 25 | `/speaking/:id` | SpeakingDetail | auth+premium | True | True | - | `@/views/speaking/SpeakingDetail.vue` |
| 26 | `/speaking/:id/record` | SpeakingRecord | auth+premium | True | True | - | `@/views/speaking/SpeakingRecord.vue` |
| 27 | `/premium` | PremiumPage | public | - | - | - | `@/views/premium/PremiumPage.vue` |
| 28 | `/premium/checkout` | PremiumCheckout | auth | True | - | - | `@/views/premium/PremiumCheckout.vue` |
| 29 | `/:pathMatch(.*)*` | - | public | - | - | - | `` |
| 30 | `/admin` | - | admin-redirect | - | - | - | `` |
| 31 | `/admin/dashboard` | AdminDashboard | admin | - | - | - | `@/views/admin/AdminDashboard.vue` |
| 32 | `/admin/lessons` | AdminLessons | admin | - | - | - | `@/views/admin/AdminLessons.vue` |
| 33 | `/admin/exercises` | AdminExercises | admin | - | - | - | `@/views/admin/AdminExercises.vue` |
| 34 | `/admin/users` | AdminUsers | admin | - | - | - | `@/views/admin/AdminUsers.vue` |
| 35 | `/admin/speaking-prompts` | AdminSpeakingPrompts | admin | - | - | - | `@/views/admin/AdminSpeakingPrompts.vue` |
| 36 | `/admin/speaking-submissions` | AdminSpeakingSubmissions | admin | - | - | - | `@/views/admin/AdminSpeakingSubmissions.vue` |
| 37 | `/admin/videos` | AdminVideoLessons | admin | - | - | - | `@/views/admin/AdminVideoLessons.vue` |
| 38 | `/admin/video-attempts` | AdminVideoAttempts | admin | - | - | - | `@/views/admin/AdminVideoAttempts.vue` |
| 39 | `/admin/:id/build` | AdminLessonBuilder | admin | - | - | - | `@/views/admin/AdminLessonBuilder.vue` |

## Measured behaviour (Chromium, 2026-09-17)

`ui/routes-all.js`: 228 visits (39 routes x 2 viewports x 3 roles) —
0 console errors, 0 API >= 400, 0 overflow, 0 not-mounted, 0 wrong landing,
52 anon visits on guarded routes all redirected, 18/18 admin visits rendered a real admin page.

Catch-all `/:pathMatch(.*)*` always redirects — routing a bad URL must never render a blank page.
