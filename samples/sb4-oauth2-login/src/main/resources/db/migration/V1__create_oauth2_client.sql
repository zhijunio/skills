CREATE TABLE oauth2_client (
    registration_id VARCHAR(64) PRIMARY KEY,
    client_name VARCHAR(120) NOT NULL,
    client_id VARCHAR(512) NOT NULL,
    encrypted_client_secret TEXT NOT NULL,
    client_authentication_method VARCHAR(40) NOT NULL,
    authorization_grant_type VARCHAR(40) NOT NULL,
    redirect_uri VARCHAR(1024) NOT NULL,
    scopes TEXT NOT NULL,
    authorization_uri VARCHAR(2048) NOT NULL,
    token_uri VARCHAR(2048) NOT NULL,
    user_info_uri VARCHAR(2048),
    user_name_attribute_name VARCHAR(255),
    issuer_uri VARCHAR(2048),
    jwk_set_uri VARCHAR(2048),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT oauth2_client_registration_id_format CHECK (registration_id ~ '^[a-z0-9][a-z0-9_-]{0,63}$'),
    CONSTRAINT oauth2_client_auth_method CHECK (
        client_authentication_method IN ('client_secret_basic', 'client_secret_post', 'none')
    ),
    CONSTRAINT oauth2_client_grant_type CHECK (authorization_grant_type = 'authorization_code')
);
