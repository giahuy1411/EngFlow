# REPORT-v5 — EngFlow audit-v9-full

> CURRENT STATUS (2026-09-18): audit NOT CLOSED. Sections 1-5 are historical
> 2026-09-17 observations, not verification of the current checkout/image.
> Source inventory coverage mixed historical probe files and did not validate
> successful assertions. The 144/144 claim is not a current coverage gate.
> F105/F106 have focused mock tests and deployed-class markers; markers alone
> do not prove runtime authorization or SQL persistence. Two mock test classes
> cannot replace DTO-wide contract testing; JAR inspection is not an independent
> reviewer. See reverify-status.md for the current evidence ledger.

**Date**: 2026-09-17 (naive VN +07) · **Predecessor**: `audit-v8-full` round 4 · **Spec dir**: `.specify/specs/audit-v9-full/`
**Scope**: full re-audit of the backend API surface, the live database, and the browser UI/UX, with root-cause fixes,
a measured performance pass, and verification of the Playful Geometric design system (Be Vietnam Pro) including the
removal of dead design-system CSS.

Every claim below points at the artifact that produced it. Anything not verified is marked `BLOCKED`, not omitted.

---

## 1. Headline result

| Gate | Result |
|---|---|
| Backend tests | **393/393 PASS**, BUILD SUCCESS (`sweep/v8/v9-r2-backend-full.log`) |
| Frontend tests | **90/90 PASS in 19 files** (`sweep/v8/v9-r1-frontend-tests.log`) |
| `vite build` | success; JS entry **176.80 kB** unchanged, CSS entry 92.42 → **85.22 kB** |
| Endpoint coverage | **144/144** mappings, 472 probe requests (`evidence/coverage-round2.json`) |
| API probe failures | **0** across p1–p5, p6, p8, p10–p15, p24 (round 2) |
| Browser sweep | 228 visits: 0 console errors, 0 UI-caused 4xx/5xx, 0 overflow, 0 wrong landings |
| Design | font set = {Be Vietnam Pro}; 0 token/drift/shadow/radius/border violations; `.geo-*` UNUSED = 0 |
| Accessibility | 0 images without `alt`, 0 tap targets < 24 px |
| DB parity | `1471|43737|76|127|28|15|4|126|14|5`, `PARITY_OK=True` after both rounds |
| Findings | 10 total: **8 FIXED**, **2 BLOCKED** (both INFO/ops, no app defect open) |

## 2. What was done

### 2.1 Findings found and fixed (with evidence)

| ID | Severity | What was wrong | Fix | Proof |
|---|---|---|---|---|
| **F105** | HIGH | An unpublished lesson (`isPublished=false`) still answered `grade`/`submit`, so a student could send a random answer and read the real `correctAnswer` out of the response — the same leak F88/F89 had closed on the read endpoints. | Added the shared `assertLessonVisible` guard to `LessonExerciseController.grade`/`submit` (admin preview still allowed). | `v9-f105-verify.log` 6/6 (student 404 on draft, admin 200), `AuditV9DraftLessonGradeGuardTest` 5 tests |
| **F106** | HIGH | `POST /api/srs/review` returned 500 **permanently** once a word's SM-2 interval grew large: `interval × easeFactor` is unbounded, `now().plusDays(interval)` left the `datetime2` range, and the row was never repaired. Live row `user_vocabulary_progress.id=20002` held `srs_interval=1,537,216` (≈4,210 years) from ~16 perfect reviews. | Capped the interval at `MAX_INTERVAL_DAYS = 365` (standard SM-2 ceiling) so the computed date always fits and a broken row heals on its next review. | `f106-red.log` (2 failures) → `f106-green.log` (3/3); live 500 → **200**; DB row healed to 365 |
| **F108** | MEDIUM | The public exercise list hydrated the whole `Lesson` entity per row via `JOIN FETCH`, dragging `content` + `content_original` (NVARCHAR(MAX), up to 67 kB + 115 kB per row). Lesson 651 (99 exercises): ~5,724 logical reads per call, 153 ms median. | New flat `ExerciseLessonProjection` + `findLessonExercisesProjection()`; the list maps the projection, grading keeps the entity path. | median **153 → 29 ms**, avg logical reads **1,054 → 182**; the old statement's exec count stayed 48→48 during the after-probe proving the list no longer runs it; `AuditV9ExerciseListProjectionTest` 2 tests |
| **F107** | INFO | A harness warning claimed `correctAnswer` leaked from `/exercises`. The DTO always serializes the key; it is `null` for non-admins (`p1.json`: `"correctAnswer":null`). | `p2.js` now parses the body and warns only on a non-null value. | `p2` 60/60, no warning |
| **F109** | INFO | `p4e` reported "admin create (hợp lệ) 429 UNEXPECTED" because the multipart helper bypassed the harness's own rate-limit bucket flush. The limiter was correct. | `lib.flushBucket(":upload")` before each multipart POST. | `v9b-p4e2.log` 6/6 with 201 |
| **F110** | INFO | `p24` journeys asserted against invented selectors (no `/lessons/<id>` anchors, deck `.first()` hitting "Tạo bộ từ", rating buttons labelled "Lại"/"Dễ", recorder at `/speaking/<id>/record`, admin dialog ids `#form-lesson-*`). | Probed the real markup first, then rewrote the harness. | `p24_journeys.json` **8/8 PASS** |
| **F111** | MEDIUM | Sweeps leaked rows (no delete API for `speaking_submissions`), and the first cleanup script used a bare `email LIKE 'zz%'` that deleted **4 legacy audit users** belonging to the 76-user baseline. | Date/namespace-scoped cleanup with MinIO object removal, `AUDIT_CLEAN_PARITY` markers, plus verbatim restore of the 4 users from the 21:06 backup (side restore DB + `IDENTITY_INSERT`, then dropped). | `v9_cleanup_sweep.py` `PARITY_OK=True`; restore scripts listed in `findings.json` |
| **F113** | LOW | ~50 `.geo-*` selectors in `design-system.css` had 0 usage — a second, drifting styling path next to the Tailwind/SFC classes. | Removed them, kept the 4 groups still referenced. | gate `selectors=4 used=4 UNUSED=0`; CSS −7.20 kB; tests/build/design-v2 still green |

### 2.2 Verification run (round 2, final build)

- API: p1 124/124 · p2 60/60 · p3a 36/36 (real Ollama) · p3b 19/19 (real Whisper upload+assess) · p4a 18/18 · p4b 16/16 ·
  p4c 10/10 · p4d 6/6 · p4e 6/6 · p5 95 probes + 60 DB asserts · p14 adversarial 0 fail · p15 deep adversarial 21/21 ·
  p6 alias surface 0 fail · p11 AI enrich 10/10 (median 1,156 ms) · p13 error-shape census.
- Browser (Chromium): `routes-all` 228 visits (39 routes × 2 viewports × 3 roles) — 0 console errors, 0 UI-caused API ≥ 400, **Arithmetic correction:** 39 x 2 x 3 = 234, not 228; six exclusions must be identified before claiming full-route coverage.
  0 overflow, 0 not-mounted, 52 anon visits all redirected, 18/18 admin visits rendered real admin pages;
  `design-v2` 60 route×viewport cells over 5 widths with font/token/shadow/lucide assertions all clean;
  `a11y2` + `altcheck` clean; `p24_journeys` 8/8 end-to-end journeys including a fake-mic recording that produced a real
  `speaking_submissions` row and an admin lesson created + deleted through the UI.
- DB: `db-audit.md` from `p6_db_audit_v9.sql` — 24 tables, 0 orphans, 0 disabled FKs, 2 filtered indexes, 0 partitioned
  tables, log 10.2% used, one index >10% fragmentation (11.6%, under the rebuild threshold).

### 2.3 Design system (Playful Geometric / Be Vietnam Pro)

- Font verified live, not assumed: `document.fonts.check` true for 400/700/900 and 7,685 sampled text nodes computing to
  Be Vietnam Pro with 0 legacy-family hits.
- 11 `--geo-*` tokens compared by exact value across 60 cells: 0 missing, 0 drift. Lucide stroke 2.5 on 750 icons: 0 violations.
- Dead CSS removed (F113) → the single code change in this area; CSS entry −7.20 kB with the JS entry unchanged.
- Decoration measured rather than re-invented: 17 large circles + 8 dot components + 23 hard-shadow elements on the landing;
  exactly one real rotation (12deg on `.app-footer__shape--tertiary`); no `scale(1.1)` anywhere.
- Correction of the previous report: `components/decor/` DOES ship confetti, squiggle and dot components. The v8 claim
  that confetti had no implementation was wrong and is retracted.

### 2.4 Skills / workflows applied in this session

`speckit-*` pipeline (constitution reused; specify/plan-of-record → tasks → implement → converge → analyze → taskstoissues
in local-only mode) · `superpowers:executing-plans`, `systematic-debugging`, `test-driven-development` (the F106 fix was
written test-first and observed RED before the patch), `verification-before-completion`, `dispatching-parallel-agents`
(attempted, unavailable) · `codex-engineering-guardrails:code-verification`, `code-work` · `codex-security`
(finding-discovery/validation framing for the authz + upload surface) · `accessibility` (alt/tap-target/focus checks) ·
`java-springboot`, `java-coding-standards` (projection pattern, Lombok, DTO separation) · `generate-test-cases` (the F105
and F108 regression tests) · `prompt-master` (`prompt-rewritten-v5.md`) · `visualize:visualize` (not needed; charts were
not part of the deliverable).

## 3. What was NOT done — and why (no silent boundaries)

| Not done | Status | Reason / substitute |
|---|---|---|
| Live endpoint registry from `/actuator/mappings` | `BLOCKED` | The endpoint is not exposed (404; `/actuator` advertises only health). Substitute: source-derived inventory (144 mappings / 26 controllers) + live coverage gate. Not worked around by opening actuator endpoints. |
| `p25_adversarial_rederive_v9.js` (adversarial fixtures re-derived from DTO source) | `PARTIAL` | The two existing adversarial suites were re-run on the final build (p14 0 fail, p15 21/21) but they replay fixtures instead of regenerating them from the DTOs. |
| Two independent read-only subagents | `BLOCKED` | No subagent runner in this session. Substitute: two independent measurement channels (browser observation vs SQL `sys.dm_exec_query_stats`) that must agree — they did on F108. |
| `spec.md` / `clarify.md` / `checklist.md` / `constitution.md` re-authored for v9 | `PARTIAL` | The audit reuses the project constitution and the v8 clarify/checklist; v9 ships the plan-of-record, tasks, findings and reports. Stated as PARTIAL rather than claimed as done. |
| Schema/index changes (drop 0-seek indexes, add new indexes, rebuild the 11.6% index) | NOT DONE — measured decision | `sys.dm_db_index_usage_stats` only covers the window since the last restart and resets on restart; on this dataset a 0-seek index can still be chosen tomorrow, and 11.6% fragmentation on 121 pages is below the 30% threshold. Executing churn here would be unmeasured change. See `performance.md`. |
| Redis caching for read hotspots | NOT DONE — measured decision | No measured read path needs it (all tens of ms warm) and every candidate is written by the admin UI, so caching would add invalidation risk for no measured win. |
| Regenerating the 9 LISTENING `audio_url` values | NOT DONE — backlog | Regeneration requires proving the MCP/supertonic → Cloudinary pipeline mints a URL for those exact rows in one pass; that proof was not produced, so per the plan they go to `issues.md` instead of a guess. |
| Unifying the last 3 legacy `{"error": ...}` responses to ProblemDetail | NOT DONE — backlog | The frontend reads `data.error` on those paths; it is a coordinated contract change, not an audit patch. `p13` + `issues.md`. |
| Vite dev server stopped serving `GET /` / `GET /src/main.js` after hours of browser sweeps | `BLOCKED` (ops) | Recorded as F114 with the curl evidence. Workaround: load `/index.html` and use client-side routing (that is how `p21` produced its numbers). Clearing `node_modules/.vite` and restarting did not fix it; a clean restart + wait did, temporarily. Root cause remains unproven; `vite build` is unaffected. |
| `p23_screens_after.js` (screenshot set re-taken after the fixes) | NOT DONE | Unused-CSS analysis is not pixel-equivalence evidence; the design gate (`design-v2`) and the route sweep were re-run instead as the visual regression signal. Stated rather than silently dropped. |
| Gate scripts named `p10_route_registry_v9.js` / `p11_coverage_gate_v9.js` in the plan | DONE under different names | The coverage gate shipped as `sweep/v8/v9_coverage_check.py` (output `evidence/coverage-round2.json`) and the route registry as `v9_route_inventory.py` + `route-map.md`; the plan's filenames were indicative, not a contract. |
| Backlog data (4,848 empty answers, 6 empty lesson contents, 3 duplicate words, 4 `exercises_bak_v5*` tables, 8 legacy `zz*` users) | NOT MUTATED | Explicitly "measure and backlog, do not mutate". Measured in `db-audit.md`, drafted in `issues.md`. |

## 4. Artifacts

| File | Content |
|---|---|
| `findings.json` | 10 findings with id/title/severity/kind/status/root cause/fix/evidence/verification |
| `tasks.md` | T0–T2 task table with DONE / PARTIAL / BLOCKED and evidence paths |
| `workflow-log.md` | Chronological log with the command behind each piece of evidence, including the near-miss that deleted 4 users |
| `converge.md` / `analyze.md` | Spec-vs-shipped convergence, cross-artifact consistency, contradictions found and resolved, gate table |
| `issues.md` | 7 ready-to-file local issue drafts (title, body, labels, milestone, estimate, branch) — no GitHub call was made |
| `db-audit.md` | 18 measured DB blocks + backlog table |
| `performance.md` | Before/after tables, bundle table, and the list of measured-but-rejected optimisations |
| `design-audit.md` | Font/token/responsive/a11y measurements + the dead-CSS change |
| `security-audit.md` | Authn/authz method, per-finding table, verified-as-designed list, BLOCKED boundary |
| `prompt-gap-v5.md` / `prompt-rewritten-v5.md` | Prompt gap closed by measurement; rewritten, contradiction-free integration prompt with a measurable definition of done |
| `endpoint-map.md` / `route-map.md` / `evidence/*.json` | Inventory, security-rule mapping, route guards, coverage |
| `sweep/v8/*` | All harnesses and raw logs referenced above |

## 5. Historical conclusion (superseded)

The audited surface is healthy on the final build: 393 backend + 90 frontend tests green, 0 failed probes over 144/144
mappings, 0 console errors / overflow / wrong landings across 228 browser visits, DB parity at baseline, and two real
defects that a learner could actually hit (F105 answer-key leak on draft lessons, F106 permanently bricked SRS review)
fixed with regression tests and live proof. One measured performance win landed (exercise list 153 → 29 ms, −83% logical
reads); everything else was measured and deliberately left alone, with the reason recorded. The residual risk is the
stated backlog plus two BLOCKED tooling/ops boundaries — both written down rather than glossed over.

## 6. Re-verification after interrupted session (2026-09-18)

The prior session stashed the F105/F106 patches, rebuilt the old backend, and was interrupted before restoring the stash.
The restored working tree was re-verified with isolated regression tests:

- `AuditV9ControllerGuardTest`: draft `grade`/`submit` throws for a student; admin preview and published-student paths remain allowed.
- `AuditV9SrsIntervalCapTest`: interval `1,537,216` is capped to `365`; negative interval is repaired to `1`.
- Targeted result: **7/7 PASS, BUILD SUCCESS** (`sweep/v8/v9-reverify-targeted-test.log`).
- Deployed backend artifact contains `assertLessonVisible` and `MAX_INTERVAL_DAYS`; smoke `GET /api/lessons/651/exercises` returned HTTP 200.
- Manifest: `sweep/v8/v9-reverify-manifest-2026-09-18.md`.
- Backup: `C:\Users\ASUS\engflow-backups\engflow_2026-09-18-audit-v9-reverify.bak`, 35.3 MB, `RESTORE VERIFYONLY` valid, `is_damaged=0`.
- Cleanup: only `zzverify106@example.com`; `AUDIT_CLEAN_TOTAL=1`, `AUDIT_CLEAN_RESIDUE=0`; parity lessons=1471, exercises=43737, users=76, speaking=28, payments=126.

Six gaps are NOT equivalent substitutes: source-only inventory is a stated boundary; DTO-wide p25 work remains PARTIAL; mock tests and JAR markers do not replace independent reviewers. SpecKit files now exist but were authored retrospectively and their gates remain open. Index/cache no-change is an accepted decision. F114 requires completed soak evidence; no reproduction does not establish a root-cause fix.
