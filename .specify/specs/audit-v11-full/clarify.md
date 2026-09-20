# audit-v11-full — clarification log

**Method:** ambiguities in the user's instruction were resolved **before** planning. Each entry
records the question, the answer as given, and the consequence for the plan. Answers came from the
user via a structured question at the start of the session; none were inferred.

---

## C1 — Scope: what should v11 actually do?

**Ambiguity.** The instruction says *"scan and check the entire codebase and database"*, but audits
v1→v10 already exist and v10 (same day) reports green. A literal re-scan risks producing a
fabricated "discovery" of work that is already done and verified.

**Options put to the user:**
- (a) close v10's gaps + a wider second round;
- (b) **redo from scratch, assuming nothing**;
- (c) design-system/font verification only.

**Answer: (b) "làm lại từ đầu".**

**Consequence for the plan.** Prior reports are demoted to *hypotheses*. Every number in the v11
report must be produced by this session's own command. Where v11 agrees with v10, that is recorded
as a **confirmation**, not as a source. Where it disagrees, the disagreement is a finding. This is
why Phase 0 re-measures the baseline instead of quoting v10's 470/106, and why the parity line is
re-run rather than cited. It is also why the spec states plainly that the font and design system
are **already implemented** — an audit that "discovers" them would be fabricating work.

## C2 — Git state: 337 uncommitted files, no rollback point

**Ambiguity.** The working tree carried the streak refactor plus all v9/v10 fixes, uncommitted.
audit-v10's `tasks.md` records **T0.5 (create the git checkpoint commit) as still open** — so there
was no git rollback point, only a DB backup.

**Options put to the user:** commit a checkpoint first / work on the dirty tree / branch for v11.

**Answer: commit a checkpoint first.**

**Consequence.** Done before any measurement: commit **`dae9667`**, 337 files, after verifying no
`.env` or credential is staged (`.gitignore` confirmed to exclude `.env`). The tree is now clean, so
every change v11 makes is attributable and revertible. The audit's own fixes therefore land on top
of a known state.

## C3 — DB writes: may the audit create and delete data?

**Ambiguity.** Testing CRUD, payment, submission and speaking end-to-end requires **writing** to
the development database. The instruction does not say whether that is permitted.

**Options put to the user:** writes allowed with backup + self-cleanup + parity assertion / read-only.

**Answer: writes allowed, with backup, in-run cleanup and a parity assertion.**

**Consequence.** Phase 2 and Phase 4 may create rows (payment orders, submissions, a CRUD lesson
and deck) — but every mutating run must (a) start from a fresh backup, (b) use an audit namespace,
(c) clean up **in the same run**, and (d) re-assert the parity line. This is the discipline v8–v10
already used, and the reason `cleanupAuditPayments()` exists in the harness. Destructive steps still
require enumerated IDs — never a bare `LIKE` pattern, which once deleted four baseline users.

## C4 — Is "replace all fonts with Be Vietnam Pro" a real task?

**Ambiguity.** The instruction says to replace all fonts with Be Vietnam Pro. Read literally, that
is a work item.

**Resolution — measured, not assumed.** The work is **already complete**:
`frontend/index.html` loads only `Be+Vietnam+Pro:wght@400;500;600;700;800;900`;
`tailwind.config.js` maps `sans`, `heading` **and** `mono` to BVP; `design-system.css` sets
`--geo-font` to BVP; and `grep` finds **zero** occurrences of Outfit or Plus Jakarta anywhere in
`frontend/src`. Constitution **P6** already mandates BVP and forbids the legacy families.

**Consequence.** v11 does **not** perform a font replacement. It **verifies** the requirement holds
in the live DOM (every text node's computed `font-family`) and reports it as already satisfied. Had
this been reported as new work, it would be a fabricated finding.

## C5 — Which browsers, and does the engine matter?

**Ambiguity.** The instruction names *chrome-devtools-mcp* and *playwright-mcp*. audit-v10's report
records that **neither MCP was available** in its session, and it substituted `playwright-core`
driving Edge/Brave via `executablePath`.

**Resolution.** Both MCPs were probed at the start of this session and **both are live**:
chrome-devtools `list_pages` returned a page; Playwright `browser_navigate` loaded the SPA.

**Consequence.** Phase 3 uses the MCPs as instructed — this is the genuine capability upgrade of
v11 over v10. Because v10's numbers came from a different driver, v11 **does not compare its
screenshot-level results against v10's**; it records which driver produced which number, so the two
sources are never silently mixed. Font rendering can differ between Edge and the MCP's browser, and
that is stated rather than glossed.

## C6 — How are "holes in the prompt" handled?

**Ambiguity.** The instruction says *"check this prompt for holes, fix it, and implement."* It does
not say whether "fix" means fix the prompt or bend the code.

**Resolution, following audit-v10's precedent.** Where the prompt is factually wrong about this
codebase, the **prompt** is corrected — not the code. Concretely: the prompt says "Lucide React"
(this is a Vue project using `lucide-vue-next`); it demands Tailwind utility names while itself
forbidding rewrites to add them; its shadow section contradicts its responsive section; it describes
a hero photograph that does not exist; and it assumes a three-tier pricing table when the product
has two plans.

**Consequence.** These are recorded as prompt/codebase divergences in `spec.md` (H1–H8) with the
evidence for each. The one hole with a real user-facing consequence — accent colours failing WCAG
AA as text — is treated as a **defect to fix in the code**, because there the prompt's intent
(legible emphasized text) and its literal token choice disagree, and accessibility is a
constitutional requirement (P7).

---

## Open questions deliberately NOT asked

- **Which defects to fix** — cannot be known before Phase 1–4 measure them. The plan fixes what is
  measured and reports the rest; asking upfront would invite speculation.
- **How long the second pass should run** — the user said time does not matter ("không quan trọng
  về thời gian"), so Phase 7 is bounded by coverage, not by a clock.
