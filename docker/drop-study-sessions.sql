-- Script to drop the unused study_sessions table
-- Run this against the english_learning database

IF OBJECT_ID('dbo.study_sessions', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.study_sessions;
    PRINT 'Table study_sessions dropped successfully.';
END
ELSE
BEGIN
    PRINT 'Table study_sessions does not exist, nothing to drop.';
END
GO
