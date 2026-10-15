package com.example.sb4memoryjwt.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
