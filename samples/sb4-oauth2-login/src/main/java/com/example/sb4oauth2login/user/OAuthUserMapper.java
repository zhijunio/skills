package com.example.sb4oauth2login.user;

import com.example.sb4oauth2login.oauth.OAuthClientService;
import com.example.sb4oauth2login.oauth.OAuthUserAttributeMapping;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuthUserMapper {
    private final InMemoryUserStore users;
    private final OAuthClientService clients;

    @Autowired
    public OAuthUserMapper(InMemoryUserStore users, OAuthClientService clients) {
        this.users = users;
        this.clients = clients;
    }

    public OAuthUserMapper(InMemoryUserStore users) {
        this.users = users;
        this.clients = null;
    }

    public AppUser map(String registrationId, OAuth2User principal) {
        String subject = principal.getName();
        Map<String, Object> attributes = principal.getAttributes();
        OAuthUserAttributeMapping mapping = clients == null
                ? new OAuthUserAttributeMapping(null, null, null)
                : clients.userAttributeMapping(registrationId);
        if (mapping.subject() != null && !mapping.subject().isBlank()) {
            subject = stableIdentifier(attributes.get(mapping.subject()));
        }
        if (subject == null || subject.isBlank()) {
            throw new OAuthUserMappingException("OAuth provider did not return a stable subject");
        }
        String displayName =
                firstText(attributes, mapping.displayName(), "name", "preferred_username", "login", "given_name");
        String email = firstText(attributes, mapping.email(), "email");
        AppUser user = users.findOrCreateExternal(registrationId, subject, displayName, email);
        if (!user.active()) {
            throw new OAuthUserMappingException("Local user is inactive");
        }
        return user;
    }

    private String firstText(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String value = text(attributes.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        return value instanceof String text ? text : null;
    }

    private String stableIdentifier(Object value) {
        if (value instanceof String text && !text.isBlank()) return text;
        if (value instanceof Number number) return number.toString();
        return null;
    }
}
