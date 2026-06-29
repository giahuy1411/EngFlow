-- Users Table
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(50) NOT NULL CONSTRAINT uq_users_username UNIQUE,
    email NVARCHAR(100) NOT NULL CONSTRAINT uq_users_email UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100),
    avatar_url NVARCHAR(500),
    role NVARCHAR(20) DEFAULT 'USER',
    current_level NVARCHAR(20) DEFAULT 'BEGINNER',
    total_points INT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    is_active BIT DEFAULT 1
);

-- Lessons Table
CREATE TABLE lessons (
    lesson_id BIGINT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    content NVARCHAR(MAX),
    level NVARCHAR(20) NOT NULL,
    category NVARCHAR(50),
    duration_minutes INT,
    thumbnail_url NVARCHAR(500),
    audio_url NVARCHAR(500),
    order_index INT,
    is_published BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Vocabulary Table
CREATE TABLE vocabulary (
    vocab_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT,
    word NVARCHAR(100) NOT NULL,
    pronunciation NVARCHAR(100),
    meaning NVARCHAR(500),
    example_sentence NVARCHAR(MAX),
    audio_url NVARCHAR(500),
    image_url NVARCHAR(500),
    word_type NVARCHAR(50),
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_vocabulary_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- Lesson Skills Table (replaces legacy vocabulary/grammar/exercises)
CREATE TABLE lesson_skills (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT NOT NULL,
    skill_type NVARCHAR(50) NOT NULL,
    content NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_lesson_skills_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- User Progress Table
CREATE TABLE user_progress (
    progress_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT,
    lesson_id BIGINT,
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    is_completed BIT DEFAULT 0,
    last_accessed DATETIME2 DEFAULT GETDATE(),
    completed_at DATETIME2,
    CONSTRAINT uq_user_progress UNIQUE (user_id, lesson_id),
    CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- Lesson Snapshots Table
CREATE TABLE lesson_snapshots (
    snapshot_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT NOT NULL,
    snapshot NVARCHAR(MAX) NOT NULL,
    created_by BIGINT,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_snapshots_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- Achievements Table
CREATE TABLE achievements (
    achievement_id BIGINT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    icon_url NVARCHAR(500),
    points_required INT,
    badge_type NVARCHAR(50),
    created_at DATETIME2 DEFAULT GETDATE()
);

-- User Achievements Table
CREATE TABLE user_achievements (
    user_achievement_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT,
    achievement_id BIGINT,
    earned_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT uq_user_achievements UNIQUE (user_id, achievement_id),
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(achievement_id) ON DELETE CASCADE
);

-- Lesson Sections Table
CREATE TABLE lesson_sections (
    section_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    order_index INT,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_lesson_sections_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- Lesson Blocks Table
CREATE TABLE lesson_blocks (
    block_id BIGINT PRIMARY KEY IDENTITY(1,1),
    section_id BIGINT NOT NULL,
    block_type NVARCHAR(50) NOT NULL,
    data NVARCHAR(MAX),
    order_index INT,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_lesson_blocks_section FOREIGN KEY (section_id) REFERENCES lesson_sections(section_id) ON DELETE CASCADE
);

-- Indexes for performance optimization
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_lessons_level ON lessons(level);
CREATE INDEX idx_lessons_category ON lessons(category);
CREATE INDEX idx_vocabulary_lesson ON vocabulary(lesson_id);
CREATE INDEX idx_lesson_skills_lesson ON lesson_skills(lesson_id);
CREATE INDEX idx_lesson_snapshots_lesson ON lesson_snapshots(lesson_id);
CREATE INDEX idx_user_progress_user ON user_progress(user_id);
CREATE INDEX idx_user_progress_lesson ON user_progress(lesson_id);
CREATE INDEX idx_user_progress_user_lesson ON user_progress(user_id, lesson_id);
CREATE INDEX idx_lesson_sections_lesson ON lesson_sections(lesson_id);
CREATE INDEX idx_lesson_blocks_section ON lesson_blocks(section_id);
