package com.example.config;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extracts claims from the current JWT token.
 * Inject this into services that need to know the authenticated user.
 */
@Component
public class JwtClaimExtractor {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    public ExternalIdentity getIdentity() {
        return ExternalIdentity.from(getJwt());
    }

    // Opaque provider id ("auth0|...", Okta "00u..."): map it to your user record, don't parse it.
    public String getSubject() {
        return getClaim("sub");
    }

    public String getEmail() {
        return getClaim("email");
    }

    public String getUsername() {
        return getClaim("preferred_username");
    }

    public List<String> getScopes() {
        return scopeConverter.convert(getJwt()).stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.substring("SCOPE_".length()))
            .toList();
    }

    /** Reads provider claim values for display. Authorize using mapped authorities. */
    public List<String> getKeycloakRoles() {
        Object realmAccess = getJwt().getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> claims)
                || !(claims.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
    }

    private String getClaim(String claimName) {
        return getJwt().getClaimAsString(claimName);
    }

    private Jwt getJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT required");
        }
        return jwt;
    }
}
