# Phase 2 — API sweep (audit-v13-full)

Live stack: backend `http://localhost:8080` (Spring Boot, JWT), container `engflow-backend`.
Inventory: `.specify/specs/audit-v13-full/evidence/endpoint-inventory.json` — 132 annotations, 148 rows, 26 controllers.

## How this was produced

* `node sweep/v13/api-sweep.js` — the full inventory sweep (15 areas, 148 rows' worth of probes).
* `node sweep/v13/deep-probe.js` — the six named features, verified on **response contract**
  (field name + JS type), plus the MULTIPLE_CHOICE guard and role auth in both directions.
* `node sweep/v13/search-sort.js` — search semantics and `?sort=` measured on real rows.
* Every HTTP batch is preceded by a `rate_limit:*` flush (`docker exec engflow-redis redis-cli`),
  so the sweep cannot manufacture its own 429s (finding F109).
* Cleanup is by **enumerated criteria** with `SET QUOTED_IDENTIFIER ON;`, and the sqlcmd output is
  scanned for `Msg \d+` (sqlcmd exits 0 even when a batch fails).

## Result

| suite | pass | fail | blocked | n/a |
|---|---|---|---|---|
| `api-sweep.js` (whole inventory) | 143 | 0 | 1 | 3 |
| `deep-probe.js` (six features + guard + roles) | 56 | **2** | 0 | 0 |
| `search-sort.js` (search + sort measured) | — | — | — | — (measurements, see below) |

Base sweep per area: auth 13, lessons 18, streak 5, search 8, crud 23, game 13, flashcard 7,
srs 6, misc 6, submission 6, speaking 9, video 7, admin 10, ai 5, payment 7.

The one BLOCK is the SePay webhook with a **valid** HMAC signature: it mutates a real
`payment_transactions` row (real-money boundary). The invalid-signature and stale-timestamp
paths are asserted and pass. The three N/A are multipart/real-media paths already covered
by v11 evidence (avatar upload, speaking upload/assess, snapshot create/restore).

## Findings

### F-13-01a — HIGH — the new MULTIPLE_CHOICE guard over-blocks a real, legitimate option style

The guard added for F-13-01 rejects bare-letter options — correct — but its regex
`(?i)^[A-D](\s*-\s*.*)?$` also matches **letter + dash + real content**, which is legitimate
choice text and exists as a real production row.

* Real row: `exercises.exercise_id=651717`, `lesson_id=10976`,
  `options='["A - Salad", "B - Cheeseburger", "C - Pizza", "D - Bread"]'`,
  `correct_answer='A - Salad'`, `question='What food is shown in the first picture?'`.
* `POST /api/admin/exercises` with those exact options → **400**
  (`"MULTIPLE_CHOICE cần nội dung lựa chọn thật, không chỉ \"a\"/\"b\"/\"c\"/\"d\""`).
* The same options on the **update** path → 400 as well, so an admin cannot even re-save the
  row it already exists as.
* Scope: `exercises` holds 2 rows in the `"A - <content>"` style (1 MULTIPLE_CHOICE, 1 FILL_BLANK).

Evidence: `deep-probe.json` → `mc-guard` FAIL "MC options [\"A - Salad\",...] -> 200";
reproduced against the container restarted at 03:17:52 (`MC dash A - Salad -> 400`,
`MC real Apple.. -> 200`).

### F-13-01b — MEDIUM — the same guard is MULTIPLE_CHOICE-only, so LISTENING keeps the defect

The learner renders LISTENING as a **choice** type: `LessonExerciseTab.vue`
`hasOptionChoices()` returns true for `MULTIPLE_CHOICE` **and** `LISTENING`. The guard
(`assertNewChoiceOptionsUsable`) only fires for `MULTIPLE_CHOICE`, so a LISTENING exercise
with bare-letter options is still accepted.

* Live DB: **31** LISTENING rows carry bare-letter options, e.g. `exercise_id=650612`
  (`lesson_id=10892`, `options='["A", "B", "C", "D"]'`,
  `correct_answer='The show explores the lives of a family...'`).
* `POST /api/admin/exercises` `{exerciseType:"LISTENING", options:["A","B","C","D"]}` → **200**
  (MULTIPLE_CHOICE with the same options → 400).
* Learner impact, measured end to end: the exercise renders 4 clickable options `A/B/C/D`,
  but grading the only clickable choice returns
  `{"correct":false,"correctAnswer":"The show explores..."}` — the learner can never be right.

Evidence: `deep-probe.json` → `mc-guard` FAIL "LISTENING with bare-letter options ... -> 200".

### F-13-01c — LOW — `?sort=` is ignored by the three paged endpoints the user would sort

Measured, not assumed (`search-sort.js`):

| endpoint | `?sort=title,asc` vs `,desc` | honoured? |
|---|---|---|
| `GET /api/lessons` | identical payloads | **no** — `LessonController` hardcodes `Sort.by("orderIndex")` |
| `GET /api/admin/exercises` | identical payloads | **no** — `AdminExerciseController` hardcodes `Sort.by("orderIndex")` |
| `GET /api/decks` | identical payloads | **no** — `DeckController` hardcodes `Sort.by("name")` |
| `GET /api/vocabulary` | reversed, correctly ordered | **yes** — uses `@PageableDefault(sort="word")` |

A `?sort=` on the first three is silently discarded (200, default order), which is worse than
a 400: the client believes the order it asked for. `/api/vocabulary` does honour it — and an
**unknown property there returns 500**, not 400:

* `GET /api/vocabulary?size=2&sort=notacolumn,asc` → **500**
  (`PropertyReferenceException: No property 'notacolumn' found for type 'Vocabulary'`;
  `sort=word;DROP` → `InvalidDataAccessApiUsageException`).

Client-controlled input reaching an unhandled Spring Data exception is a small robustness
defect (the handler catches `RuntimeException` but maps it to a generic 500), and it is
inconsistent with the other three endpoints which ignore the parameter entirely.

## The six named features — response contract (field name + JS type)

All verified against real HTTP responses; see `deep-probe.json` → `contract`.

1. **Lessons / Exercises** — list item `{id:number, title:string}` (keys:
   audioUrl, category, completionPercentage, description, durationMinutes, id, isCompleted,
   level, orderIndex, skillType, thumbnailUrl, title). Detail adds `content` + `vocabularies`.
   Exercise item `{id:number, lessonId:number, question:string, exerciseType:string}`.
   `includeAnswers=false` strips `correctAnswer`; `includeAnswers=true` as anon/student → 403.
2. **Streak** — `{currentStreak:number, studiedToday:boolean, studiedDays:array}` (+ `today`,
   `effectiveFrom`, `legacyAccessDays`, `legacyHistoryAvailable`). This matches the frontend's
   own runtime validator in `Profile.vue` (`Number.isInteger(currentStreak)`,
   `typeof studiedToday === 'boolean'`, `Array.isArray(studiedDays)`).
3. **Login / Register** — `POST /api/auth/login` → `{token:string, ...user}`; `/me` →
   `{email:string, isAdmin:boolean}`. Register duplicate → 409, short password / bad email → 400.
4. **Search** — `GET /api/vocabulary/search?keyword=` → JSON array of `{word:string, ...}`,
   case-insensitive (`rise` == `RISE`), 1-char/blank → `[]` by design. Lessons `?q=grammar`
   → 466 matches; `?q=zzzznotaword` → 0. Admin exercises `?q=the` → 16588.
5. **CRUD** — deck create→read→update→delete→404 round-trips; lesson create/update/delete as
   admin; private deck hidden from anon. `F147` guard holds (student write without `deckId`
   → 400; another user's deck → 400). `F152` holds (student `lessonId` → 400 on both the new-word
   and dedupe branches; admin still 200; nothing leaked into the public lesson payload).
6. **AI** — `POST /api/ai/generate-vocab` → array of
   `{word:string, meaning:string, pronunciation, exampleSentence, ...}`; `count` out of range
   → 400, missing topic → 400; quota-exceeded → 403. `enrich-word` and `save-vocab` return the
   same shape; `X-AI-Linked-To-Deck` header present (F145).

## Role authorisation (both directions)

All pass (`deep-probe.json` → `roles`, 25/25):

* student → admin routes **403**, anon → **401** (`/api/admin/stats|users|lessons|exercises`,
  `/api/v1/admin/speaking-prompts|video-lessons`, `/api/admin/exercises/ai/status`);
* student cannot write (`POST /api/lessons` 403, `POST /api/admin/lessons` 403,
  `PUT`/`DELETE /api/vocabulary/*` 403);
* admin → 200 on the same routes;
* anon rejected on protected (`/api/streak/snapshot`, `/api/dashboard/stats`,
  `/api/users/progress`, `/api/games/submit` → 401);
* genuinely public still public (`/api/lessons`, `/api/leaderboard`,
  `/api/vocabulary/search` → 200 anon).

## Cleanup

Every row this phase created was removed **by enumerated criteria** and the residue asserted 0:

* `api-sweep.js`: `AUDIT_DECKS=0 AUDIT_VOCAB=0 AUDIT_LESSONS=0 AUDIT_PAY=0`, 0 SQL `Msg` errors.
* `deep-probe.js`: `DEEP_LESSONS=0 DEEP_DECKS=0 DEEP_EX=0`, 0 SQL `Msg` errors.
* Manual probes: `LEFT_LESSONS=0 LEFT_DECKS=0 LEFT_VOCAB=0`.

Parity before and after: `1470|43735|72|118|29|15|4|126|10|5` — unchanged.

> Note on the parity watch: during this phase a **concurrent phase agent** was active in the
> same workspace (files `tmp/v13/adm*.js` written 03:14, `evidence/shots/v13-premium-768.png`
> 03:15). `PremiumCheckout.vue` calls `POST /api/v1/payment/create-order` **on mount**, so
> driving that route mints PENDING payment rows. Two such rows appeared at 03:11:08/11 and two
> more at 03:20:30/33 (`ENGD3571067F2AD`, `ENG22D3724B052A`, `ENG9AB0FBA2193E`, `ENGAA7BF76F3F8A`);
> this phase's own order code `ENGD37E2B2EE0AC` had **0** rows. Payments briefly read 128 and
> returned to the 126 baseline once the other agent cleaned up. This is the other agent's
> footprint, not this sweep's — recorded here so the drift is not mis-attributed.

## Harness corrections made (audit tooling, not app source)

`sweep/v13/api-sweep.js` still carried two stale v12 paths that would have made its own
cleanup wrong:

* line 554 ran `sweep/v12/_cleanup.sql` instead of the file it had just written →
  `AUDIT-V13-*` decks would have leaked;
* line 569 wrote its result JSON into `.specify/specs/audit-v12-full/evidence/`.

Both repointed to `v13`. No application source was modified.
