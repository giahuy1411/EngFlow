-- Drop study_sessions table if exists
IF OBJECT_ID('study_sessions', 'U') IS NOT NULL
BEGIN
    DROP TABLE study_sessions;
END
