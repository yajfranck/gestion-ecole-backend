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