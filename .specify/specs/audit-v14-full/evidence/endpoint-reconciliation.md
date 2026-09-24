# audit-v14-full — T2.2 endpoint reconciliation (D1/D10)

**Method:** `api-sweep.js` records every HTTP call (`R.calls`); each inventory row is
joined to concrete calls by **method + path-template**. This is the D1 delta over v13, whose
148 rows vs 147 statuses could not be reconciled.

## Tally

| metric | value |
|---|---|
| inventoryRows | 148 |
| baseSweepCalls | 151 |
| coverageSweepRows | 148 |
| matchedRows | 148 |
| unaccounted | 0 |
| sweepPass | 143 |
| sweepFail | 0 |
| sweepBlocked | 1 |
| sweepNA | 3 |

**unaccounted = 0**

## Disposition (every inventory row)

| disposition | rows |
|---|---|
| BLOCKED | 10 |
| N/A | 3 |
| PROBED | 135 |

## Per-controller coverage (D10)

| controller | rows | covered | % |
|---|---|---|---|
| AdminAiExerciseController | 6 | 6 | 100% |
| AdminAnswerBackfillController | 2 | 2 | 100% |
| AdminController | 16 | 16 | 100% |
| AdminExerciseController | 5 | 5 | 100% |
| AdminExerciseSeedController | 1 | 1 | 100% |
| AiVocabController | 3 | 3 | 100% |
| AuthController | 8 | 8 | 100% |
| DashboardController | 1 | 1 | 100% |
| DeckController | 7 | 7 | 100% |
| FlashcardController | 3 | 3 | 100% |
| GameController | 6 | 6 | 100% |
| LeaderboardController | 1 | 1 | 100% |
| LessonController | 5 | 5 | 100% |
| LessonExerciseController | 6 | 6 | 100% |
| LessonSnapshotController | 3 | 3 | 100% |
| LessonStructureController | 11 | 11 | 100% |
| LessonSubmissionController | 3 | 3 | 100% |
| MediaProxyController | 1 | 1 | 100% |
| PaymentController | 3 | 3 | 100% |
| ProgressController | 1 | 1 | 100% |
| SpeakingPromptController | 18 | 18 | 100% |
| SpeakingSubmissionController | 14 | 14 | 100% |
| SrsController | 3 | 3 | 100% |
| StreakController | 3 | 3 | 100% |
| VideoLessonController | 14 | 14 | 100% |
| VocabularyController | 4 | 4 | 100% |

**Controllers not 100%: 0**
