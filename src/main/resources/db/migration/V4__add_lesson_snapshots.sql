CREATE TABLE lesson_snapshots (
    snapshot_id BIGINT PRIMARY KEY IDENTITY(1,1),
    lesson_id BIGINT NOT NULL,
    snapshot NVARCHAR(MAX) NOT NULL,
    created_by BIGINT,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_snapshots_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

CREATE INDEX idx_lesson_snapshots_lesson ON lesson_snapshots(lesson_id);
