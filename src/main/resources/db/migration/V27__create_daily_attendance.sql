CREATE TABLE attendance_records (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    absence_justified BOOLEAN NOT NULL DEFAULT FALSE,
    comment VARCHAR(500),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT ck_attendance_justification CHECK (NOT absence_justified OR status = 'ABSENT'),
    CONSTRAINT uq_attendance_enrollment_date UNIQUE (enrollment_id, attendance_date),
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);

CREATE INDEX idx_attendance_date ON attendance_records (attendance_date);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('c81cc4f3-d7c2-4652-9d08-000000000001', 'can_attendance_read', 'Read daily class attendance'),
    ('c81cc4f3-d7c2-4652-9d08-000000000002', 'can_attendance_write', 'Record and correct daily attendance');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code IN ('can_attendance_read', 'can_attendance_write');