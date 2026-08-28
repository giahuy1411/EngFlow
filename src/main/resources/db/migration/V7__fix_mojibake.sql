-- ============================================================================
-- V7__fix_mojibake.sql
-- Sửa mojibake: UTF-8 bytes bị giải mã nhầm thành CP1252.
-- Dùng dbo.fn_fix_mojibake (idempotent).
-- ============================================================================

-- lessons
UPDATE lessons SET title = dbo.fn_fix_mojibake(title)
WHERE title LIKE N'%â€%' OR title LIKE N'%âœ%' OR title LIKE N'%Â%' OR title LIKE N'%â—»%' OR title LIKE N'%â†%' OR title LIKE N'%âˆ%';
UPDATE lessons SET description = dbo.fn_fix_mojibake(description)
WHERE description LIKE N'%â€%' OR description LIKE N'%âœ%' OR description LIKE N'%Â%' OR description LIKE N'%â—»%' OR description LIKE N'%â†%' OR description LIKE N'%âˆ%';
UPDATE lessons SET content = dbo.fn_fix_mojibake(content)
WHERE content LIKE N'%â€%' OR content LIKE N'%âœ%' OR content LIKE N'%Â%' OR content LIKE N'%â—»%' OR content LIKE N'%â†%' OR content LIKE N'%âˆ%';

-- exercises
UPDATE exercises SET question = dbo.fn_fix_mojibake(question)
WHERE question LIKE N'%â€%' OR question LIKE N'%âœ%' OR question LIKE N'%Â%' OR question LIKE N'%â—»%' OR question LIKE N'%â†%' OR question LIKE N'%âˆ%';
UPDATE exercises SET options = dbo.fn_fix_mojibake(options)
WHERE options LIKE N'%â€%' OR options LIKE N'%âœ%' OR options LIKE N'%Â%' OR options LIKE N'%â—»%' OR options LIKE N'%â†%' OR options LIKE N'%âˆ%';
UPDATE exercises SET explanation = dbo.fn_fix_mojibake(explanation)
WHERE explanation LIKE N'%â€%' OR explanation LIKE N'%âœ%' OR explanation LIKE N'%Â%' OR explanation LIKE N'%â—»%' OR explanation LIKE N'%â†%' OR explanation LIKE N'%âˆ%';
UPDATE exercises SET correct_answer = dbo.fn_fix_mojibake(correct_answer)
WHERE correct_answer LIKE N'%â€%' OR correct_answer LIKE N'%âœ%' OR correct_answer LIKE N'%Â%' OR correct_answer LIKE N'%â—»%' OR correct_answer LIKE N'%â†%' OR correct_answer LIKE N'%âˆ%';

-- lesson_blocks
UPDATE lesson_blocks SET data = dbo.fn_fix_mojibake(data)
WHERE data LIKE N'%â€%' OR data LIKE N'%âœ%' OR data LIKE N'%Â%' OR data LIKE N'%â—»%' OR data LIKE N'%â†%' OR data LIKE N'%âˆ%';

-- vocabulary
UPDATE vocabulary SET word = dbo.fn_fix_mojibake(word)
WHERE word LIKE N'%â€%' OR word LIKE N'%âœ%' OR word LIKE N'%Â%' OR word LIKE N'%â—»%' OR word LIKE N'%â†%' OR word LIKE N'%âˆ%';
GO
