# audit-v11-full — implementation plan

**Spec:** `spec.md` · **Tasks:** `tasks.md` · **Checklist:** `checklist.md` · **Clarify:** `clarify.md`
**Checkpoint:** `dae9667` · **Branch:** `audit-streak-review`

## Approach in one paragraph

v11 is a **from-scratch re-measurement**, not a re-read of v10. It runs in seven phases, each
producing evidence files under `evidence/` and numbers that are this session's own. Prior audits
are used only to aim probes at claims worth falsifying (F115/F126/F127 fixes, the parity line, the
font URL). The two tools v10 lacked — **chrome-devtools MCP** and **Playwright MCP** — carry the UI
work, which is the genuine capability upgrade of this round. The only code change proposed before
the second pass is the contrast fix (H1), and it is gated on measurement: the failing usages and
their ratios are captured first, the token fix is applied second, and the same probe re-run third
proves the fix. Nothing else in the codebase is rewritten to match a prompt that is wrong about the
product.

## Phase 0 — freeze and baseline (no writes)

| Step | Command / action | Artifact |
|---|---|---|
| T0.1 | Confirm the checkpoint commit and a clean tree | `git log`, `git status` |
| T0.2 | Confirm the DB backup exists and `RESTORE VERIFYONLY` passes | `evidence/backup-phase0.md` |
| T0.3 | Backend suite, count **from the run log** | `.p0-v11-backend-test.log` |
| T0.4 | Frontend suite + production build (entry size) | `.p0-v11-frontend-test.log`, build log |
| T0.5 | Container inventory + image/uptime, live service probes | `evidence/deployed-state.md` |
| T0.6 | Live parity line, measured by this session | `evidence/baseline.md` |
| T0.7 | Re-derive the endpoint inventory **from source** | `evidence/endpoint-inventory.md` |

Gate: no finding is closed and no code is changed until T0.3–T0.4 report their real counts.

## Phase 1 — database audit in Docker (read-only)

| Step | Action | Artifact |
|---|---|---|
| T1.1 | Schema constraints: FK, unique, filtered indexes — existence **and effect** | `evidence/db-audit.md` |
| T1.2 | Integrity: orphan scan written so NULL FKs are not miscounted | `evidence/db-audit.md` |
| T1.3 | Streak schema: `study_policy`, `study_days`, `effective_from` | `evidence/db-audit.md` |
| T1.4 | Row-level sanity on the tables the UI reads (lessons, exercises, decks, users) | `evidence/db-audit.md` |
| T1.5 | Fragmentation / index usage on the hot read paths | `evidence/db-audit.md` |

Rule: every batch output is scanned for `Msg \d+`. No DML in this phase.

## Phase 2 — API sweep over the live container

| Step | Action | Artifact |
|---|---|---|
| T2.1 | Auth: login/register/refresh/me, wrong password, lockout, JWT expiry, role in both directions | `evidence/api-auth.md` |
| T2.2 | Lessons/Exercises: list, detail, structure, content, submit, grade — incl. the F115/F126 draft guards | `evidence/api-lessons.md` |
| T2.3 | Streak: `/api/streak/snapshot` contract (all 7 fields), study-day semantics | `evidence/api-streak.md` |
| T2.4 | Search/sort: `/api/vocabulary/search`, dictionary, admin exercise search; confirm `?sort=` behaviour | `evidence/api-search.md` |
| T2.5 | CRUD: create/read/update/delete through the **API** for a lesson and a deck, with cleanup | `evidence/api-crud.md` |
| T2.6 | AI: exercise generation contract, TTS path, Whisper sidecar health | `evidence/api-ai.md` |
| T2.7 | Payment: create-order contract; **clean up every row created, in the same run** | `evidence/api-payment.md` |
| T2.8 | Re-assert parity | `evidence/api-payment.md` |

Bucket discipline: the harness flushes `rate_limit:*` before each batch, or it would prove 429s
against itself (F109).

## Phase 3 — UI sweep with chrome-devtools MCP + Playwright MCP

This is the phase v10 could not run as specified. Two independent browser drivers are used, and
their results are compared rather than merged.

| Step | Action | Artifact |
|---|---|---|
| T3.1 | Route sweep: every route × 3 roles, asserting the landing guard **in both directions** | `evidence/ui-routes.md` |
| T3.2 | Console/network health: 0 console errors, 0 page errors, 0 UI-caused API ≥ 400 | `evidence/ui-routes.md` |
| T3.3 | Responsive: 360 / 768 / 1280 / 1440 / 1920, overflow measured as raw numbers | `evidence/ui-responsive.md` |
| T3.4 | **UI↔API cross-check**: capture the network calls each core flow emits and compare to the API contract | `evidence/ui-api-parity.md` |
| T3.5 | A11y: `alt` via `hasAttribute`, tap targets at both thresholds, focus-visible, skip-link | `evidence/ui-a11y.md` |
| T3.6 | **Contrast AA across real pages** — the probe no prior audit ran | `evidence/ui-contrast.md` |
| T3.7 | Design-system conformance vs the prompt, including the H1–H8 holes | `evidence/ui-design-conformance.md` |
| T3.8 | Cleanup any payment rows the sweep created + re-assert parity | `evidence/ui-routes.md` |

## Phase 4 — CRUD through the admin UI (v10's open T4.10)

| Step | Action | Artifact |
|---|---|---|
| T4.1 | Create a lesson through the admin UI, verify it appears and the API agrees | `evidence/ui-crud.md` |
| T4.2 | Edit it, verify the change persisted | `evidence/ui-crud.md` |
| T4.3 | Delete it, verify removal and that nothing else was touched | `evidence/ui-crud.md` |
| T4.4 | Repeat for a deck | `evidence/ui-crud.md` |
| T4.5 | Re-assert parity — CRUD must leave no residue | `evidence/ui-crud.md` |

## Phase 5 — fix the real defects found (gated on measurement)

Only defects with a measured failure are fixed. For each: capture failing evidence → fix →
re-run the same probe → record both numbers.

| Step | Action |
|---|---|
| T5.1 | Contrast H1: fix the token/usage so accent text meets AA, then re-measure |
| T5.2 | Any F-numbered defect Phase 1–4 produces, with a regression test |
| T5.3 | Backend suite + frontend suite + build re-run; counts from the run log |

## Phase 6 — performance, measured before and after

| Step | Action | Artifact |
|---|---|---|
| T6.1 | Before: hot endpoints (lesson list, exercise list, search, snapshot) — median of ≥3 | `evidence/performance.md` |
| T6.2 | Before: bundle size + Lighthouse/CWV where the tooling allows | `evidence/performance.md` |
| T6.3 | Optimise only what the numbers justify; record the reason for declining the rest | `evidence/performance.md` |
| T6.4 | After: same measurements, same method | `evidence/performance.md` |

## Phase 7 — second, wider pass

| Step | Action |
|---|---|
| T7.1 | Full backend + frontend suite on the final build |
| T7.2 | Repeat the browser sweep; compare to Phase 3 |
| T7.3 | Adversarial boundary cases: streak edge conditions, SRS interval caps, cutover day |
| T7.4 | Cross-artifact consistency: spec ↔ plan ↔ tasks ↔ findings ↔ evidence |
| T7.5 | Truthful report: done / not done / fixed / how / skills loaded |

## Risk register

| Risk | Mitigation |
|---|---|
| Sweeps mutate the DB and leave residue | Audit namespace, cleanup in the same run, parity re-asserted, `Msg \d+` scanned |
| A probe reports a defect the app does not have | Every defect re-verified by a second independent probe before it is called a finding |
| MCP browser drives a different engine than the harness | Record which driver produced which number; never mix sources in one comparison |
| `sqlcmd` silently succeeds on a failed batch | Scan for `Msg \d+`; never trust exit code alone |
| A "fixed" claim with no re-measurement | Fix is closed only by re-running the probe that failed, with both numbers recorded |
| Rate-limit bucket exhaustion fakes 429s | Flush `rate_limit:*` before each batch |
| Token/secret leakage into evidence | Scan evidence for secret patterns before committing |
