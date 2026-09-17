# Constitution Compliance — audit-v8-full

**Reference:** `.specify/memory/constitution.md` (v1.0.1, ratified 2026-09-01, last amended 2026-09-03)
**Audit:** audit-v8-full · **Date:** 2026-09-16
**Diff to the constitution file:** none applied in this audit (see §3)

This file is the "constitution.md **or** constitution diff reference" required by
the plan's evidence layout. The authoritative text stays at
`.specify/memory/constitution.md`; this records compliance against it and any
change that would be needed.

## 1. Compliance check (required at the start of every session)

| Principle | Requirement | Status | Evidence |
|---|---|---|---|
| **P1** | Green baseline: backend + frontend suites pass before and after | **PASS** | backend 378/378 BUILD SUCCESS; frontend 89/89 in 18 files; `t-final-backend.log`, `t-final-frontend.log` |
| **P2** | Layered architecture: Controller → Service → Repository; Vue `<script setup>` + service modules | **PASS** | No layer was bypassed; all changes stayed inside existing Controller/Service/Repository and `services/*.js` |
| **P3** | Schema owned by Hibernate `ddl-auto=update`; Flyway disabled; no migration files | **PASS** | No migration file added. `db-audit.sql` is **read-only**; the only DML was data cleanup, applied as SQL + verified on `engflow-sqlserver` |
| **P4** | AI stays local (Ollama / Whisper); no new paid cloud-AI dependency | **PASS** | No AI dependency added. `enrich-word` still routes to local Ollama; no cloud AI call introduced |
| **P5** | Every performance claim carries before/after measurement | **PASS** | `performance.md`: N=7 live measurements per endpoint; F87 shape re-verified fresh; where no before/after pair exists the row says so |
| **P6** | Playful Geometric design system; **Be Vietnam Pro is the only font** | **PASS** | `design-audit.md`: 0/7 685 computed elements non-BVP; 0 legacy families; 19/19 `font-family` declarations via `--geo-font`; 15 faces loaded |
| **P7** | Vietnamese UI copy, English technical terms; a11y mandatory (focus-visible, skip-link, contrast AA, `prefers-reduced-motion`) | **PASS** | 0 missing focus styling; skip-link present; **0** tap targets < 24 px (WCAG 2.5.8 AA); reduced-motion honoured (transition `0s`, transform `none`) |
| **P8** | Every change has runtime evidence, not "the code looks right" | **PASS** | Real HTTP on :8080 and real Chromium for UI; 152 route visits, 105 timed API calls, real-browser XSS execution proof |

**No principle was violated.** No new abstraction, no single-use abstraction and
no unrequested feature was introduced (complexity budget respected).

## 2. How each article shaped this audit

**Article I — technical**

- **P1** drove the decision to count tests from the **run log**, never from
  `target/surefire-reports` XML. Stale XML for a deleted class
  (`UserServiceUnlimitedAiGenerationTest`) previously inflated the aggregate by
  +8. The fresh count is taken from the log's `Tests run:` lines.
- **P3** is why `db-audit.sql` contains **no DML at all** — every statement is a
  `SELECT`. Data cleanup lives in separate, explicitly-scoped files
  (`clean_r2.sql`, `clean-xss-probe.sql`), each starting with
  `SET QUOTED_IDENTIFIER ON;`.
- **P5** is why `performance.md` refuses to claim an optimisation it did not
  measure: the F87 before-figure (95 k reads / 367 ms) is a *documented
  historical* measurement, and the report says so rather than presenting it as
  freshly re-derived. What is freshly proven is that the current code holds the
  intended shape (4 600 reads / 35 ms, no `content_original`).

**Article II — design**

- **P6** was verified at four levels (declaration, token, `document.fonts.check`,
  computed style). Only the computed-style pass can catch a family applied by a
  third-party component or an inline style, so it is the one that carries the
  claim.
- **P7** required distinguishing WCAG **AA** from **AAA**: 0 targets below 24 px
  is an AA pass; the 138 targets in the 24–44 px band are an AAA recommendation
  and were deliberately **not** "fixed", because widening dense admin table rows
  to 44 px would be a real usability regression.
- **P8** is why the XSS question was answered by **executing** the payload in
  Chromium rather than by reading the sanitizer config.

## 3. Constitution diff — none applied

No amendment was made. Recording why, since P1 explicitly anticipates one:

> P1: "con số này là mốc audit-v3; mỗi kỳ audit **PHẢI đo lại và ghi giá trị thực tế**"
> ("this number is the audit-v3 marker; each audit period MUST re-measure and
> record the actual value")

The constitution's P1 text currently cites **221 backend / 73 frontend tests**
from audit-v3. The measured values are **378 backend / 89 frontend in 18 files**.

P1 instructs each audit to *re-measure and record* the actual value. Both are
done: measured here, and recorded in `REPORT.md`, `AGENTS.md` and the baseline
tables. The stale number inside the constitution text is therefore **not a
compliance failure** — the principle anticipates drift and puts the obligation on
the audit report, which is where the current figures now live.

**Deliberately not amended**, because amending is a governance act under
Article III (bump semver, write a Sync Impact Report) and it changes a document
that other rounds also cite. Editing the constitution is outside what this audit
was asked to do. Flagged as **deferred** for the owner, with the exact edit that
would be needed:

```diff
- ### P1. Baseline xanh là tiền đề
- Trước và sau mọi thay đổi: backend `mvnw.cmd test` = 221 tests, frontend
- `npx vitest run` = 73 tests / 14 files (con số này là mốc audit-v3; mỗi kỳ
- audit PHẢI đo lại và ghi giá trị thực tế). Mọi PR/commit không giữ baseline
- xanh bị từ chối.
+ ### P1. Baseline xanh là tiền đề
+ Trước và sau mọi thay đổi: backend `mvnw.cmd test` = 378 tests, frontend
+ `npx vitest run` = 89 tests / 18 files (con số này là mốc audit-v8-full,
+ đo 2026-09-16; mỗi kỳ audit PHẢI đo lại và ghi giá trị thực tế). Mọi
+ PR/commit không giữ baseline xanh bị từ chối.
```

Version would become **1.0.2** (PATCH: numbers only, no principle changed).

## 4. Governance record

| Item | Value |
|---|---|
| Compliance check performed | 2026-09-16, at session start |
| Principles checked | P1–P8 |
| Violations | **0** |
| Amendments applied | **0** |
| Amendments deferred | 1 (P1 baseline numbers, PATCH → v1.0.2) |
| Conflicts requiring resolution | **0** — `AGENTS.md` and the constitution agreed throughout |
