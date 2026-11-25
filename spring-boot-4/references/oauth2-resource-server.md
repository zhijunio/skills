# OAuth2 Resource Server

Read this when an application accepts bearer access tokens on its own API.
Also read [security.md](security.md) for shared filter-chain, authorization,
CSRF, error-response, and test rules. Use
[oauth2-client.md](oauth2-client.md) when the application signs users in
through an external provider or calls another service using OAuth2.

## Configure token validation

Add `spring-boot-starter-security` and
`spring-boot-starter-security-oauth2-resource-server`; let Spring Boot manage
their versions. Prefer issuer discovery and configure an audience when the
identity provider issues tokens for multiple APIs:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}
          audiences:
            - ${OAUTH2_AUDIENCE}
```

If discovery cannot be used, configure a JWK Set URI and explicit issuer
validation. Signature validation alone does not establish the expected
issuer. Do not hardcode keys or use an unverified issuer from the incoming
token to select a decoder. For opaque tokens, configure the introspection
endpoint and client credentials through externalized settings.

## Use the filter chain

Configure the API as a resource server. Spring Security's bearer filter
extracts and validates the token before request authorization:

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
	return http
			.securityMatcher("/api/**")
			.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(HttpMethod.GET, "/api/orders/**")
					.hasAuthority("SCOPE_orders:read")
					.requestMatchers("/api/orders/**")
					.hasAuthority("SCOPE_orders:write")
					.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2
					.jwt(Customizer.withDefaults()))
			.build();
}
```

Disable CSRF only if the chain accepts bearer credentials exclusively through
an authorization header and does not accept browser cookies. Keep a separate
session-based chain when the application also has browser login; order the
chains from most specific to least specific and ensure a final chain covers
all remaining paths.

Do not implement `/token/validate`, a custom bearer-token controller, or a
controller-based token decoder. Business API controllers may read the
authenticated principal when needed, but keep `Jwt` and provider claims out of
the service layer; pass stable application identifiers and authorized values
instead.

## Authorities and ownership

Spring maps token scopes to authorities prefixed with `SCOPE_` by default. For
example, `orders:read` becomes `SCOPE_orders:read`. Keep scopes separate from
application roles (`ROLE_*`). When provider roles must be mapped, preserve the
default scope authorities and allowlist the claim paths and values. Do not
grant roles from arbitrary claims or treat a read scope as ownership of every
row.

Enforce row ownership or tenant access using trusted identity and application
data, usually at the use-case boundary. Never derive tenant identity from an
unchecked request header or query parameter. For multiple issuers, use an
explicit issuer allowlist.

## Errors and tests

Return consistent 401 and 403 responses using the resource-server DSL's
`authenticationEntryPoint` and `accessDeniedHandler` when customizing errors.
Preserve the `WWW-Authenticate: Bearer` challenge on 401 responses. MVC
exception advice does not handle failures produced before `DispatcherServlet`.

Add `spring-boot-starter-security-oauth2-resource-server-test` in test scope
for focused resource-server tests. With MockMvc, use `jwt()` or
`opaqueToken()` request post-processors to test authorization; these tests do
not validate real signatures or issuer discovery. Test missing, invalid, and
insufficient-scope behavior. Add an integration test for issuer/JWK or
introspection wiring when that runtime contract is part of the task.

## Verify

Inspect the starter, issuer/audience configuration, each applicable filter
chain, authority mapping, 401/403 handlers, and the denied/allowed request
tests. Confirm every API path is covered by a chain and that no controller
duplicates token authentication or validation.

The runnable [security template](../assets/template-security/README.md)
contains a stateless JWT resource-server chain and MockMvc tests for missing
credentials, read scope, and insufficient scope. See its
[chain configuration](../assets/template-security/src/main/java/io/example/templatesecurity/config/SecurityConfiguration.java),
[externalized issuer/JWK settings](../assets/template-security/src/main/resources/application.yaml),
and [tests](../assets/template-security/src/test/java/io/example/templatesecurity/SecurityIntegrationTests.java).

## References

- [Spring Boot OAuth2 Resource Server](https://docs.spring.io/spring-boot/4.2/reference/security/oauth2.html)
- [Spring Security OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/7.2/servlet/oauth2/resource-server/jwt.html)
- [Spring Security OAuth2 Resource Server Opaque Tokens](https://docs.spring.io/spring-security/reference/7.2/servlet/oauth2/resource-server/opaque-token.html)
- [Spring Security OAuth2 Test Support](https://docs.spring.io/spring-security/reference/7.2/servlet/test/mockmvc/oauth2.html)
