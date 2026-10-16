-- Initial baseline before first deployment. Apply only to a new database.
-- Historical SQL is kept in dependency order to preserve the final schema and permissions.

-- create security schema
CREATE TABLE security_user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE security_roles (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE security_permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE security_user_roles (
    user_account_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_account_id, role_id),
    CONSTRAINT fk_security_user_roles_user
        FOREIGN KEY (user_account_id) REFERENCES security_user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_security_user_roles_role
        FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE
);

CREATE TABLE security_role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_security_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_security_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES security_permissions (id) ON DELETE CASCADE
);

-- seed admin tech account
INSERT INTO security_permissions (id, code, description)
VALUES ('5adcc0cb-a152-4e3a-8fcf-2636e2a2ddf0', 'ADMIN_ACCESS', 'Administrative access');

INSERT INTO security_roles (id, code, name, description, enabled)
VALUES ('af742010-e349-41bd-bdd1-621b43da2765', 'ADMIN', 'Administrator', 'Default administrator role', TRUE);

INSERT INTO security_role_permissions (role_id, permission_id)
VALUES ('af742010-e349-41bd-bdd1-621b43da2765', '5adcc0cb-a152-4e3a-8fcf-2636e2a2ddf0');

-- create refresh tokens
CREATE TABLE security_refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_account_id UUID NOT NULL,
    family_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_security_refresh_tokens_user
        FOREIGN KEY (user_account_id) REFERENCES security_user_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_security_refresh_tokens_family ON security_refresh_tokens (family_id);
CREATE INDEX idx_security_refresh_tokens_expiry ON security_refresh_tokens (expires_at);

-- create establishments and staff
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

-- normalize staff job titles and roles
CREATE TABLE staff_job_titles (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_staff_job_titles_establishment_name UNIQUE (establishment_id, name),
    CONSTRAINT fk_staff_job_titles_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

ALTER TABLE staff_members ADD COLUMN job_title_id UUID;

INSERT INTO staff_job_titles (id, establishment_id, name, enabled, created_at, updated_at)
SELECT gen_random_uuid(), establishment_id, job_title, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM staff_members
GROUP BY establishment_id, job_title;

UPDATE staff_members
SET job_title_id = (
    SELECT staff_job_titles.id
    FROM staff_job_titles
    WHERE staff_job_titles.establishment_id = staff_members.establishment_id
        AND staff_job_titles.name = staff_members.job_title
);

ALTER TABLE staff_members ADD CONSTRAINT fk_staff_members_job_title
    FOREIGN KEY (job_title_id) REFERENCES staff_job_titles (id);

CREATE TABLE staff_job_title_roles (
    job_title_id UUID NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (job_title_id, role_code),
    CONSTRAINT fk_staff_job_title_roles_title FOREIGN KEY (job_title_id)
        REFERENCES staff_job_titles (id) ON DELETE CASCADE
);

CREATE TABLE staff_member_roles (
    staff_member_id UUID NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (staff_member_id, role_code),
    CONSTRAINT fk_staff_member_roles_staff FOREIGN KEY (staff_member_id)
        REFERENCES staff_members (id) ON DELETE CASCADE
);

INSERT INTO staff_member_roles (staff_member_id, role_code)
SELECT id, 'STAFF' FROM staff_members;

INSERT INTO staff_member_roles (staff_member_id, role_code)
SELECT id, 'ESTABLISHMENT_ADMIN'
FROM staff_members
WHERE establishment_admin = TRUE;

ALTER TABLE staff_members ALTER COLUMN job_title DROP NOT NULL;

-- rename staff job titles to positions
ALTER TABLE staff_job_titles RENAME TO staff_positions;
ALTER TABLE staff_job_title_roles RENAME TO staff_position_roles;

ALTER TABLE staff_members RENAME COLUMN job_title_id TO position_id;
ALTER TABLE staff_position_roles RENAME COLUMN job_title_id TO position_id;
ALTER TABLE staff_members RENAME COLUMN job_title TO legacy_position_name;

ALTER TABLE staff_positions
    RENAME CONSTRAINT uq_staff_job_titles_establishment_name TO uq_staff_positions_establishment_name;
ALTER TABLE staff_positions
    RENAME CONSTRAINT fk_staff_job_titles_establishment TO fk_staff_positions_establishment;
ALTER TABLE staff_members
    RENAME CONSTRAINT fk_staff_members_job_title TO fk_staff_members_position;
ALTER TABLE staff_position_roles
    RENAME CONSTRAINT fk_staff_job_title_roles_title TO fk_staff_position_roles_position;

-- rename staff module to employees
ALTER TABLE staff_members RENAME TO employees;
ALTER TABLE staff_invitations RENAME TO employee_invitations;
ALTER TABLE staff_member_roles RENAME TO employee_roles;
ALTER TABLE staff_positions RENAME TO positions;
ALTER TABLE staff_position_roles RENAME TO position_roles;

ALTER TABLE employee_invitations RENAME COLUMN staff_member_id TO employee_id;
ALTER TABLE employee_roles RENAME COLUMN staff_member_id TO employee_id;

ALTER TABLE employees RENAME CONSTRAINT fk_staff_establishment TO fk_employees_establishment;
ALTER TABLE employees RENAME CONSTRAINT fk_staff_user_account TO fk_employees_user_account;
ALTER TABLE employees RENAME CONSTRAINT fk_staff_members_position TO fk_employees_position;
ALTER TABLE employee_invitations
    RENAME CONSTRAINT fk_staff_invitations_staff TO fk_employee_invitations_employee;
ALTER TABLE employee_roles
    RENAME CONSTRAINT fk_staff_member_roles_staff TO fk_employee_roles_employee;
ALTER TABLE positions
    RENAME CONSTRAINT fk_staff_positions_establishment TO fk_positions_establishment;
ALTER TABLE position_roles
    RENAME CONSTRAINT fk_staff_position_roles_position TO fk_position_roles_position;

ALTER INDEX idx_staff_establishment RENAME TO idx_employees_establishment;
ALTER INDEX idx_staff_invitations_expiry RENAME TO idx_employee_invitations_expiry;

-- seed position permissions
INSERT INTO security_permissions (id, code, description)
VALUES
    ('da89a117-3d20-45c1-9a01-000000000001', 'can_position_read', 'Read positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000002', 'can_position_create', 'Create positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000003', 'can_position_update', 'Update positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000004', 'can_position_assign_roles', 'Assign roles to positions'),
    ('da89a117-3d20-45c1-9a01-000000000005', 'can_position_delete', 'Deactivate positions in an establishment');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_position_read',
        'can_position_create',
        'can_position_update',
        'can_position_assign_roles',
        'can_position_delete'
    );

-- create students and permissions
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

-- create academic years and periods
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

-- create school catalog
ALTER TABLE academic_years
    ADD CONSTRAINT uq_academic_years_id_establishment UNIQUE (id, establishment_id);

CREATE TABLE grade_levels (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_grade_levels_establishment_code UNIQUE (establishment_id, code),
    CONSTRAINT uq_grade_levels_establishment_name UNIQUE (establishment_id, name),
    CONSTRAINT uq_grade_levels_id_establishment UNIQUE (id, establishment_id),
    CONSTRAINT fk_grade_levels_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_subjects_establishment_code UNIQUE (establishment_id, code),
    CONSTRAINT uq_subjects_id_establishment UNIQUE (id, establishment_id),
    CONSTRAINT fk_subjects_establishment FOREIGN KEY (establishment_id)
        REFERENCES establishments (id)
);

CREATE TABLE school_classes (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    grade_level_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_school_classes_year_code UNIQUE (academic_year_id, code),
    CONSTRAINT uq_school_classes_year_level_name UNIQUE (academic_year_id, grade_level_id, name),
    CONSTRAINT uq_school_classes_id_establishment UNIQUE (id, establishment_id),
    CONSTRAINT fk_school_classes_year_establishment FOREIGN KEY (academic_year_id, establishment_id)
        REFERENCES academic_years (id, establishment_id),
    CONSTRAINT fk_school_classes_grade_level_establishment FOREIGN KEY (grade_level_id, establishment_id)
        REFERENCES grade_levels (id, establishment_id)
);

CREATE TABLE school_class_subjects (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_school_class_subjects_class_subject UNIQUE (school_class_id, subject_id),
    CONSTRAINT fk_school_class_subjects_class_establishment FOREIGN KEY (school_class_id, establishment_id)
        REFERENCES school_classes (id, establishment_id),
    CONSTRAINT fk_school_class_subjects_subject_establishment FOREIGN KEY (subject_id, establishment_id)
        REFERENCES subjects (id, establishment_id)
);

CREATE INDEX idx_grade_levels_establishment_enabled_order
    ON grade_levels (establishment_id, enabled, display_order);
CREATE INDEX idx_subjects_establishment_enabled_name
    ON subjects (establishment_id, enabled, name);
CREATE INDEX idx_school_classes_year_enabled_name
    ON school_classes (academic_year_id, enabled, name);
CREATE INDEX idx_school_class_subjects_class_enabled
    ON school_class_subjects (school_class_id, enabled);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('d8c8a941-7ef8-4216-a021-000000000001', 'can_grade_level_read', 'Read grade levels'),
    ('d8c8a941-7ef8-4216-a021-000000000002', 'can_grade_level_create', 'Create grade levels'),
    ('d8c8a941-7ef8-4216-a021-000000000003', 'can_grade_level_update', 'Update grade levels'),
    ('d8c8a941-7ef8-4216-a021-000000000004', 'can_grade_level_delete', 'Archive grade levels'),
    ('d8c8a941-7ef8-4216-a021-000000000005', 'can_subject_read', 'Read subjects'),
    ('d8c8a941-7ef8-4216-a021-000000000006', 'can_subject_create', 'Create subjects'),
    ('d8c8a941-7ef8-4216-a021-000000000007', 'can_subject_update', 'Update subjects'),
    ('d8c8a941-7ef8-4216-a021-000000000008', 'can_subject_delete', 'Archive subjects'),
    ('d8c8a941-7ef8-4216-a021-000000000009', 'can_school_class_read', 'Read school classes'),
    ('d8c8a941-7ef8-4216-a021-000000000010', 'can_school_class_create', 'Create school classes'),
    ('d8c8a941-7ef8-4216-a021-000000000011', 'can_school_class_update', 'Update school classes'),
    ('d8c8a941-7ef8-4216-a021-000000000012', 'can_school_class_delete', 'Archive school classes'),
    ('d8c8a941-7ef8-4216-a021-000000000013', 'can_class_subject_assign', 'Assign subjects to classes'),
    ('d8c8a941-7ef8-4216-a021-000000000014', 'can_class_subject_unassign', 'Unassign subjects from classes'),
    ('d8c8a941-7ef8-4216-a021-000000000015', 'can_class_subject_read', 'Read class subject assignments');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_grade_level_read', 'can_grade_level_create', 'can_grade_level_update', 'can_grade_level_delete',
        'can_subject_read', 'can_subject_create', 'can_subject_update', 'can_subject_delete',
        'can_school_class_read', 'can_school_class_create', 'can_school_class_update', 'can_school_class_delete',
        'can_class_subject_assign', 'can_class_subject_unassign', 'can_class_subject_read'
    );

-- create enrollments
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

-- create guardians and parent access
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

-- create tuition fees and payments
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

-- create student number sequence
CREATE SEQUENCE student_number_sequence START WITH 1 INCREMENT BY 1;

-- add enrollment payment conditions
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

-- manage tuition types and remove enrollment payment gate
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

-- snapshot tuition labels and remove enrollment status
ALTER TABLE tuition_fee_definitions ADD COLUMN fee_type_code VARCHAR(40);
ALTER TABLE tuition_fee_definitions ADD COLUMN fee_type_name VARCHAR(100);

UPDATE tuition_fee_definitions fee
SET fee_type_code = (
        SELECT fee_type.code FROM tuition_fee_types fee_type WHERE fee_type.id = fee.fee_type_id
    ),
    fee_type_name = (
        SELECT fee_type.name FROM tuition_fee_types fee_type WHERE fee_type.id = fee.fee_type_id
    )
WHERE fee.fee_type_id IS NOT NULL;

ALTER TABLE tuition_payments ADD COLUMN payment_method_name VARCHAR(100);

UPDATE tuition_payments payment
SET payment_method_name = COALESCE(
    (SELECT method.name FROM school_payment_methods method
     WHERE method.establishment_id = payment.establishment_id
       AND method.code = payment.payment_method_code),
    payment.payment_method_code
);

ALTER TABLE tuition_payments ALTER COLUMN payment_method_name SET NOT NULL;

ALTER TABLE enrollments DROP CONSTRAINT ck_enrollments_status;
ALTER TABLE enrollments DROP COLUMN status;

-- restrict admin to establishment onboarding
INSERT INTO security_permissions (id, code, description)
VALUES ('a7e3b84d-0f93-4cee-b880-000000000001', 'can_establishment_onboard',
    'Create an establishment and invite its first administrator');

DELETE FROM security_role_permissions
WHERE role_id = (SELECT id FROM security_roles WHERE code = 'ADMIN');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ADMIN'
    AND security_permissions.code = 'can_establishment_onboard';

-- grant admin establishment listing
INSERT INTO security_permissions (id, code, description)
VALUES ('a7e3b84d-0f93-4cee-b880-000000000002', 'can_establishment_read',
    'Read the list of establishments');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ADMIN'
    AND security_permissions.code = 'can_establishment_read';

-- scope roles to establishments
ALTER TABLE security_roles ADD COLUMN establishment_id UUID;

ALTER TABLE security_roles
    ADD CONSTRAINT fk_security_roles_establishment
    FOREIGN KEY (establishment_id) REFERENCES establishments (id);

CREATE INDEX ix_security_roles_establishment_id ON security_roles (establishment_id);

-- grant establishment role management
INSERT INTO security_permissions (id, code, description)
VALUES
    ('a7e3b84d-0f93-4cee-b880-000000000003', 'can_role_read', 'Read roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000004', 'can_role_create', 'Create roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000005', 'can_role_update', 'Update roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000006', 'can_permission_read', 'Read assignable permissions');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code IN (
        'can_role_read', 'can_role_create', 'can_role_update', 'can_permission_read'
    );

-- remove grade levels and scope fees to classes
UPDATE tuition_fee_definitions
SET enabled = FALSE
WHERE grade_level_id IS NOT NULL;

ALTER TABLE tuition_fee_definitions
    ADD COLUMN school_class_id UUID;

ALTER TABLE tuition_fee_definitions DROP CONSTRAINT fk_tuition_fee_grade;
ALTER TABLE tuition_fee_definitions DROP CONSTRAINT ck_tuition_fee_target;
ALTER TABLE tuition_fee_definitions DROP COLUMN grade_level_id;
ALTER TABLE tuition_fee_definitions
    ADD CONSTRAINT ck_tuition_fee_target CHECK (school_class_id IS NULL OR student_id IS NULL);

ALTER TABLE school_classes DROP CONSTRAINT fk_school_classes_grade_level_establishment;
ALTER TABLE school_classes DROP CONSTRAINT uq_school_classes_year_level_name;
ALTER TABLE school_classes DROP COLUMN grade_level_id;
ALTER TABLE school_classes
    ADD CONSTRAINT uq_school_classes_id_establishment_year UNIQUE (id, establishment_id, academic_year_id);
ALTER TABLE tuition_fee_definitions
    ADD CONSTRAINT fk_tuition_fee_class_scope
    FOREIGN KEY (school_class_id, establishment_id, academic_year_id)
    REFERENCES school_classes (id, establishment_id, academic_year_id);

DROP TABLE grade_levels;

DELETE FROM security_role_permissions
WHERE permission_id IN (
    SELECT id FROM security_permissions WHERE code LIKE 'can_grade_level_%'
);

DELETE FROM security_permissions
WHERE code LIKE 'can_grade_level_%';

-- grant establishment update
INSERT INTO security_permissions (id, code, description)
VALUES ('b410f227-bc64-4a94-84a7-000000000001', 'can_establishment_update', 'Update establishment code and name');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code = 'can_establishment_update';

-- create daily attendance
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

-- normalize establishment codes
UPDATE establishments
SET code = UPPER(TRIM(code));

-- add parent visible attendance comment
ALTER TABLE attendance_records
    ADD COLUMN parent_comment VARCHAR(500);

-- create platform licenses
CREATE TABLE license_offers (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_license_offers_id_enabled UNIQUE (id, enabled)
);

CREATE TABLE license_offer_features (
    offer_id UUID NOT NULL,
    feature_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (offer_id, feature_code),
    CONSTRAINT fk_license_offer_features_offer FOREIGN KEY (offer_id) REFERENCES license_offers (id)
);

CREATE TABLE platform_licenses (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    establishment_id UUID NOT NULL,
    offer_id UUID NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    suspended_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_license_validity CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT fk_platform_licenses_establishment FOREIGN KEY (establishment_id) REFERENCES establishments (id),
    CONSTRAINT fk_platform_licenses_offer FOREIGN KEY (offer_id) REFERENCES license_offers (id)
);

CREATE INDEX idx_platform_licenses_establishment_period
    ON platform_licenses (establishment_id, valid_from, valid_until);

CREATE TABLE license_events (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    license_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_license_events_license FOREIGN KEY (license_id) REFERENCES platform_licenses (id)
);

CREATE INDEX idx_license_events_license_created ON license_events (license_id, created_at);

INSERT INTO license_offers (id, code, name, enabled, created_at)
VALUES ('c51d6db7-309f-4e85-b884-000000000001', 'LEGACY_FULL_ACCESS', 'Accès historique', TRUE, CURRENT_TIMESTAMP);

INSERT INTO license_offer_features (offer_id, feature_code)
VALUES
    ('c51d6db7-309f-4e85-b884-000000000001', 'SCHOOL_MANAGEMENT'),
    ('c51d6db7-309f-4e85-b884-000000000001', 'STUDENT_RECORDS'),
    ('c51d6db7-309f-4e85-b884-000000000001', 'PARENT_PORTAL'),
    ('c51d6db7-309f-4e85-b884-000000000001', 'TUITION'),
    ('c51d6db7-309f-4e85-b884-000000000001', 'ATTENDANCE'),
    ('c51d6db7-309f-4e85-b884-000000000001', 'LEARNING_CONTENT');

INSERT INTO platform_licenses (establishment_id, offer_id, valid_from, valid_until,
    suspended_at, created_by, created_at, updated_at)
SELECT id, 'c51d6db7-309f-4e85-b884-000000000001', created_at, NULL,
    NULL, 'SYSTEM_MIGRATION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM establishments;

INSERT INTO license_events (license_id, action, actor, details, created_at)
SELECT id, 'LEGACY_GRANT', 'SYSTEM_MIGRATION', 'Migration V30: accès existant préservé sans prix ni échéance', CURRENT_TIMESTAMP
FROM platform_licenses;

INSERT INTO security_permissions (id, code, description)
VALUES ('d0b03cb0-0d31-45a2-a8ce-000000000001', 'can_license_read', 'Read the establishment platform license');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code = 'can_license_read';

-- create learning content
CREATE TABLE learning_contents (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    subject_id UUID,
    type VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    body TEXT,
    due_date DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    published_by VARCHAR(100),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_learning_content_type CHECK (type IN ('LESSON', 'ASSIGNMENT')),
    CONSTRAINT ck_learning_content_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_learning_lesson_due_date CHECK (type <> 'LESSON' OR due_date IS NULL),
    CONSTRAINT fk_learning_content_year FOREIGN KEY (academic_year_id, establishment_id)
        REFERENCES academic_years (id, establishment_id),
    CONSTRAINT fk_learning_content_class FOREIGN KEY (school_class_id, establishment_id, academic_year_id)
        REFERENCES school_classes (id, establishment_id, academic_year_id),
    CONSTRAINT fk_learning_content_subject FOREIGN KEY (subject_id, establishment_id)
        REFERENCES subjects (id, establishment_id)
);

CREATE INDEX idx_learning_contents_class_status_created
    ON learning_contents (establishment_id, academic_year_id, school_class_id, status, created_at);

CREATE TABLE learning_attachments (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    storage_key VARCHAR(80) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(120) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_learning_attachment_size CHECK (size > 0),
    CONSTRAINT fk_learning_attachment_content FOREIGN KEY (content_id)
        REFERENCES learning_contents (id)
);

CREATE INDEX idx_learning_attachments_content_created
    ON learning_attachments (content_id, created_at);

INSERT INTO security_permissions (id, code, description)
VALUES
    ('f37b31e3-2450-4785-9a39-000000000001', 'can_learning_content_read', 'Read class lessons and assignments'),
    ('f37b31e3-2450-4785-9a39-000000000002', 'can_learning_content_create', 'Create class lessons and assignments'),
    ('f37b31e3-2450-4785-9a39-000000000003', 'can_learning_content_update', 'Update draft class lessons and assignments'),
    ('f37b31e3-2450-4785-9a39-000000000004', 'can_learning_content_publish', 'Publish class lessons and assignments'),
    ('f37b31e3-2450-4785-9a39-000000000005', 'can_learning_content_archive', 'Archive class lessons and assignments');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code IN (
        'can_learning_content_read', 'can_learning_content_create', 'can_learning_content_update',
        'can_learning_content_publish', 'can_learning_content_archive'
    );
