package com.example.sb4jdbcjwtauth.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private final JdbcClient jdbc;

    public RefreshTokenRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RefreshToken> findForUpdate(String token) {
        return jdbc.sql("""
                        SELECT session_id, user_id, expires_at, revoked_at
                        FROM refresh_tokens WHERE token = :token FOR UPDATE
                        """)
                .param("token", token)
                .query(RefreshToken.class)
                .optional();
    }

    public boolean lockActiveSession(String token, Instant now) {
        return jdbc.sql("""
                        SELECT TRUE
                        FROM user_sessions s
                        JOIN refresh_tokens t ON t.session_id = s.id
                        WHERE t.token = :token AND s.revoked_at IS NULL AND s.expires_at > :now
                        FOR UPDATE OF s
                        """)
                .param("token", token)
                .param("now", now)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    public void insert(String token, String sessionId, String userId, Instant expiresAt) {
        jdbc.sql("""
                        INSERT INTO refresh_tokens (token, session_id, user_id, expires_at)
                        VALUES (:token, :sessionId, :userId, :expiresAt)
                        """)
                .param("token", token)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .param("expiresAt", expiresAt)
                .update();
    }

    public void revoke(String token, Instant now) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now WHERE token = :token AND revoked_at IS NULL")
                .param("now", now)
                .param("token", token)
                .update();
    }

    public void revokeSession(String sessionId, Instant now) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now WHERE session_id = :sessionId AND revoked_at IS NULL")
                .param("now", now)
                .param("sessionId", sessionId)
                .update();
    }

}
