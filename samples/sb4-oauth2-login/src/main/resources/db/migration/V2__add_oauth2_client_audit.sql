CREATE TABLE oauth2_client_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_id VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    changed_fields TEXT NOT NULL,
    client_secret_changed BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT oauth2_client_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);

CREATE INDEX oauth2_client_audit_occurred_at_idx ON oauth2_client_audit (occurred_at DESC);
