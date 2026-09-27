-- V005__exercises_order_index.sql
-- Index for the admin exercise list (audit-v19 W3). Applied manually; Flyway is disabled.
-- Created: 2026-09-27
--
-- WHY: GET /api/admin/exercises (page 0, no filter) orders by (order_index, exercise_id) over
-- 43,738 rows. No existing index leads with order_index (the existing ones lead with lesson_id,
-- difficulty, or exercise_type), so the page query and the derived COUNT each performed a full
-- scan. Measured via sys.dm_exec_query_stats (audit-v19 W3, before/after):
--     page query : 1341 logical reads / 197 ms  ->  86 reads / 2 ms
--     count query: 1348 logical reads /  67 ms  -> 100 reads / 2 ms
-- The endpoint's own wall-clock (warm, no filter) improved from a ~107 ms median to ~72 ms.
--
-- SAFETY: additive and idempotent (IF NOT EXISTS). No data change. The index is small
-- (order_index is INT, exercise_id is the clustered key so it is included implicitly).
-- Revert with: DROP INDEX IX_exercises_order_id ON exercises;

SET NOCOUNT ON;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_exercises_order_id' AND object_id = OBJECT_ID('exercises'))
    CREATE NONCLUSTERED INDEX IX_exercises_order_id
        ON exercises(order_index, exercise_id);
GO
