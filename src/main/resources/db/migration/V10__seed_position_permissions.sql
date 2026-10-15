INSERT INTO security_permissions (id, code, description)
VALUES
    ('da89a117-3d20-45c1-9a01-000000000001', 'can_position_read', 'Read positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000002', 'can_position_create', 'Create positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000003', 'can_position_update', 'Update positions in an establishment'),
    ('da89a117-3d20-45c1-9a01-000000000004', 'can_position_assign_roles', 'Assign roles to positions'),
    ('da89a117-3d20-45c1-9a01-000000000005', 'can_position_delete', 'Deactivate positions in an establishment');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT security_roles.id, security_permissions.id
FROM security_roles
CROSS JOIN security_permissions
WHERE security_roles.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN')
    AND security_permissions.code IN (
        'can_position_read',
        'can_position_create',
        'can_position_update',
        'can_position_assign_roles',
        'can_position_delete'
    );