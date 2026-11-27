---
name: spring-boot-security
description: >
  Implement or review security for Spring Boot servlet APIs. Use for JWT
  issuance and refresh, OAuth2/OIDC client login and service credentials,
  resource servers, multi-factor authentication, authorization, ownership and
  tenant isolation, security hardening, and audit events for authentication or sensitive operations.
---

# Spring Boot Security

Trace each affected operation from request to security decision, data access,
and audit outcome. For implementation, apply the steps to the affected
operations. For review, follow the same path through the existing code and
record evidence at each boundary.

## 1. Identify the boundary

Inspect the effective Boot and Spring Security versions, active filter chains,
credential transport, and public and protected routes. The Java templates
target Boot 4 and Spring Security 7.

For each affected operation, identify who may call it, which resource or
tenant it can affect, and which outcomes require an audit record. Include
management routes and their deployment exposure when the change touches them.

## 2. Establish the caller

Determine who issues the token and how the API validates it. The same
application can issue tokens and validate them using Spring Security's resource
server support. Keep the existing validation path when changing issuance.
Use [Authentication](references/authentication.md) for JWT issuance, bearer
validation, and external identity mapping. Follow [OAuth2 Client](references/oauth2-client.md)
for browser login or outbound client credentials, and [MFA](references/mfa.md)
for factor verification or step-up rules.

Order specific filter chains first and verify route coverage. Keep CSRF
protection when browsers attach credentials automatically. The stateless resource-server templates
assume an explicit Authorization header. Configure CORS for the actual origins.
Return 401 with a bearer challenge for missing or invalid bearer credentials,
and 403 for insufficient permissions. Security filter errors need their own
entry point and access-denied handler, as described in [Authorization and hardening](references/authorization.md#security-error-responses). Follow the
[API error contract](../spring-boot-webmvc/SKILL.md#error-responses) for response bodies.

## 3. Authorize the operation

Use the verified caller from step 2 for route and service checks. Enable
method security when using authorization annotations. Enforce object ownership
and tenant membership in the affected query or write. Apply the same checks to
bulk operations, exports, and writable fields when they share the boundary.
Use [Authorization and hardening](references/authorization.md) for scoped lookups and
for reviewing inputs, persistence, uploads, or outbound calls.

## 4. Record the outcome

For operations that require auditing, use the verified actor and tenant from
the authorization decision. Record denied attempts with the available identity.
Tie committed-success events to the same transaction as the protected mutation
when durable delivery is required. Use [Security audit events](references/audit-logging.md)
for event fields, delivery, and access policy.

For example, a role-change request maps the verified token to a local
administrator, checks the administrator's permission in the target tenant,
and persists the role change and its audit event together. Verify an allowed
change, a cross-tenant denial with no mutation, and rollback with no
committed-success event.

## 5. Verify the path

Exercise the real filter chain for public access, missing or invalid credentials
(401 and challenge), insufficient authority (403), and allowed requests.
For object-level decisions, cover wrong owners and cross-tenant attempts against
the actual query or write. Exercise method security through the Spring proxy.
For audited mutations, verify rollback and retry behavior against the selected
delivery policy. Check that responses and records omit credentials and internal
diagnostics. Use [Spring Boot testing](../spring-boot-testing/SKILL.md) for test selection.

Report the checks run and remaining gaps. In a review, give each finding a
severity and a separate evidence status of confirmed, suspected, or not
verified. Include the source location, affected boundary, preconditions,
observed behavior, and how to verify the proposed fix.

[Spring Security servlet architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
