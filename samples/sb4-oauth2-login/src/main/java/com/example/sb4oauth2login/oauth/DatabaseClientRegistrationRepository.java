package com.example.sb4oauth2login.oauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository {
    private final OAuthClientService clients;

    public DatabaseClientRegistrationRepository(OAuthClientService clients) {
        this.clients = clients;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return clients.findEnabledRegistration(registrationId);
    }
}
