# audit-v18-full — Phase 5: Performance

**Ngày:** 2026-09-27 · **Nguồn:** `sweep/harness/perf-probe.js before` (34 endpoint, median của 5 lần chạy).

## T5.1 Before

| Chỉ số | Giá trị |
|---|---|
| Số endpoint | 34 |
| Median của median | **9.6 ms** |
| Nhanh nhất | 4.9 ms |
| Chậm nhất | **157.3 ms** (admin exercise search `q=the`) |

**Top 5 chậm nhất:**

| Endpoint | Median |
|---|---|
| admin exercise search `q=the` | 157.3 ms |
| admin exercise list (no q) | 42.8 ms |
| admin stats | 17.1 ms |
| lesson list (page 5) | 14.1 ms |
| leaderboard | 12.8 ms |

## T5.2 N+1 / slow query

Từ Phase 2 (`sys.dm_exec_query_stats`): không có N+1. Query nghiệp vụ nặng nhất là **admin exercises**
(LIKE `%kw%` + join lessons, 40–193 ms) — đặc tính đã biết, **không index nào cứu leading wildcard**
(AGENTS.md ghi rõ "đừng tối ưu bằng cách thêm index"). Các query nặng còn lại là JDBC metadata lúc boot.

## T5.3 Bundle

Entry `index-*.js` = **177.75 kB** (gzip 67.69) — so v17 (177.74) **+0.01 kB** (không đổi thực chất).

## T5.4 Kết luận: **KHÔNG có win rõ → KHÔNG tối ưu**

Hiến pháp **P5 cấm tối ưu khi chưa đo được win**. Toàn bộ 34 endpoint ≤ 157 ms (đa số < 20 ms); bundle không
tăng; CLS ≤ 0.00095; không N+1. **Không có mục tiêu nào để tối ưu** mà không vi phạm P5 (thêm index cho LIKE
leading-wildcard là vô ích; nới/bỏ rate-limit là đánh đổi an ninh không có bằng chứng). → Giữ nguyên.
(khớp kết luận v17.)

## T5.5 Redis

Rate-limit bucket hoạt động (`flushBuckets` clear được `rate_limit:*`); cache `dictionary::hello` tồn tại;
game session dùng Redis. Xác nhận Redis khỏe (api-sweep 0 self-429, game session 200).
