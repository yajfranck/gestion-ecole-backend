\set ON_ERROR_STOP on
\getenv admin_password_hash ADMIN_PASSWORD_HASH

\if :{?admin_password_hash}
\else
\echo 'ADMIN_PASSWORD_HASH is required.'
\quit 2
\endif

SELECT length(:'admin_password_hash') = 60
    AND :'admin_password_hash' ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'
    AS valid_bcrypt_hash
\gset

\if :valid_bcrypt_hash
\else
\echo 'ADMIN_PASSWORD_HASH must be a BCrypt hash.'
\quit 2
\endif

SELECT id AS admin_role_id
FROM security_roles
WHERE code = 'ADMIN' AND enabled = TRUE
\gset

\if :{?admin_role_id}
\else
\echo 'An enabled ADMIN role is required. Apply Flyway migrations first.'
\quit 3
\endif

BEGIN;

SELECT EXISTS (
    SELECT 1 FROM security_user_accounts WHERE username = 'admin-tech'
) AS admin_account_exists
\gset

\if :admin_account_exists
\echo 'admin-tech already exists; preserving its password, roles and permissions.'
ROLLBACK;
\quit 0
\endif

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
    gen_random_uuid(),
    'admin-tech',
    :'admin_password_hash',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
RETURNING id AS admin_user_id
\gset

INSERT INTO security_user_roles (user_account_id, role_id)
VALUES (:'admin_user_id', :'admin_role_id');

COMMIT;
\echo 'admin-tech was provisioned successfully.'