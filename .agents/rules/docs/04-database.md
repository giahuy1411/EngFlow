# 04 — Database / SQL Server

---

## 1. Naming Convention

```
🔴 Tất cả object database dùng snake_case
🔴 Tên bảng là danh từ số nhiều
🔴 PK luôn là <table_singular>_id
🔴 FK là <referenced_table_singular>_id
```

| Object | Convention | Ví dụ |
|--------|-----------|-------|
| Bảng | snake_case, số nhiều | `exercise_submissions` |
| Cột PK | `<singular>_id` | `lesson_id`, `user_id` |
| Cột FK | `<ref_singular>_id` | `lesson_id` trong bảng `exercises` |
| Index | `idx_<table>_<col(s)>` | `idx_progress_user_lesson` |
| View | `vw_<subject>` | `vw_lesson_stats` |
| Stored Proc | `sp_<verb>_<subject>` | `sp_get_user_progress` |
| Constraint | `uq_<table>_<col>`, `fk_<table>_<ref>` | `uq_users_email` |

---

## 2. Data Types

```
🔴 BIGINT IDENTITY(1,1) cho mọi PK — không dùng INT
🔴 NVARCHAR cho mọi text (hỗ trợ tiếng Việt) — không dùng VARCHAR
🔴 DATETIME2 cho timestamp — không dùng DATETIME (precision tốt hơn)
🔴 BIT cho boolean — không dùng TINYINT hay INT
🟡 DECIMAL(precision, scale) rõ ràng cho số thập phân
```

```sql
-- ✅ Đúng
user_id     BIGINT        PRIMARY KEY IDENTITY(1,1)
full_name   NVARCHAR(100) NOT NULL
created_at  DATETIME2     NOT NULL DEFAULT GETDATE()
is_active   BIT           NOT NULL DEFAULT 1
score       DECIMAL(5,2)  NOT NULL DEFAULT 0.00

-- ❌ Sai
user_id     INT           IDENTITY(1,1)   -- INT thay vì BIGINT
full_name   VARCHAR(100)                  -- VARCHAR mất tiếng Việt
created_at  DATETIME                      -- DATETIME cũ
is_active   TINYINT                       -- dùng BIT cho boolean
```

---

## 3. Constraints

```
🔴 Mọi FK phải khai báo rõ ON DELETE action
🔴 Cột unique phải có UNIQUE CONSTRAINT, không chỉ dựa vào index
🟡 Luôn có NOT NULL trừ khi trường thực sự optional
```

```sql
-- ✅ FK với ON DELETE rõ ràng
CONSTRAINT fk_exercises_lesson
    FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id)
    ON DELETE CASCADE        -- xóa lesson → xóa exercises theo

-- Nếu không muốn cascade:
    ON DELETE NO ACTION      -- báo lỗi nếu có exercises còn tham chiếu

-- ✅ UNIQUE constraint
CONSTRAINT uq_users_email    UNIQUE (email),
CONSTRAINT uq_users_username UNIQUE (username)
```

---

## 4. Indexing Rules

```
🔴 Mọi FK cột phải có index
🔴 Cột hay dùng trong WHERE/JOIN phải có index
🟡 Index composite khi thường query cùng lúc nhiều cột
🟢 Review query plan (SET STATISTICS IO ON) trước khi add index mới
```

```sql
-- ✅ Index bắt buộc
CREATE INDEX idx_exercises_lesson          ON exercises(lesson_id);
CREATE INDEX idx_progress_user             ON user_progress(user_id);
CREATE INDEX idx_progress_lesson           ON user_progress(lesson_id);
CREATE INDEX idx_submissions_user          ON exercise_submissions(user_id);
CREATE INDEX idx_submissions_exercise      ON exercise_submissions(exercise_id);
CREATE INDEX idx_vocabulary_lesson         ON vocabulary(lesson_id);

-- ✅ Composite index (thường query cùng nhau)
CREATE INDEX idx_progress_user_lesson ON user_progress(user_id, lesson_id);
CREATE INDEX idx_lessons_level_pub    ON lessons(level, is_published);
```

---

## 5. Query Rules

```
🔴 Không dùng SELECT * trong production code
🔴 Luôn dùng parameterized queries — không concatenate string SQL
🔴 Không viết business logic trong stored procedure — chỉ dùng cho reporting
🟡 Query phức tạp (JOIN nhiều bảng, subquery) phải có comment giải thích
🟢 EXPLAIN / Query Analyzer trước khi merge query quan trọng
```

```sql
-- ✅ Select cột cụ thể
SELECT l.lesson_id, l.title, l.level, up.completion_pct
FROM lessons l
JOIN user_progress up ON l.lesson_id = up.lesson_id
WHERE up.user_id = @userId
  AND l.is_published = 1
ORDER BY up.last_accessed DESC;

-- ❌ SELECT *
SELECT * FROM lessons JOIN user_progress ...

-- ✅ Parameterized (Spring JPA @Query)
@Query("SELECT l FROM Lesson l WHERE l.level = :level AND l.isPublished = true")
List<Lesson> findByLevel(@Param("level") LessonLevel level);

-- ❌ String concatenation (SQL injection risk)
String sql = "SELECT * FROM lessons WHERE level = '" + level + "'";
```

---

## 6. Migration Rules

```
🔴 Mọi thay đổi schema phải qua migration file — không ALTER trực tiếp trên production DB
🔴 Tên file migration: V<version>__<description>.sql (Flyway convention)
🔴 Migration phải idempotent khi có thể
🔴 Không xóa/đổi tên cột đang được dùng — deprecated trước, xóa sau 1 sprint
🟡 Mỗi migration nên nhỏ và làm 1 việc
```

```sql
-- ✅ Tên file đúng
V1__create_users_table.sql
V2__create_lessons_table.sql
V3__add_avatar_url_to_users.sql
V4__create_exercise_submissions.sql

-- ✅ Template migration
-- File: V5__add_streak_to_users.sql
-- Date: 2024-01-20
-- Author: dev-name
-- Description: Thêm cột streak_days cho gamification

BEGIN TRANSACTION;

ALTER TABLE users
ADD streak_days INT NOT NULL DEFAULT 0;

COMMIT TRANSACTION;
```

---

## 7. JSON trong Database

```
🟡 Lưu JSON trong NVARCHAR(MAX) — SQL Server 2019 hỗ trợ JSON functions
🔴 Không dùng JSON cho data cần query/filter — normalize thành bảng riêng
🟢 Dùng JSON chỉ cho data phụ không cần search (ví dụ: options của câu hỏi)
```

```sql
-- ✅ Dùng JSON cho options (chỉ đọc, không filter)
-- exercises.options = '["Option A", "Option B", "Option C", "Option D"]'

-- ❌ Dùng JSON cho data cần query
-- users.preferences = '{"level": "BEGINNER"}' -- sẽ khó query sau này
-- → Nên tạo cột riêng: preferred_level NVARCHAR(20)
```

---

## 8. Performance

```
🔴 Không có N+1 query — dùng JOIN hoặc @EntityGraph trong JPA
🟡 Bật lazy loading mặc định cho @OneToMany relationships
🟡 Dùng projection (interface-based) cho query chỉ cần 1 vài cột
```

```java
// ✅ Tránh N+1 — fetch join
@Query("SELECT l FROM Lesson l LEFT JOIN FETCH l.exercises WHERE l.id = :id")
Optional<Lesson> findByIdWithExercises(@Param("id") Long id);

// ✅ Projection — chỉ lấy id và title
public interface LessonSummary {
    Long getId();
    String getTitle();
    String getLevel();
}
List<LessonSummary> findAllByIsPublishedTrue();
```
