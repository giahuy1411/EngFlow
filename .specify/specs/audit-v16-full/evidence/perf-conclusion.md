# audit-v16-full — Phase 5: Performance (kết luận)

**Nguyên tắc (P5):** chỉ fix "win" rõ; mọi số có before/after; không tối ưu khi chưa đo.

## Đo được (vòng 1, `perf-before.json`, 34 endpoint)

| Chỉ số | Giá trị |
|---|---|
| Median của median | **11.9 ms** |
| Chậm nhất | **163.4 ms** — `admin exercises q=the` (LIKE `%kw%`, đặc tính) |
| Endpoint > 100ms | **1** |

## DB (`db-perf.md`)

- Truy vấn app nóng nhất: **1902 lần chạy, avg 0.14 ms** (nhanh).
- Truy vấn 215ms chỉ chạy **6 lần** (batch/khởi động) → không phải hot path.
- Không phát hiện N+1 trong top slow query.

## Bundle

| Chỉ số | Giá trị |
|---|---|
| Entry `index-*.js` | **177.74 kB** (gzip 67.68) — khớp baseline, không regression |
| Chunk lớn nhất | `markdown-*.js` 64.94 kB (gzip 22.12) |

## CLS (3 route)

| Route | CLS median | Ngưỡng tốt |
|---|---|---|
| `/` | **0.00069** | < 0.1 |
| `/lessons` | **0.00095** | < 0.1 |
| `/login` | **0.00003** | < 0.1 |

→ CLS **xuất sắc**, không cần fix.

## Kết luận Phase 5

**KHÔNG có "win" hiệu năng** ở vòng 1 (backend, DB, bundle, CLS đều tốt). Theo P5, **không tối ưu gì thêm** —
tránh "tạo việc" gây rủi ro không cần thiết. Số liệu lưu làm baseline; **vòng 2 đo lại** để xác nhận ổn định.

`perf-after` = chạy lại `perf-probe.js` ở vòng 2 (`perf-after.json`), kỳ vọng tương đương (không đổi code backend).
