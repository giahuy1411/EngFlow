# AI feature review + shadowing audio fix (owner-reported)

**Date:** 2026-09-21 (+07) · **Method:** systematic debugging — root cause before any fix

Two reports from the project owner:
1. *"khi tôi vào admin chấm shadowing thì nghe thấy âm thanh lạ chạy rất nhanh không nghe rõ"*
2. *"có một số chức năng có vấn đề và chưa có sự liên kết với nhau"* (AI features)

---

## Part 1 — F144: the shadowing player audibly played recordings at 16x

### Phase 1: root cause, not symptom

The suspect was `fixWebmDuration` in `AdminVideoAttempts.vue`, a 12-line helper whose **stated** job is
cosmetic: MediaRecorder WebM has no Duration element, so the native control shows "∞".

Two independent mistakes, both confirmed by measurement:

**(1) The guard skips only FINITE durations.**
```js
if (Number.isFinite(el.duration) && el.duration > 0) return   // Infinity is NOT finite
```
A MediaRecorder WebM reports `duration === Infinity` — *the exact case the helper exists for* — so the
guard fell straight through into the workaround. Measured on the live page: a real 358 KB recording
reported `Infinity`, and `Infinity`/`NaN` are the only inputs that reach the workaround.

**(2) `playbackRate = 16` + `play()` is AUDIBLE.** The trick only needs the element to advance its
timeline. It never needed to be heard, and it never restored the rate unless `ended` fired.

**That is the reported sound.** The admin's click on the native play control triggered a hidden 16×
playback of the learner's recording.

### What I ruled out first (so the fix targets the real cause)

| Hypothesis | Test | Result |
|---|---|---|
| "The recordings are corrupt" | decoded the EBML header of a real attempt | valid WebM, `1A45DFA3` magic, Opus 48 kHz mono, 24 clusters |
| "Missing Duration element" | searched for `0x4489` in the file | **confirmed absent** — the helper's premise is correct |
| "`application/octet-stream` breaks decoding" | loaded the **same bytes** as `audio/webm`, `application/octet-stream` and `""` | **all three identical**: `duration: Infinity`, correct 1× playback. **Not the cause** |
| "Browser truncates to 0.1s" | measured `currentTime` advance over 1.5 s wall time | real file advanced correctly; only 44-byte stubs report 0.1s |

The third row is why the fix is 12 lines and not a content-type refactor.

### Phase 4: fix, with a test that fails first

Extracted to `frontend/src/utils/webmDuration.js` (unit-testable — the component only reached it
through a template handler). The fix mutes **before** the accelerated seek, restores mute state,
`playbackRate` and `currentTime` in a `restore()` that runs on `ended`, on a 1 s safety timer, and on a
rejected `play()`.

**The test was run against the OLD body first:**
```
× does NOT speed up when duration is Infinity   → expected 16 to be 1
× does NOT speed up when duration is NaN        → expected 16 to be 1
× leaves a decodable recording audible at 1x    → expected 16 to be 1
Test Files  1 failed | Tests  3 failed | 1 passed
```
That is the user's report reproduced as a failing test. Against the fix: **8/8 pass**.

**Live browser, forced `duration = Infinity` on a real page element:**
```
rate → 16  →  play(rate=16, muted=TRUE)  →  pause  →  rate → 1
audibleFastPlay: 0        leftAt16x: false
```

### A SECOND copy of the same bug — on the student-facing page
`views/videos/VideoLesson.vue:552` had a byte-identical copy, so **students heard their own recording at
16× too**. Both now import the shared module; grep confirms no `playbackRate = 16` remains outside the
module's own comment and implementation.

### Recorded, deliberately NOT fixed here
| Observation | Evidence | Why not |
|---|---|---|
| Recordings served as `application/octet-stream` | object keys have **no extension**, so every `endsWith(".webm")` in `MediaProxyController` misses; `X-Content-Type-Options: nosniff` set | **Not the cause** — proven identical decoding above. Latent fragility, recorded rather than bundled into this fix |
| 6 of 15 attempts are 44-byte / 1644-byte stubs | `duration = 0.1` | Test data from earlier probes, not a product defect |

---

## Part 2 — F145: AI-generated vocabulary was saved into a void

### What is wrong
`AiVocabService.saveVocabBatch` builds `Vocabulary` rows with `source = "AI_GENERATED"` and **nothing
else that ties them to a person or a container** — no `lesson_id`, no deck, and the method takes no
`userId`. The endpoint charges **1 AI quota credit**, and the UI reports:

```
'Đã lưu ' + unsaved.length + ' từ vào DB!'
```

…and that is all that happens. Measured in the live DB:

```
ai_vocab_total          = 31
ai_vocab_in_a_deck      = 10
ai_vocab_orphan_no_deck = 21     <- 68% saved into a void
```

The 10 that *are* in a deck are seeder-created (`VocabularyDataSeeder`), not user-saved.

**Why unreachable:** `vocabulary` has no owner column; the rows have `lesson_id = NULL` and no
`deck_words` entry. So `/api/decks/my` cannot contain them, and `/api/vocabulary` + `/search` are
**global** tables, not user-scoped. **The user pays a credit, is told it worked, and can never see the
words again.**

### Fix
`save-vocab` now accepts an optional `deckId` and links each saved word via the existing
`DeckService.addWordToDeck` — which already does the ownership check and is idempotent. The UI gains a
deck picker that defaults to the user's first deck and warns in red when none is chosen.

### A mistake I made and corrected
My first version changed the response body from an **array** to an **object** carrying metadata. That
turned `AiVocabSaveVocabValidationTest` red (`No value at JSON path "$[0].word"`).

**Changing the shape of a live API to add information no client reads is the wrong trade.** I reverted
to the array body and moved the new state into headers instead:

```
X-AI-Saved-Count: 1
X-AI-Linked-To-Deck: true
X-AI-Deck-Id: 50064
```

Verified: body is still an array (`is array: True`), so the existing test and any other client keep
working.

### Verified end-to-end

| Probe | Result |
|---|---|
| `save-vocab?deckId=N` → header | `X-AI-Linked-To-Deck: true` |
| Word readable **through the deck** (`GET /api/decks/N`) | **yes** — `words in deck: 1 -> ['f145verify']` |
| Row in `deck_words` | `f145verify | AI_GENERATED | 50064` |
| No deck → is the response honest? | `X-AI-Linked-To-Deck: false` (was a false "saved!" success) |
| **Ownership**: attach to another user's deck | **HTTP 400 "Bạn không có quyền chỉnh sửa bộ từ này"** — no IDOR |
| New regression tests | **7/7 pass** (was 5; +2 for deck-linking and the not-linked path) |

### Other AI surface reviewed — checked, no defect found

| Feature | Probe | Result |
|---|---|---|
| Shadowing AI grading | `POST /admin/video-attempts/13/ai-grade` | **200 in 1.3 s**; silent recording → `score 0` + clear Vietnamese reason. Works as designed |
| AI quota display | `/api/auth/me` for a premium user | `aiGenerationsRemainingToday: null` = **unlimited, per the documented contract**; the UI's `unlimited` branch handles it. Not a bug |
| Ollama / Whisper reachability | direct probes | Ollama **up** (both models); Whisper `:9002` responding |
| Exercise generation pipeline | `ai-pipeline-test.js` (Phase I) | **8/8**, 12.4 s / 30.2 s, content usable |
| `ai/backfill-answers` | frontend grep | **0 callers** — an orphaned operator tool, documented in `AGENTS.md` as operator-run. Classified as intentional, not a defect |

---

## Final state

| Metric | Value |
|---|---|
| Backend suite | **485 run / 0 fail / 0 error / 11 skipped** (+2 new F145 tests) |
| Frontend suite | **118 passed / 1 skipped (23 files)** (+9: 8 webmDuration + 1) |
| Build | **177.44 kB** (was 177.40) |
| Parity | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` — exact, before and after every probe |
| Residue | 0 probe vocab · 0 probe decks · attempt 13 reverted |

## What the two reports turned out to be

- The **audio** was one helper doing something audible it never needed to do, on **two** pages.
- The **"no interconnection"** was precise: AI-generated vocabulary had no path from "generated" to
  "yours" — it was written to a global table and abandoned. 68 % of existing rows proved it.

Both were found by measuring the live system rather than reading the code: the guard *looks* correct,
and `saveVocabBatch` *looks* like it saves.
