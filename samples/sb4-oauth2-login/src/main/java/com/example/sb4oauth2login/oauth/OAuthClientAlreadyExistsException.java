package com.example.sb4oauth2login.oauth;

public class OAuthClientAlreadyExistsException extends RuntimeException {
    public OAuthClientAlreadyExistsException(String registrationId) {
        super("OAuth client already exists: " + registrationId);
    }
}
