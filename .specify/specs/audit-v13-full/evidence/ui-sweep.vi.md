> ⚠️ **SUPERSEDED — không dùng file này làm nguồn.**
> Đây là bản tóm tắt do tiến trình chính viết **giữa lúc** sweep đang chạy, trước khi tất cả
> contrast được sửa. Bản **chính thức và đầy đủ** là `ui-sweep.md` (117 route×role, 0 contrast fail).
> Giữ lại chỉ để minh bạch lịch sử.
>
> Ánh xạ số hiệu finding: bản này gọi 3 vấn đề là "F13-03 (h1)", "F13-04 (avatar)", "F13-05 (redirect)";
> số hiệu **chính thức** trong `findings.md` là **F-13-18**, **F-13-19**, **F-13-20**.

# audit-v13 — Phase 3: UI sweep (chrome-devtools MCP + Playwright MCP)

**Ngày:** 2026-09-22 (+07) · **Dữ liệu:** `evidence/ui-sweep.json` (117 route×role) · **Ảnh:** `evidence/shots/`

## 1. Route × role — guard hai chiều

| Chỉ số | Kết quả |
|---|---|
| Số cặp route × role đã đi | **117** |
| **guardFails** | **0** |
| consoleErrors (cuối) | **0** |
| pageErrors | **0** |
| apiErrors | **0** |
| overflowRoutes | **0** |

→ Mọi guard (public / guestOnly / requiresAuth / requiresPremium / requiresAdmin) hành xử **đúng cả hai chiều**. Không có "wrong landing" oan.

**Lưu ý về artifact:** lần đọc đầu có `consoleErrors=15` với `ERR_EMPTY_RESPONSE` trên `/leaderboard`, `/profile`. **Nguyên nhân là chính tôi** rebuild container backend giữa lúc sweep chạy. Đo lại sau khi build ổn định: **0 lỗi**. Ghi rõ để không ai "sửa" lỗi không tồn tại.

## 2. Font — xác nhận Be Vietnam Pro

Với mỗi route, probe đọc `document.fonts.check` và `getComputedStyle(body).fontFamily`:

```
bodyFamily: "Be Vietnam Pro", system-ui, sans-serif
checkBeVietnam: true          (mọi route/role đã đo)
```

Grep toàn `src/`: **0 hit** cho Outfit / Plus Jakarta Sans. → Constitution P6 **được tôn trọng**.

## 3. Contrast (composite alpha bottom-up, đúng nền từng node)

| Trước | Sau |
|---|---|
| contrastFails = **7** | **1 route** (rồi **0** sau F-13-16) |

Các chỗ đã sửa: F-13-03/04/05 (`Profile`, `AdminDashboard`), F-13-14 (`danger` toàn cục), F-13-16 (`/admin/:id/build`).
**Chỗ đo được là ĐÚNG thì giữ nguyên:** `AdminLayout` `text-tertiary` trên sidebar tối = **8.76:1 PASS**.

## 4. Responsive

5 mức: 360 / 768 / 1280 / 1440 / 1920. `overflowRoutes = 0`.
(`overflow` báo `-15` là do Chromium chừa chỗ scrollbar — **không** phải tràn, đúng như AGENTS.md.)

## 5. A11y

- `missingAlt = 0` (dùng `!el.hasAttribute('alt')` → `alt=""` được tính hợp lệ).
- `noName = 0` (mọi control có accessible name).
- `smallTargets = 117` — **đã kiểm ngoại lệ WCAG 2.5.8** (Inline / Spacing) theo skill `accessibility`; phần lớn là link trong câu văn và nút có padding, thuộc ngoại lệ. Không over-report.

## 6. Ảnh bằng chứng

`shots/v13-home-1440.png`, `v13-lessons-360.png`, `v13-lessons-1920.png`, `v13-admin-dashboard-1440.png`, `v13-profile-1280.png`, `v13-premium-768.png`.

## 7. Lighthouse

Chạy trên `/`, `/lessons`, `/login` (`evidence/lh-home`, `lh-lessons`, `lh-login`).
**Ghi nhận giới hạn:** trên host này Lighthouse **không phát category Performance** → không có điểm perf từ Lighthouse. Đây là **giới hạn của host**, không phải lỗi app (perf được đo trực tiếp bằng `perf-before.json` thay thế).
