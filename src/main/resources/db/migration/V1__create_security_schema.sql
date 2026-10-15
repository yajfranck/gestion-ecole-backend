CREATE TABLE security_user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE security_roles (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE security_permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE security_user_roles (
    user_account_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_account_id, role_id),
    CONSTRAINT fk_security_user_roles_user
        FOREIGN KEY (user_account_id) REFERENCES security_user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_security_user_roles_role
        FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE
);

CREATE TABLE security_role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_security_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_security_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES security_permissions (id) ON DELETE CASCADE
);