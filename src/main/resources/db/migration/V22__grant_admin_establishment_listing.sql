INSERT INTO security_permissions (id, code, description)
VALUES ('a7e3b84d-0f93-4cee-b880-000000000002', 'can_establishment_read',
    'Read the list of establishments');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ADMIN'
    AND security_permissions.code = 'can_establishment_read';