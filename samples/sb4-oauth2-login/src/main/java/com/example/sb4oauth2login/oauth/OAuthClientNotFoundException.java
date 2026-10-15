package com.example.sb4oauth2login.oauth;

public class OAuthClientNotFoundException extends RuntimeException {
    public OAuthClientNotFoundException(String registrationId) {
        super("OAuth client not found: " + registrationId);
    }
}
