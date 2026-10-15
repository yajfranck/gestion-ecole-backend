CREATE TABLE security_refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_account_id UUID NOT NULL,
    family_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_security_refresh_tokens_user
        FOREIGN KEY (user_account_id) REFERENCES security_user_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_security_refresh_tokens_family ON security_refresh_tokens (family_id);
CREATE INDEX idx_security_refresh_tokens_expiry ON security_refresh_tokens (expires_at);