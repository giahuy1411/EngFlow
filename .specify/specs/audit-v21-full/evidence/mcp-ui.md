# audit-v21-full — MCP deep UI/UX (2 engine)

**Đo:** 2026-09-28 · engine: `chrome-devtools-mcp` + `playwright` (plugin) · app: `localhost:5173` → `:8080`

> Người dùng yêu cầu **đi sâu hơn** vào test UI/UX bằng **cả hai** MCP. Mỗi kịch bản chạy trên
> 2 engine và đối chiếu chéo.

## A. chrome-devtools MCP — đo token/design bằng `evaluate_script`

### A1. Trang chủ `/`

| Chỉ số | Đo được | Kỳ vọng | ✓ |
|---|---|---|---|
| `distinctFonts` | **`["Be Vietnam Pro"]`** | đúng 1 font | ✓ |
| `bodyBg` | `rgb(255,253,245)` | `#FFFDF5` | ✓ |
| `bodyFont` | `"Be Vietnam Pro", system-ui, sans-serif` | BVP | ✓ |
| `h1Weight` / `h1Font` | **900** / Be Vietnam Pro | 800–900 | ✓ |
| lucide stroke | **2.5px** | 2.5 | ✓ |
| `.app-btn` border / radius | **2px / 9999px** | 2px / pill | ✓ |
| overflow | **0px** | ≤16px | ✓ |
| ảnh thiếu `alt` | **0** | 0 | ✓ |
| `--geo-accent` | `#8B5CF6` | violet | ✓ |
| `--geo-shadow-md` | `4px 4px 0px 0px #1E293B` | hard shadow, 0 blur | ✓ |
| `--geo-border-width` / `--geo-radius-md` | `2px` / `16px` | đúng | ✓ |

**Decoration literal của prompt (đo computed style):**

| Feature | Đo được | ✓ |
|---|---|---|
| Vòng tròn vàng lớn sau hero | `.app-hero__sun` = `rgb(251,191,36)` = `#FBBF24`, rộng **640px**, `aria-hidden=true` | ✓ |
| Blob mask cho shape hero | `.app-hero__shape--blob` border-radius `60% 40% 55% 45% / 45% 55%` | ✓ |
| Đường nối nét đứt giữa feature cards | `.app-features__connector` = `<svg>`, `aria-hidden=true` | ✓ |
| Squiggle divider | `.squiggle-divider` `aria-hidden`, bg `rgb(30,41,59)`, mask-image active | ✓ |
| Dot grid | 8 phần tử dot | ✓ |

### A2. Trang `/premium` (featured card + badge)

| Feature | Đo được | Kỳ vọng | ✓ |
|---|---|---|---|
| Card featured scale | `matrix(1.1, 0, 0, 1.1, 0, 0)` = **scale(1.1)** | 1.1 | ✓ |
| Badge xoay | `matrix(0.965926, 0.258819, …)` = **rotate(15deg)** | 15deg | ✓ |
| h1 count | 1 | 1 | ✓ |
| overflow | 0px | ≤16 | ✓ |

### A3. Lighthouse (desktop, navigation)

| Trang | Accessibility | Best Practices | SEO |
|---|---|---|---|
| `/` | **100** | **100** | **100** |
| `/lessons` | **100** | **100** | **100** |

(Chỉ "Agentic Browsing" = 67 — audit mới của Lighthouse, không phải WCAG/app.)

### A4. Emulate mobile 360 + dark

| Kiểm tra | Đo được |
|---|---|
| viewport 360×800 mobile | overflow **0px** |
| `prefers-color-scheme: dark` | app vẫn nền sáng `#FFFDF5` — **đúng thiết kế** (design system là Light Mode theo prompt) |
| tap target <24px | **1** — là **inline text link** "đăng nhập" (WCAG 2.5.8 **miễn** inline link; ui-sweep báo `smallTargets: 0`) |

## B. playwright MCP — tương tác thật, 5 vùng chính

| # | Kịch bản | Kết quả | Bằng chứng |
|---|---|---|---|
| B1 | Đăng nhập `user@gmail.com` | 200 → redirect `/lessons` | snapshot |
| B2 | `/profile` — streak panel + "Lịch học" | render đúng | `browser_find` |
| B3 | `/lessons/447` — 3 tab Nội dung/Bài tập/Lịch sử | tablist đủ 3 tab, đúng role | snapshot |
| B4 | Tab "Bài tập" — 8 câu, điền "are" → **✅ Đúng** + "Đáp án: are" | chấm đúng end-to-end | `browser_find` |
| B5 | MC-fragment thiếu options | UI fallback "nhập đáp án" + note — **đúng thiết kế** (AGENTS.md) | snapshot |
| B6 | `/search` — tra "hello" | 200, đủ noun/verb/interjection + Syn/Ant + IPA `/həˈləʊ/` | `browser_find` |
| B7 | `/search` guard <2 ký tự | nút "Tra từ" **disabled** | snapshot |
| B8 | `/videos/1` — YouTube embed | a11y snapshot có **"Play video"**; 3 tab Phụ đề/Shadowing/Quiz | snapshot |
| B9 | `/admin/exercises` khi là **student** | **bị đá về `/`** — guard đúng | snapshot |
| B10 | `/admin/exercises` khi là **admin** | 20 bài/trang, tổng **43738**, phân trang **2187 trang**, nút Sửa/Xóa, search + filter | snapshot |
| B11 | Console errors toàn phiên | **0** | `browser_console_messages` |

**Screenshot:** `shots/pw-lesson-447-exercises.png`, `shots/pw-search-hello.png`, `shots/pw-admin-exercises.png`.

## C. Đối chiếu chéo 2 engine

| Hạng mục | chrome-devtools | playwright | Khớp? |
|---|---|---|---|
| Font duy nhất BVP | 1 font | 0 console error, render BVP | ✓ |
| Token/design system | khớp hết | UI render đúng token | ✓ |
| Overflow | 0px @1440 & 360 | 0 | ✓ |
| Guard admin (student bị đá) | — | bị đá về `/` đúng | ✓ |
| Console error | — | 0 | ✓ |

## D. Kết luận

- **Design system đồng bộ 100%** với prompt "Playful Geometric": font BVP duy nhất, token khớp,
  hard shadow, border 2px, pill button, decoration literal đủ (circle/blob/connector/squiggle/dot).
- **5 vùng chính đều tương tác được** trên UI thật: auth, lessons/exercises, streak, search, CRUD admin.
- **0 console error**, Lighthouse a11y/best-practice/SEO **100/100/100**.
- Guard 2 chiều đúng (student bị đá khỏi `/admin/*`; anon bị đá khỏi route cần auth).
