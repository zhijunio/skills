package com.example.sb4jdbcjwtauth.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class UserSessionRepository {

    private final JdbcClient jdbc;

    public UserSessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String sessionId, String userId, Instant expiresAt, String userAgent, String ipAddress) {
        jdbc.sql("""
                        INSERT INTO user_sessions (id, user_id, expires_at, user_agent, ip_address)
                        VALUES (:sessionId, :userId, :expiresAt, :userAgent, CAST(:ipAddress AS inet))
                        """)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .param("expiresAt", expiresAt)
                .param("userAgent", userAgent)
                .param("ipAddress", ipAddress)
                .update();
    }

    public void touch(String sessionId, Instant now, Instant expiresAt) {
        jdbc.sql("""
                        UPDATE user_sessions SET last_refreshed_at = :now, expires_at = :expiresAt
                        WHERE id = :sessionId AND revoked_at IS NULL
                        """)
                .param("now", now)
                .param("expiresAt", expiresAt)
                .param("sessionId", sessionId)
                .update();
    }

    public List<UserSession> findActiveByUser(String userId, Instant now) {
        return jdbc.sql("""
                        SELECT id AS session_id, user_agent, ip_address::text AS ip_address,
                               created_at, last_refreshed_at, expires_at
                        FROM user_sessions
                        WHERE user_id = :userId AND revoked_at IS NULL AND expires_at > :now
                        ORDER BY last_refreshed_at DESC NULLS LAST, created_at DESC
                        """)
                .param("userId", userId)
                .param("now", now)
                .query(UserSession.class)
                .list();
    }

    public boolean isActive(String sessionId, String userId, Instant now) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM user_sessions
                            WHERE id = :sessionId AND user_id = :userId
                              AND revoked_at IS NULL AND expires_at > :now
                        )
                        """)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .param("now", now)
                .query(Boolean.class)
                .single();
    }

    public boolean revoke(String sessionId, String userId, Instant now) {
        return jdbc.sql("""
                        UPDATE user_sessions SET revoked_at = :now
                        WHERE id = :sessionId AND user_id = :userId AND revoked_at IS NULL
                        """)
                .param("now", now)
                .param("sessionId", sessionId)
                .param("userId", userId)
                .update() > 0;
    }

    public void deleteExpired(Instant now) {
        jdbc.sql("DELETE FROM user_sessions WHERE expires_at <= :now")
                .param("now", now)
                .update();
    }
}
