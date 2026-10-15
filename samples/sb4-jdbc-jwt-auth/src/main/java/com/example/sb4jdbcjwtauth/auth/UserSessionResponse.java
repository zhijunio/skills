package com.example.sb4jdbcjwtauth.auth;

import java.time.Instant;

public record UserSessionResponse(String sessionId, String userAgent, String ipAddress,
                                  Instant createdAt, Instant lastRefreshedAt, Instant expiresAt,
                                  boolean current) {
    static UserSessionResponse from(UserSession session, String currentSessionId) {
        return new UserSessionResponse(session.sessionId(), session.userAgent(), session.ipAddress(),
                session.createdAt(), session.lastRefreshedAt(), session.expiresAt(),
                session.sessionId().equals(currentSessionId));
    }
}
