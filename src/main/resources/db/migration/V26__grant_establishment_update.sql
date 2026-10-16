INSERT INTO security_permissions (id, code, description)
VALUES ('b410f227-bc64-4a94-84a7-000000000001', 'can_establishment_update', 'Update establishment code and name');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code = 'ESTABLISHMENT_ADMIN'
    AND security_permissions.code = 'can_establishment_update';