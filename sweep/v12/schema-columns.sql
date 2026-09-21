SET NOCOUNT ON;
SELECT t.name AS tbl, c.name AS col, ty.name AS typ
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
JOIN sys.types ty ON ty.user_type_id = c.user_type_id
WHERE t.name IN ('vocabulary','decks','deck_words','user_vocabulary_progress','exercises',
                 'lesson_blocks','lesson_sections','lesson_snapshots','speaking_prompts',
                 'lesson_submissions','user_progress','lessons')
ORDER BY t.name, c.column_id;
