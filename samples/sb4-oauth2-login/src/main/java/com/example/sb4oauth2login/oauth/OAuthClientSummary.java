package com.example.sb4oauth2login.oauth;

import java.util.Map;

public record OAuthClientSummary(
        String registrationId,
        String providerType,
        String clientName,
        String loginButtonLabel,
        String loginButtonIcon,
        String clientId,
        boolean clientSecretConfigured,
        String clientAuthenticationMethod,
        String redirectUri,
        String scopes,
        String authorizationUri,
        String tokenUri,
        String userInfoUri,
        String userNameAttributeName,
        String issuerUri,
        String jwkSetUri,
        Map<String, String> authorizationParameters,
        String subjectAttributeName,
        String displayNameAttributeName,
        String emailAttributeName,
        boolean enabled) {
    public OAuthClientSummary {
        authorizationParameters = Map.copyOf(authorizationParameters == null ? Map.of() : authorizationParameters);
    }
}
