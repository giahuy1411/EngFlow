# audit-v12-full — specification

**Authored:** 2026-09-21 (+07) · **Predecessor:** `audit-v11-full` · **Branch:** `audit-streak-review`
**Checkpoint:** `30bc9b6` (tree sạch) · **Artifact home:** `.specify/specs/audit-v12-full/`

## Why this audit exists, stated honestly

Yêu cầu người dùng: *"quét toàn bộ codebase và CSDL, chạy toàn bộ API, tương tác toàn bộ UI tương ứng từng API,
kiểm sát 6 chức năng chính (Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI), kiểm DB trong
Docker, tối ưu hiệu năng, test UI/UX bằng chrome-devtools-mcp và playwright-mcp, verify UI khớp design-system
Playful Geometric (font Be Vietnam Pro), dùng skill/plugin phù hợp, kiểm prompt có lỗ hổng rồi sửa, chạy 2 vòng, báo
cáo chi tiết."*

**Ba điều trong yêu cầu đó đã xong và v12 KHÔNG được giả vờ phát hiện lại:**

1. **Font đã là Be Vietnam Pro, duy nhất, ở mọi nơi.** Đo DOM live: 1 họ font duy nhất. Không có gì để "thay".
2. **Design system Playful Geometric đã triển khai đủ** — token, shadow cứng, border 2px, hero sun, blob, dashed
   connector, pricing scale+badge, squiggle, marquee, arrow button.
3. **Đã có audit v1→v11.** v11 (đã commit) báo xanh trên 485 backend test, 119 frontend test, API 74/0/0, parity khớp.

**Nhưng v11 để lại một khoảng trống thật, đo được:** source có **131 endpoint / 26 controller**, còn
`sweep/v11/api-sweep.js` chỉ probe **~27**. **12 controller chưa từng được probe.** Yêu cầu "chạy toàn bộ API"
vì thế chưa được đáp ứng.

Người dùng chọn **"làm lại từ đầu, toàn diện"** — v12 tôn trọng điều đó bằng cách **đo lại** thay vì đọc lại:
mọi con số do phiên này tự chạy; báo cáo cũ chỉ là **giả thuyết để kiểm chứng**, không phải nguồn.

## Objective

1. Tái lập baseline bằng chính phiên này (backend, frontend, build, container, DB, parity) — **không** trích v11.
2. **Phủ toàn bộ 131 endpoint** trên container thật, đúng role (anon/student/admin), đúng body, **kiểm hợp đồng
   response** (tên field + kiểu), role kiểm **cả hai chiều**; endpoint phá huỷ/tiền thật ghi rõ biên giới.
3. **UI sweep bằng cả chrome-devtools MCP và Playwright MCP**, sâu hơn v11, và **đối chiếu UI↔API**: mỗi flow UI
   truy vết tới API nó gọi.
4. Audit **DB trong Docker** như dữ liệu, gồm **hiệu lực** của constraint (không chỉ sự tồn tại).
5. **Hiệu năng** trước/sau, chỉ tối ưu khi số biện minh.
6. **Vòng 2 rộng hơn** chạy đến khi cạn lỗi mới, sửa tận gốc.
7. Báo cáo trung thực: đã làm / chưa làm / đã fix & cách fix / skill đã nạp / giới hạn.

## Requirements

### R1 — Mọi con số là đo của phiên này
Không sao chép số từ audit cũ vào kết luận. Báo cáo cũ chỉ dùng để **nhắm** probe. Trùng thì ghi **xác nhận**;
lệch thì ghi **phát hiện**.

### R2 — API surface được **chạy**, không chỉ liệt kê
Inventory tái dựng **từ source** (131 annotation). **Mọi** entry phải có status (PASS/FAIL/BLOCKED/N-A). `2xx`
không đủ để coi là đúng: phải kiểm **hợp đồng** (tên field, kiểu) và role vi phạm **cả hai chiều**.

### R3 — UI được **đối chiếu** với API, không test rời
Mỗi chức năng chính: điều khiển UI thật, **bắt network call nó phát ra**, so với hợp đồng API. UI render hợp lý
nhưng gọi sai endpoint = finding; UI báo lỗi trong khi API khỏe = finding.

### R4 — DB trong Docker audit như dữ liệu
Parity đo lại. Constraint kiểm **tồn tại** rồi kiểm **hiệu lực** (insert vi phạm có bị chặn thật không, trong DB
scratch). Orphan scan viết sao cho **NULL FK không bị đếm nhầm**.

### R5 — Design system được verify, và tìm lỗ hổng của chính nó
Prompt là **spec để kiểm ngược lại code**, gồm cả tính nhất quán nội tại. Prompt sai về sản phẩm (ghi "Lucide
React"; chỉ có 2 gói pricing; hero không có ảnh) → sửa **prompt**. Prompt đòi điều vi phạm a11y → **defect** kèm
tỷ lệ đo được.

### R6 — Vòng 2 rộng hơn vòng 1, và lặp đến khi cạn lỗi mới
Sau vòng 1, chạy lại toàn bộ trên build cuối + case biên đối kháng (streak edges, SRS cap, cutover, lockout, bucket
routing). **Dừng khi 2 vòng liên tiếp không tìm thêm lỗi mới.**

### R7 — Kỷ luật bằng chứng, không cap im lặng
Mọi khẳng định trỏ tới artifact. Chưa verify → **BLOCKED/PARTIAL** kèm lý do. Sweep có giới hạn phải nói rõ đã bỏ
gì. Probe báo lỗi app không có = **lỗi của probe**, phải ghi lại.

## Success criteria — kết quả đo

| Tiêu chí | Kết quả |
|---|---|
| Baseline đo bởi phiên này | *(T0)* |
| Endpoint inventory reconcile = 131 | *(T0.7)* |
| **Mọi entry có status** | *(T2)* |
| 12 controller 0-coverage đều có probe | *(T2.6)* |
| Role 2 chiều | *(T2.9)* |
| UI sweep 2 MCP | *(T3)* |
| UI↔API cross-check 6 chức năng | *(T3.8)* |
| DB audit + hiệu lực constraint | *(T1)* |
| Contrast AA | *(T3.7)* |
| Perf before/after | *(T6)* |
| Vòng 2 đến cạn lỗi | *(T7)* |
| Parity sau mọi run ghi | *(liên tục)* |

## Known holes in the prompt as written

| # | Lỗ hổng | Bằng chứng |
|---|---|---|
| **H1** | Bắt dùng `secondary/tertiary/quaternary` cho "chữ nhấn mạnh", nhưng các màu đó **trượt WCAG AA** khi làm chữ trên nền cream/white mà chính prompt bắt. | Đo live: `text-secondary` trên trắng = **2.65:1** (cần 3:1 large / 4.5:1 small). v11 sửa bằng tầng token `*-ink`/`*-strong`. |
| **H2** | Ghi "Lucide **React**". | Dự án Vue dùng `lucide-vue-next` (`frontend/package.json`). |
| **H3** | Bắt dùng tên utility Tailwind literal (`max-w-6xl`, `py-24`) nhưng tự nói *"đừng viết lại component chỉ để thêm chúng"* — tự mâu thuẫn. | prompt `<layout>` vs ghi chú kết. |
| **H4** | "hard shadows **4px**" **không ngoại lệ mobile**, trong khi mục responsive nói mobile giảm còn 2px. Hai mục mâu thuẫn. | prompt `<shadows>` vs `<responsive>`. |
| **H5** | Không có dark mode / không xử lý `prefers-color-scheme` trong khi đòi "bảo trì dài hạn". | 0 `dark:`/`prefers-color-scheme` trong `frontend/src/assets/*.css`. Ghi là biên giới có chủ ý. |
| **H6** | Nói hero image "có blob mask", nhưng hero **không có ảnh** — thuần hình học CSS. | `Home.vue` hero: `.app-hero__sun` + shape spans, không `<img>`. |
| **H7** | Đòi "pricing card giữa scale lên" — sản phẩm chỉ có **2 gói**. | `PremiumPage.vue`: `md:grid-cols-2`. |
| **H8** | Đòi "Features grid **3**" — Home có **4**. | `Home.vue` `features` có 4 phần tử. |
| **H9 (mới)** | "chạy **toàn bộ** API" bất khả thi: webhook chữ ký **hợp lệ** = giao dịch thật; `create-order` ghi row thật; restore/seed/backfill/generate-all mutate nội dung. | `PaymentService`, `LessonSnapshotController`, `AdminExerciseSeedController`, `AdminAnswerBackfillController`, `AdminAiExerciseController`. |
| **H10 (mới)** | "tối ưu hiệu năng" không nêu mục tiêu. | Không có ngưỡng nào trong prompt. |
| **H11 (mới)** | v11 dùng chrome-devtools MCP gần như chỉ `list_pages` → yêu cầu "test UI/UX bằng chrome-devtools-mcp" chưa đáp ứng đủ. | v11 `evidence/ui-sweep-rerun.md` chỉ nêu `list_pages`. |
| **H12 (mới)** | "thay toàn bộ font" là **việc đã xong**. | Đo DOM live + source. |

H1 là lỗ hổng duy nhất có hậu quả a11y thực tế; phần còn lại là prompt/codebase lệch nhau, đã ghi rõ. **Nguyên
tắc:** prompt sai về sản phẩm → sửa prompt; có hậu quả thực tế → sửa code kèm số đo.

## Boundaries

- Môi trường dev/verify (đồ án), không phải production rollout.
- Không thêm dependency mới, không migration Flyway, không thêm actuator endpoint.
- Không đưa `.env`, credential hay media fixture vào artifact.
- Sweep có ghi dùng namespace audit, **dọn trong cùng run**, re-assert parity.
- Bước DB phá huỷ cần backup mới + danh sách ID liệt kê — **cấm `LIKE` trần**.
- `sqlcmd` exit 0 kể cả khi batch lỗi: **quét `Msg \d+`** mọi batch; mọi DELETE kèm `SET QUOTED_IDENTIFIER ON`.
- Tool thiếu/bị từ chối → ghi **BLOCKED** kèm quan sát, không thay bằng kết quả bịa, không để hưởng lây thành công
  của hàng xóm.
