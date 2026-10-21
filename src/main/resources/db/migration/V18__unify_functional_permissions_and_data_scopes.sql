-- Functional permissions and data scope are independent. Flyway executes this conversion once.
CREATE TABLE IF NOT EXISTS security_role_scopes (
    role_id UUID NOT NULL REFERENCES security_roles(id) ON DELETE CASCADE,
    scope_code VARCHAR(40) NOT NULL CHECK (scope_code IN ('PLATFORM')),
    PRIMARY KEY (role_id, scope_code)
);

INSERT INTO security_permissions (id, code, description) SELECT '1d206f62-6414-5f28-a9a4-9942ae4742cc', 'can_employee_assign_administrator', 'Assign establishment administrator roles' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_assign_administrator');
INSERT INTO security_permissions (id, code, description) SELECT '27de0e2f-9bbc-5412-bc10-428615b7bf61', 'can_employee_create', 'Invite employees' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_create');
INSERT INTO security_permissions (id, code, description) SELECT '697f8b2c-3b7c-5d09-87f5-13ad885aa0af', 'can_employee_read', 'Read employees' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_read');
INSERT INTO security_permissions (id, code, description) SELECT '57cba942-d68b-53b3-8ab3-e4e82295b01e', 'can_employee_update', 'Update employees' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_update');
INSERT INTO security_permissions (id, code, description) SELECT '2867e1e6-e6c1-511b-beb8-18a001ee24e2', 'can_employee_delete', 'Deactivate employees' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_delete');
INSERT INTO security_permissions (id, code, description) SELECT 'c1028e19-d346-5bac-aa1b-45f68fd14bdf', 'can_employee_assign_roles', 'Assign employee roles' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_assign_roles');
INSERT INTO security_permissions (id, code, description) SELECT '1a6fd1f0-26c8-5f7b-82ab-a63f45b3305d', 'can_employee_read_self', 'Read own employee profile' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_employee_read_self');
INSERT INTO security_permissions (id, code, description) SELECT '342ee0b7-65c1-5696-99b2-aaf4a209558c', 'can_user_read', 'Read platform users' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_user_read');
INSERT INTO security_permissions (id, code, description) SELECT '9281dccd-0cd5-54c3-9e62-b2543e31cfba', 'can_user_change_password', 'Change own password' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_user_change_password');
INSERT INTO security_permissions (id, code, description) SELECT 'f7b317b6-e771-5f4a-8fc3-26347ebceb11', 'can_platform_role_read', 'Read platform roles' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_platform_role_read');
INSERT INTO security_permissions (id, code, description) SELECT 'f45772f6-9165-5bb5-9e3e-60c5eba697f1', 'can_platform_role_create', 'Create platform roles' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_platform_role_create');
INSERT INTO security_permissions (id, code, description) SELECT '7bc17da8-852c-5d82-b473-dc3024ee8f23', 'can_platform_role_update', 'Update platform roles' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_platform_role_update');
INSERT INTO security_permissions (id, code, description) SELECT '87540913-9a4c-5ebe-9996-b3efdf3cc4b3', 'can_establishment_delete', 'Deactivate establishments' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_establishment_delete');
INSERT INTO security_permissions (id, code, description) SELECT '2ce054f3-882f-50fb-845d-b645b304619c', 'can_license_offer_read', 'Read license offers' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_offer_read');
INSERT INTO security_permissions (id, code, description) SELECT '5f30bc79-ee22-5758-a916-1e09099d0529', 'can_license_offer_create', 'Create license offers' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_offer_create');
INSERT INTO security_permissions (id, code, description) SELECT 'db7132b5-c5b4-5941-86b6-7dee15d02cb2', 'can_license_assign', 'Assign licenses' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_assign');
INSERT INTO security_permissions (id, code, description) SELECT '504ac778-6590-5b90-95b4-606e4bdf37ba', 'can_license_suspend', 'Suspend licenses' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_suspend');
INSERT INTO security_permissions (id, code, description) SELECT '6956306f-4f04-514e-a22a-630af6d0a41a', 'can_license_reactivate', 'Reactivate licenses' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_reactivate');
INSERT INTO security_permissions (id, code, description) SELECT 'ca22b150-cd5b-5943-b959-76f122797fb2', 'can_license_end', 'End licenses' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_end');
INSERT INTO security_permissions (id, code, description) SELECT 'f6f37f6e-bfee-5200-a780-1a8414273f60', 'can_license_event_read', 'Read license audit events' WHERE NOT EXISTS (SELECT 1 FROM security_permissions WHERE code = 'can_license_event_read');

INSERT INTO security_role_scopes (role_id, scope_code)
SELECT r.id, 'PLATFORM' FROM security_roles r WHERE r.establishment_id IS NULL
AND (r.code = 'ADMIN' OR EXISTS (SELECT 1 FROM security_role_permissions rp JOIN security_permissions p ON p.id = rp.permission_id WHERE rp.role_id = r.id AND p.code = 'ADMIN_ACCESS'))
AND NOT EXISTS (SELECT 1 FROM security_role_scopes s WHERE s.role_id = r.id AND s.scope_code = 'PLATFORM');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_employee_assign_administrator')
AND (EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'ADMIN_ACCESS') OR r.code = 'ADMIN')
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_employee_create', 'can_employee_read', 'can_employee_update', 'can_employee_delete', 'can_employee_assign_roles', 'can_employee_read_self')
AND (EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'ESTABLISHMENT_STAFF_MANAGEMENT') OR EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'ADMIN_ACCESS'))
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_employee_read_self')
AND (EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'STAFF_ACCESS'))
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_user_read', 'can_platform_role_read', 'can_platform_role_create', 'can_platform_role_update', 'can_establishment_delete', 'can_license_offer_read', 'can_license_offer_create', 'can_license_assign', 'can_license_suspend', 'can_license_reactivate', 'can_license_end', 'can_license_event_read', 'can_role_read', 'can_role_create', 'can_role_update', 'can_permission_read', 'can_license_read', 'can_establishment_update')
AND (EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'ADMIN_ACCESS'))
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_user_read', 'can_platform_role_read', 'can_platform_role_create', 'can_platform_role_update', 'can_establishment_delete', 'can_license_offer_read', 'can_license_offer_create', 'can_license_assign', 'can_license_suspend', 'can_license_reactivate', 'can_license_end', 'can_license_event_read', 'can_permission_read', 'can_license_read', 'can_establishment_update')
AND (r.code = 'ADMIN' AND r.establishment_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_establishment_read')
AND (r.code IN ('PARENT', 'STAFF', 'ESTABLISHMENT_ADMIN') OR EXISTS (SELECT 1 FROM security_role_permissions old_rp JOIN security_permissions old_p ON old_p.id = old_rp.permission_id WHERE old_rp.role_id = r.id AND old_p.code = 'ESTABLISHMENT_STAFF_MANAGEMENT'))
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_academic_year_read')
AND (r.code = 'PARENT')
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p WHERE p.code IN ('can_user_change_password')
AND (TRUE)
AND NOT EXISTS (SELECT 1 FROM security_role_permissions existing WHERE existing.role_id = r.id AND existing.permission_id = p.id);

DELETE FROM security_role_permissions WHERE permission_id IN (SELECT id FROM security_permissions WHERE code IN ('ADMIN_ACCESS', 'ESTABLISHMENT_STAFF_MANAGEMENT', 'STAFF_ACCESS'));
DELETE FROM security_permissions WHERE code IN ('ADMIN_ACCESS', 'ESTABLISHMENT_STAFF_MANAGEMENT', 'STAFF_ACCESS');
