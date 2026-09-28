# audit-v21-full — DB integrity + performance

**Đo:** 2026-09-28 · container `engflow-sqlserver` · DB `english_learning` · script `sweep/harness/_db-audit-v21.sql`
**Bằng chứng thô:** `evidence/db-audit.log`

## 1. Toàn vẹn khoá ngoại (orphan FK)

Quét 8 quan hệ con→cha, **loại NULL**:

| Quan hệ | Orphan |
|---|---|
| exercise_attempts → users | **0** |
| exercise_attempts → lessons | **0** |
| study_days → users | **0** |
| deck_words → decks | **0** |
| user_vocabulary_progress → users | **0** |
| payment_transactions → users | **0** |
| speaking_submissions → users | **0** |
| video_attempts → users | **0** |

→ **0 orphan**. Không có dữ liệu mồ côi.

## 2. Cột FK thiếu index hỗ trợ

Quét `sys.foreign_keys` × `sys.index_columns` (key_ordinal=1): **0 dòng** ⇒ mọi cột FK
đều có index dẫn đầu. Không có full-scan do thiếu index FK.

## 3. Index đã biết — tái kiểm

| Index | Bảng | Loại | Trạng thái |
|---|---|---|---|
| `IX_exercises_order_id` | exercises | NONCLUSTERED | **còn** (audit-v19 W3: 1341→86 reads) |
| `IX_uvp_due` | user_vocabulary_progress | NONCLUSTERED | **còn** (filtered index) |

## 4. Missing index (DMV) — chiều MỚI

`sys.dm_db_missing_index_details` × `_group_stats`: **0 dòng**. SQL Server **không** đề xuất
index nào cho workload hiện tại (sau khi backend chạy các sweep). Nghĩa là: không có truy vấn
nào đang scan nặng tới mức optimizer muốn thêm index. **Không tối ưu khi chưa có số (P5).**

## 5. Kích thước bảng (xác nhận lý do projection)

| Bảng | Tổng MB | LOB MB |
|---|---|---|
| **lessons** | **160** | **159** |
| exercises | 11 | 0 |
| (12 bảng còn lại) | ~0 | 0 |

`lessons` nặng **160 MB cho 1 472 dòng**, trong đó **159 MB là LOB** (`content` +
`content_original` NVARCHAR(MAX)). Đây là **bằng chứng số** cho quy tắc: danh sách KHÔNG được
`JOIN FETCH` entity Lesson; phải dùng projection phẳng (`LessonListProjection`,
`LessonTitle`, `ExerciseLessonProjection`).

## 6. Timezone (tái kiểm)

| Nguồn | Giá trị đo |
|---|---|
| SQL Server `GETDATE()` (container) | `2026-09-28 11:18:41` (UTC) |
| Giờ VN kỳ vọng (+7) | `2026-09-28 18:18:41` |

Lệch đúng **7h** ⇒ khớp quy ước đã verify (audit-v7/v13): cột `datetime2` là **naive giờ VN**
do backend JVM (`TZ=Asia/Ho_Chi_Minh`) ghi; SQL Server container chạy UTC. Khi so sánh timestamp
phải dùng cùng naive-VN clock; `SYSDATETIME()`/`GETDATE()` trong sqlcmd sẽ lệch −7h (nguồn
false-positive "future date" cũ).

## 7. Hiệu năng endpoint (perf-probe, 34 endpoint, median)

| Endpoint | Median |
|---|---|
| `admin exercise search q=the` | **155.5 ms** (LIKE `%kw%` trên 43 738 row — không index cứu được) |
| `user progress` | 141.8 ms |
| `dashboard stats` | 99 ms |
| `game session (quiz)` | 67.5 ms |
| `auth me` | 76.7 ms |
| `admin lessons` | 40.5 ms |
| `admin exercise list (no q)` | 48.3 ms |
| **còn lại (27 endpoint)** | **11.8 – 58 ms** |

Không endpoint nào vượt 200 ms. `q=the` khớp AGENTS.md (~185 ms, đây đo 155.5 ms — nhanh hơn, plan cache ấm).

**Bằng chứng:** `evidence/db-audit.log`, `evidence/perf---audit.json`, `evidence/perf-probe.log`.
