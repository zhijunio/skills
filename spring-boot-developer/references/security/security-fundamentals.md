# Security Fundamentals

Boot 4 ships **Spring Security 7**. The lambda DSL is the only style - most Spring Security code an agent has seen will not compile.

Add `spring-boot-starter-security` only when the application needs authentication or authorization. Once present, Spring Security protects requests by default; define an explicit `SecurityFilterChain` for the application's public and protected routes. Select the authentication mechanism in the relevant guide: local accounts, self-issued JWTs, or an OAuth2 resource server.

| Removed in Security 7 | Replacement |
|---|---|
| `.and()` chaining | Lambda configuration |
| `authorizeRequests()` | `authorizeHttpRequests()` |
| `antMatchers()`, `mvcMatchers()` | `requestMatchers()` |
| `AntPathRequestMatcher`, `MvcRequestMatcher` | `PathPatternRequestMatcher` (what `requestMatchers` uses) |
| `WebSecurityConfigurerAdapter` | A `SecurityFilterChain` bean |
| `provider.setUserDetailsService(…)` | `new DaoAuthenticationProvider(userDetailsService)` |

## The filter chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemDetailEntryPoint())
                        .accessDeniedHandler(problemDetailAccessDeniedHandler()))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

- **`securityMatcher` scopes the chain.** Without it this chain claims every request, including the actuator and any UI.
- This is a chain skeleton. Configure the authentication mechanism required by the application; see [JWT authentication](jwt-authentication.md) or [OAuth2 resource server](oauth2-resource-server.md) for bearer-token APIs.
- Disable CSRF and use `STATELESS` only for a stateless token API. Cookie and session authentication need CSRF protection.
- **Order matters** inside `authorizeHttpRequests` - the first matching rule wins, so `anyRequest()` is always last.
- **`PasswordEncoderFactories.createDelegatingPasswordEncoder()`** rather than `new BCryptPasswordEncoder(12)`: it stores an algorithm prefix (`{bcrypt}…`) so you can migrate hashes later. A bare encoder locks you in.

### Multiple chains

Order them from most specific to least:

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().hasRole("OPS"))
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

The first chain whose `securityMatcher` matches handles the request; the rest are never consulted. A chain with no matcher matches everything, so it must be last.

## Use form login for browser sessions

For a server-rendered browser application, keep CSRF enabled and let Spring Security manage the session. Do not set `SessionCreationPolicy.STATELESS` for this flow:

```java
@Bean
SecurityFilterChain browserChain(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/assets/**").permitAll()
                    .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults())
            .build();
}
```

Spring Security's default form handles `/login` and `/logout`; supply a custom login page only when the application owns that UI. Session fixation protection is enabled by default. Add a session concurrency limit only when the product has a defined policy for simultaneous logins.

Use in-memory accounts only for local development or tests. Keep the password outside committed configuration:

```java
@Bean
@Profile("local")
UserDetailsService localUsers(
        PasswordEncoder passwordEncoder,
        @Value("${app.security.dev-password}") String password) {
    var user = User.withUsername("developer")
            .password(passwordEncoder.encode(password))
            .roles("USER")
            .build();
    return new InMemoryUserDetailsManager(user);
}
```

Set `app.security.dev-password` from a developer-only environment variable. Do not provide a shared default credential.

## Authentication is global, authorization belongs to the feature

The failure mode is a `SecurityConfig` that lists every URL in the application, which every feature change has to edit and nobody can read.

| Concern | Where |
|---|---|
| Filter chain skeleton, session policy, CSRF | Global `SecurityConfig` |
| Password encoder, JWT decoder, `UserDetailsService` | Global `SecurityConfig` |
| 401 and 403 responses | Global `SecurityConfig` |
| Coarse public/authenticated split | Global `SecurityConfig` |
| **Who may do what** | The feature, via `@PreAuthorize` |
| A whole URL space with different rules | The feature's own `SecurityFilterChain` with an `@Order` |

See [code-organization.md](../architecture/code-organization.md).

## Method security

```java
@Service
@Transactional(readOnly = true)
public class OrderService {

    @PreAuthorize("hasRole('ADMIN')")
    public void cancelAny(OrderId id) { … }

    @PreAuthorize("hasAuthority('SCOPE_orders:write')")
    public OrderId place(PlaceOrderCmd cmd) { … }

    @PreAuthorize("@orderAccess.isOwner(#id, authentication)")
    public OrderResponse getById(OrderId id) { … }
}
```

Requires `@EnableMethodSecurity`. Without it, `@PreAuthorize` is silently inert - and unlike a missing cache annotation, that is a security hole.

`hasRole('ADMIN')` checks the authority `ROLE_ADMIN`; the prefix is added for you. `hasAuthority('ROLE_ADMIN')` is the same check written out. Scopes have no prefix added, so an OAuth2 scope is `hasAuthority('SCOPE_orders:read')` - see [oauth2-resource-server.md](oauth2-resource-server.md).

For ownership rules, delegate to a bean rather than writing a long SpEL expression:

```java
@Component("orderAccess")
class OrderAccessRules {

    private final OrderRepository orders;

    boolean isOwner(OrderId id, Authentication authentication) {
        return orders.existsByIdAndCustomerId(id, CustomerId.of(authentication.getName()));
    }
}
```

SpEL is not type-checked or refactor-safe. A parameter rename breaks `#id` silently at runtime.

`@PreAuthorize` is proxy-based, so a self-invoked call **skips the check entirely** - see [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md). Of all the self-invocation failures, this is the one that matters most.

Avoid `@PostAuthorize` where you can: it runs the method, then decides. On a read that means the data was loaded; on anything with a side effect, it happened.

## JSON 401 and 403

Out of the box an unauthenticated API request gets an empty 401, or a redirect to a login page. Neither is usable by a client. And **`@RestControllerAdvice` cannot help** - security filters run before the `DispatcherServlet`, so nothing thrown there reaches your handler ([error-handling.md](../web/error-handling.md)).

Write the `ProblemDetail` from the entry points:

```java
private AuthenticationEntryPoint problemDetailEntryPoint() {
    return (request, response, ex) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
            "Unauthorized", "Authentication is required to access this resource.");
}

private AccessDeniedHandler problemDetailAccessDeniedHandler() {
    return (request, response, ex) -> writeProblem(response, HttpStatus.FORBIDDEN,
            "Forbidden", "You do not have permission to perform this action.");
}

private void writeProblem(HttpServletResponse response, HttpStatus status,
                          String title, String detail) throws IOException {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    jsonMapper.writeValue(response.getOutputStream(), problem);
}
```

Keep the messages generic. "No user with that email" tells an attacker which addresses are registered.

## Passwords

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

- Never store, log or return a password, hashed or not.
- Never write your own hashing. BCrypt, SCrypt and Argon2 are the options the factory already knows.
- Compare with `passwordEncoder.matches(raw, stored)`, never with `equals`.
- On a failed login, do the hash comparison anyway against a dummy value. Returning early on "no such user" leaks which accounts exist through response timing.

For username/password authentication, use a repository-backed `UserDetailsService` with `DaoAuthenticationProvider`; keep the persisted account model separate from Spring Security's `UserDetails` adapter. In-memory users are for local development or tests only. Register accounts with the configured `PasswordEncoder`, assign the least privilege by default, and enforce username/email uniqueness with a database constraint rather than a check-then-insert alone.

`UserDetailsService` looks up an account by username; it does not authenticate the submitted password. It returns a `UserDetails` principal containing the encoded password, granted authorities, and account status. `DaoAuthenticationProvider` compares the submitted password through the configured `PasswordEncoder` and creates the authenticated principal:

```java
@Service
class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    AccountDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return User.withUsername(account.username())
                .password(account.passwordHash())
                .authorities(account.authorities())
                .disabled(!account.enabled())
                .build();
    }
}
```

Use a custom `UserDetails` implementation when the authenticated principal must carry a stable account ID or other security-relevant state. Keep it as an adapter over the domain account; do not make the persistence entity implement `UserDetails` by default.

Password recovery must not reveal whether an account exists. Generate a cryptographically random, single-use reset token, store only its hash and expiry, and invalidate it after use. Keep reset responses generic and rate-limit login and recovery endpoints at a shared boundary; do not trust `X-Forwarded-For` unless a trusted proxy has set it. See [rate limiting](../integration/rate-limiting.md).

## Headers, CORS and CSRF

Boot sets sensible defaults - `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control` on secured responses. Add HSTS in production if you terminate TLS:

```java
.headers(headers -> headers
        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000).includeSubDomains(true))
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
```

CORS goes in one place, and **never `allowedOrigins("*")` with credentials** - the browser rejects the combination, and if you work around it you have disabled the same-origin policy:

```java
@Bean
CorsConfigurationSource corsConfigurationSource(WebProperties properties) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(properties.allowedOrigins());   // explicit, from configuration
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Then `.cors(Customizer.withDefaults())` on the chain. Do not scatter `@CrossOrigin`.

**CSRF**: disable it only for a stateless token API. Cookie-based session authentication needs it on.

## Actuator

Actuator endpoints are not secured by default beyond what you configure, and several are dangerous. Give them their own chain (above) and expose deliberately - see [observability.md](../observability/observability.md).

## Testing

```java
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)              // slices do NOT pick up your config automatically
class OrderControllerSecurityTest {

    @Autowired MockMvcTester mockMvc;
    @MockitoBean OrderService orderService;

    @Test
    void rejectsAnonymous() {
        assertThat(mockMvc.get().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsWrongRole() {
        assertThat(mockMvc.delete().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdmin() {
        assertThat(mockMvc.delete().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
    }
}
```

Requires the security test support on the classpath. Test the **negative** cases - a test suite that only proves the happy path passes just as well when the rules are missing entirely.

For `@PreAuthorize` on services, `@WithMockUser` works in a sliced `@SpringBootTest` too. See [testing-slices-web.md](../testing/testing-slices-web.md).

## If on Boot 3.5.x

Spring Security **6**, which is a real difference. The lambda DSL is the recommended style and works identically, but the removed APIs in the table above still *exist* on 6 (deprecated), so 3.5.x code may compile with `.and()` and `antMatchers()`. Write the lambda form on both lines and the upgrade is free.

`DaoAuthenticationProvider` still has the `setUserDetailsService` setter on 6. `ProblemDetail`, `@EnableMethodSecurity` and `@WithMockUser` are unchanged.

## Gotchas

- Agent writes `http.csrf().disable().and()...` - removed in Security 7; lambda DSL only
- Agent writes `antMatchers()` or `authorizeRequests()` - `requestMatchers()` and `authorizeHttpRequests()`
- Agent extends `WebSecurityConfigurerAdapter` - declare a `SecurityFilterChain` bean
- Agent calls `provider.setUserDetailsService(...)` on Security 7 - pass it to the constructor
- Agent omits `@EnableMethodSecurity` - `@PreAuthorize` is silently inert, which is a security hole
- Agent calls a `@PreAuthorize` method on `this` - the check is skipped entirely
- Agent omits `securityMatcher` on one of several chains - it claims every request and the later chains never run
- Agent puts `anyRequest()` before a specific matcher - first match wins; specific rules go first
- Agent disables CSRF on a session-based application - that is a real vulnerability
- Agent skips `exceptionHandling` - clients get empty 401/403 bodies, and advice cannot catch filter exceptions
- Agent uses `hasAuthority('ADMIN')` for a role - roles carry the `ROLE_` prefix; use `hasRole('ADMIN')`
- Agent uses `hasRole` for an OAuth2 scope - scopes are `hasAuthority('SCOPE_...')`
- Agent writes long SpEL in `@PreAuthorize` - delegate to a named bean; SpEL is not refactor-safe
- Agent uses `@PostAuthorize` on a mutating method - it already ran
- Agent returns "no user with that email" - leaks which accounts exist; keep messages generic
- Agent uses `new BCryptPasswordEncoder()` directly - use the delegating encoder so hashes can be migrated
- Agent sets `allowedOrigins("*")` with `allowCredentials(true)` - the browser rejects it; list origins explicitly
- Agent tests only the happy path - assert the 401 and the 403
- Agent writes a `@WebMvcTest` without `@Import(SecurityConfig.class)` - slices do not load it, so the test runs against Boot's defaults

## Related

- [jwt-authentication.md](jwt-authentication.md) · [oauth2-resource-server.md](oauth2-resource-server.md) · [rate limiting](../integration/rate-limiting.md) · [error-handling.md](../web/error-handling.md) · [observability.md](../observability/observability.md) · [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md) · [testing-slices-web.md](../testing/testing-slices-web.md)

## References

- [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html)
- [UserDetails](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details.html)
- [Form Login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html)
- [Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
