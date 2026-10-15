package com.example.sb4jdbcjwtauth.auth;

import java.time.Instant;

public record UserSession(String sessionId, String userAgent, String ipAddress,
                          Instant createdAt, Instant lastRefreshedAt, Instant expiresAt) {
}
