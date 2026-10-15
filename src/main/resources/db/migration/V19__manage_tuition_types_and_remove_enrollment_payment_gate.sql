CREATE TABLE tuition_fee_types (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_tuition_fee_type_establishment_code UNIQUE (establishment_id, code),
    CONSTRAINT uq_tuition_fee_type_establishment_name UNIQUE (establishment_id, name),
    CONSTRAINT uq_tuition_fee_type_id_establishment UNIQUE (id, establishment_id),
    CONSTRAINT fk_tuition_fee_type_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

ALTER TABLE tuition_fee_definitions ADD COLUMN fee_type_id UUID;

ALTER TABLE tuition_fee_definitions
    ADD CONSTRAINT fk_tuition_fee_definition_type_establishment
        FOREIGN KEY (fee_type_id, establishment_id)
        REFERENCES tuition_fee_types (id, establishment_id);

UPDATE enrollments SET status = 'ACTIVE' WHERE status <> 'ACTIVE';
ALTER TABLE enrollments DROP CONSTRAINT ck_enrollments_status;
ALTER TABLE enrollments ADD CONSTRAINT ck_enrollments_status CHECK (status = 'ACTIVE');

DELETE FROM security_role_permissions
WHERE permission_id IN (
    SELECT id FROM security_permissions
    WHERE code IN ('can_enrollment_payment_condition_read', 'can_enrollment_payment_condition_update')
);
DELETE FROM security_permissions
WHERE code IN ('can_enrollment_payment_condition_read', 'can_enrollment_payment_condition_update');
DROP TABLE enrollment_payment_conditions;

INSERT INTO security_permissions (id, code, description)
VALUES
    ('18f6f425-f838-4d30-a73f-000000000001', 'can_tuition_fee_type_read', 'Read tuition fee types'),
    ('18f6f425-f838-4d30-a73f-000000000002', 'can_tuition_fee_type_create', 'Create tuition fee types'),
    ('18f6f425-f838-4d30-a73f-000000000003', 'can_tuition_fee_type_update', 'Update tuition fee types'),
    ('18f6f425-f838-4d30-a73f-000000000004', 'can_tuition_fee_type_delete', 'Soft-delete tuition fee types'),
    ('18f6f425-f838-4d30-a73f-000000000005', 'can_payment_method_delete', 'Soft-delete payment methods');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_tuition_fee_type_read', 'can_tuition_fee_type_create',
        'can_tuition_fee_type_update', 'can_tuition_fee_type_delete',
        'can_payment_method_delete'
    );