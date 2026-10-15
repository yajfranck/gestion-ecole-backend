CREATE TABLE guardians (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    user_account_id UUID UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_guardians_user_account FOREIGN KEY (user_account_id)
        REFERENCES security_user_accounts (id) ON DELETE SET NULL
);

CREATE TABLE guardian_student_links (
    id UUID PRIMARY KEY,
    guardian_id UUID NOT NULL,
    student_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_guardian_student UNIQUE (guardian_id, student_id),
    CONSTRAINT fk_guardian_student_guardian FOREIGN KEY (guardian_id) REFERENCES guardians (id),
    CONSTRAINT fk_guardian_student_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE INDEX idx_guardian_student_student ON guardian_student_links (student_id);

CREATE TABLE guardian_invitations (
    id UUID PRIMARY KEY,
    guardian_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_guardian_invitations_guardian FOREIGN KEY (guardian_id)
        REFERENCES guardians (id) ON DELETE CASCADE
);

CREATE INDEX idx_guardian_invitations_expiry ON guardian_invitations (expires_at);

INSERT INTO security_roles (id, code, name, description, enabled)
VALUES ('9aa8ac38-5c31-4429-a312-2c7bb2b48703', 'PARENT', 'Parent', 'Parent or legal guardian', TRUE);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('ae9f1084-a63b-4221-998b-000000000001', 'can_guardian_read', 'Read guardians linked to students'),
    ('ae9f1084-a63b-4221-998b-000000000002', 'can_guardian_link', 'Link a guardian to a student'),
    ('ae9f1084-a63b-4221-998b-000000000003', 'can_guardian_unlink', 'Unlink a guardian from a student'),
    ('ae9f1084-a63b-4221-998b-000000000004', 'can_guardian_read_children', 'Read children linked to the authenticated guardian');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN ('can_guardian_read', 'can_guardian_link', 'can_guardian_unlink');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'PARENT'
    AND security_permissions.code = 'can_guardian_read_children';