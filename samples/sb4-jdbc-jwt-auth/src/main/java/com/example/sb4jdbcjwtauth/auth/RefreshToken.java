package com.example.sb4jdbcjwtauth.auth;

import java.time.Instant;

public record RefreshToken(String sessionId, String userId, Instant expiresAt, Instant revokedAt) {
}
