# Multi-Factor Authentication

Multi-factor authentication (MFA) requires proof from at least two distinct factor categories: something the user knows, has, or is. Requiring two passwords or two possession credentials does not automatically provide two-factor authentication. Choose factors and recovery procedures from the application's threat model; prefer phishing-resistant authenticators such as passkeys where users and clients support them.

First establish the application's authentication shape:

| Application | MFA state | Typical completion |
|---|---|---|
| Browser with a server-side session | Spring Security session and authentication flow | Spring Security 7's built-in MFA support |
| Stateless REST API that issues its own JWTs | Short-lived, server-controlled pending-MFA transaction | Issue the normal access JWT only after every required factor succeeds |
| API that accepts tokens from an identity provider | The identity provider's authentication result | Validate trusted issuer claims and map the agreed assurance result to local authorization |

## Spring Security 7

Spring Security 7 provides `FactorGrantedAuthority`, `@EnableMultiFactorAuthentication`, and `AuthorizationManagerFactories.multiFactor()` for expressing factor requirements in authorization rules. An authentication mechanism must still verify each factor and provide its factor authority. For example, password and WebAuthn can be required together:

```java
@Configuration
@EnableMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.WEBAUTHN_AUTHORITY
})
class MfaConfiguration {

    @Bean
    SecurityFilterChain browserSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .webAuthn(webAuthn -> webAuthn
                        .rpId("example.com")
                        .allowedOrigins("https://example.com"))
                .build();
    }
}
```

This illustrates a browser/session flow. Spring Security combines the required factors with authorization rules and can direct a user to the mechanism that supplies a missing factor. The annotation applies its factor requirement to authorization rules across the application, so do not use it when public routes or lower-assurance routes need different policy. For selective MFA, enable MFA without global required authorities, create an `AuthorizationManagerFactory` with `AuthorizationManagerFactories.multiFactor()`, and apply its manager only to routes that require additional proof.

Spring Security's `oneTimeTokenLogin()` is One-Time Token (OTT) login. It can deliver a login token out of band, for example by email, but it is not TOTP/HOTP. Treat it as a distinct authentication mechanism and decide explicitly whether it is an acceptable factor for the application's policy.

Spring Security 7 also provides factor-issued-at information for rules that require recent authentication. Use a freshness requirement for sensitive operations such as changing MFA settings, rotating credentials, or performing high-impact actions; do not treat an old factor proof as indefinitely fresh.

For an application-owned two-step browser flow, keep the primary login mechanisms and the application's second-factor verification separate. For example, an application may accept password or passkey as its primary step, then verify TOTP, email, or another enrolled factor in application code. In that design, configure `@EnableMultiFactorAuthentication(authorities = {})` when global built-in requirements would incorrectly require both password and WebAuthn, grant the second-factor authority only after successful verification, and apply the application's factor-combination policy to protected routes. A label such as "primary" describes the application's flow; it does not prove that two credentials belong to distinct factor categories. Treat Spring's WebAuthn authority as evidence of the WebAuthn authentication mechanism, and evaluate user verification separately when the assurance policy depends on it.

For a browser session flow, the application may keep an authenticated-but-incomplete MFA transaction in server-side state, but it must not treat that state as fully authenticated for protected routes. For a stateless API, use a separate pending-MFA transaction as described below.

## Staged MFA for a JWT API

JWT bearer authentication is not itself an MFA flow. A resource server validates a token; it does not know whether the issuer required multiple factors unless the issuer conveys that result in a trusted, signed claim and the application maps it into authorization.

Use a two-stage login:

1. Verify the primary credential through Spring Security's authentication manager.
2. Create a short-lived pending-MFA transaction bound to the user and the allowed next step. It must not authorize normal API requests.
3. Verify the selected second factor, enforce attempt limits, and consume the pending transaction once.
4. Only then issue the normal access JWT with the intended subject, issuer, audience, scopes, expiry, and verified authentication-method or assurance claims.
5. In the resource server, validate the JWT as usual and map only trusted claims from the configured issuer to local authorities. Apply authorization rules to the required factors and, when needed, authentication freshness.

Prefer an opaque, server-side transaction with a short expiry and one-time consumption. If a signed temporary JWT is used, give it a distinct audience and token purpose, accept it only on the MFA-completion endpoint, set a short expiry, and prevent replay (for example, with a consumed `jti`). Never accept it as an access token. Do not put passwords, OTP values, WebAuthn challenges, recovery secrets, or wallet seed material in a JWT.

The `amr` claim can identify authentication methods. Prefer registered values from RFC 8176 when they accurately describe the method; document and agree on private values and their authorization meaning between issuer and resource server. Map values only from a JWT whose signature, issuer, audience, and lifetime have already been validated. A client must never be able to grant itself an MFA authority by submitting a claim or header.

For externally issued tokens, prefer the identity provider's MFA. Validate its issuer and audience, and map only the provider's documented, signed authentication context or method claims. Do not re-run a local MFA flow unless the application owns that policy and can verify the underlying factors.

## Choose and implement factors

### WebAuthn and passkeys

Spring Security provides WebAuthn/passkey authentication, including registration and assertion verification. Configure the relying-party ID and allowed origins for the real application origins, and use persistent credential repositories in deployed applications. Treat a passkey as MFA only when the policy and authenticator prove the required user verification, such as a local PIN or biometric check; a user-presence tap alone is not proof of that verification. When adding or replacing credentials, require recent authentication and notify the account owner.

A passkey can also be the primary passwordless authenticator. In that case, do not claim that a separate password factor was used. Decide whether the passkey's user-verification result meets the application's assurance policy.

### TOTP and email codes

TOTP/HOTP require application-specific enrollment and verification. Confirm the authenticator-generated code before activating a newly enrolled factor. Protect TOTP shared secrets as recoverable secrets (encrypt them at rest and restrict access), allow only a narrow clock window, rate-limit guesses, and prevent reuse of an accepted time step.

Email OTP proves access to the mailbox and is a weaker fallback than a phishing-resistant authenticator. Keep codes short-lived and single-use; limit attempts and resends; invalidate a prior code when issuing a replacement; and never log the code. If a code must be stored, keep a verifier rather than plaintext; because codes have a small keyspace, hashing is short-term exposure mitigation, not strong resistance to a database compromise. Do not reveal whether an account exists when requesting or resending a code.

Spring Security's `oneTimeTokenLogin()` is an OTT login flow, commonly delivered by email or SMS. It is not a TOTP authenticator and does not by itself make a password-plus-email REST API flow. For JWT APIs, integrate email verification into the pending-MFA transaction and issue the access JWT only after it succeeds.

### Wallet signatures and recovery phrases

Never ask an application user to submit or store a Web3 wallet's 12-word recovery phrase. A BIP-39 mnemonic derives wallet seed material and can restore control of the wallet; it is not an application backup code.

To authenticate a wallet, use a signed challenge such as Sign-In with Ethereum (SIWE): issue a unique, short-lived nonce and verify the signature plus the expected domain, URI, chain, nonce, and expiration. Consume the nonce once. A wallet signature proves control of a wallet key. Email access and wallet-key control are both possession proofs, so combine the wallet with a different factor category when policy requires strict MFA.

### Application recovery codes

Application recovery credentials are separate from wallet seed phrases. If the application issues a mnemonic, generate a new application-only secret; never ask the user to enter an existing wallet phrase. Treat the issued phrase as a full recovery credential. For production, prefer a set of high-entropy, individually single-use recovery codes: show them once, store verifiers rather than plaintext, rate-limit attempts, and atomically consume each code. Let the user replace the remaining set after recovery. Recovery and factor-reset workflows need strong identity proof, audit records, notifications, and protections against social-engineering bypasses.

## Enrollment, step-up, and recovery

- Bind a factor to the authenticated account only after the user proves control of it. Keep newly registered factors pending until confirmation succeeds.
- Require recent authentication before adding, replacing, or disabling a factor, changing recovery details, or using recovery codes.
- Define one policy for every login path, including browser, mobile, API, password reset, support-assisted recovery, and alternate identity providers. A weaker parallel route can bypass MFA.
- Use step-up authentication for sensitive operations. A valid access token alone may be insufficient when its factor proof is missing or too old.
- Keep factor secrets, OTPs, recovery codes, and WebAuthn challenges out of logs, analytics, traces, and error responses.
- Rate-limit by account and an appropriate shared boundary. Avoid lockout behavior that lets an attacker deny service to a known user.
- Preserve CSRF protection for cookie/session flows. Use `STATELESS` only for a genuinely stateless bearer-token API; keep pending MFA state separate from normal access tokens.

## Testing

Test the complete authentication and authorization contract, not just a successful code comparison:

- A primary-factor-only login cannot access routes requiring MFA.
- A valid second factor completes the transaction and grants only the intended authorities and scopes.
- A wrong, expired, replayed, or over-attempted challenge is rejected and cannot mint an access token.
- A pending-MFA token cannot be used as a bearer access token; wrong issuer, audience, purpose, or signature is rejected.
- TOTP replay, email-code resend, and recovery-code reuse are rejected.
- WebAuthn registration and assertion challenges are bound to the expected account, relying-party ID, origin, and one-time challenge; verify that user verification is required where policy needs it.
- Factor enrollment, removal, recovery, and step-up paths require the intended recent authentication and produce the expected notifications or audit events.
- Every alternate login and recovery path enforces the same assurance policy.

Test the configured factor requirements and selective-MFA rules. See [testing strategy](../testing/testing-strategy.md), [integration testing](../testing/testing-integration.md), and [REST API testing](../testing/testing-rest-api.md).

## Gotchas

- Agent treats `oneTimeTokenLogin()` as TOTP - OTT and time-based OTP are different mechanisms.
- Agent issues a full access JWT after only the password step - keep the user in pending MFA state until all required factors succeed.
- Agent trusts a client-supplied `amr`, role, header, or request field - derive authorities only from verified server-side authentication or trusted signed token claims.
- Agent uses email and wallet possession as if they were different factor categories - they are both possession proofs; select factors according to the required assurance.
- Agent asks for a wallet seed phrase as MFA or account recovery - use a signature challenge for wallet control; keep mnemonic recovery within the wallet.
- Agent calls a passkey "MFA" without checking user verification - require and validate the authenticator's user-verification result when the policy depends on it.
- Agent implements factor enrollment but omits recovery and factor-reset protection - recovery is part of the authentication boundary and needs equivalent review.

## Related

- [Security fundamentals](security-fundamentals.md) · [JWT authentication](jwt-authentication.md) · [OAuth2 resource server](oauth2-resource-server.md) · [Rate limiting](../integration/rate-limiting.md) · [Authentication and authorization testing](../testing/testing-strategy.md)

## References

- [Spring Security: Multi-Factor Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html)
- [Spring Security: Passkeys](https://docs.spring.io/spring-security/reference/servlet/authentication/passkeys.html)
- [Spring Security: One-Time Token Login](https://docs.spring.io/spring-security/reference/servlet/authentication/onetimetoken.html)
- [Spring Security: JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [OWASP: Multifactor Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html)
- [RFC 8176: Authentication Method Reference Values](https://www.rfc-editor.org/rfc/rfc8176.html)
- [BIP-39: Mnemonic code for generating deterministic keys](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [EIP-4361: Sign-In with Ethereum](https://eips.ethereum.org/EIPS/eip-4361)
