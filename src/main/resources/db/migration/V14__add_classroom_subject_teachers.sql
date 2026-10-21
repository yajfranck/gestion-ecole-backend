CREATE TABLE classroom_subject_teachers (
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    CONSTRAINT pk_classroom_subject_teachers PRIMARY KEY (classroom_id, subject_id),
    CONSTRAINT fk_classroom_subject_teachers_classroom FOREIGN KEY
        (classroom_id, school_class_id, academic_year_id, establishment_id)
        REFERENCES classrooms (id, school_class_id, academic_year_id, establishment_id),
    CONSTRAINT fk_classroom_subject_teachers_subject FOREIGN KEY (school_class_id, subject_id)
        REFERENCES school_class_subjects (school_class_id, subject_id),
    CONSTRAINT fk_classroom_subject_teachers_employee FOREIGN KEY (employee_id, establishment_id)
        REFERENCES employees (id, establishment_id)
);

CREATE INDEX idx_classroom_subject_teachers_class ON classroom_subject_teachers (establishment_id, school_class_id);
CREATE INDEX idx_classroom_subject_teachers_employee ON classroom_subject_teachers (employee_id);
