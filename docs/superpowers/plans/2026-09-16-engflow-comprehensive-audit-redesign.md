# EngFlow Comprehensive Audit, UI Verification, and Design-System Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Every task is independently verifiable. Do not claim completion without fresh command output.

**Goal:** Audit and, where justified by evidence, repair the complete EngFlow backend, database, frontend, UI/UX, security, AI, and performance surface while enforcing the Playful Geometric design system with Be Vietnam Pro as the only application font.

**Architecture:** Use the existing Vue 3/Vite frontend, Spring Boot backend, SQL Server/Redis/MinIO/Ollama/Whisper Docker stack, and existing `sweep/v8` harnesses. Build an evidence-led loop: inventory → baseline → reproduce → fix surgically → regression test → re-run. Keep audit tooling and evidence separate from production code, and never treat unavailable infrastructure as a pass.

**Tech Stack:** Spring Boot 4.0.6, Java 25, Maven Wrapper, Vue 3, Vite, Tailwind CSS, Vitest/jsdom, Chromium/Playwright, SQL Server 2019, Redis, MinIO, Ollama, Whisper sidecar, Docker Compose, `.dsh` SpecKit/browser/performance/security skills.

**Spec:** `.specify/specs/audit-v8-full/spec.md`; prompt corrections: `.specify/specs/audit-v8-full/prompt-gap.md`.

## Global Constraints

- Preserve all existing uncommitted user changes; inspect `git status` before every write and never use `git reset --hard` or `git checkout --`.
- Follow the required workflow: `constitution → specify → clarify → checklist → plan → tasks → analyze → implement → converge → analyze → taskstoissues (only if needed) → report`.
- Use the `.dsh` skills for each matching phase: `speckit-*`, browser/devtools testing, frontend UI engineering, performance optimization, security hardening, test-driven development, convergence, and issue conversion.
- UI copy is Vietnamese; technical terms remain English.
- The only application font is Be Vietnam Pro. Do not add Outfit, Plus Jakarta Sans, React-only packages, or cloud AI dependencies.
- Use existing design tokens in `frontend/src/assets/design-system.css` and `frontend/tailwind.config.js`; do not create a second token system.
- Any performance claim requires a before/after measurement.
- Any database DML requires a verified backup first; filtered-index DELETE batches must begin with `SET QUOTED_IDENTIFIER ON;`.
- Do not commit `.env`, credentials, backups, PII, generated audio fixtures, or temporary login files.
- Do not seed demo data. Test data must be identifiable, minimal, and cleaned up safely.
- A service outage is `BLOCKED`, not `PASS`.
- Every finding must have an ID, severity, reproduction evidence, affected surface, fix or disposition, regression check, and final status.

## Required Evidence Layout

Use the existing feature directory `.specify/specs/audit-v8-full/` and extend it without deleting prior evidence:

- `constitution.md` or constitution diff reference
- `spec.md`, `clarify.md`, `checklist.md`
- `plan.md`, `tasks.md`, `analyze.md`, `converge.md`
- `endpoint-map.md`, `route-map.md`, `db-audit.md`, `performance.md`, `design-audit.md`, `security-audit.md`
- `REPORT.md`
- `evidence/` for fresh logs, JSON, screenshots, traces, SQL output, and metric snapshots
- `findings.json` with stable IDs and lifecycle status

Do not place secrets in evidence. Redact tokens, passwords, signed URLs, and personal data.

---

### Task 1: Freeze scope, inspect repository state, and register the workflow

**Files:**
- Read: `AGENTS.md`, `.specify/memory/constitution.md`, `.specify/specs/audit-v8-full/*`, `README.md`
- Create/Modify: `.specify/specs/audit-v8-full/clarify.md`, `.specify/specs/audit-v8-full/workflow-log.md`

**Interfaces:**
- Produces: a scope decision, assumptions, blocked-state rules, and a timestamped workflow log consumed by every later task.

- [ ] Record `git status --short`, current commit, Java/Node/npm/Docker versions, and the list of already-modified files.
- [ ] Record the current services and ports without exposing credentials.
- [ ] Record the existing baseline claims, but label them `historical` until freshly rerun.
- [ ] Confirm the audit scope: all backend mappings, all frontend router routes, all primary user/admin flows, DB schema/data/performance, and design-system compliance.
- [ ] Record explicit exclusions: production deployment, destructive data cleanup without approval, new cloud integrations, and speculative refactors.
- [ ] Verify `.dsh` skills used and their source paths in the workflow log.
- [ ] Acceptance: the scope can be handed to another agent without relying on conversation history.

### Task 2: Constitution compliance and prompt normalization

**Files:**
- Read/Modify: `.specify/memory/constitution.md`
- Create/Modify: `.specify/specs/audit-v8-full/prompt-rewritten.md`, `.specify/specs/audit-v8-full/prompt-gap.md`

- [ ] Confirm P1–P8 against the current repository, updating only if a principle or baseline is stale.
- [ ] Resolve prompt contradictions: Vue/lucide-vue-next instead of React/Lucide React; Be Vietnam Pro only; existing `--geo-*` tokens authoritative.
- [ ] Define measurable UI acceptance: computed font set, token values, focus-visible, reduced-motion, contrast, tap targets, and no horizontal overflow at `360/768/1280/1440/1920`.
- [ ] Define measurable API acceptance: endpoint inventory, auth role matrix, expected status, response shape, and cleanup strategy.
- [ ] Define measurable performance acceptance: baseline and after values for API latency, query timing, bundle/chunk sizes, and browser metrics where available.
- [ ] Define two verification rounds: Round 1 is broad discovery and remediation; Round 2 repeats the full matrix plus adversarial edge cases and regression confirmation.
- [ ] Acceptance: no contradictory font/framework instructions, no “check everything” wording without a test oracle, and no unexplained placeholder.

### Task 3: Specify the audit as independently testable user stories

**Files:**
- Modify: `.specify/specs/audit-v8-full/spec.md`

- [ ] Add user stories for anonymous/user/admin API access, auth, lessons/exercises, streak, search/sort, CRUD, AI, DB integrity, UI/UX, accessibility, and performance.
- [ ] For each story define preconditions, exact action, expected result, persisted side effect, cleanup, and evidence path.
- [ ] Define failure classes: `BUG`, `REGRESSION`, `SECURITY`, `PERFORMANCE`, `DESIGN_DRIFT`, `BLOCKED`, `NOT_APPLICABLE`.
- [ ] Define success criteria that are technology-agnostic where possible and exact commands where required.
- [ ] Acceptance: every requirement maps to at least one later task and one evidence artifact.

### Task 4: Requirements checklist and consistency analysis

**Files:**
- Create/Modify: `.specify/specs/audit-v8-full/checklist.md`, `.specify/specs/audit-v8-full/analyze.md`

- [ ] Check scope completeness, security boundaries, data safety, observability, cleanup, accessibility, and browser coverage.
- [ ] Run read-only consistency analysis after tasks are drafted and before implementation.
- [ ] Check that task names, files, commands, expected outputs, and artifact paths agree.
- [ ] Mark unresolved ambiguity explicitly; do not silently choose a destructive or user-visible behavior.
- [ ] Acceptance: analyze reports no contradictory task/spec requirement; unresolved items are either clarified or marked blocked.

### Task 5: Build authoritative backend endpoint inventory

**Files:**
- Read: `src/main/java/**/controller/**/*.java`, security configuration, DTOs, services
- Create/Modify: `sweep/v8/endpoint-map.js` or equivalent, `endpoint-map.md`, `evidence/endpoint-inventory.json`

- [ ] Extract every mapped HTTP method/path, including compatibility aliases and media/resource routes.
- [ ] Record controller, service, request DTO, response shape, auth requirement, role requirement, rate-limit bucket, and side effects.
- [ ] Add negative cases: missing auth, wrong role, malformed body, missing resource, duplicate resource, invalid enum/type, upload extension/content mismatch.
- [ ] Ensure route paths match actual mappings, not stale documentation.
- [ ] Acceptance: inventory count is reproducible from source and every entry has a runnable probe or an explicit blocked reason.

### Task 6: Establish clean backend and frontend baselines

**Files:**
- Create: `.specify/specs/audit-v8-full/evidence/baseline-<timestamp>/`
- Read-only commands: `cmd /c "mvnw.cmd test"`, `Set-Location frontend; cmd /c "npx vitest run"`, `cmd /c "npx vite build"`

- [ ] Run backend tests from repo root and save complete output plus exit code.
- [ ] Run frontend tests and save complete output plus exit code.
- [ ] Run frontend build and save output, chunk list, and total/entry sizes.
- [ ] Record Docker health and application logs for the exact run.
- [ ] Do not reuse historical counts as current results.
- [ ] Acceptance: baseline is either green with exact counts or a categorized pre-existing failure list.

### Task 7: Run the complete API matrix against real services

**Files:**
- Modify/Create: `sweep/v8/**`, `evidence/api-round-1/**`, `endpoint-map.md`

- [ ] Start or verify backend, SQL Server, Redis, MinIO, Ollama, and Whisper without exposing secrets.
- [ ] Execute each endpoint with no-auth, user, and admin contexts where applicable.
- [ ] Refresh JWTs during long sweeps; isolate rate-limit buckets and document any intentional Redis test cleanup.
- [ ] Capture status, response schema, latency, DB side effect, and correlation/finding ID.
- [ ] Test authentication, registration, reset/change password, lessons, exercises, streak, search/sort, CRUD, payment render-only paths, decks, flashcards, SRS, vocabulary, speaking, videos, admin, upload/media, and AI.
- [ ] Use real media only for Cloudinary/Whisper paths; synthetic bytes must not be called a successful upload.
- [ ] Acceptance: every inventory entry is `PASS`, `FAIL`, `BLOCKED`, or `N/A` with evidence.

### Task 8: Verify database integrity and Docker behavior

**Files:**
- Read: `src/main/java/**/model`, repositories, `docker-compose.yml`, `sql/**`
- Create/Modify: `db-audit.md`, `evidence/db-round-1/**`, read-only SQL scripts under `sweep/v8/sql/`

- [ ] Record schema/table/column/index/FK/constraint inventory from the live SQL Server container.
- [ ] Check entity-to-schema compatibility using the existing validate drill pattern without changing production `ddl-auto` behavior.
- [ ] Check orphan rows, nullability violations, duplicate business keys, stale test rows, and referential integrity.
- [ ] Check indexes and query statistics; distinguish production queries from ad-hoc audit scripts.
- [ ] Before any cleanup or bulk mutation, create and verify a backup and perform a restore drill where required.
- [ ] Acceptance: all DB claims cite query output, timestamp, database name, and whether the query was read-only or mutating.

### Task 9: Measure and optimize backend/database performance surgically

**Files:**
- Create/Modify: `performance.md`, `evidence/performance-round-1/**`, targeted production/test files only when a finding is confirmed

- [ ] Capture baseline latency for representative API calls, including pagination/search/admin list, auth, exercise grading, streak, and AI.
- [ ] Capture SQL logical reads, duration, execution plan/query stats for confirmed hot paths.
- [ ] Capture rate-limit and AI latency separately; do not mix model cold-start time with application regression without labeling it.
- [ ] For each candidate optimization, write a failing or regression test where practical, make the smallest fix, and rerun the same measurement.
- [ ] Do not add indexes for leading-wildcard queries without measured benefit; do not optimize from intuition.
- [ ] Acceptance: every optimization has before/after numbers, changed files, regression coverage, and rollback rationale.

### Task 10: Verify every frontend route and API-backed UI flow

**Files:**
- Read: `frontend/src/router/index.js`, views, components, services, stores
- Create/Modify: `route-map.md`, `evidence/ui-round-1/**`, `sweep/v8/ui/**`

- [ ] Enumerate every route, auth guard, required role, API calls, loading state, empty state, error state, success state, and cleanup action.
- [ ] Use real Chromium/Playwright or the available `.dsh` browser/devtools workflow; do not rely on snapshots alone.
- [ ] Exercise login/register/logout/reset, lesson browsing/content/exercises/submission, streak/history, search/sort, video/quiz, decks/games, speaking, profile, premium render-only path, and all admin CRUD/AI screens.
- [ ] Capture network requests, console errors, visible error states, screenshots, and persisted side effects.
- [ ] Verify API and UI contracts agree on response shape and error handling.
- [ ] Acceptance: every route has a result and every user-visible defect has a finding ID.

### Task 11: Audit design-system and accessibility compliance

**Files:**
- Read/Modify only for confirmed findings: `frontend/src/assets/design-system.css`, `frontend/tailwind.config.js`, `frontend/src/main.js`, shared UI components, affected views
- Create/Modify: `design-audit.md`, `evidence/design-round-1/**`

- [ ] Verify Be Vietnam Pro is the only computed application font across all routes and no Outfit/Plus Jakarta Sans is loaded.
- [ ] Verify token values, border width, radii, hard shadows, button/card/input states, Lucide stroke width, and responsive rules.
- [ ] Verify no horizontal overflow at `360/768/1280/1440/1920`; investigate raw `scrollWidth/clientWidth` values and ancestor overflow before declaring a defect.
- [ ] Verify WCAG 1.1.1 image alt behavior, 2.4.7/2.4.11 focus visibility, 2.3.3 reduced motion, 2.5.8 target size, and color-not-only semantics.
- [ ] Verify mobile shadow reduction and decoration hiding; keep work screens denser than marketing/landing screens.
- [ ] Fix only confirmed drift, with colocated tests where behavior is testable.
- [ ] Acceptance: computed-style and DOM evidence exists for every route class; subjective visual claims are labeled as design observations, not pass/fail facts.

### Task 12: Security and AI-specific audit

**Files:**
- Read: security config, upload writers/routes, AI services, sanitization utilities, rate-limit filter
- Create/Modify: `security-audit.md`, `evidence/security-round-1/**`, regression tests for confirmed findings

- [ ] Verify authentication, authorization, ownership checks, rate-limit bucket routing, password/reset token expiry, and error leakage.
- [ ] Verify upload extension/content handling and `/api/resources/**` stored-XSS boundaries using safe test fixtures.
- [ ] Verify secrets are absent from tracked files and evidence.
- [ ] Verify AI output is parsed/validated as data, bounded by count/size/time, and cannot execute SQL/HTML/shell behavior.
- [ ] Verify prompt inputs exclude secrets and unrelated users’ PII.
- [ ] Acceptance: no reachable critical/high issue remains unmitigated; lower findings have explicit disposition.

### Task 13: Implement confirmed fixes using TDD and surgical changes

**Files:**
- Only files named by confirmed findings; tests colocated according to existing Java/Vue conventions

- [ ] For each bug: reproduce with a focused test or deterministic harness; record the failing output.
- [ ] Implement the minimum fix consistent with existing architecture.
- [ ] Run the focused test, then the relevant suite, then the full backend/frontend tests.
- [ ] Add evidence linking finding ID to test name, changed file, and result.
- [ ] Avoid unrelated refactors, dependency additions, schema migrations, demo data, or redesign-by-opinion.
- [ ] Acceptance: each fixed finding has a regression test or a documented reason a test cannot be automated.

### Task 14: Converge and run the second, stricter audit round

**Files:**
- Create/Modify: `converge.md`, `evidence/round-2/**`, updated audit reports

- [ ] Compare implementation against spec, checklist, plan, and tasks; add missing tasks before fixing gaps.
- [ ] Rerun the complete API matrix, DB audit, frontend route matrix, design audit, security checks, and performance measurements.
- [ ] Add adversarial cases: expired token, wrong role, malformed JSON, duplicate registration, empty search, out-of-range pagination, stale resource, invalid upload, AI malformed output, reduced-motion, narrow viewport, slow network, and service-unavailable conditions.
- [ ] Compare round 1 versus round 2 findings; no finding may disappear without status/disposition.
- [ ] Acceptance: round 2 is at least as broad as round 1 and all remaining failures are accurately classified.

### Task 15: Convert unresolved actionable findings to issues, if applicable

**Files:**
- Read: `.git/config`, remote metadata, `findings.json`
- Create/Modify: `.specify/specs/audit-v8-full/issues.md` only if issue conversion is actually needed

- [ ] Run `taskstoissues` only when a GitHub remote exists and unresolved findings need external tracking.
- [ ] Never create duplicate issues for fixed or already-tracked findings.
- [ ] Include severity, reproduction, evidence path, expected/actual, impact, and suggested next step.
- [ ] Acceptance: either issues are created with verified links/IDs or the report states “not run: no GitHub issue conversion required.”

### Task 16: Produce the final evidence-backed report

**Files:**
- Create/Modify: `.specify/specs/audit-v8-full/REPORT.md`, `findings.json`, optional `tasks/evidence/**`

- [ ] Summarize exact commands, timestamps, environment, and exit codes.
- [ ] Report separately: completed, fixed, not fixed, blocked, not applicable, and deferred.
- [ ] For every fix explain root cause, implementation, regression test, and before/after metric.
- [ ] Include API coverage count, route coverage count, DB checks, performance measurements, UI/a11y results, and AI/service availability.
- [ ] List every skill loaded, including relevant `.dsh` skills and Superpowers/Visualize skills actually used.
- [ ] Attach links to evidence files and screenshots using absolute paths where applicable.
- [ ] Run a final placeholder scan and verify no claim of “pass” lacks fresh evidence.
- [ ] Acceptance: a new agent can reproduce the report from the commands and artifacts alone.

## Final Verification Commands

Run fresh commands at the end; do not rely on historical logs:

```powershell
cmd /c "mvnw.cmd test"
Set-Location frontend; cmd /c "npx vitest run"; cmd /c "npx vite build"
Set-Location ..
git diff --check
git status --short
```

Also run the full API/UI/DB harnesses defined in Tasks 7–14 and record their exit codes. If any command fails, the final report must state the failure and its evidence instead of claiming completion.

## Self-Review Checklist

- [ ] Every user requirement maps to at least one task.
- [ ] Every task has files, interfaces, commands, evidence, and acceptance criteria.
- [ ] No task says only “test the above” or “handle edge cases” without defining cases.
- [ ] No task assumes a service is healthy without a probe.
- [ ] No DML step lacks backup/restore safeguards.
- [ ] No performance claim lacks before/after numbers.
- [ ] No UI claim lacks computed DOM/browser evidence.
- [ ] No completion claim is made without fresh verification.
