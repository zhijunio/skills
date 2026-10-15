package com.example.sb4jwt.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
