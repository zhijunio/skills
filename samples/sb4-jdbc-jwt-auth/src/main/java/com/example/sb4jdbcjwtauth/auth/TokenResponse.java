package com.example.sb4jdbcjwtauth.auth;

import java.time.Instant;

public record TokenResponse(String sessionId, String accessToken, String tokenType, Instant expiresAt,
                            String refreshToken, Instant refreshExpiresAt) {
}
