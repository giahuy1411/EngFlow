# audit-v17-full — Phase 2: DB performance

Đo phiên này bằng `sys.dm_exec_query_stats` (kế hoạch cache nóng sau các sweep).

## Query nóng nhất (theo số lần chạy)

| execs | avg ms | snippet | Nhận xét |
|---|---|---|---|
| **4992** | **0.14** | `select u1_0.user_id, u1_0.ai_generation_count, …` | tra user theo email trong JWT filter — 1 lần/request, **0.14 ms** |
| 1679 | 0.07 | `select sp1_0.id, effective_from from study_policy` | policy streak |
| 1525 | 0.07 | `select sd1_0.study_date from study_days where …` | streak snapshot |
| 242 | 0.12 | (user lookup biến thể) | |
| 140 | 0.21 | `select p1_0.progress_id …` | progress |

→ **Không có N+1** (sẽ hiện thành nhiều query giống nhau, số lần lớn, avg cao). Query nóng nhất **0.14 ms**.

## Query chậm nhất theo avg (locally, > 20 execs)

| execs | avg ms | avg reads | snippet |
|---|---|---|---|
| 24 | 83.81 | 4839 | `select top (@P0) e1_0.exercise_id, audio_url, corr…` (admin exercises page) |
| 24 | 57.73 | 1348 | `select count_big(exercise_id) from exercises … join` |
| 58 | 6.72 | 123 | `count_big(*) from lessons where is_published` |

→ 2 query admin-exercises ~58–84 ms trong DB (khớp API 190–200 ms gồm cả serialize 43 738 row page).
**Đặc tính đã biết** — không thêm index (leading-wildcard `%kw%` không hưởng index).

## Kết luận

DB khỏe: hot path **sub-millisecond**, không N+1, không query app nào > ~84 ms. Không có win tối ưu (P5).
