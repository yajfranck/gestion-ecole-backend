CREATE TABLE establishments (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE staff_members (
    id UUID PRIMARY KEY,
    employee_number VARCHAR(40) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    job_title VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    establishment_admin BOOLEAN NOT NULL DEFAULT FALSE,
    establishment_id UUID NOT NULL,
    user_account_id UUID UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_staff_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id),
    CONSTRAINT fk_staff_user_account FOREIGN KEY (user_account_id)
        REFERENCES security_user_accounts (id) ON DELETE SET NULL
);

CREATE INDEX idx_staff_establishment ON staff_members (establishment_id);

CREATE TABLE staff_invitations (
    id UUID PRIMARY KEY,
    staff_member_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_staff_invitations_staff FOREIGN KEY (staff_member_id)
        REFERENCES staff_members (id) ON DELETE CASCADE
);

CREATE INDEX idx_staff_invitations_expiry ON staff_invitations (expires_at);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('4cf7e9c3-14a2-416c-a1bf-a54d40a26be4', 'STAFF_ACCESS', 'Access to staff features'),
    ('761db3f2-ae50-43fb-8c9d-e482e4e19d83', 'ESTABLISHMENT_STAFF_MANAGEMENT', 'Manage staff within an establishment');

INSERT INTO security_roles (id, code, name, description, enabled)
VALUES
    ('e156bc11-78a8-41f9-8115-30bef66a4f32', 'STAFF', 'Staff member', 'School staff member', TRUE),
    ('9f18ef85-1f24-421f-8bab-6d93a99165cc', 'ESTABLISHMENT_ADMIN', 'Establishment administrator', 'Manage staff within one establishment', TRUE);

INSERT INTO security_role_permissions (role_id, permission_id)
VALUES
    ('e156bc11-78a8-41f9-8115-30bef66a4f32', '4cf7e9c3-14a2-416c-a1bf-a54d40a26be4'),
    ('9f18ef85-1f24-421f-8bab-6d93a99165cc', '4cf7e9c3-14a2-416c-a1bf-a54d40a26be4'),
    ('9f18ef85-1f24-421f-8bab-6d93a99165cc', '761db3f2-ae50-43fb-8c9d-e482e4e19d83');