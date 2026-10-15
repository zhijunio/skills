# JWT Authentication

## First: should you issue tokens at all?

If an identity provider is available - Keycloak, Auth0, Okta, Entra, Cognito - use it and make your application a resource server ([oauth2-resource-server.md](oauth2-resource-server.md)). You then write no token code at all.

Self-issuing means owning key management, rotation, revocation, refresh-token reuse detection and account recovery. It is the right answer for a self-contained application with local accounts and no IdP, and the wrong answer whenever an IdP is on the table.

If login requires multiple factors, complete the MFA flow before issuing a normal access JWT. See [MFA](mfa.md) for browser/session and stateless REST flows, factor claims, and Boot 3.5 versus Boot 4 guidance.

## Use Spring Security's own JWT support

Do **not** add a third-party JWT library and hand-write an `OncePerRequestFilter`. Spring Security already has an encoder, a decoder and a bearer-token filter. Using them removes the dependency, the custom filter, and the entire class of bugs that comes with parsing tokens by hand - expired-token exceptions escaping the filter as a 500, forgetting to clear the security context, accepting a refresh token as an access token.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

That name is Boot 4; on 3.5.x it is `spring-boot-starter-oauth2-resource-server`. It brings the Nimbus JOSE support that `JwtEncoder` and `JwtDecoder` need.

## Keys

Prefer **RSA or EC** over a shared secret. With asymmetric keys the private key never leaves the issuer, and you can publish a JWKS endpoint so other services validate without holding a secret.

```java
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class JwtKeyConfig {

    @Bean
    RSAKey jwtRsaKey(JwtProperties properties) {
        return Jwks.loadRsaKey(properties);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey jwtRsaKey) {
        return Jwks.source(jwtRsaKey);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtRsaKey, JwtProperties properties,
                          SessionJwtValidator sessionValidator) {
        var decoder = NimbusJwtDecoder.withPublicKey(jwtRsaKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                audienceValidator, sessionValidator));
        return decoder;
    }
}
```

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

Use a small `Jwks` utility to read PKCS#8/X.509 PEM resources, verify that the configured public and private keys match, and create a signing source. Its public-set method strips the private key material; expose only that result from the JWKS endpoint:

```java
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class Jwks {
    private static final String KEY_PAIR_VERIFICATION_MESSAGE = "jwt-key-pair-check";

    private Jwks() {
    }

    public static RSAKey loadRsaKey(JwtProperties properties) {
        try {
            RSAPublicKey publicKey = readPublicKey(properties.publicKey());
            RSAPrivateKey privateKey = readPrivateKey(properties.privateKey());
            verifyKeyPair(publicKey, privateKey);
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.keyId())
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load RSA JWT keys", exception);
        }
    }

    public static JWKSource<SecurityContext> source(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    public static Map<String, Object> publicSet(List<RSAKey> rsaKeys) {
        return Map.of("keys", rsaKeys.stream()
                .map(key -> key.toPublicJWK().toJSONObject())
                .toList());
    }

    private static RSAPublicKey readPublicKey(Resource resource) throws Exception {
        byte[] bytes = decodePem(resource, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static RSAPrivateKey readPrivateKey(Resource resource) throws Exception {
        byte[] bytes = decodePem(resource, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static void verifyKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) throws Exception {
        byte[] challenge = KEY_PAIR_VERIFICATION_MESSAGE.getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(challenge);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(challenge);
        if (!verifier.verify(signer.sign())) {
            throw new IllegalArgumentException("Configured RSA public and private keys do not match");
        }
    }

    private static byte[] decodePem(Resource resource, String header, String footer) throws IOException {
        String pem;
        try (var input = resource.getInputStream()) {
            pem = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String encoded = pem.replace(header, "").replace(footer, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
```

Serve the public set from a public endpoint. Never serialize the `RSAKey` that contains the private key. Supplying a list also allows a rotation window to publish the active and retiring public keys:

```java
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
class JwksController {

    private final List<RSAKey> publishedKeys;

    JwksController(List<RSAKey> publishedKeys) {
        this.publishedKeys = publishedKeys;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> jwks() {
        return Jwks.publicSet(publishedKeys);
    }
}
```

The properties are resource references:

```yaml
app:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}     # classpath:/keys/public.pem in local dev
    private-key: ${JWT_PRIVATE_KEY}
    key-id: ${JWT_KEY_ID}
    issuer: ${JWT_ISSUER}
    audience: ${JWT_AUDIENCE}
    access-token-ttl: 15m
    refresh-token-ttl: 30d
```

Keys come from the environment or a secret manager - never the repository, never `application.yml`. See [configuration.md](../configuration/configuration.md).

The `keyID` matters: it goes in the token header as `kid`, which lets verifiers choose the matching public key. During rotation, sign new tokens with one active private key and publish both its public key and the retiring public key until all tokens signed by the old key have expired. Configure any local decoder to accept both keys during that window; the single-key decoder above is the starting configuration and needs a key-set-based validator for rollover. The `Jwks` loader shown here handles RSA; EC keys need an `ECKey` loader and a matching signature algorithm.

For HMAC (a single application, no other verifiers), use `NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` and `NimbusJwtDecoder.withSecretKey(secretKey)`. HS256 requires a key of at least 256 bits - a short secret throws at startup, which is the correct behaviour.

## Issuing the access token

### Token response contract

For token endpoints, use the OAuth 2.0 token response field names and meanings: `access_token`, `token_type` (normally `Bearer`), and `expires_in` as the access token lifetime in seconds. Include `refresh_token` when the application supports refresh tokens. Send `Cache-Control: no-store` and `Pragma: no-cache` on token responses so clients and intermediaries do not store credentials.

Application-specific metadata such as `session_id` or `refresh_expires_in` may be added as documented extension fields. Omit refresh-token fields when refresh tokens are not supported. A custom username/password login endpoint that returns this shape is not, by itself, a complete OAuth 2.0 authorization server; use Spring Authorization Server or an external identity provider when OAuth client interoperability or authorization-server flows are required.

For username/password login, the filter converts and validates the request, then delegates credential verification to an `AuthenticationManager` backed by `DaoAuthenticationProvider`. Keep the filter focused on request matching and wiring. Put response behavior in injectable `AuthenticationSuccessHandler` and `AuthenticationFailureHandler` classes when it may grow or needs independent testing. The success handler reads the authenticated `SecurityUser` principal and asks `RefreshTokenService` to create a session and token pair. Do not issue a token directly from the submitted username:

```java
public class LoginAuthenticationFilter extends AuthenticationFilter {

    public LoginAuthenticationFilter(AuthenticationManager authenticationManager,
                                     ApiAuthenticationConverter converter,
                                     LoginAuthenticationSuccessHandler successHandler,
                                     LoginAuthenticationFailureHandler failureHandler) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "POST".equals(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath()));
        setSuccessHandler(successHandler);
        setFailureHandler(failureHandler);
    }
}
```

An API success handler must stop after writing its response. `AuthenticationSuccessHandler`'s default overload that receives a `FilterChain` delegates to the three-argument method and then continues the chain. Override that overload in the concrete handler and do not call `chain.doFilter`:

```java
@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokens;
    private final JsonMapper jsonMapper;

    public LoginAuthenticationSuccessHandler(RefreshTokenService refreshTokens, JsonMapper jsonMapper) {
        this.refreshTokens = refreshTokens;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        var tokens = refreshTokens.createForUser(user.id(),
                request.getHeader("User-Agent"), request.getRemoteAddr());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        jsonMapper.writeValue(response.getOutputStream(), tokens);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain, Authentication authentication)
            throws IOException, ServletException {
        onAuthenticationSuccess(request, response, authentication);
    }
}
```

Implement the failure handler separately as well when its response contract or observability behavior may expand. Keep credentials and internal exception messages out of the response.

`ApiAuthenticationConverter` deserializes and validates the login request with `JsonMapper` and Bean Validation, then returns an unauthenticated `UsernamePasswordAuthenticationToken`. The configured `DaoAuthenticationProvider` verifies the password and returns `SecurityUser`. The filter's failure handler maps authentication failures to a generic 401 response.

### Put login, refresh and logout in the security flow when required

For a small local-account API, a controller/service may handle login. When the requirement is to use Spring Security's authentication lifecycle and filters, build credential endpoints from Spring Security components:

- Use `AuthenticationFilter` for login and refresh, with an `AuthenticationConverter`, `AuthenticationManager` and success/failure handlers.
- Use an `AuthenticationProvider` for refresh-token validation and rotation. Keep token generation and persistence in a service.
- Configure logout with Spring Security's logout filter and a `LogoutHandler` that revokes the refresh-token session.
- Keep the resource-server `BearerTokenAuthenticationFilter` for access JWTs. Do not replace it with a hand-written `OncePerRequestFilter` that parses JWTs.

This separates credential authentication from bearer-token validation while keeping login, refresh and logout in the filter chain. Do not add custom filters merely to move a simple endpoint without a security-flow requirement. Publish authentication outcomes through Spring Security's `AuthenticationEventPublisher`; publish session lifecycle events after the transaction commits. Never include credentials or tokens in events or logs.

```java
@Component
public class JwtTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public AccessToken issue(String userId, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId)
                .audience(List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("sid", sessionId)
                .claim("scope", "USER")
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
```

- **Short TTL** - 15 minutes or less. A JWT cannot be revoked, so its lifetime *is* your revocation window.
- **`subject` is the stable user id**, not the email. Emails change.
- **Never put anything secret in the payload.** A JWT is signed, not encrypted - anyone holding it can read every claim.
- **Keep it small.** The token travels on every request; a list of fifty permissions in it is a permanent bandwidth cost.
- Include `aud` and validate it alongside `iss` when the API has a defined audience.
- Include a `sid` when access tokens belong to revocable sessions. The stable session ID lets the resource server check that the session remains active.
- Include `jti` only when individual-token revocation or audit correlation is needed.

## Validating: no custom filter

Point the resource-server configuration at your decoder and Spring Security's `BearerTokenAuthenticationFilter` does the rest:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Qualifier("loginAuthenticationManager") AuthenticationManager loginManager,
        @Qualifier("refreshAuthenticationManager") AuthenticationManager refreshManager,
        ApiAuthenticationConverter converter,
        RefreshTokenLogoutHandler logoutHandler,
        SecurityProblemDetailHandler problemDetailHandler,
        LoginAuthenticationSuccessHandler loginSuccessHandler,
        LoginAuthenticationFailureHandler loginFailureHandler,
        RefreshAuthenticationSuccessHandler refreshSuccessHandler,
        RefreshAuthenticationFailureHandler refreshFailureHandler) throws Exception {
    var loginFilter = new LoginAuthenticationFilter(
            loginManager, converter, loginSuccessHandler, loginFailureHandler);
    var refreshFilter = new RefreshAuthenticationFilter(
            refreshManager, converter, refreshSuccessHandler, refreshFailureHandler);
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                            "/api/auth/logout", "/.well-known/jwks.json", "/error").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(problemDetailHandler)
                    .accessDeniedHandler(problemDetailHandler))
            .logout(logout -> logout
                    .logoutRequestMatcher(request -> "DELETE".equals(request.getMethod())
                            && "/api/auth/logout".equals(request.getServletPath()))
                    .addLogoutHandler(logoutHandler)
                    .logoutSuccessHandler((request, response, authentication) -> {
                        if (request.getAttribute(RefreshTokenLogoutHandler.INVALID_REQUEST_ATTRIBUTE) == null
                                && !response.isCommitted()) {
                            response.setStatus(HttpStatus.NO_CONTENT.value());
                        }
                    }))
            .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(refreshFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

This application-wide API chain is intentionally catch-all. Signature, expiry and issuer are validated by the decoder; `audienceValidator` checks the configured audience and `SessionJwtValidator` checks the `sid` against the session store. Invalid bearer tokens reach the configured entry point as 401 responses. Scope claims map to `SCOPE_*` authorities automatically - see [oauth2-resource-server.md](oauth2-resource-server.md) for custom claim mapping.

### Immediate session revocation

Short-lived self-contained JWTs remain usable until expiry after logout or session revocation. If the product requires immediate invalidation, include a `sid` claim and add an `OAuth2TokenValidator<Jwt>` that checks the session ID, subject ownership, expiry and revocation state against the session store on every bearer request. Revoke the session and its refresh tokens together. This adds a store lookup to each authenticated request; keep it uncached when revocation must take effect on the next request, and fail closed if the store cannot be checked.

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

Reading the caller in a controller:

```java
@GetMapping("/me")
public UserResponse me(Authentication authentication) {
    String userId = authentication.getName();
    return users.findById(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
}
```

## Refresh tokens

**A refresh token should not be a JWT.** It is long-lived, must be revocable, and must support reuse detection - all things a stateless signed token cannot do. Use an opaque random value with at least 256 bits of entropy, rotate it on every use, and treat the database row as the source of truth.

Hash the opaque value before storage by default. Because the value is high entropy, a fast cryptographic hash such as SHA-256 is appropriate; password hashing is not needed. If a requirement explicitly calls for storing the raw token, preserve that requirement rather than silently hashing it, and document that a database or backup leak exposes immediately usable credentials. Restrict database and backup access accordingly.

When raw storage is an explicit requirement, lock the active session before the presented token, then revoke the old token, extend the session, and insert the replacement in one transaction:

```java
@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
public RefreshTokenRotation rotate(String presentedToken) {
    Instant now = Instant.now();
    if (!tokens.lockActiveSession(presentedToken, now)) {
        throw new InvalidRefreshTokenException();
    }
    RefreshToken current = tokens.findForUpdate(presentedToken)
            .orElseThrow(InvalidRefreshTokenException::new);

    if (current.revokedAt() != null) {
        tokens.revokeSession(current.sessionId(), now);
        sessions.revoke(current.sessionId(), current.userId(), now);
        events.publishEvent(new AuthLifecycleEvent("refresh-token-reuse-detected", current.userId(), now));
        throw new InvalidRefreshTokenException();
    }
    if (!current.expiresAt().isAfter(now)) {
        tokens.revoke(presentedToken, now);
        throw new InvalidRefreshTokenException();
    }

    tokens.revoke(presentedToken, now);
    String nextToken = newToken();
    Instant nextExpiry = now.plus(properties.refreshTokenTtl());
    sessions.touch(current.sessionId(), now, nextExpiry);
    tokens.insert(nextToken, current.sessionId(), current.userId(), nextExpiry);
    events.publishEvent(new AuthLifecycleEvent("refresh-token-rotated", current.userId(), now));
    return new RefreshTokenRotation(current.userId(),
            response(current.userId(), current.sessionId(), nextToken, nextExpiry));
}
```

- **Rotate on every use** and revoke the previous one.
- **Reuse of a revoked token means compromise** - revoke the whole family, forcing re-authentication.
- **Choose and document storage.** Hash by default; raw storage requires an explicit requirement and documented database-leak impact.
- Generate with `SecureRandom`, at least 256 bits, Base64-URL encoded.

## Where the client keeps tokens

| Storage | Verdict |
|---|---|
| `httpOnly`, `Secure`, `SameSite=Strict` cookie | Best. JavaScript cannot read it, so XSS cannot steal it - but you must then handle CSRF |
| In-memory (a JS variable) | Acceptable for the access token; lost on refresh, which the refresh token covers |
| `localStorage` | Readable by any script on the page. One XSS is total account compromise |

Do not suggest `localStorage`, even in an example.

For browser clients that refresh access tokens, coordinate concurrent 401 responses through one in-flight refresh operation, then retry each eligible request at most once. Clear the shared refresh operation after it settles so a later expiry can start a new refresh. Do not refresh on 403, and do not blindly replay a write unless the endpoint is idempotent or the client knows the original request had no side effect; see [api-idempotency.md](../web/api-idempotency.md). Cookie-based refresh also requires a deliberate CSRF policy; route guards improve navigation but never replace server authorization. For server-rendered clients, keep per-request authentication state isolated and account for cookie forwarding and hydration.

## Logout and revocation

A JWT stays valid until it expires unless the decoder checks session state. With the stateless TTL model, "logout" means:

1. Revoke the refresh token (server-side, effective immediately).
2. Discard the access token client-side.
3. Accept that the access token remains technically valid until expiry - which is why the TTL is short.

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

## If on Boot 3.5.x

Starter name is `spring-boot-starter-oauth2-resource-server` (no `security-` prefix). `JwtEncoder`, `NimbusJwtEncoder`, `JwtDecoder`, `JwtClaimsSet` and `oauth2ResourceServer(…)` are all present in Spring Security 6 and behave identically. The DSL removals in [security-fundamentals.md](security-fundamentals.md) are the only real difference.

## Gotchas

- Agent adds a third-party JWT library and writes a custom `OncePerRequestFilter` - Spring Security has an encoder, a decoder and a bearer filter already
- Agent lets a parsing exception escape a custom filter - expired token becomes a 500 instead of a 401; another reason not to write the filter
- Agent hardcodes the signing secret - environment or secret manager only
- Agent uses a short HMAC secret - HS256 needs ≥256 bits and throws otherwise
- Agent uses HMAC when other services must verify - use RSA/EC and publish a JWKS
- Agent omits `kid` - key rotation becomes a flag-day outage
- Agent puts sensitive data in claims - a JWT is signed, not encrypted
- Agent gives the access token a multi-hour TTL - a JWT cannot be revoked; the TTL is the revocation window
- Agent uses the email as `subject` - use a stable id
- Agent issues a JWT as the refresh token - it must be revocable; use an opaque stored value
- Agent does not rotate refresh tokens - a leaked token is then valid until expiry
- Agent stores a raw refresh token without an explicit requirement or explaining database-leak impact - hash it by default
- Agent ignores refresh-token reuse - reuse means compromise; revoke the family
- Agent accepts a refresh token at the bearer endpoint - separate the two token types
- Agent suggests `localStorage` - one XSS is full account compromise
- Agent claims logout invalidates access tokens without checking revocation state on bearer requests
- Agent tests with real generated tokens - use `jwt()` post-processor; test 401 and 403 explicitly

## Related

- [security-fundamentals.md](security-fundamentals.md) · [mfa.md](mfa.md) · [oauth2-resource-server.md](oauth2-resource-server.md) · [configuration.md](../configuration/configuration.md) · [error-handling.md](../web/error-handling.md) · [caching.md](../persistence/caching.md)

## References

- [Spring Security: JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [RFC 7519: JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519)
