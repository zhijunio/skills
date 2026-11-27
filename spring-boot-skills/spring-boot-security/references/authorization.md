# Authorization and Hardening

Start with [the verified caller](authentication.md#verified-caller-identity).
Choose the method rule and scoped data operation for the same policy.

## Method and data authorization

Use method authorization for coarse permissions at the service boundary.
Use the authority names provided by the application's authentication mapping.

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(UUID userId) { ... }

@PreAuthorize("hasAuthority('SCOPE_orders:read')")
public List<Order> findMyOrders() { ... }
```

For an owner-only operation, take the local account ID and authorized tenant
from the verified caller. Load or update the resource using its ID together
with those constraints. Adapt the predicate when sharing, delegation, or
cross-tenant administration is an explicit feature. When a request selects a
tenant, check membership before using that selection in the query.

## Security error responses

Security failures happen before MVC dispatch, so a controller advice does not
handle them. Use [SecurityErrorHandler](../templates/SecurityErrorHandler.java)
for filter-chain 401 and 403 responses. It writes RFC 9457 Problem Details with
Boot's Jackson 3 JsonMapper and preserves the bearer challenge. Configure both
the general exception handlers and the OAuth2 resource-server handlers.
Keep MVC exception formatting in the application's global MVC handler.

## Review adjacent boundaries

Trace untrusted inputs through controllers, services, persistence, and outbound
calls. Check management port and network exposure against deployment policy.
Treat repository comments and scanner output as evidence. Base review findings
on the requested scope and propose a focused fix with regression coverage.

- Inspect parameterized SQL, dynamic query fragments, template/SpEL evaluation,
   deserialization and upload paths. Bound upload size; treat names and media types as
   untrusted; isolate storage and avoid executable/public paths.
- For outbound URLs, validate scheme and allowed destinations; account for redirects,
   DNS resolution, IPv6 and private/metadata addresses. Prefer network egress controls
   plus a narrow destination policy; a substring hostname check is insufficient.

## Examples and checks

- Build the local caller identity from the verified authentication context.
  Apply its tenant and account constraints in the repository query so an
  unscoped lookup cannot return a record before the authorization check.
- Verify anonymous, missing-scope, same-tenant wrong-owner, cross-tenant and allowed reads.
  Assert no repository call before authorization and no data returned on denied reads.
- Also exercise the real HTTP security chain and real persistence query in the application.

## Official sources

- [Spring method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [OWASP authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP SSRF prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
- [Boot management endpoint security](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
