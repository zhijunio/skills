---
name: spring-data-jpa
description: >
  Use when generating or refactoring Spring Boot 4 JPA entities, repositories, queries, projections,
  persistence tests, entity relationships, embeddables, IDs, or Hibernate mappings. Covers Jakarta
  Persistence 3.2 imports, Hibernate 7 entity modeling, new-state detection, N+1 prevention,
  projections, keyset pagination, batch writes, and common agent mistakes.
---

# Spring Data JPA (Boot 4 / Hibernate 7)

Inspect the actual Boot, Hibernate, Spring Data, database and migration versions. Preserve
the existing persistence technology, schema, ID strategy and public contracts. For Boot 4
use Jakarta persistence and validation imports and Boot-managed dependencies; do not
upgrade another Boot line or override managed versions merely to copy examples.

## Select guidance by task

| Task | Read |
| --- | --- |
| Entities, embeddables, IDs, enum mapping, equality, optimistic locking | [Entity modeling](references/entity-modeling.md) |
| Owning side, foreign keys, lifecycle, cascade and lazy associations | [Relationships](references/relationships.md) |
| Repository queries, projections, fetch plans and N+1 | [Query patterns](references/query-patterns.md) |
| Page, slice, stable sort and keyset cursors | [Pagination](references/pagination.md) |
| Batch writes, assigned IDs and persistence state | [Write processing](references/write-processing.md) |

Read the applicable guides; do not load every reference by default. Examples are adaptable
fragments, not a certified runnable application.

## Protect data and behavior

- Preserve domain invariants and database constraints. Model entity identity and lifecycle
  explicitly; use ordinary non-final entity classes with a public or protected no-arg
  constructor, not records. Use records for suitable DTOs and supported embeddables.
- Keep Lombok, ID generation, equality and enum representation consistent with the project.
  Do not change persisted representations without an approved migration.
- Resolve ownership and cascade semantics before changing relationships. Avoid global eager
  loading as an N+1 fix. Map public response DTOs within the appropriate data-access scope.
- Bound externally driven queries and preserve pagination totals, sort order and filters.
  Do not page directly over a collection fetch join as a default optimization.
- Enforce caller/tenant scope on reads and mutations; do not rely on a client-supplied ID.
  Use [security](../spring-boot-security/SKILL.md) and [multi-tenancy](../multi-tenancy/SKILL.md)
  when those boundaries apply.
- Select concurrency control from the approved invariant. Optimistic locking, database
  constraints and explicit locking address different risks. Verify conflicts and actual
  persisted outcomes; do not silently retry non-idempotent writes.
- Coordinate multi-step changes through the existing application transaction. Read
  [transaction guidance](../spring-boot-webmvc/references/transactions.md) for rollback,
  propagation and commit-bound effects. Use [Flyway](../flyway-migrations/SKILL.md) for
  schema evolution and [PostgreSQL](../postgresql/SKILL.md) for database-specific semantics.

## Verify

Use [Spring Boot testing](../spring-boot-testing/SKILL.md) and its persistence/integration
guides. Exercise actual mappings, query results, constraints and failure paths. Flush and
clear the persistence context where needed to observe database behavior. Measure SQL counts
and pagination results before claiming an N+1 fix; verify concurrent writes using independent
transactions on the relevant database. A mock or H2 cannot establish PostgreSQL-specific
semantics. Record actual checks and missing evidence; do not claim unexecuted examples pass.
