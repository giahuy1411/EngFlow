# Security audit — audit-v9-full (2026-09-17)

Scope: authentication, authorization, the upload/`/api/resources/**` surface, rate limiting, and the
draft-content visibility contract. This is the same scope audit-v8 covered (F81–F94) plus the v9 deltas.

## Method

1. Derive the endpoint inventory from SOURCE (`v9_endpoint_inventory.py`: 144 unique mappings / 26 controllers).
2. Map each mapping to the SecurityConfig rule that actually claims it, in DECLARATION order (first match wins),
   not by specificity — the exact bug class audit-v7 F54 hit (`endpoint-map.md`).
3. Probe every mapping as anon / user / admin and assert BOTH directions (anon 401 on authenticated routes,
   user 403 on admin routes, and a positive control where the correct role must succeed).
4. Re-derive adversarial cases from the DTO contracts rather than replaying old fixtures.

## Result

- 144/144 mappings have at least one asserted 2xx-or-correct-denial probe: `evidence/coverage-round2.json`
  (`inventoryCount 144, coveredCount 144, uncoveredCount 0`, 472 probe requests from p1–p5 + p6 + p8 + p10–p15 + p24 + security).
- Round-2 suite: p1 124/124, p2 60/60, p3a 36/36, p3b 19/19, p4a 18/18, p4b 16/16, p4c 10/10, p4d 6/6, p4e 6/6,
  p5 95/95 probes + 60/60 asserts, p14 adversarial 0 fail, p15 deep adversarial 21/21, p6 alias surface 0 fail.
- Rate-limit buckets verified at their real routes (`;auth` 20, `;mail` 5, `;ai` 10, `;upload` 15, `;order` 10, `;global` 100).
  The sweep clears only its OWN `rate_limit:*` keys before tight-bucket probes; the harness fix is F109.

## Findings

| ID | Severity | Status | One-line |
|---|---|---|---|
| F105 | HIGH | FIXED | draft lesson (isPublished=false) answered grade/submit, leaking `correctAnswer`; now 404 for non-admin, 200 for admin preview |
| F107 | INFO | FIXED (false positive refuted) | the 'correctAnswer leaked' warning came from a substring test on a field that is always serialized as `null` for non-admins |
| F112 | INFO | BLOCKED | `/actuator/mappings` is not exposed (404), so the registry was derived from source instead — recorded, not worked around silently |

## Verified-as-designed (not defects)

- Guest `GET /api/lessons/{id}/exercises` returns `correctAnswer: null` (asserted by parsing the body, F107).
- Unpublished lessons/video lessons 404 for guest/student, admin preview allowed (F88/F89 contract re-asserted in p1/p2).
- `GET /api/vocabulary` list requires auth while `/search` + `/dictionary/*` are permitAll — 401 anonymously is correct.
- Upload extension/content-type handling still goes through `SafeUploadNames.extensionOf` + `contentTypeFor` + `forceDownload`;
  p4c/p4d/p4e re-exercised the multipart writers with real media and got 200/201, and the XSS regression suite is still green in the 393-test backend run.
- `POST /api/auth/login` lockout and the `;auth` bucket were re-verified live (p4a 18/18, p5 lockout asserts 60/60).

## Boundary (BLOCKED, stated rather than implied)

No independent third-party penetration test was performed. The security scan is limited to what the harness can prove:
role matrix, draft visibility, upload surface, error-shape leakage and rate limiting. Anything requiring a different
network position (e.g. an attacker on the Docker network, TLS interception) is out of scope and marked BLOCKED in `findings.json`.