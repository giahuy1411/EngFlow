# audit-v11-full specification

**Authored:** 2026-09-20 (+07) · **Predecessor:** `audit-v10-full` · **Branch:** `audit-streak-review`
**Checkpoint:** `dae9667` (337 files committed — the first git rollback point since audit-v8)

## Why this audit exists, stated honestly

The user's instruction was: *"scan and check the entire codebase and database, test the backend by
running every API, interact with the whole web UI corresponding to each API, verify the UI matches
the design-system prompt below, and replace all fonts with Be Vietnam Pro."*

**Three of those instructions are already satisfied and v11 must not pretend to discover them:**

1. **The font is already Be Vietnam Pro, everywhere.** No second family exists. There is no
   replacement to perform. (Verified in `index.html`, `tailwind.config.js`, `design-system.css`;
   zero Outfit/Plus Jakarta hits in `frontend/src`.)
2. **The Playful Geometric design system is already implemented** — tokens, decor components and
   every literal-prompt feature from audit-v10 (hero sun, blob shape, dashed connector, pricing
   `scale(1.1)` + `rotate(15deg)` badge, squiggle dividers, marquee, arrow button).
3. **Audits v1→v10 already exist.** v10 (dated the same day as this session) reports green on 470
   backend tests, 106 frontend tests, 342 browser visits, API 35/35, streak 25/25, and a parity line
   that this session independently re-measured and **confirmed byte-for-byte**
   (`1471|43735|72|127|28|15|4|126|14|5`).

So a v11 that "scans everything from scratch and reports the design system is nicely implemented"
would be a fabricated audit. The user chose **"làm lại từ đầu"** — redo from scratch, trusting no
prior report. v11 honours that by **re-measuring rather than re-reading**: every number below is
produced by this session's own commands, and prior reports are treated as hypotheses to falsify,
not as evidence.

## Objective

Independently re-establish, from scratch and with this session's own measurements:

1. the true baseline (backend, frontend, build, containers, DB) — **not** quoted from v10;
2. full API coverage over the live container, and full UI coverage over the live SPA, with the two
   **cross-checked against each other** (each UI action traced to the API call it makes);
3. the DB state in Docker, including schema constraints and data integrity;
4. UI/UX against the Playful Geometric prompt, using **chrome-devtools MCP and Playwright MCP** —
   the two tools v10 explicitly could not use;
5. performance before/after for anything v11 proposes to change;
6. a second, wider pass over the whole surface, then a truthful report of what was done, what was
   not, what was fixed and how, and which skills were loaded.

## Requirements

### R1 — Every number is this session's own measurement
No figure is copied from a prior audit into a conclusion. Prior reports may only be used to *aim*
a probe (e.g. "v10 says F126 is fixed — go try to reproduce it"). Where a v11 number agrees with
v10, that is a **confirmation of v10**, recorded as such; where it disagrees, the disagreement is
the finding.

### R2 — API surface is exercised, not merely enumerated
The endpoint inventory is re-derived from source. Every reachable endpoint is probed with the
correct role (anon / student / admin) and the correct body. A `2xx` is not accepted as proof of
correctness: the response **contract** is checked (field names, types, the streak snapshot's seven
documented fields), and role violations are checked in **both** directions (allowed stays allowed,
forbidden actually 403/404s).

### R3 — UI is cross-checked against the API, not tested in isolation
For each core function — Lessons/Exercises, Streak, Login/Register, Search/Sort, CRUD, AI — the
UI is driven in a real browser and the **network calls it emits** are captured and compared to the
API's own contract. A UI that renders plausibly while calling the wrong endpoint is a finding; a UI
that renders an error while the API is healthy is a finding.

### R4 — The DB in Docker is audited as data, not just as a schema
Parity is re-measured. Schema constraints (FK, unique index, filtered indexes) are checked for
*existence* and then for *effect* (does the constraint actually block a violating insert?).
Orphan and integrity queries are written so that a **NULL foreign key is not miscounted as an
orphan** — a probe bug that v10 hit and recorded.

### R5 — The design system is verified, and its holes are looked for
The prompt is treated as a specification to check **against the code**, including its own
internal consistency. Where the prompt is factually wrong about this codebase (it says "Lucide
React"; the project uses `lucide-vue-next`) the prompt is corrected, not the code. Where the prompt
mandates something that fails accessibility (accent colours used as text), that is a **defect in
the prompt's practical application** and is reported with measured ratios.

### R6 — The second pass is wider than the first
After the first pass, the whole surface is re-run on the final build with additional adversarial
cases: repeated browser sweeps, boundary conditions on streak and SRS, guard assertions in both
directions, and a cross-artifact consistency check.

### R7 — Evidence discipline, and no silent caps
Every claim points at the artifact that produced it. Anything unverified is reported BLOCKED or
PARTIAL with the reason. Any bounded sweep states what it dropped. A probe that reports a defect
the app does not have is treated as a defect in the probe and is recorded.

## Success criteria — measured outcomes

| Criterion | Result |
|---|---|
| Baseline measured by this session | *(filled by tasks T0.3–T0.4)* |
| API sweep | *(T2)* |
| UI sweep with chrome-devtools + Playwright MCP | *(T3)* |
| UI↔API cross-check for the 6 core functions | *(T3)* |
| DB audit in Docker | *(T4)* |
| Design-system / prompt conformance | *(T5)* |
| Contrast AA | *(T5)* |
| Performance before/after | *(T6)* |
| Second pass | *(T7)* |
| Parity after every mutating run | *(continuous)* |

## Known holes in the prompt as written (analysed, with evidence)

The user asked specifically: *"check this prompt for holes, then fix it and implement."* The prompt's
holes are recorded here with the measurement that shows them, so the fix is grounded:

| # | Hole in the prompt | Evidence |
|---|---|---|
| **H1** | It mandates `secondary`/`tertiary`/`quaternary` for "emphasized words", but those colours **fail WCAG AA as text** on the cream/white background the same prompt mandates. | Measured live: `text-secondary` on white = **2.65:1** (needs 3:1 large / 4.5:1 small). 7 of 11 real usages fail. |
| **H2** | It says "Lucide **React**". This is a Vue 3 project using `lucide-vue-next`. | `frontend/package.json` dependency list. |
| **H3** | It requires Tailwind utility names (`max-w-6xl`, `py-24`) as literal implementation while itself saying *"do not rewrite components just to add them"* — self-contradictory. | Prompt `<layout>` vs its own closing note. |
| **H4** | It specifies "hard shadows **4px**" with **no mobile exception**, while its own responsive section says mobile must reduce shadows to 2px. Two sections disagree. | Prompt `<shadows>` vs `<responsive>`. |
| **H5** | It mandates **no dark mode** and **no `prefers-color-scheme`** handling at all, in a design system meant to be "maintainable long-term". | Zero `dark:` / `prefers-color-scheme` in `frontend/src/assets/*.css` and `tailwind.config.js`. Recorded as a deliberate boundary, not silently. |
| **H6** | It says the hero image "has a blob mask", but the hero contains **no photograph** — it is pure geometric CSS. | `Home.vue` hero: `.app-hero__sun` + four `.app-hero__shape` spans, no `<img>`. |
| **H7** | It requires a "Pricing" section where "the middle card is scaled up" — but the product has **exactly two** plans, so there is no middle card. | `PremiumPage.vue`: `Gói tháng` + `Gói năm`, `md:grid-cols-2`. |
| **H8** | It requires a "Features" grid **of 3** with alternating violet/pink/yellow headers, but the Home page has **4** features. | `Home.vue` `features` array has 4 entries. |

H1 is the only hole with a live user-facing accessibility consequence; the rest are prompt/codebase
divergences already handled in code with recorded divergence notes (audit-v10 §3.6). v11 reports
them rather than silently "fixing" the code to match a prompt that is wrong about the product.

## Boundaries

- Development/verification environment only — a graduation project, not a production rollout.
- No new dependency, no Flyway migration, no additional actuator endpoint.
- No `.env`, credential, or fixture media in any artifact.
- Sweeps that mutate use an audit namespace, clean up **in the same run**, and re-assert parity.
- Destructive DB steps require a fresh backup and an enumerated ID list — never a bare `LIKE`.
- `sqlcmd` exits 0 even when a batch fails: every batch output is scanned for `Msg \d+`, and every
  DELETE batch carries `SET QUOTED_IDENTIFIER ON`.
- If a tool is missing or refused, it is recorded as BLOCKED with the observation — never replaced
  by an invented result, and never allowed to inherit a neighbour's success.
