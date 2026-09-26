# audit-v17-full — Phase 2: DB integrity

**Đo phiên này** bằng `sweep/v8/sqlrun.py` (pipe vào `engflow-sqlserver`). Raw: `db-audit-raw.txt`.

## T2.2 — Orphan FK

**22 FK single-column, 0 orphan** (mọi dòng `orphans=0`). Raw: `db-audit-raw.txt` §T2.2.

Các bảng có FK: `exercises`, `deck_words`(×2), `decks`, `exercise_attempts`, `video_attempts`(×3),
`lesson_submissions`(×2), `payment_transactions`, `speaking_prompts`, `speaking_submissions`(×3),
`user_progress`(×2), `user_streaks`, `user_vocabulary_progress`, `study_days`, …

## T2.3 — FK index coverage

**0 FK thiếu leading index** (truy vấn trả về rỗng — mọi cột FK đều có index với `key_ordinal=1`).
→ Giữ nguyên 5 index đã tạo ở `audit-redesign-v1`.

## T2.3b — Đếm

`fk_count=22`, `table_count=19` — **khớp v16**.

## Parity (đo lại sau Phase 1)

```
1470|43738|5|118|29|4|3|12|10   ·   STUDY_DAYS=4   ·   PENDING_PAYMENTS=0
```

## Timezone (xác nhận convention)

`StudyActivityService.STUDY_ZONE = Asia/Ho_Chi_Minh` (`:34`), `today()` (`:138`);
`StreakReminderScheduler` cron `0 0 20 * * * zone=Asia/Ho_Chi_Minh`. Naive-VN convention giữ nguyên (không đổi).
