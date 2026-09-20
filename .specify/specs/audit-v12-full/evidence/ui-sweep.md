# Phase 3 — UI sweep bằng CẢ HAI MCP

**Ngày:** 2026-09-21 (+07) · **App:** `http://localhost:5173` (Vite dev) + `http://localhost:4173` (production build)
**Driver 1:** chrome-devtools MCP (Brave, `--isolated`) · **Driver 2:** Playwright (chromium 1237, `executablePath`)
Kết quả **ghi rõ driver nào ra số nào**, không trộn — engine khác nhau.

---

## 0. Sửa thiếu sót của v11: chrome-devtools MCP dùng THẬT

v11 dùng chrome-devtools MCP gần như chỉ `list_pages` + 1 `lighthouse_audit`. v12 dùng nó làm driver hạng nhất:

| Tool | Dùng cho |
|---|---|
| `new_page` / `navigate_page` | mở từng route |
| `evaluate_script` | đo token, font, a11y, CLS, seed session |
| `list_network_requests` | **UI↔API cross-check** (bắt call thật) |
| `lighthouse_audit` | 3 route (desktop) |

**Bằng chứng sống:** chrome-devtools `list_pages` → page; Playwright `browser_tabs` → tab. Cả hai chạy được.

---

## 1. Design-system conformance + FONT (T3.10) — đo trên DOM thật

`evaluate_script` trên `/` (chrome-devtools):

```
body background : rgb(255, 253, 245)          -> --geo-bg #FFFDF5  ✓ (cream)
--geo-fg        : #1E293B   ✓
--geo-accent    : #8B5CF6   ✓   --geo-secondary : #F472B6  ✓
--geo-tertiary  : #FBBF24   ✓   --geo-quaternary: #34D399  ✓
--geo-border-width : 2px    ✓
--geo-radius-sm/md/lg : 8px / 16px / 24px    ✓
--geo-shadow-sm : 3px 3px 0px 0px #1E293B    ✓ (hard shadow, 0 blur)
--geo-shadow-md : 4px 4px 0px 0px #1E293B    ✓
--geo-shadow-featured : 8px 8px 0px 0px #F472B6  ✓ (pink featured)
```

### FONT — yêu cầu "thay toàn bộ font thành Be Vietnam Pro"

```
distinctFontFamilies : ["\"Be Vietnam Pro\", system-ui, sans-serif"]     <- ĐÚNG 1 họ
fontCounts           : 90 text node, tất cả cùng họ
document.fonts.check('16px "Be Vietnam Pro"') : true
document.fonts.status : "loaded"
```

**⇒ Yêu cầu font ĐÃ ĐẠT, đo trên DOM thật.** Đây là **verify**, không phải việc mới (H12). Báo nó là "đã làm"
sẽ là bịa.

### Thành phần design-system đặc trưng (đo bằng selector)

| Thành phần | Hiện diện | Giá trị đo |
|---|---|---|
| Hero sun (vòng tròn vàng lớn) | ✓ `.app-hero__sun` | `rgb(251, 191, 36)` = `--geo-tertiary` |
| Blob shape | ✓ | — |
| Marquee vô hạn | ✓ `.app-marquee` | — |
| Squiggle divider | ✓ | — |
| Dashed connector (feature cards) | ✓ | — |
| Logo wordmark (header) | ✓ | `rgb(30, 41, 59)` = `--geo-fg` (đúng trên nền sáng) |
| Button pill + border 2px | ✓ | `border-radius: 9999px`, `border-width: 2px` |

---

## 2. Lighthouse — chrome-devtools MCP (driver 1)

| Route | Accessibility | Best Practices | SEO |
|---|---|---|---|
| `/` | **100** | **100** | **100** |
| `/lessons` | **100** | **100** | **100** |
| `/login` | **100** | **100** | 66 |

**Hai "fail" của Lighthouse đã được phân loại, không phải lỗi:**

| Audit | Verdict |
|---|---|
| `/login` `is-crawlable` = 0 | **ĐÚNG SEO hygiene** — `robots.txt` cố ý `Disallow: /login`. Lighthouse báo "fail" chính là rule đó hoạt động đúng |
| `llms-txt` = 0 (mọi route) | Category `agentic-browsing` (Lighthouse 13 preview) về chuẩn `llms.txt` — **không** phải tín hiệu a11y/perf, không áp dụng cho app này |
| `/` `cumulative-layout-shift` = 0.103 | **F149 → F150** (xem §5), lỗi thật, đã truy gốc |

---

## 3. Route × role — guard assert CẢ HAI CHIỀU (T3.3)

Playwright, 20 route × 3 role, seed **cả** `localStorage.token` **và** `localStorage.user` (bẫy
`HARNESS-TOKEN-ONLY-SEED`), assert `page.url()` sau điều hướng.

```
anon    /                       landed /                        text=1709
anon    /lessons                landed /lessons                 text=1813
anon    /videos                 landed /premium?redirect=/videos text= 657   <- upsell funnel (đúng thiết kế, P9)
anon    /leaderboard            landed /leaderboard             text=1502
anon    /decks                  landed /decks                   text=1308
anon    /search                 landed /search                  text= 234
anon    /premium                landed /premium                 text= 657
anon    /login                  landed /login                   text= 137
anon    /register               landed /register                text= 158
student /profile                landed /profile                 text= 536
student /speaking               landed /speaking                text= 997
student /speaking/history       landed /speaking/history        text=1429
student /decks/create           landed /decks/create            text= 328
student /ai-vocab-generator     landed /ai-vocab-generator      text= 370
admin   /admin/dashboard        landed /admin/dashboard         text= 721
admin   /admin/lessons          landed /admin/lessons           text=2800
admin   /admin/exercises        landed /admin/exercises         text=2471
admin   /admin/users            landed /admin/users             text=1218
admin   /admin/videos           landed /admin/videos            text= 588
admin   /admin/speaking-prompts landed /admin/speaking-prompts  text= 921
```

**Chiều ngược (bounce) — kiểm bằng chrome-devtools MCP:**
- anon → `/admin/dashboard` **bounced** về `/login` ✓
- student (session hạ cấp) → `/admin/users` **bounced** về `/` ✓
- admin → `/admin/users` **stay**, render 10 row, `<h1>` = "Người dùng" ✓
- admin → `/admin/dashboard` **stay**, `<h1>` = "Tổng quan", 703 ký tự ✓

**Không route nào render nhầm trang chủ mà báo PASS.** `/videos` → `/premium?redirect=` là **upsell funnel có chủ
ý** (guard kiểm `requiresPremium` trước `requiresAuth`) — **xác nhận** P9 của v11.

## 4. Console / network health (T3.4)

```
consoleErrors: 0   pageErrors: 0   apiErrors: 0
```

Trên **20 route × 3 role**. (`apiErrors` chỉ đếm ≥400 **không phải** 401/403/404 dự kiến.)

### UI↔API cross-check — call UI thật sự phát ra

| Route | Network call bắt được | Khớp hợp đồng Phase 2? |
|---|---|---|
| `/admin/dashboard` | `GET /api/auth/me` [200], `GET /api/admin/stats` [200] | ✓ đúng 2 call, không endpoint lạ |

(Đo bằng chrome-devtools `list_network_requests`.)

## 5. Responsive (T3.5) — số thô

`document.documentElement.scrollWidth − clientWidth`, 3 route × 5 width:

```
/         @360/768/1280/1440/1920 -> -15 (mọi width)
/lessons  @360/768/1280/1440/1920 -> -15
/premium  @360/768/1280/1440/1920 -> -15
```

**−15 = gutter scrollbar Chromium chừa ra, KHÔNG phải overflow.** Không width nào có overflow thật
(`> 0`). `/premium` từng tràn +8/+28px (F131 v11) — **xác nhận đã fix**.

## 6. A11y (T3.6) — 20 route

| Kiểm | Kết quả |
|---|---|
| `<img>` thiếu **thuộc tính** `alt` (`!el.hasAttribute('alt')`) | **0** |
| Control không có accessible name | **0** |
| `<h1>` mỗi trang | **1** trên mọi route |
| Heading skip cấp | **0** |
| `lang` | `vi` trên mọi route |
| Skip-link | có trên mọi route |
| `:focus-visible` rule | có |

### Tap target — 7 "vi phạm" thô, và tại sao **KHÔNG** phải vi phạm

Probe đầu đếm `< 24px` được 7 phần tử. Kiểm lại theo **WCAG 2.5.8** (có ngoại lệ **Inline** và **Spacing**) —
đo khoảng cách giữa tâm các target:

| Phần tử | Kích thước | Ngoại lệ áp dụng | Verdict |
|---|---|---|---|
| `/login` checkbox "Ghi nhớ" | 16×16 | **Spacing** — tâm cách target khác > 24px | EXEMPT |
| `/login` link "Quên mật khẩu?" | 120×16 | **Spacing** | EXEMPT |
| `/login` link "Đăng ký" | 58×18 | **Inline** (nằm trong câu) | EXEMPT |
| `/premium` link "đăng nhập" | 75×18 | **Inline** | EXEMPT |
| `/videos`, `/register` link | 75–77×18 | **Inline** | EXEMPT |
| `/decks/create` checkbox | 20×20 | **Spacing** | EXEMPT |

**⇒ 0 vi phạm WCAG 2.5.8.** Probe thô của tôi **over-report**; đây là lỗi probe (V9), ghi lại để không ai "sửa"
CSS đang đúng. (24–44px chỉ là khuyến nghị AAA, không phải ngưỡng AA.)

## 7. Contrast AA — composite alpha bottom-up (T3.7)

Phép đo **composite từng lớp alpha từ dưới lên trên nền trắng** (gốc rễ F138 v11: probe composite sai nên bỏ sót).

```
contrastFails trên 20 route: 1 lỗi thật (2 text node)  -> xem F149
```

**F149 (đã fix):** `/admin/lessons`, class `text-muted-foreground/60` → composited `rgb(153,160,169)` trên trắng =
**2.65:1** (cần 4.5:1). Đây là **cùng lớp lỗi F138** (opacity làm loãng màu tới mức trượt AA).

---

## Verdict

**Phase 3: PASS sau khi fix 1 lỗi.** Dùng **cả hai** MCP thật; 20 route × 3 role guard **hai chiều**; 0 console/page/API
error; 0 overflow thật ở 5 width; 0 `alt` thiếu; 0 heading skip; 0 vi phạm tap-target (sau khi áp ngoại lệ WCAG);
Lighthouse a11y **100** trên 3 route. **1 lỗi thật: F149** (contrast), đã fix + verify live + có regression test.
Hai tín hiệu của probe bị **falsify**: 7 "tap-target" là EXEMPT theo WCAG, và `is-crawlable` `/login` là SEO hygiene
đúng chủ ý. Thêm **F150** (CLS) phát hiện qua Lighthouse, truy gốc ở `evidence/performance.md`.
