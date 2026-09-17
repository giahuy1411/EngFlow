# prompt-rewritten.md — bản prompt thiết kế đã chuẩn hoá (Task 2)

**Mục đích:** bản thay thế cho prompt thiết kế gốc, đã sửa hết mâu thuẫn với codebase thật.
Prompt gốc yêu cầu **Outfit + Plus Jakarta Sans**, **Lucide React**, và token tên
`background/foreground/...` — cả ba đều **sai với EngFlow** (Vue 3, `--geo-*`, `lucide-vue-next`).
Phân tích từng lỗ hổng ở `prompt-gap.md`; đây là bản đã sửa, dán được nguyên khối.

**Quy tắc áp dụng:** một prompt duy nhất, không thêm framework tự diễn kịch (MoE/ToT/GoT),
không CoT cho model suy luận, giữ `<context>` dạng XML vì cấu trúc rõ, mọi thành phần
đều có tiêu chí nghiệm thu đo được.

---

```xml
<role>Expert frontend + design-token engineer cho Vue 3 + Vite + Tailwind
(không React, không shadcn runtime).</role>

<stack_context>
Vue 3 `<script setup>`; Tailwind config map tới biến CSS `--geo-*` trong
frontend/src/assets/design-system.css; icons = lucide-vue-next (stroke 2.5).
</stack_context>

<tokens authoritative, không đổi tên>
--geo-bg #FFFDF5 | --geo-fg #1E293B | --geo-muted #F1F5F9 | --geo-muted-fg #64748B
--geo-accent #8B5CF6 (CTA chính) | --geo-secondary #F472B6 | --geo-tertiary #FBBF24
--geo-quaternary #34D399 | --geo-border #E2E8F0 | --geo-card #FFFFFF
radius 8/16/24/full · border-width 2px · shadow "pop" = offset cứng, 0 blur
</tokens>

<typography>
Font DUY NHẤT: Be Vietnam Pro (Google Fonts, display=swap, preconnect).
Headings 900 (không dùng 800 — 0 lượt dùng), body 400/500.
Không nạp lại Outfit / Plus Jakarta Sans.
</typography>

<layout>
Container max-w-6xl. Nhịp dọc: 96px cho marketing/landing, 24–32px cho màn hình
làm việc (lesson, admin table). Phá vỡ lưới 12 cột thành khối 6/6 hoặc 4/4/4.
</layout>

<decoration "Stable Grid, Wild Decoration">
Được dùng: circle/square/pill/squiggle tĩnh, polka-dot + grid-line, blob radius
bất đối xứng. Chưa có trong hệ thống: diagonal stripe — CHỈ thêm nếu được yêu cầu rõ.
</decoration>

<breakpoints bắt buộc>
- <=768px: pop shadow về 2px, ẩn shape nền phức tạp, target chạm >=48px cao.
- 1280–1535.98px: nén header (font/link padding/gap/username 92px) sao cho toàn bộ
  logo + nav + cluster phiên đăng nhập nằm gọn trong container 1265px.
  Nghiêm cấm: scrollWidth > clientWidth ở bất kỳ viewport 360–1920px.
</breakpoints>

<accessibility đo được>
WCAG 1.1.1 (mọi <img> có alt; ảnh trang trí = alt=""), 2.4.7 (focus-visible đầy đủ
+ shadow màu), 2.3.3 + prefers-reduced-motion (transition 0s, không translate khi hover),
2.5.8 (target >=24px, mục tiêu >=44px trên mobile), sắc màu không phải tín hiệu duy nhất.
</accessibility>

<motion>
transition 300ms cubic-bezier(0.34,1.56,0.64,1); entrance pop scale 0->1;
wiggle 3deg chỉ ở 1280px+.
</motion>

<definition_of_done>
1. `npx vitest run` xanh; `npx vite build` xanh VÀ entry chunk không tăng so với
   baseline (kiểm tra: nếu muốn nạp DOMPurify thì import lazy, không ở main.js).
2. Với mọi route trong router: 0 console error, 0 API >=400, document mount,
   tập hợp font-family == {Be Vietnam Pro}, scrollWidth <= clientWidth tại
   360/768/1280/1440/1920.
3. UI text tiếng Việt; thuật ngữ kỹ thuật giữ tiếng Anh.
4. Mọi tuyên bố về "đồng bộ design system" phải kèm phép đo thật
   (computed style / bounding rect), không kết luận bằng cảm quan.
</definition_of_done>
```

---

## Đối chiếu bản gốc → bản viết lại

| # | Bản gốc | Bản viết lại | Lý do |
|---|---|---|---|
| 1 | Outfit (headings) + Plus Jakarta Sans (body) | **Be Vietnam Pro** duy nhất | Bản gốc tự mâu thuẫn: vừa chỉ định 2 font vừa đòi "thay toàn bộ bằng Be Vietnam Pro". Hai font thừa ≈ 35–45 kB, vi phạm boundary "không thêm dependency". Plus Jakarta Sans đã bị bỏ từ audit-v5. |
| 2 | Token `background/foreground/muted/accent/...` (Tailwind/shadcn) | Token `--geo-*` | Viết `bg-background` sẽ trỏ vào class không tồn tại → class chết, trang vỡ. |
| 3 | Shadow `8px 8px 0 #E2E8F0` cho card, `4px 4px 0 #1E293B` cho pop | `shadow-pop-*` + patch mobile 2px | Bản gốc đặt 2 chuẩn cho cùng 1 component và không nhắc mobile → shadow tràn ngang trên mobile. |
| 4 | "Diagonal stripes" trong Pattern Fills | Ghi rõ **chưa có trong hệ thống**, chỉ thêm khi được yêu cầu | `repeating-linear-gradient` sọc chéo không tồn tại trong codebase; đòi "đồng bộ 100%" sẽ buộc phát minh CSS mới. |
| 5 | `py-24` (96px) + `max-w-6xl` cho **toàn bộ** app | Nhịp lớn cho landing, mật độ bảng cho admin | 96px dọc mỗi section làm trang admin/bảng kéo dài vô nghĩa. |
| 6 | Không định nghĩa breakpoint | `<=768px` và band nén `1280–1535.98px` | Thiếu ngưỡng chính là nguồn gốc bug header tràn ngang 1280–1535px (F-UI1). |
| 7 | "contrast AAA", "focus high-contrast", "reduced-motion" — không nêu WCAG SC | Nêu đích danh 1.1.1 / 2.4.7 / 2.3.3 / 2.5.8 | Không có SC thì không đo được; chính vì thiếu mà 30+ ảnh scraped không có `alt`. |
| 8 | "verify giao diện đã đồng bộ" — không có tiêu chí pass/fail | Phép đo cụ thể trong `<definition_of_done>` | Không có oracle thì không thể kết luận, chỉ đoán. |
| 9 | Không nhắc tiếng Việt dù đây là app VN | Giữ quy định P7 trong `<definition_of_done>` | Rủi ro UI copy tiếng Anh lọt vào. |
| 10 | "Lucide **React**" | `lucide-vue-next` | Sai hệ sinh thái (stack là Vue). |
| 11 | Headings 800 | Headings **900** | Đo thực tế: weight 800 có **0 lượt dùng**, 900 dùng ở mọi `h1` — khai báo 800 là nạp face thừa. |

## Ghi chú về một lỗi chất lượng trong bản gốc của tài liệu này

`prompt-gap.md` (bản phân tích) từng lẫn **ký tự CJK** vào văn bản tiếng Việt
(`指令`, `已从`, `ảnh装饰`, `二元`) và có một thẻ đóng sai (`</mition>` thay vì `</motion>`).
Đã sửa. Đây đúng là lớp lỗi mà `REPORT.md` §3 mục 13 ghi nhận ở output AI — nhưng ở đây
là **văn bản do người/agent viết**, không phải output model, nên sửa trực tiếp là đủ,
không cần thêm guard runtime.
