# Skeptic #2 — REFUTATION of "Webfont payload is 125376 bytes and invisible to Resource Timing"

**Claim under test (area performance (Phase 5), severity LOW):** the Google Fonts CSS declares 18
faces; the browser fetched 15 woff2 totalling **125 376 bytes**; those bytes are absent from
Resource Timing `local_transfer` (290 387 B for `/`, 350 539 B for `/lessons`) because cross-origin
woff2 without `Timing-Allow-Origin` hides `transferSize`. Root cause: 18 faces across 3 subsets; the
800 face is deliberate. Recorded, no cut recommended.

**Verdict: REFUTED.** The byte figure is wrong by ~1.9×, and its stated mechanism is contradicted by
the live headers. The claim is not a probe artefact (the 15-file fetch is real) — it is a
**hand-entered number with no probe behind it**, plus a mechanism that is false for `fonts.gstatic.com`.

---

## 1. The 125 376 B figure is impossible for 15 files

Fetched the stylesheet with a real Chrome UA and downloaded every woff2 it references:

```
$ curl -s -A "<Chrome 126 UA>" \
  "https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800;900&display=swap"
HTTP size 7836 B · @font-face count = 18 · unicode-range count = 18
```

Per-file byte sizes (`curl -w '%{size_download}'`, and confirmed via `content-length` response
headers):

| subset | 400 | 500 | 600 | 700 | 800 | 900 |
|---|---|---|---|---|---|---|
| latin | 21 168 | 21 892 | 22 032 | 22 152 | 22 320 | 21 760 |
| latin-ext | 13 056 | 13 548 | 13 608 | 13 852 | 13 752 | 13 592 |
| vietnamese | 11 532 | 12 172 | 12 176 | 12 468 | 12 404 | 12 072 |

- All 18 files total = **285 556 B**.
- The **smallest single file is 11 532 B**, so 15 files can never weigh 125 376 B:
  **15 × 11 532 = 172 980 B is the floor.**
- A brute-force search over all 2^18 subsets found exactly 7 subsets equal to 125 376, and **none has
  15 members** (the smallest is 7 files). So "15 files = 125 376 B" is arithmetically impossible.

**What the browser actually fetches.** On a cold isolated-context load of `/`, the CDP network panel
lists exactly **15 woff2 requests** (weights 400/500/700/800/900 × 3 subsets; 600 is not fetched on
the home route). Summing their real `content-length` values:

```
weight 400: 45756   500: 47612   700: 48472   800: 48476   900: 47424
TOTAL 15 files = 237740 B
```

**Real 15-file payload = 237 740 B, not 125 376 B — the claim undercounts by 112 364 B (1.896×).**

Compression is not the explanation either: `fonts.gstatic.com` serves woff2 with **no
`Content-Encoding`** (`curl -H 'Accept-Encoding: br, zstd, gzip'` → `Content-Length: 21168`, no
`content-encoding` header). Compressed-sum of the 15 files is the same 237 740 B. The 125 376 figure
matches nothing measurable.

## 2. The stated mechanism is false — gstatic sends Timing-Allow-Origin

```
$ curl -s -D - -o /dev/null "<woff2>" | grep -i timing
timing-allow-origin: *
access-control-allow-origin: *
```

The claim says the bytes are hidden "because cross-origin woff2 lack Timing-Allow-Origin". The live
response **does** carry `timing-allow-origin: *`. The premise is directly contradicted.

The observation "no woff2 in Resource Timing" is nevertheless reproducible — but for a different
reason, and the claim misattributes it. On a blank page (`about:blank`) I injected a Google Fonts
stylesheet for a family never used before; the network panel shows the gstatic woff2 fetched
(reqid=2, `[200]`) while `performance.getEntriesByType('resource')` returns **0** gstatic entries.
Conversely, a **direct `fetch()`** of the same woff2 **does** appear in Resource Timing with
`encodedBodySize: 21168`. So the invisibility is a CSS-font-loading/browser artefact (this browser is
**Brave**, `navigator.brave === true` — fingerprinting protection is a plausible contributor), not a
missing-TAO issue. The claim's causal explanation is wrong even though its surface observation holds.

## 3. The "no cut recommended" reasoning does not rescue the finding

- The 800-weight usage count of 17 is **verified**: `font-weight: 800` appears 17× (app-layout.css 2,
  app-logo.css 2, design-system.css 10, LearningPath.vue 3) — matches the claim.
- But the cited width evidence does **not** reproduce here. Live measurement of an `"EngFlow"` span at
  16 px in Be Vietnam Pro gave a **monotonic** progression:
  `400: 67.469 | 500: 68.094 | 600: 68.719 | 700: 69.375 | 800: 69.984 | 900: 70.609` px.
  The claim asserts 800 and 900 are "TRÙNG KHÍT" (identical, 633.000 px) vs 700 = 618.406 px — i.e.
  800 collapsing onto 900. Here 800 and 900 differ by 0.625 px and the 800 face loads as its own face
  (`loadedFaces` shows distinct 400/500/600/700/800/900 entries), so no synthesis collapse is visible
  at this string size. (The claim measured in Edge at a larger size; I could not reproduce the
  identical-width collapse in Chromium, so this sub-claim is unverified rather than disproven.)
- None of this makes the **number** right. "No cut recommended" is a recommendation, not evidence for
  125 376 B.

## 4. Is it a probe artefact?

No. The 15-file fetch is genuine and reproducible (two independent cold contexts: Playwright and CDP
both show 15 gstatic woff2). The defect is the opposite of a probe artefact: **no probe measured these
numbers at all.** `sweep/v13/perf-probe.js` (147 lines) contains no `font`/`woff`/`gstatic`/`transfer`
reference, and the values appear only as literals in `perf-before.json`:

```
sweep/v13/perf-before.json:1302  "files_fetched_by_browser": 15,
sweep/v13/perf-before.json:1303  "bytes_fetched": 125376,
sweep/v13/perf-before.json:1304  "note": "cross-origin woff2 are invisible to Resource Timing ..."
```

`grep -rn 125376` finds no other occurrence in the repo (excluding the report prose). A number with no
producer is an assertion, not a measurement.

## 5. Corrected severity / disposition

- The finding's headline quantity is **wrong** (237 740 B real vs 125 376 B claimed) and its root-cause
  sentence is **false** (TAO is present). As written the finding does not hold.
- A *corrected* version could survive at the same **LOW** severity: "webfont payload ≈ 238 kB for a
  cold home load, not counted in local_transfer" — but only if the 18-face/3-subset figure and the
  real byte count are restated, and the mechanism replaced (CSS font loading / browser privacy
  filtering, not missing Timing-Allow-Origin). Even then, "recorded, not recommended for cutting" is
  consistent with LOW.
- As submitted — with a specific byte count and a specific causal mechanism, both of which the live
  system contradicts — the claim should be **refuted** and re-derived before it is recorded.

## Evidence provenance

- Google Fonts CSS + 18 woff2 byte sizes: `curl` with Chrome UA, plus `content-length` response
  headers via `curl -D -` and CDP `get_network_request` (reqid 54 → `content-length: 11532`,
  `timing-allow-origin: *`).
- Cold-load 15-file list: Playwright `browser_network_requests` and CDP `list_network_requests`
  (`resourceTypes: ["font"]`) on isolated contexts `fontprobe3` / `fontprobe`.
- Resource Timing: `performance.getEntriesByType('resource')` on `http://localhost:5173/`
  (0 gstatic entries; only the googleapis CSS at transferSize 903) and on `about:blank` after
  injecting a fresh Roboto Slab stylesheet (0 gstatic entries despite reqid=2 [200]).
- Direct-fetch control: `fetch()` of a gstatic woff2 → 1 entry, `encodedBodySize: 21168`.
- 800-weight usage: `grep -rn "font-weight: *800\|font-extrabold" frontend/src` = 17.
- Width measurement: `getBoundingClientRect().width` on a 16 px `"EngFlow"` span, per weight.
- Browser: Brave 153 (Chromium 153), `navigator.brave === true`.
