# Analyze — Cross-Artifact Consistency

**Feature:** audit-v8-full · **Dates:** 2026-09-16 (analyze 1), 2026-09-16 (analyze 2)
**Artifacts analyzed:** `constitution.md` · `spec.md` · `clarify.md` · `checklist.md` ·
`plan.md` · `tasks.md` · `findings.json` · `REPORT.md` · `route-map.md` ·
`db-audit.md` · `performance.md` · `design-audit.md` · `security-audit.md` · `evidence/`

This file records **both** analyze passes required by the workflow
(`… → analyze → implement → converge → analyze 2 → taskstoissues → report`).
Analyze is non-destructive: it finds inconsistency between artifacts and either
fixes the artifact or records why the inconsistency is intentional. It does not
change application code.

---

# ANALYZE 1 — before convergence

## A1. Findings ↔ evidence

Every finding in `findings.json` must have an ID, severity, reproduction, root
cause, affected files, fix, regression test and status.

| ID | Severity | Status | Regression test present | Runtime evidence |
|---|---|---|---|---|
| F90 | INFORMATIONAL | CLOSED-NOT-A-BUG | n/a (not a defect) | `p9_f90_scope.txt`, `p10_f90_falsify.txt` |
| F91 | MEDIUM | FIXED-VERIFIED | `RateLimitFilterTest` (+6) | `p8-bucket-coverage.txt` |
| F92 | MEDIUM | FIXED-VERIFIED | `RateLimitFilterTest` (+6) | `p8-bucket-coverage.txt` |
| F93 | MEDIUM | FIXED-VERIFIED (bounded) | `GlobalExceptionHandlerProblemDetailTest` (+3) | `p12_f93_live.txt` |
| F94 | LOW | FIXED-VERIFIED | `api.test.js` (+4) | `p13_error_contract.txt` |
| GAP-COVERAGE-16 | MEDIUM | FIXED-VERIFIED | n/a (coverage) | `coverage-round1.json` |
| CLAIM-COUNT | LOW | FIXED-VERIFIED | n/a (reporting) | `endpoint-inventory.json` |
| SWEEP-LEAK | LOW | FIXED-VERIFIED | n/a (harness) | `clean_r2.sql` output |
| PLANB-VERIFY | INFORMATIONAL | VERIFIED-PASS | `design-v2.js` | `design-v2.txt` |
| BASELINE-DRIFT | INFORMATIONAL | FIXED-VERIFIED | both suites | `t-final-backend.log`, `t-final-frontend.log` |

**Result: consistent.** 10/10 findings carry an ID, severity and status; all
five code-level findings (F91–F94) carry a named regression test, and all ten
cite a runtime artifact.

## A2. Findings ↔ status vocabulary

`findings.json` uses a fixed lifecycle vocabulary. Checked for misuse:

| Status | Meaning | Misuse found |
|---|---|---|
| `FIXED-VERIFIED` | Code changed **and** re-measured after the change | none |
| `CLOSED-NOT-A-BUG` | Investigated, no defect; explicitly **not** "fixed" | none |
| `VERIFIED-PASS` | Verification only, nothing to change | none |

**F90 is the one to scrutinise**, because a reader could mistake
`CLOSED-NOT-A-BUG` for "we skipped it". It is not skipped: 17 `permitAll`
surfaces were measured individually (`p10`) and 0 reached the app unthrottled,
so there is no bypass to fix. The status is deliberate and the reasoning is
recorded in `security-audit.md` §6. **Intentional, not an inconsistency.**

## A3. Evidence layout ↔ plan requirement

The plan's "Required Evidence Layout" lists 14 documents. Reconciliation:

| Required | Present | Note |
|---|---|---|
| `constitution.md` or diff reference | ✅ | written this round; records 0 amendments + 1 deferred PATCH |
| `spec.md` | ✅ | |
| `clarify.md` | ✅ | |
| `checklist.md` | ✅ | A1–A11 / B1–B7 / C1–C6 / D1–D5 / E1–E5 / F1–F3 |
| `plan.md` | ✅ | |
| `tasks.md` | ✅ | audit-v8 rounds T01–T41 |
| `analyze.md` | ✅ | **this file** (was missing) |
| `converge.md` | ✅ | written this round (was missing) |
| `endpoint-map.md` | ✅ | |
| `route-map.md` | ✅ | **written this round** (was missing) |
| `db-audit.md` | ✅ | **written this round** (was missing) |
| `performance.md` | ✅ | **written this round** (was missing) |
| `design-audit.md` | ✅ | **written this round** (was missing) |
| `security-audit.md` | ✅ | **written this round** (was missing) |
| `REPORT.md` | ✅ | |
| `evidence/` | ✅ | + `route-inventory.json` added this round |
| `findings.json` | ✅ | |

**Gap found and closed.** Six required documents did not exist. That is the
single largest inconsistency this pass found: the plan demanded them, the
workflow ran through implement, but the evidence layout was incomplete. All six
are now written, each backed by a re-runnable harness rather than prose.

## A4. Numeric consistency across artifacts

Numbers drift between documents more often than logic does. Every headline
figure was traced to its source:

| Figure | Value | Traced to | Contradiction |
|---|---|---:|---|
| Backend tests | 378 | `t-final-backend.log` (`Tests run: 378, Failures: 0`) | none |
| Frontend tests | 89 / 18 files | `t-final-frontend.log` | none |
| Bundle entry | 176.80 kB / 67.39 gzip | `t-final-build.log` | none |
| Frontend routes | 39 | `route-inventory.json`, `route_verify.py` = CONSISTENT | none |
| Route visits | 152 | `routes-all.txt` | none |
| Design elements sampled | 7 685 | `design-v2.txt` | none |
| Route×viewport combos | 60 | `design-v2.txt` | none |
| `vocabulary` rows | 127 | `db-audit-out.txt` | none |
| `exercises` rows | 43 737 | `db-audit-out.txt` | none |
| `lessons` rows | 1 471 | `db-audit-out.txt` | none |
| Empty `correct_answer` | 4 848 | `db-audit-out.txt` | none |
| AI endpoints on `:ai` | 13 | `p8-bucket-coverage.txt` | none |

**Two discrepancies were found and resolved during this pass** — both were stale
numbers in prose, not measurement errors:

1. **Stale p8 evidence.** The committed `p8.json` showed all 16 rows as
   `status=404` with no bucket recorded, i.e. the evidence file predated the
   fix. Re-running `p8_bucket_coverage.js` produced the correct routing
   (`:ai` ×13, `:upload` ×2 + control). Recorded because "the evidence file
   exists" is not the same as "the evidence file shows the fix".
2. **`db-audit.md` did not exist**, so its figures (127/43 737/1 471/4 848) had
   no document to live in even though the harness produced them. Now written.

## A5. Requirement coverage — the three source plans

Every requirement in the three input documents was mapped to a task or an
explicit disposition:

| Requirement | Where satisfied |
|---|---|
| Audit backend + DB + frontend + UI/UX + security + AI + performance | `REPORT.md` §1, plus the five audit documents |
| Be Vietnam Pro the only font | `design-audit.md` §1 (4 verification levels) |
| Viewports 360/768/1280/1440/1920 | `design-v2.js` (60 combos) + `routes-all.js` (1440/360) |
| No "hoàn thành"/"pass" without fresh evidence | §A6 below |
| Findings need ID/severity/repro/root cause/files/fix/test/status | `findings.json` (A1) |
| Report tables: checked / fixed / not fixed / blocked / n/a / deferred / skills / command-evidence-exit | `REPORT.md` §1–§5 |
| Backup policy before DML | No bulk DML ran; cleanup was row-scoped. `db-audit.sql` is read-only |
| `SET QUOTED_IDENTIFIER ON` for DELETE batches | `clean_r2.sql`, `clean-xss-probe.sql` |
| Test policy / expected results / acceptance criteria | `checklist.md`, `spec.md` |
| Re-run `mvnw.cmd test`, `npx vitest run`, `npx vite build`, `git diff --check`, `git status --short` | `REPORT.md` §5; run fresh in analyze 2 |

**No unmapped requirement.** The one requirement needing a judgement call is
"no claim without fresh evidence", handled explicitly next.

## A6. Freshness audit — the claim the prompt cares most about

The audit prompt forbids writing "hoàn thành", "pass" or "đã tối ưu" without
evidence from the **last** run. Every headline claim was checked against the
timestamp and command that produced it:

| Claim | Fresh this round? | Evidence |
|---|---|---|
| Backend 378/378 | ✅ | `t-final-backend.log`, run this round |
| Frontend 89/89 | ✅ | `t-final-frontend.log`, run this round |
| Bundle 176.80 kB | ✅ | `t-final-build.log`, run this round |
| 152 route visits, 0 defects | ✅ | `routes-all.txt`, run this round |
| 0/7 685 non-BVP fonts | ✅ | `design-v2.txt`, run this round |
| 0 a11y violations | ✅ | `design.txt`, run this round |
| Security 0 failures | ✅ | `security.txt`, run this round |
| XSS non-executable | ✅ | `xss-prove.txt`, run this round |
| Perf N=7 measurements | ✅ | `perf.txt`, run this round |
| DB parity 127/43 737/1 471 | ✅ | `db-audit-out.txt` + cleanup output, this round |
| Round-2 matrix (p9–p13) | ✅ | re-run this round, all exit 0 |
| F87 shape holds (4 600 reads) | ✅ | `perf-reads-out.txt`, this round |
| F93 live 504 response | ❌ **not fresh** | documented as a boundary (see A7) |

## A7. Deliberate boundaries — stated, not hidden

Three claims are **not** fully verified, and each is recorded as such rather
than rounded up to "pass":

1. **F93's live 504.** Unit tests prove the handler maps a timeout to `504`, and
   the running container is confirmed to contain the handler
   (`p12`: 1 hit for `Gateway Timeout`). Forcing a *live* 504 requires making
   local Ollama exceed 30 s, which is GPU-load-dependent and non-deterministic.
   Recorded as `verificationBoundary` in `findings.json`. **Not claimed as
   end-to-end verified.**
2. **F87's before/after.** The "before" (95 k reads / 367 ms) is a documented
   historical measurement from the F87 round; re-measuring it would mean
   reverting the fix and rebuilding. What is fresh is that the current code
   holds the intended shape. `performance.md` §2 states this explicitly.
3. **`noShadow=46` / `thinBorder=5`.** Reported as informational because the
   design system applies hard shadows and 2 px borders to containers, not to
   every descendant node. Not a defect, and not silently dropped either.

## A8. Checklist ↔ findings consistency

`checklist.md` uses A/B/C/D/E/F groups. Cross-checked that no checklist item
contradicts a finding status, and that no finding is missing from the checklist:

- F90–F94 all appear; F90's item is marked as "closed, not a bug" matching
  `findings.json`.
- The a11y items match `design-audit.md` §4 (0 AA violations, 138 AAA
  recommendations).
- The `alt=""` item encodes the corrected metric (`hasAttribute`, not
  `getAttribute`), matching the harness.

**Consistent.** No item claims a pass that the evidence contradicts.

## Analyze 1 — verdict

| Dimension | Result |
|---|---|
| Findings complete (ID/severity/repro/fix/test/status) | ✅ 10/10 |
| Evidence layout complete | ✅ after writing 6 missing documents |
| Numbers traceable to a source | ✅ 12/12 |
| Requirements mapped | ✅ all |
| Fresh-evidence rule respected | ✅ with 3 stated boundaries |
| Contradictions requiring a code change | **0** |

**Inconsistencies found: 6 missing documents + 1 stale evidence file + 1
undocumented figure set. All were artifact defects, none was an application
defect.** No code change was required, which is the expected outcome of analyze.

---

# ANALYZE 2 — after convergence

Run after `converge.md` to confirm convergence did not break artifact
consistency, and to re-verify on the **final** build.

## B1. Post-convergence re-verification

Every suite was re-run on the frozen tree after all writes stopped:

| Command | Result | Evidence |
|---|---|---|
| `cmd /c "mvnw.cmd test"` | **378/378**, `BUILD SUCCESS` | `t-final-backend.log` |
| `npx vitest run` | **89/89 in 18 files** | `t-final-frontend.log` |
| `npx vite build` | **176.80 kB / gzip 67.39**, built in 9.51s | `t-final-build.log` |
| `p9`–`p13` round-2 matrix | all exit **0** | `p9_*.txt` … `p13_*.txt` |
| `security.js` | **0 failures** | `security.txt` |
| `xss-prove.js` | **PASS** (3/3 checks) | `xss-prove.txt` |
| `routes-all.js` | 228 visits (38 × 2 viewports × 3 roles), **0** errors / 0 4xx / 0 overflow / 0 wrong landing | `routes-all.txt` |
| `design-v2.js` | 0/60 overflow, 0/7 685 non-BVP | `design-v2.txt` |
| `db-audit.sql` | 0 SQL errors, parity exact | `db-audit-out.txt` |

## B2. Convergence did not invalidate any earlier finding

| Finding | Still holds after convergence? | Check |
|---|---|---|
| F91/F92 bucket routing | ✅ | `p8-bucket-coverage.txt` re-run: `:ai` ×13, `:upload` ×2 |
| F93 timeout mapping | ✅ | `p12`: handler in container, success path 200/1021 ms, unauth 401 |
| F94 error normalization | ✅ | `p13`: 3/10 still `LEGACY{error}` — the documented boundary, unchanged |
| F90 closed-not-a-bug | ✅ | `p10`: 0 permitAll surfaces without a bucket |
| PLANB-VERIFY | ✅ | `design-v2`: 0 non-BVP, 15 faces, weights 400/700/900 loaded |
| BASELINE-DRIFT | ✅ | 378/89 match the corrected baseline exactly |

## B3. Working tree ↔ tested tree

The final claim is only meaningful if what was tested is what is on disk.

| Check | Result |
|---|---|
| Source tree tested == source tree on disk | ✅ **yes** — no file under `src/` or `frontend/src/` was modified after the final suite run. The suite was re-run **after** the last code change (F93 handler + F91/F92 bucket routing), and no code change followed it. |
| `git diff --check` | **clean** — no whitespace errors |
| Untracked files under `src/` or `frontend/src/` | **0** |
| Files modified *after* the final suite run | **only audit artifacts** — `REPORT.md`, `findings.json`, `tasks.md`, `workflow-log.md`, `analyze.md`, `converge.md`, `prompt-*.md`, and `sweep/v8` evidence. **No production code.** |

> **Correction.** An earlier revision of this table read `git diff HEAD` =
> **empty — committed state == tested state**, and `git status --short` = "no
> modified tracked files". Both were true when written and are **no longer true**:
> the artifact-authoring pass afterwards edited tracked audit documents, so the
> tracked diff is non-empty by design (artifacts changed, code did not). The
> claim that actually matters — *the tested code is the code on disk* — still
> holds and is what the first row now states. Keeping the stronger, now-false
> wording would have been an unsupported claim.

## B4. Residual inconsistencies

| Item | Assessment |
|---|---|
| `tasks.md` stopped at T41 while `REPORT.md` reported 378 tests and 15 findings | **Fixed** — T42–T62 appended; each entry cites its harness and measured result. `converge.md` §4.1 records the reversed decision. |
| An earlier `report`/`converge` claim that `tasks.md` needs no retro-fill | **Withdrawn** — recording completed, evidenced work is provenance, not fabrication. Two artifacts must not contradict each other. |
| `tasks.md` covers audit-v8 T01–T41 but not the audit-v8-full F90–F94 round | **Known and intentional.** The F90–F94 round is recorded in `REPORT.md` §2b and `findings.json`, which are the authoritative records for it. Rewriting `tasks.md` retroactively would fabricate a task plan that did not exist at the time. Recorded as a documented gap, not silently patched. |
| `constitution.md` P1 still cites 221/73 from audit-v3 | **Deferred, by design.** P1 explicitly obliges each audit to re-measure and record in the report, which is done. Amending the constitution is a governance act under Article III. Exact diff proposed in `constitution.md` §3. |
| 8 pre-existing audit users, 6 empty-content lessons, 4 `_bak_v5` tables, ~9 zero-seek indexes | **Deferred with reasons** in `db-audit.md` §9/§7/§1/§5. Each is outside this run's cleanup scope or unjustified by measurement. |

## B5. Consistency of the three input plans

The three source documents were cross-checked against each other for
contradictory instructions:

| Question | PLAN.md | audit prompt | redesign plan | Resolution |
|---|---|---|---|---|
| Full workflow required? | yes | implied | yes (Global Constraints) | followed the longest chain |
| Font rule | BVP only | BVP only | BVP only | **agree** |
| Evidence without fresh runs? | forbidden | forbidden | forbidden | **agree** |
| Backup before DML? | required | — | required | followed the stricter rule; no bulk DML ran |
| Viewports | — | 360/768/1280/1440/1920 | — | followed the audit prompt |
| Report tables | required set | required set | — | used the union |

**No contradictions.** Where the plans differed in strictness, the stricter
requirement was applied.

## Analyze 2 — verdict

| Dimension | Result |
|---|---|
| Suites green on final build | ✅ 378 / 89 / build OK |
| Findings still hold after convergence | ✅ 6/6 re-verified |
| Tested **code** == code on disk | ✅ no `src/` or `frontend/src/` file changed after the final suite run |
| Residual inconsistencies | 3, all documented with reasons |
| Blocking issues | **0** |

**The artifact set is internally consistent and every claim is backed by
evidence produced on the final build.**

---

## Analyze 3 — post-artifact consistency pass

The artifact-authoring pass (the 8 new documents, `tasks.md` T42–T62,
`findings.json` 10 → 15, `prompt-rewritten.md`) ran **after** analyze 2, so its
output was re-analyzed rather than assumed consistent. This pass found and fixed
**5 self-inflicted inconsistencies** — all of them the same class of error the
audit exists to catch: **a true statement that stopped being true.**

| # | Inconsistency | Why it arose | Fix |
|---|---|---|---|
| 1 | `converge.md` said "**0 tasks appended**" while T42–T62 had been added | Two passes, opposite decisions, neither aware of the other | §4.1 records the reversed decision and **why the earlier reasoning was too broad**; verdict table now splits "by convergence (0)" from "provenance later (21)" |
| 2 | `converge.md` + `analyze.md` claimed `git diff HEAD` = **empty** | True when written; the artifact pass then edited tracked files | Both replaced with the check that is still meaningful (`git diff --check`, and "no source file changed after the final run"), with an explicit note that the strong claim is no longer true |
| 3 | `analyze.md` claimed `git status --short` = "no modified tracked files" | Same cause | Corrected to "only audit artifacts modified, no production code" |
| 4 | `REPORT.md` headline claimed "**17 lỗi thật**" (gộp cả F90) | F90 is a *rejected hypothesis*, not a defect | Replaced with a 5-row table separating 16 real defects, 1 rejected hypothesis, 5 audit-tooling defects, 1 verification pass — each count re-checked against `findings.json` |
| 5 | `workflow-log.md` scope section still said "149 mapping / 27 controller / 32 route" | Hand-typed figures with no generator | Replaced with script-generated **144 / 26 / 39**; the note records that the hand-typed numbers were **not reproducible** |

**Analyze 3 changed no code and reversed no finding.** Its value is that it
demonstrates the audit's own artifacts are subject to the same evidence standard
as its code — including when the claim being corrected is one the audit itself
made earlier.

---

## Analyze 4 — round 3 (screenshots + backup), and what it says about assertions

Round 3 added Task 5 (36 screenshots), the backup restore point, and
`HARNESS-TOKEN-ONLY-SEED`. It also produced **4 more self-inflicted errors**, and
unlike analyze 3's list these were not stale claims — they were **assertions that
were wrong the moment they were written**. The distinction matters: a stale claim
decays, a wrong assertion never was true.

| # | Error | Why it arose | Fix |
|---|---|---|---|
| 1 | `routes-all.js` reported **24 "wrong landing"** | The assertion was one-directional: it required every visit to end at the requested URL. But `/login` is `guestOnly` (a logged-in visitor is *correctly* bounced) and the catch-all *always* redirects | Encoded the full guard × role contract, asserting **both** directions: privileged **stays**, under-privileged is **bounced** → 0 false alarms |
| 2 | `design-v2.js` reported **5 "wrong landing"** | Same bug, smaller surface: the same `/login` row, once per viewport | Same fix; `LANDED ON WRONG PAGE: 0`, exit 0 |
| 3 | `cleanupAuditPayments()` reported **`after=2`** while the table held 126 | It parsed "the last number in the output", which is from `(1 rows affected)`, not the count | Emit an unambiguous marker `AUDIT_CLEAN_TOTAL=<n>` and regex on that; also scan for `Msg \d+` because `sqlcmd` exits 0 on batch failure |
| 4 | `REPORT.md` was about to claim **"the entire 152-visit sweep was invalidated"** | Extrapolated from the fresh-context harnesses to all of them without measuring | Ran `p20` (A/B of old vs new seed in walk order): **0/9 bounced either way**, identical text ⇒ claim narrowed to the 2 harnesses actually affected |

**Error 4 is the one worth keeping.** Errors 1–3 are bugs in code the audit
wrote; error 4 is a bug in *reasoning* — the audit nearly published an inflated
impact claim in exactly the direction that makes an audit look more valuable. The
fix was not a code change but a measurement, and the measurement cut the claim
from "the whole sweep" to "9 of 36 screenshots."

The same discipline applied to `F93`'s evidence list: one entry read
`sweep/v8/p3a-r1.log (FAIL gốc)`, i.e. **an annotation inside the path string**,
so it could not be opened even though the file existed. Split into
`evidenceNotes[path]` ⇒ **72/72 evidence refs now resolve.** A path that cannot be
opened breaks the trace chain even when the conclusion is right.

**Analyze 4 changed no code and reversed no finding.** Its value is the same as
analyze 3's, one level deeper: the audit's *reasoning* is held to the evidence
standard, not just its artifacts.

---

# Summary

| Pass | Inconsistencies found | Code changes required |
|---|---|---|
| Analyze 1 | 6 missing documents, 1 stale evidence file, 1 undocumented figure set | **0** |
| Analyze 2 | 0 new; 3 residuals documented with reasons | **0** |
| Analyze 3 | **5 self-inflicted** (stale claims in `converge`/`analyze`/`REPORT`/`workflow-log`) | **0** |
| Analyze 4 | **4 self-inflicted** (2 wrong assertions, 1 wrong parser, 1 inflated impact claim) + 1 unopenable evidence path | **0** |

All four passes confirmed the same thing: the audit's weaknesses were in its
**artifacts** and its **reasoning**, not its code. That is why the fix for analyze
1 was writing six documents backed by re-runnable harnesses, why analyze 2's
residual list contains only items deliberately left alone, why analyze 3's list
consists entirely of **this audit's own earlier statements that had expired**, and
why analyze 4's consists of **assertions that were never true** — the two ways a
confident claim fails. The whole exercise exists to eliminate both.
