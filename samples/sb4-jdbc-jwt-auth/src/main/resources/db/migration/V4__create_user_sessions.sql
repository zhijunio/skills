CREATE TABLE user_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_agent TEXT,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT user_sessions_id_user_unique UNIQUE (id, user_id)
);

CREATE INDEX user_sessions_user_created_idx ON user_sessions (user_id, created_at DESC);

INSERT INTO user_sessions (id, user_id, expires_at, revoked_at)
SELECT family_id, user_id, MAX(expires_at),
       CASE WHEN BOOL_OR(revoked_at IS NULL) THEN NULL ELSE MAX(revoked_at) END
FROM refresh_tokens
GROUP BY family_id, user_id;

ALTER TABLE refresh_tokens
    RENAME COLUMN family_id TO session_id;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_session_user_fk FOREIGN KEY (session_id, user_id)
        REFERENCES user_sessions(id, user_id) ON DELETE CASCADE;

ALTER INDEX refresh_tokens_family_idx RENAME TO refresh_tokens_session_idx;
