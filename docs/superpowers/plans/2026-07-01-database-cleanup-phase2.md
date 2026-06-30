# Database Cleanup — Phase 2

Date: 2026-07-01

## Scope

- Drop 3 orphan tables: `shop_items`, `skill_submissions`, `user_shop_items`
- Drop dead column `users.role` (NVARCHAR, replaced by `is_admin` BIT)

---

## Findings

### 1. FOREIGN KEY Constraints

From `sys.foreign_keys` (Docker SQL Server):

| FK Name | Parent Table | Referenced Table |
|---|---|---|
| `FKmyhmqps798d4se159hwb9nf` | `user_shop_items` | `shop_items` |
| `FKbp6ll32537ppt9hhewtb5hv8l` | `user_shop_items` | `users` |
| `FK2mcc17cqo7dluo2n2xihmgq9s` | `skill_submissions` | `users` |

**No active (non-orphan) table references any of the 3 orphan tables as a parent/referenced table.** The listed FKs are all **from** the orphan tables **to** other tables — safe to drop via cascade or by dropping dependent tables first.

### 2. Key Column Usage

- `shop_items.item_id` — PK + unique constraint
- `skill_submissions.submission_id` — PK
- `user_shop_items.user_item_id` — PK; FKs on `(item_id, user_id)` to `shop_items` and `users`

### 3. Row Counts

| Table | Rows |
|---|---|
| `shop_items` | 6 (avatar items, backgrounds) |
| `skill_submissions` | 0 (empty) |
| `user_shop_items` | 0 (empty, junction) |

**Risk:** `shop_items` has 6 production rows — optionally back up before dropping.

### 4. Source Code References

**Java entities/controllers/services:**
- No entity, repository, service, or controller found for `ShopItem`, `SkillSubmission`, or `UserShopItem`.
- `getLessonSkillSubmission` method in `LessonSubmissionService`/`LessonSubmissionController` uses `SkillType` enum — **not** related to the `skill_submissions` table.

**SQL files:**
- No references to `shop_items`, `skill_submissions`, or `user_shop_items` in any `.sql` file.

**Verdict:** Zero code references — safe to drop all 3 tables.

### 5. `users.role` Column

- **Exists** in Docker DB (type `varchar`, has data).
- **Not present** in `User.java` entity — entity uses `isAdmin` (BIT) instead.
- **Zero Java references** to `getRole`, `setRole`, or `.role`.
- V2 migration (`V2__database_cleanup.sql`) already includes `ALTER TABLE users DROP COLUMN role;` but has NOT been applied to Docker (Flyway is disabled).

**Verdict:** Dead column — safe to drop.

---

## Drop Order (dependency-aware)

1. **`user_shop_items`** — FKs to `shop_items` and `users`; no active table references it. Drop first.
2. **`skill_submissions`** — FK to `users`; no active table references it. Drop second.
3. **`shop_items`** — referenced only by `user_shop_items` (already dropped). Drop third.
4. **`users.role`** — dead column, zero code refs. Drop last.

---

## Plan Tasks

### Task 1: Drop `user_shop_items`

```sql
DROP TABLE IF EXISTS user_shop_items;
```

Unblocks `shop_items` FK dependency.

### Task 2: Drop `skill_submissions`

```sql
DROP TABLE IF EXISTS skill_submissions;
```

### Task 3: Drop `shop_items`

```sql
DROP TABLE IF EXISTS shop_items;
```

**Optionally back up first:**
```sql
SELECT * INTO shop_items_backup_20260701 FROM shop_items;
```

### Task 4: Drop `users.role`

```sql
ALTER TABLE users DROP COLUMN role;
```

### Task 5: Update V2 Migration SQL

Add the 4 DROP statements above to `src/main/resources/db/migration/V2__database_cleanup.sql`, preserving the existing `role` drop (already there).

### Task 6: Execute Directly Against Docker

All commands can be run via:

```powershell
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -C -d english_learning -Q "DROP TABLE IF EXISTS user_shop_items;"
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -C -d english_learning -Q "DROP TABLE IF EXISTS skill_submissions;"
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -C -d english_learning -Q "DROP TABLE IF EXISTS shop_items;"
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -C -d english_learning -Q "ALTER TABLE users DROP COLUMN role;"
```

Note: FK constraints on the orphan tables will be automatically dropped when each table is dropped.
