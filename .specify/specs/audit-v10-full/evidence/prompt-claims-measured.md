# Phase 2.11 — Đo từng claim của prompt trên UI thật (2026-09-20 00:1x +07)

Công cụ: `sweep/v10/prompt-claims.js` — Edge thật (playwright-core + `executablePath`), 5 viewport.
**Kết quả cuối: PASS=184, FAIL=0.** Parity sau sweep: `1471|43737|76|127|28|15|4|126|14|5` (không đổi).

## Vì sao cần script mới thay vì dùng `design-v2.js` của v8

`design-v2.js` kiểm **nền tảng**: font Be Vietnam Pro có load thật không, token `--geo-*` có đúng giá trị không, lucide stroke = 2.5, shadow mobile = 2px. Đó là "Plan B" của v8 — **không phải** các claim cụ thể của prompt.

Script mới đo đúng những câu prompt nói:

| Câu trong prompt | Cách đo |
|---|---|
| `Container: max-w-6xl` | `getBoundingClientRect().width` của `.geo-container` + `max-width` computed |
| `Spacing: py-24 (96px)` | `paddingTop` computed của `.app-hero` |
| `A massive yellow circle behind the text` | `.app-hero__sun` — màu, border-radius, kích thước |
| `The image itself has a blob mask` | `.app-hero__shape--blob` — border-radius, có hiện không |
| `Each card is connected by a dashed SVG line` | `stroke-dasharray` trên `<path>` + ẩn/hiện theo viewport |
| `middle card scaled up (1.1)` | `transform` computed của `.app-plan--featured` |
| `badge ... rotated 15deg` | `transform` computed của `.app-plan__badge` |
| `Use infinite scrolling text` | `animation-iteration-count` của `.app-marquee__track` |
| `ArrowRight, circular background (white)` | `.app-btn__arrow` — nền trắng, radius tròn, stroke 2.5 |

## Kết quả theo viewport

| Viewport | PASS | Ghi chú |
|---|---|---|
| 360px | 36 | connector ẩn đúng, pricing không scale đúng thiết kế |
| 768px | 37 | connector hiện, pricing bắt đầu scale |
| 1280px | 37 | container đạt đúng 1152px |
| 1440px | 37 | container đạt đúng 1152px |
| 1920px | 37 | container đạt đúng 1152px |

Container **đo được đúng 1152px** ở cả 3 viewport ≥1280 — không phải "khai báo max-width rồi tin", mà là số đo thật từ layout engine.

## Ba FAIL ban đầu — cả ba đều là LỖI PROBE, không phải lỗi UI

Lần chạy đầu: **144 PASS / 15 FAIL** (3 loại × 5 viewport). Đã điều tra từng loại bằng `sweep/v10/diag-buttons.js` trước khi sửa, để không "sửa" một thứ đang đúng.

### 1. `connector la SVG dashed -> stroke-dasharray=null`

**Nguyên nhân:** `stroke-dasharray` nằm trên `<path>` **bên trong** `<svg>`, không phải trên `<svg>`. Đọc attribute ở `<svg>` trả `null`.

**Bằng chứng đo lại:**
```
svgAttr: null
paths[0]: { d: "M0 30 H1000", stroke: "var(--geo-fg)", width: "2",
            dasharrayAttr: "10 10", dasharrayComputed: "10px, 10px",
            strokeComputed: "rgb(30, 41, 59)" }
```
Đường dashed **có thật và đúng**. Probe đọc sai element.

### 2. `nut primary co hard shadow -> shadow=none`

**Nguyên nhân:** probe dùng `document.querySelector(".app-btn")` — **phần tử đầu tiên** trong DOM. Trên trang đã đăng nhập, nút đầu tiên là nút **"Thoát"** ở header, mà đó là `app-btn--secondary`. Prompt ghi rõ secondary: `Shadow: none`.

**Bằng chứng đo lại (`diag-buttons.js` @1440px):**
```
[0] app-btn--secondary  "Thoát"          shadow=none                      header=true
[2] app-btn--primary    "Bắt đầu ngay"   shadow=rgb(30, 41, 59) 4px 4px   header=false
[9] app-btn--primary    "Đăng ký miễn phí" shadow=rgb(30, 41, 59) 4px 4px arrow=true
```
Nút primary **có** hard shadow đúng. Probe đo nhầm đối tượng.

### 3. `nut primary co icon (arrow affordance) -> khong co svg`

**Nguyên nhân kép:**
- Cùng lỗi chọn element như trên (nút "Thoát" không có arrow).
- Và `with-arrow` là **opt-in** (`AppButton.vue` dòng 28: `withArrow: { type: Boolean, default: false }`) — comment trong source giải thích: *"~100 existing call sites must not change shape"*. Nên assertion đúng phải là **"có ít nhất một nút primary dùng affordance này"**, không phải "mọi nút primary đều có".

**Sửa cả hai:** chọn `.app-btn--primary`, và đổi assertion sang `withArrow >= 1`.

## Sau khi sửa probe — 0 FAIL

Các assert mới được thêm và đều PASS:
- arrow có nền **trắng** (`rgb(255, 255, 255)`) — đúng câu prompt *"circular background (white) inside button"*
- arrow là **hình tròn**
- arrow dùng **icon ArrowRight** (svg có thật)
- arrow **stroke = 2.5** — khớp *"Stroke Width: 2.5px (Bold/Chunky)"*

## Bài học phương pháp

Một probe sai element **không** tạo ra false negative vô hại — nó tạo ra **kết luận sai rằng UI hỏng**, và cách "sửa" nguy hiểm nhất là đi sửa CSS cho khớp probe. Ở đây cả 3 FAIL đều biến mất khi đọc đúng element, và **không một dòng CSS nào bị thay đổi**. Quy tắc: khi probe báo lỗi, kiểm tra probe trước khi kiểm tra sản phẩm — và ghi lại bằng chứng của lần kiểm tra đó, như mục này.

## Giới hạn phải ghi rõ

- Browser là **Microsoft Edge** (`C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`), không phải Chrome. Cùng engine Chromium nên mọi phép đo layout/CSS/computed-style đều hợp lệ. **Không được trộn** số đo này với screenshot Chromium của v8 để so sánh pixel.
- `sweep/v8/ui/*.js` gọi `H.pw.chromium.launch()` trực tiếp nên **không chạy được** trên máy này (thiếu Chromium của playwright). Đã tạo `sweep/v10/ui-lib.js` bọc lại, truyền `executablePath`. Các script v10 dùng wrapper này; script v8 thì không — ghi lại để vòng sau không mất thời gian chẩn đoán lại.
