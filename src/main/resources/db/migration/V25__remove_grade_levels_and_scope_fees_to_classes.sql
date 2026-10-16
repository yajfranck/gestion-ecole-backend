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