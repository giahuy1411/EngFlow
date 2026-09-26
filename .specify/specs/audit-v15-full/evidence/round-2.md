# Phase 7 — vòng 2 (loop-until-dry)

**Ngày:** 2026-09-25 (+07) · Chạy lại toàn bộ probe trên build cuối, tìm cái vòng 1 bỏ sót.

## Phát hiện mới ở vòng 2 (đều là hệ quả của việc gỡ Đường B — cần thiết, không phải lỗi sản phẩm)

| # | Loại | Mô tả | Xử lý |
|---|---|---|---|
| R2-1 | **Harness drift** | `sweep/v15/api-sweep.js:160` khẳng định `GET /api/lessons/{id}/structure` → 200; endpoint đã gỡ | Đổi thành khẳng định **404** |
| R2-2 | Harness drift | `api-sweep.js:172` khẳng định F126 draft structure → 404 (endpoint không còn) | Đổi tên + giữ 404 |
| R2-3 | Harness drift | `api-sweep.js:412-416` khẳng định `/snapshots` → 200/403/401 | Đổi: admin **404** (route gỡ), student **403** / anon **401** (security layer) |
| R2-4 | Harness drift | `perf-probe.js` đo `/api/lessons/41881/structure` (404) | Bỏ khỏi danh sách target |
| R2-5 | Harness drift | `coverage-sweep.js` parity query còn `COUNT(lesson_snapshots)` → `Msg 208` | Bỏ khỏi query |
| R2-6 | Harness drift | `ui-sweep.js:78` khẳng định `/admin/{id}/build` → `/admin/{id}/build` cho admin | Đổi thành **`/`** (catch-all) |
| R2-7 | **Stale assertion tái nhập** | `deep-probe.js` 2 assertion v13 đã sửa ở v14 lại xuất hiện (LISTENING bare-letter→400; copy row 651717→400) | Sửa lại theo v14: **200** (F-13-13 guard MC-scoped; F-13-12 "A - Salad" hợp lệ) |
| R2-8 | **Residue (F-14-C2 tái diễn)** | `ui-sweep.js` ghé `/premium/checkout` (student) → tạo **2 hàng PENDING thật**; script **không có cleanup** | Xoá 2 hàng (80301, 80302) + **thêm cleanup** vào ui-sweep |
| R2-9 | Doc drift | `README.md` 21 bảng / 194 test / 523 test / 48 view — stale | Sửa: **18 bảng / 178 test / 512 test / 46 view** |
| R2-10 | Doc drift | `AGENTS.md` baseline 525 / 194 | Sửa: **512 / 178** |
| R2-11 | Doc drift | `docs/erd-sql-guide.md` mô tả 3 bảng đã gỡ + 21 bảng + row counts cũ | Sửa: **18 bảng, 22 FK**, bỏ DDL/DBML 3 bảng, cập nhật row counts |
| R2-12 | Doc stale | `docs/lesson-builder-status.md` mô tả tính năng đã gỡ | Xoá; thay bằng `docs/lesson-builder-removal.md` |
| R2-13 | Doc/migration | `db/migration/V1` vẫn CREATE 3 bảng đã DROP | Thêm `V10__drop_lesson_builder.sql` (theo tiền lệ V2) |
| R2-14 | Comment stale | `api.js` + `api.test.js` nhắc `LessonStructureController` như còn tồn tại | Ghi rõ "đã gỡ ở audit-v15" |
| R2-15 | Pointer stale | `.specify/feature.json` trỏ `audit-v14-full` | Trỏ `audit-v15-full` |

## Kết quả probe vòng 2 (sau khi sửa)

| Probe | Kết quả |
|---|---|
| `api-sweep.js` | **143 pass / 0 fail** (1 blocked = webhook real-money, 2 n/a) |
| `deep-probe.js` | **58 pass / 0 fail** |
| Backend suite | 512 / 0 fail / 0 error / 11 skipped |
| Frontend suite | 178 passed / 1 skipped (29 files) |
| Parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + 0 Route B table |
| Residue | payments **12**, PENDING-today **0**, study_days **5** |

## Ghi chú về kỷ luật dữ liệu

R2-8 là **cùng lớp lỗi F-14-C2** (probe tạo row thật rồi không dọn). Phát hiện vì parity đọc
`payments=14` thay vì 12. Đã: (a) xoá đúng 2 hàng theo **ID liệt kê**, (b) thêm cleanup vào ui-sweep
để **không tái diễn**. Không mất dữ liệu thật (2 hàng đều là PENDING do probe tạo, `transaction_id IS NULL`).

---

## Kết quả `ui-sweep` cuối (sau khi sửa F-15-11/12)

| Chỉ số | Trước sửa | **Sau sửa** |
|---|---|---|
| `guardFails` | 27 | **0** ✅ |
| `contrastFails` | 0 | **0** |
| `missingAlt` | 0 | **0** |
| `overflowRoutes` | 0 | **0** (35 ô responsive, 0 tràn) |
| `apiErrors` | 0 | **0** |
| `pageErrors` | 0 | **0** |
| `consoleErrors` | 0 | **1** — `"Permissions policy violation: compute-pressure is not allowed in this document."` (cảnh báo trình duyệt, **không phải lỗi app**) |
| `smallTargets` | 115 | 115 → triage **0 REAL** (`tap-targets.md`) |
| Font | — | `"Be Vietnam Pro"` trên mọi route (`checkBV=true`) |
| **Cleanup** | không có | **`BEFORE=4 → AFTER=0`** ✅ |

→ Parity cuối cùng: **`1470|43738|5|118|29|4|3|12|10`** + 0 bảng Đường B.
