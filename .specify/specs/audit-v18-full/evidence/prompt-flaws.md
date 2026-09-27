# audit-v18-full — T0.8: prompt flaw audit (Playful Geometric + Be Vietnam Pro)

**Nguồn:** prompt người dùng dán vào phiên này (role/design-system/accessibility + "thay toàn bộ font
thành Be Vietnam Pro"), và bản chuẩn kế thừa `.specify/specs/audit-v17-full/prompt-rewritten-v17.md`.
**Mọi con số dưới đây ĐO LẠI trong phiên này** bằng công thức WCAG relative-luminance (script Node) và grep
source — KHÔNG chép từ v17.

## Kết luận ngắn

Prompt dán vào là **bản cũ đã regress**: nó lặp lại **4 khẳng định SAI** mà codebase đã sửa từ audit-v11
(contrast) và `prompt-rewritten-v10/v17` (lucide-vue-next, font). Dùng nguyên bản làm chuẩn sẽ **tái tạo lỗi
đã fix**. Yêu cầu mới "thay toàn bộ font thành Be Vietnam Pro" là **verify + guard** — font đã đúng.

## Lỗ hổng đo được

### A. Contrast — khẳng định "AAA" SAI (nghiêm trọng nhất) — ĐO LẠI, khớp v17

Prompt viết: *"The text is slate-800 on off-white/white, which is AAA"* và dùng `accent` làm nền chữ trắng.

| Cặp | Tỉ lệ (đo phiên này) | Ngưỡng | Verdict |
|---|---|---|---|
| `#1E293B` (fg) trên `#FFFDF5` (bg) | **14.36** | ≥ 7 AAA | ✅ đúng (chỉ cặp này) |
| `#FFFFFF` trên `#8B5CF6` (accent) | **4.23** | ≥ 4.5 AA body | ❌ **FAIL AA** |
| `#FFFFFF` trên `#F472B6` (secondary) | **2.65** | ≥ 3 AA-large | ❌ **FAIL cả AA-large** |
| `#8B5CF6` (accent) làm TEXT trên `#FFFDF5` | **4.16** | ≥ 4.5 | ❌ **FAIL** |
| `#FFFFFF` trên `#7C3AED` (accent-strong) | **5.70** | ≥ 4.5 | ✅ (codebase đã dùng) |
| `#FFFFFF` trên `#DB2777` (secondary-strong) | **4.60** | ≥ 4.5 | ✅ |
| `#6D28D9` (accent-ink) trên `#FFFDF5` | **6.98** | ≥ 4.5 | ✅ |
| `#556070` (muted-fg) trên `#F1F5F9` | **5.82** | ≥ 4.5 | ✅ |
| `#1E293B` trên `#FBBF24` (tertiary) | 8.76 | ≥ 7 AAA | ✅ |
| `#1E293B` trên `#34D399` (quaternary) | 7.61 | ≥ 7 AAA | ✅ |

→ "AAA" chỉ đúng cho cặp fg/bg. **White-on-accent và white-on-secondary đều FAIL.** Codebase đã sửa bằng
lớp `--geo-*-ink` (text trên nền sáng) + `--geo-*-strong` (nền dưới chữ trắng) — xem
`frontend/src/assets/design-system.css:76-83`.

**Sửa trong prompt doc:** thay "AAA" bằng luật hai tầng: *vivid token = fill/border/decoration; `*-ink` =
text trên nền sáng; `*-strong` = nền dưới chữ trắng.*

### B. Type scale — "1.25 (Major Third)" SAI — ĐO LẠI

Bước (rem): `0.75, 0.875, 1, 1.125, 1.25, 1.5, 1.875, 2.25, 3, 3.75` → tỉ lệ liên tiếp:
```
1.167  1.143  1.125  1.111  1.200  1.250  1.200  1.333  1.250
```
→ **Không phải hằng số 1.25** (1.111–1.333). Prompt mô tả sai.

### C. "Lucide React" SAI stack — ĐO LẠI

`grep -ri "lucide-react" frontend/src frontend/package.json` = **0 hit**. `frontend/package.json` có
**`lucide-vue-next`** (1 hit). React package không áp dụng cho Vue.

### D. Font — "Plus Jakarta Sans"/"Outfit" — ĐO LẠI

`grep -rniE "\b(Outfit|Plus Jakarta Sans|Poppins|Inter)\b" frontend/src frontend/index.html frontend/tailwind.config.js`
= **1 hit duy nhất**, và đó là **comment lịch sử** ở `frontend/index.html:32` ("Plus Jakarta Sans (dead
payload) removed"). **0 hit trong code.** `tailwind.config.js` fontFamily: `sans`/`heading`/`mono` đều →
Be Vietnam Pro. (Lưu ý: grep thô `Inter` bắt nhầm "po**inter**-events" — phải dùng word-boundary.)

### E. "Thay toàn bộ font thành Be Vietnam Pro" — VERIFY, không phải migrate

Đo phiên này: `frontend/index.html` load `Be+Vietnam+Pro:wght@400;500;600;700;800;900`; `--geo-font` =
`'Be Vietnam Pro', system-ui, sans-serif`; `tailwind.config.js` map cả 3 family về BVP. **Font đã là BVP duy
nhất** → yêu cầu này là **verify + guard** (đảm bảo không có family thứ hai lọt vào), KHÔNG phải đổi font.

### F. Khoảng trống literal TÙY CHỌN (không phải drift)

Prompt nêu pattern fill *"polka dots … diagonal stripes"*. Codebase có `DotBackground` (dot grid),
`SquiggleDivider`, `DecoConfetti`, `DecoShape`, marquee — **nhưng không có pattern polka/diagonal-stripe**
dạng utility. Đây là khoảng trống literal tùy chọn, KHÔNG phải lệch chuẩn (R6: chỉ fix khi đo được lệch).
Ghi nhận, không bắt buộc làm.

### G. Đúng và đã khớp (giữ nguyên)

Hard shadow `0px` blur; border 2px `#1E293B`; radius 8/16/24/full; palette cream/violet/pink/amber/emerald;
motion `cubic-bezier(0.34,1.56,0.64,1)`; hover `translate(-2px,-2px)`; `prefers-reduced-motion` — tất cả có
trong `design-system.css` + `tailwind.config.js`.

## Việc phải làm (T0.10)

Viết `prompt-rewritten-v18.md` = bản đã sửa 4 lỗ hổng (A–D) + ghi rõ (E) là verify + (F) là optional gap,
dùng làm **chuẩn đối chiếu** cho Phase 3 và Phase D.
