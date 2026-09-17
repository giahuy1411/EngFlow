SET NOCOUNT ON;
SELECT i.index_id, i.name, i.type_desc,
       size_kb = CAST(ISNULL(SUM(a.used_pages),0)*8/1024 AS numeric(10,1)),
       keycols = STUFF((SELECT ',' + c.name FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=0 ORDER BY ic.key_ordinal FOR XML PATH('')),1,1,''),
       inclcols = STUFF((SELECT ',' + c.name FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=1 ORDER BY ic.index_column_id FOR XML PATH('')),1,1,''),
       filt = ISNULL(i.filter_definition,'')
FROM sys.indexes i
LEFT JOIN sys.partitions p ON p.object_id=i.object_id AND p.index_id=i.index_id
LEFT JOIN sys.allocation_units a ON a.container_id = p.hobt_id
WHERE i.object_id = OBJECT_ID('lessons')
GROUP BY i.object_id,i.index_id,i.name,i.type_desc,i.filter_definition ORDER BY size_kb DESC;