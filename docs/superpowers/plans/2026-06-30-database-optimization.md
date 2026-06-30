# Database Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove dead columns, drop orphan tables, fix data integrity constraints, and add missing indexes.

**Architecture:** Changes to JPA entities define the schema via Hibernate `ddl-auto=update` (`application.properties:11`). Flyway is disabled (`spring.flyway.enabled=false`), so V1/V2 SQL files are reference-only. A V2 migration SQL is provided as reference for manual production DB sync.

**Tech Stack:** Spring Boot 4.0.6 (JPA/Hibernate), SQL Server, Maven

---

## Findings

### 1. Dead Column: `users.role`

| Item | Evidence |
|------|----------|
| V1 creates `role NVARCHAR(20) DEFAULT 'USER'` | `V1__init_schema.sql:9` |
| Entity `User.java` has NO `role` field | `User.java` — has `isAdmin` mapped to `is_admin` instead |
| Zero Java code reads `role` from DB | All auth uses `is_admin` via `user.getIsAdmin()` |
| `UserResponse.role` is **computed**, not from DB | `UserService.java:126` — `Boolean.TRUE.equals(user.getIsAdmin()) ? "ADMIN" : "USER"` |
| `UserPrincipal` grants authority from `isAdmin` | `UserPrincipal.java:26` |
| `SecurityConfig` uses `hasRole("ADMIN")` | `SecurityConfig.java:69-74` — which maps to `ROLE_ADMIN` authority from UserPrincipal |

**Conclusion:** `role` column is dead. Remove it.

### 2. Orphan Table: `lesson_skills`

| Item | Evidence |
|------|----------|
| Created in V1 | `V1__init_schema.sql:49-58` |
| No entity class exists | `grep @Table.*lesson_skills → no results` |
| No repository exists | No `LessonSkillRepository` found |
| No service references it | Zero Java references to `lesson_skills` table or `LessonSkill` entity |
| Comment says "replaces legacy" | `V1__init_schema.sql:49` — but actual code uses `lesson_sections`/`lesson_blocks` |

**Conclusion:** Orphan table. Drop it.

### 3. Potentially Orphan Tables: `exercises`, `exercise_submissions`, `grammar`

| Item | Evidence |
|------|----------|
| Referenced in V2 cleanup | `V2__database_cleanup.sql:11-13` — DROP IF EXISTS |
| Not in current V1 migration (only in comment) | `V1__init_schema.sql:49` — "replaces legacy vocabulary/grammar/exercises" |
| May exist in production DB from earlier schema versions | Unknown. V2 uses `DROP IF EXISTS` for safety |

**Conclusion:** Drop if exist. Already handled by V2 SQL.

### 4. Missing NOT NULL Constraints (V1 SQL vs Entity Mismatch)

| Table | Column | V1 SQL (nullable) | Entity annotation |
|-------|--------|-------------------|-------------------|
| `user_progress` | `user_id` | `BIGINT` (nullable) | `@JoinColumn(nullable = false)` |
| `user_progress` | `lesson_id` | `BIGINT` (nullable) | `@JoinColumn(nullable = false)` |
| `user_achievements` | `user_id` | `BIGINT` (nullable) | `@JoinColumn(nullable = false)` |
| `user_achievements` | `achievement_id` | `BIGINT` (nullable) | `@JoinColumn(nullable = false)` |

**Conclusion:** Entity expects these FKs to never be null. DB should enforce it.

### 5. Missing Indexes on FK Columns

Tables created by Hibernate `ddl-auto=update` (not present in V1 migration):

| Table | FK Column | Index? |
|-------|-----------|--------|
| `user_streaks` | `user_id` | Missing |
| `user_vocabulary_progress` | `user_id` | Missing |
| `user_vocabulary_progress` | `vocabulary_id` | Missing |
| `lesson_submissions` | `user_id` | Missing |
| `lesson_submissions` | `lesson_id` | Missing |
| `decks` | `owner_id` | Missing |
| `deck_words` | `deck_id` | Missing |
| `deck_words` | `vocab_id` | Missing |

### 6. Column Width Mismatch (Entity vs V1 SQL)

| Table | Column | V1 SQL width | Entity width |
|-------|--------|-------------|--------------|
| `vocabulary` | `pronunciation` | `NVARCHAR(100)` | `length = 50` (`Vocabulary.java:34`) |
| `users` | `avatar_url` | `NVARCHAR(500)` | `length = 300` (`User.java:39`) |

**Conclusion:** Entity widths are more restrictive. Align DB to entity definitions.

### 7. Active Columns Confirmed NOT Dead

- `vocabulary.meaning` (Vietnamese translation) — actively used in: `GameService`, `SrsService`, `LessonService`, `AdminService`, `AiVocabService`, `VocabularyController`
- `vocabulary.definition_en` (English definition) — actively used in: `SrsService`, `AdminService`, `AiVocabService`, `VocabularyController`
- `lessons.content` — actively used in: `LessonService` (returned in API response), `LessonStructureService` (migrated to sections/blocks if empty), `AdminService` (create/update)

**Conclusion:** Keep all three.

---

## Tasks

### Task 1: Remove `role` from `UserResponse` DTO and `JwtTokenProvider`

**Why:** `role` column is dead in DB. The `role` field in `UserResponse` is a computed DTO field (`isAdmin ? "ADMIN" : "USER"`). Remove it since it's redundant with the client already having access to `isAdmin` or can derive role from it.

**Files to change:**

1. **`src/main/java/com/datn/engflow/model/dto/response/UserResponse.java:20`**
   - Remove `private String role;`

2. **`src/main/java/com/datn/engflow/service/UserService.java:126`**
   - Remove `.role(Boolean.TRUE.equals(user.getIsAdmin()) ? "ADMIN" : "USER")` from builder

3. **`src/main/java/com/datn/engflow/security/JwtTokenProvider.java:27`**
   - Change `generateToken(String email, String role)` → `generateToken(String email, boolean isAdmin)`
   - Change `.claim("role", role)` → `.claim("role", isAdmin ? "ADMIN" : "USER")`

4. **`src/main/java/com/datn/engflow/service/UserService.java:62,78`**
   - Change `generateToken(savedUser.getEmail(), Boolean.TRUE.equals(savedUser.getIsAdmin()) ? "ADMIN" : "USER")`
   - To: `generateToken(savedUser.getEmail(), Boolean.TRUE.equals(savedUser.getIsAdmin()))`
   - Same for line 78

**Verification:**
```powershell
.\mvnw.cmd compile -q
```

**Git:**
```powershell
git add -A; git commit -m "db: remove role from UserResponse and JwtTokenProvider"
```

---

### Task 2: Fix NOT NULL Constraints in V1 Migration and Entity

**Why:** Entity annotations specify `nullable = false` on FKs, but V1 migration SQL omits NOT NULL. Add them.

**No entity changes needed** — entities already have `nullable = false`. Only the V1 SQL and V2 SQL reference need updating.

**Files to change:**

1. **`src/main/resources/db/migration/V1__init_schema.sql:62-68`** — Add NOT NULL:
```sql
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
```

2. **`src/main/resources/db/migration/V1__init_schema.sql:97-99`** — Add NOT NULL:
```sql
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
```

3. **`src/main/resources/db/migration/V2__database_cleanup.sql:16-19`** — Already correct:
```sql
ALTER TABLE user_progress ALTER COLUMN user_id BIGINT NOT NULL;
ALTER TABLE user_progress ALTER COLUMN lesson_id BIGINT NOT NULL;
ALTER TABLE user_achievements ALTER COLUMN user_id BIGINT NOT NULL;
ALTER TABLE user_achievements ALTER COLUMN achievement_id BIGINT NOT NULL;
```

**Verification:**
```powershell
.\mvnw.cmd compile -q
```

**Git:**
```powershell
git add -A; git commit -m "db: add NOT NULL constraints on FK columns"
```

---

### Task 3: Reduce Column Widths in Entity and V1 Migration

**Why:** Entity lengths are smaller than V1 SQL. Align both to the smaller (entity-defined) values.

**Files to change:**

1. **No entity changes needed** — entities already have the correct smaller lengths:
   - `Vocabulary.java:34` — `@Column(length = 50)` ✓
   - `User.java:39` — `@Column(name = "avatar_url", length = 300)` ✓

2. **`src/main/resources/db/migration/V1__init_schema.sql:40`**:
   - Change `pronunciation NVARCHAR(100)` → `pronunciation NVARCHAR(50)`

3. **`src/main/resources/db/migration/V1__init_schema.sql:8`**:
   - Change `avatar_url NVARCHAR(500)` → `avatar_url NVARCHAR(300)`

4. **`src/main/resources/db/migration/V2__database_cleanup.sql:22-23`** — Already correct:
```sql
ALTER TABLE vocabulary ALTER COLUMN pronunciation NVARCHAR(50);
ALTER TABLE users ALTER COLUMN avatar_url NVARCHAR(300);
```

5. **`src/main/resources/db/migration/V1__init_schema.sql:37`**: (already handled — `word NVARCHAR(100) NOT NULL` matches entity `length=100`)

**Verification:**
```powershell
.\mvnw.cmd compile -q
```

**Git:**
```powershell
git add -A; git commit -m "db: reduce column widths to match entity lengths"
```

---

### Task 4: Add Missing Indexes to V2 Migration

**Why:** Tables created by Hibernate `ddl-auto=update` lack FK indexes. Add them for query performance.

**Files to change:**

1. **`src/main/resources/db/migration/V2__database_cleanup.sql`** (verify indexes are present at lines 26-33):
```sql
CREATE INDEX idx_user_streaks_user ON user_streaks(user_id);
CREATE INDEX idx_user_vocab_progress_user ON user_vocabulary_progress(user_id);
CREATE INDEX idx_user_vocab_progress_vocab ON user_vocabulary_progress(vocabulary_id);
CREATE INDEX idx_lesson_submissions_user ON lesson_submissions(user_id);
CREATE INDEX idx_lesson_submissions_lesson ON lesson_submissions(lesson_id);
CREATE INDEX idx_decks_owner ON decks(owner_id);
CREATE INDEX idx_deck_words_deck ON deck_words(deck_id);
CREATE INDEX idx_deck_words_vocab ON deck_words(vocab_id);
```

No entity changes needed — indexes are DB-level, not entity annotations.

**Verification:**
```powershell
.\mvnw.cmd compile -q
```

**Git:**
```powershell
git add -A; git commit -m "db: add missing FK indexes for auto-created tables"
```

---

### Task 5: Drop Orphan Tables — Add to V2 Migration

**Why:** `lesson_skills` has no entity. `exercises`, `exercise_submissions`, `grammar` are legacy.

**Files to change:**

1. **`src/main/resources/db/migration/V2__database_cleanup.sql:9-13`** — Verify these DROP statements exist:
```sql
DROP TABLE IF EXISTS lesson_skills;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS exercise_submissions;
DROP TABLE IF EXISTS grammar;
```

**Verification:**
```powershell
.\mvnw.cmd compile -q
```

**Git:**
```powershell
git add -A; git commit -m "db: drop orphan tables lesson_skills, exercises, exercise_submissions, grammar"
```

---

## Summary of Changes by File

| File | Change Type | Task |
|------|-------------|------|
| `src/main/resources/db/migration/V1__init_schema.sql` | Edit SQL column widths | Task 3 |
| `src/main/resources/db/migration/V1__init_schema.sql` | Edit SQL NOT NULL | Task 2 |
| `src/main/resources/db/migration/V2__database_cleanup.sql` | Verify all cleanup SQL | Tasks 2-5 |
| `src/main/java/.../UserResponse.java` | Remove `role` field | Task 1 |
| `src/main/java/.../UserService.java` | Remove `.role()` from builder, update `generateToken()` calls | Task 1 |
| `src/main/java/.../JwtTokenProvider.java` | Change `generateToken` signature | Task 1 |

**No entity files need changes** — all entities already have the correct annotations.
