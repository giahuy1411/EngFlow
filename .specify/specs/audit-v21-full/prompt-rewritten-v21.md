# prompt-rewritten-v21 — Playful Geometric integration prompt (audit-v21 edition)

Thay thế `prompt-rewritten-v20.md`. Mô tả codebase **SAU** vòng v21 và **sửa các lỗi đo được**
trong prompt gốc (xem §Prompt corrections). Đây là **chuẩn conformance** để audit giao diện,
KHÔNG phải prompt dán gốc.

> **Ghi chú v21:** vòng này KHÔNG thay font và KHÔNG dựng lại design system — cả hai đã xong từ
> audit-v5 và được hiến pháp **P6** luật hoá. Việc của vòng là **verify bằng browser thật** (P8)
> và sửa lỗi prompt. Mọi số dưới đây đo trong phiên này.

**Font: Be Vietnam Pro là font DUY NHẤT.** Không có font thứ hai, không fallback cũ. Lệnh
"thay toàn bộ font thành Be Vietnam Pro" là việc **verify + guard**: font đã đúng, nên nhiệm vụ
là chứng minh không font nào lọt vào — không phải swap.

## Prompt corrections (đo được, luỹ kế)

| # | Prompt gốc nói | Thực tế đo được | Luật đã sửa |
|---|---|---|---|
| 1 | *"text is slate-800 on off-white/white, which is AAA"* | Chỉ `#1E293B`/`#FFFDF5` = 14.36:1 mới AAA. White trên `#8B5CF6` = **4.23:1 (FAIL AA)**; `#8B5CF6` làm chữ trên cream = **4.16:1 (FAIL)**. | **Luật 2 tầng:** vivid `--geo-accent/-secondary/-tertiary/-quaternary` = fill/border/shape/tint `/10–/30`. **Chữ trên nền sáng → `--geo-*-ink`.** **Nền vivid dưới chữ trắng → `--geo-*-strong`.** Không bao giờ white-on-vivid-accent. |
| 2 | *"Scale Ratio: 1.25 (Major Third)"* | Bước thật 1.111–1.333 (đo: 1.167, 1.143, 1.125, 1.111, 1.200, 1.250, 1.200, 1.333, 1.250). Không đồng nhất. | Ghi "≈ Major Third, không đồng nhất". |
| 3 | *"Iconography — **Lucide React** settings"* | Stack dùng **`lucide-vue-next ^0.368.0`** (`package.json:16`); `lucide-react` = **0 hit**. Package React không áp dụng. | `lucide-vue-next`, stroke-width 2.5 qua `.lucide { stroke-width: 2.5 }`. |
| 4 | *"Body: Plus Jakarta Sans" / "Headings: Outfit"* | **Be Vietnam Pro là font duy nhất** (P6). Đo runtime: `distinctFonts = ["Be Vietnam Pro"]`. | Không thêm font thứ hai; không khôi phục Outfit / Plus Jakarta / Inter / Poppins. |
| 5 | *"Ask the user focused questions"* trước khi làm | Mâu thuẫn: hiến pháp P6/P7 đã quyết. | Khi hiến pháp đã luật hoá → verify theo P6/P7, không hỏi lại. |
| 6 | *"Accessibility: text is slate-800 on off-white/white"* | Chỉ xét 2 nền; bỏ sót nền `--geo-muted` + vivid + tint. `--geo-muted-fg` từng 4.34:1 (**FAIL**) trên muted → đã sửa `#556070`. | Rule contrast phải tính cả nền muted + vivid + tint. |

**Khoảng trống literal (OPTIONAL, không phải drift):** prompt nêu *"polka dots … diagonal stripes"*.
Repo có `DotBackground` (dot grid), `SquiggleDivider`, `DecoConfetti`, `DecoShape` — **không** có
utility polka/diagonal-stripe (`grep polka|diagonal|stripes` = **0 hit**). Ghi OPTIONAL; chỉ fix khi
có sai lệch đo được.

---

<role>
Bạn là senior frontend engineer có chiều sâu UI/UX, visual design và typography. Bạn làm trong
codebase **Vue 3 + Vite + Tailwind** ĐÃ CÓ. Bạn dựng mental model trước, khớp pattern hiện có,
và giải thích mỗi quyết định kiến trúc trong 1–2 câu.
</role>

<codebase_facts verify_these_before_acting>
- Stack: Vue 3 `<script setup>`, Vite 5, Tailwind **3.4** (config-file, KHÔNG phải v4 `@theme`),
  Vitest + jsdom, **`lucide-vue-next`** (package Vue — "Lucide React" là thư viện khác, không áp dụng),
  services trong `frontend/src/services/*.js` sau instance `api` chung.
- Design system ở `frontend/src/assets/design-system.css`; token là CSS var `--geo-*` trên `:root`.
  File đó là nguồn duy nhất của ngôn ngữ hình ảnh. Tailwind lo layout.
- **Container là `max-w-6xl`** (72rem / 1152px) qua `frontend/src/components/layout/Container.vue`
  (`.geo-container`); `size="sm"` (40rem) cho form auth, `size="xl"` cho bảng admin rộng.
- **Nhịp section là `py-24`** qua `frontend/src/components/layout/PageSection.vue` (3rem mobile, 6rem từ `md`).
- Font: **Be Vietnam Pro là font duy nhất** — `frontend/index.html:54`, `tailwind.config.js:81-85`
  (sans/heading/mono đều trỏ BVP), `--geo-font`. Đo: 0 code hit Outfit/Plus Jakarta/Poppins/Inter.
- Decoration ở `frontend/src/components/decor/` (DecoConfetti, DecoShape, DotBackground,
  SquiggleDivider). `SquiggleDivider` tô bằng `mask-image` + `background-color`.
- **KHÔNG đặt comment HTML ở cấp gốc `<template>`** (audit-v21 F-21-03): nó là root node thứ hai →
  Vue fragment → `wrapper.attributes()` = undefined → vỡ test. Comment template đặt TRƯỚC
  `<template>` hoặc trong `<script setup>`.
- Dead CSS là nguy hiểm: lớp `.geo-btn`, `.geo-heading*`, `.geo-body`, `.geo-shadow-*` đã bị gỡ.
  Lớp sống: `.app-btn*`, `.geo-card`, `.geo-markdown`, `.geo-audio`. **Grep class trước khi thêm;
  khi gỡ, grep cả `frontend/src`** — không có gate dead-CSS tự động.
</codebase_facts>

<design_system>
Tên: **Playful Geometric** — "stable grid, wild decoration". Friendly, tactile, pop, energetic.
Nội dung nằm trong khối đọc êm; không gian quanh mang cá tính.

Token — dùng ĐÚNG biến này, không hex thô trong component:

| Mục đích | Biến | Giá trị |
|---|---|---|
| background (kem) | `--geo-bg` | #FFFDF5 |
| foreground | `--geo-fg` | #1E293B |
| muted / muted-fg | `--geo-muted` / `--geo-muted-fg` | #F1F5F9 / **#556070** |
| accent | `--geo-accent` / `--geo-accent-fg` | #8B5CF6 / #FFFFFF |
| secondary (pink) | `--geo-secondary` | #F472B6 |
| tertiary (amber) | `--geo-tertiary` | #FBBF24 |
| quaternary (mint) | `--geo-quaternary` | #34D399 |
| **AA text on light** | `--geo-{accent,secondary,tertiary,quaternary,success,warning,danger}-ink` | #6D28D9 / #BE185D / #B45309 / #047857 / #047857 / #B45309 / #BE123C |
| **vivid surface under white** | `--geo-{accent,secondary}-strong` | #7C3AED / #DB2777 |
| border / input / card | `--geo-border` / `--geo-input` / `--geo-card` | #E2E8F0 / #FFFFFF / #FFFFFF |
| font | `--geo-font` (Be Vietnam Pro) | — |
| radii | `--geo-radius-sm\|md\|lg\|full` | 8 / 16 / 24 / 9999 px |
| hard shadows | `--geo-shadow-xs\|sm\|md\|lg\|xl` | 2/3/4/6/8 px offset, **0 blur** |
| featured shadow | `--geo-shadow-featured` | 8px 8px, `--geo-secondary` |
| border width | `--geo-border-width` | 2px |

Motion: hover = `translate(-2px,-2px)` + shadow lên 1 bậc; active = `translate(2px,2px)` + shadow xuống.
`prefers-reduced-motion: reduce` → decoration tĩnh, transition tắt, marquee tắt hẳn.

### Literal feature của prompt (đo được — verify trước khi đổi)

| Feature | Ở đâu | Ghi chú |
|---|---|---|
| Vòng tròn vàng lớn sau hero | `Home.vue` `.app-hero__sun` | **640px**, `--geo-tertiary` `#FBBF24`, `aria-hidden` |
| Blob mask cho shape hero | `Home.vue` `.app-hero__shape--blob` | radius `60% 40% 55% 45% / 45% 55%` |
| Đường nối nét đứt giữa feature cards | `Home.vue` `.app-features__connector` (SVG inline, `aria-hidden`) | ẩn dưới 768px |
| Card pricing featured + badge xoay | `PremiumPage.vue` `.app-plan--featured` (**scale 1.1**), `.app-plan__badge` (**rotate 15deg**) | chỉ từ `md` lên |
| Squiggle dividers | `SquiggleDivider` giữa section Home | `color`/`height` là prop thật |
| Marquee vô hạn | `Home.vue` `.app-marquee` | bản sao `aria-hidden` |
| ArrowRight trong vòng tròn trắng | `AppButton` prop `with-arrow` | opt-in; arrow `aria-hidden` |
</design_system>

<responsiveness>
- Verify đúng **360, 768, 1280, 1440, 1920 px**.
- `768px` là biên Tailwind `md`. Container 1152px → từ 1152px lên trang căn giữa có gutter.
- Dưới 640px hard shadow co còn 3px (2px cho `xl`/`featured`) — đã làm; đừng undo.
- Acceptance cơ học: mỗi width, `document.documentElement.scrollWidth - clientWidth` ≤ **16px**.
  Chromium chừa ~15px cho scrollbar → 13–15px KHÔNG phải overflow.
</responsiveness>

<accessibility>
- Giữ/cải thiện WCAG 2.2 AA: 1.1.1 (`img` cần thuộc tính `alt` — `alt=""` HỢP LỆ cho trang trí;
  test `hasAttribute('alt')`), 1.4.3 contrast 4.5:1, 1.4.11 non-text 3:1, 2.4.7 focus visible
  (`--geo-accent` ring), 2.5.8 target ≥ 24×24px (**inline link trong câu văn được MIỄN**),
  4.1.2 name/role/value.
- Keyboard: mọi phần tử tương tác tới được; skip-link phải là focusable ĐẦU TIÊN.
- Nút chỉ-icon phải có accessible name (`aria-label`/text ẩn).
- Element trang trí (shape, squiggle, marquee bản sao, arrow) phải `aria-hidden`.
</accessibility>

<workflow>
1. Đọc `design-system.css`, `tailwind.config.js`, `frontend/index.html`, `app-layout.css` + component
   sẽ sửa. Verify facts block trên với file thật trước khi viết.
2. Nêu plan 1 đoạn ngắn: component nào đổi, token nào, vì sao.
3. Thay đổi nhỏ nhất thể hiện design system; ưu tiên token + decor component có sẵn.
4. Thêm/gỡ class `.geo-*` thì grep `frontend/src` tìm straggler (không gate tự động).
5. Verify trong browser thật ở 5 width, rồi báo cáo kèm số.
</workflow>

<definition_of_done measurable>
Backend + frontend suite phải xanh trên build cuối, đọc số từ **run log** (KHÔNG đọc XML
`target/surefire-reports` — XML stale từng làm sai +8). `mvnw -q` NUỐT dòng `Tests run:` → chạy không `-q`.

- `.\mvnw.cmd test` (repo root) xanh (baseline v21: **541 / 0 / 0 / 11**).
- `npx vitest run` trong `frontend/` xanh (baseline v21: **194 passed / 1 skipped / 32 files**).
- `npx vite build` xanh; entry JS không vượt baseline (v21: **177.75 kB**, gzip 67.68).
- `node sweep/v8/ui/design-v2.js`: 0 font non-BVP (BVP **loaded**), 0 legacy family, 0 token thiếu,
  0 drift, 0 lucide stroke ≠2.5, 0 overflow, 0 wrong landing.
- `node sweep/v8/ui/routes-all.js`: 0 console error, 0 API ≥400 do UI, 0 not-mounted, 0 overflow,
  0 wrong landing — assert guard **2 chiều**.
- `node sweep/harness/f1302-a1-a11y.js`: 0 ảnh thiếu `alt`, 0 tap target < 24px.
- `node sweep/harness/assert-harness.js`: **ALL CLEAN (8 check)**.
- Mọi sweep chạm DB kết thúc bằng parity `1470|43738|5|118|29|4|3|13|10` + marker `STUDY_DAYS=4`,
  `PENDING_PAYMENTS=0`, `EXERCISE_ATTEMPTS=33`; nếu không → là finding, không phải pass.
- Lighthouse `/` và `/lessons`: a11y = 100, best-practices = 100, SEO = 100.
- `GET /api/streak/snapshot` trả 200 với `today`, `currentStreak`, `studiedToday`, `effectiveFrom`,
  `studiedDays`, `legacyAccessDays`, `legacyHistoryAvailable`.
- Mọi trích dẫn `File.java:start-end` trong `docs/demo-engflow-4-chuc-nang.md` vẫn trỏ đúng
  (chạy `doc_citation_remap.py`, rồi hand-verify).
</definition_of_done>

<constraints>
- Không thêm dependency khi chưa kiểm bundle size + license.
- Không thêm font thứ hai; không khôi phục alias cũ đã gỡ.
- Không commit `.env`, key, fixture `frontend/public/*.wav`.
- Không viết Flyway migration: schema do Hibernate `ddl-auto=update` + SQL review.
- Không mở thêm actuator endpoint cho audit.
- Sweep có mutate phải dùng namespace audit, tự dọn trong cùng run, và tự assert parity.
- Không dùng `email LIKE 'zz%'` trần để tìm user cũ — liệt kê ID chính xác (từng xoá oan 4 user baseline).
- `sqlcmd` exit 0 kể cả khi batch lỗi: luôn quét `Msg \d+`, và `SET QUOTED_IDENTIFIER ON` mọi batch DELETE.
- Comment trong `.vue` KHÔNG đặt ở cấp gốc `<template>` (F-21-03).
- Nếu tool thiếu/hỏng: ghi `BLOCKED` kèm quan sát — không bịa kết quả thay thế.
</constraints>
