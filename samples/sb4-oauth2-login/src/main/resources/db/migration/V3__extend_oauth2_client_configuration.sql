ALTER TABLE oauth2_client
    ADD COLUMN authorization_parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN subject_attribute_name VARCHAR(255),
    ADD COLUMN display_name_attribute_name VARCHAR(255),
    ADD COLUMN email_attribute_name VARCHAR(255);
