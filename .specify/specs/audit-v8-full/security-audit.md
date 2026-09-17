# Security Audit — EngFlow

**Task:** comprehensive-audit-redesign Task 12 · **Date:** 2026-09-16
**Harnesses:**
`sweep/v8/security.js` → `security.txt` / `security.json` (authz matrix, leakage, token handling)
`sweep/v8/xss-prove.js` → `xss-prove.txt` / `xss-prove.json` (stored-XSS execution proof)
`sweep/v8/p8_bucket_coverage.js` → `p8-bucket-coverage.txt` (rate-limit bucket routing)
`frontend/src/utils/sanitize-a11y.js` + `markdown.js` (sanitizer config under test)

## Headline result

| Suite | Failures |
|---|---:|
| Admin surface × role matrix (10 surfaces × 3 roles) | **0** |
| Authenticated self-scoped surfaces | **0** |
| Public surfaces reachable | **0** |
| Token handling (4 malformed/absent cases) | **0** |
| Response-body leakage (10 patterns × 14 responses) | **0 matches** |
| Stored-XSS execution proof | **0 executions** |
| Secret patterns across 1 091 tracked files | **0 hits** |

## 1. Authorization matrix

Every admin surface was called with an admin token, a **regular user** token, and
**no** token. A regular user and an anonymous caller must both be rejected with
`401`/`403`; the admin must actually succeed.

| Method | Path | admin | user | anon |
|---|---|---|---|---|
| GET | `/api/admin/stats` | 200 | **403** | **401** |
| GET | `/api/admin/lessons?page=0&size=5` | 200 | **403** | **401** |
| GET | `/api/admin/lessons/445/structure` | 200 | **403** | **401** |
| GET | `/api/admin/exercises?page=0&size=5` | 200 | **403** | **401** |
| GET | `/api/admin/users?page=0&size=5` | 200 | **403** | **401** |
| POST | `/api/admin/lessons` | 400 (validation) | **403** | **401** |
| DELETE | `/api/admin/lessons/99999999` | 404 (absent) | **403** | **401** |
| GET | `/api/admin/exercises/ai/status` | 200 | **403** | **401** |
| GET | `/api/v1/admin/speaking-submissions?page=0&size=5` | 200 | **403** | **401** |
| GET | `/api/v1/admin/video-lessons` | 200 | **403** | **401** |

Note the **distinction between 403 and 401**: an authenticated non-admin gets
`403 Forbidden` (identity known, authority lacking) while an anonymous caller
gets `401 Unauthorized`. The app does not conflate the two, which is what makes
the frontend's logout-on-401 logic safe (see §6).

**Method-aware assertion.** A `GET` must return `200` for the admin — otherwise a
passing "role-gated" row could just be a URL that does not exist. Write methods
may legitimately answer `400` (validation) or `404` (absent target). An earlier
revision accepted *any* non-401/403 admin response, which let a wrong URL
(`/api/admin/dashboard/stats`, a real 404) pass as OK.

### Authenticated self-scoped surfaces

| Method | Path | user | anon |
|---|---|---|---|
| GET | `/api/dashboard/stats` | 200 | **401** |
| GET | `/api/auth/me` | 200 | **401** |

`/api/dashboard/stats` is **not** an admin endpoint: `DashboardService`
keys off `authentication.getName()` and returns only the caller's own points,
streak and progress. Returning `200` for a normal user is correct, and asserting
it as admin-only would have been a false finding. The admin dashboard calls
`/api/admin/stats` instead.

### Public surfaces (must remain reachable)

| Method | Path | anon |
|---|---|---|
| GET | `/api/v1/video-lessons` | 200 |
| GET | `/api/v1/video-lessons/1` | 200 |
| GET | `/api/vocabulary/search?keyword=work` | 200 |
| POST | `/api/auth/login` | 400 (bad credentials) |

These match `SecurityConfig`'s `permitAll` matchers. `POST /api/auth/login`
returning `400` for wrong credentials is correct — and importantly it is not
`401`, so a failed login does not trigger the frontend's session-expiry path.

## 2. Response-body leakage — 0 matches

10 patterns were tested against every response in §1 (14 responses × 3 roles):

`at com.datn.engflow` · `java.lang.*Exception` · `org.springframework.*Exception` ·
`SELECT … FROM` · `INSERT INTO` · `C:\Users\ASUS` · `/app/src/main` · `BOOT-INF` ·
`hibernate` · `stacktrace`

**0 matches.** Errors are returned as RFC 7807 ProblemDetail
(`{"detail","instance","status","title"}`) with a Vietnamese user-facing message
and no internal detail, which is the contract the frontend relies on.

## 3. Token handling

| Case | Expected | Observed |
|---|---|---|
| No `Authorization` header | reject | **401** |
| `not.a.jwt` (malformed) | reject | **401** |
| Valid JWT with tampered signature | reject | **401** |
| `Authorization: Bearer ` (empty) | reject | **401** |

No case was accepted. Signature tampering specifically does **not** degrade to a
"parse the payload anyway" path.

JWT TTL is **900 s** (`jwt.expiration=900000`). This is a deliberate short
lifetime, and it is why long browser sweeps must re-seed their token — a
152-visit sweep outlives the token, and the resulting 401s are the harness
exhausting its credential, not an authorization defect.

## 4. Stored XSS from untrusted AI output — proved non-executable

This is the highest-risk surface in the app: a **local LLM** generates
`meaning`, `exampleSentence` and other fields that are rendered with `v-html`,
and model output is untrusted input.

**The risk is real, not theoretical.** The model sometimes echoes a raw tag
back. Measured nondeterministically: 1 of 3 runs returned
`"exampleSentence":"The URL <svg/onload=alert(1)> is …"` for the input
`<svg/onload=alert(1)>`.

**Why it is nonetheless not exploitable** — two independent proofs:

**(a) The sanitizer allowlist neutralises every payload.** Every `v-html` call
site routes through `sanitizeText()` (`utils/markdown.js`) or `sanitizeHtml()`,
which use DOMPurify with an explicit `ALLOWED_TAGS` allowlist:
`b, strong, i, em, u, s, span, br, a, code, sub, sup, small, mark`
(and `ALLOWED_ATTR: href, title, target, rel`). `script`, `svg`, `img`, `iframe`
and all `on*` handlers are absent from the allowlist, so they are stripped.

Tested by importing the app's **actual** module and running its real config:

| Payload | `sanitizeText()` output | Dangerous? |
|---|---|---|
| `<img src=x onerror="…">` | `""` | no |
| `<svg/onload=…>` | `""` | no |
| `<script>…</script>` | `""` | no |
| `<iframe src="javascript:…">` | `""` | no |

**0 / 4 dangerous outputs.**

**(b) End-to-end execution proof in real Chromium.** Payloads were pushed
through the real write path — `POST /api/ai/enrich-word` →
`POST /api/ai/save-vocab` → rendered page — then 6 deck pages were opened and
inspected:

| Check | Result |
|---|---|
| Marker executed (`window.__p`) | **false** |
| Inline `on*` handler attributes in DOM | **0** |
| Pages opened | 6 |

**Verdict: PASS.** Stored AI output cannot execute.

### Two false-positive traps I had to correct

Both produced *wrong* answers before being fixed, and are recorded because the
difference between "substring present" and "exploitable" is the whole finding:

1. **Naive substring matching.** Searching for `onload=` matched
   `{"word":"svg/onload=alert(1)"}` — the model had **stripped the angle
   brackets**, so the value was inert plain text. Reported as a false positive.
2. **Over-correction.** After fixing (1), a genuine raw-tag echo was then scored
   as a hard failure — but a raw tag inside a JSON *string* is still inert; JSON
   does not escape `<`/`>`. This made the harness flaky (1-in-3 runs) for a
   non-defect.

The response-shape check in `security.js` §4 is therefore now explicitly
**informational** and does not increment the failure count; the authoritative
verdict belongs to `xss-prove.js`, which tests the sanitizer config *and* real
browser execution.

### The proof harness must clean up after itself

`xss-prove.js` writes real `vocabulary` rows — a stored-XSS proof has to store
something. Its cleanup used to be a separate manual step, so residue accumulated
and a later audit read **130** rows instead of the 127 baseline. The cleanup is
now part of the run and asserts **parity**, not exit status: the batch must
report `remaining = 0` and `vocabulary_total = 127`.

Two bugs were found by testing the cleanup instead of assuming it worked:
the run-start timestamp was computed at cleanup time (so the window began *after*
the writes and matched 0 rows while reporting success), and "no SQL error" was
being accepted as success. Both are fixed; details in `db-audit.md` §9.

**This matters for the security conclusion:** the XSS verdict itself was never in
doubt (the sanitizer allowlist and the browser execution check are independent of
the DB), but the audit's *data hygiene* claim was wrong until the harness was
corrected. The parity figures below are from after the fix.


## 5. Rate limiting — buckets match real routes

`RateLimitFilter` buckets, verified live by observing the Redis key each request
produces (`p8_bucket_coverage.js`):

| Surface | Expected bucket | Observed | Limit |
|---|---|---|---|
| `/api/ai/**` (control) | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/generate-async` | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/generate` | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/generate-all` | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/generate-batch` | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/validate` | `:ai` | `:ai` | 10/min |
| `admin/exercises/ai/backfill-answers` | `:ai` | `:ai` | 10/min |
| `speaking-prompts/ai-generate` | `:ai` | `:ai` | 10/min |
| `speaking-prompts/ai-generate-full` | `:ai` | `:ai` | 10/min |
| `video-prompts/ai-generate` | `:ai` | `:ai` | 10/min |
| `video-lessons/translate-transcript` | `:ai` | `:ai` | 10/min |
| `video-lessons/fetch-youtube` | `:ai` | `:ai` | 10/min |
| `video-attempts/ai-grade` | `:ai` | `:ai` | 10/min |
| `auth/avatar/upload` | `:upload` | `:upload` | 15/min |
| `video-lessons/upload` | `:upload` | `:upload` | 15/min |
| `/api/admin/audio-upload` (control) | `:upload` | `:upload` | 15/min |

**All 13 AI endpoints route to `:ai`; both upload endpoints route to
`:upload`.** This closes F91/F92: bucket prefixes previously drifted from the
real route table, so expensive AI and upload endpoints fell into the loose
`:global` bucket (100/min) instead of their intended 10–15/min.

Bucket key format: `rate_limit:<ip>:<bucket>`. Ceilings were confirmed by burst:
`:ai` first `429` at request **11**, `:upload` at **16** — matching the declared
limits exactly.

Full bucket set: `:auth` 20 · `:mail` 5 · `:global` 100 · `:ai` 10 ·
`:upload` 15 · `:order` 10 per minute per IP.

> The `status=404` values in the harness output are expected: the probe sends
> requests to non-existent ids purely to observe **which bucket key** the filter
> writes. It measures routing, not endpoint success.

## 6. Filter ordering — F90, closed as NOT A BUG

**Observation (real):** Spring Security's filter chain registers at order
`-100` (`SecurityProperties.DEFAULT_FILTER_ORDER`, verified with `javap` against
`spring-boot-security-4.0.6.jar`), so it runs **before** `RateLimitFilter`
(`@Order(1)`).

**Why it is not a bypass:** all 17 `permitAll` surfaces were measured
individually — **0** reached the application without passing through a bucket.
Requests rejected by security perform no business work, so the ordering cannot be
used to consume AI/upload capacity or reach a service unthrottled. Changing the
`@Order` was rejected as a high-risk change to the security chain with **no
measured benefit**.

**Disposition: CLOSED — not a bug.** Deliberately *not* "fixed", because
"fixing" it would alter security-chain ordering for an unmeasured, unproven gain.

## 7. Upload → `/api/resources/**` (stored XSS surface)

This route is `permitAll` and same-origin with the SPA (Vite proxies `/api`), so
any file with a browser-renderable extension (`.html`, `.svg`, `.js`) would be
stored XSS capable of reading the JWT from `localStorage`.

Controls verified present in code:

| Control | Location | Status |
|---|---|---|
| Extension validation on write | `SafeUploadNames.extensionOf` | present |
| Content-Type derivation on serve | `SafeUploadNames.contentTypeFor` (`LessonStructureController.java:105`) | present |
| Forced download for dangerous types | `SafeUploadNames.forceDownload` (`:111`) | present |
| Regression test | `AuditV8UploadXssTest` (9 tests) | present |

`AGENTS.md` records this was measured end-to-end in real Chromium *before* the
fix. New writers or routes on this surface must go through both helper functions.

## 8. Secrets

| Check | Result |
|---|---|
| `.env` tracked by git | **no** — `.gitignore` has `.env` and `.env.*` |
| Secret patterns across **1 091 tracked files** | **0 hits** |
| Patterns tested | Google API keys, `sk-…` keys, JWTs, PEM private keys, `cloudinary://user:pass@` |
| Config externalisation | `jwt.secret`, `cloudinary.api-secret`, `spring.mail.password`, `minio.secret-key`, `sepay.webhook.secret`, `spring.datasource.password` all read from `${ENV_VAR}` — none hardcoded |

### On the local dev `sa` password

`YourPassword123` appears in tracked files and in 5 commits. **This is a
deliberate local-development credential, not a leaked secret** — it is the
documented Docker SQL Server password for a laptop-only stack, present in
`AGENTS.md`, `README.md`, `.env.example`, `docs/erd-sql-guide.md` and two
`sweep/` scripts. The real boundary is `.env`, which holds the Cloudinary and
SEPAY keys and **is** correctly ignored.

`API_SECRET` occurrences in documentation are environment-variable *names* in
prose, never assigned values. No live credential is committed.

## 9. AI-specific risks

| Risk | Mitigation | Evidence |
|---|---|---|
| Prompt injection via user input reaching the model | Output is never trusted: it is sanitized before render and schema-validated before storage | §4 |
| Runaway generation cost | `:ai` bucket 10/min; `generateAll` treats `count` as a ceiling (F84) | §5 |
| Malformed model output | JSON salvage in 3 layers + schema validation + guards; `ungradeable=true` rather than a guessed grade | `AGENTS.md` |
| Duplicate answer options | MC duplicate-option guard (F85) | regression tests |
| Upstream timeout surfacing as a server fault | 30 s timeout → **504 Gateway Timeout** (F93) | 3 unit tests; container verified to contain the handler |
| Local-only inference | Ollama on localhost; no cloud AI dependency | constitution P4 |

## Summary of dispositions

| Finding | Disposition |
|---|---|
| Admin surfaces role-gated (10 × 3 roles) | **PASS** — 0 failures |
| Self-scoped surfaces correctly user-accessible | **PASS** — verified, not misreported as admin-only |
| Response-body leakage | **PASS** — 0 matches over 10 patterns |
| Token handling (4 cases) | **PASS** — all rejected |
| Stored XSS from AI output | **PASS** — sanitizer + real-browser execution proof |
| Rate-limit bucket routing (F91/F92) | **PASS** — 13 AI + 2 upload endpoints, ceilings confirmed |
| Filter ordering (F90) | **CLOSED — not a bug** — 0 of 17 permitAll surfaces unthrottled |
| Upload → `/api/resources/**` | **PASS** — `contentTypeFor` + `forceDownload` in place, 9 regression tests |
| Secrets | **PASS** — 0 hits in 1 091 tracked files; `.env` ignored |
