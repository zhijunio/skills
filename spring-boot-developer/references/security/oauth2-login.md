# OAuth2 Login and Single Sign-On

Use this guide when a Spring Boot application logs users in through an external identity provider (IdP), consumes the resulting identity in its own session, or needs to decide between OIDC, SAML, and running an authorization server. It covers the common integration path and the security decisions shared across providers; provider-specific settings must still be checked against the actual IdP metadata and documentation.

## Choose the application role first

| Requirement | Application role | Start with |
|---|---|---|
| Sign a user into a browser application through Google, Entra ID, Keycloak, Okta, or another IdP | OAuth2/OIDC client | This guide |
| Accept access tokens on an API | Resource server | [OAuth2 resource server](oauth2-resource-server.md) |
| Issue and manage tokens for your own clients | Authorization server | Use a managed IdP unless owning the protocol and account lifecycle is a requirement |
| Connect to an enterprise IdP that requires XML assertions | SAML service provider (relying party) | SAML section below |

OAuth2 is an authorization framework. Use **OpenID Connect (OIDC)** when the application needs a standardized user identity; a plain OAuth2 provider may only authorize access to an API and may not provide a stable identity contract.

## Prefer OIDC for a new login integration

Choose OIDC when the IdP supports it. Use SAML when a customer or existing enterprise identity system requires it, or when the IdP's OIDC support cannot meet the integration requirements. Do not select a protocol based only on familiarity: confirm supported protocols, issuer/entity ID, claims, logout behavior, and registration requirements with the IdP owner.

Before coding, establish:

- The exact issuer and discovery document, or the IdP metadata document.
- Whether the app is a server-side browser application, a backend-for-frontend (BFF), or a public SPA.
- Which stable subject identifier and profile claims the app needs.
- How local accounts are created, linked, disabled, and authorized.
- Whether login must support multiple organizations or IdPs.
- Who owns IdP registration, redirect URIs, secrets, signing keys, and logout configuration.

## OIDC login with Spring Security

Use Spring Boot's OAuth2 client support and Spring Security's `oauth2Login()` filter flow. Boot 4 uses `spring-boot-starter-security-oauth2-client`; Boot 3.5 uses `spring-boot-starter-oauth2-client`.

Configure credentials outside committed files. With an OIDC issuer that supports discovery, start with `issuer-uri`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          company:
            provider: company
            client-id: ${OIDC_CLIENT_ID}
            client-secret: ${OIDC_CLIENT_SECRET}
            scope: openid, profile, email
        provider:
          company:
            issuer-uri: ${OIDC_ISSUER_URI}
```

Keep the normal session and CSRF protections for a browser login:

```java
@Bean
SecurityFilterChain browserSecurity(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/assets/**", "/error").permitAll()
                    .anyRequest().authenticated())
            .oauth2Login(Customizer.withDefaults())
            .logout(Customizer.withDefaults())
            .build();
}
```

The default login initiation path is `/oauth2/authorization/{registrationId}` and the default callback pattern is `/login/oauth2/code/{registrationId}`. Register the exact externally reachable callback URI with the IdP. When TLS terminates at a proxy, configure trusted forwarded headers so Spring constructs the public `https` redirect URI; do not trust forwarded headers from arbitrary clients.

Start a browser login with a top-level navigation to the initiation path. An AJAX `fetch` cannot turn an API call into a usable cross-site login experience; it commonly ends as a CORS or redirect-handling failure.

Use discovery from the issuer where available. If the provider requires explicit endpoints, configure its authorization, token, and user-info endpoints and the user-name attribute from the provider's actual metadata. Do not guess endpoint paths or build issuer URLs from tenant names without validating the discovery document.

Spring Security correlates the authorization response with the saved authorization request. Preserve `state`, OIDC `nonce`, exact redirect URI matching, and PKCE where applicable. Do not build the authorization-code exchange or callback validation by hand. Keep client secrets in environment-backed configuration or a secret manager.

## Map the external identity to a local account

An authenticated OIDC principal is not automatically a local application user. Resolve it to a local account using the validated **issuer plus subject (`iss` + `sub`)** as the external identity key. Email can be a profile attribute, but it may be absent, mutable, or unverified; do not silently link accounts by matching email alone.

Review the actual claims returned by each IdP before mapping them. Claim names and group/role locations differ by provider and token type; do not assume that a `roles`, `groups`, or email claim has the same meaning across providers. Preserve the default scope authorities when adding custom role mappings, and keep the mapping allowlisted and tenant-scoped.

For example, map only known application roles from a claim the IdP has agreed to issue. Replace `app_roles` and the values below with the provider's verified contract:

```java
@Bean
GrantedAuthoritiesMapper oidcAuthoritiesMapper() {
    Set<String> allowedRoles = Set.of("APP_USER", "APP_ADMIN");
    return authorities -> {
        Set<GrantedAuthority> mapped = new HashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidc) {
                Object claim = oidc.getIdToken().getClaims().get("app_roles");
                if (claim instanceof Collection<?> roles) {
                    roles.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .filter(allowedRoles::contains)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .forEach(mapped::add);
                }
            }
        }
        return mapped;
    };
}
```

Attach it to the login flow with `.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(oidcAuthoritiesMapper())))`. Preserve existing scope authorities, and test the mapper with the exact claims your IdP returns.

Choose one account lifecycle:

- **Just-in-time provisioning:** create a local account on first successful login, with minimal default access.
- **Pre-provisioning:** require an existing local account and reject unknown external subjects.
- **SCIM provisioning:** synchronize lifecycle changes from an IdP when provisioning, updates, and deactivation must happen before the next login.

Map provider roles or groups explicitly to application authorities. Keep provider claims and tokens out of domain services; pass a local user ID and the authorities the use case needs. Never grant administrator access solely because a claim is named `admin` or `roles` without confirming its issuer, meaning, audience, and mapping.

## Browser, SPA, and API boundaries

For a browser application, prefer a BFF: the browser uses an HttpOnly, Secure, SameSite session cookie, while the server keeps OAuth tokens. Retain CSRF protection for cookie-authenticated state-changing requests.

A pure SPA is a public OAuth client and cannot keep a client secret. Use Authorization Code with PKCE through a supported client library; validate `state` and OIDC `nonce`, and keep access tokens in memory where possible. Do not embed a client secret or treat a browser-held token as a server session.

Do not confuse OAuth2 login with API token validation. An API accepting bearer access tokens should be configured as a resource server and validate issuer, signature, expiry, and audience. See [OAuth2 resource server](oauth2-resource-server.md). For self-issued JWTs, see [JWT authentication](jwt-authentication.md). For MFA and step-up policy, see [MFA](mfa.md).

## SAML service provider

Use SAML 2.0 when required by the IdP or enterprise contract. In this flow the Spring application is the **service provider (SP) / relying party** and the customer's IdP is the **asserting party**. Prefer Spring Security's SAML support over hand-written XML or a legacy vendor adapter.

The implementation needs a `RelyingPartyRegistration` that binds the SP entity ID and assertion consumer service to the asserting party's entity ID, SSO endpoint, and signing certificates. Start from IdP metadata when available and publish or provide SP metadata for registration. Protect private signing/decryption keys and plan certificate rollover with the IdP owner.

For a basic Boot configuration, supply the IdP entity ID, SSO URL, and verification certificate, then enable SAML login:

```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          partner:
            assertingparty:
              entity-id: ${PARTNER_SAML_ENTITY_ID}
              sso-url: ${PARTNER_SAML_SSO_URL}
              verification:
                credentials:
                  - certificate-location: ${PARTNER_SAML_CERTIFICATE}
```

```java
http.saml2Login(Customizer.withDefaults());
```

Use Boot's SAML starter on Boot 4 (`spring-boot-starter-security-saml2`) or `spring-security-saml2-service-provider` on Boot 3.5, and follow the Boot version's build guide for the required Shibboleth repository.

For each SAML deployment, verify at minimum:

- The response signature and issuer are validated against the configured IdP; unsigned or untrusted assertions are rejected.
- Audience, destination, recipient, time conditions, and `InResponseTo` are checked.
- The assertion consumer URL, external scheme/host, entity IDs, and certificate fingerprints match the production deployment.
- IdP-initiated login is enabled only when required and is reviewed for login CSRF and unsolicited-response risks.
- Single logout behavior and certificate expiry/rotation are tested with the real IdP.

SAML deployments often need IdP-specific configuration. Treat metadata and certificates as inputs to verify, not as trusted merely because they were supplied by a customer.

## Multiple organizations and IdPs

### Runtime-managed client registrations

Keep a small, fixed provider set in Spring configuration when deployments own provider settings. Use database-managed registrations only when operators need to add or change providers at runtime; protect the management endpoints with a narrowly scoped administrator authority and audit changes without recording secrets.

Treat every saved registration as security-sensitive input. Allow only supported grant and client-authentication methods, validate required OIDC/OAuth fields, and restrict authorization, token, user-info, issuer, and JWK Set endpoints to HTTPS hosts from a server-controlled allow-list (with an explicit localhost exception for local development). Do not let an HTTP request choose an arbitrary issuer or callback URL. Network egress controls remain necessary because hostname checks alone do not prevent DNS changes or access to internal networks.

Encrypt client secrets at rest with a separately managed key, never return decrypted secrets in admin responses, and define how key rotation re-encrypts stored values. Keep the key outside source control and fail startup if a required key is missing or invalid.

For create operations, let a database uniqueness constraint decide races and translate a duplicate-key failure to a stable `409 Conflict`; a prior existence check is only a usability optimization. Do not use an upsert for create because concurrent requests can overwrite an existing provider. Keep update and delete behavior explicit; an update must not recreate a row deleted concurrently. Test the conflict path against the production database.

For a small fixed set of providers, configure a separate client registration for each provider and use an explicit `registrationId`. For per-customer SSO, resolve the tenant from a trusted pre-login signal (such as a verified organization slug or email domain policy), then select only that tenant's configured registration.

Keep tenant-to-registration mappings in server-controlled storage. Validate issuer and audience per registration, isolate account links and role mappings by tenant, and reject unknown tenants before redirecting to an IdP. Do not accept arbitrary issuer URLs or registration IDs from a request; that can turn the app into an open redirect or an attacker-selected token consumer. Define what happens when the same person authenticates through two tenants or two providers.

If users need to choose an organization before login, implement that as a small unauthenticated home-realm discovery step with allowlisted tenants and generic error responses. Do not infer a tenant from an unverified email claim before authentication. Prefer service-provider-initiated login. If IdP-initiated login is required, validate the unsolicited response with the same issuer, signature, audience, recipient, time, and tenant checks, and protect the post-login redirect from open redirects.

## Provisioning and deprovisioning

Use just-in-time provisioning for simple systems where account creation at first login is sufficient. Use SCIM or an equivalent trusted lifecycle integration when administrators must provision, update, or disable access without waiting for a user to log in. SCIM is a separate provisioning protocol, not part of the OIDC login callback; authenticate and authorize its endpoints independently, make updates idempotent, and define how soft deletion and reactivation work. Deprovisioning must disable local access and invalidate active sessions or other credentials according to policy; deleting the IdP link alone does not end an existing application session.

## Running your own authorization server

Prefer a managed identity provider when the requirement is simply “let users sign in” or “issue OAuth tokens.” Run Spring Authorization Server only when the product must own the issuer, client registry, consent, keys, token lifecycle, and operational response. It is a separate security product with its own persistence, key rotation, account recovery, abuse controls, and conformance testing. Do not mistake adding an authorization-server starter for completing those responsibilities.

## Session, logout, and security checks

- Preserve session fixation protection and rotate the application session after authentication.
- Let Spring validate the OIDC response and ID token. Bind the callback to the selected registration and validate the issuer, signature, audience, expiry, and nonce; validate `azp` when required for a token with multiple audiences. Use an explicit allowed signing-algorithm policy and reject issuer/registration mismatches, especially when several IdPs are configured.
- Keep the ID token for establishing the login identity. Do not use it as an API access token; APIs should validate access-token audience and scopes as a resource server.
- Decide whether local logout also triggers the provider's OIDC or SAML logout; clearing the local session does not necessarily end the IdP session.
- Protect cookie-based flows with CSRF controls. Configure CORS only for known browser origins; CORS is not authentication.
- Use a stable external identity key and explicit authority mapping; treat display name and email as profile data.
- Rate-limit account linking, login initiation, provisioning endpoints, and recovery flows.
- Do not log authorization codes, access/refresh tokens, ID tokens, assertions, client secrets, or session identifiers.
- Apply MFA and recent-authentication requirements to account linking, factor changes, and high-impact operations. See [MFA](mfa.md).

## Tests

Test the contract without relying on a live provider for every run:

- The login initiation and callback create an authenticated local session only for a valid response.
- Invalid `state`, nonce, signature, issuer, audience, redirect URI, expired response, and unknown external subject are rejected.
- Claim-to-authority mapping preserves least privilege and cannot grant access from an untrusted claim.
- Account linking never joins users based only on an unverified email address.
- Logout clears the local session and follows the documented IdP logout behavior.
- Tenant selection cannot resolve an unconfigured IdP or cross-link accounts between tenants.
- SAML rejects modified, unsigned, expired, replayed, wrong-audience, and unsolicited assertions unless the accepted flow explicitly allows them.
- Provisioning retries are idempotent, and deprovisioned users lose access according to the documented policy.

Use Spring Security's mock OIDC/SAML support for focused web tests and a local IdP in integration tests when protocol wiring or provider metadata must be exercised. A mock login tests authorization behavior, not the real redirect/callback protocol:

```java
@Test
void adminEndpointRequiresMappedAdminRole() throws Exception {
    mvc.perform(get("/api/admin").with(oidcLogin()
                    .authorities(new SimpleGrantedAuthority("ROLE_APP_ADMIN"))))
            .andExpect(status().isOk());

    mvc.perform(get("/api/admin").with(oidcLogin()))
            .andExpect(status().isForbidden());
}
```

Unit-test claim mapping separately; use a local IdP for redirect URI, nonce/state, metadata, and callback integration.

## References

- [Spring Security OAuth 2.0 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/)
- [Spring Security OAuth 2.0 Login advanced configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
- [Spring Security OAuth 2.0 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/)
- [Spring Security SAML 2.0 Login](https://docs.spring.io/spring-security/reference/servlet/saml2/login/)
- [Spring Security OAuth 2.0 Test Support](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html)
- [Spring Boot OAuth2](https://docs.spring.io/spring-boot/reference/security/oauth2.html)
- [Spring Boot SAML 2.0](https://docs.spring.io/spring-boot/reference/security/saml2.html)
- [Spring Boot 3.5 Security](https://docs.spring.io/spring-boot/3.5/reference/web/spring-security.html)
- [Spring Authorization Server](https://docs.spring.io/spring-authorization-server/reference/)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
- [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://www.rfc-editor.org/rfc/rfc9700.html)
- [System for Cross-domain Identity Management (RFC 7644)](https://www.rfc-editor.org/rfc/rfc7644.html)
