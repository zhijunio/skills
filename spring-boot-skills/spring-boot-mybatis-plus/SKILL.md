---
name: spring-boot-mybatis-plus
description: >
  Use when adding or changing MyBatis-Plus persistence in Spring Boot 4: dependencies, mapper
  scanning, entities, wrappers, pagination, audit fields, optimistic locking, or tenant interception.
  Does not cover Spring Data JPA or direct JDBC access.
---

# Spring Boot MyBatis-Plus

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md). If unavailable, inspect resolved dependencies and release-matched official documentation; report missing compatibility evidence.

Check the selected starter, MyBatis-Plus release, parser module and actual MyBatis/Spring integration dependencies. Preserve compatible existing versions and verify custom SQL and tenant insertion semantics on the selected release.

Read [MyBatis-Plus guidance](references/mybatis-plus.md) when adding a mapper, persistence model,
or MyBatis-Plus configuration. Keep SQL schema changes in
[Flyway migrations](../flyway-migrations/SKILL.md), application transactions in
[transaction guidance](../spring-boot-webmvc/references/transactions.md), HTTP mapping in
[Spring Boot Web MVC](../spring-boot-webmvc/SKILL.md), and application tests in
[Spring Boot testing](../spring-boot-testing/SKILL.md).

Use the project's existing build and version-management conventions. This skill covers MyBatis-Plus;
for Spring Data JPA, use [spring-data-jpa](../spring-data-jpa/SKILL.md).
