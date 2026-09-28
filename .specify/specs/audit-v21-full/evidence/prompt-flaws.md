# audit-v21-full — lỗ hổng trong prompt gốc (đo lại, không chép)

Prompt người dùng dán vào chứa design system "Playful Geometric". Dưới đây là các **lỗi đo được**
(không suy đoán) — mỗi lỗi kèm bằng chứng và cách sửa. Đây là cơ sở cho `prompt-rewritten-v21.md`.

## Lỗi 1 — "slate-800 on off-white/white, which is AAA" — SAI một phần

Prompt khẳng định cả palette đạt AAA. Thực tế chỉ `#1E293B` trên `#FFFDF5` mới AAA. Các cặp khác
**fail**:

| Cặp | Tỉ lệ đo | Ngưỡng | Kết luận |
|---|---|---|---|
| `#1E293B` trên `#FFFDF5` | **14.36:1** | 7:1 (AAA) | ✓ AAA |
| white trên `#8B5CF6` (accent) | **4.23:1** | 4.5:1 (AA) | ✗ **FAIL AA** |
| white trên `#7C3AED` (accent-strong) | **5.70:1** | 4.5:1 | ✓ AA |
| `#8B5CF6` làm chữ trên `#FFFDF5` | **4.16:1** | 4.5:1 | ✗ **FAIL** |

Công thức: WCAG relative-luminance. **Sửa:** dùng lớp `--geo-*-ink` cho chữ trên nền sáng, `--geo-*-strong`
cho nền vivid dưới chữ trắng. Không bao giờ white-on-vivid-accent. (Đã hiện thực trong `design-system.css`.)

## Lỗi 2 — "Scale Ratio: 1.25 (Major Third)" — KHÔNG đều

Đo các bước trong `tailwind.config.js:87-99`: `0.75 → 0.875 → 1 → 1.125 → 1.25 → 1.5 → 1.875 → 2.25 → 3 → 3.75`.
Tỉ lệ từng bước: **1.167, 1.143, 1.125, 1.111, 1.200, 1.250, 1.200, 1.333, 1.250** — dao động 1.111–1.333.
**Sửa:** ghi "≈ Major Third, không đồng nhất".

## Lỗi 3 — "Iconography — Lucide React settings" — SAI package

Stack dùng Vue 3, không phải React. Đo: `grep -rn "lucide-react"` = **0 hit**;
`package.json:16` = **`lucide-vue-next ^0.368.0`**. **Sửa:** `lucide-vue-next`, stroke-width 2.5 qua `.lucide { stroke-width: 2.5 }`.

## Lỗi 4 — "Body: Plus Jakarta Sans / Headings: Outfit" — KHÔNG áp dụng

Hiến pháp **P6** quy định **Be Vietnam Pro là font DUY NHẤT**. Đo: **0** tham chiếu sống tới Outfit/Plus
Jakarta (1 hit duy nhất là comment lịch sử trong `index.html`). `tailwind.config.js:81-85` (sans/heading/mono)
và `design-system.css:88` (`--geo-font`) đều trỏ Be Vietnam Pro; `index.html:54` nạp BVP 400–900.
**Sửa:** không thêm font thứ hai; không khôi phục Outfit/Plus Jakarta/Inter/Poppins.

## Khoảng trống literal (không phải drift — OPTIONAL)

Prompt nêu *"Polka dots, grid lines, and diagonal stripes"*. Repo có `DotBackground` (dot grid),
`SquiggleDivider`, `DecoConfetti`, `DecoShape` — nhưng **không** có utility polka/diagonal-stripe.
Đo: `grep -rni "polka\|diagonal\|stripes" frontend/src/` = **0 hit**. Ghi nhận OPTIONAL; chỉ fix khi có
sai lệch đo được (hiến pháp: không thêm feature không được yêu cầu).

## Lỗi 5 (mới, v21) — "Ask the user focused questions ... do the following" — mâu thuẫn tự thân

Prompt vừa yêu cầu agent **hỏi người dùng** trước khi làm, vừa ép một design system cố định. Trong ngữ
cảnh dự án đã có hiến pháp P6/P7, "hỏi rồi mới làm" tạo vòng lặp vô nghĩa. **Sửa:** khi hiến pháp đã
quyết, ghi rõ "verify against P6/P7, không hỏi lại điều đã luật hoá".

## Lỗi 6 (mới, v21) — "Accessibility: The text is slate-800 on off-white/white" — bỏ sót nền khác

Prompt chỉ xét 2 nền (off-white/white). Thực tế UI có nền `--geo-muted #F1F5F9` (panel), nền vivid
(accent/secondary/tertiary/quaternary), và tint `/10–/30`. Đo: `--geo-muted-fg` từng là `#64748B` →
4.34:1 trên muted (**FAIL**) — đã sửa thành `#556070` (audit-v11 F138). **Sửa:** rule contrast phải
tính cả nền muted + vivid + tint, không chỉ trắng/cream.
