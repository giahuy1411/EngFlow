# audit-v19-full — Phase 5: Performance

**Ngày:** 2026-09-27 · Nguồn: `perf-probe.js`, `sys.dm_exec_query_stats`.

## T5.1/T5.4 — W3 (admin exercise index) before/after

| Endpoint / query | Before | After | Δ |
|---|---|---|---|
| `GET /api/admin/exercises` (no q) — wall | **42.8 ms** (v18) | **26.6 ms** | **−38%** |
| `GET /api/admin/exercises?q=the` | 157.3 ms | 130.1 ms | −17% |
| Page query logical reads | **1 341** | **86** | **15.6×** |
| Count query logical reads | **1 348** | **100** | **13.5×** |
| Filter by lesson_id logical reads | 1 341 | **22** | **61×** |

**Fix:** `IX_exercises_order_id (order_index, exercise_id)` — `sql/migrations/V005__exercises_order_index.sql`
(additive, idempotent). Xem `w3-perf-admin.md`.

## T5.2 N+1 / slow query

Không N+1 trong request path (leaderboard/SRS đã batch từ v13/v14). Query nghiệp vụ nặng nhất còn lại là
admin exercises LIKE `%the%` (leading wildcard — không index nào cứu; **đặc tính**, không phải bug).

## T5.3 Bundle

`index-*.js` = **177.75 kB** (gzip 67.69) — không đổi (không sửa frontend runtime).

## T5.5 Redis

Rate-limit bucket + cache hoạt động (api-sweep 0 self-429).

## Ràng buộc trung thực (P5)

- W3 là **1 endpoint admin** → median 34 endpoint **không đổi đáng kể** (dao động do backend vừa rebuild).
- Win **đo được rõ** ở logical reads (15×) + wall-clock endpoint (−38%) → **giữ** (P5 thoả).
- **JWT filter DB-per-request** (~0.14ms/9.6ms = 1.5%): **KHÔNG** tối ưu — P5 cấm tối ưu khi lợi < chi phí rủi ro.

## Kết luận

**Có win rõ (W3), đã fix + đo before/after.** Không tối ưu mù.
