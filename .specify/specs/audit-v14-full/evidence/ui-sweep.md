# audit-v14-full — Phase 3: UI sweep (Playwright driver) + tap-target triage

**Ngày:** 2026-09-25 (+07) · **Driver:** Playwright (chromium, headless) · **Data:** `ui-sweep.json`, `tap-targets.json`
**Ảnh:** `evidence/shots/v14-*.png` (6)

## Summary (final)

| Trục | Kết quả |
|---|---|
| Route × role matrix | **117 cell** (39 route × 3 role) |
| **guardFails** | **0** |
| Console errors | **0** |
| Page errors | **0** |
| API 4xx/5xx do UI gây | **0** |
| Responsive overflow | **0** (360/768/1280/1440/1920) |
| Contrast fails (AA) | **0** |
| Missing `alt` | **0** |
| Elements no accessible name | **0** |
| **Tap targets < 24px** | **118** → **triage: 0 REAL** (50 spacing-except + 68 inline-except) |
| Font `Be Vietnam Pro` | **true** (`document.fonts.check`) |
| Design-system token | khớp (xem `ui-chrome-devtools.md`) |
| Payment cleanup | **ok=true** (4 candidate → 0 remaining → after 126) |
| Parity sau sweep | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — baseline |

## Sửa 3 assertion sai của harness v13 (probe bug, không phải lỗi sản phẩm)

| # | v13 expectation sai | Thực tế đúng | Nguồn xác nhận |
|---|---|---|---|
| P1 | 27 route bounce kỳ vọng URL **trần** (`/login`) → báo 27 guardFail | Router mang `?redirect=` (F-13-20) → so **pathname** | `router/index.js:210` |
| P2 | `/videos`, `/speaking` (premium+auth) anon kỳ vọng `/login` | Premium check chạy TRƯỚC auth → `/premium?redirect=…` | v13 **tự ghi** trong `ui-sweep.md` nhưng harness để sai |
| P3 | Không dọn payment row sau sweep | `PremiumCheckout` tạo row thật lúc mount → phải dọn | AGENTS.md; v13 parity 126→128 |

→ Sau khi sửa: **guardFails 27 → 0**, consoleErrors 1 → 0 (lỗi "compute-pressure permissions policy" là
thông báo Chromium vô hại, không phải lỗi app), payment parity giữ nguyên.

## T3.3 — Tap-target triage (D2) — **ĐÓNG KHOẢNG TRỐNG v13**

v13 **đo 118 vi phạm tap-target** (`<24px` = WCAG 2.5.8 AA) rồi **bỏ khỏi báo cáo** — `REPORT.md` §1 của
nó chỉ ghi "0 guardFails, 0 contrast fail, 0 overflow", **không** nhắc 118. v14 triage **cả 118**:

| Verdict | Số | Nghĩa |
|---|---|---|
| **REAL (AA violation)** | **0** | không có control thật nào < 24px không được miễn trừ |
| SPACING-EXCEPT | 50 | WCAG 2.5.8 "Spacing" — control có ≥24px khoảng trống quanh |
| INLINE-EXCEPT | 68 | WCAG 2.5.8 "Inline" — target nằm trong câu văn |
| **Tổng** | **118** | **khớp chính xác** số thô của `ui-sweep.json` |

**Sửa lỗi triage tự gây (ghi lại minh bạch):** bản triage đầu chỉ áp Inline exception cho `<a>` → báo
**2 REAL** (token từ đơn ký tự "I" trên `/videos/1`, 8×26px). Kiểm lại WCAG 2.5.8: exception Inline áp cho
**mọi** target nằm trong câu, không riêng `<a>` — token trong transcript là `<button>` trong `<p>`, **thuộc
miễn trừ**. Đã sửa logic triage; **KHÔNG** sửa code (sửa code cho thứ được miễn trừ = over-fix).
**Verify:** `git diff frontend/src/views/videos/VideoLesson.vue` = **rỗng**.

## Font — xác nhận Be Vietnam Pro

```
bodyFamily: "Be Vietnam Pro", system-ui, sans-serif
checkBeVietnam: true   (mọi route/role)
grep Outfit/Plus Jakarta trong frontend/src = 0
```

## Responsive

5 mức 360/768/1280/1440/1920, `overflowRoutes = 0`. (`ovf=-15` là Chromium chừa scrollbar — không phải tràn.)

## Ảnh bằng chứng

`shots/v14-home-1440.png`, `v14-lessons-360.png`, `v14-lessons-1920.png`, `v14-admin-dashboard-1440.png`,
`v14-profile-1280.png`, `v14-premium-768.png`.
