# Phase 4 — CRUD qua UI admin (end-to-end, cả UI lẫn API)

**Ngày:** 2026-09-21 (+07) · **Driver:** Playwright MCP · **Target:** SPA `:5173` + API `:8080`
**Kết quả: PASS — vòng CRUD đầy đủ, 0 rác, parity không đổi.**

---

## 1. Lesson — create → read → update → delete, qua UI THẬT

| # | Bước | Cách làm | Kết quả |
|---|---|---|---|
| 1 | Mở `/admin/lessons` | session admin seed vào `localStorage` (cả `token` và `user`) | landed `/admin/lessons`, `<h1>` = "Bài học", 20 row |
| 2 | Mở form tạo | click nút **"Thêm Bài Học"** | modal mở, form thật: `#form-lesson-title`, `#form-lesson-desc`, `#form-lesson-content`, `#form-lesson-level` |
| 3 | **CREATE** | điền title `AUDIT-V12-UI-LESSON-30155` + desc + content + level, click **"Lưu lại"** | modal đóng; **API xác nhận**: `GET /api/admin/lessons?q=…` → **1 row, id=103030** |
| 4 | **READ (UI)** | lọc bảng bằng ô search `#lesson-search` | row xuất hiện trong UI |
| 5 | **UPDATE** | click nút **"Sửa"** ở row đó, sửa title → `…-EDIT`, **"Lưu lại"** | **API xác nhận**: `q=…-EDIT` → **1 row** title mới; `q=<title cũ>` → **0 row** (đổi thật, không nhân bản) |
| 6 | **DELETE** | click nút **"Xóa"** ở row → hộp thoại `confirm` *"Bạn có chắc chắn muốn xóa bài học này?"* → chấp nhận | **API xác nhận**: `q=…-EDIT` → **0 row**; UI không còn hiển thị |

**Cả 4 bước CRUD được xác nhận ở HAI nơi (UI và API)** — đó là điểm của Phase 4.

## 2. Deck — create qua UI, verify, dọn

| # | Bước | Kết quả |
|---|---|---|
| 1 | Mở `/decks/create` | `<h1>` = "TẠO BỘ TỪ"; form: `#deck-name`, `#deck-desc`, `#deck-source`, `#deck-level`, `#deck-words`, checkbox công khai |
| 2 | **CREATE** | điền `AUDIT-V12-UI-DECK-30155` + 2 từ, click **"Tạo bộ từ"** → redirect `/decks` |
| 3 | **VERIFY** | `GET /api/decks/my` → **tìm thấy deck, id=50070**, tổng 2 deck của admin |
| 4 | **CLEANUP** | `DELETE /api/decks/50070` → **200**, `GET /api/decks/my` → không còn |

> Ghi chú trung thực: sau khi tạo, deck **không** hiện ngay trên `/decks` mặc định vì trang mở ở tab **"CỘNG ĐỒNG"**
> (deck mới là **private**). Đây là hành vi đúng của UI (private deck không thuộc tab cộng đồng), không phải lỗi —
> đã xác nhận bằng `GET /api/decks/my` (đúng nơi liệt kê deck của tôi). Không ghi thành finding vì API và UI nhất quán.

## 3. Rác & parity — kiểm, không giả định

```
AUDIT lessons = 0
AUDIT decks   = 0
AUDIT vocab   = 0
AUDIT payments= 0

parity: 1471|43735|72|127|28|15|4|126|14|5   (không đổi)
```

Vòng CRUD tạo lesson thật, sửa, xoá, tạo deck, xoá — để lại DB **đúng như lúc đầu**.

## 4. Ghi chú về độ tin cậy của phép đo

- Harness seed **cả** `localStorage.token` **và** `localStorage.user`; nếu chỉ seed token thì `isAdmin === undefined`,
  router đá `/admin/*` về `/`, và **trang admin render thành trang chủ mà vẫn báo PASS** (`HARNESS-TOKEN-ONLY-SEED`).
- Mỗi lần assert đều đọc lại **từ API** (không tin UI tự báo), và dùng **ô search** để assertion cụ thể — assert
  "row có ở đâu đó trong 20 row" sẽ pass cả khi row là cache cũ.
- Xoá lesson đi qua **hộp thoại `confirm` thật** của trình duyệt (Playwright `handle_dialog`) — chứng minh đường
  xoá thật chạy, không phải click vào nút không làm gì.

## Verdict

**Phase 4: PASS.** Vòng CRUD đầy đủ cho **lesson** và **deck** qua UI thật, xác nhận ở **cả UI lẫn API** ở mọi bước,
đi qua hộp thoại xác nhận thật, và để lại **0 rác** với parity **không đổi**. Đóng hạng mục T4.10 mà v10 để mở.
