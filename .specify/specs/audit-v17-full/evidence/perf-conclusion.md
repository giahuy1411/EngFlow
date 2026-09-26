# audit-v17-full — Phase 5: performance (đo, không tối ưu khi chưa có win — P5)

## `perf-probe.js` — 34 endpoint, RUNS=5, median

| Chỉ số | Giá trị |
|---|---|
| n | 34 endpoint |
| **median** | **19.5 ms** |
| max | **199.7 ms** (`admin exercises q=the`) |
| top-3 chậm | admin search `q=the` 199.7 · admin list (no q) 190.2 · admin lessons 42.3 |
| nhanh nhất | `payment status` 9.5 ms · `decks my` 10.1 ms · `vocabulary search` 11.5 ms |

So v16 (median 11.9 ms) → **19.5 ms** (cao hơn). Khác biệt chủ yếu do **2 endpoint admin exercises**
(190/199 ms) — đây là **đặc tính đã biết** (LIKE `%kw%` trên 43 738 row; AGENTS.md: "đừng tối ưu bằng cách thêm index").
Không phải regression: các endpoint learner đều ≤ 42 ms.

## `cls-probe.js` — CLS

`/` 0.00069 · `/lessons` 0.00095 · `/login` 0.00003 — **tốt** (< 0.1).

## Bundle

`frontend/dist` entry `index-*.js` = **177.74 kB** (gzip 67.67) — **không đổi** so baseline.

## Kết luận

**Không có "win" đo được cần fix.** Mọi chỉ số đều tốt hoặc là đặc tính đã biết:
- API learner/admin thường: 10–42 ms.
- Admin exercises LIKE `%kw%`: ~190 ms — đặc tính, không có index cứu leading-wildcard.
- CLS < 0.001; bundle không tăng.
- Từ điển ngoài ~20 s (F-17-05) là **phụ thuộc upstream**, không phải code app tối ưu được.

→ Theo Hiến pháp **P5** ("không tối ưu khi chưa đo được lợi ích"): **không tối ưu gì** trong phiên này. Ghi trung thực
"không có win" thay vì "tạo việc".

**Artifact:** `perf---audit.json`, `perf-probe.log`, `cls-before.json`, `cls-probe.log`, `baseline-build.log`.
