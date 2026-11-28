# Spring Boot Skills

Use the [Spring Boot entry skill](spring-boot/SKILL.md) for application work that
crosses skill boundaries. For a focused change, invoke the matching skill below
directly. Skills are focused references and workflows; load only those relevant
to the task. Preserve the approved project foundation; optional build, architecture, and developer tools are not mandatory defaults. See [version resolution](spring-boot/references/version-resolution.md) for version-sensitive guidance.

## Application structure and design

- [Scaffolding](spring-boot-scaffolding/SKILL.md) — create a Boot 4 application skeleton.
- [Feature architecture](feature-architecture/SKILL.md) — organize packages around business capabilities.
- [Layered architecture](layered-architecture/SKILL.md) — organize controllers, services, and persistence.
- [Hexagonal architecture](hexagonal-architecture/SKILL.md) — isolate domain logic behind ports and adapters.
- [Multi-module Maven](multi-module-maven/SKILL.md) — structure a Maven reactor.
- [Spring Modulith](spring-modulith/SKILL.md) — define and verify module boundaries.
- [API versioning](spring-boot-api-versioning/SKILL.md) — evolve HTTP API versions.

## Web APIs and integration

- [Web MVC](spring-boot-webmvc/SKILL.md) — HTTP behavior, validation, error handling, and transactions.
- [OpenAPI](spring-boot-openapi/SKILL.md) — contract-first APIs and generated documentation.
- [HTTP interface clients](http-interface-clients/SKILL.md) — typed outbound HTTP clients.
- [Frontend integration](spring-boot-frontend-integration/SKILL.md) — integrate React or Next.js clients.
- [Email](spring-boot-email/SKILL.md) — send email and test delivery with Mailpit.
- [Boot UI](bootui/SKILL.md) — build a local developer console.

## Security, data, and reliability

- [Security](spring-boot-security/SKILL.md) — authentication, authorization, and security controls.
- [Multi-tenancy](multi-tenancy/SKILL.md) — tenant isolation across application and data boundaries.
- [PostgreSQL](postgresql/SKILL.md) — types, time zones, constraints, and database practices.
- [Spring Data JPA](spring-data-jpa/SKILL.md) — persistence mapping and query behavior.
- [Spring Data Redis](spring-data-redis/SKILL.md) — caching and Redis-backed data access.
- [Flyway migrations](flyway-migrations/SKILL.md) — safe, deployable schema changes.
- [Idempotency patterns](idempotency-patterns/SKILL.md) — deduplicate retried operations.
- [Resilience and retry](resilience-retry/SKILL.md) — bounded retries and failure handling.
- [Event-driven messaging](event-driven-messaging/SKILL.md) — broker contracts, delivery, and consumers.

## AI and agent integrations

- [Spring AI](spring-ai/SKILL.md) — model, tool, retrieval, and MCP integrations.
- [Agent QA browser regression](agent-qa-browser-regression/SKILL.md) — browser-based regression workflows for agents.

## Build, delivery, and operations

- [Maven](spring-boot-maven/SKILL.md) — Maven builds, plugins, coverage, and releases.
- [Gradle](spring-boot-gradle/SKILL.md) — Gradle builds and dependency management.
- [Docker](spring-boot-docker/SKILL.md) — container images and local Compose services.
- [Kubernetes](spring-boot-kubernetes/SKILL.md) — workload manifests and rollout safety.
- [CI/CD](spring-boot-cicd/SKILL.md) — GitHub Actions, Renovate, and release automation.
- [Configuration](spring-boot-configuration/SKILL.md) — validate and bind application configuration.
- [mise](mise/SKILL.md) — pin project tools and expose shared developer tasks.
- [Task execution](spring-task-execution/SKILL.md) — asynchronous work and scheduled jobs.
- [Observability](spring-boot-observability/SKILL.md) — metrics, tracing, logs, and bounded labels.
- [JVM diagnostics](jvm-diagnostics/SKILL.md) — collect and interpret runtime evidence.

## Verification and code quality

- [Testing](spring-boot-testing/SKILL.md) — choose tests and report evidence-based readiness.
- [ArchUnit](archunit/SKILL.md) — enforce selected architecture constraints with ArchUnit or optional Taikai.
- [Null safety](null-safety/SKILL.md) — make nullability contracts explicit.
- [MyBatis-Plus](spring-boot-mybatis-plus/SKILL.md) — persistence with MyBatis-Plus.

This collection adapts selected skills from
[Rrezart Prebreza's Spring Boot Skills at `b90c3db`](https://github.com/rrezartprebreza/spring-boot-skills/tree/b90c3dbd9699c0d0bfb81660c857ef58262168e6/skills/spring-boot-4).
See the [upstream MIT license](LICENSE). The upstream repository maintains its
own [verification matrix](https://github.com/rrezartprebreza/spring-boot-skills/blob/b90c3dbd9699c0d0bfb81660c857ef58262168e6/verification/MATRIX.md)
for templates and fixtures at that revision; local changes have their own
verification scope.
