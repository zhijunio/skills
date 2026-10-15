package com.example.sb4oauth2login.oauth;

import java.util.Map;

public record OAuthClientTemplateSummary(
        String providerId,
        String providerType,
        String clientName,
        String loginButtonLabel,
        String loginButtonIcon,
        String authorizationUri,
        String tokenUri,
        String scopes,
        String userInfoUri,
        String userNameAttributeName,
        String clientAuthenticationMethod,
        String issuerUri,
        String jwkSetUri,
        String subjectAttributeName,
        String displayNameAttributeName,
        String emailAttributeName,
        Map<String, String> authorizationParameters,
        String description) {
    public OAuthClientTemplateSummary {
        authorizationParameters = Map.copyOf(authorizationParameters == null ? Map.of() : authorizationParameters);
    }
}
