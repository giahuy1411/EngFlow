# audit-v19-full — Phase 3: UI/UX automated sweep

**Ngày:** 2026-09-27 · Nguồn: `ui-sweep.js`, `design-v2.js`, `routes-all.js`, `cls-probe.js`, `danger-tint.js`, `focused-probe.js`, `f1302-a1-a11y.js`.

## T3.1 ui-sweep — route × role × viewport

```
=== TOTALS === {"contrastFails":0,"missingAlt":0,"smallTargets":0,"noName":0,
                "consoleErrors":0,"apiErrors":0,"pageErrors":0,"guardFails":0,"overflowRoutes":0}
responsive: 35 cells, 0 overflowing
assertClean: parity=1470|43738|5|118|29|4|3|12|10 ... -> CLEAN
```
→ **0 mọi loại**, gồm **smallTargets=0** (sau khi sửa harness W2 — xem `w2-tap-targets.md`).
Trước fix harness: 115 (naive, list bị cắt). Sau fix: **0** (đúng WCAG 2.5.8, mutation-test PASS).

## T3.2 design-v2 — conformance
```
font loaded (400/700/900): loaded=true bold=true black=true faces=18
non-BVP computed fonts: 0 · legacy-family hits: 0 · routes w/ overflow: 0/60
tokens missing: 0 (660 checks) · token value drift: 0
lucide icons checked: 655, stroke != 2.5: 0 · LANDED ON WRONG PAGE: 0
```
→ **Be Vietnam Pro LOADED thật**, 0 drift, 0 overflow.

## T3.4 routes-all — guard 2 chiều
```
console errors: 0 · API >=400: 0 · overflow routes: 0 · not mounted: 0 · wrong landing: 0
```

## T3.5 CLS
| Route | CLS median |
|---|---|
| `/` | 0.00069 |
| `/lessons` | 0.00095 |
| `/login` | 0.00003 |

## T3.6 danger-tint + focused + a11y
- danger-tint: error-state `text-danger-ink` = **5.28:1** (PASS AA).
- focused-probe: `recheck /profile /search /leaderboard /videos/1 -> []`; video word-chip **rawSmallUnder24=12**
  (inline exception → không phải vi phạm; note đã thêm).
- f1302-a1-a11y: `content a11y: 5/5 PASS`.

## T3.7 (W2) — đã sửa harness, xem `w2-tap-targets.md`.
