# audit-v19-full — T0.8: prompt flaw audit (đo lại, không chép)

**Nguồn:** prompt người dùng dán + bản chuẩn `.specify/specs/audit-v18-full/prompt-rewritten-v18.md`.
Mọi số **đo lại phiên này** (WCAG relative-luminance + grep).

## Kết luận: 4 lỗ hổng vẫn y nguyên (prompt là bản cũ đã regress)

| # | Prompt gốc | Đo phiên này | Sửa |
|---|---|---|---|
| A | *"text is slate-800 on off-white/white, which is AAA"* | `#1E293B`/`#FFFDF5` = **14.36** ✅ (chỉ cặp này); trắng/`#8B5CF6` = **4.23 FAIL AA**; trắng/`#F472B6` = **2.65**; `#8B5CF6` text/cream = **4.16 FAIL** | Luật 2 tầng: vivid = fill; `*-ink` = text trên nền sáng; `*-strong` = nền dưới chữ trắng |
| B | *"Scale Ratio: 1.25 (Major Third)"* | bước đo `1.167 1.143 1.125 1.111 1.200 1.250 1.200 1.333 1.250` → **không hằng số** | "≈ Major Third, không đều" |
| C | *"Lucide **React**"* | `grep lucide-react` = **0**; stack dùng `lucide-vue-next` | `lucide-vue-next`, stroke 2.5 |
| D | *body "Plus Jakarta Sans" / Outfit* | word-boundary = **1** (comment lịch sử `index.html:32`), code = 0 | Be Vietnam Pro là font duy nhất (P6) |
| E | *"thay toàn bộ font thành Be Vietnam Pro"* | font **đã là** BVP duy nhất (`index.html:54`, `tailwind.config.js`, `--geo-font`) | **verify + guard**, KHÔNG migrate |
| F | *"polka dots / diagonal stripes"* pattern fill | repo có dot grid + squiggle, **không** có polka/diagonal utility | OPTIONAL GAP (không phải drift) |

→ Viết `prompt-rewritten-v19.md` (kế thừa v18 + ghi rõ E/F). Dùng làm chuẩn đối chiếu Phase 3/D.
