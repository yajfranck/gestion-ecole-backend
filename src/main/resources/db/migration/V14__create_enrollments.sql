ALTER TABLE students
    ADD CONSTRAINT uq_students_id_establishment UNIQUE (id, establishment_id);

ALTER TABLE school_classes
    ADD CONSTRAINT uq_school_classes_id_year_establishment
        UNIQUE (id, academic_year_id, establishment_id);

CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_enrollments_student_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT fk_enrollments_student_establishment FOREIGN KEY (student_id, establishment_id)
        REFERENCES students (id, establishment_id),
    CONSTRAINT fk_enrollments_year_establishment FOREIGN KEY (academic_year_id, establishment_id)
        REFERENCES academic_years (id, establishment_id),
    CONSTRAINT fk_enrollments_class_year_establishment
        FOREIGN KEY (school_class_id, academic_year_id, establishment_id)
        REFERENCES school_classes (id, academic_year_id, establishment_id)
);

CREATE INDEX idx_enrollments_establishment_year
    ON enrollments (establishment_id, academic_year_id);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('e14a6db2-0482-45a8-9ee4-000000000001', 'can_enrollment_read', 'Read student enrollments'),
    ('e14a6db2-0482-45a8-9ee4-000000000002', 'can_enrollment_create', 'Enroll students in classes');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN ('can_enrollment_read', 'can_enrollment_create');