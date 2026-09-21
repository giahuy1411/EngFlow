SET NOCOUNT ON;
-- Every FK that points at the tables we are about to delete from.
SELECT fk.name AS fk_name,
       OBJECT_NAME(fk.parent_object_id) AS child_table,
       COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS child_col,
       OBJECT_NAME(fk.referenced_object_id) AS parent_table,
       COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS parent_col,
       fk.delete_referential_action_desc AS on_delete
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
WHERE OBJECT_NAME(fk.referenced_object_id) IN ('vocabulary','decks','deck_words','lessons','exercises')
ORDER BY parent_table, child_table;
