# Probe artifacts — candidates that were FALSIFIED, not "fixed"

A probe that reports a defect the application does not have is **itself a defect**: acting on it means
editing correct code. Every candidate below was raised by a first probe, then killed by a **second
independent probe** before it could become a finding. They are recorded so a future run does not
re-raise them, and so the count of "real findings" is not inflated by them.

---

## P1 — "1-char vocabulary search should return 400"

**First probe said:** `GET /api/vocabulary/search?keyword=a` returned **200**, expected 400.
**Verdict: probe bug.** The expectation was invented, not read from the source.

`VocabularyController.search`:
```java
if (query.isBlank() || query.length() < 2) {
    return ResponseEntity.ok(List.of());
}
```
**200 + `[]` is the designed contract** for a too-short query — a soft empty result, not a client error.
Second probe confirmed: 1 char → `[]`; 2 chars → 5 results.

*Corrected in `sweep/v11/api-sweep.js` with a comment.*

---

## P2 — "A private deck is readable by an anonymous visitor"

**First probe said:** created a deck, then `GET /api/decks/{id}` as anon returned **200** — expected 403/404.
**Verdict: probe bug.** The probe never established the precondition it then asserted.

`Deck` declares `@Builder.Default private Boolean isPublic = true;`, and `DeckService.createDeck` only
overrides it when the request supplies a value. My probe omitted `isPublic`, so the deck was **public by
design**.

Second probe, with `isPublic: false` set explicitly:

```
create private deck  -> isPublic=false
anon GET             -> 400  (correctly blocked)
owner GET            -> 200  (correct)
```

**The access control works.** The probe did not.

*Corrected in `sweep/v11/api-sweep.js`.*

---

## P3 — "`/api/ai/generate-vocab` returns 504 — the AI feature is broken"

**First probe said:** authenticated `POST /api/ai/generate-vocab` → **504**.
**Verdict: probe bug (premature conclusion).** The probe reported a transient latency as an outage.

Rather than write it up, I probed the dependency directly:
```
GET http://localhost:11434/api/tags  ->  200   (Ollama is UP)
```
Ollama was healthy, so the 504 was a **cold-model timeout**. Re-running the same call returned **200**
once the model was warm — consistent with `AGENTS.md`'s recorded ~5.7 s model-swap cost and the
deliberately-raised 180 s LLM timeout (v6 F29).

**Recorded as a latency characteristic, not a defect.** Had it been written up on first observation it
would have been a false finding — and "the AI is broken" is exactly the kind of claim that invites
wasted work.

---

## P4 — "White `▶` play glyph on `/videos` is 1.1:1 — invisible"

**First probe said:** `<span class="text-white text-xl ml-1">▶</span>` measured white-on-`rgb(241,245,249)`
= **1.1:1**, on five cards.
**Verdict: probe bug — wrong background resolution.**

The probe walked ancestors and returned the first **fully opaque** background, discarding partial-alpha
layers beneath it. The glyph's actual parent is `bg-foreground/85` (`rgba(30,41,59,0.85)`), which the
probe skipped in favour of the thumbnail placeholder further up.

A corrected probe that **composites layers bottom-up**:

```
glyph: color rgb(255,255,255)
parent bg: rgba(30,41,59,0.85)
composited: rgb(62,72,88)
ratio: 9.24:1     (passes 4.5:1 body and 3:1 large)
```

**The glyph is fine.** This artifact is also a warning about the probe itself: any future contrast
measurement on this project must composite alpha layers, or it will report false failures on every
translucent surface.

---

## P5 — "`.app-plan__badge` is the only cause of the /premium overflow"

**First probe said:** the badge (`right=781`) overhung the 753px document.
**Verdict: incomplete, not wrong.** The badge was a real contributor — but fixing only the badge left
**+3/+4px** of overflow at 768–800px.

The second probe isolated the second cause: `scale(1.1)` on `.app-plan--featured` widens the card ~17px
per side at `md+`. Both had to be fixed. Recorded because "the first plausible cause" was not the whole
cause, and stopping there would have left the defect open at exactly the breakpoint where it was worst.

---

## P6 — "3 duplicate `(word, lesson_id)` rows in `vocabulary`"

**First probe said:** `GROUP BY word, lesson_id HAVING COUNT(*) > 1` returned 3 rows.
**Verdict: probe bug — wrong grouping key.**

```
collaborate  lesson_id=NULL  vocab_id=10089 (OXFORD5000 -> deck "Oxford 5000 (C1-C2)")
collaborate  lesson_id=NULL  vocab_id=10078 (TOEIC      -> deck "Workplace English")
innovate     ...30124 (AI_GENERATED) / ...10094 (OXFORD5000)
negotiate    ...10038 (TOEIC)        / ...10083 (TOEIC)
```

The same English word legitimately belongs to **more than one deck**. Grouping by `(word, lesson_id)`
collapses them because `lesson_id` is NULL for every deck-owned row. The correct key includes the deck.

**Not a defect** — and deleting these rows would have destroyed real vocabulary.

---

## P7 — My own SQL audit script, drafts 1 and 2

**First probe said:** a clean DB audit (exit code 0).
**Verdict: probe bug.** `sqlcmd` **exits 0 even when a batch fails**, so exit code alone proved nothing.

Draft 1 produced **8 × `Msg 207 Invalid column name 'id'`** — I had assumed every primary key is `id`.
It is not (`vocab_id`, `deck_id`, `lesson_id`, `user_id`, `attempt_id`, `deck_word_id`).
Draft 2 produced **3 × `Msg 207 Invalid column name 'user_id'`** — `lessons` has no author column, and
`decks`' owner column is `owner_id`.

Both drafts reported "clean" while failing. Only the **`Msg \d+` scan** caught them. Final run: 0 errors.

---

## P8 — "`video_attempts` has two duplicate foreign keys to `users`"

**First probe said:** two FKs from `video_attempts` to `users` — suspected duplication.
**Verdict: probe bug — different columns.** `user_id` and `graded_by` are distinct columns that both
legitimately reference `users`. Resolving the FK columns before writing it up showed correct modelling.
(Same for `speaking_submissions`: `user_id` + `graded_by`.)

---

## P9 — "Anonymous users are wrongly redirected to `/premium` instead of `/login`"

**First probe said:** anon hitting `/videos`, `/speaking`, `/speaking/history` landed on `/premium`, not
`/login` — 3 failures against my expected-landing model.
**Verdict: probe bug — wrong model of intended behaviour.**

The guard order in `router/index.js` checks `requiresPremium` **before** `requiresAuth`, so a premium
route bounces an anon visitor to `/premium` **with `?redirect=`**, where the page presents **both
Đăng nhập and Đăng ký** CTAs. Decisive re-probe:

```
/videos           -> /premium?redirect=/videos           heading "EngFlow Premium"  login+register CTAs present
/speaking         -> /premium?redirect=/speaking         same
/speaking/history -> /premium?redirect=/speaking/history same
```

This is a deliberate **upsell funnel**: show the value proposition before asking the visitor to sign in.
Not a defect. My expected-landing table encoded "auth before premium" without reading the guard order.

---

## P10 — "The design system / Be Vietnam Pro font must be implemented"

**Verdict: not a finding at all — already done, and reporting it as work would be fabrication.**

Verified live in the DOM across every element:
```
body font: "Be Vietnam Pro", system-ui, sans-serif
distinct primary font families on the page: 1   ("Be Vietnam Pro")
```
and in source: `index.html` loads only `Be+Vietnam+Pro:wght@400;500;600;700;800;900`;
`tailwind.config.js` maps `sans`, `heading` **and** `mono` to BVP; `design-system.css` sets `--geo-font`
to BVP; **zero** occurrences of Outfit or Plus Jakarta anywhere in `frontend/src`.

The "replace all fonts with Be Vietnam Pro" instruction therefore **requires no work**. It is recorded as
a **verification**, which is why the font does not appear as a finding above.
