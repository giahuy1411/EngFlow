# audit-v18-full — Phase 3: UI/UX automated sweep

**Ngày:** 2026-09-27 · **Nguồn:** `sweep/harness/ui-sweep.js`, `sweep/v8/ui/design-v2.js`, `routes-all.js`, `cls-probe.js`.

## T3.1 ui-sweep — route × role × viewport

```
=== TOTALS === {"contrastFails":0,"missingAlt":0,"smallTargets":115,"noName":0,
                "consoleErrors":0,"apiErrors":0,"pageErrors":0,"guardFails":0,"overflowRoutes":0}
responsive: 35 cells, 0 overflowing
assertClean: parity=1470|43738|5|118|29|4|3|12|10 study_days=4 pending_payments=0 exercise_attempts=33 -> CLEAN
```

- **0 contrast fail · 0 missing alt · 0 no-name · 0 console error · 0 API error · 0 page error · 0 guard fail · 0 overflow.**
- Guards đúng cả hai chiều: anon → `/login`, student → `/` trên route admin; catch-all → `/`.
- Tự dọn: `cleanupAuditPayments` 2→0, parity CLEAN.

### Triage `smallTargets=115` (không phải lỗi thật)

| Loại | Số | Kích thước | Kết luận |
|---|---|---|---|
| `<A>` link văn bản inline | 52 | 58–136 × 16–18 | **WCAG 2.5.8 inline exception** — link nằm trong dòng chữ, miễn trừ |
| `<INPUT type=checkbox>` | 21 | 16×16, **label 80×16** | Checkbox nằm trong `<label>` (`inLabel=true`) → target hiệu dụng = label; **spacing exception** (cô lập, không target lân cận) |

→ **0 vi phạm thật.** Đo trực tiếp trên `/login` bằng chrome-devtools MCP xác nhận: checkbox có
`closest('label')` = "Ghi nhớ" (label box 80×16). Ghi chú: có thể phóng to checkbox cho thoải mái hơn
(không bắt buộc AA) — để lại như cải tiến tùy chọn.

## T3.2 design-v2 — conformance 5 viewport

```
elements sampled      : 7670
non-BVP computed fonts: 0
legacy-family hits    : 0
routes w/ overflow    : 0 / 60
tokens missing        : 0 (of 660 checks)
token value drift     : 0
lucide icons checked  : 655   stroke != 2.5: 0
mobile shadow !2px    : 0
LANDED ON WRONG PAGE  : 0
font loaded (400/700/900): {"loaded":true,"bold":true,"black":true,"faces":18}
```

→ **Be Vietnam Pro LOADED thật** (không chỉ khai báo), 0 font lạ, 0 legacy, 0 token drift, 0 overflow,
655 lucide icon stroke 2.5. **Xác nhận yêu cầu "thay toàn bộ font = Be Vietnam Pro" đã thoả.**

## T3.4 routes-all — guard 2 chiều

```
visits            : 228 (38 routes x 2 viewports x 3 roles)
console errors    : 0
API >=400         : 0
overflow routes   : 0
not mounted       : 0
routes w/ bad font: 0
anon on guarded   : 52 (not redirected: 0)
wrong landing     : 0
admin visits rendering a real admin page: 16/18
```

## T3.5 CLS

| Route | CLS median | Armed |
|---|---|---|
| `/` | 0.00069 | true |
| `/lessons` | 0.00095 | true |
| `/login` | 0.00003 | true |

→ Rất tốt (ngưỡng "good" < 0.1).

## T3.6 danger-tint + focused-probe

- **danger-tint**: trạng thái lỗi ép buộc (route abort) ở `/admin/dashboard`, `/decks`, `/leaderboard` →
  chữ `text-danger-ink` `rgb(190,18,60)` trên `rgb(252,231,228)` = **5.28:1 ≥ 4.5 → PASS AA**.
- **focused-probe**: leaderboard forced-error 5.28:1 PASS; admin sidebar `bg-fg` chữ trắng 14.63:1; badge
  "Tổng quan" trắng trên `accent-strong` `rgb(124,58,237)` = **5.70:1** PASS; `text-tertiary` trên `bg-fg` 8.76:1;
  `tertiary-ink` 5.02:1. `recheck /profile /search /leaderboard /videos/1 -> []` (0 vấn đề).

## T3.7 a11y content panel (f1302)

`content a11y: 5/5 PASS` — đúng 1 `h1`, 0 `<img>` thiếu alt, 0 id trùng, contrast 63 text node AA.

## Fix harness trong Phase 3 (F-18-01)

`danger-tint.js` và `focused-probe.js` **hardcode `chromium-1237`** — bundle cài đặt là **1234** →
`browserType.launch: executable doesn't exist`. Đây là **lỗi harness** (đường dẫn revision cũ), không phải
lỗi app. **Fix:** thêm `resolveChromium()` vào `sweep/v8/ui/lib.js` (đọc `playwright-core` registry →
quét `ms-playwright/chromium-*` → Brave fallback); `ui-sweep.js`, `danger-tint.js`, `focused-probe.js` dùng
chung. Cả 2 probe chạy lại **EXIT=0**.
