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
SELECT 'e86412cd-2f24-47e1-9738-ea6d1f73aa34', id
FROM security_roles
WHERE code = 'ADMIN';