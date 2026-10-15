package com.example.sb4oauth2login.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record OAuthClientInput(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9_-]{0,63}")
        String registrationId,

        @NotBlank String providerType,
        @NotBlank @Size(max = 120) String clientName,
        @Size(max = 120) String loginButtonLabel,
        @Size(max = 16) String loginButtonIcon,
        @NotBlank @Size(max = 512) String clientId,
        @Size(max = 4000) String clientSecret,
        @NotBlank String clientAuthenticationMethod,
        @NotBlank @Size(max = 1024) String redirectUri,
        @NotBlank @Size(max = 1000) String scopes,
        @NotBlank @Size(max = 2048) String authorizationUri,
        @NotBlank @Size(max = 2048) String tokenUri,
        @Size(max = 2048) String userInfoUri,
        @Size(max = 255) String userNameAttributeName,
        @Size(max = 2048) String issuerUri,
        @Size(max = 2048) String jwkSetUri,
        @Size(max = 20) Map<String, @Size(max = 512) String> authorizationParameters,
        @Size(max = 255) String subjectAttributeName,
        @Size(max = 255) String displayNameAttributeName,
        @Size(max = 255) String emailAttributeName,
        boolean enabled) {}
