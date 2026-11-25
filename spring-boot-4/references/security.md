# Spring Security

Read this when adding or changing servlet-based authentication,
authorization, HTTP Basic, form login, CSRF, CORS, sessions, or security
filter-chain behavior. For an OAuth2/OIDC browser client, also read
[oauth2-client.md](oauth2-client.md). For bearer-token APIs, also read
[oauth2-resource-server.md](oauth2-resource-server.md). For Spring Security's
factor authorities and MFA authorization, also read [mfa.md](mfa.md). The application
package and shared `config` location are in [architecture.md](architecture.md).

## Defaults

1. Add `spring-boot-starter-security` only when the application requires
   security. Boot manages its version. With the starter present, define an
   explicit `SecurityFilterChain`; do not rely on Boot's generated default
   user or default request policy.
2. Configure servlet security through Spring Security's filter mechanisms:
   `SecurityFilterChain`, `HttpSecurity`, and the built-in authentication
   filters. Use the Spring Security 7 lambda DSL. Do not use
   `WebSecurityConfigurerAdapter`, `.and()`, `authorizeRequests()`,
   `antMatchers()`, or `mvcMatchers()`.
3. Do not define authentication or protocol endpoints in `@Controller` or
   `@RestController`. This includes login processing, logout, OAuth2
   authorization initiation/callback, and bearer-token validation. Enable the
   relevant filter through the `HttpSecurity` DSL and use its standard paths.
   Do not create controller methods that authenticate credentials, exchange
   authorization codes, decode tokens, or implement logout. Login success,
   failure, and identity mapping belong in filter callbacks such as
   `AuthenticationSuccessHandler`, `AuthenticationFailureHandler`, and
   `OAuth2UserService`. Business API endpoints remain ordinary controllers
   and are protected by the filter chain or method security.
4. Put shared security beans and chain definitions in the application root
   `config` package. Keep authorization policy close to the use case when it
   needs trusted domain data; use `@EnableMethodSecurity` when using
   `@PreAuthorize` or related annotations.
5. Each request is handled by the first matching `SecurityFilterChain` only.
   Scope specialized chains with `securityMatcher`, order them from specific
   to general, and retain a final chain for all remaining application paths.
   Order `requestMatchers` from specific to general and put `anyRequest()`
   last. Never accidentally leave Actuator or application routes outside all
   chains.
6. Keep CSRF enabled for cookie/session authentication, including form login
   and OAuth2 login. Disable it only for a stateless API that accepts
   credentials exclusively from an explicit authorization header and does not
   authenticate with browser cookies. CORS is not a replacement for CSRF; use
   exact allowed origins and enable credentials only for a deliberate
   cookie-based cross-origin flow.
7. Choose the session policy from the authentication mechanism. Browser login
   uses Spring's session and session-fixation protections. A stateless bearer
   API can use `SessionCreationPolicy.STATELESS`. Do not apply stateless mode
   globally to an application that also has browser login.
8. Use Spring's standard DSL for HTTP Basic, form login, OAuth2 login, and
   resource-server authentication. Add a custom `Filter` only for a concrete
   requirement that Spring Security does not provide; register it at an
   explicit position with `addFilterBefore`, `addFilterAfter`, or
   `addFilterAt`. Do not replace an existing built-in filter accidentally.
   Use HTTP Basic only over TLS; Base64 encoding does not encrypt credentials.
9. Security failures occur before MVC exception advice. Configure
   `AuthenticationEntryPoint` for unauthenticated requests and
   `AccessDeniedHandler` for forbidden requests. For bearer APIs, configure
   the handlers inside the resource-server DSL as well as any general
   exception handling when needed. Reuse a shared response writer/factory so
   filter failures and MVC `ProblemDetail` errors follow the same public
   error schema, status fields, and safe detail policy; do not call
   `@RestControllerAdvice` from a filter. Browser login failures may redirect
   through a filter failure handler rather than return an API problem body.
   Preserve bearer `WWW-Authenticate` headers.
10. For password authentication, use a `PasswordEncoder`; never store,
    return, log, or compare raw passwords directly. Keep local demo accounts
    confined to a local/test profile and require their password through
    external configuration. Assign least privilege by default.

## Basic filter-chain example

This example delegates HTTP Basic authentication to Spring Security's filter
chain. It does not create a login controller or an application-owned login
URL:

```java
@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.build();
	}
}
```

Use the relevant mechanism reference to add form login, OAuth2 login, or
resource-server support. For an API-only Basic setup, define the intended
session and CSRF policy explicitly; do not disable CSRF merely because the
application returns JSON.

## Error response boundary

Use one API error schema for MVC failures and security failures, but keep the
handling at the boundary where each failure occurs. MVC validation and domain
errors are handled by the application's `@RestControllerAdvice`. Unauthenticated
and forbidden requests rejected in the security chain are handled by
`AuthenticationEntryPoint` and `AccessDeniedHandler`. Factor shared
`ProblemDetail` creation/serialization into a bean that both sides can use;
do not route filter exceptions through controllers or MVC advice. Use the
same content type and stable error fields, avoid exposing implementation
details, and retain `WWW-Authenticate` for HTTP Basic/Bearer challenges as
appropriate. OAuth2 browser login success/failure remains on filter handlers;
see [oauth2-client.md](oauth2-client.md).

## Execution context

The servlet `SecurityContext` is thread-bound. It does not follow a task onto
an executor automatically. Propagate it only when an in-process task must
perform authorization as the submitting user; use Spring Security's
`DelegatingSecurityContextAsyncTaskExecutor` or corresponding concurrency
wrapper. The wrapper must restore/clear context when the task ends. Never
enable inheritable thread-local mode as a thread-pool propagation shortcut.
For delayed or durable work, pass an immutable actor ID and required tenant
scope as explicit job data, then revalidate authorization when the job runs;
do not persist a live `SecurityContext`. See [async.md](async.md) for executor
configuration and thread-reuse tests.

## Testing

Add `spring-boot-starter-security-test` in test scope when using Spring
Security test support; Boot manages its version. Test the actual filter chain
with MockMvc or the project's MVC test client. Cover public and protected
routes, unauthenticated 401 behavior, authenticated success, denied 403
behavior, CSRF rejection for cookie-based writes, and the configured security
error body. Use `@WithMockUser`, `httpBasic()`, `csrf()`, or the OAuth2/JWT
post-processors rather than implementing test-only controller endpoints.
Focused tests must load the security configuration; a test setup that excludes
the filter chain does not verify request security.

## Verify

Inspect the dependencies, every `SecurityFilterChain`, matcher order, session
and CSRF policy, custom filters, and security error handlers. Search for
controller mappings of security protocol paths and remove them. Confirm every
application and Actuator path is intentionally matched by a chain, and test
both allowed and denied requests through the filter chain.

The runnable [security template](../assets/template-security/README.md)
contains separate ordered chains for HTTP Basic, a stateless resource server,
and browser authentication with OAuth2 login and MFA. Its
[filter-chain configuration](../assets/template-security/src/main/java/io/example/templatesecurity/config/SecurityConfiguration.java)
and [MockMvc tests](../assets/template-security/src/test/java/io/example/templatesecurity/SecurityIntegrationTests.java)
exercise these rules without adding security to the base `template-maven`
project.

## References

- [Spring Security Servlet Architecture](https://docs.spring.io/spring-security/reference/7.2/servlet/architecture.html)
- [Spring Security Authentication Architecture](https://docs.spring.io/spring-security/reference/7.2/servlet/authentication/architecture.html)
- [Spring Security Concurrency Support](https://docs.spring.io/spring-security/reference/7.2/servlet/integrations/concurrency.html)
- [Spring Security Testing](https://docs.spring.io/spring-security/reference/7.2/servlet/test/)
