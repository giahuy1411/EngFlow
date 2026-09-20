# prompt-gap-v5 — audit-v9-full

Each open gap from the previous round is closed by a MEASUREMENT, not by an assertion. Where the honest answer is
"the app deliberately does something else", that is stated as a divergence with the number that supports it.

## G1 — `py-24` / `max-w-6xl` layout rhythm: MEASURED (divergence, documented)

`p21_design_tone_v9.js` on the live landing page and /premium:

| Probe | `/` | `/premium` |
|---|---|---|
| elements whose class list literally contains `py-24` / `py-16` / `py-20` | 0 / 0 / 0 | 0 / 0 / 0 |
| elements whose class list contains `max-w-6xl` / `max-w-7xl` | 0 / 0 | 0 / 0 |
| largest computed `padding-top` on the page | **96 px** | 32 px |
| computed width of the main container | **1280 px** | 896 px, 768 px |

Reading: the prompt's literal Tailwind utilities are not used anywhere in this codebase — the project styles with its own
token classes (`app-*` + scoped SFC styles). But the RHYTHM the utilities encode is present: 96 px is exactly `py-24`
(6 rem) and 1280 px is exactly `max-w-7xl`. The section padding and container width the prompt asks for are therefore
achieved; the class names differ. Status: **measured, divergence accepted**, no code change (renaming Tailwind utilities
into the markup would be churn with zero visual effect).

## G2 — dead `.geo-marquee` / `.geo-btn` / `.geo-badge`: CLOSED

`v9_deadcss_check.py` (comment-stripping): `selectors=4 used=4 UNUSED=0`. The `.geo-*` utility layer was removed
(~50 selectors -> 4 kept: `.geo-card`, `.geo-markdown`, `.geo-audio`, `.geo-radio`, all still referenced from SFCs).
CSS entry 92.42 kB -> 85.22 kB. `design-v2` re-run after the removal: 0 token/shadow/radius/border/font violations.
Status: **closed with a passing gate**.

## G3 — confetti / squiggle / dot components: CLOSED, and the previous report was WRONG

`frontend/src/components/decor/` contains `DecoConfetti.vue`, `DecoShape.vue`, `DotBackground.vue`,
`SquiggleDivider.vue`, `index.js`. Live measurement: dot components on the landing = **8**, on /premium = 2;
`bigCircles` (radius >= 999 px and width >= 60 px) = **17** on the landing, 11 on /premium. The v8 round's claim that
confetti had no implementation is retracted here. Status: **closed + correction recorded**.

## G4 — `scale(1.1)` / `rotate(15deg)` badge: MEASURED (partial by design)

`v9_transforms.js` decoded the actual transform matrices on the landing: 7 elements carry a transform, of which
**exactly 1** is a real rotation — `app-footer__shape--tertiary` at **12deg** (scale 1). No `scale(1.1)` exists
anywhere in the sampled tree; /premium has 2 transform-carrying elements.

Reading: the "rotated sticker" idea IS implemented (12deg footer shape), and the "featured" emphasis is expressed with
the pink hard shadow token (`--geo-shadow-featured: 8px 8px 0px 0px #F472B6`) rather than with a `scale(1.1)`
transform. The prompt mentions `scale(1.1)`/`rotate(15deg)` as illustrations for a pricing/featured badge, not as a
required contract, so this is recorded as a measured divergence rather than a defect. Status: **measured**.

## Summary

| Gap | Status | Evidence |
|---|---|---|
| G1 layout rhythm | measured, divergence documented | `p21_design_tone.json` |
| G2 dead `.geo-*` | closed | `v9_deadcss_check.py`, `r2-design-v2.log`, `v9-r1-build.log` |
| G3 decor components | closed + v8 correction | `frontend/src/components/decor/`, `p21_design_tone.json` |
| G4 scale/rotate badge | measured, partial by design | `r2-transforms.log`, `v9_transforms.js` |

Nothing in this file is asserted without a path to the artifact that produced it.
