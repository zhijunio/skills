---
name: spring-boot-4
description: >-
  Use whenever a task creates, implements, changes, or debugs a Java Spring
  Boot 4 application, including its build, APIs, persistence, tests,
  configuration, integrations, observability, or delivery. Route
  stack-specific work to the matching reference.
license: MIT
metadata:
  author: Zhijunio
  version: '0.1'
---

# Spring Boot 4

Use this skill as the entry point for Spring Boot 4 application work. First
establish the project's Boot version, Java version, and build tool. Then read
the applicable references below and follow their rules. References are
progressively disclosed: open only those matching the task.

## Route the task

| Work involves | Read |
| --- | --- |
| New app, feature boundaries, package organization, deployment topology, or cross-feature dependencies | [references/architecture.md](references/architecture.md) |
| Build files, dependencies, plugins, release, or coverage | For Maven work, use [references/maven.md](references/maven.md); for other build tools, follow the project's existing build conventions |
| Controller, application service, transactions, or exception advice | [references/webmvc.md](references/webmvc.md) |
| HTTP contract, paths, status, request/response shape, or API versioning | [references/rest.md](references/rest.md) |
| OpenAPI, springdoc, Swagger UI, or JavaDoc API descriptions | [references/openapi.md](references/openapi.md) |
| MyBatis-Plus, Mapper, persistence model, or database access | [references/mybatis-plus.md](references/mybatis-plus.md) |
| PostgreSQL datasource, SQL types, or database time zone | [references/postgresql.md](references/postgresql.md) |
| SQL schema or migrations | [references/flyway.md](references/flyway.md) |
| Local PostgreSQL, Redis, Mailpit, or Docker Compose | [references/docker.md](references/docker.md) |
| Redis-backed application cache | [references/cache.md](references/cache.md) |
| Outbound email or SMTP | [references/email.md](references/email.md) |
| Spring properties, profiles, or `@ConfigurationProperties` | [references/configuration.md](references/configuration.md) |
| In-process asynchronous work or task executors | [references/async.md](references/async.md) |
| Logs, metrics, traces, health, or Actuator exposure | [references/observability.md](references/observability.md) |
| Tests or datasource-related context loading | [references/testing.md](references/testing.md) |
| Taskfile or `go-task` | [references/taskfile.md](references/taskfile.md) |
| Optional local BootUI console | [references/bootui.md](references/bootui.md) |
| GitHub Actions, CI, tags, or GitHub releases | [references/cicd.md](references/cicd.md) |

Read more than one reference when a task crosses boundaries. For example,
schema changes that add persistence use MyBatis-Plus, Flyway, PostgreSQL, and
testing references as applicable. Local service setup routes to Docker; it
does not by itself imply a production architecture decision.

For Gradle or another build tool, follow the existing build and wrapper
conventions. The Maven reference applies only to Maven-specific tasks.

## Core Rules

1. For a new project, use Spring Boot 4.x and Java 21 (minimum Java 17).
   For an existing project, retain its Boot line unless the requested change
   requires an upgrade.
2. Organize code by business feature. Read
   [references/architecture.md](references/architecture.md) when creating a
   project or changing its module, feature, or deployment boundaries.
3. Follow `jackson.major` and `jackson.mapper` in `versions.json` (currently
   Jackson 3) and inject `JsonMapper`; use `tools.jackson` for databind and
   retain `com.fasterxml.jackson.annotation`. Serialize `Instant` as a
   timestamp with `spring.jackson.datatype.datetime.write-dates-as-timestamps:
   true`; do not add `@JsonFormat` for this. Do not declare an `ObjectMapper`
   bean or add Jackson 2, Gson, or JSON-B.
4. Use `RestClient` for outbound HTTP. Do not use `RestTemplate`.
5. Use constructor injection and avoid Lombok in new code.
6. Keep the generated `*ApplicationTests` context-load test. For test changes,
   follow [references/testing.md](references/testing.md).
7. New applications use the defaults supported by their selected build tool.
   Read [references/architecture.md](references/architecture.md) for new
   projects. For Maven, also read [references/maven.md](references/maven.md).
   For web applications, read [references/openapi.md](references/openapi.md).
   Apply these new-project defaults unless the user declines them. Do not add
   them to an existing application unless asked to align it.
8. Optional integrations and developer tools are added only when the task
   needs them. GitHub Actions workflows are opt-in.

## Workflow

### Create

1. Generate the skeleton with the script below, or use the requested build
   tool's Spring Initializr option when it is not Maven.
2. Read the generated build file and `versions.json`; establish the Boot and
   Java versions and build tool. For Maven, use `springBoot.maven`; for other
   build tools, use `springBoot.default` where the Boot version must be pinned
   and follow that tool's plugin or dependency-management configuration.
3. Apply the new-project defaults: architecture checks, build-tool conventions,
   and OpenAPI for a web application unless the user declined them. Read the
   matching references, then add only requested optional stacks and features.
4. Review the generated project against Core Rules and every reference used.
   Resolve each violation before handing off.
5. Run the project's test task once, following its wrapper and test
   conventions. Start required services when available; report any tests that
   could not run because a required service was unavailable.

### Change

1. Read the build file and establish Boot version, Java version, and build
   tool. Keep the existing Boot line and package/API contracts unless the task
   requires changing them.
2. Identify the affected behavior, read the relevant configuration, code,
   tests, and matching reference files.
3. Make the smallest coherent change. Do not add new-project defaults to an
   existing application unless requested.
4. Review the change against Core Rules and every reference used. Resolve each
   violation before handing off.
5. Run the relevant test task once, following the project's wrapper and test
   conventions. Start required services when available; report any tests that
   could not run because a required service was unavailable.

## Create script

Run from this skill directory and set `--output-dir` to a workspace outside
the skill tree. The generator needs Node.js 18+, `unzip`, and access to Spring
Initializr:

```bash
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
```

The script requests `web`, `validation`, and `actuator` from Spring Initializr,
then pins the Maven parent to `springBoot.maven`. It creates only a starter
skeleton: the **Create** workflow adds the required house defaults from the
Maven, architecture, and OpenAPI references. Optional stacks remain opt-in.
For another build tool, use Spring Initializr with that tool and follow its
existing dependency-management conventions plus the same stack references.

## Scope boundaries

- Java language, JDK, or language-server work with no Boot application change:
  [java-developer](../java-developer/SKILL.md)
- PostgreSQL server operations, SQL tuning, or replication:
  [postgresql-developer](../postgresql-developer/SKILL.md)
- SSO, OIDC, SAML, or identity providers:
  [spring-sso-developer](../spring-sso-developer/SKILL.md)
- Spring Cloud, config server, or service discovery:
  [spring-cloud-developer](../spring-cloud-developer/SKILL.md)
- JVM performance, GC, or profiling:
  [java-performance-developer](../java-performance-developer/SKILL.md)
