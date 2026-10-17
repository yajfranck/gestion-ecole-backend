CREATE TABLE school_year_rollover_classes (
    establishment_id UUID NOT NULL,
    source_academic_year_id UUID NOT NULL,
    target_academic_year_id UUID NOT NULL,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (target_academic_year_id, source_id),
    CONSTRAINT uq_rollover_class_target UNIQUE (target_academic_year_id, target_id),
    CONSTRAINT ck_rollover_class_years CHECK (source_academic_year_id <> target_academic_year_id),
    CONSTRAINT fk_rollover_class_source FOREIGN KEY (source_id, source_academic_year_id, establishment_id)
        REFERENCES school_classes (id, academic_year_id, establishment_id),
    CONSTRAINT fk_rollover_class_target FOREIGN KEY (target_id, target_academic_year_id, establishment_id)
        REFERENCES school_classes (id, academic_year_id, establishment_id)
);

CREATE TABLE school_year_rollover_classrooms (
    establishment_id UUID NOT NULL,
    source_academic_year_id UUID NOT NULL,
    target_academic_year_id UUID NOT NULL,
    source_class_id UUID NOT NULL,
    target_class_id UUID NOT NULL,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (target_academic_year_id, source_id),
    CONSTRAINT uq_rollover_classroom_target UNIQUE (target_academic_year_id, target_id),
    CONSTRAINT ck_rollover_classroom_years CHECK (source_academic_year_id <> target_academic_year_id),
    CONSTRAINT fk_rollover_classroom_source FOREIGN KEY (source_id, source_class_id, source_academic_year_id, establishment_id)
        REFERENCES classrooms (id, school_class_id, academic_year_id, establishment_id),
    CONSTRAINT fk_rollover_classroom_target FOREIGN KEY (target_id, target_class_id, target_academic_year_id, establishment_id)
        REFERENCES classrooms (id, school_class_id, academic_year_id, establishment_id)
);

CREATE TABLE school_year_rollover_fees (
    establishment_id UUID NOT NULL,
    source_academic_year_id UUID NOT NULL,
    target_academic_year_id UUID NOT NULL,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (target_academic_year_id, source_id),
    CONSTRAINT uq_rollover_fee_target UNIQUE (target_academic_year_id, target_id),
    CONSTRAINT ck_rollover_fee_years CHECK (source_academic_year_id <> target_academic_year_id),
    CONSTRAINT fk_rollover_fee_source FOREIGN KEY (source_id, establishment_id, source_academic_year_id)
        REFERENCES tuition_fee_definitions (id, establishment_id, academic_year_id),
    CONSTRAINT fk_rollover_fee_target FOREIGN KEY (target_id, establishment_id, target_academic_year_id)
        REFERENCES tuition_fee_definitions (id, establishment_id, academic_year_id)
);

INSERT INTO security_permissions (id, code, description) VALUES
    ('c18e5b15-234a-4ae2-b87a-000000000020', 'can_school_year_rollover', 'Copy previous school year classrooms and academic/financial configuration');
INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p
WHERE r.code='ESTABLISHMENT_ADMIN' AND p.code='can_school_year_rollover';
