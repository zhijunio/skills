# Changelog

This file records notable repository changes. Keep unreleased work under `Unreleased`; add a version and date when a release is cut.

## Unreleased

### Added

- Added the `frontend-spring-contract` skill for React/Vue and Spring Boot API boundaries.
- Added SkillsJars Maven packaging, a root Maven Wrapper, and build instructions.
- Added the `demo-task-manager` sample with task lifecycle, optimistic locking, status history, Flyway, OpenAPI, caching, health probes, Prometheus metrics, and Docker Compose.
- Added Spring Boot references for local development, server-rendered views, integrations, persistence, API design, security, operations, and testing, with Chinese counterparts for most references.
- Added database schema and SQL design guidance in English and Chinese.
- Added English and Chinese MyBatis-Plus guidance for Boot-specific starters, safe SQL, pagination, plugins, and real-database testing.
- Added Maven Surefire guidance for test discovery, focused test runs, and separating fast tests from Docker-backed integration tests.
- Added optional JUnit 5 tree console reporting through Surefire.
- Added the Spring Cloud Developer skill entry point.
- Added five authoring/review prompts and five SDD skills with their planning and review references.

### Changed

- Pinned reusable GitHub workflows and included the demo application's integration tests in CI.
- Updated SkillsJars metadata and repository links, and expanded `.gitignore` for common build, IDE, secret, and runtime files.
- Expanded Spring Boot guidance for Boot 4 and 3.5 compatibility, configuration, persistence, APIs, testing, security, caching, messaging, operations, and local Docker Compose workflows.
- Expanded `java-developer` through Java 27, including version-specific API and preview guidance, migration notes, and verification limits.

### Removed

- Removed the standalone Angular and NgRx skill packages.
