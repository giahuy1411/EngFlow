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

-- Grammar Table
CREATE TABLE grammar (
    grammar_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT,
    title NVARCHAR(200) NOT NULL,
    explanation NVARCHAR(MAX),
    formula NVARCHAR(500),
    examples NVARCHAR(MAX),
    level NVARCHAR(20),
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_grammar_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- Exercises Table
CREATE TABLE exercises (
    exercise_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT,
    title NVARCHAR(200) NOT NULL,
    question NVARCHAR(MAX) NOT NULL,
    exercise_type NVARCHAR(50) NOT NULL,
    options NVARCHAR(MAX), -- JSON string of options
    correct_answer NVARCHAR(MAX) NOT NULL,
    explanation NVARCHAR(MAX),
    points INT DEFAULT 10,
    difficulty NVARCHAR(20),
    audio_url NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_exercises_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
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

-- Exercise Submissions Table
CREATE TABLE exercise_submissions (
    submission_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT,
    exercise_id BIGINT,
    user_answer NVARCHAR(MAX),
    is_correct BIT,
    points_earned INT DEFAULT 0,
    submitted_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id) ON DELETE CASCADE
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

-- Indexes for performance optimization
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_lessons_level ON lessons(level);
CREATE INDEX idx_lessons_category ON lessons(category);
CREATE INDEX idx_vocabulary_lesson ON vocabulary(lesson_id);
CREATE INDEX idx_grammar_lesson ON grammar(lesson_id);
CREATE INDEX idx_exercises_lesson ON exercises(lesson_id);
CREATE INDEX idx_user_progress_user ON user_progress(user_id);
CREATE INDEX idx_user_progress_lesson ON user_progress(lesson_id);
CREATE INDEX idx_user_progress_user_lesson ON user_progress(user_id, lesson_id);
CREATE INDEX idx_submissions_user ON exercise_submissions(user_id);
CREATE INDEX idx_submissions_exercise ON exercise_submissions(exercise_id);
