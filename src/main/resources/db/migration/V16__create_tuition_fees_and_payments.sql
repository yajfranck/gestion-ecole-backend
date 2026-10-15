CREATE SEQUENCE tuition_receipt_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE tuition_fee_definitions (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    due_date DATE NOT NULL,
    grade_level_id UUID,
    student_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_tuition_fee_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_tuition_fee_target CHECK (grade_level_id IS NULL OR student_id IS NULL),
    CONSTRAINT uq_tuition_fee_year_name UNIQUE (establishment_id, academic_year_id, name),
    CONSTRAINT uq_tuition_fee_id_scope UNIQUE (id, establishment_id, academic_year_id),
    CONSTRAINT fk_tuition_fee_year FOREIGN KEY (academic_year_id, establishment_id)
        REFERENCES academic_years (id, establishment_id),
    CONSTRAINT fk_tuition_fee_grade FOREIGN KEY (grade_level_id, establishment_id)
        REFERENCES grade_levels (id, establishment_id),
    CONSTRAINT fk_tuition_fee_student FOREIGN KEY (student_id, establishment_id)
        REFERENCES students (id, establishment_id)
);

CREATE TABLE student_fee_charges (
    id UUID PRIMARY KEY,
    fee_id UUID NOT NULL,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    student_id UUID NOT NULL,
    student_number VARCHAR(40) NOT NULL,
    student_first_name VARCHAR(100) NOT NULL,
    student_last_name VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_student_fee_charge_amount_positive CHECK (amount > 0),
    CONSTRAINT uq_student_fee_charge_fee_student UNIQUE (fee_id, student_id),
    CONSTRAINT uq_student_fee_charge_scope UNIQUE (id, establishment_id, student_id),
    CONSTRAINT fk_student_fee_charge_fee FOREIGN KEY (fee_id, establishment_id, academic_year_id)
        REFERENCES tuition_fee_definitions (id, establishment_id, academic_year_id),
    CONSTRAINT fk_student_fee_charge_student FOREIGN KEY (student_id, establishment_id)
        REFERENCES students (id, establishment_id)
);

CREATE INDEX idx_student_fee_charges_year_due
    ON student_fee_charges (establishment_id, academic_year_id, due_date);
CREATE INDEX idx_student_fee_charges_student_due
    ON student_fee_charges (establishment_id, student_id, due_date);

CREATE TABLE tuition_payments (
    id UUID PRIMARY KEY,
    charge_id UUID NOT NULL,
    establishment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    receipt_number VARCHAR(32) NOT NULL UNIQUE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method_code VARCHAR(40) NOT NULL,
    reference VARCHAR(120),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    CONSTRAINT ck_tuition_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_tuition_payment_charge FOREIGN KEY (charge_id, establishment_id, student_id)
        REFERENCES student_fee_charges (id, establishment_id, student_id)
);

CREATE INDEX idx_tuition_payments_charge_received ON tuition_payments (charge_id, received_at);

CREATE TABLE school_payment_methods (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_school_payment_method UNIQUE (establishment_id, code),
    CONSTRAINT fk_school_payment_method_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('fa7d29cd-1134-4e47-9e5c-000000000001', 'can_tuition_read', 'Read tuition fees and balances'),
    ('fa7d29cd-1134-4e47-9e5c-000000000002', 'can_tuition_create', 'Define tuition fees'),
    ('fa7d29cd-1134-4e47-9e5c-000000000003', 'can_tuition_apply', 'Apply tuition fees to enrolled students'),
    ('fa7d29cd-1134-4e47-9e5c-000000000004', 'can_tuition_outstanding_read', 'Read outstanding tuition balances'),
    ('fa7d29cd-1134-4e47-9e5c-000000000005', 'can_payment_record', 'Record tuition payments'),
    ('fa7d29cd-1134-4e47-9e5c-000000000006', 'can_payment_receipt_read', 'Read tuition payment receipts'),
    ('fa7d29cd-1134-4e47-9e5c-000000000007', 'can_payment_method_update', 'Configure accepted tuition payment methods');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_tuition_read', 'can_tuition_create', 'can_tuition_apply',
        'can_tuition_outstanding_read', 'can_payment_record',
        'can_payment_receipt_read', 'can_payment_method_update'
    );