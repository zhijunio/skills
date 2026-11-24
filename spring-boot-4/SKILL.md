---
name: spring-boot-4
description: >-
  Creates and maintains Spring Boot 4 applications. Use whenever the task
  scaffolds, implements, changes, or debugs a Spring Boot 4 service, including
  MyBatis-Plus, REST, Jackson 3, OpenAPI, Flyway, Maven, or Compose work in
  that app. Not for Java language-only tasks, PostgreSQL engine diagnosis, SSO
  or identity providers, Spring Cloud, CI/CD pipelines, or JVM performance.
license: MIT
metadata:
  author: Zhijunio
  version: '0.1'
---

# Spring Boot 4

Router for Spring Boot 4 application work. Core Rules are the v1 floor. Stack
detail lives in `references/` and is read only when that file exists.

## When to use

- Scaffold a new Spring Boot 4 project
- Implement or change behavior in a Boot 4 app
- Debug a Boot 4 service that this skill's stacks cover

## When not to use

- Java language, JDK, or language-server work with no Boot app change → [java-developer](../java-developer/SKILL.md)
- PostgreSQL engine, SQL tuning, or replication → [postgresql-developer](../postgresql-developer/SKILL.md)
- SSO, OIDC, SAML, or identity providers → [spring-sso-developer](../spring-sso-developer/SKILL.md)
- Spring Cloud, config server, or service discovery → [spring-cloud-developer](../spring-cloud-developer/SKILL.md)
- CI pipelines or GitHub Actions → [ci-cd-developer](../ci-cd-developer/SKILL.md)
- JVM performance, GC, or profiling → [java-performance-developer](../java-performance-developer/SKILL.md)

## Core Rules

1. Read the build file and establish the Boot version, Java version, and build
   tool before giving version-sensitive guidance.
2. Use Boot 4.x for new projects. Reject Boot 3.x in the generator.
3. Default Java is 21. Floor is 17.
4. Package by feature. A feature owns its Controller, Service, and Mapper.
   Do not start with top-level `controller/`, `service/`, and `mapper/` packages.
5. Controller binds HTTP. Service owns `@Transactional`. Mapper extends
   `BaseMapper` and uses Lambda Wrappers. No SQL in annotations or XML.
6. Persistence is MyBatis-Plus. Do not add Spring Data JPA, `spring-boot-starter-jdbc`,
   `JdbcTemplate`, or `JdbcClient`.
7. Schema changes go in Flyway. Singular `snake_case` tables. `bigint` primary
   keys. Do not declare `FOREIGN KEY`. Keep `PRIMARY KEY`, `UNIQUE`, and
   `NOT NULL`.
8. REST uses plural `/api/{resources}`, `*Request` and `*Response` records,
   `@Valid`, and RFC 9457 `ProblemDetail`. Do not return persistence types.
9. JSON is Jackson 3. Inject `JsonMapper`. Use `tools.jackson` for databind.
   Keep `com.fasterxml.jackson.annotation`. Do not declare an `ObjectMapper`
   bean, add `spring-boot-jackson2`, Gson, or JSON-B.
10. OpenAPI is code-first springdoc. Do not add a spec-first generator unless
    the task names it.
11. Constructor injection. No Lombok on new code.
12. Keep the Initializr `*ApplicationTests` context load. Do not add other test
    classes. Testing conventions live in `references/testing.md` when that file
    exists.
13. Verify with the project wrapper. After generation run
    `node scripts/doctor-project.mjs --strict <project>`. After code changes run
    `./mvnw verify` and report the commands actually run.

## Create a project

Run scripts from this skill directory. Set `--output-dir` to the user workspace.
Do not generate inside this skill directory.

```bash
node scripts/check-environment.mjs
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
node scripts/doctor-project.mjs --strict /absolute/path/to/workspace/inventory-api
```

Defaults are Maven, Java 21, Boot from `versions.json`, MyBatis-Plus, and
PostgreSQL. Check `--help` and `versions.json`. PostgreSQL projects overwrite
Initializr's `compose.yaml` with `assets/compose.yaml` and the image from
`versions.json`.

Phase 1 emits a Boot 4 skeleton with MyBatis-Plus, Flyway, `@MapperScan`,
Jackson 3, springdoc OpenAPI, PostgreSQL Compose, and the Initializr context-load
test. It does not copy a sample feature. It does not add Testcontainers classes,
slice tests, or architecture tests.

## Change an existing project

1. Identify the outcome. Read build files, configuration, neighboring code, and
   tests.
2. Keep existing Boot line, package layout, and API contract unless the change
   requires an update.
3. Read only the matching stack reference when that file exists. Implement the
   smallest coherent change.
4. Do not add test classes until `references/testing.md` exists.
5. Run the wrapper and `doctor-project.mjs --strict` when configuration changed.
   Report actual results.

## Stack references

Read `references/<stack>.md` only when that file exists and the task is on that
stack. Phase 1 has no files under `references/`. Until a file exists, stay on
Core Rules. Do not invent stack rules. Do not create stub reference files.

Each later reference owns that stack's conventions, templates, and Verify
command.

Default stack, generated every time.

| Task | File |
| --- | --- |
| Architecture and packaging | `references/architecture.md` |
| MVC roles | `references/mvc.md` |
| MyBatis-Plus | `references/mybatis-plus.md` |
| REST | `references/rest.md` |
| Schema SQL | `references/schema-sql.md` |
| Validation | `references/validation.md` |
| Errors | `references/error-handling.md` |
| Jackson 3 | `references/json-jackson.md` |
| OpenAPI | `references/openapi.md` |
| Transactions | `references/transactions.md` |
| Configuration | `references/configuration.md` |
| Testing | `references/testing.md` |
| Maven | `references/maven.md` |
| Compose | `references/docker-compose.md` |
| Security defaults | `references/security.md` |
| Actuator | `references/observability.md` |

On demand. Read only when the task names that stack.

| Task | File |
| --- | --- |
| HTTP clients | `references/http-clients.md` |
| Redis cache | `references/caching.md` |
| Async | `references/async.md` |
| Messaging | `references/messaging.md` |
| Resilience | `references/resilience.md` |
| Gradle | `references/gradle.md` |
| API versioning | `references/api-versioning.md` |
| File storage | `references/file-storage.md` |
| Mail | `references/mail.md` |
