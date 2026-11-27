# OAuth2 Client

Use this guide for OIDC browser login or OAuth2 client credentials for
service-to-service calls. For inbound bearer-token validation, use
[Bearer Authentication](authentication.md#oauth2-resource-server-validation).

## Browser login

Add spring-boot-starter-security and
spring-boot-starter-security-oauth2-client. Let Boot manage their versions.
Use [OAuth2ClientSecurityConfig](../templates/OAuth2ClientSecurityConfig.java)
when a browser user signs in through an external identity provider and the
application establishes a session. Configure registration details and secrets
through externalized properties:

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
            scope: [openid, profile, email]
        provider:
          company:
            issuer-uri: ${OIDC_ISSUER_URI}
```

The template enables oauth2Login and logout. Spring Security owns
/oauth2/authorization/{registrationId} and
/login/oauth2/code/{registrationId}. Keep authorization redirects, callback
handling, code exchange, state/nonce checks, and logout out of MVC controllers.
Map the validated (iss, sub) pair to a local account with OidcUserService or
AuthenticationSuccessHandler. Treat email as profile data, and allowlist
claims before mapping them to local authorities. Preserve session fixation
protection and CSRF for browser login. Trust forwarded headers only from
configured proxies when TLS terminates upstream.

## Service credentials

Use a client_credentials registration when a backend calls another service as
itself. Configure its client ID, secret, token URI or issuer, and required
scopes in externalized properties. Connect an OAuth2AuthorizedClientManager to
the outbound HTTP client. Keep the service identity and requested scopes
narrow. Do not expose an endpoint that returns or forwards its access token.
An OAuth2 client registration does not configure inbound bearer-token
validation. Configure a resource-server chain for APIs that accept access
tokens.

## Verification

Test the actual filter chain with Spring Security's oauth2Login() or oidcLogin()
support. These helpers exercise authorization with a mock principal. Use a
local test provider when redirect URI, discovery, state, nonce, or callback
wiring must be verified. Test outbound token acquisition against a stub token
endpoint and assert that the outgoing request carries the expected bearer
token. Confirm no MVC mapping implements login initiation, callback, token
exchange, or logout.

[Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/) ·
[Spring Boot OAuth2 Client](https://docs.spring.io/spring-boot/reference/security/oauth2.html)
