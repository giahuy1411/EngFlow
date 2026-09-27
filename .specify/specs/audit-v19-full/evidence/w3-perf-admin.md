# audit-v19-full — W3: perf admin-exercise (index, đo before/after)

**Ngày:** 2026-09-27 · **Files:** `sql/migrations/V005__exercises_order_index.sql` (additive, không đổi code app)

## Giả thuyết (từ khảo sát) và kết quả

Khảo sát đề xuất: admin exercises chạy 2 scan + hydrate full entity + query title riêng → áp
`ExerciseLessonProjection` (như đường public). **Đo ra thì giả thuyết SAI một phần:**
- Title **đã** batch sẵn (`ExerciseService.java:431-437`, audit-v8).
- **Projection KHÔNG giảm reads:** đo cột hẹp vs full entity = **1417 vs 1417 reads** (cùng plan — chi phí là
  **sort 43 738 dòng**, không phải độ rộng cột).

→ Nguyên nhân thật: **không có index nào lead bằng `order_index`** (các index hiện có lead `lesson_id`/
`difficulty`/`exercise_type`). Page query + derived count đều **scan cả bảng**.

## Fix: `IX_exercises_order_id (order_index, exercise_id)`

Additive, idempotent, không đổi dữ liệu (`sql/migrations/V005__exercises_order_index.sql`).

## Before / After (`sys.dm_exec_query_stats`, sau `DBCC FREEPROCCACHE`)

| Query | Before (avg reads / ms) | After | Giảm |
|---|---|---|---|
| Page (`TOP 20 … ORDER BY order_index, id`) | **1 341 / 197 ms** | **86 / 2 ms** | **15.6× reads** |
| Derived count | **1 348 / 67 ms** | **100 / 2 ms** | **13.5× reads** |
| Filter by `lesson_id` | 1 341 / 197 ms | **22 / 1 ms** | **61× reads** |
| LIKE `%the%` (leading wildcard) | scan | scan (không đổi) | — (đặc tính) |

**Endpoint wall-clock** (warm, no filter, 5 run, median): **~93 ms → ~63 ms** (−32%).

## Ràng buộc trung thực (P5)

- Đây là **1 endpoint ADMIN**, không phải đường user-facing → **median 34 endpoint sẽ KHÔNG đổi**.
- Wall-clock cải thiện vừa (bảng nằm trong buffer pool) nhưng **logical reads giảm 15×** — win rõ, đo được.
- **Giữ** (P5 thoả: có before/after). Không đụng entity/controller.

## Kiểm chứng

- `IX_exercises_order_id` tồn tại sau migration.
- API `GET /api/admin/exercises` vẫn 200 + đúng total (43738) — không regression.
