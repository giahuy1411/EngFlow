# audit-v13 — Kiểm chứng độc lập 5 hạng mục OPEN (trước khi quyết định)

**Ngày:** 2026-09-22 (+07) · Mọi số dưới đây do phiên này đo lại, không kế thừa.

## F-13-02 — Lesson Builder không tới học viên — **XÁC NHẬN + MỞ RỘNG**

| Đo | Kết quả |
|---|---|
| Blocks theo loại | QUESTION 3 (1 lesson), SUBMISSION 1, TABLE 1, TEXT 10 (7 lesson) |
| Lesson có blocks | **7 lesson**, trong đó **6 đã publish** (446, 447, 567, 11301, 41881, 91900) |
| Learner đọc blocks? | `grep getStructure` ngoài service = **0** → **không bao giờ** |
| `lesson.content` | Là HTML **scraped** (vd 446: `content_len=5986`, `content_original=21140`) — **không** sinh từ blocks |
| **MỚI: nút "Xem trước" của builder** | `AdminLessonBuilder.vue:29` trỏ `/lessons/{id}/preview` → **route KHÔNG tồn tại** → catch-all (`router/index.js:192`) đá về `/` |
| **Bằng chứng live** | `GET /lessons/446/preview` → URL cuối **`http://localhost:5173/`** (trang chủ) |
| `LessonPreview.vue` | Chỉ hiển thị **lịch sử làm bài**, không phải preview nội dung |

→ Hệ quả nặng hơn báo cáo: admin bấm "Xem trước" **tưởng đã kiểm tra được**, nhưng thực ra về trang chủ. Nội dung builder không tới học viên, và **không có cách nào tự kiểm tra**.

## F-13-07 — `study_policy` thiếu CHECK — chờ agent (đang điều tra)

## F-13-08 — `users.current_streak` cũ — chờ agent (đang điều tra)

## F-13-20 — `?redirect=` không ai đọc — chờ agent (đang điều tra)

## F-13-09 — 3 đồng hồ lệch — **XÁC NHẬN (đã biết, không phải lỗi mới)**

Đo lại độc lập:
```
SYSDATETIME()  = 2026-09-22T06:16:45  (UTC)
GETUTCDATE()   = 2026-09-22T06:16:45  (UTC — giống nhau ⇒ SQL Server chạy UTC)
host VN        = 2026-09-22 13:16:45  (+07)
container JVM  = TZ=Asia/Ho_Chi_Minh, 13:16:46 +07
newest exercise created_at = 2026-09-21T22:01 (naive, giờ VN — khớp phiên test của user)
sys.default_constraints = 0  (không constraint nào dùng GETDATE())
```
→ Đúng như AGENTS.md đã ghi từ audit-v7. **Không phải lỗi mới**, không có consumer thứ hai ⇒ **không migrate**.

## F-13-10 — "3/5 index dư thừa" — **KẾT LUẬN BAN ĐẦU SAI** ❌

Đo lại bằng `sys.indexes` + `sys.dm_db_index_usage_stats` (uptime 12h):

| index | key columns | seeks | scans | updates |
|---|---|---|---|---|
| idx_exercises_type | (exercise_type) | 29 | 0 | 242 |
| idx_exercises_difficulty | (difficulty) | 0 | 147 | 210 |
| idx_exercises_lesson_order | (lesson_id, order_index) | 255 | 11 | 242 |
| idx_exercises_lesson_type_order | (lesson_id, exercise_type, order_index) | 12 | 1 | 160 |
| IX_exercises_lesson_type_diff_order | (lesson_id, exercise_type, difficulty, order_index) | 1485 | 1 | 242 |

**Phân tích prefix (script độc lập):** **KHÔNG** index nào là strict prefix của index khác.
- `lesson_order` = (lesson_id, order_index) ≠ prefix của `lesson_type_order` = (lesson_id, exercise_type, …)
- `type` = (exercise_type) không phải prefix của cái nào (vì `lesson_type_*` bắt đầu bằng lesson_id)

→ **Mọi index đều đang được dùng** (seeks hoặc scans > 0). Tuyên bố "3/5 dư leading column" là **sai**; đây là **kết luận của probe, không phải lỗi sản phẩm**. Cần **đóng** hạng mục này là "không có vấn đề", không phải DEFERRED.
