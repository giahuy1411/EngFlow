# audit-v9-full specification

Authored retrospectively 2026-09-18. These requirements do not prove the original pipeline ran in order.

## Objective

Run a full local backend/API/DB/UI audit of EngFlow, verify fixes with real evidence, preserve a two-round audit scope, and keep the Playful Geometric design-system verification separate from application hardening.

## Requirements

- Verify live infrastructure before accepting any sweep result.
- Derive the endpoint inventory from source and probe it over HTTP.
- Audit core features: auth, lessons/exercises, streaks, search/sort, CRUD, AI, payments, speaking.
- Audit database integrity, schema drift, performance, and residue without speculative mutation.
- Exercise real browser routes and accessibility behavior.
- Every app finding gets an ID, root cause, patch, regression test, and re-measurement.
- Every mutation test uses a run-scoped namespace, backup, cleanup manifest, parity check, and independent cleanup proof.
- Report PARTIAL/BLOCKED boundaries instead of claiming implied success.

## Success criteria

- Backend and frontend gates pass on the tested artifact.
- Functional coverage must include every mapping in a current source-derived inventory. Historical denominator 144 is not fixed. Require current run/image, hashed artifacts and passing named assertions; negative controls alone do not establish functional coverage.
- Browser route and design gates report zero wrong landings or console errors.
- Database parity returns to the documented baseline after fixtures are removed.
- Report states exactly which fixes are deployed, re-verified, deferred, or blocked.

## Boundaries

- Do not expose actuator mappings solely for audit.
- Do not mutate backlog data unless an approved manifest authorizes the exact row.
- Do not claim a dev-server workaround as a root-cause fix.
