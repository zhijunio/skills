package com.example.sb4oauth2login.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final OAuth2AuthorizationRequestResolver delegate;
    private final OAuthClientService clients;

    public DatabaseAuthorizationRequestResolver(
            ClientRegistrationRepository registrations, OAuthClientService clients) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        this.clients = clients;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return addConfiguredParameters(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return addConfiguredParameters(delegate.resolve(request, registrationId));
    }

    private OAuth2AuthorizationRequest addConfiguredParameters(OAuth2AuthorizationRequest request) {
        if (request == null) return null;
        String registrationId = (String) request.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
        Map<String, Object> parameters = new HashMap<>(request.getAdditionalParameters());
        parameters.putAll(clients.authorizationParameters(registrationId));
        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(parameters)
                .build();
    }
}
