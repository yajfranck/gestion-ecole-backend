CREATE TABLE tuition_fee_classes (
    fee_id UUID NOT NULL,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    CONSTRAINT pk_tuition_fee_classes PRIMARY KEY (fee_id, school_class_id),
    CONSTRAINT fk_tuition_fee_classes_fee FOREIGN KEY (fee_id, establishment_id, academic_year_id)
        REFERENCES tuition_fee_definitions (id, establishment_id, academic_year_id),
    CONSTRAINT fk_tuition_fee_classes_class FOREIGN KEY (school_class_id, establishment_id, academic_year_id)
        REFERENCES school_classes (id, establishment_id, academic_year_id)
);

CREATE INDEX idx_tuition_fee_classes_class ON tuition_fee_classes (school_class_id, establishment_id, academic_year_id);

INSERT INTO tuition_fee_classes (fee_id, establishment_id, academic_year_id, school_class_id)
SELECT id, establishment_id, academic_year_id, school_class_id
FROM tuition_fee_definitions
WHERE school_class_id IS NOT NULL;
