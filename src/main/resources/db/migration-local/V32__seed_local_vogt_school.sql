INSERT INTO establishments (id, code, name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000001', 'VOGT', 'COLLEGE VOGT', TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO security_user_accounts (id, username, password_hash, enabled, locked, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000002', 'franckyaj@gmail.com',
    '$2a$10$jpLBy6mDEjotqZy7UKzJ3eQXmwo8hksM6f/8uKczSYsufuJujmFue',
    TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO security_user_roles (user_account_id, role_id)
SELECT 'f0320000-0000-4000-8000-000000000002', id
FROM security_roles WHERE code IN ('STAFF', 'ESTABLISHMENT_ADMIN');

INSERT INTO positions (id, establishment_id, name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000003', 'f0320000-0000-4000-8000-000000000001',
    'School administrator', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO position_roles (position_id, role_code)
VALUES ('f0320000-0000-4000-8000-000000000003', 'ESTABLISHMENT_ADMIN');

INSERT INTO employees (id, employee_number, first_name, last_name, email, enabled,
    establishment_admin, establishment_id, position_id, user_account_id, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000004', 'VOGT-ADM-001', 'FRANCK', 'YAJE',
    'franckyaj@gmail.com', TRUE, TRUE, 'f0320000-0000-4000-8000-000000000001',
    'f0320000-0000-4000-8000-000000000003', 'f0320000-0000-4000-8000-000000000002',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO employee_roles (employee_id, role_code)
VALUES ('f0320000-0000-4000-8000-000000000004', 'STAFF');

INSERT INTO platform_licenses (establishment_id, offer_id, valid_from, valid_until,
    suspended_at, created_by, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000001', 'c51d6db7-309f-4e85-b884-000000000001',
    CURRENT_TIMESTAMP + INTERVAL '2' MINUTE, CURRENT_TIMESTAMP + INTERVAL '182' MINUTE,
    NULL, 'LOCAL_SEED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO license_events (license_id, action, actor, details, created_at)
SELECT id, 'ASSIGNED', 'LOCAL_SEED', 'Local VOGT fixture: starts after two minutes, lasts three hours',
    CURRENT_TIMESTAMP
FROM platform_licenses WHERE establishment_id = 'f0320000-0000-4000-8000-000000000001';

INSERT INTO tuition_fee_types (id, establishment_id, code, name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000005', 'f0320000-0000-4000-8000-000000000001',
    'SCOLARITE', 'frais de scolarite', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO academic_years (id, establishment_id, name, starts_on, ends_on, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000006', 'f0320000-0000-4000-8000-000000000001',
    '2026-2027', DATE '2026-09-01', DATE '2027-08-31', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO school_classes (id, establishment_id, academic_year_id, code, name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000007', 'f0320000-0000-4000-8000-000000000001',
    'f0320000-0000-4000-8000-000000000006', 'SIXIEME', 'sixieme', TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO subjects (id, establishment_id, code, name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000008', 'f0320000-0000-4000-8000-000000000001',
    'MATH', 'Mathematique', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO school_class_subjects (id, establishment_id, school_class_id, subject_id, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000009', 'f0320000-0000-4000-8000-000000000001',
    'f0320000-0000-4000-8000-000000000007', 'f0320000-0000-4000-8000-000000000008',
    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO students (id, establishment_id, student_number, first_name, last_name, enabled, created_at, updated_at)
VALUES ('f0320000-0000-4000-8000-000000000010', 'f0320000-0000-4000-8000-000000000001',
    CONCAT('STU-', LPAD(CAST(nextval('student_number_sequence') AS VARCHAR), 8, '0')),
    'Miya', 'WANDJA', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO enrollments (id, establishment_id, student_id, academic_year_id, school_class_id, created_at)
VALUES ('f0320000-0000-4000-8000-000000000011', 'f0320000-0000-4000-8000-000000000001',
    'f0320000-0000-4000-8000-000000000010', 'f0320000-0000-4000-8000-000000000006',
    'f0320000-0000-4000-8000-000000000007', CURRENT_TIMESTAMP);