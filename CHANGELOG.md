# Changelog

This file records notable changes to the repository. Unreleased work stays under `Unreleased`; add a version and date when a release is cut.

## Unreleased

### Added

- Added SkillsJars Maven packaging for repository skills under `META-INF/skills`.
- Added a root Maven Wrapper and build instructions for creating and extracting the SkillsJars artifact.
- Added the `demo-task-manager` project using Java 21, Spring Boot 4.1.1, Maven, and PostgreSQL 18.
- Added task creation and paginated listing to the demo project.
- Added task lifecycle transitions to the demo project.
- Added optimistic concurrency control to task updates.
- Added append-only task status history.
- Added Flyway database migrations to the demo project.
- Added generated OpenAPI documentation to the demo project.
- Added Caffeine caching to the demo project.
- Added Actuator health and readiness probes to the demo project.
- Added Prometheus metrics to the demo project.
- Added Docker Compose configuration and local run instructions for the demo project.
- Added a Taskfile reference for discoverable, portable development commands.
- Added a Thymeleaf reference for server-rendered MVC views and forms.
- Added a file upload and download reference covering validation, authorization, streaming, and storage lifecycle.
- Added an email reference covering SMTP configuration, delivery timing, retries, and testing.
- Added an internationalization reference covering message bundles, locale selection, and localized errors.
- Added a database concurrency reference covering constraints, locking, retries, and concurrency tests.
- Added an auditing reference covering Spring Data metadata, system actors, and immutable audit history.
- Added a cache consistency reference covering commit ordering, invalidation, stale reads, and cache failures.
- Added an API idempotency reference covering safe retries, durable key scope, duplicate requests, and replay authorization.
- Added a rate limiting and resource protection reference covering gateway/application limits, quotas, overload responses, and hard resource bounds.
- Added a multi-tenancy reference covering trusted tenant context, persistence isolation, async propagation, and cross-tenant tests.
- Added a CI/CD and release reference covering immutable artifact promotion, schema compatibility, rollout gates, and recovery evidence.
- Added a database backup and recovery reference covering RPO/RTO, isolated restore drills, and cross-store consistency.
- Added an external service testing reference covering local stubs, provider contracts, safe substitutes, and failure-path checks.
- Added a WebSocket reference covering STOMP messaging, raw handlers, authorization, scaling, resource limits, and protocol testing.
- Expanded configuration guidance with secret ownership, rotation, emergency revocation, and CI identity practices.
- Expanded API versioning guidance with compatibility checks, client migration, and deprecation lifecycle operations.
- Expanded observability guidance with actionable alert ownership, SLOs, deploy annotations, and correlation ID handling.
- Added optional SDKMAN! guidance to local development for aligning developer JDKs while keeping the build Wrapper authoritative.
- Added Chinese counterparts for the new Spring Boot references.
- Added a Chinese Spring Boot skill entry point.
- Added Chinese summaries for most existing Spring Boot references.
- Added a Chinese counterpart for the WebSocket and STOMP reference.
- Added a Spring Cloud Developer skill entry point for version-aware distributed-system guidance.

### Changed

- Linked the Spring Cloud skill's Boot compatibility check to the official release train table.
- Updated the SkillsJars Maven coordinates, SCM metadata, repository links, and local Git remote from `0avi` to `zhijunio`.
- Expanded the root `.gitignore` for Maven and Gradle outputs, local IDE state, secrets, runtime artifacts, and OS files.
- Updated Boot 4 Jackson guidance to document ISO-8601 date defaults and remove the unsupported Jackson 2 timestamp property.
- Clarified that the Jackson 2 timestamp property remains available on Boot 3.5.x.
- Updated the Boot 4 `@WebMvcTest` import path in the web testing reference.
- Clarified that Boot 3.5.x continues to use the previous `@WebMvcTest` package.
- Clarified that database constraint exceptions must not be swallowed inside a failed write transaction.
- Added archive expansion limits, per-tenant quotas, active-content isolation, and pre-signed URL risks to file-storage guidance.
- Updated auditing guidance to use anonymous-authentication resolution, stable subject IDs, and explicit actor-retention policies.
- Added tenant/locale cache-key dimensions, authorization boundaries, outbox worker claims, and invalidation-lag monitoring to cache consistency guidance.
- Added mail-template localization, attachment bounds, bounce handling, worker coordination, and queue-age monitoring guidance.
- Added pluralization, currency formatting, locale-preference precedence, and safe locale-switch redirect guidance.
- Expanded REST endpoint guidance for query parameters, HTTP method semantics, and safe retries.
- Documented advice ordering when custom handlers compete with Boot's Problem Details advice.
- Documented that API version request headers must be explicitly included in generated OpenAPI documentation.
- Clarified when Docker Compose starts automatically with the application and when developers must start services manually.
- Updated local PostgreSQL guidance for the official PostgreSQL 18+ volume path.
- Added springdoc Javadoc generation guidance using the Therapi runtime library and annotation processor.
- Expanded JPA new-entity detection guidance for assigned IDs using nullable `@Version` fields.
- Documented flushing before mapping a response that must include the incremented JPA version.
- Moved detailed Taskfile guidance into its own reference and linked it from the local development reference.
- Reorganized the workflow and reference index in `java-developer/SKILL.md`.
- Clarified the performance-task boundary in `java-developer/SKILL.md`.
