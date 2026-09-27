# audit-v18-full — Phase 2: DB audit + secret scan

**Ngày:** 2026-09-27 · **Nguồn:** `sweep/v8/sqlrun.py` + `sys.*` DMVs trên `engflow-sqlserver`.

## T2.1 Parity + marker

```
1470|43738|5|118|29|4|3|12|10
STUDY_DAYS=4
PENDING_PAYMENTS=0
EXERCISE_ATTEMPTS=33
```
→ Khớp baseline Phase 0. 15 bảng có dữ liệu (bảng lớn: exercises 43 738, lessons 1 470, vocabulary 118).

## T2.2 Orphan FK = 0

**22 FK** trên 19 bảng. Lần đầu đếm thấy "orphan" ở 5 FK — **đó là NULL** trong cột FK nullable
(`vocabulary.lesson_id` 118/118 NULL, `decks.owner_id` 10/10 NULL, `speaking_prompts.lesson_id` 7/7 NULL,
`speaking_submissions.graded_by` 18/29 NULL, `video_attempts.graded_by` 1/4 NULL). NULL không join được nên
bị đếm nhầm là orphan.

**Đếm lại có loại NULL:**

| Quan hệ | Orphan |
|---|---|
| vocabulary→lessons | **0** |
| decks→users | **0** |
| speaking_submissions→users | **0** |
| speaking_submissions→speaking_prompts | **0** |
| speaking_prompts→lessons | **0** |
| video_attempts→users | **0** |
| video_attempts→video_lessons | **0** |

→ **0 orphan thật.** (Bài học: orphan-check phải loại NULL, không thì báo động giả.)

## T2.3 FK leading-index coverage = 0 thiếu

Cả **22/22 FK** đều `HAS_INDEX` (có index mà cột FK là key_ordinal=1). Không FK nào thiếu leading index.

## T2.4 Slow query

Top theo `total_logical_reads` (`sys.dm_exec_query_stats`):

| Query | execs | avg reads | avg ms |
|---|---|---|---|
| JDBC `sp_columns_100` (boot metadata) | 98 | 5 361 | 61 |
| JDBC `sp_fkeys` (boot metadata) | 588 | 219 | 1 |
| schema-metadata (boot) | 1 | 120 130 | 230 |
| **exercises page join lessons** (admin list) | 7 | 5 089 | 51 |
| **count exercises join lessons** (admin list) | 7 | 1 348 | 40 |
| **admin exercises page** | 6 | 1 341 | 193 |
| `select users...` (auth me) | 1 940 | 6 | 0 |

→ Phần lớn là **JDBC metadata lúc boot** (không phải query nghiệp vụ). Query nghiệp vụ nặng nhất là
**admin exercises** (LIKE `%kw%` + join lessons, 40–193 ms) — đặc tính đã biết (không index nào cứu leading
wildcard), không phải N+1. `select users` (auth me, 1 940 lần, 0 ms) phản ánh đúng
`JwtAuthenticationFilter` reload user mỗi request — nhanh, không phải bottleneck.

## T2.5 Timezone

| Nguồn | Giá trị |
|---|---|
| SQL Server `SYSDATETIME()` | `2026-09-27 10:56 UTC` |
| Container backend `date` | `2026-09-27 17:56 +07` (TZ=Asia/Ho_Chi_Minh) |
| Host | `2026-09-27 17:56` |
| `study_days` | 4 hàng, `2026-09-21`..`2026-09-22` (naive VN) |
| `study_policy` | id=1, `effective_from=2026-09-20` |

→ SQL Server chạy UTC (lệch −7h so với VN); `StudyActivityService` dùng `Asia/Ho_Chi_Minh`. Nhất quán naive-VN.

## T2.6 `ddl-auto=validate` drill

Chạy sau (Phase 6, khi build ổn định) — boot-drill với `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

## T2.7 Secret scan

| Kiểm | Kết quả |
|---|---|
| Secret pattern trong tracked source (`git ls-files` trừ node_modules/.env) | **0 hit** |
| `.env` / `.env.bak-*` có bị track? | **KHÔNG** (gitignored) — tốt |
| `application.properties` | mọi secret qua `${ENV_VAR}`; chỉ `spring.datasource.username=sa` hardcode (mặc định SQL Server, không phải bí mật) |

**Quan sát bảo mật (báo người dùng, KHÔNG tự đổi trong vòng audit):**
1. `.env` + **4 file `.env.bak-*`** trong working tree chứa secret thật — đã gitignore (OK), nhưng backup cũ nên dọn thủ công.
2. `.agents/mcp_config.json` chứa **Figma OAuth clientSecret** — đã gitignore (`.gitignore:75`), OK.
3. `~/.claude/settings.json` (ngoài repo) chứa **API key plaintext** — người dùng nên xoay.
