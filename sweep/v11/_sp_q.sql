SET NOCOUNT ON;
SELECT 'VALUE=' + CASE WHEN MAX(assessment_error) IS NULL THEN 'NULL' ELSE 'set' END FROM speaking_submissions WHERE id = 40045;
