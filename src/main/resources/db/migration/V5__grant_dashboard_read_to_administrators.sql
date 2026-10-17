INSERT INTO security_permissions (id, code, description)
VALUES ('c18e5b15-234a-4ae2-b87a-000000000001', 'can_dashboard_read', 'Read the administrator dashboard within the authenticated scope');

INSERT INTO security_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p
WHERE r.code IN ('ADMIN', 'ESTABLISHMENT_ADMIN') AND p.code = 'can_dashboard_read';
