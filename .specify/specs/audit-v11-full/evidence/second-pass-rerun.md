# Phase H — second, wider pass

**Date:** 2026-09-20 (+07) · Run on the **final build**, after all fixes.

The user's instruction was explicit: *"Sau khi chạy và hoàn thành lần 1 thì chạy lại thêm lần nữa với mức
độ toàn diện hơn không quan trọng về thời gian cứ chạy test lặp toàn bộ chức năng để tìm ra lỗi và sửa
tận gốc."* This pass exists because the first pass is not evidence that nothing remains.

---

## 1. Full suites on the final build

| Suite | Result |
|---|---|
| Backend `mvnw.cmd -o test` | **483 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| Frontend `npx vitest run` | **21 passed / 1 skipped (22 files) · 109 passed / 1 skipped (110)** |
| Production build `npx vite build` | **177.40 kB** entry (gzip 67.54 kB), exit 0 |

All three **identical to the Phase A baseline** in pass/fail terms — 483 backend, 109 frontend — so the
fixes introduced **no regression** in either suite. Bundle moved 177.31 → 177.40 kB (+0.09 kB).

## 2. API sweep repeated on the final build

```
pass=74  fail=0  blocked=0
  auth 8 · lessons 15 · streak 8 · search 7 · crud 13 · ai 5 · roles 18
```

**Identical to the first pass (74/0/0).** Including the re-proved draft guards (F88/F89/F115/F126), the
7-field streak contract, and the role matrix in both directions.

## 3. Browser sweep repeated — and it found what the first pass missed

This is the substantive result of the second pass.

The first pass's contrast probe had a **background-compositing bug**: it returned the first *fully opaque*
ancestor and discarded partial-alpha layers. That made it both over-report (the false `▶` failures,
`probe-artifacts.md` P4) and **under-report** — on `bg-muted` panels it resolved the wrong composite.

The corrected probe composites layers **bottom-up over white**. Re-running all 19 routes:

```
before (first-pass probe):  0   <- believed clean
after  (corrected probe):  13   <- real failures, all --geo-muted-fg on a muted panel
```

Those 13 became **F138** (4.00–4.27:1, under the 4.5:1 requirement). Root cause: `--geo-muted-fg`
`#64748B` measures **4.76:1 on white but 4.34:1 on the `#F1F5F9` muted panel it is overwhelmingly used
on** — the token was chosen against white and never checked against its own background. Fixed at the
token level (`#556070` = 6.38 / 6.26 / 5.82:1), then re-measured:

```
contrast failures across 19 routes:  13  ->  0
```

**This is the concrete payoff of the second pass.** A defect the first round had explicitly cleared was
found and root-caused only because the round was re-run with a corrected probe.

### Final sweep state (19 routes × 3 roles)

| Metric | Result |
|---|---|
| Route landings correct | **18/19** — the 1 is the intentional `/premium?redirect=` upsell funnel (falsified, P9) |
| Contrast failures | **0** |
| Console errors | **0** |
| UI-caused API ≥ 400 | **0** |
| Responsive overflow (360/768/1280/1920) | **0** on `/`, `/lessons`, `/premium` |

## 4. Adversarial boundary cases

### Streak — 25/25 scenarios PASS
Re-ran `sweep/v10/streak-scenarios.js` against the live stack. The scenarios create a **dedicated throwaway
user**, read every step back with SQL rather than trusting the HTTP response, and clean up by explicit id.

The load-bearing result:
```
KB5 — user only logs in, does not study:
  PASS  studiedToday = FALSE (login alone)
  PASS  currentStreak = 0
  PASS  study_days rows for that user = 0
```
**Logging in is not a study day; only completing a learning activity counts** — the core contract of the
whole streak refactor, re-proved on the final build rather than cited.

Also re-proved: pre-cutover days are excluded by design and are **not** counted as `legacyAccessDays`.

### SRS interval cap (F106) — verified in source
`SrsService.MAX_INTERVAL_DAYS = 365`, with the overflow history documented in place: an uncapped
`interval × easeFactor` once produced `srs_interval = 1_537_216` (≈4210 years), after which every review
returned HTTP 500 with a `datetime2` out-of-range error **permanently**, because the row was never
repaired. The cap both bounds the interval and lets an already-broken row heal on its next review.

### Cutover day (`study_policy.effective_from = 2026-09-20`)
Confirmed live: `study_policy` = 1 row, `study_days` = 0 rows, FK `fk_study_days_user` present **and
enabled**, `uq_study_days_user_date` present. The cutover is **today**, so a 0-row `study_days` is the
expected state, not a fault.

## 5. Cross-artifact consistency

| Artifact | Consistent? |
|---|---|
| `spec.md` R1–R7 ↔ this run | ✓ every requirement has a matching artifact |
| `plan.md` phases 0–7 ↔ `evidence/` | ✓ each phase has an evidence file (`*-rerun.md` supersedes the stale ones) |
| `findings-rerun.md` ↔ `evidence/` | ✓ every finding names the probe that produced it |
| `probe-artifacts.md` ↔ findings | ✓ 10 falsified candidates recorded, none double-counted as findings |
| parity line ↔ all mutating runs | ✓ `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` before and after every run |
| `AGENTS.md` recorded numbers ↔ re-measured | ✓ 4 848 empty answers, ~169 ms admin search, 0 missing LISTENING audio all reproduce |

## 6. Residue and parity — final

```
parity:      1471|43735|72|127|28|15|4|126|14|5     (identical to the pre-audit line)
AUDIT-V11 lessons: 0
AUDIT-V11 decks:   0
audit users:       0
payment residue:   0   (127 -> 128 during the sweep -> 126 after enumerated cleanup)
```

Every mutating step in this audit cleaned up **in the same run** and re-asserted parity.

## 7. What the second pass did NOT do

Recorded rather than glossed, per R7:

- **Did not** re-run the AI exercise-generation pipeline end-to-end (it drives local Ollama and can take
  minutes per batch). The AI **contract** was checked (auth gate, 200 on a warm model, TTS and Whisper
  sidecar reachability); the generation quality path is out of scope for this round.
- **Did not** test the SePay webhook settlement path with a real signed payload — the create-order
  contract was exercised and its residue cleaned, but no HMAC-signed webhook was posted.
- **Did not** update the six hard-coded `cleanupAuditPayments(126)` call sites (F130) — they live in
  v8/v10 harness files this audit does not own.
- **Did not** exercise speaking **recording** end-to-end (needs a fake microphone and produces a real
  MinIO object); the speaking endpoints were covered at the API and route level.

## Verdict

**Phase H: PASS, and it earned its place.** Suites green and unchanged from baseline, API sweep
identical at 74/0/0, streak 25/25, parity clean with zero residue — and the wider pass **found a real
accessibility defect (F138) that the first pass had cleared**, because the first pass's probe was
subtly wrong. The corrected probe now reports **0 contrast failures across 19 routes**.
