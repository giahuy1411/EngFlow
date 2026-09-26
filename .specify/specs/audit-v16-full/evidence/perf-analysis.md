# audit-v16-full — Phase 5: Performance (đo trước)

**Công cụ:** `sweep/harness/perf-probe.js` — mỗi endpoint gọi 5 lần, lấy **median**; đọc hết body trước khi dừng đồng hồ;
flush `rate_limit:*` trước mỗi batch. **Chỉ ĐO** (Hiến pháp P5).
**Kết quả:** `evidence/perf-before.json` (34 endpoint).

## Tổng quan

| Chỉ số | Giá trị |
|---|---|
| Số endpoint đo | **34** |
| Median của median | **11.9 ms** |
| Endpoint chậm nhất | **163.4 ms** — `GET /api/admin/exercises?q=the` (LIKE `%kw%`) |
| Số endpoint > 100 ms | **1** |

## Top chậm nhất

| Endpoint | Median | Ghi chú |
|---|---|---|
| `admin exercise search q=the` | 163.4 ms | LIKE `%kw%` trên 43 738 dòng — **đặc tính đã biết** (AGENTS.md: "đừng tối ưu bằng cách thêm index") |
| `admin exercise list (no q)` | 48.9 ms | phân trang 43.7k dòng |
| `vocabulary list (auth)` | 23.6 ms | |
| `lesson list (page 5)` | 22.0 ms | |
| `admin users` | 21.1 ms | |
| `admin stats` | 20.9 ms | |

Phần còn lại (28 endpoint) đều **7–21 ms**.

## Phân tích — có "win" rõ không?

- **Không có endpoint nào chậm bất thường.** Max 163 ms là truy vấn LIKE leading-wildcard — **cố ý không index**
  (index vô ích với `%kw%`, và thêm vào chỉ tăng chi phí ghi).
- Đối chiếu `db-perf.md`: truy vấn app nóng nhất avg **0.14 ms**; truy vấn 215 ms chỉ chạy 6 lần (batch, không phải hot path).
- **Kết luận: KHÔNG có "win" hiệu năng backend ở vòng 1.** Ghi số làm baseline; vòng 2 đo lại để xác nhận ổn định.

## Bundle frontend

| Chỉ số | Giá trị |
|---|---|
| Entry `index-*.js` | **177.74 kB** (gzip **67.68 kB**) |
| Chunk lớn nhất khác | `markdown-*.js` 64.94 kB (gzip 22.12) |
| Build | 8.78 s, exit 0 |

→ Khớp baseline v15 (177.74 kB). **Không có regression bundle.**

## Kết luận

Vòng 1: **không phát hiện vấn đề hiệu năng**. Mọi số có bằng chứng (`perf-before.json`, `baseline-build.log`).
Không tối ưu khi chưa chứng minh tác động (P5).
