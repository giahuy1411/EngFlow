# Skeptic #2 — REFUTATION of F-13-01b ("Guard is MULTIPLE_CHOICE-only, so LISTENING keeps the same unusable-options defect")

**Verdict: REFUTED** (claimed MEDIUM → corrected **LOW**, informational/consistency only)

Claim under test (api-sweep.md, F-13-01b):
> The learner renders LISTENING as a choice type: `hasOptionChoices()` returns true for
> MULTIPLE_CHOICE **and** LISTENING. … the exercise renders 4 clickable options A/B/C/D, but
> grading the only clickable choice returns `{"correct":false,...}` — the learner can never be right.

---

## 1. The backend half of the evidence is REAL (reproduced)

Flushed rate-limit buckets first, logged in as admin (`admin@gmail.com`), then:

```
POST /api/admin/exercises {exerciseType:"LISTENING", options:["A","B","C","D"]} -> HTTP 200  (id 777633)
POST /api/admin/exercises {exerciseType:"MULTIPLE_CHOICE", options:["A","B","C","D"]}
     -> HTTP 400  {"detail":"MULTIPLE_CHOICE cần nội dung lựa chọn thật, không chỉ \"a\"/\"b\"/\"c\"/\"d\""}
```

The asymmetry in `assertNewChoiceOptionsUsable()` (`ExerciseService.java:176-180`, returns
early unless the type equalsIgnoreCase MULTIPLE_CHOICE) is real. The live DB row is real too:

```
exercise_id=650612 | lesson_id=10892 | LISTENING | options=["A", "B", "C", "D"]
correct_answer='The show explores the lives of a family who are not human, coming from another part of the universe.'
```
Count of LISTENING rows whose options contain `"A"` = **30** (25 with the full 4 letters,
length < 40). So "31 rows" is approximately right.

## 2. The claimed LEARNER IMPACT is FALSE — this is what refutes the finding

The claim's root cause states `LessonExerciseTab.vue` `hasOptionChoices()` "returns true for
MULTIPLE_CHOICE and LISTENING". That is **not** what the code does.

**Current working tree** (`frontend/src/views/lessons/LessonExerciseTab.vue:232-258`):
```js
function hasOptionChoices(ex) {
  if (ex.exerciseType === 'MULTIPLE_CHOICE') { return rawOptions(ex).length >= 2 }
  if (ex.exerciseType === 'LISTENING') {
    // bare letters carry no meaning → fall through to the text answer
    return hasRealOptions(ex)          // false when every option is a bare letter
  }
  ...
}
function hasRealOptions(ex) {
  const opts = rawOptions(ex)
  return opts.length >= 2 && opts.some(o => !isBareLetterOption(o))
}
```

**HEAD version** (the state the sweep ran against) — `parsedOptions` returns `null` when
`allPlaceholders` (`/^[A-D](\s*-\s*.*)?$/i`), and `hasOptionChoices` returns
`Array.isArray(opts) && opts.length > 0` → also **false** for `["A","B","C","D"]`.
So even at HEAD, bare-letter LISTENING fell through to the **text input**.

### Live browser measurement (Chrome DevTools, page 10, logged in as student, lesson 10892)

Rendered exercise cards (each `div.bg-card.shadow-pop-lg`), read from the real DOM:

| # | type | question | `[data-testid="mc-option"]` | text input |
|---|------|----------|------------------------------|-----------|
| 1 | NGHE | (LISTENING probe) | 0 | yes |
| 2 | TRẮC NGHIỆM | Dick Solomon and his family… | **4 (A,B,C,D)** | no |
| 3 | ĐIỀN TỪ | Water ___ at 100… | 0 | yes |
| 5 | DỊCH | Can you match each description… | 0 | yes |
| **6** | **NGHE** | **What is the theme of '3rd Rock from the Sun'? (row 650612)** | **0** | **yes (+ `<audio>`)** |
| 7 | NGHE | skeptic-probe | 0 | yes |

Card 6 is the exact row the claim says "renders 4 clickable options A/B/C/D". It renders an
**audio player and a single text input**, and shows **no** "missing options" warning.
Screenshot: `skeptic2-listening-650612.png`.

The claim conflated card **2** (a genuine MULTIPLE_CHOICE whose options happen to be
A/B/C/D and whose `correct_answer` is a letter — legitimately choice-rendered) with card
**6** (the LISTENING row). The "4 clickable options" reading is a probe artefact of that
conflation.

### Live grading of row 650612 (student token)

```
POST /api/lessons/10892/exercises/grade {"answers":[{"exerciseId":650612,"userAnswer":"<the real answer>"}]}
 -> {"percentage":100.0,"score":1,"total":1,"results":[{"correct":true,...}]}

POST … {"userAnswer":"A"} -> {"percentage":0.0,…,"correct":false}
```

The learner's rendered control (the text input) accepts the answer and scores 100%. The
claim's "the learner can never be right" scenario ("grading the only clickable choice") does
not exist in the UI. The `A`→0 result is only reachable by POSTing a value the UI never offers.

## 3. Cross-check

- `npx vitest run src/views/lessons/LessonExerciseTab.f13.test.js` → **11 passed**, including
  "keeps a text answer input for LISTENING whose options are bare letters (39 real rows)".
- No other component renders LISTENING options as choices (`grep` for `mc-option` → only
  `LessonExerciseTab.vue`).
- `sweep/v8/p16-parity.sql` after cleanup → `1470|43735|72|118|29|15|4|126|10|5` (matches baseline).

## 4. Corrected assessment

- **Learner impact: none.** The rendering guard that actually protects the learner
  (`hasOptionChoices`/`hasRealOptions`) already excludes bare-letter LISTENING options, at
  HEAD and in the working tree. The defect class F-13-01 is NOT reachable through LISTENING.
- **Residual real issue (LOW, consistency):** the *admin-facing* guard is
  MULTIPLE_CHOICE-only, so an admin can still persist a LISTENING row whose stored options
  are meaningless. The learner UI ignores them, so this is cosmetic/defence-in-depth, not a
  MEDIUM learner defect. Reporting it at MEDIUM, on the stated end-to-end impact, is not
  justified.

## 5. Cleanup

- Probe row created by this refutation: `exercise_id=777633` (LISTENING, question
  `zz-skeptic2 LISTENING probe`), deleted by listed ID.
  `DELETE FROM exercises WHERE exercise_id = 777633` → `@@ROWCOUNT=1`; remaining `=0`.
- Rate-limit buckets flushed before each HTTP batch; no `Msg \d+` in any batch output.
