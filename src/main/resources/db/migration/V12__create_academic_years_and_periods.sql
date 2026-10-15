CREATE TABLE academic_years (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_academic_years_establishment_name UNIQUE (establishment_id, name),
    CONSTRAINT ck_academic_years_date_range CHECK (ends_on >= starts_on),
    CONSTRAINT fk_academic_years_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

CREATE INDEX idx_academic_years_establishment_enabled
    ON academic_years (establishment_id, enabled, starts_on);

CREATE TABLE academic_periods (
    id UUID PRIMARY KEY,
    academic_year_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_academic_periods_year_name UNIQUE (academic_year_id, name),
    CONSTRAINT ck_academic_periods_date_range CHECK (ends_on >= starts_on),
    CONSTRAINT fk_academic_periods_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_years (id)
);

CREATE INDEX idx_academic_periods_year_enabled
    ON academic_periods (academic_year_id, enabled, starts_on);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('cf9b11d4-c94e-45c4-9c21-000000000001', 'can_academic_year_read', 'Read academic years'),
    ('cf9b11d4-c94e-45c4-9c21-000000000002', 'can_academic_year_create', 'Create academic years'),
    ('cf9b11d4-c94e-45c4-9c21-000000000003', 'can_academic_year_update', 'Update academic years'),
    ('cf9b11d4-c94e-45c4-9c21-000000000004', 'can_academic_year_delete', 'Archive academic years'),
    ('cf9b11d4-c94e-45c4-9c21-000000000005', 'can_academic_period_read', 'Read academic periods'),
    ('cf9b11d4-c94e-45c4-9c21-000000000006', 'can_academic_period_create', 'Create academic periods'),
    ('cf9b11d4-c94e-45c4-9c21-000000000007', 'can_academic_period_update', 'Update academic periods'),
    ('cf9b11d4-c94e-45c4-9c21-000000000008', 'can_academic_period_delete', 'Archive academic periods');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_academic_year_read',
        'can_academic_year_create',
        'can_academic_year_update',
        'can_academic_year_delete',
        'can_academic_period_read',
        'can_academic_period_create',
        'can_academic_period_update',
        'can_academic_period_delete'
    );