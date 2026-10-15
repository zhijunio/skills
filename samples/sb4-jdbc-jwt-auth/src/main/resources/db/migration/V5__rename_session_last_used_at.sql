ALTER TABLE user_sessions
    RENAME COLUMN last_used_at TO last_refreshed_at;

ALTER TABLE user_sessions
    ALTER COLUMN last_refreshed_at DROP DEFAULT,
    ALTER COLUMN last_refreshed_at DROP NOT NULL;

UPDATE user_sessions
SET last_refreshed_at = NULL
WHERE (SELECT COUNT(*) FROM refresh_tokens token WHERE token.session_id = user_sessions.id) <= 1;
