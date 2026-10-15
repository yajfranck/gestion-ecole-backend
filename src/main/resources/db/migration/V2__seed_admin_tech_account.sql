INSERT INTO security_permissions (id, code, description)
VALUES ('5adcc0cb-a152-4e3a-8fcf-2636e2a2ddf0', 'ADMIN_ACCESS', 'Administrative access');

INSERT INTO security_roles (id, code, name, description, enabled)
VALUES ('af742010-e349-41bd-bdd1-621b43da2765', 'ADMIN', 'Administrator', 'Default administrator role', TRUE);

INSERT INTO security_role_permissions (role_id, permission_id)
VALUES ('af742010-e349-41bd-bdd1-621b43da2765', '5adcc0cb-a152-4e3a-8fcf-2636e2a2ddf0');

INSERT INTO security_user_accounts (
    id,
    username,
    password_hash,
    enabled,
    locked,
    created_at,
    updated_at
)
VALUES (
    'e86412cd-2f24-47e1-9738-ea6d1f73aa34',
    'admin-tech',
    '$2a$10$sFs3n2kOsIS8syUAE/gUn.yAYq5w8jQHZqhgvvkUzxI10YMIfkF9a',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO security_user_roles (user_account_id, role_id)
VALUES ('e86412cd-2f24-47e1-9738-ea6d1f73aa34', 'af742010-e349-41bd-bdd1-621b43da2765');
