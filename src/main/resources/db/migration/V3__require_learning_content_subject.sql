-- Existing content must be assigned a subject before applying this migration.
-- Do not infer a subject or remove historical content automatically.
ALTER TABLE learning_contents ALTER COLUMN subject_id SET NOT NULL;
