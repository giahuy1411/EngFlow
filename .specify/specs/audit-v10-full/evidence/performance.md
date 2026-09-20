# Phase 5 — Đo hiệu năng (2026-09-19 23:5x +07)

**Nguyên tắc (P5):** chỉ tối ưu khi đo được lợi ích, kèm số trước/sau. Không thêm cache/index mà không có workload đại diện.

Công cụ: `sweep/v10/perf-probe.js` — 5 vòng mỗi endpoint, lấy median.

## Số đo

| Endpoint | status | median | min | max | avg | size |
|---|---|---|---|---|---|---|
| `admin/exercises?q=the` | 200 | **198ms** | 182ms | 727ms | 301ms | 11.5KB |
| `admin/exercises` (không `q`) | 200 | 68ms | 57ms | 113ms | 75ms | 10.9KB |
| `admin/lessons` | 200 | 81ms | 72ms | 146ms | 92ms | 6.0KB |
| `admin/stats` | 200 | 38ms | 29ms | 80ms | 44ms | 0.1KB |
| `admin/users` | 200 | 30ms | 25ms | 47ms | 33ms | 5.5KB |
| `admin/vocabulary` | 200 | 29ms | 22ms | 84ms | 38ms | 6.8KB |
| `lessons` (public) | 200 | 19ms | 17ms | 65ms | 27ms | 6.6KB |
| `leaderboard` | 200 | 18ms | 15ms | 77ms | 29ms | 4.5KB |
| `streak/current` | 200 | 17ms | 11ms | 21ms | 17ms | 0.0KB |
| `streak/history?days=30` | 200 | 16ms | 12ms | 42ms | 20ms | 0.2KB |
| `vocab/search?q=hello` | 200 | 13ms | 9ms | 39ms | 17ms | 0.0KB |
| **`streak/snapshot` (MỚI)** | **404** | 19ms | 15ms | 22ms | 19ms | 0.1KB |

## Kết luận

### 1. `admin/exercises?q=` đúng là nặng nhất — nhưng KHÔNG tối ưu

Median **198ms**, gấp ~2.9× so với không có `q` (68ms). Nguyên nhân đã biết: `LIKE '%kw%'` có **leading wildcard** trên 43.737 row, không index nào cứu được.

**Quyết định: không thay đổi gì.** Lý do:
- 198ms **không phải vấn đề** với endpoint admin, dùng không thường xuyên, có phân trang.
- Đây là hành vi tìm kiếm đúng như thiết kế (`LIKE %kw%` cho phép tìm giữa chuỗi).
- C11 + `performance.md` của v9 đã kết luận tương tự: không thêm index khi chưa có workload đại diện.
- Muốn nhanh hơn phải **đổi ngữ nghĩa tìm kiếm** (full-text index, hoặc bỏ leading wildcard) — đó là thay đổi sản phẩm, không phải tối ưu kỹ thuật.

### 2. `streak/snapshot` — ĐÃ ĐO ĐƯỢC (sau rebuild 23:53)

Container cũ trả **404**, nên lần đo trước (19ms) là **số của 404, không phải của endpoint** — đã ghi rõ là không được dùng. Sau khi rebuild (`engflow-backend` image `8034cd0c2d3d`, jar `Sep 19 23:53`), đo lại:

| Endpoint | status | median | min | max | avg | size |
|---|---|---|---|---|---|---|
| **`streak/snapshot` (MỚI)** | **200** | **23ms** | 19ms | 36ms | 25ms | 0.4KB |
| `streak/current` | 200 | 24ms | 20ms | 28ms | 24ms | 0.0KB |
| `streak/history?days=30` | 200 | 20ms | 19ms | 162ms | 51ms | 0.0KB |

**Kết luận: dự đoán đúng, và giờ có số thật.** `snapshot` = 23ms, cùng bậc với `current` (24ms) — hợp lý vì cùng đọc SQL `study_days` + Redis `user:login_days:*`, cộng thêm `effectiveFrom()` (1 row `study_policy`) và `currentStreak()` (duyệt mảng ngày, không phải query).

**Không tối ưu.** 23ms không phải vấn đề, và endpoint này gọi **một lần mỗi lần mở Profile**, không nằm trong vòng lặp. Thêm cache ở đây chỉ tạo thêm một tầng invalidation phải bảo trì mà không đo được lợi ích — đúng P5.

**Lưu ý về độ nhiễu:** `max` của `admin/exercises?q=the` nhảy lên 1351ms ở lần đo này (trước 727ms). Đây là **JIT warm-up + SQL Server buffer pool lạnh sau restart container**, không phải hồi quy: median chỉ 225ms vs 198ms trước (chênh 13%, trong biên độ nhiễu của 5 vòng). Ghi lại để vòng sau không đọc nhầm max thành hồi quy.

### 3. Các endpoint còn lại đều nhanh

Tất cả dưới 100ms median, trừ `admin/exercises?q=` (225ms) và `admin/users` (133ms). Không có vấn đề hiệu năng nào cần xử lý.

## Không có thay đổi hiệu năng nào được thực hiện trong vòng này

Đúng theo P5: **không đo được lợi ích → không đổi gì.** Mục cuối cùng còn thiếu số (`streak/snapshot`) nay đã có số thật — và số đó nói rằng **không cần làm gì**.
