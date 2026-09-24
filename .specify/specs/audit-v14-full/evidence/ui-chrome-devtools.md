# audit-v14-full — Phase 3: chrome-devtools MCP driver (T3.2)

**Ngày:** 2026-09-25 (+07) · **Driver:** `chrome-devtools-mcp` (Chrome DevTools Protocol)
**Mục đích:** driver thứ HAI độc lập với Playwright — đo token/font/console/network/Lighthouse trên
cùng stack thật. Số của driver nào ghi rõ driver đó (không trộn 2 engine).

## Design-system + font (live DOM, `evaluate_script`)

Route `/`:

```json
{"bodyFont":"\"Be Vietnam Pro\", system-ui, sans-serif","checkBeVietnam":true,
 "bodyBg":"rgb(255, 253, 245)","geoBg":"#FFFDF5","geoFg":"#1E293B","geoAccent":"#8B5CF6",
 "geoBorderWidth":"2px","geoRadiusMd":"16px","geoShadowMd":"4px 4px 0px 0px #1E293B",
 "hasLucide":true,"lucideStroke":"2.5px","skipLink":true,"scrollGutter":"stable","fontCount":18}
```

→ Font **Be Vietnam Pro** (`document.fonts.check` = **true**); token khớp prompt; border **2px**;
shadow cứng **4px 4px 0 #1E293B**; lucide stroke **2.5px**; skip-link có; scrollbar-gutter stable.

## Console health

`/` — `list_console_messages` (error+warn): **không có message nào**.

## A11y (live DOM)

`/lessons`:

```json
{"imgs":12,"missingAlt":0,"h1":1,"headingSkip":null,"lang":"vi","ovf":0,
 "title":"EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí"}
```

→ 12 ảnh / **0 thiếu alt**; **1 `h1`**; **không skip heading**; `lang="vi"`; **0 overflow**.

`/login`:

```json
{"inputs":[{"type":"email","w":380,"h":54},{"type":"password","w":380,"h":54},{"type":"checkbox","w":16,"h":16}],
 "labels":3,"submitH":52}
```

→ input 54px, submit 52px (đều > 44px); checkbox 16×16 (nhỏ — đưa vào tap-target triage).

## Lighthouse (chrome-devtools MCP, desktop)

| Route | Accessibility | Best Practices | SEO | Agentic |
|---|---|---|---|---|
| `/` | **100** | **100** | **100** | 67 |
| `/lessons` | **100** | **100** | **100** | 67 |
| `/login` | **100** | **100** | 66 | 67 |

**Ghi nhận giới hạn host:** Lighthouse **không phát category Performance** trên host này (như v13 đã ghi)
→ perf đo trực tiếp bằng `perf-probe.js` (Phase 5). Đây là giới hạn host, không phải lỗi app.

Reports: `evidence/lh-home/`, `evidence/lh-lessons/`, `evidence/lh-login/` (`report.json` + `report.html`).
