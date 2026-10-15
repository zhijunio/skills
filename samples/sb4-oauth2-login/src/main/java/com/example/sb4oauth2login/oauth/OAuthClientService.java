package com.example.sb4oauth2login.oauth;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OAuthClientService {
    private static final Set<String> CLIENT_AUTHENTICATION_METHODS =
            Set.of("client_secret_basic", "client_secret_post", "none");
    private static final Set<String> PROVIDER_TYPES = Set.of("OAUTH2", "OIDC");

    private final OAuthClientStore clients;
    private final OAuthClientSecretCipher secrets;
    private final OAuthClientAuditStore audit;
    private final OAuthEndpointChecker endpointChecker;
    private final Clock clock;
    private final Set<String> allowedHosts;

    OAuthClientService(
            OAuthClientStore clients,
            OAuthClientSecretCipher secrets,
            OAuthClientAuditStore audit,
            OAuthEndpointChecker endpointChecker,
            Clock clock,
            @Value("${app.oauth2.allowed-hosts}") List<String> allowedHosts) {
        this.clients = clients;
        this.secrets = secrets;
        this.audit = audit;
        this.endpointChecker = endpointChecker;
        this.clock = clock;
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.strip().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<OAuthClientSummary> findAll() {
        return clients.findAll().stream()
                .map(OAuthClientConfiguration::toSummary)
                .toList();
    }

    public List<OAuthClientSummary> findEnabled() {
        return clients.findAll().stream()
                .filter(OAuthClientConfiguration::enabled)
                .filter(this::providerEndpointsAllowed)
                .map(OAuthClientConfiguration::toSummary)
                .toList();
    }

    public List<OAuthClientAuditRecord> findRecentAudit() {
        return audit.findRecent();
    }

    public OAuthClientConnectionResult testConnection(String registrationId) {
        OAuthClientConfiguration client = clients.findByRegistrationId(registrationId)
                .orElseThrow(() -> new OAuthClientNotFoundException(registrationId));
        validateProviderEndpoints(client);
        List<OAuthEndpointCheck> checks = new ArrayList<>();
        checks.add(endpointChecker.check("授权端点", client.authorizationUri()));
        checks.add(endpointChecker.check("Token 端点", client.tokenUri()));
        checks.add(endpointChecker.check("用户信息端点", client.userInfoUri()));
        checks.add(endpointChecker.check("Issuer", client.issuerUri()));
        checks.add(endpointChecker.check("JWK Set", client.jwkSetUri()));
        return new OAuthClientConnectionResult(
                registrationId, checks.stream().allMatch(OAuthEndpointCheck::reachable), clock.instant(), checks);
    }

    public Map<String, String> authorizationParameters(String registrationId) {
        return clients.findByRegistrationId(registrationId)
                .filter(OAuthClientConfiguration::enabled)
                .map(OAuthClientConfiguration::authorizationParameters)
                .orElse(Map.of());
    }

    public OAuthUserAttributeMapping userAttributeMapping(String registrationId) {
        return clients.findByRegistrationId(registrationId)
                .filter(OAuthClientConfiguration::enabled)
                .map(client -> new OAuthUserAttributeMapping(
                        client.subjectAttributeName(), client.displayNameAttributeName(), client.emailAttributeName()))
                .orElse(new OAuthUserAttributeMapping(null, null, null));
    }

    @Transactional
    public OAuthClientSummary create(OAuthClientInput input, String actor) {
        OAuthClientSummary saved;
        try {
            saved = save(input, null, false);
        } catch (DuplicateKeyException exception) {
            throw new OAuthClientAlreadyExistsException(input.registrationId());
        }
        audit.record(
                saved.registrationId(),
                "CREATE",
                actor,
                "providerType, clientName, loginButton, clientId, clientAuthenticationMethod, redirectUri, scopes, endpoints, enabled",
                StringUtils.hasText(input.clientSecret()));
        return saved;
    }

    @Transactional
    public Optional<OAuthClientSummary> update(String registrationId, OAuthClientInput input, String actor) {
        if (!registrationId.equals(input.registrationId())) {
            throw new IllegalArgumentException("Registration ID cannot be changed");
        }
        OAuthClientConfiguration existing =
                clients.findByRegistrationIdForUpdate(registrationId).orElse(null);
        if (existing == null) {
            return Optional.empty();
        }
        OAuthClientSummary previous = existing.toSummary();
        OAuthClientSummary updated = save(input, existing, true);
        boolean secretChanged = StringUtils.hasText(input.clientSecret())
                || (!Objects.equals(previous.clientAuthenticationMethod(), updated.clientAuthenticationMethod())
                        && !"none".equals(previous.clientAuthenticationMethod()));
        audit.record(registrationId, "UPDATE", actor, changedFields(previous, updated), secretChanged);
        return Optional.of(updated);
    }

    @Transactional
    public boolean delete(String registrationId, String actor) {
        OAuthClientConfiguration existing =
                clients.findByRegistrationId(registrationId).orElse(null);
        if (existing == null || !clients.delete(registrationId)) {
            return false;
        }
        audit.record(
                registrationId,
                "DELETE",
                actor,
                "all configuration",
                existing.toSummary().clientSecretConfigured());
        return true;
    }

    private String changedFields(OAuthClientSummary oldValue, OAuthClientSummary newValue) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(oldValue.clientName(), newValue.clientName())) fields.add("clientName");
        if (!Objects.equals(oldValue.providerType(), newValue.providerType())) fields.add("providerType");
        if (!Objects.equals(oldValue.loginButtonLabel(), newValue.loginButtonLabel())
                || !Objects.equals(oldValue.loginButtonIcon(), newValue.loginButtonIcon())) fields.add("loginButton");
        if (!Objects.equals(oldValue.clientId(), newValue.clientId())) fields.add("clientId");
        if (!Objects.equals(oldValue.clientAuthenticationMethod(), newValue.clientAuthenticationMethod()))
            fields.add("clientAuthenticationMethod");
        if (!Objects.equals(oldValue.redirectUri(), newValue.redirectUri())) fields.add("redirectUri");
        if (!Objects.equals(oldValue.scopes(), newValue.scopes())) fields.add("scopes");
        if (!Objects.equals(oldValue.authorizationUri(), newValue.authorizationUri())
                || !Objects.equals(oldValue.tokenUri(), newValue.tokenUri())
                || !Objects.equals(oldValue.userInfoUri(), newValue.userInfoUri())
                || !Objects.equals(oldValue.issuerUri(), newValue.issuerUri())
                || !Objects.equals(oldValue.jwkSetUri(), newValue.jwkSetUri())) fields.add("endpoints");
        if (!Objects.equals(oldValue.userNameAttributeName(), newValue.userNameAttributeName()))
            fields.add("userNameAttributeName");
        if (!Objects.equals(oldValue.authorizationParameters(), newValue.authorizationParameters()))
            fields.add("authorizationParameters");
        if (!Objects.equals(oldValue.subjectAttributeName(), newValue.subjectAttributeName())
                || !Objects.equals(oldValue.displayNameAttributeName(), newValue.displayNameAttributeName())
                || !Objects.equals(oldValue.emailAttributeName(), newValue.emailAttributeName())) {
            fields.add("userAttributeMapping");
        }
        if (oldValue.enabled() != newValue.enabled()) fields.add("enabled");
        if (fields.isEmpty()) fields.add("no configuration fields");
        return String.join(", ", fields);
    }

    ClientRegistration findEnabledRegistration(String registrationId) {
        return clients.findByRegistrationId(registrationId)
                .filter(OAuthClientConfiguration::enabled)
                .filter(this::providerEndpointsAllowed)
                .map(this::toRegistration)
                .orElse(null);
    }

    private OAuthClientSummary save(OAuthClientInput input, OAuthClientConfiguration existing, boolean update) {
        validate(input);
        String encryptedSecret;
        if ("none".equals(input.clientAuthenticationMethod())) {
            encryptedSecret = secrets.encrypt("");
        } else if (StringUtils.hasText(input.clientSecret())) {
            encryptedSecret = secrets.encrypt(input.clientSecret());
        } else if (existing != null && !"none".equals(existing.clientAuthenticationMethod())) {
            encryptedSecret = existing.encryptedClientSecret();
        } else {
            throw new IllegalArgumentException("Client secret is required for this authentication method");
        }

        OAuthClientConfiguration configuration = new OAuthClientConfiguration(
                input.registrationId(),
                input.providerType(),
                input.clientName().strip(),
                emptyToNull(input.loginButtonLabel()),
                emptyToNull(input.loginButtonIcon()),
                input.clientId().strip(),
                encryptedSecret,
                input.clientAuthenticationMethod(),
                input.redirectUri().strip(),
                normalizeScopes(input.scopes()),
                input.authorizationUri().strip(),
                input.tokenUri().strip(),
                emptyToNull(input.userInfoUri()),
                emptyToNull(input.userNameAttributeName()),
                emptyToNull(input.issuerUri()),
                emptyToNull(input.jwkSetUri()),
                normalizeAuthorizationParameters(input.authorizationParameters()),
                emptyToNull(input.subjectAttributeName()),
                emptyToNull(input.displayNameAttributeName()),
                emptyToNull(input.emailAttributeName()),
                input.enabled());
        if (configuration.enabled()) {
            toRegistration(configuration);
        }
        if (update) clients.update(configuration);
        else clients.insert(configuration);
        return configuration.toSummary();
    }

    private void validate(OAuthClientInput input) {
        if (!CLIENT_AUTHENTICATION_METHODS.contains(input.clientAuthenticationMethod())) {
            throw new IllegalArgumentException("Unsupported client authentication method");
        }
        if (!PROVIDER_TYPES.contains(input.providerType())) {
            throw new IllegalArgumentException("Provider type must be OAUTH2 or OIDC");
        }
        List<String> scopes = splitScopes(input.scopes());
        boolean oidc = "OIDC".equals(input.providerType());
        if (oidc != scopes.contains("openid")) {
            throw new IllegalArgumentException("OIDC clients require the openid scope; OAuth clients must omit it");
        }
        validateProviderUri(input.authorizationUri(), "Authorization URI");
        validateProviderUri(input.tokenUri(), "Token URI");
        validateHttpUri(
                input.redirectUri()
                        .replace("{baseUrl}", "https://app.example.test")
                        .replace("{registrationId}", "provider"),
                "Redirect URI");
        if (StringUtils.hasText(input.userInfoUri())) {
            validateProviderUri(input.userInfoUri(), "User info URI");
        }
        if (StringUtils.hasText(input.issuerUri())) {
            validateProviderUri(input.issuerUri(), "Issuer URI");
        }
        if (StringUtils.hasText(input.jwkSetUri())) {
            validateProviderUri(input.jwkSetUri(), "JWK Set URI");
        }
        if (oidc && (!StringUtils.hasText(input.issuerUri()) || !StringUtils.hasText(input.jwkSetUri()))) {
            throw new IllegalArgumentException("OIDC clients require issuer URI and JWK Set URI");
        }
        if (!oidc
                && (!StringUtils.hasText(input.userInfoUri()) || !StringUtils.hasText(input.userNameAttributeName()))) {
            throw new IllegalArgumentException("OAuth clients require user info URI and user name attribute");
        }
        if ("none".equals(input.clientAuthenticationMethod()) && StringUtils.hasText(input.clientSecret())) {
            throw new IllegalArgumentException("Public clients must not have a client secret");
        }
        validateAttributeKey(input.subjectAttributeName(), "Subject attribute");
        validateAttributeKey(input.displayNameAttributeName(), "Display name attribute");
        validateAttributeKey(input.emailAttributeName(), "Email attribute");
        if (StringUtils.hasText(input.loginButtonIcon())
                && !input.loginButtonIcon().matches("[A-Za-z0-9+.-]{1,16}")) {
            throw new IllegalArgumentException("Login button icon must be a short text token");
        }
    }

    private ClientRegistration toRegistration(OAuthClientConfiguration client) {
        List<String> scopes = splitScopes(client.scopes());
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(client.registrationId())
                .clientName(client.clientName())
                .clientId(client.clientId())
                .clientSecret(secrets.decrypt(client.encryptedClientSecret()))
                .clientAuthenticationMethod(authenticationMethod(client.clientAuthenticationMethod()))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(client.redirectUri())
                .scope(scopes)
                .authorizationUri(client.authorizationUri())
                .tokenUri(client.tokenUri())
                .userInfoUri(client.userInfoUri())
                .userNameAttributeName(client.userNameAttributeName())
                .issuerUri(client.issuerUri())
                .jwkSetUri(client.jwkSetUri());
        return builder.build();
    }

    private ClientAuthenticationMethod authenticationMethod(String method) {
        return switch (method) {
            case "client_secret_basic" -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case "none" -> ClientAuthenticationMethod.NONE;
            default -> throw new IllegalArgumentException("Unsupported client authentication method");
        };
    }

    private List<String> splitScopes(String scopes) {
        return Arrays.stream(scopes.split("[\\s,]+"))
                .map(String::strip)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalizeScopes(String scopes) {
        return String.join(" ", splitScopes(scopes));
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }

    private Map<String, String> normalizeAuthorizationParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) return Map.of();
        if (parameters.size() > 20)
            throw new IllegalArgumentException("At most 20 authorization parameters are allowed");
        Set<String> reserved = Set.of(
                "client_id",
                "response_type",
                "redirect_uri",
                "scope",
                "state",
                "nonce",
                "code_challenge",
                "code_challenge_method");
        Map<String, String> normalized = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            if (key == null
                    || !key.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}")
                    || reserved.contains(key.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Invalid or reserved authorization parameter name");
            }
            if (value == null || value.length() > 512) {
                throw new IllegalArgumentException(
                        "Authorization parameter values must be non-null and at most 512 characters");
            }
            normalized.put(key, value);
        });
        return Map.copyOf(normalized);
    }

    private void validateAttributeKey(String key, String label) {
        if (StringUtils.hasText(key) && !key.matches("[A-Za-z0-9_.-]{1,255}")) {
            throw new IllegalArgumentException(label + " must be a simple attribute name");
        }
    }

    private void validateProviderUri(String value, String label) {
        validateHttpUri(value, label);
        URI uri = URI.create(value);
        String host = uri.getHost();
        if (host == null || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(label + " host is not in OAUTH2_ALLOWED_HOSTS");
        }
        if ("http".equalsIgnoreCase(uri.getScheme())
                && !Set.of("localhost", "127.0.0.1").contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(label + " must use HTTPS outside localhost");
        }
    }

    private boolean providerEndpointsAllowed(OAuthClientConfiguration client) {
        try {
            validateProviderEndpoints(client);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validateProviderEndpoints(OAuthClientConfiguration client) {
        validateProviderUri(client.authorizationUri(), "Authorization URI");
        validateProviderUri(client.tokenUri(), "Token URI");
        if (StringUtils.hasText(client.userInfoUri())) validateProviderUri(client.userInfoUri(), "User info URI");
        if (StringUtils.hasText(client.issuerUri())) validateProviderUri(client.issuerUri(), "Issuer URI");
        if (StringUtils.hasText(client.jwkSetUri())) validateProviderUri(client.jwkSetUri(), "JWK Set URI");
    }

    private void validateHttpUri(String value, String label) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute()
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException(label + " must be an absolute HTTP(S) URI");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(label + " must be an absolute HTTP(S) URI");
        }
    }
}
