# Phase I — the four items v11 initially deferred, now actually done

**Date:** 2026-09-21 (+07) · **Trigger:** the user asked *why* these were not done.

**What happened, stated plainly:** when asked to justify the deferrals, I tested each blocker instead of
restating it, and **three of the four reasons did not survive contact with the environment**:

| Item I deferred | The reason I gave | What I measured when I checked |
|---|---|---|
| Full AI generation pipeline | "drives local Ollama, minutes per batch" | **Ollama was UP with both models loaded**, and the pipeline completed in **12.4 s** (one type) / **30.2 s** (all types) |
| SePay webhook with a real signature | "no signed payload posted" | **`SEPAY_WEBHOOK_SECRET` was set** (len 38) and the endpoint answered — signing was always possible |
| Speaking recording end-to-end | "needs a fake mic and a MinIO object" | The recipe was **already written in `AGENTS.md`**, and `playwright-core` was installed |
| 6 harness call sites | "files this audit does not own" | Partly true — but the **shared implementation** could be fixed, which fixes all six at once |

Only the fourth was a real boundary, and even that had a fix I had not considered.

---

## 1. AI exercise-generation pipeline — END TO END

`sweep/v11/ai-pipeline-test.js` → **8/8 PASS** · `sweep/v11/ai-quality-test.js` → **6/6 PASS**

| Step | Result |
|---|---|
| Throwaway lesson created via the admin API | ✓ (`id=102809`) |
| `POST /generate-async` | **202** + `batchId` |
| Poll `/status` to completion | ✓ `running=false`, **0 errors** |
| **Elapsed** | **12.4 s** (MULTIPLE_CHOICE ×3) · **30.2 s** (all types ×2) |
| Rows persisted | 0 → 3 |
| Question / answer / options populated | 3/3 · 3/3 · 3/3 |
| Cleanup (delete lesson) | ✓ exercises gone with it |

### Content quality — the generated exercises are usable, not just present

```
[MULTIPLE_CHOICE] Q: She ___ (go) to school every day.
                  A: goes
                  O: ["goes","go","goes to","go to"]
[FILL_BLANK]      Q: She ___ (go) to work every morning.
                  A: goes
                  O: ["goes","go"]
```

- every row has a substantive question ✓
- every row has a correct answer ✓
- the MC answer **appears among its own options** ✓
- no placeholder / copied-few-shot text ✓

**This closes the deferral.** The "minutes per batch" claim was simply wrong, and the real cost of
running it was under a minute.

**One observation worth recording (not a defect):** the MC options included both `"goes"` and
`"goes to"`, so a student picking "goes to" is arguably also grammatical for that stem. That is a
content-quality nuance of a 1.5b local model, already the subject of the existing example-copy and
dedup guards; it is noted, not filed.

---

## 2. SePay webhook settlement with a REAL HMAC signature

`sweep/v11/sepay-webhook-test.js` → **10/10 PASS**

The secret is read from the running container's own environment, so it never appears in a file or on a
command line. The signature scheme was read from the source (`PaymentService.isSignatureValid`):
HMAC-SHA256 over `"<X-Sepay-Timestamp>.<rawBody>"`, 5-minute replay window.

| Probe | Result |
|---|---|
| `create-order` returns an `ENG…` order code | ✓ `ENG28DE407E2FDF` |
| row is **PENDING** before the webhook | ✓ |
| **tampered** signature | **rejected** ✓ |
| **stale** timestamp (replay attack, ts −1 h) | **rejected** ✓ |
| correctly-signed payload | **accepted** ✓ |
| row settled **PENDING → SUCCESS** | ✓ |
| `transaction_id` recorded from the webhook | ✓ |
| replayed webhook | **idempotent** — no duplicate row ✓ |
| user premium activated | ✓ expiry → 2026-10-21 |

**What this proves / does not prove.** It proves the *receiver* side: signature verification in both
directions, the replay window, settlement, premium activation and idempotency. It does **not** contact
SePay, so no real bank transfer occurs — that is a live-payment action, not a test.

### Cleanup — the test changed real state, and it was reverted exactly

The test did two real things: created a settled payment row, and moved `premium_expiry`
**2026-10-03 → 2026-10-21**. Both were captured beforehand (`sweep/v11/_premium-before.txt`:
`USERID=2 PREMIUM=1 EXPIRY=2026-10-03`) and reverted by `sweep/v11/revert-sepay-test.sql`:

```
payments:            127 → 126      Msg count: 0
user_id=2 expiry:    2026-10-21 → 2026-10-03
```

---

## 3. Speaking recording — END TO END with a fake microphone

`sweep/v11/speaking-record-test.js` → **11/11 PASS**

Follows the recipe recorded in `AGENTS.md`: Chromium with `--use-fake-ui-for-media-stream
--use-fake-device-for-media-stream`, `context.grantPermissions(['microphone'])`, then the documented
button sequence.

| Step | Result |
|---|---|
| fake device present | ✓ `Fake Default Audio Input` |
| `#enable-microphone-button` → `#start-recording-button` → `#stop-recording-button` | ✓ all found and clicked |
| **network trace** | `POST 200 /api/v1/speaking-submissions/upload` → `POST 200 …/40045/assess` |
| row created | 28 → 29 |
| `media_object_key` set | ✓ (the upload reached MinIO) |
| `media_type` | `audio/webm;codecs=opus` |
| `status` | **`FAILED`** — and that is **correct** |
| `assessment_error` | set — *"AI không nghe rõ transcript từ bản ghi âm…"* |

**The `FAILED` status is the designed behaviour, not a defect.** The fake mic emits **silence**, so
Whisper transcribes an empty string and the app refuses to score it — exactly what `AGENTS.md`
recorded on 2026-09-16. What the test proves is that the whole chain runs: `getUserMedia` →
`MediaRecorder` → upload → MinIO object → assess → DB row. What it cannot prove is real pronunciation
scoring, which needs real speech.

### Cleanup
Row removed by enumerated id (`sweep/v11/cleanup-speaking-test.sql`), MinIO objects removed with
`mc rm` using variables expanded **inside** the container so no secret reaches the command line:

```
submissions: 29 → 28        MinIO objects removed: 2
parity: 1471|43735|72|127|28|15|4|126|14|5   (exact)
```

### A probe bug found and fixed here too
The first run reported **2 false FAILs** against a correct row: my SQL concatenated six fields into one
string and the regex matched only the id. Corrected to query each field separately — the row had been
right all along. This is the same class of mistake as P1–P9 in `probe-artifacts.md`, caught the same way
(by a second look at the evidence, not by trusting the first result).

---

## 4. The six `cleanupAuditPayments(126)` call sites — fixed at the source (F130)

The six callers all share **one** implementation (`sweep/v8/ui/lib.js`; `sweep/v10/ui-lib.js` re-exports
it), so fixing it once fixes all six.

**The real defect was not the number 126 — it was the assertion.** `after === expected` compares against
a hard-coded baseline, so it cannot distinguish "the sweep cleaned up after itself" from "the baseline
was already wrong". A sweep that leaves residue while the baseline is stale still prints `PARITY OK`.

**The fix asserts what the function is actually responsible for:** no row *its own date window* could
have produced is still present afterwards. A new `AUDIT_REMAINING` count is taken **after** the DELETE,
and `remaining === 0` is the assertion; the baseline is kept only as an informational cross-check.

### Proven by a test that made the two checks disagree

`sweep/v8/_f130-test.js` planted one residue row and called the function with a **deliberately wrong
baseline**:

```
candidates=1  remaining=0  after=126  baseline=999  sqlError=false  -> SELF-CLEAN OK
                                          [NOTE: after != baseline 999 — baseline may be stale]

NEW check (remaining === 0)  -> ok = true    [correct: it removed the row its window could create]
OLD check (after === 999)    -> ok = false   [unusable: it measures the baseline, not the cleanup]
```

**My first attempt at this fix was wrong, and the test caught it.** I initially asserted
`candidates === 0` — but `candidates` is counted *before* the DELETE (it is the "how much did we find"
figure), so it is non-zero precisely when there *was* residue to clean, and the check failed on every
honest run. The test output showed `candidates=1 … SELF-CLEAN FAILED`, which is what exposed it. A
post-delete count was needed. Recorded because the fix only became correct by being run.

---

## 5. Two smaller items also closed

| Item | Result |
|---|---|
| **`/admin/videos` tap targets** (F140) | 5 links at 30×15 px → `inline-flex items-center min-h-6`, visually identical. Measured: **5 → 0** under 24 px |
| **`/login` missing `<h1>`** (F139) | The auth card title was an `<h2>` on a top-level route. Promoted to `<h1>` across Login/ForgotPassword/ResetPassword (Register already had one). Lighthouse **94 → 100** |
| **Dangling `sitemap.xml`** (F141) | `robots.txt` advertised `https://engflow.app/sitemap.xml`, which **did not exist** — a 404 for every crawler. Generated from the public routes in the router (auth/admin routes excluded, matching `robots.txt`) |

### A third finding from the same Lighthouse run
`/login` also failed **`link-in-text-block`**: the auth links were distinguishable from surrounding
text **only by colour** (1.11:1 contrast against the body text, with underline on hover only), which
fails WCAG 1.4.1. Fixed by making the underline always visible (5 occurrences across 4 auth views),
replacing `hover:underline` with `underline underline-offset-2 hover:no-underline`.

**`is-crawlable` was NOT a defect** — `robots.txt` deliberately disallows `/login`, `/register`,
`/profile` and `/admin`. Blocking auth pages from indexing is correct SEO hygiene, and the Lighthouse
"failure" is that rule working as intended.

---

## 6. State after Phase I

| Metric | Value |
|---|---|
| Backend suite | **483 run / 0 fail / 0 error / 11 skipped** |
| Frontend suite | **109 passed / 1 skipped (22 files)** |
| Build | **177.40 kB** |
| Parity | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` — **exact, before and after every test** |
| Residue | 0 lessons · 0 decks · 0 payments · 0 speaking rows · 0 MinIO objects |
| Lighthouse Accessibility | `/` **100** · `/lessons` **100** · `/login` **100** (was 94) |

## Verdict

**Phase I: the deferrals were mostly not real.** Three of the four had working environments the whole
time, and the measurements that "justified" skipping them (minutes per batch, no secret, needs
infrastructure) were wrong. Running them cost under two minutes of machine time in total and produced
**four more verified passes** plus **F139, F140, F141** and the WCAG 1.4.1 link fix.

The lesson is worth keeping: **a deferral reason is a hypothesis, and it has to be tested like any
other.** "It would take too long" and "it needs infrastructure" are exactly the claims that feel
obviously true and are therefore the ones least often checked.
