# audit-v17-full — T0.8: prompt flaw audit (Playful Geometric + Be Vietnam Pro)

**Nguồn:** prompt người dùng dán vào phiên này (role/design-system/accessibility), và bản chuẩn trước đó
`.specify/specs/audit-v10-full/prompt-rewritten-v10.md`. Mọi con số dưới đây **đo trong phiên này** bằng công thức
WCAG relative-luminance (script Node) và grep source — không chép lại.

## Kết luận ngắn

Prompt dán vào là **bản cũ đã regress**: nó chứa 4 khẳng định SAI mà codebase đã sửa từ audit-v11 (contrast) và
`prompt-rewritten-v10` (lucide-vue-next). Nếu dùng nguyên bản để làm chuẩn đối chiếu sẽ **tái tạo lỗi đã fix**.

## Lỗ hổng đo được

### A. Contrast — khẳng định "AAA" SAI (nghiêm trọng nhất)

Prompt viết: *"The text is slate-800 on off-white/white, which is AAA"* và *"Use `accent` for primary actions …
white text"*.

Đo (WCAG contrast ratio):

| Cặp | Tỉ lệ | Ngưỡng | Verdict |
|---|---|---|---|
| `#1E293B` (fg) trên `#FFFDF5` (bg) | **14.36** | ≥ 7 AAA | ✅ đúng (chỉ cặp này) |
| `#FFFFFF` trên `#8B5CF6` (accent) | **4.23** | ≥ 4.5 AA body | ❌ **FAIL AA** |
| `#FFFFFF` trên `#F472B6` (secondary) | **2.65** | ≥ 3 AA-large | ❌ **FAIL cả AA-large** |
| `#8B5CF6` (accent) làm TEXT trên `#FFFDF5` | **4.16** | ≥ 4.5 | ❌ **FAIL** |
| `#1E293B` trên `#FBBF24` (tertiary) | 8.76 | ≥ 7 AAA | ✅ |
| `#1E293B` trên `#34D399` (quaternary) | 7.61 | ≥ 7 AAA | ✅ |
| `#64748B` (muted-fg CŨ) trên `#F1F5F9` (muted) | **4.34** | ≥ 4.5 | ❌ **FAIL** |

→ "AAA" chỉ đúng cho cặp fg/bg. **White-on-accent và white-on-secondary đều FAIL.** Đây đúng là lỗi audit-v11 F132.

**Codebase đã sửa:** lớp `--geo-*-ink` (text trên nền sáng) và `--geo-*-strong` (nền dưới chữ trắng):
`--geo-accent-strong #7C3AED` (white-on-it = **5.70** ✅), `--geo-accent-ink #6D28D9` (7.10 ✅),
`--geo-secondary-strong #DB2777` (4.60 ✅), `--geo-muted-fg #556070` (6.38/6.26/5.82 ✅).
Xem `frontend/src/assets/design-system.css:43-83`, `frontend/tailwind.config.js`.

**Sửa trong prompt doc:** thay khẳng định "AAA" bằng luật hai tầng: *vivid token = fill/border/decoration;
`*-ink` = text trên nền sáng; `*-strong` = nền dưới chữ trắng.*

### B. Type scale — "1.25 (Major Third)" SAI

Prompt viết: *"Scale Ratio: 1.25 (Major Third)"*.

Đo tỉ lệ các bước trong `design-system.css:91-100` / `tailwind.config.js` fontSize:

```
0.75→0.875 = 1.167   0.875→1 = 1.143   1→1.125 = 1.125   1.125→1.25 = 1.111
1.25→1.5 = 1.200     1.5→1.875 = 1.250  1.875→2.25 = 1.200  2.25→3 = 1.333   3→3.75 = 1.250
```

→ **Không phải hằng số 1.25** (1.111–1.333). Prompt mô tả sai scale thực tế.

**Sửa:** ghi đúng là thang đo gần Major Third nhưng **không đều**; hoặc chấp nhận và ghi "xấp xỉ".

### C. "Lucide React" SAI stack

Prompt viết: *"**Lucide React** settings: Stroke Width 2.5px"*. Stack dùng **`lucide-vue-next`** (`frontend/package.json`).
React package không áp dụng cho Vue. (Đã sửa trong `prompt-rewritten-v10.md`; prompt dán vào regress.)
**Đúng:** `lucide-vue-next`, stroke-width 2.5 qua `.lucide { stroke-width: 2.5 }` (`design-system.css:7-9`).

### D. Font — "Plus Jakarta Sans" / "Outfit"

Prompt gốc viết headings `Outfit`, body `Plus Jakarta Sans`. **Hiến pháp P6 cấm**; Be Vietnam Pro là font duy nhất
(`frontend/index.html:54`, `--geo-font`). Người dùng đã tự thay trong tin nhắn → **giữ Be Vietnam Pro**.
Đo: `Outfit` = 0 hit, `Plus Jakarta Sans` = 0 load (chỉ comment lịch sử).

### E. Signature thiếu (mức thấp, tùy chọn)

Prompt nêu pattern fill *"polka dots … diagonal stripes"*. Codebase có `DotBackground` (dot grid), `SquiggleDivider`,
`DecoConfetti`, `DecoShape`, marquee — **nhưng không có pattern fill polka/diagonal-stripe** dạng utility.
Đây là **khoảng trống literal tùy chọn**, KHÔNG phải lỗi drift (v16 R6: chỉ fix khi đo được lệch chuẩn).

### F. Đúng và đã khớp (giữ nguyên)

Hard shadow `0px` blur; border 2px `#1E293B`; radius 8/16/24/full; palette cream/violet/pink/amber/emerald;
motion `cubic-bezier(0.34,1.56,0.64,1)`; hover `translate(-2px,-2px)` + shadow tăng; `prefers-reduced-motion` —
tất cả **có trong code** (`design-system.css`, `tailwind.config.js`).

## Việc phải làm (T8.2)

Viết `prompt-rewritten-v17.md` = bản v10 (đã đúng stack/font) + **thêm mục contrast hai tầng** + **sửa type-scale**
+ ghi rõ đây là **chuẩn đối chiếu**, kèm bảng token đã đo. Không để lại khẳng định "AAA" trần, "Lucide React",
"Plus Jakarta Sans".
