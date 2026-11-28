# Security testing

## Security regression matrix

For protected reads and writes, cover anonymous requests, invalid/expired credentials,
missing authority, same-tenant wrong owner, cross-tenant access, and the allowed caller.
Exercise the real SecurityFilterChain and method-security proxy where those enforce policy;
a mock Authentication or direct service call does not validate JWT decoding or proxy wiring.
Preserve the application's 401/403/404 contract instead of imposing one globally.

For denied writes, assert unchanged persisted state and no external effect, not just the
response status. For denied reads, assert that no other owner's data reaches the response.
Use the actual database query for tenant/owner isolation tests; mocks only prove delegation.
Keep CSRF enabled for cookie-authenticated browser flows and explicitly test missing/invalid
CSRF tokens. Do not disable filters or add permissive test security merely to get green tests.

Include secret-free error/log assertions, audit rollback/sink-failure cases, and bounded
AI-tool calls where those features exist. Keep adversarial fixtures synthetic and offline.
Running active scans or provider-backed evaluations requires the relevant scope and approval.

Use [MVC testing](mvc-testing.md) for focused request tests and [integration testing](integration-testing.md)
for actual token verification, server wiring, real tenant data and persisted effects. Test a real
security boundary when claiming that boundary is enforced. Keep missing credentials, insufficient
authority, wrong ownership and tenant isolation distinct; expected status codes and behavior come
from the approved contract.
