-- V2: Add lesson_skills table (replaces legacy vocabulary/grammar/exercises)
-- Legacy tables are kept in schema for backwards compatibility (Hibernate manages them)

CREATE TABLE lesson_skills (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT NOT NULL,
    skill_type NVARCHAR(50) NOT NULL,
    content NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_lesson_skills_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

CREATE INDEX idx_lesson_skills_lesson ON lesson_skills(lesson_id);
