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
| Build files, dependencies, plugins, release, or coverage | Maven: [references/maven.md](references/maven.md). Gradle: [references/gradle.md](references/gradle.md). For other tools, follow the project's existing build conventions |
| Controller, application service, transactions, or exception advice | [references/webmvc.md](references/webmvc.md) |
| HTTP contract, paths, status, request/response shape, or API versioning | [references/rest.md](references/rest.md) |
| OpenAPI, springdoc, Swagger UI, or JavaDoc API descriptions | [references/openapi.md](references/openapi.md) |
| MyBatis-Plus, Mapper, persistence model, or database access | [references/mybatis-plus.md](references/mybatis-plus.md) |
| PostgreSQL datasource, SQL types, or database time zone | [references/postgresql.md](references/postgresql.md) |
| SQL schema or migrations | [references/flyway.md](references/flyway.md) |
| Local PostgreSQL, Redis, Mailpit, or Docker Compose | [references/docker.md](references/docker.md) |
| Kubernetes workload manifests, probes, or graceful shutdown | [references/kubernetes.md](references/kubernetes.md) |
| GraalVM Native Image, Spring AOT, or CRaC | [references/native-deployment.md](references/native-deployment.md) |
| Redis-backed application cache | [references/cache.md](references/cache.md) |
| Outbound email or SMTP | [references/email.md](references/email.md) |
| Spring properties, profiles, or `@ConfigurationProperties` | [references/configuration.md](references/configuration.md) |
| In-process asynchronous work or task executors | [references/async.md](references/async.md) |
| Logs, metrics, traces, health, or Actuator exposure | [references/observability.md](references/observability.md) |
| Spring Security filter chains, HTTP Basic/form login, authorization, CSRF, or CORS | [references/security.md](references/security.md) |
| Spring Security multi-factor authentication (MFA), factor authorities, or step-up authorization | [references/mfa.md](references/mfa.md) |
| OAuth2/OIDC client login or outbound OAuth2 client credentials | [references/oauth2-client.md](references/oauth2-client.md) |
| Validating OAuth2 bearer tokens on an API | [references/oauth2-resource-server.md](references/oauth2-resource-server.md) |
| Tests or datasource-related context loading | [references/testing.md](references/testing.md) |
| Taskfile or `go-task` | [references/taskfile.md](references/taskfile.md) |
| Optional local BootUI console | [references/bootui.md](references/bootui.md) |
| React SPA or Next.js frontend integrated with Spring Boot | [references/frontend-integration.md](references/frontend-integration.md), plus [references/react.md](references/react.md) or [references/nextjs.md](references/nextjs.md) |
| GitHub Actions, CI, Renovate, tags, or GitHub releases | [references/cicd.md](references/cicd.md) |

Read more than one reference when a task crosses boundaries. For example,
schema changes that add persistence use MyBatis-Plus, Flyway, PostgreSQL, and
testing references as applicable. Local service setup routes to Docker; it
does not by itself imply a production architecture decision.

For Gradle, read [references/gradle.md](references/gradle.md). For another
build tool, follow its existing build and wrapper conventions. The Maven
reference applies only to Maven-specific tasks.

## Core Rules

1. For a new project, use Spring Boot 4.x and Java 21 (minimum Java 17).
   For an existing project, retain its Boot line unless the requested change
   requires an upgrade.
   For new Java 21+ blocking web applications, recommend virtual threads by
   default; read [references/async.md](references/async.md) for runtime and
   executor boundaries. Keep them disabled when workload or library
   constraints justify that choice.
2. For new code, organize by business feature. In existing code, keep the
   established structure unless the task changes a feature or module boundary.
   Read [references/architecture.md](references/architecture.md) when creating
   a project or changing its module, feature, or deployment boundaries.
3. In new projects, follow the Jackson major and mapper in `versions.json`.
   Use `tools.jackson` for databind, retain
   `com.fasterxml.jackson.annotation`, and serialize `Instant` as a timestamp
   using `spring.jackson.datatype.datetime.write-dates-as-timestamps: true`.
   Do not add `@JsonFormat` or replace Boot's mapper without a requirement. In
   existing projects, preserve the JSON contract and mapper configuration
   unless the task changes them.
4. For new outbound HTTP calls, use `RestClient`. In an existing project,
   follow its established client unless the task calls for migration.
5. Use constructor injection in new code. Avoid introducing Lombok; follow
   established conventions in existing code unless the task changes them.
6. For new projects, keep the generated `*ApplicationTests` context-load
   test. For test changes, follow [references/testing.md](references/testing.md)
   and retain existing project test conventions.
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

1. Check that the available JDK meets the selected Java version (21 by
   default), and that Node.js 18+, `unzip`, and Spring Initializr access are
   available. If another Boot-supported JDK is required, pass it explicitly
   with `--java`. The generator does not install or select a JDK. Then generate
   the skeleton with the script below, or use Spring Initializr with the
   requested build tool when it is not Maven.
2. Read the generated build file and `versions.json`; establish the Boot and
   Java versions and build tool. For Maven, use `springBoot.maven`; for
   Gradle, use `springBoot.default` and follow [references/gradle.md](references/gradle.md).
3. Read and apply the applicable new-project defaults from the architecture,
   selected build-tool, configuration, REST/OpenAPI, and testing references.
   Add only requested optional stacks and features; honor explicit user
   overrides and record them in the handoff. When a runnable template covers
   the requested feature, use its matching code and tests as the concrete
   reference; keep the generated project's name, package, and feature scope.
4. Review the generated project against Core Rules and every rule in the
   references used. For a new project, inspect the actual package tree and
   trace each type to its documented layer; confirm the bootstrap class is at
   the scan root, application-wide configuration is in `config`, feature
   configuration is feature-local, and persistence configuration is not
   misplaced in an unrelated feature. For paginated APIs, check the returned
   Spring Data `Page<T>` contains response DTOs rather than persistence types,
   and check pagination semantics at the HTTP boundary. Check build
   dependencies, runtime configuration,
   tests, and deployment controls alongside source code. Resolve each
   applicable violation before handoff, or identify it explicitly as an
   intentional user override or an unverified external control.
5. Run the selected build reference's complete verification command once.
   Start required services when available; report any checks that could not
   run because a required service was unavailable.

Do not describe a project as fully compliant based only on a successful
generator run or build. A compliance claim requires checking the generated
code and configuration against each applicable reference rule. When the user
overrides a rule (for example, declines Spring Security), follow the override
and distinguish it from accidental noncompliance in the final report.

### Change

1. Read repository instructions (`AGENTS.md` and relevant README files), then
   inspect the build file to establish Boot version, Java version, and build
   tool. Keep the existing Boot line and package/API contracts unless the task
   requires changing them. This skill's references target Boot 4; for an
   existing Boot 3.5 project, use the compatibility guidance in
   [spring-boot-developer](../spring-boot-developer/SKILL.md) before applying
   these Boot 4 references.
2. Identify the affected behavior, read the relevant configuration, code,
   tests, and matching reference files.
3. Make the smallest coherent change. Do not add new-project defaults to an
   existing application unless requested.
4. Review the changed behavior against applicable Core Rules and reference
   rules. Fix violations introduced by the change; keep unrelated existing
   issues out of scope unless they block the requested behavior.
5. Run the relevant test task once, following the project's wrapper and test
   conventions. Start required services when available; report any tests that
   could not run because a required service was unavailable.

## Create script

Run from this skill directory and set `--output-dir` to a workspace outside
the skill tree. The generator needs Node.js 18+, `unzip`, and access to Spring
Initializr. Build and test commands also need the JDK selected by `--java`
(Java 21 by default); the script does not install or switch Java versions.

```bash
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
```

The script supports Maven (default) and Gradle:

```bash
node scripts/create-project.mjs \
  --name inventory-api \
  --build gradle \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
```

It requests `web`, `validation`, and `actuator` from Spring Initializr and
uses version numbers without a `.RELEASE` suffix. For Maven it requests `springBoot.maven`; for Gradle it requests
`springBoot.default`. Spring Initializr sets the selected build file version. The generator creates only a starter
skeleton: the **Create** workflow adds the house defaults from the selected
build, architecture, and OpenAPI references. Optional stacks remain opt-in.

## Runnable templates

Use [template-maven](assets/template-maven/README.md) as the base example for
the default PostgreSQL/MyBatis-Plus stack and cross-cutting features. Use
[template-security](assets/template-security/README.md) only for requested
authentication features. The templates are checked examples, not project
generators: create new projects with the script above, then adapt only the
relevant patterns. Keep each referenced code example and its reference file in
agreement when either changes. After changing a template or its references,
run `node scripts/check-template-links.mjs` to validate local Markdown links
ensure each runnable template is indexed and referenced, and check Maven/Java
and managed Compose image pins against `versions.json`.

## Scope boundaries

- Existing Spring Boot 3.x application work that does not include a Boot 4
  migration: [spring-boot-developer](../spring-boot-developer/SKILL.md); use its
  Boot 3.5 compatibility guidance and focused topic references.
- Java language, JDK, or language-server work with no Boot application change:
  [java-developer](../java-developer/SKILL.md)
- PostgreSQL server operations, SQL tuning, or replication:
  [postgresql-developer](../postgresql-developer/SKILL.md)
- SAML federation, identity-provider administration, or broader identity
  architecture: use the matching Spring Security and OAuth2 references for
  application-level integration; provider administration is outside this
  skill's scope.
- Advanced identity features outside these references, including passkey
  enrollment and credential management, a custom authorization server, and issuing local
  JWTs:
  [spring-boot-developer](../spring-boot-developer/SKILL.md); read its matching
  security reference.
- Spring Cloud, config server, or service discovery:
  [spring-cloud-developer](../spring-cloud-developer/SKILL.md)
- JVM performance, GC, or profiling:
  [java-performance-developer](../java-performance-developer/SKILL.md)
