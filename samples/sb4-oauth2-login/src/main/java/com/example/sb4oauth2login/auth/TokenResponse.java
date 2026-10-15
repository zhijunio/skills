package com.example.sb4oauth2login.auth;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {}
