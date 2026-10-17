ALTER TABLE employees ADD CONSTRAINT uq_employees_id_establishment UNIQUE (id, establishment_id);
ALTER TABLE school_class_subjects ADD CONSTRAINT uq_class_subject_scope
    UNIQUE (school_class_id, subject_id, establishment_id);

CREATE TABLE timetable_entries (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    teacher_id UUID,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    room_code VARCHAR(40),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    copied_from_id UUID,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_timetable_entry_scope UNIQUE (id, establishment_id, academic_year_id),
    CONSTRAINT ck_timetable_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT ck_timetable_times CHECK (end_time > start_time),
    CONSTRAINT ck_timetable_dates CHECK (valid_until >= valid_from),
    CONSTRAINT fk_timetable_classroom FOREIGN KEY (classroom_id, school_class_id, academic_year_id, establishment_id)
        REFERENCES classrooms (id, school_class_id, academic_year_id, establishment_id),
    CONSTRAINT fk_timetable_subject FOREIGN KEY (school_class_id, subject_id, establishment_id)
        REFERENCES school_class_subjects (school_class_id, subject_id, establishment_id),
    CONSTRAINT fk_timetable_teacher FOREIGN KEY (teacher_id, establishment_id)
        REFERENCES employees (id, establishment_id),
    CONSTRAINT fk_timetable_copy_source FOREIGN KEY (copied_from_id, establishment_id, academic_year_id)
        REFERENCES timetable_entries (id, establishment_id, academic_year_id)
);
CREATE INDEX idx_timetable_classroom ON timetable_entries (classroom_id, enabled, day_of_week, start_time);
CREATE INDEX idx_timetable_teacher ON timetable_entries (establishment_id, teacher_id, enabled);

CREATE TABLE timetable_non_working_days (
    establishment_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    day_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (academic_year_id, day_date),
    CONSTRAINT fk_timetable_non_working_year FOREIGN KEY (academic_year_id, establishment_id)
        REFERENCES academic_years (id, establishment_id)
);

INSERT INTO security_permissions (id, code, description) VALUES
    ('c18e5b15-234a-4ae2-b87a-000000000010', 'can_timetable_read', 'Read classroom timetables'),
    ('c18e5b15-234a-4ae2-b87a-000000000011', 'can_timetable_create', 'Create classroom timetable entries'),
    ('c18e5b15-234a-4ae2-b87a-000000000012', 'can_timetable_update', 'Update classroom timetables and non-working days'),
    ('c18e5b15-234a-4ae2-b87a-000000000013', 'can_timetable_delete', 'Archive classroom timetable entries'),
    ('c18e5b15-234a-4ae2-b87a-000000000014', 'can_timetable_copy', 'Copy a timetable to another classroom');
INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p
WHERE r.code = 'ESTABLISHMENT_ADMIN' AND p.code IN
    ('can_timetable_read','can_timetable_create','can_timetable_update','can_timetable_delete','can_timetable_copy');
