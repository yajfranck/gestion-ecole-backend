CREATE TABLE classrooms (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_classrooms_class_code UNIQUE (school_class_id, code),
    CONSTRAINT uq_classrooms_scope UNIQUE (id, school_class_id, academic_year_id, establishment_id),
    CONSTRAINT fk_classrooms_class_scope FOREIGN KEY (school_class_id, academic_year_id, establishment_id)
        REFERENCES school_classes (id, academic_year_id, establishment_id)
);

INSERT INTO classrooms (id, establishment_id, academic_year_id, school_class_id, code, name, enabled, created_at, updated_at)
SELECT id, establishment_id, academic_year_id, id, 'DEFAULT', 'Salle principale', enabled, created_at, updated_at
FROM school_classes;

ALTER TABLE enrollments ADD COLUMN classroom_id UUID;
UPDATE enrollments SET classroom_id = school_class_id;
ALTER TABLE enrollments ALTER COLUMN classroom_id SET NOT NULL;
ALTER TABLE enrollments ADD CONSTRAINT fk_enrollments_classroom_scope
    FOREIGN KEY (classroom_id, school_class_id, academic_year_id, establishment_id)
    REFERENCES classrooms (id, school_class_id, academic_year_id, establishment_id);
CREATE INDEX idx_enrollments_classroom ON enrollments (classroom_id);
CREATE INDEX idx_classrooms_class_enabled_name ON classrooms (school_class_id, enabled, name);

INSERT INTO security_permissions (id, code, description) VALUES
    ('c18e5b15-234a-4ae2-b87a-000000000002', 'can_enrollment_update', 'Change the classroom of a student enrollment');
INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p
WHERE r.code = 'ESTABLISHMENT_ADMIN' AND p.code = 'can_enrollment_update';
