# audit-v17-full — Phase 3: UI/UX automated sweep (engine 2 = headless playwright-core)

**Vì sao file này:** chrome-devtools MCP là engine 1 (tương tác thật, `mcp-walkthrough.md`). Playwright MCP
BLOCKED (thiếu Chrome channel) → engine 2 là harness **headless `playwright-core`** (Chromium bundle), chạy được thật.
Hai engine **độc lập** cùng chạy các luồng trọng yếu → cross-validation (R11).

## `ui-sweep.js` — route × role × 5 viewport

**TOTALS (đo phiên này):**
```json
{"contrastFails":0,"missingAlt":0,"smallTargets":115,"noName":0,"consoleErrors":1,"apiErrors":0,
 "pageErrors":0,"guardFails":0,"overflowRoutes":0}
```
- **0** contrast fail · **0** missing alt · **0** noName · **0** api error · **0** page error · **0** guard fail
  · **0** overflow (responsive 35 ô, 0 tràn).
- **consoleErrors = 1** → duy nhất `student /videos/1`: `[warn] postMessage … youtube.com` (**third-party iframe**,
  không phải lỗi app — đã verify bằng MCP). → **F-17-06** (harness nên phân biệt warn/error).
- `smallTargets=115` — **trùng số v14/v16** đã triage 0 REAL (24–44px = AAA; AA = 24px). Không phải lỗi mới.

**Guard (2 chiều, đúng thiết kế):**
- `anon /admin/*` → `/login` (đúng) · `student /admin/*` → `/` (đúng) · `admin /admin/*` → **stay** (đúng).
- catch-all `/:pathMatch(.*)*` → `/` (đúng).

**Self-clean + parity (cổng chặn):**
```
cleanupAuditPayments: candidates=2 remaining=0 after=12 baseline=12 -> SELF-CLEAN OK
cleanupStudyDays:    candidates=0 remaining=0 -> SELF-CLEAN OK
assertClean: parity=1470|43738|5|118|29|4|3|12|10 study_days=4 pending_payments=0 -> CLEAN
```

## `design-v2.js` — design conformance (5 viewport)

```
elements sampled      : 7670
non-BVP computed fonts: 0
legacy-family hits    : 0        (Outfit/Plus Jakarta/Inter/Poppins = 0)
routes w/ overflow    : 0 / 60
tokens missing        : 0 (of 660 checks)
token value drift     : 0
lucide icons checked  : 655   stroke != 2.5: 0
mobile shadow !2px    : 0
LANDED ON WRONG PAGE  : 0
```
→ **Playful Geometric + Be Vietnam Pro khớp prompt đã sửa**: font đơn nhất, token đủ, không drift, hard-shadow 2px
trên mobile, icon 2.5.

## `cls-probe.js` — Cumulative Layout Shift

```
/          CLS median 0.00069
/lessons   CLS median 0.00095
/login     CLS median 0.00003
```
→ Tốt (ngưỡng "good" = < 0.1).

## Cross-validation (2 engine)

| Luồng trọng yếu | Engine 1 (chrome-devtools MCP) | Engine 2 (playwright-core headless) | Đồng thuận |
|---|---|---|---|
| Guard student → `/admin/*` | bounced → `/` | `student /admin/users -> /` | ✅ |
| `/videos/1` console | 1 warn (YouTube) | `err=1` | ✅ (cùng 1 message) |
| Overflow 5 viewport | `ovf=-15` mọi route | `0 / 60` tràn | ✅ |
| Font/token | — | 0 badFont / 0 drift | ✅ |
| Parity sau sweep | — | CLEAN | ✅ |
