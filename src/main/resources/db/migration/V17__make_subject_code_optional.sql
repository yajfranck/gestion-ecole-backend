-- Preserve historical codes while allowing new subjects to be created by name only.
ALTER TABLE subjects ALTER COLUMN code DROP NOT NULL;
