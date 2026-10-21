ALTER TABLE classrooms ADD COLUMN max_students INTEGER NOT NULL DEFAULT 40;
ALTER TABLE classrooms ADD CONSTRAINT ck_classrooms_max_students_positive CHECK (max_students > 0);