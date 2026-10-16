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