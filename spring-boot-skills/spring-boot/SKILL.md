---
name: spring-boot
description: >
  Use as the entry point for creating, changing, or debugging a Spring Boot 4 application,
  especially when a task crosses build, web, data, security, integration, or delivery concerns.
  Identify the relevant focused skills in the Spring Boot Skills collection and follow them together.
---

# Spring Boot

Use this skill to route Spring Boot application work to the focused skills in this collection. Keep
the request's scope and project conventions; the focused skills own their domain rules.

## Establish the project

For an existing project, inspect its build file and application configuration to establish the
Spring Boot line, Java version, build tool, persistence technology, and existing conventions. Keep
those choices unless the request requires a change. These focused skills target Spring Boot 4; do not
silently upgrade a different Boot line or apply version-specific APIs without checking compatibility.

For a new project, read [scaffolding](../spring-boot-scaffolding/SKILL.md). Resolve consequential
choices such as destination, coordinates, build tool, Java version, persistence technology, and
requested integrations before generating files. Treat JPA, MyBatis-Plus, and JDBC as distinct
choices: a database name or CRUD requirement does not select one. If the user has not chosen a
persistence technology, ask before generating or implementing the project. For analysis or skill
routing that does not require a decision, mark persistence as unresolved and present the relevant
skills as options; do not recommend one as the default. Add only requested capabilities.

For an Initializr-based new project, use scaffolding plus exactly one selected build skill: Maven or Gradle. When the user has selected another generator, such as Progen, do not also run Initializr. If technical foundation or generator selection is unresolved and the user requests that decision, hand off to project-foundation where available. Read other skills only for requested capabilities and affected behavior. mise, Docker,
CI/CD, BootUI, and other developer tools are optional; do not add them by default. Use [mise](../mise/SKILL.md)
when the user requests project tool-version management or shared developer tasks.

## Route by task

Before making changes, read the matching skill entry points below and the specific references they
identify. Read multiple skills when the requested change crosses their boundaries. Do not load every
skill in the collection for a single task.

| Task area | Read |
| --- | --- |
| New project skeleton | [Scaffolding](../spring-boot-scaffolding/SKILL.md), then the selected build skill |
| Maven build, plugins, dependencies, coverage, or releases | [Maven](../spring-boot-maven/SKILL.md); add [multi-module Maven](../multi-module-maven/SKILL.md) for reactor structure |
| Gradle build, plugins, dependencies, or packaging | [Gradle](../spring-boot-gradle/SKILL.md) |
| Business capability packages and cross-feature ownership | [Feature architecture](../feature-architecture/SKILL.md) |
| Controller, service and data-access responsibilities in a layered design | [Layered architecture](../layered-architecture/SKILL.md) |
| Framework-independent domain, ports and adapters | [Hexagonal architecture](../hexagonal-architecture/SKILL.md) |
| Spring Modulith module APIs, dependencies or module events | [Spring Modulith](../spring-modulith/SKILL.md) |
| Static architecture rules and enforcement tests | [ArchUnit](../archunit/SKILL.md) |
| MVC endpoint, validation, or error response | [Web MVC](../spring-boot-webmvc/SKILL.md) for runtime HTTP behavior; add [API versioning](../spring-boot-api-versioning/SKILL.md) for version resolution or [OpenAPI](../spring-boot-openapi/SKILL.md) for the API contract and published document |
| Application transaction, propagation, rollback, locking, or commit-bound effects (including non-HTTP callers) | [Transaction guidance](../spring-boot-webmvc/references/transactions.md); add only the relevant persistence, messaging, task execution or retry skill. No controller changes implied. |
| Authentication, authorization, MFA, or OAuth2 | [Security](../spring-boot-security/SKILL.md) for caller identity and permissions; add [multi-tenancy](../multi-tenancy/SKILL.md) when data access must be tenant-scoped |
| MyBatis-Plus persistence | [MyBatis-Plus](../spring-boot-mybatis-plus/SKILL.md) |
| JPA entities, repositories, or Hibernate behavior | [Spring Data JPA](../spring-data-jpa/SKILL.md) |
| PostgreSQL types, constraints, or time zones | [PostgreSQL](../postgresql/SKILL.md) |
| Schema migration | [Flyway migrations](../flyway-migrations/SKILL.md) |
| Redis cache or Redis-backed data access | [Spring Data Redis](../spring-data-redis/SKILL.md); state whether Redis is a cache, store, or another integration |
| Tenant resolution or data isolation | [Multi-tenancy](../multi-tenancy/SKILL.md), plus the selected persistence and security skills |
| Outbound HTTP | [HTTP interface clients](../http-interface-clients/SKILL.md); add [resilience and retry](../resilience-retry/SKILL.md) when retries or concurrency limits are requested |
| Email | [Email](../spring-boot-email/SKILL.md) |
| Kafka or other broker messaging | [Event-driven messaging](../event-driven-messaging/SKILL.md); add [idempotency](../idempotency-patterns/SKILL.md) when duplicate effects matter |
| In-process async work or scheduling | [Task execution](../spring-task-execution/SKILL.md); use a durable job or broker workflow when required work must survive process restart |
| Docker image or local Compose services | [Docker](../spring-boot-docker/SKILL.md) |
| Kubernetes deployment | [Kubernetes](../spring-boot-kubernetes/SKILL.md), with Docker, security, and observability guidance as relevant |
| Configuration and profiles | [Configuration](../spring-boot-configuration/SKILL.md) |
| Metrics, logs, traces, probes, or Actuator | [Observability](../spring-boot-observability/SKILL.md) |
| CI, Renovate, releases, SBOM, or provenance | [CI/CD](../spring-boot-cicd/SKILL.md) |
| React or Next.js client | [Frontend integration](../spring-boot-frontend-integration/SKILL.md) |
| BootUI local console or shared developer tasks | [BootUI](../bootui/SKILL.md) or [mise](../mise/SKILL.md) |
| Test strategy or service-backed tests | [Testing](../spring-boot-testing/SKILL.md); use [browser regression](../agent-qa-browser-regression/SKILL.md) only for user-visible browser flows |
| JVM CPU, memory, GC, or thread diagnosis | [JVM diagnostics](../jvm-diagnostics/SKILL.md) |
| Nullness contracts | [Null safety](../null-safety/SKILL.md) |
| Spring AI models, tools, retrieval, or MCP | [Spring AI](../spring-ai/SKILL.md) |

## Architecture routing boundaries

Preserve the architecture already in use unless the user requests a redesign. Feature packaging and internal layering can coexist; layering and ports/adapters are design choices, not mandatory consecutive steps. Do not load all architecture skills for an ordinary endpoint change. Select Modulith only for its module facilities and ArchUnit when architecture enforcement is in scope. If architecture choices conflict, resolve the decision before editing. For unavailable sibling skills, identify the missing dependency and use relevant official documentation only when sufficient; do not claim to have loaded missing guidance.

## Coordinate combined changes

For a multi-area request, identify the relevant skills from the table, read each matching `SKILL.md`,
then follow only the references that apply. Resolve choices that affect multiple areas before
editing; for example, a PostgreSQL-backed CRUD service with Redis caching may involve Web MVC,
Maven, MyBatis-Plus, PostgreSQL, Flyway, Redis, Docker, and testing guidance. Keep dependency and
configuration decisions consistent across those skills.

If a consequential choice is unspecified, inspect the existing project first. For a new project,
ask only for choices that cannot be inferred, such as whether Redis is a cache or the source of
business data, or whether persistence should use JPA or MyBatis-Plus. Do not infer the persistence
technology from popularity, the database, or the CRUD use case. Do not add optional stacks by
default.

## Finish

Follow the applicable focused skills for implementation and verification. Report the files changed,
checks actually run, and any requested behavior that remains unverified. Do not claim a check passed
unless it was run.
