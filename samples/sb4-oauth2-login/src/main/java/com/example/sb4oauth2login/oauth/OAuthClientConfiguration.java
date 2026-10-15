package com.example.sb4oauth2login.oauth;

import java.util.Map;

record OAuthClientConfiguration(
        String registrationId,
        String providerType,
        String clientName,
        String loginButtonLabel,
        String loginButtonIcon,
        String clientId,
        String encryptedClientSecret,
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
    OAuthClientConfiguration {
        authorizationParameters = Map.copyOf(authorizationParameters == null ? Map.of() : authorizationParameters);
    }

    OAuthClientSummary toSummary() {
        return new OAuthClientSummary(
                registrationId,
                providerType,
                clientName,
                loginButtonLabel,
                loginButtonIcon,
                clientId,
                !"none".equals(clientAuthenticationMethod)
                        && encryptedClientSecret != null
                        && !encryptedClientSecret.isBlank(),
                clientAuthenticationMethod,
                redirectUri,
                scopes,
                authorizationUri,
                tokenUri,
                userInfoUri,
                userNameAttributeName,
                issuerUri,
                jwkSetUri,
                authorizationParameters,
                subjectAttributeName,
                displayNameAttributeName,
                emailAttributeName,
                enabled);
    }
}
