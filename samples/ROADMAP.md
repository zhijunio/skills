# Planned Spring Security Samples

These examples are approved for planning and have not been started. Each will be an independent, runnable application with its own README, local verification setup, and security-focused tests.

## `sb4-saml2-login`

Demonstrate a Spring Security SAML 2.0 Service Provider (relying party) login with a local IdP that can run through Docker Compose.

Planned scope:

- Configure and exchange SP/IdP metadata and certificates.
- Map the validated SAML identity to a local application user and authorities.
- Provide a small browser flow for login, authenticated user details, and local logout.
- Verify signed responses, issuer, audience, destination/recipient, time conditions, and replay rejection.
- Document certificate rotation and the difference between local logout and IdP logout.

The sample will not implement a SAML IdP or hand-parse SAML XML.

## `sb4-multitenant-authorization`

Demonstrate tenant resolution from a trusted host name and enforce tenant isolation through request authorization and data access.

Planned scope:

- Store tenants and their configured host names in the application database.
- Resolve `c.example.test` to tenant `c` from trusted request host information.
- Carry tenant context through authorization and persistence operations.
- Include tenant-scoped roles and resources, plus cross-tenant denial tests.
- Document trusted reverse-proxy host handling and local host-based verification.

DNS provisioning is an infrastructure prerequisite, not an application feature: the sample will assume either wildcard DNS or pre-created DNS records and will not call a DNS provider API.

## `sb4-multi-issuer-resource-server`

Demonstrate an API that accepts JWT access tokens from multiple explicitly trusted issuers.

Planned scope:

- Select an authentication manager from an allowlisted issuer using Spring Security's request-time resolver support.
- Validate issuer, audience, expiry, signature, and issuer-specific authority mapping.
- Protect representative endpoints with scopes and prove tokens cannot cross issuer boundaries.
- Include a small local issuer setup and negative tests for unknown issuers, invalid audiences, and insufficient scopes.

JWT support is the initial target. Opaque-token introspection is a possible extension if it can be included without obscuring the multi-issuer trust model.
