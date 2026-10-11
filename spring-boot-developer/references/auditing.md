# Auditing

Auditing metadata answers who created or last changed a current row and when. It is useful for operational traceability, but it is not an immutable audit trail: updates overwrite the current `updated*` values. Regulated or security-sensitive history needs append-only events with retention, access control and tamper-evidence appropriate to the requirement.

## Spring Data auditing

Use Spring Data auditing for entity lifecycle metadata rather than setting timestamps in every service. Enable it once and keep common fields in a small mapped superclass only when the fields truly apply across the model:

The JPA auditing listener requires `spring-aspects` on the classpath. Add `org.springframework:spring-aspects` without a version when the selected Boot BOM does not already provide it; let dependency management choose the compatible version.

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentAuditor")
class JpaAuditingConfig {

    @Bean
    AuditorAware<String> currentAuditor() {
        var trustResolver = new AuthenticationTrustResolverImpl();
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(authentication -> !trustResolver.isAnonymous(authentication))
                .map(Authentication::getName); // use getName() only when it is a stable subject ID
    }
}
```

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class AuditedEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

If only timestamps are needed, no `AuditorAware` is required. Use `Instant` for persisted timestamps and keep `@Version` separate: auditing describes authorship/time; versioning detects concurrent writes. See [spring-data-jpa.md](spring-data-jpa.md).

## MyBatis-Plus auditing

For MyBatis-Plus entities, use its `MetaObjectHandler` auto-fill lifecycle instead of Spring Data's JPA auditing listener. Keep the same property names across persistence models where practical: `createdAt`, `updatedAt`, `createdBy`, and `updatedBy`. Mark insert-only fields with `@TableField(fill = FieldFill.INSERT)` and fields refreshed on insert and update with `FieldFill.INSERT_UPDATE`; implement the fill behavior in a Spring-managed handler. See [mybatis-plus.md](mybatis-plus.md) for the handler example and the SQL paths that need explicit handling.

Resolve actor IDs from trusted server-side identity. Define an explicit system actor for scheduled jobs and message consumers with no request principal, and pass an initiating actor as a deliberate command value when required. Do not assume JPA's `@CreatedBy` / `@LastModifiedBy` annotations run for MyBatis-Plus writes. Custom mapper SQL, direct JDBC, and update forms not invoking auto-fill must set these values explicitly and be covered by tests.

## Principal and background work

Treat the auditor as an immutable subject identifier, not a display name, email address or copied security token. Resolve it from the authenticated principal through an application-owned mapping; `Authentication.getName()` is suitable only when the configured authentication mechanism guarantees a stable ID. Use `AuthenticationTrustResolver` to recognize anonymous authentication rather than comparing a configurable username string.

Define behavior when there is no authenticated request: scheduled jobs, migrations, message consumers and system maintenance can have no `SecurityContext`. Use an explicit system actor such as `system:reconciliation` (or nullable actor if the schema allows it), never a stale request identity. When a user is deleted or pseudonymized, preserve the stable audit subject and apply the system's retention/privacy policy; do not cascade-delete historical audit records by default.

Do not carry servlet request state into background work. Pass a deliberate actor ID in the command/event when a job must preserve the initiating user. Use the same identifier namespace across request and async processing.

## Boundaries and privacy

Clients must not be able to set or overwrite audit fields. Exclude them from request DTOs and assign them only through auditing infrastructure. Choose the minimum actor identifier needed; usernames, emails and IP addresses can be personal data and should follow the application's retention and access rules. Do not put secrets or full request payloads into audit metadata.

Auditing callbacks run as part of persistence lifecycle; they are not a replacement for domain events, compliance logs or a history table. When a business audit event must be retained even after a record changes or is deleted, write a separate append-only record in the same transaction.

## Testing

Test create and update metadata with a fixed clock or controlled persistence time where practical. Test authenticated and system actors, and verify clients cannot supply audit fields. For message consumers or scheduled jobs, test the no-request-context path. Verify JPA callbacks in a JPA slice and MyBatis-Plus fill and custom SQL in a persistence slice, both against the real database engine ([testing-slices-persistence.md](testing-slices-persistence.md), [testcontainers.md](testcontainers.md)).

## Gotchas

- Agent assumes `@CreatedBy` works without a principal provider - configure `AuditorAware` when actor fields are present.
- Agent adds `@EnableJpaAuditing` but omits the required `spring-aspects` dependency - verify the runtime classpath.
- Agent records `anonymousUser` for scheduled work - define an explicit system actor or nullable policy.
- Agent filters anonymous authentication by comparing a literal username - use `AuthenticationTrustResolver` and map authenticated principals to stable subject IDs.
- Agent lets request DTOs carry `createdBy` or timestamps - auditing fields are server-owned.
- Agent uses a mutable username/email as the audit actor and then renames or deletes it - store a stable subject ID and define retention/pseudonymization behavior.
- Agent expects JPA auditing annotations to fill MyBatis-Plus entities - use a `MetaObjectHandler` for MyBatis-Plus writes.
- Agent assumes the MyBatis-Plus handler covers custom SQL or direct JDBC - set audit fields explicitly on those paths and test them.
- Agent calls `@LastModifiedDate` an audit history - it is overwritten; use append-only records for retained history.
- Agent stores full payloads or credentials in audit columns - retain only the minimum safe metadata.
- Agent mixes auditing with optimistic locking - keep `@Version` for concurrency detection.

## If on Spring Boot 3.5.x

The Spring Data auditing annotations and `AuditorAware` contract remain the same. Keep `@EnableJpaAuditing` in one configuration location and check the managed Spring Data version before relying on provider-specific extensions.

## Related

- [spring-data-jpa.md](spring-data-jpa.md) · [security-fundamentals.md](security-fundamentals.md) · [async-and-scheduling.md](async-and-scheduling.md) · [transactions.md](transactions.md)
