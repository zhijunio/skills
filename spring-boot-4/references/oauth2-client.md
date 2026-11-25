# OAuth2 Client

Read this for external OAuth2/OIDC login or for outbound requests that need
tokens from an authorization server. Also read [security.md](security.md) for
the shared servlet filter-chain, CSRF, session, and testing rules. This guide
covers OAuth2 client behavior only; accepting bearer tokens on an API is a
resource-server role covered by [oauth2-resource-server.md](oauth2-resource-server.md).

## Select the client flow

- Use OIDC login when a browser user signs in through an external identity
  provider and the application establishes its own authenticated session.
- Use an OAuth2 client with the `client_credentials` grant for a backend
  service calling another protected service as itself. This does not log in
  the current application user.
- Use an OAuth2 resource server when this application accepts bearer tokens
  on its own API. Do not configure an OAuth2 client as a substitute for token
  validation.

Prefer OIDC when the application needs a standardized user identity. Before
implementation, confirm the issuer/discovery metadata, redirect URI, required
claims/scopes, account-linking policy, and provider logout behavior. Do not
guess provider endpoints or claim names.

## Browser login

Add `spring-boot-starter-security` and
`spring-boot-starter-security-oauth2-client`; Boot manages their versions.
Declare each registration in `application.yaml` or a profile-specific file,
and supply client credentials and issuer values from environment-backed
configuration or a secret manager:

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
            scope:
              - openid
              - profile
              - email
        provider:
          company:
            issuer-uri: ${OIDC_ISSUER_URI}
```

Use YAML lists for scopes. Prefer issuer discovery. Configure explicit
authorization, token, user-info, and JWK endpoints only when the provider
metadata requires it. Never commit client secrets or use a shared default
credential.

Enable the login flow in `SecurityFilterChain`:

```java
@Bean
SecurityFilterChain browserSecurity(HttpSecurity http) throws Exception {
	return http
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/", "/assets/**").permitAll()
					.anyRequest().authenticated())
			.oauth2Login(Customizer.withDefaults())
			.logout(Customizer.withDefaults())
			.build();
}
```

Spring Security owns the protocol endpoints through filters:

- Start login by navigating the browser to
  `/oauth2/authorization/{registrationId}`.
- The default callback is `/login/oauth2/code/{registrationId}`.
- Form login processing and logout are also handled by Spring Security when
  those filters are enabled.

Do not map these paths in a controller, and do not implement authorization
redirects, `state`/`nonce` handling, code exchange, token validation, or logout
in application code. If a custom login screen is needed, use the security
login-page configuration and serve the UI separately; leave the form
processing and OAuth2 callback to the filters. Preserve session and CSRF
protection for browser login. When TLS terminates at a proxy, trust forwarded
headers only from configured proxies so the generated redirect URI uses the
correct public scheme and host.

## Identity mapping

The external identity must be mapped to an application account and its local
authorities. For OIDC, use the validated issuer plus subject (`iss` + `sub`)
as the external identity key. Treat email as profile data; do not link accounts
solely because email values match. Map only allowlisted provider claims to
local roles. Do not assume that a provider's `roles` or `groups` claim has the
same meaning as an application authority.

Use `OAuth2UserService`/`OidcUserService` for identity loading and mapping, or
an `AuthenticationSuccessHandler` for post-authentication navigation or
response behavior. Keep token handling and account policy out of controllers.
Do not log authorization codes, ID/access/refresh tokens, client secrets, or
session identifiers.

## Outbound client credentials

For service-to-service calls, configure a client registration with the
`client_credentials` grant and use Spring Security's
`OAuth2AuthorizedClientManager` integration with the outbound HTTP client.
Keep the client secret externalized, request only required scopes, and define
which service identity is used. Do not expose a controller endpoint that
returns or forwards an access token.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          inventory-service:
            provider: partner
            client-id: ${PARTNER_CLIENT_ID}
            client-secret: ${PARTNER_CLIENT_SECRET}
            authorization-grant-type: client_credentials
            scope:
              - inventory.read
        provider:
          partner:
            token-uri: ${PARTNER_TOKEN_URI}
```

## Testing

Add `spring-boot-starter-security-oauth2-client-test` in test scope when using
OAuth2 client test support. Test access decisions with Spring Security's
`oidcLogin()` or `oauth2Login()` post-processors while keeping the real filter
chain enabled. These helpers establish a mock authenticated principal; they do
not verify the provider redirect/callback protocol. Use a local test IdP or a
focused protocol integration test when redirect URI, discovery, state, nonce,
or callback wiring must be verified. Use `oauth2Client()` or a stubbed token
response to test outbound authorized-client wiring. Assert that controller
mappings do not implement the security initiation or callback endpoints.

## Verify

Check the registration properties, active profile, secret source, redirect
URI, `oauth2Login()` filter configuration, session/CSRF policy, identity
mapping, and security tests. Confirm the initiation and callback paths are
handled by Spring Security and have no controller mappings. Confirm API token
validation is configured as a resource server rather than inferred from an
OAuth2 client registration.

The runnable [security template](../assets/template-security/README.md)
shows OIDC client configuration, filter-owned login routes, and a MockMvc
`oidcLogin()` test. It also configures a `client_credentials` registration and
an application-scoped [RestClient integration](../assets/template-security/src/main/java/io/example/templatesecurity/config/PartnerClientConfiguration.java)
used by [PartnerInventoryClient](../assets/template-security/src/main/java/io/example/templatesecurity/integration/PartnerInventoryClient.java).
The integration test stubs the token and resource endpoints locally and asserts
the grant request and outgoing bearer token. See its
[chain configuration](../assets/template-security/src/main/java/io/example/templatesecurity/config/SecurityConfiguration.java),
[properties](../assets/template-security/src/main/resources/application.yaml),
and [tests](../assets/template-security/src/test/java/io/example/templatesecurity/SecurityIntegrationTests.java).

## References

- [Spring Boot OAuth2 Client](https://docs.spring.io/spring-boot/4.2/reference/security/oauth2.html)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/7.2/servlet/oauth2/login/)
- [Spring Security OAuth2 Login Advanced Configuration](https://docs.spring.io/spring-security/reference/7.2/servlet/oauth2/login/advanced.html)
- [Spring Security OAuth2 Test Support](https://docs.spring.io/spring-security/reference/7.2/servlet/test/mockmvc/oauth2.html)
