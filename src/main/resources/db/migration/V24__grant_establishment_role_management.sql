INSERT INTO security_permissions (id, code, description)
VALUES
    ('a7e3b84d-0f93-4cee-b880-000000000003', 'can_role_read', 'Read roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000004', 'can_role_create', 'Create roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000005', 'can_role_update', 'Update roles in an establishment'),
    ('a7e3b84d-0f93-4cee-b880-000000000006', 'can_permission_read', 'Read assignable permissions');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code IN (
        'can_role_read', 'can_role_create', 'can_role_update', 'can_permission_read'
    );