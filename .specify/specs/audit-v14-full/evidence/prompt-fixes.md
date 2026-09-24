# audit-v14-full — T6.5 prompt holes (claim của prompt vs thực tế đo)

Prompt design-system được dùng như **spec để test code**. Nó cũng phải tự nhất quán. Mỗi lỗ hổng dưới
đây kèm **số đo phiên này**. Nguyên tắc: prompt sai về **sản phẩm** → sửa **prompt**; có hậu quả
**thực tế** (a11y) → sửa **code** kèm số.

## H1 — Prompt nói "AAA" nhưng bắt dùng màu trượt AA — **THẬT, đã xử lý ở v11/v12**

Prompt: *"The text is slate-800 on off-white/white, which is AAA"* — đúng **chỉ** cho `foreground` trên
`background`/`card`. Nhưng cùng prompt lại nói dùng `secondary`/`tertiary`/`quaternary` cho
*"emphasized words"*. Áp nguyên văn → trượt AA.

**Số đo phiên này** (probe composite, `ui-sweep.json`): **contrastFails = 0** trên 117 cell route×role.
Tầng `*-ink`/`*-strong` (vá ở v11 F132/F138, v13 F-13-03/04/05/14/16) **vẫn hiệu lực**. Xác nhận.

## H2 — Prompt chỉ định font sai cho sản phẩm — **ĐÃ THAY BẰNG Be Vietnam Pro**

Prompt ghi **Outfit** + **Plus Jakarta Sans**. Người dùng đã thay bằng **Be Vietnam Pro**; constitution
P6 bắt buộc. **Đo phiên này:** `document.fonts.check('16px "Be Vietnam Pro"') = true` (chrome-devtools
MCP + Playwright); grep Outfit/Plus Jakarta trong `frontend/src` = **0**. Xác nhận.

## H3 — "Lucide **React**" là sai thư viện — **ghi nhận, không phải defect**

Prompt ghi *"Lucide React settings"*. Đây là app **Vue 3** → đúng là `lucide-vue-next` (có trong
`package.json`; stroke 2.5px ép toàn cục qua `.lucide { stroke-width: 2.5 }`). **Đo:** `lucideStroke
= "2.5px"` trên `/`. *Ý định* (icon dày 2.5px trong hình khối) đã đúng; chỉ tên thư viện trong prompt sai.

## H4 — Claim layout không khớp sản phẩm thật — **ghi nhận**

- *"Hero: Text left, Image right"* — hero thật **không có ảnh**; có blob + vòng tròn mặt trời. Prompt tự
  hedge (*"The image itself has a blob mask"*) nên phần trang trí được tôn trọng.
- *"Pricing: the middle card is scaled up (1.1) and has a massive yellow star badge ... rotated 15deg"* —
  sản phẩm thật có **2 gói**, không có "middle card"; `PremiumPage.vue` scale card featured + render badge.
  *Ý định* đúng; *nguyên văn* bất khả thi với sản phẩm 2 gói.

## H5 — Không dark mode / `prefers-color-scheme` — **biên giới có chủ ý**

Prompt chỉ định **light mode**; app không có dark mode. Ghi là biên giới có chủ ý, không phải thiếu sót.

## H6 — "shadow 4px, không ngoại lệ mobile" mâu thuẫn "mobile 2px" — **ghi nhận**

Prompt mâu thuẫn nội bộ (hard shadow 4px ở phần chung, 2px ở phần Responsive). Code theo mobile 2px
(`design-system.css` `@media (max-width:768px)`). Ghi nhận.

## H7 (mới v14) — "chạy test lặp toàn bộ chức năng" không nêu **tiêu chí dừng** — **v14 định nghĩa**

Không có tiêu chí dừng → vòng lặp vô hạn hoặc dừng tuỳ tiện. **v14 chốt: loop-until-dry** — dừng khi
**2 vòng liên tiếp không thêm finding mới** (ghi rõ đã dừng ở vòng nào). Xem `round-2.md`.

## H8 (mới v14) — "xoá file rác" không định nghĩa **cái gì là rác** — **v14 định nghĩa**

Không có định nghĩa → hoặc không xoá gì, hoặc xoá nhầm dữ liệu thật. **v14 định nghĩa Cleanup protocol**
(bảng KEEP/DELETE/UNTOUCHED) + `cleanup-manifest.md`. Xem `cleanup-manifest.md`.

## H9 (mới v14) — "verify giao diện đã đồng bộ với prompt hay chưa" không nêu **oracle đo** — **v14 định nghĩa**

"Đồng bộ" là chủ quan. **v14 dùng oracle đo được:** token live (`--geo-*`) + `document.fonts.check` +
contrast composite + tap-target triage + design-system conformance. Xem `ui-sweep.json` §design.

## H10 (mới v14) — "chạy **toàn bộ** API" bất khả thi — **ghi biên giới**

Webhook chữ ký **hợp lệ** = giao dịch tiền thật; một số POST = mutate nội dung thật. **v14 ghi rõ:**
11/148 row là **BLOCKED kèm lý do** (không skip im); phần còn lại PROBED. Xem `endpoint-reconciliation.md`.

## H11 (mới v14) — "tối ưu hiệu năng" không nêu **mục tiêu** — **v14 đo trước**

Không có mục tiêu → tối ưu mò. **v14 theo P5:** đo trước, chỉ tối ưu khi số biện minh, ghi lý do từ chối.
Xem `performance.md`.

## Verdict

| Hole | Ảnh hưởng sản phẩm thật | Hành động |
|---|---|---|
| H1 contrast / "AAA" | Có (đã vá v11/v12/v13) | **Xác nhận 0 fail** phiên này |
| H2 fonts | Không — đã thay BVP | Verify, ghi nhận |
| H3 Lucide React | Không — chỉ tên thư viện | Ghi nhận |
| H4 layout literals | Không — 2 gói / không ảnh hero | Ghi nhận |
| H5 dark mode | Không — biên giới có chủ ý | Ghi nhận |
| H6 shadow mâu thuẫn | Không | Ghi nhận |
| H7 tiêu chí dừng | Không — lỗ hổng phương pháp | **v14 định nghĩa loop-until-dry** |
| H8 định nghĩa rác | Không — lỗ hổng phương pháp | **v14 định nghĩa Cleanup protocol** |
| H9 oracle UI | Không — lỗ hổng phương pháp | **v14 định nghĩa oracle đo** |
| H10 "toàn bộ API" | Không — biên giới | Ghi BLOCKED kèm lý do |
| H11 mục tiêu perf | Không — lỗ hổng phương pháp | **v14 đo trước (P5)** |
