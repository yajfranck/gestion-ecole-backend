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