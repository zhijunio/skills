-- Existing rows contain token hashes and cannot be converted back to their original tokens.
DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens
    ALTER COLUMN token TYPE VARCHAR(43),
    ADD CONSTRAINT refresh_tokens_token_length CHECK (length(token) = 43);

COMMENT ON COLUMN refresh_tokens.token IS 'Plaintext opaque refresh token; treat this column as credential material';
