ALTER TABLE staff_members RENAME TO employees;
ALTER TABLE staff_invitations RENAME TO employee_invitations;
ALTER TABLE staff_member_roles RENAME TO employee_roles;
ALTER TABLE staff_positions RENAME TO positions;
ALTER TABLE staff_position_roles RENAME TO position_roles;

ALTER TABLE employee_invitations RENAME COLUMN staff_member_id TO employee_id;
ALTER TABLE employee_roles RENAME COLUMN staff_member_id TO employee_id;

ALTER TABLE employees RENAME CONSTRAINT fk_staff_establishment TO fk_employees_establishment;
ALTER TABLE employees RENAME CONSTRAINT fk_staff_user_account TO fk_employees_user_account;
ALTER TABLE employees RENAME CONSTRAINT fk_staff_members_position TO fk_employees_position;
ALTER TABLE employee_invitations
    RENAME CONSTRAINT fk_staff_invitations_staff TO fk_employee_invitations_employee;
ALTER TABLE employee_roles
    RENAME CONSTRAINT fk_staff_member_roles_staff TO fk_employee_roles_employee;
ALTER TABLE positions
    RENAME CONSTRAINT fk_staff_positions_establishment TO fk_positions_establishment;
ALTER TABLE position_roles
    RENAME CONSTRAINT fk_staff_position_roles_position TO fk_position_roles_position;

ALTER INDEX idx_staff_establishment RENAME TO idx_employees_establishment;
ALTER INDEX idx_staff_invitations_expiry RENAME TO idx_employee_invitations_expiry;