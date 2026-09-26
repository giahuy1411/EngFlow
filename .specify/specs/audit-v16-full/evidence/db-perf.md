# audit-v16-full — Phase 2: slow query (db-perf)

**Nguồn:** `sys.dm_exec_query_stats` (đo 2026-09-26). Đơn vị: `total_elapsed_time` (ms).

## Top theo tổng thời gian

| # | Truy vấn (rút gọn) | exec | total_ms | avg_ms | logical_reads |
|---|---|---|---|---|---|
| 1 | `select e1_0.exercise_id, ... from exercises` (Hibernate projection) | 6 | 1292.1 | **215.35** | 8 046 |
| 2 | `SELECT db_id() as database_id, sm.is_inlineable ...` (SQL Server metadata nội bộ) | 1 | 599.8 | 599.80 | 120 154 |
| 3 | Hibernate param query (`@P0..@P5`) | 6 | 580.6 | 96.77 | 67 769 |
| 4 | `sys.dm_xe_session_targets` (DMV nội bộ) | 15 | 561.3 | 37.42 | 64 |
| 5 | `sys.sp_columns_100` (metadata nội bộ) | 6 | 363.7 | 60.62 | 63 547 |
| 6 | `select u1_0.user_id, ... ai_generation_count ...` (Hibernate) | 1902 | 257.6 | **0.14** | 11 417 |

## Phân tích

- **Truy vấn app nóng nhất (#6)**: 1902 lần chạy, **avg 0.14 ms** → rất nhanh, không phải vấn đề.
- **#1** (215 ms avg) chỉ chạy **6 lần** — nhiều khả năng là batch/projection lúc khởi động hoặc thao tác admin,
  **không phải hot path**; 6 lần × 215 ms = 1.3 s tổng trong suốt vòng đời. **Không phải "win" rõ** (P5: không tối ưu khi chưa chứng minh tác động người dùng).
- **#2/#4/#5**: DMV/metadata nội bộ của SQL Server (không phải app) — bỏ qua.
- Không thấy N+1 rõ ràng trong top (không có truy vấn lặp lại cùng shape với exec cao + avg cao cùng lúc).

## Kết luận

**Không có "win" hiệu năng ở tầng DB trong vòng này.** Ghi số làm baseline; nếu `perf-probe` phát hiện endpoint
chậm thì đối chiếu lại đây. (Kỷ luật AGENTS.md: `GET /api/admin/exercises?q=` LIKE `%kw%` ~185 ms là **đặc tính**,
"đừng tối ưu bằng cách thêm index".)
