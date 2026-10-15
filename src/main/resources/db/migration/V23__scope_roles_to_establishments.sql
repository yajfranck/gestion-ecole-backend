ALTER TABLE security_roles ADD COLUMN establishment_id UUID;

ALTER TABLE security_roles
    ADD CONSTRAINT fk_security_roles_establishment
    FOREIGN KEY (establishment_id) REFERENCES establishments (id);

CREATE INDEX ix_security_roles_establishment_id ON security_roles (establishment_id);