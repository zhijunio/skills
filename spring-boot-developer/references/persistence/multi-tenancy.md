# Multi-Tenancy and Authorization

Multi-tenancy is a data-isolation boundary. A tenant identifier is not proof of access: derive the active tenant from an authenticated, trusted identity or a server-validated membership, then enforce it for every read and write.

## Resolve the tenant at the boundary

For token-based APIs, validate issuer and audience, then map a trusted claim or membership lookup to an internal tenant ID. For session applications, resolve the tenant from the authenticated user's active membership. A path or request-body `tenantId` may select among memberships only after authorization; never trust it by itself. Reject ambiguous or missing tenant context instead of silently falling back to a default.

Establish tenant context once at the request boundary and pass it explicitly into application commands and queries. Avoid mutable static or singleton fields and avoid relying on `ThreadLocal` across async, scheduled, reactive or executor boundaries. Propagate an immutable tenant identifier deliberately, revalidate the actor for background work, and clear request-scoped context in all completion paths.

## Enforce isolation in persistence

Choose the isolation model deliberately: shared tables with a `tenant_id`, separate schemas, or separate databases. Shared tables are operationally simple but require consistent enforcement; stronger physical separation costs more to provision and operate. Keep tenant filtering in repository methods or a narrowly tested persistence boundary, not as an optional controller convention.

For shared tables:

- Include `tenant_id` in unique constraints, foreign keys where appropriate, and indexes used by tenant-scoped queries.
- Require tenant-scoped lookups and updates, such as `findByTenantIdAndId`; never fetch by a globally supplied object ID and assume it is safe.
- On create, derive the tenant from trusted context rather than copying an untrusted request field.
- Make bulk updates, deletes, exports and native SQL explicitly tenant-scoped.
- Consider database row-level security as defense in depth, but set and clear the database tenant context within the transaction and test connection-pool reuse.

### MyBatis-Plus tenant-line plugin

For a shared-table MyBatis-Plus application, `TenantLineInnerInterceptor` can add a tenant predicate to supported SQL statements. Treat it as a persistence safety net, not as the source of tenant identity or a replacement for explicit tenant-scoped service and mapper contracts. Register the tenant interceptor once in the application's `MybatisPlusInterceptor` and verify its ordering with pagination and other SQL-rewriting plugins.

Resolve the tenant from the authenticated principal or a server-validated membership. If the tenant-line handler reads from Spring Security's `SecurityContext`, ensure mapper calls only run after authentication has been established. Use a request filter only when the application needs to bind a verified tenant into a context carrier for persistence code; place it after authentication and clear the carrier in `finally`. Do not derive it directly from an untrusted header, path, or body field. A filter is not required if the handler can safely resolve the tenant directly from the authenticated principal.

A small `TenantContext` backed by `ThreadLocal` can be appropriate for synchronous mapper execution or explicit maintenance operations. Give it scoped APIs that restore any previous value and remove the value in `finally`; fail closed when no tenant is present. Never treat the context itself as authentication or authorization. Do not rely on it across async, scheduled, reactive, or executor boundaries: pass an immutable tenant identifier explicitly, establish a fresh validated scope in the worker, and clear it on every completion path. Avoid inheritable thread locals because pooled threads can leak tenant state.

Maintain an explicit allowlist of global tables and test every excluded table. Review joins, custom XML/annotation SQL, subqueries, batch operations, dynamic table names, and any `@InterceptorIgnore` use: plugin coverage and SQL parsing are not universal guarantees, so tenant ownership must remain explicit in custom SQL. Insert behavior must also assign the trusted tenant ID; do not assume a select predicate alone protects writes. Integration tests should verify generated SQL and cross-tenant reads, updates, deletes, inserts, missing context, global tables, and connection/thread reuse against the production database family. See [mybatis-plus.md](mybatis-plus.md).

Return a consistent not-found or forbidden policy for cross-tenant object IDs so responses do not reveal another tenant's records. Authorization still checks the actor's permission for the action; tenant filtering alone is not object authorization. See [security-fundamentals.md](../security/security-fundamentals.md) and [oauth2-resource-server.md](../security/oauth2-resource-server.md).

## Preserve the boundary across the system

Include tenant identity in cache keys, idempotency scopes, audit records, rate-limit keys, storage ownership and outbox events. Do not put raw tenant IDs in metric tags unless the tenant set is bounded and approved; tenant identifiers in logs and traces need the same privacy and access controls as other identifiers. See [caching.md](caching.md), [api-idempotency.md](../web/api-idempotency.md), [auditing.md](auditing.md) and [rate-limiting.md](../integration/rate-limiting.md).

Tenant-aware background jobs must enumerate or claim tenant work explicitly, apply the same isolation rules, and bound concurrency so one tenant cannot starve others. Never run a tenant job under whichever request context happened to schedule it.

## Test isolation as a security property

Integration tests should create at least two tenants and prove that reads, updates, deletes, uniqueness, pagination, exports, caches, idempotency replay and async processing cannot cross the boundary. Include missing context, forged tenant input, revoked membership, and connection-pool reuse. Test against the production database engine when relying on row-level security, constraints or vendor-specific behavior.

## Gotchas

- Agent accepts a request `tenantId` as authorization - resolve tenant membership from trusted identity.
- Agent scopes controller routes but leaves repository reads, updates or native queries global - enforce tenant predicates at the persistence boundary.
- Agent relies on a `ThreadLocal` across executor work - propagate immutable context explicitly and clear it reliably.
- Agent caches an entity by ID alone - include tenant and other visibility dimensions in the key.
- Agent assumes tenant filtering replaces role or object-level authorization - enforce both.
- Agent tests only one tenant - use cross-tenant negative tests and verify async and connection-pool paths.

## Related

- [security-fundamentals.md](../security/security-fundamentals.md) · [oauth2-resource-server.md](../security/oauth2-resource-server.md) · [spring-data-jpa.md](spring-data-jpa.md) · [caching.md](caching.md) · [auditing.md](auditing.md) · [api-idempotency.md](../web/api-idempotency.md)

## References

- [Hibernate ORM User Guide](https://docs.hibernate.org/orm/current/userguide/html_single/Hibernate_User_Guide.html)
- [PostgreSQL Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
