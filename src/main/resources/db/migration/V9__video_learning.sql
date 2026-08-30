-- ============================================================================
-- V9__video_learning.sql
-- Tính năng "Học tiếng Anh qua Video" (mô hình Corodomo — nghiên cứu tại
-- docs/research-corodomo-video-learning.md).
--
-- Thiết kế:
--   * video_lessons   : 1 video YouTube (chỉ nhúng IFrame, không tải video)
--                       + transcript JSON từng dòng {start,end,textEn,textVi}.
--   * video_attempts  : bản ghi shadowing theo từng dòng của người học,
--                       nộp cho giáo viên chấm (thang 0-10 + nhận xét),
--                       cùng pattern với speaking_submissions.
--
-- Cột Unicode dùng NVARCHAR (bài học từ V8: varchar CP1 làm mất ✓ và tiếng Việt).
-- ============================================================================

CREATE TABLE video_lessons (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    youtube_video_id VARCHAR(20) NOT NULL,
    level VARCHAR(20) NOT NULL,               -- ELEMENTARY / PRE_INTERMEDIATE / INTERMEDIATE / UPPER_INTERMEDIATE
    category NVARCHAR(50),
    duration_seconds INT,
    transcript_json NVARCHAR(MAX) NOT NULL,   -- [{start,end,textEn,textVi}]
    is_published BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE video_attempts (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    video_lesson_id BIGINT NOT NULL,
    line_index INT NOT NULL,
    media_object_key VARCHAR(500),
    media_type VARCHAR(100),
    status VARCHAR(30) DEFAULT 'SUBMITTED',   -- SUBMITTED / GRADED
    score DECIMAL(3,1),                       -- 0.0 - 10.0 (thang giáo viên)
    admin_feedback NVARCHAR(MAX),
    graded_by BIGINT,
    graded_at DATETIME2,
    submitted_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_video_attempts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_video_attempts_lesson FOREIGN KEY (video_lesson_id) REFERENCES video_lessons(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_attempts_grader FOREIGN KEY (graded_by) REFERENCES users(user_id)
);

CREATE INDEX idx_video_lessons_level ON video_lessons(level, is_published);
CREATE INDEX idx_video_attempts_user ON video_attempts(user_id);
CREATE INDEX idx_video_attempts_lesson ON video_attempts(video_lesson_id);
CREATE INDEX idx_video_attempts_status ON video_attempts(status);
