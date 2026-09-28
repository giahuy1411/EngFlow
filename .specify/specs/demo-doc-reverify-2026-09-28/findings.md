# demo-doc re-verification — 2026-09-28

Đối chiếu lại `docs/demo-engflow-4-chuc-nang.md` theo quy trình đã dùng ở audit-v19/20/21:
đo lại số liệu, hand-verify trích dẫn, cập nhật doc + baseline, xuất bản HTML in được.

## Số liệu đo lại (bằng chứng: `evidence/`)

| Số liệu | Kỳ vọng | Đo được | Khớp |
|---|---|---|---|
| Test backend | 541/0/0/11 | **541/0/0/11 BUILD SUCCESS** | ✓ |
| Test frontend | 194/1 (32) | **194 passed / 1 skipped (32 file)** | ✓ |
| Build frontend | 177.75 kB | **index-DKhxaawa.js 177.75 kB** (gzip 67.69) | ✓ |
| lessons | 1470 (1465 xuất bản) | **1470 / 1465** | ✓ |
| exercises | 43 738 | **43 738** | ✓ |
| users / vocabulary | 5 / 118 | **5 / 118** | ✓ |
| controllers | 25 | **25** | ✓ |
| endpoints | 121 | **121** | ✓ |
| test files / Audit | 85 / 13 | **85 / 13** | ✓ |
| bảng CSDL / FK | 18 / 22 | **18 / 22** | ✓ |
| bài demo 445 / 91900 / 10888 | 6 / 41 / nháp | **6 / 41 / is_published=0** | ✓ |
| docker | 8 Up | **8 Up** | ✓ |

## Trích dẫn dòng

- `python sweep/harness/doc_citation_remap.py --force` → **giữ nguyên 167/167**, A=B=0, bỏ qua 0.
  → **không lệch** (2 file source đổi kể từ lần map cuối — `SpeakingSubmissionRepository.java`,
  `design-system.css` — đều **không được trích** trong doc).
- Hand-verify đủ 7 trích dẫn dạng danh sách phẩy (tool báo `UNHANDLED`) → **tất cả đúng**
  (xem `evidence/hand-verify.txt`).
- Hand-verify thêm 14 trích dẫn đơn trải đều 4 chương → **tất cả đúng**.

## DB lệch baseline — dữ liệu thật, KHÔNG phải rác

| Cột | Cũ | Mới | Nguồn |
|---|---|---|---|
| `video` (video_attempts) | 4 | **5** | 1 lượt xem video mới |
| `study_days` | 4 | **5** | 1 ngày học mới |
| `exercise_attempts` (2 tk demo) | 33 | **34** | 1 lượt nộp bài mới |
| `payments` | 13 | **13** | giữ nguyên — giao dịch SePay thật |

`PENDING_PAYMENTS=0` ⇒ không phải harness residue. Đã cập nhật baseline ở 3 nơi:
`docs/demo-engflow-4-chuc-nang.md` (Phụ lục D), `AGENTS.md:17`, `sweep/v8/ui/lib.js`.

## Lỗi thật đã sửa trong doc (ngoài số liệu)

1. **Snippet `router/index.js:202-231` cũ** — doc in `next({ path, query })`; source nay dùng
   `next('/login?redirect=' + encodeURIComponent(to.fullPath))` + nhánh `guestOnly` qua `safeRedirect`.
2. **Bảng rate-limit thiếu 2 bucket** — thêm `upload` 15/phút, `order` 10/phút (`RateLimitFilter.java:42-43`).
3. **Bảng III.4 thiếu `lesson_submissions`** — thêm vào nhóm 6; ghi rõ 18 bảng = 17 nghiệp vụ + `user_streaks` legacy.
4. **Snippet `SecurityConfig` lệch** — thêm rule `POST /api/ai/save-vocab`, sửa "bỏ N dòng".
5. **5 khối code in Javadoc tiếng Anh cũ** (source nay đã Việt hoá ở v20/v21): `LessonRepository`,
   `StudyDayRepository`, `RedisConfig` (×2), `DictionaryService`. → đổi sang bản tiếng Việt khớp source.
   (Công cụ remap neo-theo-số-dòng **không** bắt được lớp lỗi này — phải đọc khối in ra.)
6. **Cross-ref sai** — "đã đọc ở Chương 3" → **Chương 2** (LessonController).
7. **Làm rõ "8 dịch vụ"** — thêm ghi chú `sqlserver-init` là container chạy-một-lần (compose có 9 service).
8. **"4 endpoint đầu là cửa công khai"** → nêu đúng tên 4 endpoint công khai (row 3 `/me` cần đăng nhập).
9. Cập nhật mốc ngày: 27/09 → **28/09/2026**; 4 header chương; Phụ lục D/E.

## Xuất bản HTML in được

- Script tái lập: **`sweep/harness/export_demo_html.js`** (Node, dùng `marked` có sẵn trong
  `frontend/node_modules`). Lệnh: `node sweep/harness/export_demo_html.js`.
- Xuất: **`docs/demo-engflow-4-chuc-nang.html`** (~285 KB, tự chứa).
- Kiểm chứng trên chrome-devtools MCP: **5/5 sơ đồ Mermaid render** thành SVG, 22 bảng, 17 H2,
  `@page size: a4`, `h2 { break-before: page }`, `.toolbar { display:none }` khi in.

## Xuất bản HTML cho điện thoại (mobile)

- Script tái lập: **`sweep/harness/export_demo_mobile.js`**. Lệnh: `node sweep/harness/export_demo_mobile.js`.
- Xuất: **`docs/demo-engflow-4-chuc-nang-mobile.html`** (~308 KB, tự chứa).
- **Đầy đủ nội dung** — cùng nguồn `docs/demo-engflow-4-chuc-nang.md`; đếm khớp: 1 H1, 17 H2, 55 H3,
  56 H4, 22 bảng, 5 sơ đồ, **66 khối code**, 11 checkbox.
- Tối ưu để đọc/học trên điện thoại:
  - Bố cục **một cột hẹp** (max 720px), chữ **17px**, line-height 1.72, safe-area cho tai thỏ.
  - **Sáng/tối tự động** theo hệ thống (`prefers-color-scheme`).
  - **Menu mục lục trượt** (72 mục H2+H3), đóng khi chạm mục; nút ☰ 44px.
  - **Khối code dài gấp/mở** (`<details>`, >18 dòng) — màn hình gọn, chạm để xem.
  - **Bảng vuốt ngang** trong khung riêng (không phá layout trang); cột đầu giữ một dòng.
  - **Sơ đồ Mermaid giữ cỡ đọc được** (1600px) rồi cuộn ngang — không bị thu nhỏ li ti.
  - **Thanh tiến độ đọc** + nút **lên đầu trang** + **nhớ vị trí đọc** (localStorage, bọc try/catch).
  - Checklist Phụ lục C thành **checkbox chạm được**.
- Kiểm chứng chrome-devtools MCP @390×844 (emulate mobile, touch):
  - **0 tràn ngang** cấp trang (`documentElement.scrollWidth == clientWidth`).
  - 72 link mục lục, drawer mở/đóng đúng, 5/5 Mermaid render SVG, 0 lỗi console.
  - Screenshot: `evidence/mobile-top.jpeg`, `mobile-drawer.jpeg`, `mobile-table2.jpeg`,
    `mobile-mermaid2.jpeg`, `mobile-light.jpeg`.

## Kiểm chứng cuối

| Kiểm tra | Kết quả |
|---|---|
| `node sweep/harness/assert-harness.js` | **ALL CLEAN** (10 check) |
| `assertClean` với baseline mới | `parity=…29\|5\|3\|13\|10 study_days=5 pending=0 attempts=34 -> CLEAN` |
| `doc_citation_remap.py --force` | giữ nguyên 167, 0 lệch |
| `p16-parity.sql` | khớp chuỗi trong doc |
| HTML mở bằng browser | 5 mermaid SVG, 22 bảng, không lỗi console (ngoài cảnh báo file:// của trình duyệt) |
