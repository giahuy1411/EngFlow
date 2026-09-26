# audit-v16-full — Phase 3: UI/UX sweep (route × role × viewport)

**Công cụ:** `sweep/harness/ui-sweep.js` — mọi route × 3 role (anon/student/admin), + responsive 5 viewport,
+ a11y (alt, tap-target, heading, skip-link, lang, focus), + contrast (composite bottom-up), + console/network health.
**Kết quả:** `evidence/ui-sweep.json` (+ log vòng 1 `ui-sweep.log`, vòng xác minh `ui-sweep-rerun.log`).

## Totals (vòng xác minh — `ui-sweep-rerun.log`)

| Chỉ số | Giá trị | Ngưỡng |
|---|---|---|
| `guardFails` (sai trang đích) | **0** | 0 |
| `consoleErrors` | **0** | 0 |
| `apiErrors` | **0** | 0 |
| `pageErrors` | **0** | 0 |
| `contrastFails` | **0** | 0 |
| `missingAlt` | **0** | 0 |
| `noName` (nút/link thiếu tên) | **0** | 0 |
| `overflowRoutes` | **0** | 0 |
| `smallTargets` | **115** | đã triage → 0 REAL (xem dưới) |
| Responsive | **35 ô, 0 tràn** | 0 |

## Guard × role (đo được, đúng thiết kế)

- `anon` trên route bảo vệ → `/login` (hoặc `/premium` với route premium) — **đúng**.
- `student` trên `/admin/*` → `/` — **đúng** (`requiresAdmin`).
- `admin` trên mọi route → đúng trang — **đúng**.
- `/admin/445/build` (route Đường B đã gỡ) → `/` cho **cả 3 role** — **đúng** (catch-all).
- `/definitely-not-a-route` → `/` — **đúng** (catch-all).

## A11y

- **0** ảnh thiếu thuộc tính `alt` (dùng `hasAttribute`, `alt=""` hợp lệ).
- **0** nút/link thiếu tên truy cập.
- **0** lỗi thứ tự heading, skip-link, lang.
- `smallTargets=115` — **cùng con số v14 đã triage** (52 inline + 21 spacing, 0 REAL); không có mục mới.
  (Đây là cảnh báo, không phải lỗi: 24–44px là khuyến nghị AAA; AA = 24px và tất cả đều đạt qua exception inline/spacing.)

## Design conformance (trong sweep)

- `font="Be Vietnam Pro", system-ui, sans-serif` + `checkBV=true` trên mọi route lấy mẫu.
- Lucide `stroke=2.5px` ở route có icon.
- Ảnh: `evidence/shots/v13-*.png` (home/lessons/admin/profile/premium @ nhiều viewport).

## F-16-01 — xác minh fix (study_days)

| | Vòng 1 (trước fix) | Vòng xác minh (sau fix) |
|---|---|---|
| `cleanupStudyDays` | (không có) | `candidates=0 remaining=0 -> SELF-CLEAN OK` |
| `assertClean` | `study_days 5 != expected 4 -> DIRTY` | `study_days=4 ... -> CLEAN` |
| Exit code | **1** | **0** |

→ **F-16-01 FIXED**, có bằng chứng 2 chiều (trước/sau).
