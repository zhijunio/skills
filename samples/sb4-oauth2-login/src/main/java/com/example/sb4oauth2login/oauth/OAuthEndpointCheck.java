package com.example.sb4oauth2login.oauth;

public record OAuthEndpointCheck(String endpoint, boolean reachable, Integer httpStatus, String message) {}
