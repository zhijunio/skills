# Multi-Factor Authentication

Use Spring Security's native factor model for multi-factor authentication and
step-up authorization. This guide covers servlet applications and assumes
filter-chain and security error handling from the main
[Spring Boot Security workflow](../SKILL.md).

## Selective step-up

Start from [MfaSecurityConfig](../templates/MfaSecurityConfig.java) for
selective step-up rules. It demonstrates a recent-password requirement for
payment approval and password-plus-OTT for administrative routes. Adapt the
roles, paths, and factor policy to the application.

The chain must configure an authentication mechanism for each required factor.
The template uses form login for the password factor and OTT login for the
second factor. Provide a OneTimeTokenGenerationSuccessHandler that delivers
the generated token through a verified channel. The default development
delivery behavior is not a production delivery policy.

Prefer Spring Security's factor authorities and supported authentication
mechanisms. Enabling MFA authorization does not itself verify another factor.
Keep challenge and verification processing in filters and authentication
providers, not MVC controllers. Grant a factor authority only after successful
verification.

Combine factor requirements with role or permission checks in the same
authorization rule. Use global MFA only when every authorization rule requires
all configured factors. Use validDuration when an operation requires recent
reauthentication. An OIDC login establishes an authenticated principal but
does not automatically satisfy the application's factor policy. Map upstream
assurance only from a validated issuer context and explicit policy.

Spring Security's OTT login is a one-time-token flow, not TOTP. For TOTP or
another unsupported factor, implement verification as an authentication
mechanism and issue the factor authority only after successful verification.
Choose factors with distinct assurance properties appropriate to the threat
model. Do not describe every two-step flow as phishing-resistant MFA.

## Verification

Exercise the real filter chain. Confirm a first-factor-only user is denied on
MFA-protected routes, a user with required factors and permissions is allowed,
and ordinary authenticated routes remain usable when MFA is selective. Verify
recent-factor expiry and the missing-factor response when configured. Test OTT
delivery without simulating factor completion through a controller.

[Spring Security MFA](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html)
