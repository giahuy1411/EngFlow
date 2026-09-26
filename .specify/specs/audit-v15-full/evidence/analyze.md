# audit-v15-full — analyze (chất lượng bằng chứng)

**Ngày:** 2026-09-25 (+07)

## Điểm mạnh

1. **Ground truth được đo lại, không kế thừa** — 15 giả thuyết trong `prior-hypotheses.md`, **5 cái SAI**
   đã sửa (tên cột, số lesson, "controller chỉ phục vụ Đường B", tab Lịch sử, số writer timezone).
2. **Backup + restore-drill thật** — không chỉ `BACKUP`, mà **restore sang DB scratch** rồi so parity
   (`1470|43735|72|126|15|10|5`) trước khi xoá bất cứ thứ gì.
3. **DML theo ID liệt kê** — 67 user + 114 payment viết thành literal trong transaction có
   **assertion trước/sau** (`RAISERROR` nếu ≠ 67/114/12). Không có `LIKE 'zz%'`.
4. **Dry-run trước commit** — `ROLLBACK` cho thấy đúng số hàng, DB không đổi (`72|126`).
5. **Verify 3 tầng** — SQL (parity) + API thật (`/grade` 3/3 & 0/3) + UI thật (Playwright DOM).
6. **F-15-01 tự khỏi** — gỡ Đường B làm endpoint rò rỉ biến mất; **đo 404** thay vì suy luận.
7. **Tự bắt lỗi của chính mình** — F-15-06 (`GETDATE()` UTC) phát hiện qua đối chiếu timezone;
   F-15-09 (residue payment) phát hiện qua parity đọc 14 thay vì 12.

## Điểm yếu / giới hạn (ghi rõ, không giấu)

1. **Harness v15 là snapshot chưa đồng bộ** — nhiều bản sửa v14 (P1/P2/D3, F-13-12/13) **không có** trong
   bản khôi phục, gây **5 finding probe SAI** (F-15-08/11/12/13) phải sửa lại. Nguyên nhân: harness v15
   dựng từ commit cũ hơn bản v14 đã sửa.
2. **`ui-sweep` từng không có cleanup** → tạo 2 payment thật (F-15-09), **lặp lại lớp lỗi F-14-C2**.
   Đã thêm cleanup, nhưng đây là lần thứ hai cùng một lỗi — cần cẩn trọng hơn với mọi probe chạm
   endpoint ghi.
3. **Không chạy webhook SePay thật** (blocked) — biên real-money, giữ nguyên quyết định v11.
4. **`e2e-3tier` chưa chạy lại** trong phiên này (không có trong `sweep/v15`) — regression 3 tầng dựa
   vào `api-sweep` + `deep-probe` + `ui-sweep` thay thế.
5. **`perf-probe` chỉ đo "before"** — không có optimisation nào trong phạm vi v15 (không phải mục tiêu).

## Chất lượng từng finding

| Finding | Bằng chứng | Probe 2 | Mức tin cậy |
|---|---|---|---|
| F-15-01 | API anon + DB 3 block | 404 sau gỡ + anon `/exercises` 0 leak | **CAO** |
| F-15-02 | grep + `AuditV8UploadXssTest` | upload 200 + traversal 400 live | **CAO** |
| F-15-03 | grep import | `/admin/exercises` render không lỗi | **CAO** |
| F-15-04 | `LEN(data)` vs `LEN(content)` 8 lesson | — (dữ liệu, xoá) | **CAO** |
| F-15-05 | `COUNT=5` snapshot | DROP 0 `Msg` | **CAO** |
| F-15-06 | `created_at` 19:48 vs 02:48 | sau fix 02:48 | **CAO** |
| F-15-07 | 6 chỗ harness | api-sweep 143/0 | **CAO** |
| F-15-08 | deep-probe 2 fail | mc-guard 18/0 | **CAO** |
| F-15-09 | parity 14 vs 12 | sau dọn 12 + cleanup thêm vào | **CAO** |
| F-15-10 | grep doc vs đo | counts khớp | **CAO** |
| F-15-11/12 | 27 guard fail | 0 guard fail sau fix | **CAO** |
| F-15-13/14/15 | đọc code + chạy | output đúng thư mục / 18-18 / 5-5 | **CAO** |

## Kết luận

Bằng chứng **mạnh** cho phần DML và gỡ Đường B (đo 3 tầng, có drill). Điểm yếu chính là **kỷ luật
harness**: 8/15 finding là lỗi probe tự gây (F-15-07..15), trong đó 2 lần tái diễn lỗi đã biết của v14.
Đây là chi phí thật của việc dựng lại harness từ snapshot cũ — đã ghi để vòng sau tránh.
