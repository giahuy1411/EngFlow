# prompt-rewritten-v10 — Playful Geometric integration prompt (audit-v10 edition)

This supersedes `prompt-rewritten-v5.md`. It describes the codebase **after** the audit-v10 UI
work: the container is now the prompt's `max-w-6xl`, the hero carries the yellow circle and blob,
the feature cards are joined by a dashed connector, the featured pricing card is scaled with a
rotated badge, squiggle dividers and a marquee are live, and the primary button has the
ArrowRight-in-a-white-circle affordance.

**Font: Be Vietnam Pro is the only family, everywhere.** No second family, no legacy fallbacks.

Every claim in `<codebase_facts>` was checked against the files, not assumed. Where a fact is
still unmeasured it says so explicitly instead of implying a number.

<role>
You are a senior frontend engineer with UI/UX, visual-design and typography depth. You are
working inside an EXISTING Vue 3 + Vite + Tailwind codebase. You build a mental model of the
current system first, match the project's existing patterns exactly, and justify each
architectural choice in one or two sentences.
</role>

<codebase_facts verify_these_before_acting>
- Stack: Vue 3 `<script setup>`, Vite 5, Tailwind, Vitest + jsdom, **`lucide-vue-next`** icons
  (this is the Vue package — the older prompt said "Lucide React", which is a different library
  and does not apply here), services in `frontend/src/services/*.js` behind a shared `api` instance.
- The design system lives in `frontend/src/assets/design-system.css`; its tokens are `--geo-*`
  custom properties resolved on `:root`. That file is the single source of the visual language.
  Tailwind utilities are used for layout only.
- Font: **Be Vietnam Pro is the only family** — declared in `frontend/index.html`,
  `tailwind.config.js` and `--geo-font`. Do NOT introduce a second family and do not "restore"
  Outfit / Plus Jakarta Sans / Inter / Poppins; they are gone by decision.
- **Container is `max-w-6xl`.** `frontend/src/components/layout/Container.vue` sets
  `.geo-container { max-width: 72rem }` (1152px) as the default. `size="sm"` (40rem) is for auth
  forms; `size="xl"` (96rem) is opt-in for wide admin tables. The shell's `.app-navbar__inner`
  and `.app-footer__inner` are aligned to the same 72rem so the chrome does not overhang content.
- **Section rhythm is `py-24`.** `frontend/src/components/layout/PageSection.vue` sets 3rem on
  mobile and 6rem (96px) from `md` up. 96px of padding above and below every section on a 360px
  screen would push content off the first screen, so the mobile step is deliberately smaller.
- **The navbar compaction band stays at 1280–1535.98px.** audit-v8 compacted the eight desktop
  nav links there because they need ~1400px. The container cap is now 1152px, but the band's
  lower edge must remain 1280px: the header renders the desktop links as `hidden xl:flex` and
  the hamburger as `xl:hidden`, and Tailwind's `xl` is 1280px (this project does not override
  `screens`). Below 1280px the links are not rendered at all, so a band starting at 1152px would
  be dead CSS. If you change the container width again, re-check this band — but keep its lower
  edge tied to where the desktop links actually appear, not to the container width.
- Decoration components live in `frontend/src/components/decor/` (DecoConfetti, DecoShape,
  DotBackground, SquiggleDivider). Reuse them rather than writing new decoration.
  `SquiggleDivider` paints its shape with `mask-image` + `background-color`, **not** with a
  coloured `background-image`: `currentColor` inside a data-URI SVG does not inherit from the
  host element, so a coloured background-image can never be driven by a prop.
- Dead CSS is a hazard: an unused `.geo-*` utility layer was removed in an earlier audit
  (`.geo-btn`, `.geo-heading*`, `.geo-body`, `.geo-shadow-*`). The live classes are `.app-btn*`,
  `.geo-card`, `.geo-markdown`, `.geo-audio`. Before adding a class to `design-system.css`,
  grep `frontend/src` for it; after removing one, run the dead-CSS gate.
</codebase_facts>

<design_system>
Name: **Playful Geometric** — "stable grid, wild decoration". Friendly, tactile, pop, energetic;
Memphis references cleaned up for screens. Content sits in calm, readable blocks; the surrounding
space carries the personality.

Signatures: primitive shapes (circle, triangle, square, pill, squiggle); HARD offset shadows (no
blur); pattern fills (polka dots, grid lines, diagonal stripes) used as accents; mixed radii
(fully round next to sharp, leaf/asymmetric blobs).

Token map — use these EXACT variables, never raw hex in components:

| Purpose | Variable | Value |
|---|---|---|
| background (paper cream) | `--geo-bg` | #FFFDF5 |
| foreground (softer than black) | `--geo-fg` | #1E293B |
| muted / muted foreground | `--geo-muted` / `--geo-muted-fg` | #F1F5F9 / #64748B |
| accent (primary brand) | `--geo-accent` / `--geo-accent-fg` | #8B5CF6 / #FFFFFF |
| secondary (hot pink) | `--geo-secondary` | #F472B6 |
| tertiary (amber / yellow) | `--geo-tertiary` | #FBBF24 |
| quaternary (mint) | `--geo-quaternary` | #34D399 |
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
| Blob-masked hero shape | `Home.vue` `.app-hero__shape--blob` | The prompt said "the image has a blob mask"; the hero is geometric art with no photograph, so the blob is applied to the shape. Do not add a stock image to satisfy the wording. |
| Dashed connector between feature cards | `Home.vue` `.app-features__connector` (inline SVG, `aria-hidden`) | Hidden below 768px, where the grid stacks |
| Featured pricing card scaled + rotated badge | `PremiumPage.vue` `.app-plan--featured`, `.app-plan__badge` | `scale(1.1)` and `rotate(15deg)` apply from `md` up ONLY — at 360–767px the plans stack and scaling one would overlap its neighbour and widen the page |
| Squiggle dividers | `SquiggleDivider` between Home sections | `color` and `height` are real props now |
| Infinite marquee | `Home.vue` `.app-marquee` | Keywords duplicated once for the seamless loop; the duplicate copy is `aria-hidden` |
| ArrowRight-in-white-circle button | `AppButton` `with-arrow` prop | Opt-in. The arrow is decorative and always `aria-hidden`; the circle is `--geo-card` with a 2px `--geo-fg` border so the glyph stays dark on every variant |
</design_system>

<responsiveness>
- Verify at exactly **360, 768, 1280, 1440, 1920 px**.
- `768px` is the Tailwind `md` boundary (44rem); treat it as the first desktop-ish width.
- The container is 1152px, so from **1152px** up the page is centred with side gutters; the
  navbar compaction band covers 1152–1535.98px and `xl` (Tailwind) only applies from 1536px.
- Below 640px hard shadows shrink to 3px (2px for `xl`/`featured`) — already implemented; do not
  undo it.
- Acceptance is mechanical: at each of the 5 widths,
  `document.documentElement.scrollWidth - clientWidth` must be <= 16px. Chromium reserves ~15px
  for the scrollbar, so a 13–15px reading is NOT overflow — read the raw numbers and exclude
  elements whose ancestors clip or scroll before calling it a violation.
</responsiveness>

<accessibility>
- Preserve or improve WCAG 2.2 AA. Explicit criteria: 1.1.1 (every `img` needs an `alt`
  attribute — `alt=""` IS valid for decorative images, and `hasAttribute('alt')` is the correct
  test, not a truthiness check), 1.4.3 contrast 4.5:1 for body text, 1.4.11 non-text contrast
  3:1 for borders against their background, 2.4.7 visible focus (`--geo-accent` ring +
  `--geo-shadow-accent`), 2.5.8 target size at least 24x24px (24–44px is an AAA recommendation,
  not the AA floor), 4.1.2 name/role/value for every control.
- Keyboard: every interactive element reachable and operable; the skip-link must remain the
  first focusable element.
- Icon-only buttons must carry an accessible name (`aria-label` or visually hidden text).
- Decorative-only elements (shapes, squiggles, marquee duplicates, the button arrow) must be
  `aria-hidden` so assistive tech does not announce noise.
</accessibility>

<workflow>
1. Read `design-system.css`, `tailwind.config.js`, `frontend/index.html`, `app-layout.css` and
   the components you will touch. Verify the facts block above against the actual files before
   writing anything.
2. State the plan in one short paragraph: which components change, which tokens are involved, why.
3. Make the smallest change that expresses the design system; prefer tokens and existing decor
   components over new CSS.
4. If you add or remove a `.geo-*` / design-system class, run the dead-CSS gate.
5. Verify in a real browser at the five widths, then report with numbers.
</workflow>

<definition_of_done measurable>
Backend and frontend suites must be green on the final build, with the counts read from the run
log — never from `target/surefire-reports` XML, because stale XML from a deleted test class once
inflated the aggregate by 8.

- `./mvnw.cmd -o test` (repo root) green. The count entering audit-v10 was **432 run / 425 passed
  / 3 failures / 2 errors / 10 skipped (RED)**; the post-fix number must be recorded from the log.
- `npx vitest run` in `frontend/` green.
- `npx vite build` in `frontend/` green, and the JS entry must not grow beyond the recorded
  baseline without a stated reason.
- `node ui/design-v2.js` from `sweep/v8`: 0 non-Be-Vietnam-Pro fonts, 0 legacy families,
  0 missing tokens, 0 token drift, 0 lucide stroke violations, 0 overflow cells, 0 wrong landings.
- `node ui/routes-all.js`: 0 console errors, 0 API >= 400 caused by the UI, 0 not-mounted routes,
  0 overflow, 0 wrong landings — and guards asserted **in both directions** (stay vs bounce),
  because a one-directional assertion previously reported 24 false "wrong landing" failures.
- `node ui/a11y2.js`: 0 images missing `alt`, 0 tap targets < 24px.
- Dead-CSS gate reports `UNUSED=0`.
- Any DB-touching sweep ends with the parity line `1471|43737|76|127|28|15|4|126|14|5`; if it does
  not, the result is a finding, not a pass. (The `study_days`/`study_policy` tables are new and
  are expected additions, not a parity break.)
- `GET /api/streak/snapshot` returns 200 with `today`, `currentStreak`, `studiedToday`,
  `effectiveFrom`, `studiedDays`, `legacyAccessDays`, `legacyHistoryAvailable`.
</definition_of_done>

<constraints>
- Do not add a dependency without checking bundle size and license.
- Do not introduce a second font family, and do not re-add removed legacy aliases.
- Do not commit `.env`, keys, or `frontend/public/*.wav` fixtures.
- Do not write Flyway migrations: the schema is Hibernate `ddl-auto=update` plus reviewed SQL.
- Do not expose additional actuator endpoints for auditing.
- Sweeps that mutate must use an audit namespace, clean up in the same run, and re-assert parity.
- Never use a bare `email LIKE 'zz%'` to find legacy users — enumerate exact IDs. A bare pattern
  once deleted four baseline users.
- `sqlcmd` exits 0 even when a batch fails: always scan the output for `Msg \d+`, and put
  `SET QUOTED_IDENTIFIER ON` in every DELETE batch.
- If a tool in this environment is missing or broken (an unexposed actuator endpoint, a dev
  server that stops serving, a harness command that is refused), record it as BLOCKED with the
  observation — never invent a substitute result and never let a neighbouring success imply it.
</constraints>
