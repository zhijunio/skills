# Bearer Authentication

Choose issuance and validation independently. Add issuance when the application
owns login or refresh. Use the resource server when the API relies on a trusted
issuer and decoder, including when the issuer belongs to the same system.

## Related authentication flows

Use [OAuth2 Client](oauth2-client.md) for external OIDC login and outbound
service credentials. Use [Multi-Factor Authentication](mfa.md) for factor
verification and step-up authorization.

## First-party issuance and refresh

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

### Authentication and token endpoints

Do not create login, token, refresh, logout, or token-validation endpoints in
MVC controllers. Configure Spring Security's authentication filters and
protocol support through HttpSecurity.

When the application must act as an OAuth2 authorization server, use Spring
Authorization Server and its protocol endpoints. For an application-specific
credential flow, authenticate through AuthenticationManager from a security
filter and issue tokens in its success handler. Keep credential verification,
refresh-token rotation, replay detection, and failure handling out of MVC
controllers. A registration or account-management API is a business endpoint,
but it must delegate password handling to configured authentication services
and never issue credentials by bypassing that boundary.

[SecurityConfig](../templates/SecurityConfig.java) configures a stateless
resource-server chain and Nimbus encoder/decoder beans.
[JwtFactory](../templates/JwtFactory.java) creates typed access and refresh
tokens after authentication and carries the authenticated authorities into
the signed token. The dedicated decoder accepts only access tokens on API
routes. The authority converter maps the trusted signed claim to Spring
authorities, so keep token issuance restricted to authenticated callers.

### Token settings

```yaml
app:
  jwt:
    secret: ${JWT_SECRET} # min 256-bit base64 encoded key
    access-token-expiration: 900000   # 15 minutes
    refresh-token-expiration: 604800000 # 7 days
```

The configuration creates Spring Security's Nimbus JWT encoder and decoder
at startup. Supply a Base64 encoded HMAC key with at least 256 bits and
positive token lifetimes in milliseconds. The factory creates refresh tokens. The application must accept only validated `type=refresh` tokens
in its refresh flow and implement one-time rotation, revocation, and reuse
detection, for example using a hashed token-family record.
Use storage suited to the client and protect cookies if they carry credentials.

Check login, access-token use, account state changes, and the application's
actual refresh flow. Include rejection of refresh tokens on resource routes
and handling of account lookup failures.

[Security configuration template](../templates/SecurityConfig.java) ·
[Bad configuration](../examples/legacy-security-config.java)

## OAuth2 resource-server validation

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

### Filter chain and authority mapping

Start from [ResourceServerConfig](../templates/ResourceServerConfig.java)
for a servlet bearer API. Adapt routes and CORS origins to the deployment.
It wires the shared [SecurityErrorHandler](../templates/SecurityErrorHandler.java)
for filter-level responses. Follow the [caller rules](../SKILL.md#2-establish-the-caller) for
credential transport, chain coverage, and error responses.

The template uses Spring Security's default JWT converter. It maps `scope` or
`scp` to `SCOPE_*`. Use `hasAuthority("SCOPE_orders:read")` for scope checks.
When a policy requires provider-specific roles such as Keycloak's
`realm_access.roles`, add a JWT authentication converter that retains the
default scope authorities and maps the provider's roles to explicit authorities.

### Issuer and audience

Configure the trusted issuer and required API audience. Set a JWK Set URI
explicitly when the deployment needs it, and keep issuer validation enabled.
The following are separate provider examples.

```yaml
# Keycloak
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/realms/my-realm
          jwk-set-uri: https://keycloak.example.com/realms/my-realm/protocol/openid-connect/certs
          audiences: my-api
```

```yaml
# Auth0
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-domain.auth0.com/
          audiences: https://your-api.example.com
```

Exercise the configured decoder with signed tokens and verify issuer,
audience, expiry, and scope mapping. Verify local account lookup on the actual
security chain.

[Good configuration](../examples/resource-server-config.java) ·
[Bad configuration](../examples/resource-server-security-misconfigured.java) ·
[Spring Security JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

## Verified caller identity

[JwtClaimExtractor](../templates/JwtClaimExtractor.java)
shows how to read profile claims from the authenticated context. Its role
accessor exposes raw claim values for display. Apply authorization to the
authorities produced by the authentication converter.
Provider subjects are opaque strings.

Use the exact `(iss, sub)` pair as the external identity key, with a database unique
constraint on that pair. Email and username are optional, mutable profile attributes,
not account lookup or automatic account-linking keys, even when email is verified.
The [ExternalIdentity template](../templates/ExternalIdentity.java) accepts an already
verified access token; signature, issuer, audience and expiry validation must happen first.
Allow only configured issuers; never discover an arbitrary issuer URL from an untrusted token.
For existing subject-only mappings, backfill the known issuer before introducing a second issuer.
Account linking requires an explicit authenticated flow proving control of both accounts.

See [OpenID claim stability](https://openid.net/specs/openid-connect-core-1_0.html#ClaimStability).

```java
// Excerpt from a controller with constructor-injected services.
@RestController
public class OrderController {

    @GetMapping("/api/v1/orders/my")
    public List<OrderResponse> myOrders(
        @AuthenticationPrincipal Jwt jwt  // inject JWT directly
    ) {
        ExternalIdentity identity = ExternalIdentity.from(jwt);
        return orderService.findByOwner(identity.issuer(), identity.subject());
    }

    // Or with JwtAuthenticationToken for full principal
    @GetMapping("/api/v1/profile")
    public ProfileResponse profile(JwtAuthenticationToken token) {
        ExternalIdentity identity = ExternalIdentity.from(token.getToken());
        return userService.findByExternalIdentity(identity.issuer(), identity.subject());
    }
}
```
