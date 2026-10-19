# JWT Authentication

## First: should you issue tokens at all?

When a managed identity provider meets the requirements, use it and make your application a resource server ([oauth2-resource-server.md](oauth2-resource-server.md)). Self-issuing JWTs is reasonable when the application has a concrete need to own local authentication and can operate the associated key, token, revocation, and recovery lifecycle.

Self-issuing means owning key management, rotation, revocation, refresh-token reuse detection and account recovery. It is the right answer for a self-contained application with local accounts and no IdP, and the wrong answer whenever an IdP is on the table.

If login requires multiple factors, complete the MFA flow before issuing a normal access JWT. See [MFA](mfa.md) for browser/session and stateless REST flows and factor claims.

## Use Spring Security's own JWT support

For a standard Spring application, start with Spring Security's encoder, decoder, and bearer-token filter rather than parsing tokens in a custom `OncePerRequestFilter`. This avoids common validation and security-context errors. Use an existing or alternative library only when a concrete requirement is not met by Spring Security, and keep token validation inside a well-tested authentication integration rather than handwritten request parsing.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

It brings the Nimbus JOSE support that `JwtEncoder` and `JwtDecoder` need.

## Keys

Prefer **RSA or EC** over a shared secret. With asymmetric keys the private key never leaves the issuer, and you can publish a JWKS endpoint so other services validate without holding a secret.

```java
@Bean
JwtDecoder jwtDecoder(RSAKey signingKey, JwtProperties properties) {
    var decoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
    decoder.setJwtValidator(validatorsFor(properties)); // issuer, audience, and any required session checks
    return decoder;
}
```

Provide a matching `JwtEncoder` from the configured signing key when issuing tokens. `validatorsFor` represents the application's composed issuer, audience, and optional session validators; its details depend on the token contract.

```java
@Validated
@ConfigurationProperties("app.jwt")
record JwtProperties(@NotNull Resource publicKey,
                     @NotNull Resource privateKey,
                     @NotBlank String keyId,
                     @NotBlank String issuer,
                     @NotBlank String audience,
                     @NotNull Duration accessTokenTtl,
                     @NotNull Duration refreshTokenTtl) {
}
```

Load keys through a maintained JOSE/key-management library or the platform's secret integration. Verify the public/private pair at startup. Publish only public JWK material; during rotation, publish active and retiring public keys together until old tokens expire. Never serialize a private `RSAKey`:

```java
return Map.of("keys", signingKeys.stream()
        .map(RSAKey::toPublicJWK)
        .map(JWK::toJSONObject)
        .toList());
```

This is the response mapping only; key loading, endpoint wiring, and rotation storage depend on the selected key-management setup.

The properties are resource references:

```yaml
app:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}
    private-key: ${JWT_PRIVATE_KEY}
    key-id: ${JWT_KEY_ID}
    issuer: ${JWT_ISSUER}
    audience: ${JWT_AUDIENCE}
    access-token-ttl: ${JWT_ACCESS_TOKEN_TTL}
    refresh-token-ttl: ${JWT_REFRESH_TOKEN_TTL}
```

Keys come from the environment or a secret manager - never the repository, never `application.yml`. These values are `Resource` locations. A value with no protocol, including one that starts with `/`, is loaded from the classpath. Use `classpath:/keys/public.pem` for a local classpath key and `file:/run/secrets/jwt-private.pem` for a filesystem path. See [configuration.md](../configuration/configuration.md).

The `keyID` goes in the token header as `kid`, letting verifiers select the matching public key. During rotation, sign with the active key and publish the retiring public key until its tokens expire; local decoders must accept both keys during that window. The example uses RSA; EC requires a matching key type and signature algorithm.

For HMAC (a single application, no other verifiers), use `NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` and `NimbusJwtDecoder.withSecretKey(secretKey)`. HS256 requires a key of at least 256 bits - a short secret throws at startup, which is the correct behaviour.

## Issuing the access token

### Token response contract

For token endpoints, use the OAuth 2.0 token response field names and meanings: `access_token`, `token_type` (normally `Bearer`), and `expires_in` as the access token lifetime in seconds. Include `refresh_token` when the application supports refresh tokens. Send `Cache-Control: no-store` and `Pragma: no-cache` on token responses so clients and intermediaries do not store credentials.

Application-specific metadata such as `session_id` or `refresh_expires_in` may be added as documented extension fields. Omit refresh-token fields when refresh tokens are not supported. A custom username/password login endpoint that returns this shape is not, by itself, a complete OAuth 2.0 authorization server; use Spring Authorization Server or an external identity provider when OAuth client interoperability or authorization-server flows are required.

For username/password login, authenticate through an `AuthenticationManager` backed by `DaoAuthenticationProvider`. A controller/service is sufficient for a small API; use `AuthenticationFilter` and its converter/handlers when filter lifecycle integration is required. Build tokens from the authenticated principal, never from the submitted username. Give refresh its own validation path, and use a logout handler when logout must revoke server-side state. Keep access-token validation in the resource-server support. Avoid custom filters that only relocate simple controller logic. Never include credentials or tokens in events or logs.

The issuer should build claims from authenticated server-side state, not raw request fields. Its signing step should include the configured algorithm and key id, plus required issuer, subject, audience, issued-at, and expiry claims; add session or scope claims only when the application uses them.

- **Configure the access-token lifetime for the deployment's revocation policy.** Without a session-state check, a JWT remains usable until expiry. Set the lifetime from the acceptable exposure window, client refresh behavior, and availability requirements; there is no universal duration. Use a session-state check when revocation must take effect before expiry.
- **`subject` is the stable user id**, not the email. Emails change.
- **Never put anything secret in the payload.** A JWT is signed, not encrypted - anyone holding it can read every claim.
- **Keep it small.** The token travels on every request; a list of fifty permissions in it is a permanent bandwidth cost.
- Include `aud` and validate it alongside `iss` when the API has a defined audience.
- Include a `sid` when access tokens belong to revocable sessions. The stable session ID lets the resource server check that the session remains active.
- Include `jti` only when individual-token revocation or audit correlation is needed.

## Validating: no custom filter

Point the resource-server configuration at your decoder and Spring Security's `BearerTokenAuthenticationFilter` does the rest:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/login", "/api/auth/registrations", "/.well-known/jwks.json").permitAll()
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(Customizer.withDefaults())
        .authenticationEntryPoint(problemHandler)
        .accessDeniedHandler(problemHandler));
```

Add stateless session and CSRF settings only when the API authenticates exclusively with bearer credentials. Configure the resource-server handlers as shown because its bearer filter has its own defaults. `/api/auth/**` is a useful grouping convention, not an anonymous-access rule: permit only the specific public endpoints, and authorize refresh, logout, revocation, and account operations according to their credentials and policy. Applications may organize these routes differently; keep route rules explicit and aligned with the established API.

### Immediate session revocation

Short-lived self-contained JWTs remain usable until expiry after logout or session revocation. If the product requires immediate invalidation, include a `sid` claim and add an `OAuth2TokenValidator<Jwt>` that checks the session ID, subject ownership, expiry and revocation state against the session store on every bearer request. Revoke the session and its refresh tokens together. This adds a store lookup to each authenticated request; keep it uncached when revocation must take effect on the next request, and fail closed if the store cannot be checked.

Distinguish an inactive or revoked session from an unavailable session store. The former is an invalid credential (`401`); when Redis or another required shared-state store cannot be checked, fail closed with `503 Service Unavailable` rather than treating the token as valid or misreporting it as an invalid credential. Apply the same distinction to login, refresh and logout operations that depend on that store. Return a stable problem response and keep infrastructure details out of the response.

The validator performs the session lookup through a repository and rejects missing IDs as invalid tokens:

```java
@Override
public OAuth2TokenValidatorResult validate(Jwt jwt) {
    String sessionId = jwt.getClaimAsString("sid");
    String userId = jwt.getSubject();
    if (sessionId == null || sessionId.isBlank() || userId == null || userId.isBlank()) {
        return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
    return sessions.isActive(sessionId, userId, Instant.now())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID_SESSION);
}
```

When the product needs session management, keep one session row per login and link each rotated refresh-token row to it. Store metadata such as `user_agent`, `ip_address`, `created_at`, `last_refreshed_at`, `expires_at` and `revoked_at` at the session level. Treat IP and user-agent as untrusted audit/display metadata, not device identity. A refresh-token replay revokes the associated session and all tokens in that session.

Reading the caller in a controller (for example, under a `/api/me` route group):

```java
@GetMapping("/api/me")
public UserResponse me(Authentication authentication) {
    String userId = authentication.getName();
    return users.findById(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
}
```

## Refresh tokens

**Default to an opaque, stored refresh token.** It makes revocation and reuse detection straightforward. A self-contained refresh JWT needs equivalent server-side state or another explicit revocation and replay strategy; use that design only when the architecture justifies the added complexity. In either case, rotate on use, detect reuse, and revoke the affected token family according to policy. Opaque tokens should be unpredictable (for example, at least 256 bits of entropy) and the stored verifier should be the source of truth.

Hash the opaque value before storage by default. Because the value is high entropy, a fast cryptographic hash such as SHA-256 is appropriate; password hashing is not needed. If a requirement explicitly calls for storing the raw token, preserve that requirement rather than silently hashing it, and document that a database or backup leak exposes immediately usable credentials. Restrict database and backup access accordingly.

Implement rotation as one transaction: lock the owning session, reject missing or expired credentials, detect replay of a revoked token and revoke its token family, then revoke the presented token and persist its replacement. Return generic authentication errors and test concurrent refresh attempts against the production database.

- **Rotate on every use** and revoke the previous one.
- **Reuse of a revoked token means compromise** - revoke the whole family, forcing re-authentication.
- **Choose and document storage.** Hash by default; raw storage requires an explicit requirement and documented database-leak impact.
- Generate with `SecureRandom`, at least 256 bits, Base64-URL encoded.

## Where the client keeps tokens

| Storage | Verdict |
|---|---|
| `HttpOnly`, `Secure` cookie | Often a good browser choice; select `SameSite` for the login flow and retain CSRF protection for cookie-authenticated writes |
| In-memory browser storage | Limits persistence of an access token but requires a deliberate refresh and reload experience |
| `localStorage` | Exposes tokens to scripts running on the origin; generally avoid for credentials, and document the XSS trade-off if an existing client requires it |

Prefer a backend-for-frontend or in-memory access-token handling when it fits the client architecture. Do not treat a client-side route guard as authorization.

For browser clients that refresh access tokens, coordinate concurrent 401 responses through one in-flight refresh operation, then retry each eligible request at most once. Clear the shared refresh operation after it settles so a later expiry can start a new refresh. Do not refresh on 403, and do not blindly replay a write unless the endpoint is idempotent or the client knows the original request had no side effect; see the [idempotency contract](../web/rest-controllers.md#idempotency-for-retried-commands). Cookie-based refresh also requires a deliberate CSRF policy; route guards improve navigation but never replace server authorization. For server-rendered clients, keep per-request authentication state isolated and account for cookie forwarding and hydration.

## Logout and revocation

A JWT stays valid until it expires unless the decoder checks session state. With the stateless TTL model, "logout" means:

1. Revoke the refresh token (server-side, effective immediately).
2. Discard the access token client-side.
3. Accept that the access token remains technically valid until expiry; choose its lifetime according to the application's exposure and revocation policy.

A logout filter can call a `LogoutHandler` that revokes the session and its refresh tokens. The `sid` validator then rejects that access token on the next request. A `jti` denylist is another option when individual token revocation is required. Choose a per-request lookup only when the revocation requirement justifies its latency and availability cost.

## Testing

Do not generate real tokens in tests. Use the JWT post-processor:

```java
@Test
void allowsCallerWithScope() {
    String userId = "user-42";
    mockMvc.perform(get("/api/orders")
            .with(jwt().jwt(builder -> builder.subject(userId))
                       .authorities(new SimpleGrantedAuthority("SCOPE_orders:read"))))
            .andExpect(status().isOk());
}
```

Test that an **expired** token gives 401 and that a token missing the scope gives 403. Those are the paths that break silently.
Also test that active sessions are accepted, while revoked, expired or subject-mismatched sessions are rejected; after revocation, the next request must fail. Unit-test the session validator and use a PostgreSQL integration test for its query and constraints.

## Gotchas

- Agent adds a custom token parser without a requirement - Spring Security's encoder, decoder and bearer filter cover the standard flow
- Agent lets a parsing exception escape a custom filter - malformed credentials can become a 500 instead of a 401
- Agent hardcodes the signing secret - environment or secret manager only
- Agent uses a short HMAC secret - HS256 needs ≥256 bits and throws otherwise
- Agent uses HMAC when other services must verify - use RSA/EC and publish a JWKS
- Agent omits `kid` - key rotation becomes a flag-day outage
- Agent puts sensitive data in claims - a JWT is signed, not encrypted
- Agent hardcodes an access-token lifetime without matching the application's risk and revocation needs - document the chosen lifetime and keep it configurable
- Agent uses the email as `subject` - use a stable id
- Agent issues a refresh token without a revocation and reuse-detection strategy - choose a token format the system can revoke and rotate safely
- Agent does not rotate refresh tokens - a leaked token is then valid until expiry
- Agent stores a raw refresh token without an explicit requirement or explaining database-leak impact - hash it by default
- Agent ignores refresh-token reuse - reuse means compromise; revoke the family
- Agent accepts a refresh token at the bearer endpoint - separate the two token types
- Agent stores credentials where same-origin scripts can read them without assessing the XSS exposure
- Agent claims logout invalidates access tokens without checking revocation state on bearer requests
- Agent tests with real generated tokens - use `jwt()` post-processor; test 401 and 403 explicitly

## Related

- [security-fundamentals.md](security-fundamentals.md) · [mfa.md](mfa.md) · [oauth2-resource-server.md](oauth2-resource-server.md) · [configuration.md](../configuration/configuration.md) · [error-handling.md](../web/error-handling.md) · [caching.md](../persistence/caching.md)

## References

- [Spring Security: JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [RFC 7519: JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519)
