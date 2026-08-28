IF NOT EXISTS (SELECT * FROM sys.tables WHERE object_id = OBJECT_ID(N'dbo.speaking_prompts'))
BEGIN
    EXEC sp_rename 'dbo.video_speaking_prompts', 'speaking_prompts';
END
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE object_id = OBJECT_ID(N'dbo.speaking_submissions'))
BEGIN
    EXEC sp_rename 'dbo.video_submissions', 'speaking_submissions';
END
GO
