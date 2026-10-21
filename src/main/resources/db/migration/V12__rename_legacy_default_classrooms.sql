UPDATE classrooms
SET code = 'LEGACY-' || SUBSTRING(REPLACE(CAST(id AS VARCHAR), '-', ''), 1, 8),
    name = 'Groupe ' || SUBSTRING(CAST(id AS VARCHAR), 1, 8)
WHERE code = 'DEFAULT' AND name = 'Salle principale';