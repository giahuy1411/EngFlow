# Hướng dẫn vẽ ERD & Code SQL Database — EngFlow

> Nguồn sự thật: **entity classes** trong `src/main/java/com/datn/engflow/model/entity/`
> (Hibernate `ddl-auto=update` tự sinh schema từ entity — **Flyway disabled**, không có migration file).
> Mọi sơ đồ/DDL trong bài luận văn phải khớp tài liệu này.
> Cập nhật lần cuối: **2026-09-22** (audit-v13) — đối chiếu trực tiếp từ SQL Server đang chạy (`english_learning`): 21 bảng thật, 25 FK.

---

## 1. Tổng quan: 21 bảng, chia 7 nhóm chức năng

| # | Nhóm | Bảng | Ghi chú |
|---|------|------|---------|
| 1 | Users & Auth | `users` | 1 bảng duy nhất, mang cả premium/streak counter |
| 2 | Lessons | `lessons`, `lesson_sections`, `lesson_blocks`, `lesson_snapshots` | Cấu trúc bài học 3 tầng: Lesson → Section → Block |
| 3 | Vocabulary & Decks | `vocabulary`, `decks`, `deck_words` | `deck_words` là bảng nối nhiều-nhiều |
| 4 | Progress & SRS | `user_progress`, `user_vocabulary_progress` | `user_vocabulary_progress` = engine SM-2 |
| 5 | Streak & Study Policy | `study_days`, `study_policy` | `study_days` = nguồn sự thật của streak (1 row/ngày/user); `study_policy` = singleton `id=1` |
| 6 | Exercises | `exercises`, `exercise_attempts` | Bảng lớn nhất DB (~43k dòng exercises) |
| 7 | Premium (Speaking + Payments + Video) | `speaking_prompts`, `speaking_submissions`, `payment_transactions`, `video_lessons`, `video_attempts` | |

> ⚠️ Trong DB live **còn** `user_streaks` (1 row) — bảng **legacy**: không có entity Java nào map tới, 0 reader trong code. Streak thật nằm ở `study_days`. **Không đưa `user_streaks` vào ERD luận văn.** Cũng không đưa `sysdiagrams`.

## 2. Quan hệ (25 foreign keys — nền tảng của ERD)

```
users (1) ──< decks (owner_id, nullable = system deck)
users (1) ──< exercise_attempts
users (1) ──< lesson_submissions
users (1) ──< payment_transactions        (nullable — giao dịch chưa gắn user)
users (1) ──< speaking_submissions        (user_id)
users (1) ──< speaking_submissions        (graded_by — admin chấm)
users (1) ──< user_progress
users (1) ──< user_vocabulary_progress
users (1) ──< study_days                  (fk_study_days_user — nguồn streak thật)
users (1) ──< video_attempts              (user_id + graded_by)

lessons (1) ──< lesson_sections ──< lesson_blocks
lessons (1) ──< lesson_snapshots
lessons (1) ──< exercises
lessons (1) ──< lesson_submissions
lessons (1) ──< vocabulary                (lesson_id nullable — từ AI-generated không gắn bài)
lessons (1) ──< speaking_prompts          (lesson_id nullable)
lessons (1) ──< user_progress

vocabulary (1) ──< deck_words >── (1) decks
vocabulary (1) ──< user_vocabulary_progress

speaking_prompts (1) ──< speaking_submissions
video_lessons (1) ──< video_attempts
exercise_attempts: user_id FK + lesson_id là BIGINT thường (không FK)
```

**Cách đọc cardinality khi vẽ:** mỗi dòng `A (1) ──< B` = một đường nối từ khóa chính của A sang khóa ngoại của B, đầu A ghi `1`, đầu B ghi `N` (crow's foot về phía B).

## 3. Hướng dẫn vẽ ERD (Draw.io)

### 3.1. Quy trình
1. Mở [draw.io](https://app.diagrams.net) → tạo file mới, để khổ A4 landscape.
2. Dùng shape **Entity Relation → Table** (kéo thả 21 bảng). Mỗi bảng: tên in đậm + danh sách cột `tên (KIỂU)`, PK đánh `PK`, FK đánh `FK`, UNIQUE đánh `UQ`.
3. Nối đường **crow's foot** theo mục 2 ở trên.
4. Tô màu theo nhóm chức năng (mục 1) — 7 màu, có legend.
5. Sắp xếp: `users` + `lessons` ở trung tâm (2 bảng hub), tỏa ra ngoài theo nhóm.

### 3.2. Checklist đối chiếu sau khi vẽ
- [ ] Đủ 21 bảng, không thừa `user_streaks` (legacy) / `sysdiagrams`
- [ ] `video_lessons` + `video_attempts` **có mặt** (lỗi cũ hay thiếu 2 bảng này)
- [ ] `study_days` + `study_policy` **có mặt** (streak engine hiện tại; lỗi cũ hay thiếu 2 bảng này)
- [ ] `study_days` có **UNIQUE (`user_id`, `study_date`)** và **FK tới `users`**
- [ ] `study_policy` là **singleton** — có CHECK `ck_study_policy_singleton` (`id = 1`)
- [ ] `speaking_submissions` có **2 FK tới users** (`user_id`, `graded_by`)
- [ ] `video_attempts` có **2 FK tới users** (`user_id`, `graded_by`)
- [ ] `decks.owner_id` nullable (system deck không có chủ)
- [ ] `speaking_submissions.score_total` kiểu **FLOAT** (thang 0–10, không phải INT)
- [ ] `exercises` có cột `correct_answer` (lỗi cũ: bản DDL thứ 2 thiếu cột này)

### 3.3. Mẹo
- Muốn nhanh: import SQL vào draw.io — menu **Extras → Edit Diagram** dán XML, hoặc dùng [drawio-sql-plugin]; hoặc vẽ bằng **dbdiagram.io** rồi export ảnh (xem mục 6).
- Trong luận văn FPT Polytechnic: ERD nên tách 2 mức — **luận lý** (chỉ tên bảng + quan hệ) và **vật lý** (đủ cột + kiểu).

## 4. Code SQL DDL hiện tại (21 bảng — trích từ DB live)

> Sinh bởi Hibernate `ddl-auto=update`; tên constraint `UK...` là hash tự động — khi viết lại tay nên đặt tên dễ đọc (`uq_users_email`, ...). Kiểu NVARCHAR độ dài = `length × 2` byte trong `sys.columns`.

```sql
-- ============================================
-- NHÓM 1: USERS & AUTH
-- ============================================
CREATE TABLE users (
    user_id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    username            VARCHAR(50)   NOT NULL UNIQUE,   -- entity khai báo NVARCHAR
    email               VARCHAR(100)  NOT NULL UNIQUE,   -- đăng nhập bằng EMAIL, không phải username
    password_hash       VARCHAR(255)  NOT NULL,          -- bcrypt
    full_name           NVARCHAR(100),
    avatar_url          VARCHAR(300),
    is_admin            BIT DEFAULT 0,
    current_level       VARCHAR(20),                     -- enum LessonLevel: BEGINNER/...
    total_points        INT DEFAULT 0,
    is_active           BIT DEFAULT 1,
    is_premium          BIT DEFAULT 0,
    premium_expiry      DATE,
    ai_generation_count INT DEFAULT 0,
    created_at          DATETIME2,
    updated_at          DATETIME2
);

-- ============================================
-- NHÓM 2: LESSONS (cấu trúc 3 tầng)
-- ============================================
CREATE TABLE lessons (
    lesson_id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    title            NVARCHAR(200) NOT NULL,
    description      NVARCHAR(MAX),
    content          NVARCHAR(MAX),
    content_original NVARCHAR(MAX),      -- bản gốc trước khi AI chỉnh
    level            VARCHAR(20)  NOT NULL,   -- LessonLevel
    category         VARCHAR(50),
    skill_type       VARCHAR(20),             -- SkillType (7 giá trị)
    duration_minutes INT,
    thumbnail_url    VARCHAR(500),
    audio_url        VARCHAR(500),
    order_index      INT,
    is_published     BIT DEFAULT 1,
    created_at       DATETIME2,
    updated_at       DATETIME2
);

CREATE TABLE lesson_sections (
    section_id  BIGINT IDENTITY(1,1) PRIMARY KEY,
    lesson_id   BIGINT NOT NULL REFERENCES lessons(lesson_id),
    title       NVARCHAR(255) NOT NULL,
    order_index INT,
    created_at  DATETIME2,
    updated_at  DATETIME2
);

CREATE TABLE lesson_blocks (
    block_id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    section_id  BIGINT NOT NULL REFERENCES lesson_sections(section_id),
    block_type  VARCHAR(255) NOT NULL,   -- enum BlockType
    data        NVARCHAR(MAX),           -- JSON nội dung block
    order_index INT,
    created_at  DATETIME2,
    updated_at  DATETIME2
);

CREATE TABLE lesson_snapshots (
    snapshot_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    lesson_id   BIGINT NOT NULL REFERENCES lessons(lesson_id),
    snapshot    NVARCHAR(MAX) NOT NULL,  -- JSON toàn bộ bài học (chụp/khôi phục)
    created_by  BIGINT,                  -- user_id admin, không có FK
    created_at  DATETIME2
);

-- ============================================
-- NHÓM 3: VOCABULARY & DECKS
-- ============================================
CREATE TABLE vocabulary (
    vocab_id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    lesson_id        BIGINT REFERENCES lessons(lesson_id),  -- nullable: từ AI không gắn bài
    word             NVARCHAR(100) NOT NULL,
    pronunciation    NVARCHAR(50),
    meaning          NVARCHAR(500),
    example_sentence NVARCHAR(MAX),
    definition_en    NVARCHAR(MAX),
    cefr_level       VARCHAR(10),        -- A1..C2
    source           VARCHAR(50),        -- oxford3000/awl/toeic/ielts/thpt/ai_generated...
    word_type        VARCHAR(50),
    audio_url        VARCHAR(500),
    image_url        VARCHAR(500),
    created_at       DATETIME2
);

CREATE TABLE decks (
    deck_id       BIGINT IDENTITY(1,1) PRIMARY KEY,
    owner_id      BIGINT REFERENCES users(user_id),  -- NULL = system deck
    name          NVARCHAR(200) NOT NULL,
    description   NVARCHAR(MAX),
    source        VARCHAR(50),
    cefr_level    VARCHAR(10),
    is_public     BIT DEFAULT 1,
    thumbnail_url VARCHAR(500),
    created_at    DATETIME2,
    updated_at    DATETIME2
);

CREATE TABLE deck_words (
    deck_word_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    deck_id      BIGINT NOT NULL REFERENCES decks(deck_id),
    vocab_id     BIGINT NOT NULL REFERENCES vocabulary(vocab_id),
    order_index  INT,
    CONSTRAINT uq_deck_words UNIQUE (deck_id, vocab_id)
);

-- ============================================
-- NHÓM 4: PROGRESS & SRS (SM-2)
-- ============================================
CREATE TABLE user_progress (
    progress_id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES users(user_id),
    lesson_id             BIGINT NOT NULL REFERENCES lessons(lesson_id),
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    is_completed          BIT DEFAULT 0,
    last_accessed         DATETIME2,
    completed_at          DATETIME2,
    CONSTRAINT uq_user_progress UNIQUE (user_id, lesson_id)
);

-- Streak engine HIỆN TẠI (audit-v13): 1 row / user / ngày học.
CREATE TABLE study_days (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    study_date DATE NOT NULL,
    CONSTRAINT fk_study_days_user      FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_study_days_user_date UNIQUE (user_id, study_date)
);

-- Singleton cấu hình: chỉ được có đúng 1 row, id = 1.
CREATE TABLE study_policy (
    id             INT PRIMARY KEY,
    effective_from DATE NOT NULL,
    CONSTRAINT ck_study_policy_singleton CHECK (id = 1)
);

-- LEGACY — không có entity Java map tới, 0 reader trong code. Streak thật ở study_days.
-- Giữ lại chỉ để ddl-auto=update không DROP; ĐỪNG vẽ vào ERD luận văn.
CREATE TABLE user_streaks (
    streak_id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(user_id),
    study_date   DATE NOT NULL,
    words_studied INT DEFAULT 0,
    games_played  INT DEFAULT 0,
    created_at    DATETIME2,
    CONSTRAINT uq_user_streaks UNIQUE (user_id, study_date)
);

CREATE TABLE user_vocabulary_progress (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(user_id),
    vocabulary_id    BIGINT NOT NULL REFERENCES vocabulary(vocab_id),
    mastery_level    INT DEFAULT 0,      -- 0 New / 1 Learning / 2 Almost / 3 Mastered
    next_review_date DATETIME2,
    review_count     INT DEFAULT 0,
    ease_factor      FLOAT DEFAULT 2.5,  -- SM-2
    srs_interval     INT DEFAULT 1,
    repetitions      INT DEFAULT 0,
    created_at       DATETIME2,
    updated_at       DATETIME2,
    CONSTRAINT uq_user_vocab UNIQUE (user_id, vocabulary_id)
);

-- ============================================
-- NHÓM 5: EXERCISE ENGINE
-- ============================================
CREATE TABLE exercises (
    exercise_id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    lesson_id      BIGINT NOT NULL REFERENCES lessons(lesson_id),
    question       NVARCHAR(MAX) NOT NULL,
    options        NVARCHAR(MAX),            -- JSON array ["opt1","opt2",...]
    correct_answer NVARCHAR(500) NOT NULL,   -- entity length=500 → live NVARCHAR(1000) byte
    exercise_type  VARCHAR(20) NOT NULL,     -- enum ExerciseType
    difficulty     VARCHAR(10),              -- enum ExerciseDifficulty
    explanation    NVARCHAR(MAX),
    image_url      VARCHAR(500),
    audio_url      VARCHAR(500),
    order_index    INT,
    created_at     DATETIME2,
    updated_at     DATETIME2
);

CREATE TABLE exercise_attempts (
    attempt_id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(user_id),
    lesson_id    BIGINT NOT NULL,            -- BIGINT thường, KHÔNG có FK
    score        INT NOT NULL,
    total        INT NOT NULL,
    percentage   DECIMAL(5,2) NOT NULL,
    details      NVARCHAR(MAX),              -- JSON chi tiết từng câu
    completed_at DATETIME2
);

-- ============================================
-- NHÓM 6: PREMIUM — SPEAKING & PAYMENTS
-- ============================================
CREATE TABLE speaking_prompts (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    title                       NVARCHAR(200) NOT NULL,
    description                 NVARCHAR(MAX),
    prompt                      NVARCHAR(MAX) NOT NULL,
    lesson_id                   BIGINT REFERENCES lessons(lesson_id),  -- nullable
    mode                        VARCHAR(30) DEFAULT 'FREE_SPEAKING',   -- enum SpeakingPromptMode
    reference_text              NVARCHAR(MAX),   -- shadowing: câu gốc để nhại theo
    reference_media_object_key  VARCHAR(500),    -- key MinIO
    reference_media_url         VARCHAR(1000),
    max_duration_seconds        INT DEFAULT 120,
    attempt_limit               INT DEFAULT 10,
    level                       VARCHAR(20),
    category                    VARCHAR(50),
    is_premium                  BIT DEFAULT 0,
    thumbnail_url               VARCHAR(500),
    order_index                 INT,
    is_published                BIT DEFAULT 1,
    created_at                  DATETIME2,
    updated_at                  DATETIME2
);

CREATE TABLE speaking_submissions (
    id                             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                        BIGINT NOT NULL REFERENCES users(user_id),
    prompt_id                      BIGINT NOT NULL REFERENCES speaking_prompts(id),
    video_url                      VARCHAR(500),
    media_object_key               VARCHAR(500),      -- MinIO
    media_type                     VARCHAR(100),
    transcript                     NVARCHAR(MAX),     -- Whisper sinh ra
    status                         VARCHAR(30) DEFAULT 'SUBMITTED',  -- enum SpeakingSubmissionStatus
    assessment_provider            VARCHAR(100),      -- ollama/whisper...
    assessment_error               VARCHAR(500),
    pronunciation_accuracy         FLOAT,             -- 4 dim điểm phát âm chi tiết
    pronunciation_fluency          FLOAT,
    pronunciation_completeness     FLOAT,
    pronunciation_prosody          FLOAT,
    pronunciation_details_json     NVARCHAR(MAX),
    score_pronunciation            INT,               -- rubric 3 chiều (thang 0-10 mỗi chiều)
    score_grammar                  INT,
    score_vocabulary               INT,
    score_fluency                  INT,
    score_total                    FLOAT,             -- ★ DOUBLE 0-10, KHÔNG phải INT
    feedback                       NVARCHAR(MAX),
    admin_feedback                 NVARCHAR(MAX),     -- admin chấm thủ công
    score                          DECIMAL(3,1),      -- điểm admin (khác score_total)
    graded_by                      BIGINT REFERENCES users(user_id),  -- FK thứ 2 tới users
    graded_at                      DATETIME2,
    private_note                   NVARCHAR(MAX),
    submitted_at                   DATETIME2
);

CREATE TABLE payment_transactions (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id VARCHAR(100) UNIQUE,
    user_id        BIGINT REFERENCES users(user_id),  -- nullable
    order_code     VARCHAR(50),
    amount         DECIMAL(18,0),                     -- VND
    gateway        VARCHAR(50),                       -- 'sepay'
    content        NVARCHAR(500),
    status         VARCHAR(20) DEFAULT 'PENDING',     -- PENDING/SUCCESS/FAILED
    plan_type      VARCHAR(20),                       -- 'MONTH' | 'YEAR'
    premium_expiry DATE,
    webhook_raw    NVARCHAR(MAX),                     -- payload SePay để audit
    created_at     DATETIME2
);

-- ============================================
-- NHÓM 7: VIDEO LESSONS (shadowing)
-- ============================================
CREATE TABLE video_lessons (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    title            NVARCHAR(200) NOT NULL,
    description      NVARCHAR(MAX),
    youtube_video_id VARCHAR(20) NOT NULL,
    level            VARCHAR(20) NOT NULL,            -- LessonLevel
    category         NVARCHAR(50),
    duration_seconds INT,
    transcript_json  NVARCHAR(MAX) NOT NULL,          -- phụ đề từng dòng (teacher-authored)
    is_published     BIT DEFAULT 1,
    created_at       DATETIME2,
    updated_at       DATETIME2
);

CREATE TABLE video_attempts (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(user_id),
    video_lesson_id  BIGINT NOT NULL REFERENCES video_lessons(id),
    line_index       INT NOT NULL,                    -- dòng phụ đề người dùng nhại theo
    media_object_key VARCHAR(500),                    -- bản ghi âm MinIO
    media_type       VARCHAR(100),
    status           VARCHAR(30) DEFAULT 'SUBMITTED',
    score            DECIMAL(3,1),                    -- AI chấm (0-10, 1 số lẻ)
    admin_feedback   NVARCHAR(MAX),
    graded_by        BIGINT REFERENCES users(user_id),-- admin chấm lại
    graded_at        DATETIME2,
    submitted_at     DATETIME2
);
```

## 5. Những điểm hay sai khi vẽ ERD / viết DDL (rút từ audit luận văn)

1. **`score_total` là FLOAT** (Double, trung bình 3 chiều rubric, thang 0–10) — đừng viết INT.
2. **Đừng định nghĩa `exercises`/`exercise_attempts` 2 lần** — chỉ 1 lần, và **phải có `correct_answer`**.
3. **Đừng bỏ quên `video_lessons` + `video_attempts`** — module này đang chạy thật (menu "Video" trong admin).
4. `exercise_attempts.lesson_id` **không phải FK** — vẽ đường nối tới `lessons` là sai, chỉ `user_id` có FK.
5. `speaking_submissions` và `video_attempts` mỗi bảng có **2 FK tới `users`** (người nộp + người chấm) — ERD phải thể hiện cả 2 đường.
6. Enum lưu dạng **VARCHAR** (`@Enumerated(STRING)`), không phải số.
7. `users` mang cả trường premium (`is_premium`, `premium_expiry`) — không có bảng premium riêng. Hai cột `current_streak` / `last_study_date` **đã bị xoá** (audit-v13 F-13-08) — streak thật ở `study_days`.
8. Giá Premium thật: **MONTH 10.000đ, YEAR 20.000đ** (`PremiumPage.vue`) — đừng dùng số cũ 99k/890k.
9. Streak/ngày học **không** đọc từ `users` hay `user_streaks` — đọc `study_days` (UNIQUE `user_id`+`study_date`).

## 6. Tái tạo tài liệu khi schema đổi (maintenance)

Schema đổi = **sửa entity + chạy SQL trực tiếp** (không viết migration — xem AGENTS.md). Sau đó cập nhật file này:

```bash
# 1. Liệt kê bảng
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -C -Q \
  "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME"

# 2. Dump cột (đã loại bảng backup)
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -C -W -w 200 -Q \
  "SELECT t.name, c.name, ty.name, c.max_length, c.precision, c.scale, c.is_nullable \
   FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id \
   JOIN sys.types ty ON ty.user_type_id=c.user_type_id \
   WHERE t.name NOT LIKE '%_bak%' AND t.name<>'sysdiagrams' ORDER BY t.name, c.column_id"

# 3. Dump FK (nền tảng vẽ ERD)
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -C -W -w 200 -Q \
  "SELECT OBJECT_NAME(fk.parent_object_id), COL_NAME(fkc.parent_object_id,fkc.parent_column_id), \
          OBJECT_NAME(fk.referenced_object_id), COL_NAME(fkc.referenced_object_id,fkc.referenced_column_id) \
   FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id=fk.object_id"
```

> Lưu ý Git Bash trên Windows: prefix `MSYS_NO_PATHCONV=1` cho `docker exec` nếu không path `/opt/...` sẽ bị biến thành path Windows.

### dbdiagram.io (nếu muốn auto-render ERD)

```dbml
// Cú pháp DBML — dán vào dbdiagram.io, export PNG/SVG đưa vào luận văn
Table users {
  user_id bigint [pk, increment]
  username varchar(50) [unique, not null]
  email varchar(100) [unique, not null]
  password_hash varchar(255) [not null]
  is_admin bit [default: 0]
  is_premium bit [default: 0]
  premium_expiry date
  total_points int [default: 0]
}

Table lessons {
  lesson_id bigint [pk, increment]
  title nvarchar(200) [not null]
  level varchar(20) [not null]
  skill_type varchar(20)
  is_published bit [default: 1]
}

Table vocabulary {
  vocab_id bigint [pk, increment]
  lesson_id bigint [ref: > lessons.lesson_id]
  word nvarchar(100) [not null]
  cefr_level varchar(10)
}

Table decks {
  deck_id bigint [pk, increment]
  owner_id bigint [ref: > users.user_id]
  name nvarchar(200) [not null]
  is_public bit [default: 1]
}

Table deck_words {
  deck_word_id bigint [pk, increment]
  deck_id bigint [ref: > decks.deck_id]
  vocab_id bigint [ref: > vocabulary.vocab_id]
  order_index int
}

Table user_vocabulary_progress {
  id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  vocabulary_id bigint [ref: > vocabulary.vocab_id]
  mastery_level int [default: 0]
  next_review_date datetime2
  ease_factor float [default: 2.5]
  srs_interval int [default: 1]
}

Table user_progress {
  progress_id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  lesson_id bigint [ref: > lessons.lesson_id]
  completion_percentage decimal(5,2) [default: 0]
  is_completed bit [default: 0]
}

Table user_streaks {
  streak_id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  study_date date [not null]
  words_studied int [default: 0]
  games_played int [default: 0]
}

Table study_days {
  id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  study_date date [not null]
  indexes {
    (user_id, study_date) [unique]
  }
}

Table study_policy {
  id int [pk]
  effective_from date [not null]
  note: 'singleton — CHECK (id = 1)'
}

Table lesson_sections {
  section_id bigint [pk, increment]
  lesson_id bigint [ref: > lessons.lesson_id]
  title nvarchar(255) [not null]
  order_index int
}

Table lesson_blocks {
  block_id bigint [pk, increment]
  section_id bigint [ref: > lesson_sections.section_id]
  block_type varchar(255) [not null]
  data nvarchar [note: 'JSON']
  order_index int
}

Table lesson_snapshots {
  snapshot_id bigint [pk, increment]
  lesson_id bigint [ref: > lessons.lesson_id]
  snapshot nvarchar [not null]
  created_by bigint
}

Table exercises {
  exercise_id bigint [pk, increment]
  lesson_id bigint [ref: > lessons.lesson_id]
  question nvarchar [not null]
  options nvarchar
  correct_answer nvarchar(500) [not null]
  exercise_type varchar(20) [not null]
  difficulty varchar(10)
}

Table exercise_attempts {
  attempt_id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  lesson_id bigint [note: 'BIGINT thường, không có FK']
  score int [not null]
  total int [not null]
  percentage decimal(5,2) [not null]
  details nvarchar
}

Table lesson_submissions {
  submission_id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  lesson_id bigint [ref: > lessons.lesson_id]
  skill_type varchar(50) [not null]
  submission_text nvarchar
  audio_url varchar(500)
  score float
  status varchar(20) [not null, default: 'PENDING']
}

Table speaking_prompts {
  id bigint [pk, increment]
  lesson_id bigint [ref: > lessons.lesson_id]
  title nvarchar(200) [not null]
  prompt nvarchar [not null]
  mode varchar(30) [default: 'FREE_SPEAKING']
  reference_text nvarchar
  max_duration_seconds int [default: 120]
  attempt_limit int [default: 10]
  is_premium bit [default: 0]
}

Table speaking_submissions {
  id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  prompt_id bigint [ref: > speaking_prompts.id]
  graded_by bigint [ref: > users.user_id]
  video_url varchar(500)
  transcript nvarchar
  status varchar(30) [default: 'SUBMITTED']
  score_pronunciation int
  score_grammar int
  score_vocabulary int
  score_fluency int
  score_total float [note: '0-10, trung bình rubric — KHÔNG phải INT']
  score decimal(3,1) [note: 'điểm admin chấm']
  admin_feedback nvarchar
}

Table payment_transactions {
  id bigint [pk, increment]
  transaction_id varchar(100) [unique]
  user_id bigint [ref: > users.user_id]
  order_code varchar(50)
  amount decimal(18,0)
  gateway varchar(50)
  status varchar(20) [default: 'PENDING']
  plan_type varchar(20) [note: 'MONTH | YEAR']
  premium_expiry date
  webhook_raw nvarchar
}

Table video_lessons {
  id bigint [pk, increment]
  title nvarchar(200) [not null]
  youtube_video_id varchar(20) [not null]
  level varchar(20) [not null]
  duration_seconds int
  transcript_json nvarchar [not null]
  is_published bit [default: 1]
}

Table video_attempts {
  id bigint [pk, increment]
  user_id bigint [ref: > users.user_id]
  video_lesson_id bigint [ref: > video_lessons.id]
  graded_by bigint [ref: > users.user_id]
  line_index int [not null]
  media_object_key varchar(500)
  status varchar(30) [default: 'SUBMITTED']
  score decimal(3,1)
  admin_feedback nvarchar
}
```

## 7. Số liệu thật trong database (trích `english_learning` live, **2026-09-22**)

> Dùng cho luận văn (chương "Thực hiện dự án" / demo defense). Số liệu này là **thật từ DB đang chạy**, không phải ví dụ minh họa.
> ⚠️ Con số đổi theo dữ liệu người dùng tạo khi test — **đo lại trước khi chốt slide**, đừng chép từ trí nhớ.

### 7.1. Tổng bản ghi theo bảng

| Bảng | Rows | Ghi chú |
|---|---:|---|
| `users` | 72 | gồm admin + premium |
| `lessons` | 1.470 | 4 cấp độ: ELEMENTARY / PRE_INTERMEDIATE / INTERMEDIATE / UPPER_INTERMEDIATE |
| `lesson_sections` / `lesson_blocks` / `lesson_snapshots` | 10 / 15 / 5 | |
| `vocabulary` | 118 | |
| `decks` / `deck_words` | 10 / 100 | |
| `user_progress` / `user_vocabulary_progress` | 21 / 51 | |
| `study_days` / `study_policy` | 3 / 1 | streak engine hiện tại; `study_policy` luôn = 1 (singleton) |
| `user_streaks` | 1 | **legacy** — không vẽ vào ERD |
| `exercises` | **43.735** | ★ bảng lớn nhất DB |
| `exercise_attempts` | 46 | |
| `lesson_submissions` | 4 | |
| `speaking_prompts` / `speaking_submissions` | 7 / 29 | |
| `payment_transactions` | 126 | |
| `video_lessons` / `video_attempts` | 5 / 15 | |

### 7.2. Phân bố bài tập (43.735 dòng)

| exercise_type | n | | difficulty | n |
|---|---:|---|---|---:|
| MULTIPLE_CHOICE | 33.556 | | MEDIUM | 41.789 |
| FILL_BLANK | 9.112 | | EASY | 1.622 |
| TRANSLATION | 377 | | NULL | 312 |
| LISTENING | 358 | | HARD | 12 |
| MATCHING | 332 | | | |

### 7.3. Bài học theo kỹ năng (SkillType)

GRAMMAR 469 · LISTENING 305 · READING 215 · VOCABULARY 142 · WORD_SKILLS 120 · WRITING 111 · SPEAKING 107 (đủ 7 kỹ năng; +1 lesson `skill_type IS NULL` = tổng 1.470).

### 7.4. Dữ liệu mẫu (dùng cho slide demo)

**Users** (`user@gmail.com` = student id 2, `admin@gmail.com` = administrator id 3):

| user_id | username | email | is_admin | is_premium | total_points | current_streak |
|---|---|---|---|---|---|---|
| 2 | student | user@gmail.com | 0 | 1 | 69 | 1 |
| 3 | administrator | admin@gmail.com | 1 | 1 | 31 | 1 |

> ⚠️ `users.current_streak` / `users.last_study_date` là **cột legacy** — streak thật tính từ `study_days`. Đừng đọc 2 cột này để demo chuỗi ngày học.

**Vocabulary** (deck hệ thống Oxford 3000, deck_id 10006):

| word | pronunciation | cefr_level | source |
|---|---|---|---|
| ambitious | /æmˈbɪʃ.əs/ | B2 | OXFORD3000 |
| benefit | /ˈben.ɪ.fɪt/ | A2 | OXFORD3000 |
| candidate | /ˈkæn.dɪ.dət/ | B1 | OXFORD3000 |

**Decks hệ thống**: Oxford 3000 (A1–B2) · Academic Word List (C1) · TOEIC 600 Essential Words (B2) · IELTS Academic: Environment (C1) · THPT Quốc Gia: Lớp 12 (B1).

**Speaking submissions** (27 bài): COMPLETED 7 · GRADED 10 · FAILED 7 · SUBMITTED 2 · NULL 1. Bài id 40018: `score_total = 9.7` (FLOAT — chứng minh cột này không phải INT), grammar 10, fluency 9, status GRADED.

**Video lessons** (5 bài, YouTube ID thật):

| id | title | youtube_video_id | level | duration |
|---|---|---|---|---|
| 1 | Gọi cà phê bằng tiếng Anh | 2VeQTuSSiI0 | ELEMENTARY | 72s |
| 2 | Daily Routine bằng tiếng Anh | reKgQh0E9kg | ELEMENTARY | 96s |
| 3 | Luyện nghe: hội thoại hằng ngày cho người mới | 4EtXW3nnfPI | ELEMENTARY | 42s |
| 4 | Luyện nghe ESL: hội thoại hằng ngày | MfW9rkoDABg | PRE_INTERMEDIATE | 42s |

`video_attempts`: 15 bài nộp (GRADED 6, SUBMITTED 9), điểm cao nhất 9.2 (DECIMAL(3,1)).

**Payments** (123 giao dịch): SUCCESS 17 (16× MONTH + 1× MONTHLY, tổng 150.000đ) · PENDING 106. Gateway: MBBank 16, sepay 1, NULL 106 (giao dịch test cũ).

**SRS** (`user_vocabulary_progress` 51 dòng): mastery 0→1, 1→22, 2→18, 3→10 — phân bố đúng 4 trạng thái SM-2.

**Exercise attempt mẫu** (attempt_id **40102**): user 3 làm lesson 91920, score 1/1 = 100.00%, completed 2026-09-21 00:44:17.

### 7.5. Lưu ý khi dùng số liệu vào luận văn
- Tổng exercise_attempts (46) và lesson_submissions (4) còn ít — nếu cần con số "khuôn mẫu" đẹp hơn cho demo, chạy thêm thao tác trên UI rồi re-query, đừng bịa số.
- `speaking_submissions` có 7 FAILED — đây là hành vi thật (Whisper/Ollama lỗi trên máy yếu), nên trình bày là "hệ thống ghi nhận trạng thái FAILED để retry" thay vì che đi.

## 8. Nguồn tham chiếu trong code

| Thành phần | Đường dẫn |
|---|---|
| 19 entity | `src/main/java/com/datn/engflow/model/entity/` |
| Enums (LessonLevel, SkillType, ExerciseType, BlockType, SpeakingPromptMode, SpeakingSubmissionStatus...) | `src/main/java/com/datn/engflow/model/enums/` |
| Hibernate config (`ddl-auto=update`) | `src/main/resources/application.properties` |
| Script tạo DB | `docker/init-db.sql` (chỉ tạo database, không tạo bảng) |
| UI giá Premium | `frontend/src/views/premium/PremiumPage.vue` |
| DB đang chạy | container `engflow-sqlserver` :1433, database `english_learning` |
