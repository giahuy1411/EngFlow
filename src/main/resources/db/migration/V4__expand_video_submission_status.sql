SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.video_submissions', N'U') IS NULL
    RETURN;

DECLARE @constraintName SYSNAME;

SELECT TOP (1) @constraintName = checkConstraint.name
FROM sys.check_constraints AS checkConstraint
JOIN sys.columns AS checkedColumn
    ON checkedColumn.object_id = checkConstraint.parent_object_id
    AND checkedColumn.column_id = checkConstraint.parent_column_id
WHERE checkConstraint.parent_object_id = OBJECT_ID(N'dbo.video_submissions')
  AND checkedColumn.name = N'status';

IF @constraintName IS NOT NULL
BEGIN
    DECLARE @dropConstraintSql NVARCHAR(MAX) =
        N'ALTER TABLE dbo.video_submissions DROP CONSTRAINT '
        + QUOTENAME(@constraintName) + N';';
    EXEC sys.sp_executesql @dropConstraintSql;
END

ALTER TABLE dbo.video_submissions WITH CHECK
ADD CONSTRAINT CK_video_submissions_status CHECK (
    status IN (
        'UPLOADED',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'SUBMITTED',
        'UNDER_REVIEW',
        'GRADED'
    )
);

ALTER TABLE dbo.video_submissions
CHECK CONSTRAINT CK_video_submissions_status;
