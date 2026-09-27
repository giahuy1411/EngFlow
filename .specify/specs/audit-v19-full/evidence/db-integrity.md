# audit-v19-full — Phase 2: DB audit + secret scan

**Ngày:** 2026-09-27 · **Nguồn:** `sweep/v8/sqlrun.py` + `sys.*` DMVs trên `engflow-sqlserver`.

## T2.1 Parity + marker
```
1470|43738|5|118|29|4|3|12|10
STUDY_DAYS=4 · PENDING_PAYMENTS=0 · EXERCISE_ATTEMPTS=33
```
→ Khớp baseline Phase 0.

## T2.2 Orphan FK = 0 (loại NULL)
| Quan hệ | Orphan |
|---|---|
| vocabulary→lessons | **0** |
| decks→users | **0** |
| speaking_submissions→users | **0** |
| speaking_submissions→speaking_prompts | **0** |
| speaking_prompts→lessons | **0** |
| video_attempts→users | **0** |
| video_attempts→video_lessons | **0** |
| exercise_attempts→users | **0** |
| payment_transactions→users | **0** |

→ **0 orphan thật.** (Loại NULL — bài học v18: FK nullable bị đếm nhầm.)

## T2.3 FK leading-index coverage = 0 thiếu
**22/22 FK `HAS_INDEX`**, **0 `MISSING`**, 0 SQL error.

## T2.4 Slow query
Top theo `total_logical_reads`: JDBC `sp_columns_100`/`sp_fkeys` (metadata lúc boot) + schema-metadata;
query nghiệp vụ nặng nhất vẫn là **admin exercises** (LIKE `%kw%`, 7 exec, 5 275 reads, 129 ms).
Không N+1. (Đây là candidate **W3** — sẽ đo before/after ở Phase 5.)

## T2.5 Timezone
| Nguồn | Giá trị |
|---|---|
| SQL Server `SYSDATETIME()` | `2026-09-27 12:49 UTC` |
| Container backend | `+07` (TZ=Asia/Ho_Chi_Minh) |
| `study_days` | 4 hàng, max `2026-09-22` (naive VN) |
→ Nhất quán naive-VN (SQL UTC −7h).

## T2.6 `ddl-auto=validate` drill — PASS
`docker compose run --rm --no-deps -T -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate backend`:
**`Started EngflowApplication in 12.29 seconds`**, **0 ERROR**, 0 schema-warning → entity ↔ DB khớp.
Container `engflow-backend-run-*` đã stop theo **tên** (không dùng ancestor filter).

## T2.7 Secret scan — CLEAN
- Secret pattern trong tracked source (trừ `.env`/node_modules) = **0 hit**.
- `.env`/`.env.bak-*` **không** bị track; chỉ `.env.example` (template) được track.
- `application.properties`: mọi secret qua `${ENV_VAR}`.

**Quan sát bảo mật (báo người dùng, không tự đổi):** `.env` + 4 `.env.bak-*` (gitignored) chứa secret thật;
`.agents/mcp_config.json` chứa Figma OAuth secret (gitignored); `~/.claude/settings.json` có API key plaintext (ngoài repo).

## T2.8 Backup
Không có DML hàng loạt trong vòng này (probe tự dọn); backup chỉ cần nếu W4 xử lý row tiền thật — xem W4.
