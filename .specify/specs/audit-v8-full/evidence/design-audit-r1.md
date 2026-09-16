# Plan B — Be Vietnam Pro design-system migration: verification

**Ngày đo:** 2026-09-16 · **Harness:** `sweep/v8/ui/design-v2.js` (playwright-core + Chromium thật)
**Kết quả thô:** `sweep/v8/ui/design-v2.txt`, `sweep/v8/ui/design-v2.json`

## Yêu cầu (theo audit prompt + constitution P6)

1. Be Vietnam Pro là font **DUY NHẤT** — không còn Outfit / Plus Jakarta Sans.
2. Đo tại **5 viewport**: 360 / 768 / 1280 / 1440 / 1920.
3. Không vỡ layout (horizontal overflow) ở bất kỳ viewport nào.

## Kết quả

| Hạng mục | Cách đo | Kết quả |
|---|---|---|
| Webfont thật sự LOADED (không chỉ khai báo) | `document.fonts.check()` sau `document.fonts.ready` cho weight 400/700/900 | **true / true / true** — 15 face Be Vietnam Pro |
| Font computed trên element thật | quét `h1..h6,p,a,button,span,label,li,td,th,input,textarea,select,strong,em,small,code` đang hiển thị, lấy `getComputedStyle().fontFamily` | **7 685 element, 0 element không phải Be Vietnam Pro** |
| Font cũ còn sót trong cascade | tìm chuỗi `Outfit`, `Plus Jakarta Sans`, `PlusJakarta`, `Inter`, `Roboto`, `Poppins` trong `fontFamily` đầy đủ | **0 hit** |
| Horizontal overflow | `documentElement.scrollWidth - clientWidth`, ngưỡng nhiễu scrollbar ~15 px (AGENTS.md) | **0/60 route×viewport vượt ngưỡng** |
| Token design system | `getComputedStyle(:root).getPropertyValue()` | `--geo-font` = `'Be Vietnam Pro', system-ui, sans-serif` |

**Phạm vi quét:** 12 route × 5 viewport = 60 tổ hợp.

Route: `/`, `/lessons`, `/lessons/445`, `/decks`, `/speaking`, `/premium`, `/login`,
`/leaderboard`, `/videos`, `/admin/dashboard`, `/admin/lessons`, `/admin/exercises`.

## Nguồn khai báo font (đã đối chiếu tĩnh)

- `frontend/index.html` — `<link>` Google Fonts `family=Be+Vietnam+Pro:wght@400;500;600;700;900&display=swap` + `preconnect` tới `fonts.googleapis.com` / `fonts.gstatic.com`.
- `frontend/tailwind.config.js` — `fontFamily.sans`, `.heading`, **và `.mono`** đều trỏ về `"Be Vietnam Pro"` (audit-v5 gỡ hẳn nhánh monospace riêng ⇒ single-family).
- `frontend/src/assets/design-system.css` — `--geo-font: 'Be Vietnam Pro', system-ui, sans-serif;` + 30 token `--geo-*` (màu, thang chữ, radius).
- Chỉ còn **comment** nhắc tới Plus Jakarta Sans (`frontend/index.html`, `design-system.css`) giải thích việc đã gỡ — không có khai báo thật.

## Kết luận

**Plan B ĐẠT.** Be Vietnam Pro là font duy nhất trên toàn bộ 60 tổ hợp route×viewport,
webfont load thật ở cả 3 weight kiểm tra, không còn font cũ trong cascade, không vỡ layout
ngang ở viewport hẹp nhất (360 px) lẫn rộng nhất (1920 px).

### Ghi chú về ngưỡng overflow

`overflow = -15` ở mọi tổ hợp là **giá trị âm hợp lệ**: Chromium dành ~15 px cho scrollbar
nên `scrollWidth < clientWidth`. Đây chính là false-positive mà AGENTS.md cảnh báo
("đừng tin `scrollWidth > clientWidth` một mình") — harness đã dùng ngưỡng `> 16 px`
thay vì `> 0` để không đếm nhầm.
