# Skeptic #2 — Refutation attempt: "Two Lighthouse audit failures are correct behaviour / missing file, not app defects"

**Claim area:** performance (Phase 5), severity LOW
**Verdict: NOT REFUTED — claim holds.** All five factual assertions reproduce against the live system and the stored Lighthouse reports. Severity LOW is justified.

## What I checked and what I found

### 1. llms-txt fails on all 4 runs — CONFIRMED
Parsed `audits.llms-txt.score` from each stored report:

| report | finalUrl | llms-txt score | is-crawlable score | SEO cat |
|---|---|---|---|---|
| lh-home | http://localhost:5173/ | 0 | 1 | 1.00 |
| lh-lessons | http://localhost:5173/lessons | 0 | 1 | 1.00 |
| lh-login | http://localhost:5173/login | 0 | 0 | 0.66 |
| lh-prod-home | http://localhost:8098/ | 0 | 1 | 1.00 |

(Lighthouse 13.4.1.) `llms-txt` is the *only* audit in the `agentic-browsing` category scoring 0
(the other three scored 1; the three webmcp-* are notApplicable with weight 0).

### 2. GET /llms.txt returns 200 with index.html — CONFIRMED (live, not a probe artefact)
```
curl -s -o /dev/null -w "%{http_code} %{content_type} %{size_download}" http://localhost:5173/llms.txt
→ 200 text/html 3670
```
Body is the SPA shell (`<!DOCTYPE html><html lang="vi">…<script type="module" src="/@vite/client">`).
This is genuine SPA fallback, **not** a 429 and not a guard redirect: an unrelated random path returns
byte-identical results —
```
curl http://localhost:5173/zzz-nonexistent-xyz-12345 → 200 text/html 3670   (same size)
```
So the 200-with-HTML is the dev-server catch-all, reproducible for any unknown path.

### 3. Exact Lighthouse messages — CONFIRMED
`lh-home/report.json → audits.llms-txt.details.items`:
- `"File is missing a required H1 header (e.g., \"# Title\")."`
- `"File does not appear to contain any links."`

Independent cross-check: the served HTML contains **0** `<h1` tags and **0** `^# ` markdown-H1 lines.
Same two messages appear verbatim in `lh-prod-home`.

### 4. File absent from frontend/public/ and frontend/dist/ — CONFIRMED
`frontend/public/`: `e2e-tts.wav`, `robots.txt`, `sitemap.xml` (no llms.txt).
`frontend/dist/`: `assets/`, `e2e-tts.wav`, `index.html`, `robots.txt`, `sitemap.xml` (no llms.txt).
`find . -iname "llms*.txt" -not -path "*/node_modules/*"` → **empty**.
`git log --all -- '**/llms.txt'` → empty (never existed, never ignored).

> Minor imprecision in the claim (does not affect the conclusion): it says both dirs contain
> "only e2e-tts.wav, robots.txt, sitemap.xml". `dist/` additionally contains `index.html` and
> `assets/`, which is expected for a built SPA. The substantive point — no llms.txt — is correct.

### 5. is-crawlable fails only on /login, due to intentional robots.txt — CONFIRMED
`lh-login → audits.is-crawlable`: score 0, details item `source.url = http://localhost:5173/robots.txt`,
`line: 3` (0-based) — which is `Disallow: /login`. The other three runs score 1.
`frontend/public/robots.txt`:
```
User-agent: *
Allow: /
Disallow: /admin
Disallow: /login        ← line index 3
Disallow: /register
Disallow: /forgot-password
Disallow: /profile

Sitemap: https://engflow.app/sitemap.xml
```
The five Disallow rules match the claim exactly; `/login` SEO = 0.66 (is-crawlable weight 4.04 in the
SEO category) while all other routes = 1.00 — i.e. the 66 is arithmetically explained by that one
intentional rule, exactly as claimed.

> Minor imprecision: the claim states all 4 runs hit `localhost:5173/llms.txt`. Run 4 was the
> production build on `:8098`. The llms-txt audit failed identically there, so the substance holds;
> only the literal URL in the evidence line is off for one run.

## Adversarial angles considered and rejected
- **Probe artefact (self-inflicted 429 / rate-limit)?** No — plain GET, no auth, no rate-limit bucket involved; response is stable and identical to any unknown path.
- **Scrollbar false positive?** N/A — neither audit measures layout.
- **Guard redirect misread as failure?** No — `/llms.txt` is not a guarded route; it's the static/dev-server fallback. `/login`'s failure is a robots directive, explicitly reported as such by Lighthouse with the source line.
- **Is "missing file" hiding a real defect?** The claim already concedes llms.txt is a *missing file* (never created), not a code bug; the SPA fallback is standard Vue/Vite behaviour, not an application defect. No false assertion is made.
- **Severity too low?** No functional, security, or data impact. One item is an intentional robots policy; the other is an optional, never-created convention file affecting only the new `agentic-browsing` category. LOW is appropriate; not lower (a real file is genuinely absent), not higher (no user-visible or crawler-blocking harm beyond the deliberate policy).

## Conclusion
Every load-bearing fact in the claim reproduces from real stored reports and live HTTP. Two trivial
imprecisions (dist/ contents listed incompletely; run-4 URL given as :5173 instead of :8098) do not
change the conclusion. The finding is accurate, correctly scoped as non-defect, and correctly rated LOW.

**refuted = false. correctedSeverity = LOW (unchanged).**
