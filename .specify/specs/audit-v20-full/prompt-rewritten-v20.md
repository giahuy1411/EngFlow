# prompt-rewritten-v20 — Playful Geometric integration prompt (audit-v20 edition)

This supersedes `prompt-rewritten-v19.md`. It describes the codebase **after** the audit-v20 pass, and it
**corrects the flaws measured in the prompts of earlier rounds** (see §Prompt corrections). Treat this
file as the **conformance standard** the design is audited against — not the original pasted prompt.

> **audit-v20 note — this file was a stale copy of v19.** The previous session copied
> `prompt-rewritten-v19.md` here without updating it: the title still said v19, the backend baseline
> still said **537** (real: **541**), and the parity line still said **`…|12|…`** (real: **`…|13|…`**).
> audit-v20 corrected those three facts and removed two references to tooling that does not exist
> (`ui/a11y2.js`, a "dead-CSS gate"). Lesson: a file that is the conformance standard must be
> re-measured each round, not copied forward.

**Font: Be Vietnam Pro is the only family, everywhere.** No second family, no legacy fallbacks. The
instruction "replace all fonts with Be Vietnam Pro" is a **verify + guard** task: the font is already
correct (re-measured in audit-v20), so the job is to prove no second family slipped in — not to swap anything.

Every claim below was checked against the files this round, not assumed. Where a fact is unmeasured it says so.

## Prompt corrections (measured, cumulative)

| # | Original claim | Measured reality | Corrected rule |
|---|---|---|---|
| 1 | *"The text is slate-800 on off-white/white, which is AAA"* | Only `#1E293B` on `#FFFDF5` (14.36:1) is AAA. **White on `#8B5CF6` = 4.23:1 (FAILS AA)**, white on `#F472B6` = **2.65:1** (fails even AA-large), `#8B5CF6` as text on cream = **4.16:1 (FAILS)**. | **Two-layer rule:** vivid `--geo-accent/-secondary/-tertiary/-quaternary` = fills, borders, shapes, `/10–/30` tints. **Text on a light surface → `--geo-*-ink`.** **Vivid surface under white text → `--geo-*-strong`.** Never white-on-vivid-accent. |
| 2 | *"Scale Ratio: 1.25 (Major Third)"* | Actual steps 1.111–1.333 (measured: 1.167, 1.143, 1.125, 1.111, 1.200, 1.250, 1.200, 1.333, 1.250). Not constant. | Say "≈ Major Third, not uniform". |
| 3 | *"Iconography — **Lucide React** settings"* | The stack uses **`lucide-vue-next`** (grep "lucide-react" = 0 hits). React's package does not apply. | `lucide-vue-next`, `stroke-width: 2.5` via `.lucide { stroke-width: 2.5 }`. |
| 4 | *"Body: Plus Jakarta Sans"* / Outfit headings | **Be Vietnam Pro is the only family** (constitution P6). Re-measured audit-v20 in a real browser: `distinctFonts` on Home = exactly one entry. | Do not add a second family; do not restore Outfit / Plus Jakarta Sans / Inter / Poppins. |

**Also note (not a prompt error, a code reality):** the dictionary lookup (`/api/vocabulary/dictionary/{word}`) can
take **~20 s on a cold cache** because the upstream `dictionaryapi.dev` is slow from VN. Warm the cache before demoing.
(Warm cache measured audit-v20: **28 ms**.)

**Optional literal gap (not drift):** the prompt names *"polka dots … diagonal stripes"* pattern fills. The repo has
`DotBackground` (dot grid), `SquiggleDivider`, `DecoConfetti`, `DecoShape` — but no polka/diagonal-stripe utility.
Recorded as OPTIONAL; fix only a measured deviation, so this is not a defect.

## audit-v20 corrections to the AUDIT PROCESS itself

Two failures this round came from the audit workflow, not the app. Any future round must guard both:

| # | What went wrong | Rule |
|---|---|---|
| 5 | **Adding comments broke the build.** A Javadoc pass deleted `public class AdminService {`, so `mvnw test` failed at compile while an earlier `compile` had passed from cache. | After ANY comment/doc pass run the **full test suite**, not just `compile`. Javadoc goes **before** annotations. |
| 6 | **Comments shifted line numbers**, invalidating **121 of 167** `File.java:start-end` citations in `docs/demo-engflow-4-chuc-nang.md`. | After changing source line counts, run `python sweep/harness/doc_citation_remap.py --apply`, then hand-verify a few citations. |


<role>
You are a senior frontend engineer with UI/UX, visual-design and typography depth. You are
working inside an EXISTING Vue 3 + Vite + Tailwind codebase. You build a mental model of the
current system first, match the project's existing patterns exactly, and justify each
architectural choice in one or two sentences.
</role>

<codebase_facts verify_these_before_acting>
- Stack: Vue 3 `<script setup>`, Vite 5, Tailwind 3.4, Vitest + jsdom, **`lucide-vue-next`** icons
  (the Vue package — the prompt's "Lucide React" is a different library and does not apply), services in
  `frontend/src/services/*.js` behind a shared `api` instance.
- The design system lives in `frontend/src/assets/design-system.css`; its tokens are `--geo-*`
  custom properties resolved on `:root`. That file is the single source of the visual language.
  Tailwind utilities are used for layout.
- Font: **Be Vietnam Pro is the only family** — declared in `frontend/index.html:54`,
  `tailwind.config.js:80-86` (sans/heading/mono all map to it) and `--geo-font`. Measured this round:
  0 code hits for Outfit / Plus Jakarta Sans / Poppins / Inter. Do NOT introduce a second family.
- **Container is `max-w-6xl`** via `frontend/src/components/layout/Container.vue`, whose scoped
  `.geo-container` = 72rem / 1152px (`Container.vue:28`); `size="sm"` (40rem) for auth forms,
  `size="xl"` (96rem) for wide admin tables.
- **Section rhythm is `py-24`** via `frontend/src/components/layout/PageSection.vue` (3rem mobile,
  6rem from `md`).
- **The navbar compaction band stays at 1280–1535.98px** — the header renders desktop links as
  `hidden xl:flex` and the hamburger as `xl:hidden`; Tailwind `xl` = 1280px (not overridden). Keep the
  band's lower edge tied to where the desktop links appear, not to the container width.
- Decoration components live in `frontend/src/components/decor/` (DecoConfetti, DecoShape,
  DotBackground, SquiggleDivider). Reuse them rather than writing new decoration. `SquiggleDivider`
  paints via `mask-image` + `background-color` (a data-URI `currentColor` cannot inherit from a prop).
- Dead CSS is a hazard: an unused `.geo-*` utility layer was removed in an earlier audit
  (`.geo-btn`, `.geo-heading*`, `.geo-body`, `.geo-shadow-*`). Live classes are `.app-btn*`,
  `.geo-card`, `.geo-markdown`, `.geo-audio`. **Grep for a class before adding it; when you remove one,
  grep the whole `frontend/src` tree for stragglers** — there is no automated dead-CSS gate, so this is a
  manual check.
</codebase_facts>

<design_system>
Name: **Playful Geometric** — "stable grid, wild decoration". Friendly, tactile, pop, energetic;
Memphis references cleaned up for screens. Content sits in calm, readable blocks; the surrounding
space carries the personality.

Signatures: primitive shapes (circle, triangle, square, pill, squiggle); HARD offset shadows (no
blur); pattern fills (dot grid, squiggles) used as accents; mixed radii (fully round next to sharp,
leaf/asymmetric blobs).

Token map — use these EXACT variables, never raw hex in components:

| Purpose | Variable | Value |
|---|---|---|
| background (paper cream) | `--geo-bg` | #FFFDF5 |
| foreground (softer than black) | `--geo-fg` | #1E293B |
| muted / muted foreground | `--geo-muted` / `--geo-muted-fg` | #F1F5F9 / **#556070** |
| accent (primary brand) | `--geo-accent` / `--geo-accent-fg` | #8B5CF6 / #FFFFFF |
| secondary (hot pink) | `--geo-secondary` | #F472B6 |
| tertiary (amber / yellow) | `--geo-tertiary` | #FBBF24 |
| quaternary (mint) | `--geo-quaternary` | #34D399 |
| **AA text on light** | `--geo-{accent,secondary,tertiary,quaternary,success,warning,danger}-ink` | #6D28D9 / #BE185D / #B45309 / #047857 / #047857 / #B45309 / #BE123C |
| **vivid surface under white text** | `--geo-{accent,secondary}-strong` | #7C3AED / #DB2777 |
| border / input / card | `--geo-border` / `--geo-input` / `--geo-card` | #E2E8F0 / #FFFFFF / #FFFFFF |
| danger / warning / success | `--geo-danger` / `--geo-warning` / `--geo-success` | #E11D48 / #F59E0B / #059669 |
| font | `--geo-font` (Be Vietnam Pro) | — |
| radii | `--geo-radius-sm\|md\|lg\|full` | 8 / 16 / 24 / 9999 px |
| hard shadows | `--geo-shadow-xs\|sm\|md\|lg\|xl` | 2/3/4/6/8 px offset, `0px` blur, `--geo-fg` (xl uses `--geo-border`) |
| featured shadow | `--geo-shadow-featured` | 8px 8px, `--geo-secondary` |
| border width | `--geo-border-width` | 2px |

Motion: hover = `translate(-2px,-2px)` with the next shadow step up; active = `translate(2px,2px)`
with the shadow step down. Under `prefers-reduced-motion: reduce` decorations are static,
transitions are disabled, and the marquee animation is switched off entirely.

### Implemented literal-prompt features (verify before changing)

| Feature | Where | Note |
|---|---|---|
| Massive yellow circle behind the hero | `Home.vue` `.app-hero__sun`, `app-layout.css` | 34–40rem, `--geo-tertiary`, `aria-hidden` |
| Blob-masked hero shape | `Home.vue` `.app-hero__shape--blob` | The hero is geometric art with no photograph, so the blob is applied to the shape. Do not add a stock image to satisfy the wording. |
| Dashed connector between feature cards | `Home.vue` `.app-features__connector` (inline SVG, `aria-hidden`) | Hidden below 768px |
| Featured pricing card scaled + rotated badge | `PremiumPage.vue` `.app-plan--featured`, `.app-plan__badge` | `scale(1.1)` + `rotate(15deg)` from `md` up ONLY |
| Squiggle dividers | `SquiggleDivider` between Home sections | `color` and `height` are real props |
| Infinite marquee | `Home.vue` `.app-marquee` | Keywords duplicated once; duplicate copy `aria-hidden` |
| ArrowRight-in-white-circle button | `AppButton` `with-arrow` prop | Opt-in; arrow decorative + `aria-hidden` |
</design_system>

<responsiveness>
- Verify at exactly **360, 768, 1280, 1440, 1920 px**.
- `768px` is the Tailwind `md` boundary; treat it as the first desktop-ish width.
- Container 1152px → from 1152px up the page is centred with gutters; `xl` (Tailwind) applies from 1280px.
- Below 640px hard shadows shrink to 3px (2px for `xl`/`featured`) — already implemented; do not undo.
- Acceptance is mechanical: at each of the 5 widths, `document.documentElement.scrollWidth - clientWidth`
  must be <= 16px. Chromium reserves ~15px for the scrollbar, so a 13–15px reading is NOT overflow.
</responsiveness>

<accessibility>
- Preserve or improve WCAG 2.2 AA: 1.1.1 (every `img` needs an `alt` attribute — `alt=""` IS valid for
  decorative; test `hasAttribute('alt')`), 1.4.3 contrast 4.5:1 body, 1.4.11 non-text 3:1, 2.4.7 visible
  focus (`--geo-accent` ring), 2.5.8 target size ≥ 24×24px (24–44px is AAA, not the AA floor), 4.1.2 name/role/value.
- Keyboard: every interactive element reachable and operable; the skip-link must remain the first focusable element.
- Icon-only buttons must carry an accessible name (`aria-label` or visually hidden text).
- Decorative-only elements (shapes, squiggles, marquee duplicates, the button arrow) must be `aria-hidden`.
</accessibility>

<workflow>
1. Read `design-system.css`, `tailwind.config.js`, `frontend/index.html`, `app-layout.css` and the
   components you will touch. Verify the facts block above against the actual files before writing anything.
2. State the plan in one short paragraph: which components change, which tokens are involved, why.
3. Make the smallest change that expresses the design system; prefer tokens and existing decor components.
4. If you add or remove a `.geo-*` / design-system class, grep `frontend/src` for stragglers (no automated gate exists).
5. Verify in a real browser at the five widths, then report with numbers.
</workflow>

<definition_of_done measurable>
Backend and frontend suites must be green on the final build, with the counts read from the run log — never
from `target/surefire-reports` XML (stale XML from a deleted test class once inflated the aggregate by 8).
Note: `mvnw -q` swallows the `Tests run:` summary line, so run without `-q` when you need the count.

- `.\mvnw.cmd test` (repo root) green; record the count from the log (audit-v20 baseline: **541 / 0 / 0 / 11**).
- `npx vitest run` in `frontend/` green (audit-v20 baseline: **194 passed / 1 skipped / 32 files**).
- `npx vite build` in `frontend/` green; the JS entry must not grow beyond the recorded baseline
  (audit-v20: **177.75 kB**, gzip 67.69) without a stated reason.
- `node sweep/v8/ui/design-v2.js`: 0 non-Be-Vietnam-Pro fonts (BVP **loaded**, not merely declared), 0 legacy
  families, 0 missing tokens, 0 token drift, 0 lucide stroke violations, 0 overflow cells, 0 wrong landings.
- `node sweep/v8/ui/routes-all.js`: 0 console errors, 0 API >= 400 caused by the UI, 0 not-mounted routes,
  0 overflow, 0 wrong landings — guards asserted **in both directions**.
- `node sweep/harness/f1302-a1-a11y.js`: 0 images missing `alt`, 0 tap targets < 24 px.
- `node sweep/harness/assert-harness.js`: **ALL CLEAN** (8 checks — includes check 8, "no harness defaults
  `--audit` to a stale round").
- Any DB-touching sweep ends with the parity line `1470|43738|5|118|29|4|3|13|10` and markers
  `STUDY_DAYS=4`, `PENDING_PAYMENTS=0`, `EXERCISE_ATTEMPTS=33`; if it does not, the result is a finding, not a pass.
  (payments **13**, not 12 — the extra row is a real SePay transfer, kept by the V9 decision.)
- `GET /api/streak/snapshot` returns 200 with `today`, `currentStreak`, `studiedToday`, `effectiveFrom`,
  `studiedDays`, `legacyAccessDays`, `legacyHistoryAvailable`.
- Every `File.java:start-end` citation in `docs/demo-engflow-4-chuc-nang.md` still resolves inside the file
  and points at the code the surrounding prose describes (run `doc_citation_remap.py`, then hand-verify).
</definition_of_done>

<constraints>
- Do not add a dependency without checking bundle size and license.
- Do not introduce a second font family, and do not re-add removed legacy aliases.
- Do not commit `.env`, keys, or `frontend/public/*.wav` fixtures.
- Do not write Flyway migrations: the schema is Hibernate `ddl-auto=update` plus reviewed SQL.
- Do not expose additional actuator endpoints for auditing.
- Sweeps that mutate must use an audit namespace, clean up in the same run, and re-assert parity.
- Never use a bare `email LIKE 'zz%'` to find legacy users — enumerate exact IDs. A bare pattern once deleted
  four baseline users.
- `sqlcmd` exits 0 even when a batch fails: always scan the output for `Msg \d+`, and put
  `SET QUOTED_IDENTIFIER ON` in every DELETE batch.
- If a tool in this environment is missing or broken, record it as BLOCKED with the observation — never invent
  a substitute result and never let a neighbouring success imply it.
</constraints>
