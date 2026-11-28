# Write processing

## Batch Writes

Consider JDBC batching for measured write-heavy workloads; choose configuration based on the actual driver, database and workload:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true
```

`GenerationType.IDENTITY` disables insert batching because Hibernate needs the generated key after
each row. For new designs consider UUIDs or pooled sequences when batch insert throughput matters; do not change existing identifiers without an approved migration.

## New-State Detection

Spring Data JPA detects new entities by nullable wrapper `@Version` first, then nullable ID. A
primitive version cannot be used because JPA treats `0` as the first persisted version.

For manually assigned IDs, add `@Version Long version` or implement `Persistable` with an `isNew`
flag cleared by `@PostPersist` and `@PostLoad`. Use the
[BaseAssignedIdEntity template](../templates/BaseAssignedIdEntity.java).

For large workloads bound persistence-context growth with deliberate flush/clear intervals and suitable transaction boundaries. Clearing detaches entities; do not keep mutating detached objects assuming dirty checking. A flush is not a commit. Splitting transactions changes atomicity and restart semantics; get the required contract first. Verify actual batching and rollback, not only configuration. Bulk JPQL/native changes bypass ordinary entity state synchronization and may bypass callbacks or optimistic version checks; flush/clear or refresh appropriately and enforce required invariants. saveAll alone does not prove batching.
