ALTER TABLE enrollments
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE enrollments
    ADD CONSTRAINT ck_enrollments_status
        CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE'));

CREATE TABLE enrollment_payment_conditions (
    establishment_id UUID PRIMARY KEY,
    mode VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_enrollment_payment_condition_mode
        CHECK (mode IN ('NONE', 'PARTIAL_PAYMENT', 'FULL_PAYMENT')),
    CONSTRAINT fk_enrollment_payment_conditions_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id)
);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('b70a1e39-6196-4be9-a0fc-000000000001', 'can_enrollment_payment_condition_read', 'Read enrollment payment requirements'),
    ('b70a1e39-6196-4be9-a0fc-000000000002', 'can_enrollment_payment_condition_update', 'Configure enrollment payment requirements');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_enrollment_payment_condition_read',
        'can_enrollment_payment_condition_update'
    );