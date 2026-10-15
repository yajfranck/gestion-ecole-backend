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