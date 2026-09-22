# Skeptic #1 — REFUTATION: "Guard is MULTIPLE_CHOICE-only, so LISTENING keeps the unusable-options defect"

**Claim under review:** `assertNewChoiceOptionsUsable()` returns early unless `exerciseType == MULTIPLE_CHOICE`,
so LISTENING still ships the F-13-01 "unusable options" defect. Claimed severity **MEDIUM**.
**Verdict: REFUTED.** Two of the three evidence pillars are false, and the stated root cause is factually wrong.
Residual issue, if any, is at most **LOW** (data-quality nit, not the claimed learner-facing defect).

---

## 1. What is TRUE (verified live, reproduced)

The API asymmetry is real. Flushed rate-limit buckets first.

| Request | Result |
|---|---|
| `POST /api/admin/exercises {exerciseType:"LISTENING", options:"[\"A\",\"B\",\"C\",\"D\"]", ...}` | **HTTP 200** (created id 777632, later deleted) |
| `POST /api/admin/exercises {exerciseType:"MULTIPLE_CHOICE", options:"[\"A\",\"B\",\"C\",\"D\"]", ...}` | **HTTP 400** `MULTIPLE_CHOICE cần nội dung lựa chọn thật, không chỉ "a"/"b"/"c"/"d"` |

Live DB confirms the population (`tmp/count_listen.sql`):

```
listening_total | null_opts | bare_letter_abcd
358             | 78        | 25
```

Grouped option shapes for LISTENING (options not null): `[]`×241, `["A", "B", "C", "D"]`×25,
`["A", "B", "C"]`×5, `["A","B","C","D"]`×2, … — **32 rows carry a bare-letter token** (claim said "31"; close).

Row cited by the claim is real: `exercise_id=650612, lesson_id=10892, exercise_type=LISTENING,
options='["A", "B", "C", "D"]', correct_answer='The show explores the lives of a family who are not human, coming from another part of the universe.'`

---

## 2. What is FALSE — the claimed learner impact

Claim: *"the exercise renders 4 clickable options A/B/C/D"*.

**Measured live DOM (`http://localhost:5173/lessons/10892`, tab "Bài tập"), per exercise card:**

| # | type badge | question | `[data-testid="mc-option"]` buttons | text inputs |
|---|---|---|---|---|
| 6 | **NGHE** (LISTENING) | *"What is the theme of '3rd Rock from the Sun'?"* (row 650612) | **0** | **1** |
| 1 | TRẮC NGHIỆM (MULTIPLE_CHOICE) | *"Dick Solomon and his family are not really humans…"* | 4 | 0 |

The 4 A/B/C/D buttons belong to a **different, MULTIPLE_CHOICE card** in the same lesson. The claimed
LISTENING row renders **a text input and zero option buttons** — exactly the opposite of the claim.
Scoped DOM probe on the card containing "3rd Rock":

```json
{ "qText": "What is the theme of '3rd Rock from the Sun'?",
  "optionButtons": [], "textInputs": 1,
  "badge": ["What is the theme of '3rd Rock from the Sun'?", "", "NGHE & TRẢ LỜI", ""] }
```

## 3. What is FALSE — the grade evidence is a probe artefact

Claim: *"POST …/grade with the only clickable choice returns percentage 0 / correct:false"*.

That result is produced by a **malformed payload**: the DTO field is `userAnswer`
(`src/main/java/com/datn/engflow/model/dto/request/GradeRequest.java`), not `answer`.
A payload using `answer` is silently ignored → empty answer → `correct:false`.

| Payload sent | Response |
|---|---|
| `{"answers":[{"exerciseId":650612,"answer":"<exact transcript>"}]}` (**wrong field**) | `percentage:0.0, correct:false` |
| `{"answers":[{"exerciseId":650612,"userAnswer":"<exact transcript>"}]}` (**correct field**) | **`percentage:100.0, correct:true`** |

The frontend sends the correct field: `LessonExerciseTab.vue:296 → { exerciseId: ex.id, userAnswer }`.
`deep-probe.json`/`api-sweep.json` contain **no** `userAnswer` token, confirming the probe used the wrong key.
So the "only clickable choice grades wrong" narrative is a self-inflicted probe artefact, not a learner defect.

## 4. What is FALSE — the stated root cause

Claim: *"LessonExerciseTab.vue hasOptionChoices() returns true for MULTIPLE_CHOICE and LISTENING."*

Current source **explicitly separates** them (`LessonExerciseTab.vue:232-247`):

```js
function hasOptionChoices(ex) {
  if (ex.exerciseType === 'MULTIPLE_CHOICE') return rawOptions(ex).length >= 2
  if (ex.exerciseType === 'LISTENING') return hasRealOptions(ex)   // >=2 opts AND some non-bare-letter
  ...
}
```

`hasRealOptions(["A","B","C","D"])` → `false`, so LISTENING falls through to the text-input branch.
This is also codified in the co-located test `LessonExerciseTab.f13.test.js:126`:
*"keeps a text answer input for LISTENING whose options are bare letters"* →
`expect(input[type=text]).toBe(true)` and `expect(mc-option).toBe(0)`.

## 5. Why the residual API gap is not the claimed defect

An admin *can* still create a LISTENING row with bare-letter options (guard is MC-only). But:
- The learner UI ignores those options and shows the text-answer control.
- Grading is server-side against `correct_answer`, and the transcript answer scores **100%**.
- Therefore the F-13-01 defect class (a clickable option that cannot be answered correctly) is
  **not reachable** through LISTENING.

Residual, at most LOW: an admin can persist junk `options` on a LISTENING row that the UI silently ignores —
a data-quality nit with no learner impact, not a MEDIUM usability defect.

## 6. Cleanup

Probe row created by this skeptic (`exercise_id=777632`, `question='skeptic-probe'`) deleted by listed ID:
`deleted = 1`. (`SET QUOTED_IDENTIFIER ON;`, no `Msg \d+` in output.)
