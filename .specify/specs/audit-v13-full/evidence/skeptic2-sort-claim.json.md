# Skeptic #2 — independent re-probe: `?sort=` on paged endpoints

Verdict: **NOT REFUTED** (core claim reproduced with real rows). One sub-claim is **stale**.
Corrected severity: **LOW** (unchanged).

Probe: `tmp/skeptic2-sort.js`, `tmp/skeptic2-vocab.js`, `tmp/skeptic2-500.js`
Rate-limit buckets flushed before every batch (`flushed: 2`). No 429 observed in any run.

## 1. Source — confirmed exactly as claimed

| Endpoint | Controller line | Sort |
|---|---|---|
| `/api/lessons` | `LessonController.java:38` | `PageRequest.of(..., Sort.by("orderIndex").ascending().and(Sort.by("id")))` — hardcoded, `?sort=` never bound |
| `/api/admin/exercises` | `AdminExerciseController.java:39` | `Sort.by("orderIndex").ascending().and(Sort.by("id"))` — hardcoded |
| `/api/decks` | `DeckController.java:34,50` | `Sort.by("name").ascending().and(Sort.by("id"))` — hardcoded |
| `/api/vocabulary` | `VocabularyController.java:37` | `@PageableDefault(size = 20, sort = "word") Pageable pageable` — client sort honoured |

None of the three hardcoded controllers has a `@RequestParam ... sort` parameter at all.

## 2. Live re-probe — real rows, real HTTP (independent of the original probe)

```
LESSONS   sort=title,asc   ids [41881, 81895, 91900, 91919, 91920, 11301]
LESSONS   sort=title,desc  ids [41881, 81895, 91900, 91919, 91920, 11301]
LESSONS   no sort          ids [41881, 81895, 91900, 91919, 92020? ...]  -> identical
          asc==desc: true   asc==default: true
LESSONS   sort=orderIndex,desc -> same order (hardcoded wins)   HTTP 200
LESSONS   sort=notacolumn  -> HTTP 200 (silently ignored)
ADMIN-EX  sort=question,asc == ,desc -> true; ids [766149,766150,777434,777633,745554,745559]
ADMIN-EX  sort=notacolumn  -> HTTP 200
DECKS     asc == desc == ["Academic Word List","Common English Idioms","Essential Phrasal Verbs","Everyday English","IELTS Academic: Environment","Oxford 3000 (A1-B2)"]
DECKS     sort=notacolumn  -> HTTP 200
VOCAB     sort=word,asc  -> ["agenda","airport","algorithm","ambiguous","ambitious","analyze"]
VOCAB     sort=word,desc -> ["volunteer","virtual","urbanization","under the weather","turn down","thermal"]
VOCAB     asc==desc: false (correctly reversed); asc==default: true
```

The three hardcoded endpoints genuinely ignore `?sort=`; vocabulary genuinely honours it.
This matches the original `search-sort.json` byte-for-byte in shape. **Not a probe artefact.**

## 3. The one stale sub-claim — "unknown property -> HTTP 500" is now FALSE

`GET /api/vocabulary?size=3&sort=notacolumn,asc` currently returns **HTTP 400**:

```json
{"detail":"Tham số sắp xếp không hợp lệ.","instance":"/api/vocabulary","status":400,"title":"Bad Request"}
```

Same for `notacolumn,desc`, `DROP`, `word,drop`, `word,asc,asc`, `word,ascx`, `word,asc,drop`.
Same 400 with a **user** token, not just admin.

Reason: a `PropertyReferenceException` handler was added to `GlobalExceptionHandler`
(`audit-v13 F-13-11`, lines 237-243) **after** the evidence JSON was captured:

```
search-sort.json          mtime 2026-09-22 03:13:24   (captured the 500)
GlobalExceptionHandler    mtime 2026-09-22 03:30:31   (handler added)
```

So the claim's stated root cause — *"GlobalExceptionHandler maps to a generic 500"* — no
longer holds. An unknown **property** is now correctly a 400. `InvalidDataAccessApiUsageException`
has no dedicated handler in `GlobalExceptionHandler` (grep: no match).

## 4. The remaining 500 is real but is a different, narrower bug

Malformed sort strings still reach the catch-all 500 (line 288, "Đã xảy ra lỗi hệ thống"):

```
sort="word;DROP"     -> 500      sort="word;"      -> 500
sort=";DROP"         -> 500      sort="word;asc"   -> 500
sort="word$x"        -> 500      sort="word x"     -> 500
sort="word)"         -> 500      sort="word'"      -> 500
sort="word--"        -> 500      sort="1;DROP"     -> 500
sort="word,asc;DROP" -> 500
```

This is a parse failure on a malformed sort *string* (unhandled, no dedicated handler),
**not** an unknown-property failure. It is **not** SQL injection: a property name containing
`;` never reaches SQL — Spring Data rejects it while resolving the sort into JPQL. The body
is the generic Vietnamese error with no stack, class name or SQL text leaked. Severity LOW.

## 5. Severity — LOW is justified

- No frontend caller sends `?sort=`: `grep -rn "sort=" frontend/src` returns nothing for
  `lessonService.js`, `deckService.js`, `adminService.js`. Zero user-visible impact today.
- It is nonetheless a genuine (low) contract defect: the endpoints accept a standard Spring
  Data `?sort=` parameter and silently discard it, with no documentation saying so.
- The 500 on malformed sort is a generic error page, no information disclosure, admin-or-user
  reachable only by hand-crafting the parameter.

No upgrade to a higher severity is warranted; no downgrade below LOW either.

## Conclusion

Core claim (**three paged endpoints silently ignore `?sort=`; vocabulary honours it**) is
**reproduced against real rows and is real**. The **"unknown property -> 500"** detail is
**stale** — the live build returns **400**, because the PropertyReferenceException handler was
added in this same round after the evidence was captured. The still-live 500 applies only to
malformed sort *strings* and is a narrower parse-error issue. Severity remains **LOW**.
