-- ============================================================================
-- V10 — drop Lesson Builder "Đường B" tables (audit-v15, 2026-09-25)
-- NOTE: Flyway is disabled. Run manually if applying to a real DB.
--
-- The Lesson Builder feature was removed: lesson content comes from the scrape
-- (lessons.content) and exercises live in `exercises`. See docs/lesson-builder-removal.md.
--
-- Applied to the live DB via sweep/v15/t3-drop-route-b.sql.
-- Order: dependent first (lesson_blocks references lesson_sections).
-- ============================================================================

DROP TABLE IF EXISTS lesson_blocks;
DROP TABLE IF EXISTS lesson_sections;
DROP TABLE IF EXISTS lesson_snapshots;
