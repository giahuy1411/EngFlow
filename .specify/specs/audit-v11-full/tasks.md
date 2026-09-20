# Tasks — audit-v11-full

Execution order. `[x]` means **verified with an artifact**; `[~]` means the code exists but is
unverified; `[ ]` means not started. Nothing moves to `[x]` on the strength of a neighbouring task.

## Phase 0 — freeze and baseline

- [x] T0.1 Commit the checkpoint and confirm a clean tree — **`dae9667`**, 337 files, 0 dirty
- [x] T0.2 Confirm the DB backup exists and verify it — `engflow_2026-09-20-pre-backlog-dml.bak`, `RESTORE VERIFYONLY` valid
- [x] T0.3 Backend suite, count from the run log — **483 run / 0 fail / 0 error / 11 skipped, BUILD SUCCESS**
- [x] T0.4 Frontend suite + build — **109 passed / 1 skipped (22 files)**; entry **177.31 kB**
- [x] T0.5 Container inventory + live service probes — 8 containers up, SPA 200, MinIO 200, login 200
- [x] T0.6 Live parity line measured — **`1471|43735|72|127|28|15|4|126|14|5`**, matches v10 exactly
- [x] T0.7 Endpoint inventory re-derived from source — **131 annotations / 26 controllers**
- [x] T0.8 Streak schema confirmed live — policy 1 row, `effective_from=2026-09-20`, FK 1, UQ 1
- [x] T0.9 Record drifts vs v10 — backend **+13**, frontend **+3**, bundle **0**

## Phase 1 — database audit in Docker (read-only)

- [ ] T1.1 Constraint **effect**: prove the FK and UQ actually reject violating inserts (scratch DB, dropped after)
- [ ] T1.2 Orphan scan written so NULL FKs are **not** miscounted
- [ ] T1.3 Streak tables: policy, days, cutover semantics
- [ ] T1.4 Row-level sanity on the tables the UI reads
- [ ] T1.5 Index usage / fragmentation on the hot read paths
- [ ] T1.6 Scan every batch output for `Msg \d+`

## Phase 2 — API sweep over the live container

- [ ] T2.1 Auth: login/register/refresh/me, wrong password, lockout, expiry, role both directions
- [ ] T2.2 Lessons/Exercises incl. reproducing the F115/F126 draft guards
- [ ] T2.3 Streak `/api/streak/snapshot` — all 7 documented fields, study-day semantics
- [ ] T2.4 Search/sort — vocabulary search, dictionary, admin exercise search; confirm `?sort=` behaviour
- [ ] T2.5 CRUD through the API, with in-run cleanup
- [ ] T2.6 AI contracts + TTS path + Whisper health
- [ ] T2.7 Payment create-order, **cleanup in the same run**
- [ ] T2.8 Re-assert parity

## Phase 3 — UI sweep with chrome-devtools MCP + Playwright MCP

- [ ] T3.1 Route sweep, every route × 3 roles, guards asserted **both ways**
- [ ] T3.2 Console/network health — 0 console errors, 0 page errors, 0 UI-caused API ≥ 400
- [ ] T3.3 Responsive 360/768/1280/1440/1920, overflow as raw numbers
- [ ] T3.4 **UI↔API cross-check** — capture emitted network calls per core flow, compare to contract
- [ ] T3.5 A11y — `alt` via `hasAttribute`, tap targets both thresholds, focus, skip-link
- [ ] T3.6 **Contrast AA on real pages** — the probe no prior audit ran
- [ ] T3.7 Design-system conformance vs the prompt, incl. holes H1–H8
- [ ] T3.8 Cleanup payment rows the sweep created + re-assert parity

## Phase 4 — CRUD through the admin UI (v10's open T4.10)

- [ ] T4.1 Create a lesson through the admin UI; verify in UI and via API
- [ ] T4.2 Edit it; verify persistence
- [ ] T4.3 Delete it; verify removal and that nothing else changed
- [ ] T4.4 Repeat for a deck
- [ ] T4.5 Re-assert parity — CRUD leaves no residue

## Phase 5 — fix measured defects

- [ ] T5.1 Contrast H1: capture failing evidence → fix → re-measure → record both numbers
- [ ] T5.2 Any further F-numbered defect, each with a regression test
- [ ] T5.3 Re-run backend + frontend + build; counts from the run log

## Phase 6 — performance, before and after

- [ ] T6.1 Before: hot endpoints, median of ≥3
- [ ] T6.2 Before: bundle size, and CWV/Lighthouse where tooling allows
- [ ] T6.3 Optimise only what the numbers justify; record declines with reasons
- [ ] T6.4 After: same measurements, same method

## Phase 7 — second, wider pass

- [ ] T7.1 Full suites on the final build
- [ ] T7.2 Repeat the browser sweep; compare to Phase 3
- [ ] T7.3 Adversarial boundaries: streak edges, SRS caps, cutover day
- [ ] T7.4 Cross-artifact consistency: spec ↔ plan ↔ tasks ↔ findings ↔ evidence
- [ ] T7.5 Write the report: done / not done / fixed / how / skills loaded

## Phase 8 — close-out

- [ ] T8.1 `speckit-analyze` — cross-artifact consistency, read-only
- [ ] T8.2 `speckit-converge` — append any unbuilt work, then implement it
- [ ] T8.3 Scan all evidence for secret patterns before committing
- [ ] T8.4 Commit the audit artifacts

## Skills to load (per the user's instruction to use `C:\Users\ASUS\.claude`)

| Skill / plugin | Why it applies |
|---|---|
| `speckit-*` | The mandated pipeline: constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze |
| `superpowers:using-superpowers` | Governing rule: invoke relevant skills before acting |
| `superpowers:brainstorming` | Classify the request and get approval before implementation |
| `superpowers:verification-before-completion` | Every completion claim needs a command's output behind it |
| `superpowers:systematic-debugging` | Root-cause each defect rather than patching the symptom |
| `superpowers:test-driven-development` | Each fix gets a failing test first |
| `chrome-devtools-mcp:*` | The mandated browser driver, incl. a11y and LCP debugging |
| `playwright` (MCP) | Second independent browser driver; route/flow interaction |
| `accessibility` | WCAG 2.2 audit — the contrast finding lives here |
| `addyosmani-performance-optimization` | Phase 6 method: measure, then optimise |
| `java-springboot`, `java-coding-standards` | Backend fix conventions |
| `frontend-design`, `design-system.css` | Design-system conformance work |
| `update-config` | If a permission allowlist is needed to run a blocked command |
