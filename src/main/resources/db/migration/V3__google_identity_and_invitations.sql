CREATE TABLE IF NOT EXISTS external_identities (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_external_identity_provider_subject UNIQUE (provider, subject)
);

CREATE TABLE IF NOT EXISTS tenant_invitations (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    token VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invited_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_tenant_invitation_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_external_identity_user ON external_identities (user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_invitations_email_status ON tenant_invitations (email, status);
