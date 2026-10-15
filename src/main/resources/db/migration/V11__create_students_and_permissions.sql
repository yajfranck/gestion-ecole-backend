CREATE TABLE students (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    student_number VARCHAR(40) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_students_establishment_number UNIQUE (establishment_id, student_number),
    CONSTRAINT fk_students_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

CREATE INDEX idx_students_establishment_enabled_name
    ON students (establishment_id, enabled, last_name, first_name);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('b9a6a1c1-5159-41fd-a110-000000000001', 'can_student_read', 'Read students in an establishment'),
    ('b9a6a1c1-5159-41fd-a110-000000000002', 'can_student_create', 'Create students in an establishment'),
    ('b9a6a1c1-5159-41fd-a110-000000000003', 'can_student_update', 'Update students in an establishment'),
    ('b9a6a1c1-5159-41fd-a110-000000000004', 'can_student_delete', 'Archive students in an establishment');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_student_read',
        'can_student_create',
        'can_student_update',
        'can_student_delete'
    );