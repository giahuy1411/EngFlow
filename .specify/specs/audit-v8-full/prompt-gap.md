# Phân tích lỗ hổng đoạn prompt thiết kế + bản viết lại (skill: prompt-master)

## 1. Lỗ hổng tìm thấy (đối chiếu prompt gốc với codebase thật)

| # | Vấn đề | Hệ quả nếu triển khai nguyên văn | Đã xử lý |
|---|---|---|---|
| G1 | Prompt yêu cầu font **Outfit** (headings) + **Plus Jakarta Sans** (body), rồi lại yêu cầu "thay toàn bộ font bằng Be Vietnam Pro" | Hai指令 mâu thuẫn; nếu làm theo phần đầu sẽ nạp 2 font thừa (~35–45 kB) vi phạm boundary "không thêm dependency" |已从 index.html bỏ Plus Jakarta Sans (audit-v5); chỉ Be Vietnam Pro, weights theo usage thật |
| G2 | Token palette đặt tên `background/foreground/muted/accent/secondary/tertiary/quaternary/border/input/card/ring` theo Tailwind/shadcn; codebase dùng biến CSS `--geo-*` | Nếu viết thẳng `bg-background` sẽ không tồn tại → class chết, trang vỡ | Map 1-1: `--geo-bg/--geo-fg/--geo-accent/--geo-secondary/--geo-tertiary/--geo-quaternary` (đã kiểm tra: tất cả tồn tại) |
| G3 | Shadow yêu cầu `8px 8px 0px #E2E8F0` cho card nhưng `4px 4px 0 #1E293B` cho pop | Hai chuẩn khác nhau cho cùng 1 component; mobile không được nhắc → overflow shadow | Đã có quy ước `shadow-pop-*` + patch mobile 2px (audit-v6/v7 F64) |
| G4 | "Pattern Fills: Polka dots, grid lines, **diagonal stripes**" | `repeating-linear-gradient` cho sọc chéo **không có trong codebase** → nếu đòi "đồng bộ 100%" sẽ phải thêm CSS mới | Không phát minh: báo cáo ghi nhận là điểm prompt-vs-code duy nhất còn lệch (xem §4 REPORT) |
| G5 | Layout `py-24` (96px) + `max-w-6xl` cho **toàn bộ** app | Với app học tập có bảng admin + sidebar, 96px dọc mỗi section làm trang kéo dài vô nghĩa | Áp dụng có chọn lọc: landing dùng nhịp lớn, admin dùng mật độ bảng |
| G6 | Không định nghĩa breakpoint; "mobile: giảm pop shadow, ẩn shape trang trí" | Không có ngưỡng → mỗi lần sửa một ý khác nhau; đây chính là nguồn gốc bug header tràn ngang 1280–1535px (F-UI1) | Chuẩn hoá `@media (max-width:768px)` cho shadow/shape + band nén 1280–1535.98px |
| G7 | A11y: "contrast AAA", "focus high-contrast", "respect prefers-reduced-motion" nhưng không nêu **WCAG SC** nào, không nói ảnh装饰 phải có alt | Không đo được; chính vì thiếu mà 30+ ảnh scraped trong lesson content không alt | Đã chốt WCAG 1.1.1 / 2.4.7 / 2.3.3 / 2.5.8 + hook DOMPurify tự điền alt |
| G8 | Yêu cầu "verify giao diện đã đồng bộ" nhưng không cho **tiêu chí pass/fail** | Không thể kết luận; audit trước đó phải đoán | Nay mỗi mục có phép đo thật (computed style, scrollWidth, font-family tập hợp) |
| G9 | Không nói gì về **text tiếng Việt** dù đây là app VN; prompt là thiết kế system tiếng Anh | Rủi ro: UI copy tiếng Anh lọt vào | Constitution P7 đã quy định; giữ nguyên |
| G10 | Nhắc "Lucide **React**" trong stack Vue | Sai hệ sinh thái | Dùng `lucide-vue-next`, stroke 2.5, bọc trong hình tròn |

## 2. Bản prompt viết lại (đoản mạch, đo được, không mâu thuẫn)

```
<role>Expert frontend + design-token engineer cho Vue 3 + Vite + Tailwind (không React, không shadcn runtime).</role>

<stack_context>
Vue 3 `<script setup>`; Tailwind config map tới biến CSS `--geo-*` trong
frontend/src/assets/design-system.css; icons = lucide-vue-next.
</stack_context>

<tokens authoritative, không đổi tên>
--geo-bg #FFFDF5 | --geo-fg #1E293B | --geo-muted #F1F5F9 | --geo-muted-fg #64748B
--geo-accent #8B5CF6 (CTA chính) | --geo-secondary #F472B6 | --geo-tertiary #FBBF24
--geo-quaternary #34D399 | --geo-border #E2E8F0 | --geo-card #FFFFFF
radius 8/16/24/full · border-width 2px · shadow "pop" = offset cứng, 0 blur
</tokens>

<typography>
Font DUY NHẤT: Be Vietnam Pro (Google Fonts, display=swap, preconnect).
Headings 900 (không dùng 800 — 0 lượt dùng), body 400/500. Không nạp lại Outfit/Plus Jakarta.
</typography>

<layout>
Container max-w-6xl. Nhịp dọc: 96px cho marketing/landing, 24–32px cho màn hình làm việc
(lesson, admin table). Phá vỡ lưới 12 cột thành khối 6/6 hoặc 4/4/4.
</layout>

<decoration "Stable Grid, Wild Decoration">
Được dùng: circle/square/pill/squiggle tĩnh, polka-dot + grid-line, blob radius bất đối xứng.
Chưa có trong hệ thống: diagonal stripe — CHỈ thêm nếu được yêu cầu rõ.
</decoration>

<breakpoints bắt buộc>
- <=768px: pop shadow về 2px, ẩn shape nền phức tạp, target chạm >=48px cao.
- 1280–1535.98px: nén header (font/link padding/gap/username 92px) sao cho toàn bộ
  logo + nav + cluster phiên đăng nhập nằm gọn trong container 1265px.
  Nghiem cam: scrollWidth > clientWidth o bat ky viewport 360–1920px.
</breakpoints>

<accessibility đo được>
WCAG 1.1.1 (moi <img> co alt; anh trang tri = alt=""), 2.4.7 (focus-visible day + shadow mau),
2.3.3 + prefers-reduced-motion (transition 0s, khong translate khi hover),
2.5.8 (target >=24px, muc tieu >=44px tren mobile), sac mau khong phai tin hieu duy nhat.
</accessibility>

<motion>
transition 300ms cubic-bezier(0.34,1.56,0.64,1); entrance pop scale 0->1; wiggle 3deg chi o 1280px+.
</mition>

<definition_of_done>
1. `npx vitest run` xanh; `npx vite build` xanh VA entry chunk khong tang so voi baseline
   (kiem tra: ma muon nap DOMPurify thi import lazy, khong o main.js).
2. Voi moi route trong router: 0 console error, 0 API >=400, document mount,
   tap hop font-family == {Be Vietnam Pro}, scrollWidth <= clientWidth tai 360/768/1280/1440/1920.
3. UI text tieng Viet; thuat ngu ky thuat gieng tieng Anh.
4. Moi tuyen bo ve "dong bo design system" phai kem phep đo that (computed style / bounding rect),
   khong ket luan bang cam quan.
</definition_of_done>
```

## 3. Ghi chú quy trình

Áp dụng nguyên tắc prompt-master: một prompt duy nhất, paste được; không thêm framework
tự diễn kịch (MoE/ToT/GoT); không CoT cho model suy luận; giữ `<context>` dạng XML vì
Codex đọc tốt cấu trúc rõ; mọi thành phần đều có tiêu chí nghiệm thu二元.
