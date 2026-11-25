# Multi-Factor Authentication

Read this when an application needs Spring Security MFA or step-up
authorization. This reference covers servlet applications; follow
[security.md](security.md) for filter-chain, error-response, and execution
context rules.

## Defaults

Import `@EnableMultiFactorAuthentication` from
`org.springframework.security.config.annotation.authorization` and
`FactorGrantedAuthority` from `org.springframework.security.core.authority`.
Spring Boot's dependency management selects the compatible Spring Security
version; do not override it for this feature.

1. Use Spring Security's native factor model. Its authentication mechanisms
   add `FactorGrantedAuthority` values to the authenticated `Authentication`;
   authorization then requires the configured factors. MFA requires both an
   authorization rule and an authentication mechanism for every required
   factor. Enabling MFA alone does not verify a second factor.
2. Prefer Spring Security's supported mechanisms, such as password, one-time
   token (OTT), and WebAuthn where the application needs them. `oneTimeTokenLogin`
   is Spring Security's OTT flow; it is not a TOTP authenticator implementation.
   If the requirement is TOTP or another factor without a suitable built-in
   mechanism, implement verification as a Spring Security authentication
   mechanism and grant its factor authority only after successful verification.
   OTT login also requires a `OneTimeTokenGenerationSuccessHandler` that
   delivers the generated token through the application's verified channel.
3. Configure factor authentication through `SecurityFilterChain` and Spring
   Security filters/providers, using success/failure handlers as needed. Keep
   login, challenge, and factor-verification protocol processing out of
   `@Controller` and `@RestController` methods. Do not grant a factor based on
   a request parameter, client-supplied authority, or an unverified identity
   claim.
4. Choose MFA scope deliberately. `@EnableMultiFactorAuthentication` with
   required authorities applies the factors to every authorization rule.
   Use `AuthorizationManagerFactories.multiFactor()` on selected matchers
   when only sensitive routes require step-up. Keep role/permission checks in
   the same authorization decision as factor requirements.
5. Use `validDuration` when a sensitive operation requires recent
   reauthentication. A factor authority records when it was issued, allowing
   the authorization rule to require a factor within a chosen duration.
6. Treat factor selection as an assurance decision: two mechanisms count as
   MFA only when they establish distinct factors appropriate to the threat
   model. Email or SMS OTT has different assurance properties from a
   cryptographic authenticator; do not describe every two-step flow as
   phishing-resistant MFA.
7. OAuth2/OIDC login can establish an authenticated principal, but does not
   automatically prove that the application-specific MFA policy was met.
   Map upstream assurance to factor authorities only when the issuer's
   authentication context is validated and the mapping is explicit.

## Global MFA

Use global MFA only when every authorization rule, including lower-risk
application routes, should require all configured factors. Add the actual
factor mechanisms to the chain:

```java
@Configuration
@EnableMultiFactorAuthentication(authorities = {
		FactorGrantedAuthority.PASSWORD_AUTHORITY,
		FactorGrantedAuthority.OTT_AUTHORITY
})
class SecurityConfiguration {

	@Bean
	SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.formLogin(Customizer.withDefaults())
				.oneTimeTokenLogin(Customizer.withDefaults())
				.build();
	}
}
```

The health matcher is explicitly public. Confirm how global MFA interacts with
every authorization rule in the selected Spring Security version before using
this pattern in an application with public or machine-to-machine routes.

## Step-up for selected routes

For APIs or applications where only sensitive actions require MFA, create a
multi-factor authorization manager and apply it to those matchers. Order
matchers from specific to general:

```java
@Configuration
@EnableMultiFactorAuthentication(authorities = {})
class SecurityConfiguration {

@Bean
SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
	// Enable selective use of MFA authorization managers in this chain.
	var mfa = AuthorizationManagerFactories.multiFactor()
			.requireFactors(
					FactorGrantedAuthority.PASSWORD_AUTHORITY,
					FactorGrantedAuthority.OTT_AUTHORITY)
			.build();

	return http
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/admin/**").access(mfa.hasRole("ADMIN"))
					.requestMatchers("/profile/security/**").access(mfa.authenticated())
					.anyRequest().authenticated())
			.formLogin(Customizer.withDefaults())
			.oneTimeTokenLogin(Customizer.withDefaults())
			.build();
}
}
```

For a recent-password rule, configure a factor manager with a duration and
combine it with the required role or authentication predicate:

```java
var recentPassword = AuthorizationManagerFactories.multiFactor()
		.requireFactor(factor -> factor
				.passwordAuthority()
				.validDuration(Duration.ofMinutes(30)))
		.build();

// In authorizeHttpRequests:
auth.requestMatchers("/payments/approve/**").access(recentPassword.hasRole("APPROVER"));
```

## Errors, sessions, and asynchronous work

An unauthenticated request and an authenticated request missing a required
factor are different security outcomes. Preserve Spring Security's challenge
and access-denied behavior, and use the shared security error writer described
in [security.md](security.md) so filter responses follow the same public error
contract as MVC errors. Browser flows may redirect to the configured missing
factor mechanism; API flows should return the documented API error response.

For session-based browser authentication, retain session fixation protection
and CSRF protection. Treat factor authorities as part of the current
authentication state and ensure session/authentication renewal follows the
chosen mechanism. For asynchronous work, propagate the security context only
when required and reauthorize sensitive work at execution time; see
[async.md](async.md).

## Testing

Use Spring Security's test support and exercise the real filter chain. Verify
that a user with only the first factor is denied on MFA-protected routes, a
user with all required factors and permissions is allowed, and ordinary
authenticated routes remain accessible when MFA is scoped selectively. Test
the missing-factor browser/API response and recent-factor expiry when those
behaviors are configured. Tests must not bypass the chain or simulate factor
completion through a controller.

The runnable [security template](../assets/template-security/README.md)
demonstrates selective password-plus-OTT authorization, email delivery through
Spring Security's OTT success handler, and MockMvc tests for missing factors
and generated-token delivery. See its
[MFA filter-chain rules](../assets/template-security/src/main/java/io/example/templatesecurity/config/SecurityConfiguration.java),
[OTT email handler](../assets/template-security/src/main/java/io/example/templatesecurity/config/EmailOneTimeTokenSuccessHandler.java),
and [tests](../assets/template-security/src/test/java/io/example/templatesecurity/SecurityIntegrationTests.java).

## References

- [Spring Security Multi-Factor Authentication](https://docs.spring.io/spring-security/reference/7.2/servlet/authentication/mfa.html)
- [Spring Security Servlet Authentication Architecture](https://docs.spring.io/spring-security/reference/7.2/servlet/authentication/architecture.html)
- [Spring Security Servlet Testing](https://docs.spring.io/spring-security/reference/7.2/servlet/test/)
