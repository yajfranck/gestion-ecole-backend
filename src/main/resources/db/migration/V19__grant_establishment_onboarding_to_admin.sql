INSERT INTO security_permissions (id, code, description)
SELECT 'a7e3b84d-0f93-4cee-b880-000000000001', 'can_establishment_onboard',
    'Create an establishment and invite its first administrator'
WHERE NOT EXISTS (
    SELECT 1 FROM security_permissions WHERE code = 'can_establishment_onboard'
);

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM security_roles r
CROSS JOIN security_permissions p
WHERE r.code = 'ADMIN'
    AND r.establishment_id IS NULL
    AND r.enabled = TRUE
    AND p.code = 'can_establishment_onboard'
    AND NOT EXISTS (
        SELECT 1 FROM security_role_permissions rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
    );

INSERT INTO security_role_scopes (role_id, scope_code)
SELECT r.id, 'PLATFORM'
FROM security_roles r
WHERE r.code = 'ADMIN'
    AND r.establishment_id IS NULL
    AND r.enabled = TRUE
    AND NOT EXISTS (
        SELECT 1 FROM security_role_scopes rs
        WHERE rs.role_id = r.id AND rs.scope_code = 'PLATFORM'
    );