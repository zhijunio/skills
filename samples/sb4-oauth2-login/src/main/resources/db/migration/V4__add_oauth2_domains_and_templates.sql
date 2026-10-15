CREATE TABLE domain (
    id UUID PRIMARY KEY,
    name VARCHAR(253) NOT NULL UNIQUE,
    oauth2_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oauth2_client_registration_template (
    provider_id VARCHAR(64) PRIMARY KEY,
    client_name VARCHAR(120) NOT NULL,
    authorization_uri VARCHAR(2048),
    token_uri VARCHAR(2048),
    scopes VARCHAR(1000) NOT NULL,
    user_info_uri VARCHAR(2048),
    user_name_attribute_name VARCHAR(255),
    client_authentication_method VARCHAR(40) NOT NULL DEFAULT 'client_secret_basic',
    issuer_uri VARCHAR(2048),
    jwk_set_uri VARCHAR(2048),
    subject_attribute_name VARCHAR(255),
    display_name_attribute_name VARCHAR(255),
    email_attribute_name VARCHAR(255),
    authorization_parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    description VARCHAR(1000) NOT NULL DEFAULT ''
);

CREATE TABLE domain_oauth2_client (
    domain_id UUID NOT NULL REFERENCES domain(id) ON DELETE CASCADE,
    registration_id VARCHAR(64) NOT NULL REFERENCES oauth2_client(registration_id) ON DELETE CASCADE,
    PRIMARY KEY (domain_id, registration_id)
);

CREATE INDEX domain_oauth2_client_registration_id_idx ON domain_oauth2_client (registration_id);

INSERT INTO domain (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'localhost');

INSERT INTO oauth2_client_registration_template (
    provider_id, client_name, authorization_uri, token_uri, scopes, user_info_uri,
    user_name_attribute_name, client_authentication_method, issuer_uri, jwk_set_uri,
    subject_attribute_name, display_name_attribute_name, email_attribute_name, description
) VALUES
    ('github', 'GitHub', 'https://github.com/login/oauth/authorize', 'https://github.com/login/oauth/access_token',
     'read:user user:email', 'https://api.github.com/user', 'id', 'client_secret_basic', NULL, NULL,
     'id', 'name', 'email', 'GitHub OAuth App'),
    ('google', 'Google', 'https://accounts.google.com/o/oauth2/v2/auth', 'https://oauth2.googleapis.com/token',
     'openid profile email', 'https://openidconnect.googleapis.com/v1/userinfo', 'sub', 'client_secret_basic',
     'https://accounts.google.com', 'https://www.googleapis.com/oauth2/v3/certs',
     'sub', 'name', 'email', 'Google OpenID Connect'),
    ('facebook', 'Facebook', NULL, NULL,
     'public_profile email', NULL, 'id', 'client_secret_post', NULL, NULL,
     'id', 'name', 'email', 'Fill in endpoints for the Graph API version enabled by your Meta app'),
    ('apple', 'Apple', 'https://appleid.apple.com/auth/authorize', 'https://appleid.apple.com/auth/token',
     'openid email', NULL, 'sub', 'client_secret_post', 'https://appleid.apple.com',
     'https://appleid.apple.com/auth/keys', 'sub', 'name', 'email',
     'Apple Sign in; Client Secret must be an Apple-signed JWT'),
    ('custom-oidc', '自定义 OIDC', NULL, NULL, 'openid profile email', NULL, 'sub', 'client_secret_basic',
     NULL, NULL, 'sub', 'name', 'email', '填写受信任的 OIDC 提供方端点');
