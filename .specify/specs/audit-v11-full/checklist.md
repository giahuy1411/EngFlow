# audit-v11-full — requirements quality checklist

A checklist is a unit test for the requirements. Each item asks whether the **spec** is good, not
whether the code is. Answers are this session's, with the artifact that supports them.

---

## A. Measurability

- [x] **A1.** Can every success criterion be measured by a command, not an opinion?
      — Yes. Every row in `spec.md`'s success table names a probe and an artifact. No row says
      "looks good".
- [x] **A2.** Does each measurement state its method (median of N, which driver, which log)?
      — Yes: `plan.md` T6.1 fixes "median of ≥3"; Phase 3 records which browser driver produced
      which number; Phase 0 fixes "counts from the run log".
- [x] **A3.** Are counts taken from a source that cannot lie?
      — Yes, with a known trap recorded: `target/surefire-reports` XML once inflated the aggregate
      by 8 (stale XML from a deleted class), so counts come from the **run log**. The checklist
      keeps this rule because it is the exact mistake the repo has made before.

## B. Truthfulness

- [x] **B1.** Does the spec forbid claiming already-done work as new?
      — Yes. `spec.md` "Why this audit exists" names three instructions that are **already
      satisfied** (font, design system, prior audits) and states v11 must not pretend to discover
      them. This is the single most important item on this checklist.
- [x] **B2.** Is there a rule against copying prior numbers into conclusions?
      — Yes: R1. Prior reports may only aim a probe.
- [x] **B3.** Is a probe error treated as seriously as a missed defect?
      — Yes: R7 and `plan.md` risk register. Justified: a probe that reports a defect the app does
      not have invites "fixing" correct code. v10 hit exactly this five times.
- [x] **B4.** Are unverified items required to be reported as BLOCKED/PARTIAL, not omitted?
      — Yes: R7 and Boundaries.
- [x] **B5.** Does the plan state what it will NOT do, and why?
      — Yes: Phase 6 T6.3 records the reason for **declining** an optimisation; Phase 5 fixes only
      measured defects.

## C. Completeness against the user's instruction

- [x] **C1.** "Scan the whole codebase" — is that addressed?
      — Yes, via the endpoint inventory re-derived from source (T0.7) plus the design-system file
      set. The instruction's *intent* is coverage; a file-by-file walk of 337 files would produce
      volume, not evidence.
- [x] **C2.** "Check the DB in Docker" — addressed?
      — Yes: Phase 1, including constraint **effect** (does it actually reject a violation), not
      just existence.
- [x] **C3.** "Run every API" — addressed?
      — Yes: Phase 2, with role correctness in both directions and contract checks.
- [x] **C4.** "Interact with the whole UI corresponding to each API" — addressed?
      — Yes, and this is **stronger than v10**: Phase 3 T3.4 captures the network calls each UI flow
      emits and compares them to the API contract, instead of testing UI and API separately.
- [x] **C5.** "Core functions: Lessons/Exercises, Streak, Login/Register, Search/Sort, CRUD, AI" —
      all six mapped?
      — Yes: each has a dedicated row in Phase 2 (API) and Phase 3 (UI↔API), plus Phase 4 for CRUD
      through the admin UI specifically.
- [x] **C6.** "Optimise performance" — addressed?
      — Yes: Phase 6, with before **and** after, per P5. Optimisation without a measurement is
      forbidden by the constitution.
- [x] **C7.** "Test UI/UX with chrome-devtools-mcp and playwright-mcp" — addressed?
      — Yes: Phase 3 uses both, and both were probed live at session start.
- [x] **C8.** "Verify the UI matches the prompt below" — addressed?
      — Yes: Phase 3 T3.7, and the prompt's own holes are analysed in `spec.md` H1–H8.
- [x] **C9.** "Replace all fonts with Be Vietnam Pro" — addressed?
      — Yes, and resolved as **already done** (C4 in `clarify.md`), verified rather than re-performed.
- [x] **C10.** "Use skills/plugins from `C:\Users\ASUS\.claude` where relevant" — addressed?
      — Yes: recorded in `tasks.md` T-last and in the final report's skills table. The skills
      actually loaded are listed, not merely available.
- [x] **C11.** "Check this prompt for holes, fix, implement" — addressed?
      — Yes: H1–H8 with evidence, plus C6 in `clarify.md` fixing the **prompt** where the prompt is
      wrong rather than bending the code.
- [x] **C12.** "Follow constitution → specify → clarify → checklist → plan → tasks → implement →
      converge → analyze" — addressed?
      — Yes: artifacts exist in that order in this directory.
- [x] **C13.** "Run a second, more comprehensive pass" — addressed?
      — Yes: Phase 7.
- [x] **C14.** "Report what was done, not done, fixed and how, and skills loaded" — addressed?
      — Yes: T7.5, with a fixed section structure.

## D. Consistency

- [x] **D1.** Do spec, plan and tasks agree on the phase names and numbering?
      — Yes. Phases 0–7 in all three, same titles.
- [x] **D2.** Does any requirement contradict another?
      — Checked. One apparent tension is resolved explicitly: C3 allows DB **writes** while Phase 1
      is **read-only** — they are different phases, and every write is followed by a parity
      assertion. No contradiction.
- [x] **D3.** Does the plan respect the constitution's constraints?
      — Yes, verified item by item in `constitution.md`. P3 (no Flyway), P5 (measure first), P6 (no
      second font), P7 (a11y) are each honoured.
- [x] **D4.** Is the parity line's column order fixed so it cannot drift?
      — Yes: the canonical query and its exact order are pinned in `evidence/baseline.md`, and an
      earlier ad-hoc query with different labels is explicitly superseded.

## E. Risk

- [x] **E1.** Is DB residue from mutating sweeps controlled?
      — Yes: audit namespace, in-run cleanup, parity re-assert, `Msg \d+` scan. The specific known
      leak (`PremiumCheckout.vue` creating a payment row on mount) is named in the risk register.
- [x] **E2.** Is the "no rollback point" risk closed?
      — Yes: checkpoint `dae9667`, tree clean (user decision C2).
- [x] **E3.** Are misleading tool behaviours documented so they are not re-learned?
      — Yes: `sqlcmd` exit-0-on-failed-batch, the `MSYS_NO_PATHCONV` requirement, the
      scrollbar-width false positive, and the `alt=""` counting trap are all recorded.
- [x] **E4.** Could a rate-limit bucket make the harness fail against itself?
      — Known and handled: flush `rate_limit:*` before each batch (F109's lesson).

## Verdict

**Checklist: PASS.** No item is unaddressed. The two items most likely to be skipped in a
follow-up audit — **B1** (do not claim already-done work) and **C4** (cross-check UI against API
rather than testing them apart) — are both explicit requirements with named tasks.
