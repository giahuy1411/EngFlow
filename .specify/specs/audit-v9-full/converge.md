# Converge — audit-v9-full

> Historical 2026-09-17 evidence. Current audit remains OPEN. Do not use the
> historical PASS tables as current coverage or completion proof. See
> reverify-status.md for 2026-09-18 evidence, interrupted full-suite execution,
> two completed independent reviews, and unresolved acceptance gates.

Assessment of the current codebase against the audit spec (a full-stack re-audit with root-cause fixes and a
performance pass), plus the gaps the audit APPENDS rather than hides.

## Spec vs shipped state

| Spec requirement | Shipped state | Evidence |
|---|---|---|
| Re-derive the endpoint inventory and prove coverage | 144 mappings / 26 controllers from source; 144/144 covered by asserted probes | `evidence/endpoint-inventory.json`, `evidence/coverage-round2.json` |
| Re-derive the route map and prove guard behaviour in both directions | 39 routes classified; 228 browser visits, 0 wrong landings | `route-map.md`, `sweep/v8/r2-routes-all.log` |
| Sweep every endpoint with all three roles | p1–p5 + p6 + p8 + p10–p15 + p24; 0 probe failures | `sweep/v8/r2-p*.log` |
| Probe the core features closely | auth/lockout, streak (against the DB), search/sort, CRUD lifecycle, AI via real Ollama/Whisper | `p4a`, `p5`, `p3a`, `p3b`, `p11` |
| Verify the UI in a real browser | Chromium: routes, design (5 viewports), a11y, alt, interactions, video, 8 end-to-end journeys | `ui/*.js`, `p24_journeys.js` |
| Audit the DB and reconcile it with the app | 24 tables, 0 orphans, 0 disabled FKs, backlog measured | `db-audit.md`, `sweep/v8/v9b-db-audit.txt` |
| Fix root causes, each with a regression test | F105, F106, F108 fixed with colocated tests; F107/F109/F110/F111 fixed in the harness/process layer | `findings.json`, `sweep/v8/f106-*.log`, `f108-test2.log`, `f105-green.log` |
| Optimise performance with before/after numbers | F108: 153 → 29 ms median, 1054 → 182 avg logical reads; everything else measured and deliberately left alone | `performance.md` |
| Clean up dead design-system CSS | ~50 → 4 selectors, 0 unused; CSS 92.42 → 85.22 kB | `design-audit.md`, `v9_deadcss_check.py` |
| Leave the DB at parity | `1471\|43737\|76\|127\|28\|15\|4\|126\|14\|5`, `PARITY_OK=True` after both rounds | `v9_cleanup_sweep.py` |
| Emit the audit artifacts | findings, tasks, converge, analyze, issues, workflow log, reports, prompt gap/rewrite | this directory |

## Gaps appended (not silently dropped)

1. **`/actuator/mappings` is not exposed** (404). The plan called for dumping the live registry; that is impossible
   without opening an actuator endpoint purely for auditing, which was rejected. Substitution: source-derived inventory
   + a live coverage gate. Marked `BLOCKED` in `findings.json` (F112).
2. **No dedicated adversarial re-derivation script (`p25_adversarial_rederive_v9.js`).** The two existing adversarial
   suites were re-run on the final build (p14 0 fail / p15 21/21) but they replay fixtures rather than regenerating them
   from the DTO source. Status `PARTIAL` in `tasks.md`.
3. **No read-only subagents.** No subagent runner exists in this session, so the cross-check was done with two
   independent channels (browser harness vs SQL runtime statistics) that have to agree. Status `BLOCKED`.
4. **`spec.md` / `clarify.md` / `checklist.md` / `constitution.md` were not re-authored for v9.** The audit reuses the
   project constitution and the v8 clarify/checklist; v9 ships plan-of-record, tasks, findings and reports instead.
   Status `PARTIAL`.
5. **Backlog left unmutated on purpose**: 4,848 empty `correct_answer`, 6 empty `lessons.content`, 3 duplicate vocab
   words, 4 `exercises_bak_v5*` tables, 8 legacy `zz*` users, 9 LISTENING exercises without `audio_url`.
   Measured in `db-audit.md`; drafted as issues in `issues.md`.
6. **Postgres-grade re-verification of the timezone convention** was not redone: the naive-VN convention was verified in
   audit-v7 and re-confirmed here only where it mattered (block [8] of the DB audit: 0 rows "created in the future").

## Does the app converge?

Yes for the audited surface: 393/393 backend tests, 90/90 frontend tests, 0 failed probes across ~470 API requests,
0 console errors / 0 overflow / 0 wrong landings across 228 browser visits, DB parity at baseline, one measured
performance win with a regression test, and the dead design-system CSS removed. The six gaps above are the honest
boundary of that statement.
