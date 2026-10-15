---
name: spring-boot-developer
description: Creates and maintains Spring Boot applications, including REST APIs, persistence, tests, frontend integration, and container deployment. Use when scaffolding a Spring Boot project, implementing or changing application behavior, debugging failures, upgrading Boot versions, or preparing an application for delivery.
---

# Spring Boot Full-Stack Developer

Use this skill for Spring Boot project creation and implementation. It includes a Spring Initializr project generator, environment checker, setup assets, domain guides, frontend integration guides, deployment instructions, and a CRUD workshop. All instructions and assets needed by this skill are contained in this directory.

## Core Rules

1. **Determine the versions before giving version-sensitive guidance.** Read the build configuration and establish the exact Spring Boot version, Java version, and build tool. Do not infer them from the installed JDK or from another project.
2. **Preserve existing project choices.** For existing projects, keep the detected Boot line, build tool, package structure, API contract, and migration strategy unless the requested change requires an update. Do not silently upgrade dependencies or migrate Boot 3.5.x to 4.x.
3. **Use Boot 4.x for new projects by default.** Boot 3.5.x is supported for existing projects and realistic upgrades; the generator cannot create 3.5.x projects because Spring Initializr no longer offers them. Consult [Spring Boot version compatibility and migration](references/spring-boot-migration.md) before changing versions, starters, or APIs.
4. **Read the focused guide for the task.** Use the reference map below to select only the relevant guidance. The topic guides describe Boot 4 / Spring Framework 7 / Jackson 3. In a Boot 3.5.x project, first read [Working in an existing Boot 3.5.x project](references/spring-boot-migration.md#working-in-an-existing-boot-35x-project) and translate topic-guide code with it.
5. **Follow the project's dependency management.** Prefer the Spring Boot parent or BOM. Check compatibility before overriding managed versions or adding third-party libraries.
6. **Keep boundaries clear.** Organize new code by business feature where practical. Use request and response DTOs at HTTP boundaries; do not expose persistence entities as API representations.
7. **Avoid unnecessary infrastructure.** Add authentication, messaging, caching, cloud services, or other optional systems only when the request requires them. Follow existing project conventions when they already define the choice.
8. **Verify implementation changes.** Use the project's wrapper and established checks. Compile and run relevant tests after code changes; validate Compose configuration after container changes. Report the commands run and their actual results, including checks not run.

## Version and Environment Discovery

Inspect the repository before implementation:

- Maven: check the parent version or imported Spring Boot BOM in `pom.xml`, properties, and profiles.
- Gradle: check the Spring Boot plugin and dependency management in the build files and version catalog.
- Java: check the toolchain, compiler release, Maven properties, CI configuration, and project wrapper settings.
- Also read repository instructions, application configuration, existing tests, and relevant neighboring code.

For a new project, use `versions.json` as the generator's compatibility baseline and defaults. Respect explicit user choices and project constraints. Use `node scripts/check-environment.mjs --help` and `node scripts/create-project.mjs --help` for supported options.

## Project Workflow

### Create a project

Run the bundled scripts from this skill directory and set the output directory to the intended workspace. Do not generate a project inside the skill directory.

```bash
node scripts/check-environment.mjs --docker
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --docker \
  --output-dir /absolute/path/to/workspace
```

The generator defaults to JPA persistence and PostgreSQL. Select `--persistence jpa|mybatis-plus|none` and `--database postgresql|none`; MyBatis-Plus uses `mybatis-plus-spring-boot4-starter`. Generated projects include Spotless Java formatting, applied once after generation so the first `verify` passes, a wrapper-based `Taskfile.yml` with `format`, `test`, and `verify` tasks, and an `.sdkmanrc` for the selected Java major. PostgreSQL/Testcontainers Maven projects also receive Failsafe-bound `*IT` integration tests. Front-end, VS Code, and Dev Container files are optional and are not generated unless their workflow is requested. The generator uses Spring Initializr, then applies this skill's project setup assets. It does not overwrite an existing target, install global tools, or initialize Git. Docker generation requires PostgreSQL and Maven; the generator rejects Gradle with `--docker`. Check `--help` for all supported arguments and the actual version choices in `versions.json`.

For changes to generated project defaults or repository files, read [project setup](references/tooling/project-setup.md). For local PostgreSQL and Compose workflows, read [local development](references/tooling/local-development.md).

### Change an existing project

1. Inspect repository instructions, the working tree, build files, configuration, related code, and tests.
2. Identify the behavior and constraints to preserve. For CRUD or REST work, establish fields, validation, pagination and ordering, status codes, error behavior, persistence changes, and client states.
3. Read the relevant topic guides and implement the smallest coherent change in the existing conventions.
4. Add or update tests at the narrowest layer that proves the behavior. Use the real database or the project's established integration setup for database-specific behavior.
5. Run relevant checks and review the final diff for unintended changes.

If a requirement is ambiguous and affects the API, data model, security, or deployment behavior, state the assumption or ask for the missing decision before locking in the implementation.

### Debug a failure

Start from the observed symptom. Inspect the failing code path, configuration, logs, and recent changes; reproduce with the narrowest useful command; test the leading cause; then verify the affected path. If the cause is uncertain, compare plausible causes against evidence instead of changing unrelated code.

## Implementation Guidance

- Keep controllers focused on HTTP binding and response mapping. Put business rules and transaction coordination in an appropriate application service; avoid pass-through layers without a clear responsibility.
- Validate untrusted input at the boundary. Return useful client errors without exposing stack traces, internal details, or secrets.
- Use database migrations for deployed schema changes. Treat automatic schema generation as a local-development aid, not a production migration strategy.
- Use constructor injection. Follow the project's established Java conventions and the guidance in [Java in Spring](references/architecture/java-in-spring.md).
- For new API errors, prefer RFC 9457 `ProblemDetail` unless the project already has a required error contract.
- Keep credentials outside source control. Use documented environment variables and placeholder values in example environment files.
- For frontend integration, align browser calls with backend routes, validation, pagination, JSON wire types, and error responses. Keep generated wire types at the API boundary; handle loading, empty, success, HTTP-error, and no-response states. Prefer a same-origin API contract when suitable. See [OpenAPI](references/web/openapi.md), [JSON and Jackson](references/web/json-and-jackson.md), and [Error handling](references/web/error-handling.md).
- Keep operational endpoints and metrics limited to the intended management boundary. Never log credentials, access tokens, or sensitive request payloads.

## Testing and Verification

Choose tests according to the behavior under test; the levels complement one another:

| Behavior | Preferred starting point |
| --- | --- |
| Isolated domain or application logic | Plain unit test |
| MVC binding, validation, JSON, or controller behavior | Web slice |
| JPA mappings, repositories, SQL, or migrations | Persistence slice with the production database engine when relevant |
| Several Spring components or configuration working together | Focused integration test |
| Full HTTP behavior through a running server | REST end-to-end test |

Use Testcontainers for PostgreSQL-specific integration behavior unless the project already provides an equivalent real-database environment. See [testing strategy](references/testing/testing-strategy.md), the specific [testing guides](#testing), and [Testcontainers](references/testing/testcontainers.md). Run the build tool's wrapper and report actual outcomes; do not claim verification that was not performed.

## Java Code Intelligence

Generated projects include `.github/lsp.json` for JDTLS integration. When a Java language server is available, use semantic navigation for code changes: go to definition, find references before changing a signature, inspect symbols and types, rename symbols with LSP rename, and check diagnostics. Use the [JDTLS guide](references/tooling/jdtls.md) for setup and troubleshooting. If language-server support is unavailable, use Java-scoped search and inspect the relevant call sites.

## Reference Guide

Load the guide that matches the task. The focused guides contain implementation details and version-specific caveats; this file defines the workflow and routes requests to those guides.

### Spring Boot versions and project setup

- **Version selection, changes in a Boot 3.5.x project, or Boot 3.5.x to 4.x migration**: [Spring Boot version compatibility and migration](references/spring-boot-migration.md)
- **Project generation, defaults, or repository setup assets**: [Project setup](references/tooling/project-setup.md)
- **Maven build configuration**: [Maven](references/tooling/maven.md)
- **Gradle build configuration**: [Gradle](references/tooling/gradle.md)
- **Local development, PostgreSQL/Redis Compose, or DevTools**: [Local development](references/tooling/local-development.md) and [Docker](references/deployment/docker.md)
- **Task commands and Taskfile**: [Taskfile](references/tooling/taskfile.md)
- **Git branches, commits, review, or undo**: [Git workflow](references/tooling/git.md)
- **Java navigation and JDTLS**: [JDTLS](references/tooling/jdtls.md)

### Architecture and Java

- **Package and feature boundaries**: [Code organization](references/architecture/code-organization.md)
- **Domain model, aggregates, and domain events**: [Domain modelling](references/architecture/domain-modelling.md)
- **Java language choices in Spring-managed code**: [Java in Spring](references/architecture/java-in-spring.md)
- **Ports and adapters**: [Hexagonal architecture](references/architecture/hexagonal-architecture.md)
- **Spring Modulith boundaries and events**: [Spring Modulith](references/architecture/spring-modulith.md)
- **Dependency injection, proxies, and self-invocation**: [Spring proxies and dependency injection](references/architecture/spring-proxies-and-di.md)
- **Architecture tests**: [ArchUnit](references/architecture/archunit.md)

### Web and API

- **REST endpoints, HTTP semantics, and pagination**: [REST controllers](references/web/rest-controllers.md)
- **Validation**: [Validation](references/web/validation.md)
- **Error responses**: [Error handling](references/web/error-handling.md)
- **JSON serialization and Jackson**: [JSON and Jackson](references/web/json-and-jackson.md)
- **API versioning**: [API versioning](references/web/api-versioning.md)
- **OpenAPI and API documentation**: [OpenAPI](references/web/openapi.md)
- **HATEOAS**: [HATEOAS](references/web/hateoas.md)
- **Internationalization**: [Internationalization](references/web/internationalization.md)
- **WebSocket and STOMP**: [WebSocket](references/web/websocket.md)
- **Thymeleaf views**: [Thymeleaf](references/web/thymeleaf.md)
- **Serving frontend files and SPA routes**: [Static resources](references/web/static-resources.md)
- **Idempotent API commands**: [API idempotency](references/web/api-idempotency.md)

### Persistence and configuration

- **Spring Data JPA entities, repositories, and queries**: [Spring Data JPA](references/persistence/spring-data-jpa.md)
- **MyBatis-Plus**: [MyBatis-Plus](references/persistence/mybatis-plus.md)
- **Transaction boundaries and propagation**: [Transactions](references/persistence/transactions.md)
- **Schema changes and Flyway**: [Flyway](references/persistence/flyway.md)
- **SQL, indexes, and schema design**: [Database schema and SQL](references/persistence/database-schema-and-sql.md)
- **Backup and recovery objectives, database restore, or recovery drills**: [Backup and recovery](references/persistence/backup-and-recovery.md)
- **Concurrency, locking, and uniqueness races**: [Database concurrency](references/persistence/database-concurrency.md)
- **Auditing**: [Auditing](references/persistence/auditing.md)
- **Caching, Redis, cache failure behavior, or Redis Compose**: [Caching](references/persistence/caching.md), [cache consistency](references/persistence/cache-consistency.md), and [Docker](references/deployment/docker.md)
- **Multi-tenant data isolation**: [Multi-tenancy](references/persistence/multi-tenancy.md)
- **Profiles, properties, environment variables, and secrets**: [Configuration](references/configuration/configuration.md)

### Security

- **Filter chains, authorization, passwords, CORS, and CSRF**: [Security fundamentals](references/security/security-fundamentals.md)
- **Multi-factor authentication, WebAuthn/passkeys, staged JWT login, and recovery**: [MFA](references/security/mfa.md)
- **External OAuth2/OIDC login, SAML SSO, account mapping, and provisioning**: [OAuth2 login and SSO](references/security/oauth2-login.md)
- **Issuing JWTs for local accounts and token response conventions**: [JWT authentication](references/security/jwt-authentication.md)
- **Validating tokens from an identity provider**: [OAuth2 resource server](references/security/oauth2-resource-server.md)

### Testing

- **Test levels, context caching, and build setup**: [Testing strategy](references/testing/testing-strategy.md)
- **Unit tests**: [Unit testing](references/testing/testing-unit.md)
- **MVC, JSON, and web client tests**: [Web slice testing](references/testing/testing-slices-web.md)
- **JPA and database tests**: [Persistence slice testing](references/testing/testing-slices-persistence.md)
- **Focused application integration tests**: [Integration testing](references/testing/testing-integration.md)
- **Full REST tests**: [REST API testing](references/testing/testing-rest-api.md)
- **Containers and service connections (PostgreSQL, Redis, and other dependencies)**: [Testcontainers](references/testing/testcontainers.md)
- **Testing external services and outbound HTTP clients**: [External service testing](references/testing/testing-external-service.md)

### Integration and runtime

- **HTTP service clients**: [HTTP clients](references/integration/http-clients.md)
- **Embedded local developer console**: [BootUI](references/integration/bootui.md)
- **Retries, concurrency limits, and resilience**: [Resilience](references/integration/resilience.md)
- **Kafka, RabbitMQ, JMS, and outbox patterns**: [Messaging](references/integration/messaging.md)
- **Async work and scheduled tasks**: [Async and scheduling](references/integration/async-and-scheduling.md)
- **Email delivery**: [Mail](references/integration/mail.md)
- **File upload and storage**: [File storage](references/integration/file-storage.md)
- **Rate limiting and resource protection**: [Rate limiting](references/integration/rate-limiting.md)
- **Batch jobs**: [Spring Batch](references/integration/spring-batch.md)
- **Spring AI applications**: [Spring AI](references/integration/spring-ai.md)
- **MCP servers**: [MCP server](references/integration/mcp-server.md)

### Observability and performance

- **Logging**: [Logging](references/observability/logging.md)
- **Health, metrics, tracing, and alerts**: [Observability](references/observability/observability.md)
- **HTTP server and application performance settings**: [Performance](references/observability/performance.md)
- **AI and token usage signals**: [AI observability](references/observability/ai-observability.md)

### Frontend integration

Choose the requested client. The guides cover scaffolding and API integration; packaging depends on whether the frontend is static or requires its own runtime:

- [Vue](references/frontend/vue.md)
- [React](references/frontend/react.md)
- [Next.js](references/frontend/nextjs.md)
- [Angular](references/frontend/angular.md)
- [Vanilla JavaScript](references/frontend/vanilla-js.md)

### Containers and deployment

- **Dockerfiles, Compose, and container workflows**: [Docker](references/deployment/docker.md)
- **Native images and AOT**: [GraalVM Native Image](references/deployment/graalvm.md)
- **Azure Container Apps deployment**: [Azure](references/deployment/azure.md)
- **CI/CD and release practices**: [CI/CD and releases](references/deployment/ci-cd-release.md)
- **Guided full-stack implementation**: [Spring Boot Full-Stack Workshop](workshop/README.md)
- **Review an application against the collected conventions**: [Best practices checklist](references/checklist.md)
