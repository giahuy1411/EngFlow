# audit-v13-full — specification

**Authored:** 2026-09-22 (+07) · **Branch:** `audit-streak-review` · **Checkpoint:** `3c1c525`
**Predecessor:** `audit-v12-full` · **Artifact home:** `.specify/specs/audit-v13-full/`
**Remote:** `github.com/giahuy1411/EngFlow`

## Why this audit exists

The user asked for a sweep that is **completely new relative to every previous audit — more comprehensive, deeper, end-to-end**: run the whole API, drive the whole UI against it, scrutinise six core features (Lessons/Exercises, Streak, Login/Register, Search/Sort, CRUD, AI), audit the DB inside Docker, optimise performance, test UI/UX with **both** chrome-devtools-mcp and playwright-mcp, verify the UI against the Playful Geometric design system with **Be Vietnam Pro**, use the skills/plugins in `C:\Users\ASUS\.claude`, find and fix holes in the prompt, follow the `constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze` workflow, run **two rounds** fixing defects at the root, and report honestly.

### Three things v13 must NOT pretend to discover (already true, measured this session)

| Fact | Evidence measured in this session |
|---|---|
| **Be Vietnam Pro is already the single font** | `index.html:54`; `tailwind.config.js:76-80`; `design-system.css:86`; grep for Outfit / Plus Jakarta = **0 hits** |
| **Playful Geometric is already implemented** | `Home.vue` hero blob/sun/dashed connector/marquee; `PremiumPage.vue` scale(1.1)+star badge; `design-system.css` `--geo-*` tokens, `shadow-pop-*`, 2px borders, `prefers-reduced-motion` |
| **The prompt's contrast holes were already patched at v11/v12** | `design-system.css` `--geo-*-ink` / `--geo-*-strong` layer with measured ratios |

v13 re-measures with **its own probes**; agreement is recorded as *confirmation*, disagreement as a *finding*.

### The real gaps v13 closes

1. **A user-reported bug, reproduced with real data** — F-13-01: choosing "Trắc nghiệm" (multiple choice) rendered a fill-in-the-blank box. Real row `exercise_id=777434` (lesson 91920, options `["a","b","c","d"]`, created 2026-09-21 22:01). Live UI: badge "TRẮC NGHIỆM" with **0 option buttons and 2 text inputs**. Scale: 33,556 MC rows, only 742 with real option text (97.8% placeholder). **Fixed at both render and create paths, with regression tests.**
2. **Residual contrast defects** the earlier patches missed — 3 real WCAG AA failures (F-13-03/04/05), plus 1 suspected site **measured as already correct** and deliberately left alone (sidebar amber = 8.76:1).
3. **`sweep/v13/` was a mislabelled copy of v12** — would have written v13 coverage into v12's evidence file (F-13-06).
4. **A lesson-builder dead end** — admin can author `QUESTION` blocks (3 in the real DB) but no learner view ever renders them (F-13-02).
5. **E2E at 3 tiers (UI → API → DB)** for the core features — v12 stopped at UI↔API.

## Objective

1. Re-establish the baseline with this session's own measurements.
2. Sweep the **whole API** (132 annotations → 148 rows / 26 controllers) with correct roles, real bodies, **response-contract** checks, and authorisation in **both** directions.
3. **E2E 3 tiers** for the six core features: real UI action → captured network call → API contract → **the DB row that changed**.
4. Audit the DB in Docker **as data**, including constraint **effectiveness** (a violating insert must actually be blocked, in a scratch DB).
5. Performance **before/after**, optimising only where a number justifies it (constitution P5).
6. Verify the design system and **find the holes in the prompt itself**.
7. **Fix every measured defect at the root, with a regression test.**
8. **Round 2, loop-until-dry**: stop when two consecutive rounds add no new defect.
9. Report honestly: done / not done / fixed & how / skills used / limits.

## Requirements

- **R1 — every number is this session's measurement.** Old audits are hypotheses to falsify, not sources. Agreement = confirmation; difference = finding.
- **R2 — the API surface is *run*, not listed.** Every inventory entry has a status; a 2xx is not enough — field names and types are checked, and role violations are tested in both directions.
- **R3 — UI is *cross-checked against* the API.** Each core feature: drive the real UI, capture the network call it emits, compare with the API contract, and confirm the DB row changed.
- **R4 — the DB is audited as data.** Parity re-measured; constraints checked for effectiveness; orphans scanned with NULL FKs counted separately.
- **R5 — the design system is verified, and the prompt's own holes are found and fixed.** A prompt claim that violates a11y is a defect with a measured ratio.
- **R6 — round 2 is broader than round 1, and repeats until dry** (two consecutive rounds with no new defect).
- **R7 — evidence discipline, no silent caps.** Every claim points to an artifact; unverified = BLOCKED/PARTIAL with a reason; a probe error is also a defect to record.
- **R8 — real data only.** No fabricated/mock data to prove a result. Mocks are allowed only inside unit tests, and must be labelled as such.
- **R9 — every AI-authored fix and every new UI↔API↔DB link gets cross-reviewed** (adversarial verification) before a finding is closed.

## Out of scope (stated so it is not silently skipped)

- Real money movement through SePay (only create-order + a wrong-signature webhook; boundary recorded).
- Deleting the lesson-builder feature (needs an owner decision; recorded as F-13-02, not actioned).
- Migrating the DB timezone convention (no second consumer; would be a large risky change).
