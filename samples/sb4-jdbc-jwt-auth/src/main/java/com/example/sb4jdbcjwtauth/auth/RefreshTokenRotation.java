package com.example.sb4jdbcjwtauth.auth;


public record RefreshTokenRotation(String userId, TokenResponse tokens) {
}
