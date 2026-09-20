# Phase C — API sweep over the live container

**Date:** 2026-09-20 (+07) · **Target:** `http://localhost:8080` (running container, not a local JVM)
**Probe:** `sweep/v11/api-sweep.js` → `sweep/v11/api-sweep-result.json`
**Result: 74 pass / 0 fail / 0 blocked**

---

## Method, and the two hazards designed around

**Rate-limit buckets.** The global bucket is 100/min/IP. The harness flushes `rate_limit:*` **before each
batch**, because a harness that charges its own bucket manufactures fake 429s (finding F109, reproduced
once deliberately in v10). Flushing uses `execFileSync` with argv — **not** `execSync` with an
interpolated string — because a Redis key is data and must never reach a shell (security-guidance caught
this in v10).

**Concurrency — measured, not assumed.** I read the live Redis keys and confirmed there are only **two**
usable buckets: `rate_limit:172.18.0.1:global` (host) and `rate_limit:172.18.0.7:global` (Vite proxy).
`TRUSTED_PROXY_ENABLED` is unset, so `X-Forwarded-For` cannot shard them. A 6-way parallel fan-out would
therefore be **F109 reproduced on purpose**. The sweep is **serial**. (SQL and analysis parallelise
freely; HTTP does not.)

**Authentication.** `POST /api/auth/login` returns a **flat** object with `.token` (not `data.token`).
Credentials are the seeded `user@gmail.com` / `admin@gmail.com` with password `123456` — *not* the
seeder's `password123` fallback, which is why my first login attempts returned 401. Both roles verified
before the sweep ran.

---

## C1 — Auth (8/8)

| Probe | Result |
|---|---|
| `GET /api/auth/me` (student) | 200; `email` round-trips; `isAdmin === false` |
| `GET /api/auth/me` (admin) | 200; `isAdmin === true` |
| `GET /api/auth/me` anon | **401** |
| login with wrong password | **401** |
| login with malformed email | **400** (validation, distinct from 401) |
| `GET /api/auth/me` with a garbage JWT | **401** |

## C2 — Lessons / Exercises (15/15)

| Probe | Result |
|---|---|
| `GET /api/lessons?page=0&size=5` | 200, `content[]` non-empty |
| lesson list does **not** leak the `content` LOB | confirmed — projection in use (audit-v9 F108 holds) |
| `GET /api/lessons/{id}` | 200 |
| `GET /api/lessons/{id}/exercises` | 200 |
| `?includeAnswers=false` strips `correctAnswer` | confirmed |
| `?includeAnswers=true` as **anon** does not leak answers | confirmed |
| `?includeAnswers=true` as **admin** | 200 |
| `GET /api/lessons/{id}/structure` (auth) | 200 |
| `GET /api/lessons/{id}/exercises/content` | 200 |
| grade as anon | **401/403** |

### Draft-visibility regression checks — all four prior fixes still hold

Against draft lesson `10888`:

| Guard | Finding it re-proves | Result |
|---|---|---|
| `GET /api/lessons/10888` as **anon** | F88 | **404 — hidden** |
| `GET /api/lessons/10888` as **student** | F89 | **404 — hidden** |
| `GET /api/lessons/10888/structure` as **student** | F126 | **404 — hidden** |
| `GET /api/lessons/10888/exercises` as **student** | F115 | **blocked** |

## C3 — Streak (8/8)

`GET /api/streak/snapshot` → **200**, and the **contract is complete**: all **7** fields declared by
`StudySnapshot.java` are present in the response —

```
today, currentStreak, studiedToday, effectiveFrom, studiedDays, legacyAccessDays, legacyHistoryAvailable
```

Type checks: `currentStreak` is a number, `studiedToday` is a boolean, `studiedDays` is an array.
`/api/streak/current` and `/api/streak/history?days=30` both 200; anon → **401**.

> I read the DTO before writing the probe, so this asserts the declared contract rather than "a 200
> arrived". A 200 with a missing field would have passed a naive check.

## C4 — Search / sort (7/7)

| Probe | Result |
|---|---|
| `GET /api/vocabulary/search?keyword=ab` | 200 (permitAll) |
| `GET /api/vocabulary/search?keyword=a` (1 char) | **200 + `[]`** — see below |
| `GET /api/vocabulary/dictionary/hello` | reachable (fail-soft to `[]` on upstream error) |
| `GET /api/vocabulary` anon | **401** (documented: only `/search` and `/dictionary/*` are permitAll) |
| `GET /api/admin/exercises?q=the` (admin) | 200 |
| same, anon | **401/403** |

### `?sort=` — measured, and it is IGNORED

```
?sort=title,asc   -> [41881, 81895, 91900, 91919, 91920]
?sort=title,desc  -> [41881, 81895, 91900, 91919, 91920]
```

**Identical order** → the `?sort=` parameter has **no effect**. This **confirms** audit-v10's note rather
than inheriting it. Recorded as a **product gap, not a defect**: nothing in the UI exposes a sort
control, so no user is misled. Anyone who assumes a sort feature exists would be.

## C5 — CRUD through the API, with in-run cleanup (13/13)

**Deck** (created private, exercised, deleted):

```
create 200 (id=50057)  ->  read 200, name round-trips  ->  update 200, edit persisted
->  anon GET private deck = 400 (blocked)  ->  delete 200  ->  re-GET = 404 (gone)
```

**Lesson** (admin, created, exercised, deleted):

```
create 200 (id=102733) -> read 200 -> update 200 -> delete 200 -> re-GET = 404 (gone)
```

**Cleanup verified, not asserted:**

```
SELECT COUNT(*) FROM decks   WHERE name  LIKE 'AUDIT-V11%'  ->  0
SELECT COUNT(*) FROM lessons WHERE title LIKE 'AUDIT-V11%'  ->  0
```

**Parity after the mutating sweep is unchanged:**

```
1471|43735|72|127|28|15|4|127|14|5     (identical to the pre-sweep line)
```

## C6 — AI / TTS / Whisper (5/5)

| Probe | Result |
|---|---|
| TTS sidecar `:8001/synthesize` | reachable |
| Whisper sidecar `:9002` | reachable |
| `POST /api/ai/generate-vocab` anon | **401/403** |
| `POST /api/ai/generate-vocab` (auth) | **200** (first run: 504 — see below) |
| admin AI status anon | **401/403** |

**The 504 on the first run, and why it is not a defect.** The first sweep saw `504` on
`/api/ai/generate-vocab`. Rather than write that up as a finding, I probed the dependency directly:

```
GET http://localhost:11434/api/tags  ->  200        (Ollama is UP)
```

Ollama was healthy, so the 504 was a **cold-model timeout**, not an outage — and the second run returned
**200** once the model was warm. This is consistent with `AGENTS.md`'s recorded model-swap cost (~5.7 s)
and the deliberately-raised 180 s LLM timeout (v6 F29). Recorded as **an observed latency
characteristic**, not as a defect. Had I written it up on the first observation, it would have been a
false finding.

## C7 — Role matrix, asserted in BOTH directions (18/18)

Six admin-only endpoints × three roles:

| Endpoint | ANON | STUDENT | ADMIN |
|---|---|---|---|
| `GET /api/admin/stats` | 401 ✓ | 403 ✓ | 200 ✓ |
| `GET /api/admin/users` | 401 ✓ | 403 ✓ | 200 ✓ |
| `GET /api/admin/lessons` | 401 ✓ | 403 ✓ | 200 ✓ |
| `GET /api/v1/admin/speaking-prompts` | 401 ✓ | 403 ✓ | 200 ✓ |
| `GET /api/v1/admin/video-lessons` | 401 ✓ | 403 ✓ | 200 ✓ |
| `GET /api/v1/admin/video-attempts` | 401 ✓ | 403 ✓ | 200 ✓ |

Asserting **both** directions matters: a check that only proves "admin is allowed" cannot detect a guard
that has stopped guarding.

---

## Two probe bugs found and falsified (recorded, because a probe error is a defect too)

My first sweep reported **2 failures**. Both were **my probe being wrong**, and both were killed by a
second independent probe before being written up:

| Probe said | Second probe found | Verdict |
|---|---|---|
| "1-char search should be 400, got 200" | `VocabularyController.search` returns `200 + List.of()` when `query.length() < 2` — **by design**. Confirmed: 1 char → `[]`, 2 chars → 5 results | **probe bug** — expectation was invented, not read from source |
| "private deck hidden from anon, got 200" | `Deck` has `@Builder.Default isPublic = true`. My probe never sent `isPublic`, so the deck was **public by design**. Re-probed with `isPublic:false` explicitly → anon **400 (blocked)**, owner **200** | **probe bug** — the probe did not set up the precondition it then asserted |

After correcting both, the sweep is **74/0/0**. Both corrections are now commented in
`sweep/v11/api-sweep.js` with the reason, so a future run does not re-learn them.

---

## Verdict

**Phase C: PASS.** Every area the user named — Auth, Lessons/Exercises, Streak, Search/Sort, CRUD, AI —
was exercised against the live container, with the streak contract checked field-by-field, role
permissions asserted in both directions, and CRUD verified to leave **zero residue** with parity
unchanged. Four previously-fixed defects (F88, F89, F115, F126) were **re-proved live**, not assumed.

Two apparent failures were falsified as probe bugs. One real product gap was confirmed (`?sort=` is
ignored) and classified as a gap rather than a defect. **No new product defect was found by the API
sweep.**
