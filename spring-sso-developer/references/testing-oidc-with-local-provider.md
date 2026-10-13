# Testing OIDC with a Local Provider

Use a local OIDC provider when a test needs to exercise discovery, authorization-code login, token exchange, issuer
validation, or JWKS signature verification without depending on a customer identity provider. Choose Dex for a
standalone, configurable IdP with upstream connector options; choose Rose Authorization Server for a Spring-oriented
test server with controlled users, clients, claims, and a Testcontainers wrapper. Neither substitutes for testing
provider-specific behavior against a customer's IdP.

## Choose the test boundary

- For controller and method-security tests, use Spring Security's `oidcLogin()` or `jwt()` test support. These are
  fast and deterministic, but they do not run discovery, redirect handling, token exchange, or cryptographic checks.
- For a focused OIDC integration test, start Dex or Rose Authorization Server and exercise a real browser
  authorization-code flow through the application callback. This covers the application's OIDC wiring and token
  validation.
- For customer-provider compatibility, keep a separate smoke or acceptance test against that provider's sandbox.
  A local provider cannot reproduce its claim mapping, consent screens, MFA, group limits, logout behavior, or policy.

Both options in this guide implement OIDC. They do not provide a SAML identity provider for testing Spring's SAML
service-provider flow.

## Rose Authorization Server with Testcontainers

[Rose Authorization Server](https://github.com/zhijun-io/rose-authorization-server) is a local OIDC server intended
for development and tests, not production. Its Testcontainers wrapper exposes a mapped issuer URI, readiness probe,
default client constants, custom configuration mounting, and TLS/mTLS setup. The server can configure test users and
custom ID-token claims such as roles, which is useful for exercising the application's real claim-to-authority mapping.

Use the published `io.zhijun.rose:rose-authorization-server` artifact in test scope, plus the Testcontainers JUnit
integration. The released `0.1.0` artifact is built with Spring Boot 4.1.1 and Testcontainers 2; the project
repository may be ahead of that release. For Boot 4 projects, use the Testcontainers 2 JUnit artifact shown below.
Boot 3.5 projects using Testcontainers 1 use `org.testcontainers:junit-jupiter`, but compatibility of the
published Rose wrapper with Testcontainers 1 is not established; use Dex or verify the wrapper against that
dependency line before relying on it. The Rose artifact makes Testcontainers optional, so the consuming project
must provide its own test dependency. Check the repository's release page before pinning a newer version.

```xml
<dependency>
    <groupId>io.zhijun.rose</groupId>
    <artifactId>rose-authorization-server</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

The application needs Spring Security's OAuth2 client support in addition to the test dependencies. This example
verifies Spring can create a client registration from the container's dynamic issuer; a resource-server-only
application does not create a `ClientRegistrationRepository`.

```java
import io.zhijun.rose.authorization.testcontainers.RoseAuthorizationServerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class OidcClientIntegrationTest {

    @Container
    static final RoseAuthorizationServerContainer authServer = new RoseAuthorizationServerContainer();

    @DynamicPropertySource
    static void oauth2Properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.client.provider.rose-authorization-server.issuer-uri",
                authServer::getIssuerUri);
        registry.add(
                "spring.security.oauth2.client.registration.rose-authorization-server.provider",
                () -> "rose-authorization-server");
        registry.add(
                "spring.security.oauth2.client.registration.rose-authorization-server.client-id",
                () -> RoseAuthorizationServerContainer.DEFAULT_CLIENT_ID);
        registry.add(
                "spring.security.oauth2.client.registration.rose-authorization-server.client-secret",
                () -> RoseAuthorizationServerContainer.DEFAULT_CLIENT_SECRET);
        registry.add(
                "spring.security.oauth2.client.registration.rose-authorization-server.authorization-grant-type",
                () -> "authorization_code");
        registry.add(
                "spring.security.oauth2.client.registration.rose-authorization-server.scope",
                () -> "openid,profile,email");
    }

    @Test
    void createsClientRegistrationFromIssuerDiscovery(
            @Autowired ClientRegistrationRepository registrations) {
        var registration = registrations.findByRegistrationId("rose-authorization-server");

        assertThat(registration).isNotNull();
        assertThat(registration.getProviderDetails().getIssuerUri())
                .isEqualTo(authServer.getIssuerUri());
    }
}
```

The wrapper's default client credentials and provider ID are available as constants; the client registration must
also request the `openid` scope. For a complete browser login test, use the default callback registered by the
fixture or mount test configuration with the exact callback URI. For resource-server tests, configure the issuer
from `authServer.getIssuerUri()` and keep the real decoder so issuer and JWKS checks run. Its user attributes can
exercise application-specific ID-token claim and role mapping without modifying the production IdP configuration.

The wrapper defaults to the server's published `latest` image. For reproducible CI, pass an explicit image tag or
digest using the wrapper's image constructor and update it deliberately. Do not reuse the server's development
credentials, keys, or permissive redirect defaults outside tests.

## Dex with Docker Compose

Use Dex when tests benefit from an independently configurable IdP or when the fixture needs its local connector or
other Dex connector behavior. Dex's local connector and static users are configured in a separate file, so keep the
credentials disposable and confined to test/local environments.

Keep a test-only Dex configuration and Compose file in `src/test/oidc/`:

```yaml
issuer: http://localhost:5556/dex

storage:
  type: memory

web:
  http: 0.0.0.0:5556

oauth2:
  skipApprovalScreen: true

staticClients:
  - id: spring-sso-test
    secret: spring-sso-test-secret
    name: Spring SSO integration test
    redirectURIs:
      - http://localhost:8080/login/oauth2/code/dex

enablePasswordDB: true
staticPasswords:
  - email: developer@example.test
    username: developer
    userID: 00000000-0000-4000-8000-000000000001
    # Dex's documented bcrypt hash for the test password "password".
    hash: "$2a$10$2b2cU8CPhOTaGrs1HRQuAueS7JTT5ZHsHSzYiFPm1leZck7Mc8T4W"
```

Use that fixture only for disposable local/CI tests. Replace the known password hash if the fixture leaves the test
environment; never reuse it for a shared or deployed provider.

```yaml
services:
  dex:
    image: ghcr.io/dexidp/dex:v2.45.1
    command: ["dex", "serve", "/etc/dex/config.yaml"]
    ports:
      - "5556:5556"
    volumes:
      - ./dex-config.yaml:/etc/dex/config.yaml:ro
```

Pin the Dex image to the version used by the test environment and update it deliberately. The example uses
`v2.45.1`; check the [Dex releases](https://github.com/dexidp/dex/releases) before selecting a newer tag. Start it before the
integration test with `docker compose -f src/test/oidc/compose.yaml up -d` and stop it afterward with
`docker compose -f src/test/oidc/compose.yaml down`; do not rely on a developer's already-running IdP. The test should
poll the discovery URL before starting the application context:

```text
http://localhost:5556/dex/.well-known/openid-configuration
```

Use the `issuer` returned by discovery verbatim. Here it is `http://localhost:5556/dex`; do not substitute a container
hostname, trailing slash, or reconstructed URL in only one side of the test.

## Configure the Spring test client

For an OAuth2 Login application using the Dex fixture, configure a test profile with the same issuer and registered
client. Use browser automation or an equivalent browser-capable harness to visit a protected page, follow the redirect
to Dex, submit the fixture user's credentials, and return through the application's callback:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          dex:
            provider: dex
            client-id: spring-sso-test
            client-secret: spring-sso-test-secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile, email
        provider:
          dex:
            issuer-uri: http://localhost:5556/dex
```

Keep the application reachable at `http://localhost:8080` for this fixture, or change the Dex client's registered
redirect URI to the exact test callback. If the application runs in a container, solve host and container DNS so the
browser and application both reach Dex through the same canonical issuer URL; OIDC issuer comparison is exact.

For Rose Authorization Server, use its `rose-authorization-server` provider ID and default client credentials from
the server's client configuration output or container constants. Its default test client accepts the callback at
`http://localhost:8080/login/oauth2/code/rose-authorization-server`; use that callback or provide a matching
test-only client configuration.

For a resource-server test, set `spring.security.oauth2.resourceserver.jwt.issuer-uri` to the same issuer and send a
real access token obtained through the configured test flow. Do not replace `JwtDecoder` in this test: the point is to
exercise discovery, JWKS retrieval, issuer validation, and signature verification. Keep separate unit and MVC tests
that mock authentication so most tests remain independent of Docker.

## Keep the test meaningful

- Use Authorization Code with PKCE for a public client and Authorization Code with a test-only client secret for a
  confidential server-side client. Do not enable or use the Resource Owner Password Credentials grant for browser
  login tests.
- Assert the post-login principal and the exact claims and authorities the application consumes, then assert the
  expected access decision. A successful redirect alone does not prove role mapping.
- Add negative cases for an unauthenticated request, a token with the wrong issuer or audience, and missing required
  authorities when those checks are part of the application configuration.
- Exercise logout separately. Local logout and provider logout are distinct behaviors.
- Keep fixture credentials, signing keys, and secrets test-only. Never point the test profile at production.

## Version notes

The examples use the current Spring Security OAuth2 client property model and Dex v2 configuration format. Spring
Security 7 on Boot 4 and Spring Security 6.5 on Boot 3.5 both support `issuer-uri` discovery; confirm the exact
dependency and property names against the application's managed versions before adapting the fixture. Dex is
independent of the Spring Boot version.

## Gotchas

- Agent tests every OIDC path with `oidcLogin()` and claims it verified discovery or token signatures; mock login does
  not contact an identity provider
- Agent replaces the real `JwtDecoder` in the test intended to verify resource-server validation
- Agent changes `localhost` to a Compose service name in only one place; the configured issuer and token `iss` must
  match exactly
- Agent uses Dex to claim compatibility with a customer IdP; Dex does not reproduce vendor-specific policies or
  claims
- Agent uses a password grant to simplify a browser login test; test the authorization-code redirect and callback
- Agent uses a development password, client secret, or signing key outside the disposable test profile

## Related

- [spring-security-oidc.md](spring-security-oidc.md) · [security-oauth2-oidc.md](security-oauth2-oidc.md) ·
  [security-hardening.md](security-hardening.md)

## References

- [Rose Authorization Server](https://github.com/zhijun-io/rose-authorization-server)
- [Rose Authorization Server on Maven Central](https://central.sonatype.com/artifact/io.zhijun.rose/rose-authorization-server)
- [Dex built-in local connector](https://dexidp.io/docs/connectors/local/)
- [Dex configuration reference](https://github.com/dexidp/dex/blob/master/config.yaml.dist)
- [Dex releases](https://github.com/dexidp/dex/releases)
- [Spring Security OAuth2 test support](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html)
