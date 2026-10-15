ALTER TABLE oauth2_client
    ADD COLUMN provider_type VARCHAR(10) NOT NULL DEFAULT 'OAUTH2',
    ADD COLUMN login_button_label VARCHAR(120),
    ADD COLUMN login_button_icon VARCHAR(16),
    ADD CONSTRAINT oauth2_client_provider_type CHECK (provider_type IN ('OAUTH2', 'OIDC'));

UPDATE oauth2_client
SET provider_type = 'OIDC'
WHERE scopes ~ '(^|[ ,])openid([ ,]|$)';

ALTER TABLE oauth2_client_registration_template
    ADD COLUMN provider_type VARCHAR(10) NOT NULL DEFAULT 'OAUTH2',
    ADD COLUMN login_button_label VARCHAR(120),
    ADD COLUMN login_button_icon VARCHAR(16),
    ADD CONSTRAINT oauth2_template_provider_type CHECK (provider_type IN ('OAUTH2', 'OIDC'));

UPDATE oauth2_client_registration_template
SET provider_type = 'OIDC'
WHERE scopes ~ '(^|[ ,])openid([ ,]|$)';
