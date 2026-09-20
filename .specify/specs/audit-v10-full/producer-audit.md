# Producer audit — which completion events may write a study day

Contract (`tasks/streak-study/README.md`): a study day is written by a **backend-verified
completion**, inside the transaction that persists the result. Upload-only, client-declared and
teacher-graded-later events must NOT count, because a day must mean "the learner finished
something", not "the learner sent bytes".

Legend: **WIRED** = calls `recordStudy` today; **GAP** = real completion, not wired;
**NOT ELIGIBLE** = deliberately not wired, with the reason.

| # | Producer | Entry point | Verified by backend? | State | Evidence |
|---|---|---|---|---|---|
| 1 | Exercise submit | `POST /api/lessons/{id}/exercises/submit` → `ExerciseService.submitExercises` | Yes — server grades against the stored key; only counts when at least one answer is non-blank | **WIRED** | `ExerciseService.java:323-326` |
| 2 | SRS review | `POST /api/srs/review` → `SrsService.reviewWord` | Yes — server applies SM-2 and persists progress | **WIRED** | `SrsService.java:110` |
| 3 | Speaking submission | `POST /api/v1/speaking-submissions/upload` → `SpeakingSubmissionService` | Yes — only when status reaches `COMPLETED`, i.e. Whisper returned a non-blank transcript and the rubric produced scores | **WIRED** | `SpeakingSubmissionService.java:133-135` |
| 4 | Game check-in | `GameService` finish → `StreakService.checkin` | Partially — the server owns the Redis session, and `checkin` ignores `totalQuestions <= 0` so an empty session cannot count | **WIRED** | `GameService.java:258`, `StreakService.java:16-20` |
| 5 | Skill submission (writing/reading/speaking tab) | `POST /api/lesson-submissions/submit` → `LessonSubmissionService.submitLessonSkill` | **No** — the row is stored `PENDING` and a teacher grades it later | **NOT ELIGIBLE** | see note A |
| 6 | Video shadowing attempt | `POST /api/v1/video-lessons/{id}/attempts` → `VideoLessonService.submitAttempt` | **No** — status starts `SUBMITTED`; nothing has checked the audio | **NOT ELIGIBLE** | see note B |
| 7 | Video quiz | `VideoLesson.vue` `buildQuiz()` | **No** — the quiz is built and scored **entirely in the browser** from the transcript; there is no endpoint that receives quiz answers | **NOT ELIGIBLE** | see note C |

## Note A — skill submissions are upload-only at write time

`LessonSubmissionService.submitLessonSkill` persists `status = "PENDING"` with
`score = null` and `feedback = null`. Nothing validates the text or the audio. A learner could
type one character and earn a study day. The moment of *verified* completion is the teacher's
grading, which lives in the admin flow — not in this request. Wiring `recordStudy` here would
make the streak claim "I opened a textarea".

## Note B — shadowing attempts are unassessed at write time

`VideoLessonService.submitAttempt` checks the file is audio, ≤ 20 MB, and the line index is
valid — then stores it as `SUBMITTED`. The content is only assessed later, either by
`ShadowingAiGradingService.aiGrade` (Whisper transcript → coverage score) or by a teacher
(`grade`). Recording a day on upload would reward an empty recording.

## Note C — the video quiz never reaches the server

`frontend/src/views/videos/VideoLesson.vue:496` builds quiz items client-side from
`transcript.value`, computes the correct option in the browser, and keeps `quizAnswered` in
component state. Grepping the whole frontend for `quiz` finds no POST of quiz results, and
`VideoDtos` has no quiz request record. Counting this would mean trusting a number the client
chose. `tasks/streak-study/README.md` explicitly forbids "client-declared completion".

## Consequence

The four wired producers are exactly the four events the backend can verify on its own. The
three unwired ones are unwired **by design**, and wiring them would violate the contract rather
than complete it. This table is the evidence for that decision, so the next audit does not
re-open it as an oversight.
