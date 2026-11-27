---
name: postgresql
description: >
  Use when configuring PostgreSQL for a Spring Boot application, selecting SQL types, defining
  database time-zone behavior, or reviewing constraints, indexes, and concurrent writes. Does not
  cover PostgreSQL server administration, tuning, replication, or cluster operations.
---

# PostgreSQL

Use this skill for application-level PostgreSQL behavior: JDBC configuration, types, time zones,
constraints, indexes, and database-backed concurrency.
Read the [PostgreSQL application guidance](references/postgresql.md) for concrete type,
constraint, and transaction patterns.

Keep related work in its focused skill: [Flyway migrations](../flyway-migrations/SKILL.md) for
schema changes, [Spring Data JPA](../spring-data-jpa/SKILL.md) or
[MyBatis-Plus](../spring-boot-mybatis-plus/SKILL.md) for persistence mappings,
[Spring Boot Docker](../spring-boot-docker/SKILL.md) for a local PostgreSQL service, and
[Spring Boot testing](../spring-boot-testing/SKILL.md) for integration-test setup.
