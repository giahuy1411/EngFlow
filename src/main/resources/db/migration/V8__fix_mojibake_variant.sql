-- ============================================================================
-- V8__fix_mojibake_variant.sql (REVISED)
-- Sửa mojibake checkmark âœ" -> ✓, kèm chuyển cột correct_answer sang nvarchar.
--
-- LÝ DO CHUYỂN CỘT:
--   Cột exercises.correct_answer là varchar (SQL_Latin1_General_CP1_CI_AS),
--   không thể lưu ký tự Unicode như ✓ (U+2713) -> SQL Server ép thành '?' (0x3F).
--   Bản V8 trước đã ghi ✓ vào varchar và mất dữ liệu gốc âœ" (0xE29C22).
--
-- GIẢI PHÁP:
--   1) Chuyển cột sang nvarchar(500) (không mất giá trị ASCII hiện có).
--   2) ALTER FUNCTION bổ sung mapping âœ" (U+00E2 U+0153 U+0022) -> ✓.
--   3) UPDATE các cột chứa pattern âœ.
--   4) Khôi phục 18 rows xác định được (question có ✓/✗) về âœ" rồi để
--      function biến thành ✓. Các rows khác từng là âœ" nhưng không còn
--      dấu vết nhận diện (đã thành '?') thì giữ nguyên - không thể khôi phục.
--
-- Idempotent: REPLACE không khớp thì không đổi; ALTER COLUMN an toàn chạy lại.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Chuyển cột correct_answer varchar(500) -> nvarchar(500)
--    (giữ NOT NULL; giá trị ASCII không đổi)
-- ----------------------------------------------------------------------------
IF EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'exercises' AND COLUMN_NAME = 'correct_answer'
      AND DATA_TYPE = 'varchar'
)
BEGIN
    ALTER TABLE exercises ALTER COLUMN correct_answer NVARCHAR(500) NOT NULL;
    PRINT 'Column converted to nvarchar';
END
ELSE
BEGIN
    PRINT 'Column already nvarchar (or not varchar) - skip';
END
GO

-- ----------------------------------------------------------------------------
-- 2. ALTER FUNCTION bổ sung mapping âœ" -> ✓
-- ----------------------------------------------------------------------------
IF OBJECT_ID('dbo.fn_fix_mojibake') IS NULL
BEGIN
    THROW 50000, 'fn_fix_mojibake does not exist. Run V7 first.', 1;
END;
GO

CREATE OR ALTER FUNCTION dbo.fn_fix_mojibake(@s nvarchar(max))
RETURNS nvarchar(max)
AS
BEGIN
    IF @s IS NULL RETURN NULL;
    -- Stored mojibake chars are CP1252-decoded UTF-8 bytes:
    -- E2 80 98 -> â € ˜ ; E2 80 99 -> â € ™ ; E2 80 9C -> â € œ ; E2 80 9D -> â € "
    -- E2 80 93 -> â € " ; E2 80 A6 -> â € ¦ ; E2 82 AC -> â ‚ ¬ ; E2 9C 93 -> â œ "
    -- E2 9C 97 -> â œ — ; E2 96 BB -> â – » ; C2 B0 -> Â ° ; C2 A3 -> Â £ ; C2 A5 -> Â ¥
    -- E2 86 92 -> â † ' ; E2 86 93 -> â † " ; E2 88 92 -> â ˆ ' ; C2 AD -> Â  ; C2 BA -> Â º
    -- Codepoint sequences (verified by varbinary probe):
    -- â€˜ = U+00E2 U+20AC U+02DC -> U+2018
    -- â€™ = U+00E2 U+20AC U+2122 -> U+2019
    -- â€œ = U+00E2 U+20AC U+0153 -> U+201C
    -- â€  = U+00E2 U+20AC U+201D -> U+201D
    -- â€“ = U+00E2 U+20AC U+201C -> U+2013
    -- â€¦ = U+00E2 U+20AC U+2026 -> U+2026
    -- â‚¬ = U+00E2 U+201A U+20AC -> U+20AC
    -- âœ“ = U+00E2 U+0153 U+201C -> U+2713
    -- âœ— = U+00E2 U+0153 U+2014 -> U+2717
    -- â—» = U+00E2 U+2014 U+00BB -> U+25FB
    -- Â°  = U+00C2 U+00B0 -> U+00B0
    -- Â£  = U+00C2 U+00A3 -> U+00A3
    -- Â¥  = U+00C2 U+00A5 -> U+00A5
    -- â†' = U+00E2 U+2020 U+2019 -> U+2192
    -- â†" = U+00E2 U+2020 U+201C -> U+2193
    -- âˆ' = U+00E2 U+02C6 U+2019 -> U+2212
    -- Â  = U+00C2 U+00AD -> U+00AD
    -- Âº  = U+00C2 U+00BA -> U+00BA
    -- V8 additions (varbinary probe 0xE29C22):
    -- âœ" = U+00E2 U+0153 U+0022 -> U+2713 (checkmark, straight-quote variant)
    -- âœ" = U+00E2 U+0153 U+0027 -> U+2713 (checkmark, apostrophe variant)
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x02DC), NCHAR(0x2018));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x2122), NCHAR(0x2019));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x0153), NCHAR(0x201C));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x201D), NCHAR(0x201D));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x201C), NCHAR(0x2013));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x20AC)+NCHAR(0x2026), NCHAR(0x2026));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x201A)+NCHAR(0x20AC), NCHAR(0x20AC));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x0153)+NCHAR(0x201C), NCHAR(0x2713));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x0153)+NCHAR(0x2014), NCHAR(0x2717));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x2014)+NCHAR(0x00BB), NCHAR(0x25FB));
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00B0)+N'C', NCHAR(0x00B0)+N'C');
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00B0), NCHAR(0x00B0));
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00A3), NCHAR(0x00A3));
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00A5), NCHAR(0x00A5));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x2020)+NCHAR(0x2019), NCHAR(0x2192));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x2020)+NCHAR(0x201C), NCHAR(0x2193));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x02C6)+NCHAR(0x2019), NCHAR(0x2212));
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00AD), NCHAR(0x00AD));
    SET @s = REPLACE(@s, NCHAR(0x00C2)+NCHAR(0x00BA), NCHAR(0x00BA));
    -- V8: straight-quote variants of the checkmark
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x0153)+NCHAR(0x0022), NCHAR(0x2713));
    SET @s = REPLACE(@s, NCHAR(0x00E2)+NCHAR(0x0153)+NCHAR(0x0027), NCHAR(0x2713));
    RETURN @s;
END
GO

-- ----------------------------------------------------------------------------
-- 3. Khôi phục 18 rows xác định được (question chứa ✓/✗) về âœ" ban đầu
--    để function biến thành ✓ đúng nghĩa (chỉ khi chúng đang là '?')
-- ----------------------------------------------------------------------------
UPDATE exercises SET correct_answer = NCHAR(0x00E2)+NCHAR(0x0153)+NCHAR(0x0022)
WHERE CONVERT(VARBINARY(MAX), correct_answer) = 0x3F
  AND (question LIKE N'%' + NCHAR(0x2713) + N'%' OR question LIKE N'%' + NCHAR(0x2717) + N'%');
PRINT 'Restored identifiable placeholder rows';
GO

-- ----------------------------------------------------------------------------
-- 4. Chạy function fix trên toàn bộ cột (giờ là nvarchar, sẽ lưu được ✓)
-- ----------------------------------------------------------------------------
UPDATE exercises SET correct_answer = dbo.fn_fix_mojibake(correct_answer)
WHERE correct_answer LIKE N'%âœ%';
PRINT 'Applied fn_fix_mojibake to correct_answer';
GO
