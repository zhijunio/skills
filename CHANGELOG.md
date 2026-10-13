# Changelog

This file records notable repository changes. Keep unreleased work under `Unreleased`; add a version and date when a release is cut.

## Unreleased

### Added

- Added SkillsJars Maven packaging, a root Maven Wrapper, and build instructions.
- Added Spring Boot references for local development, server-rendered views, integrations, persistence, API design, security, operations, and testing.
- Added database schema and SQL design guidance.
- Added MyBatis-Plus guidance for Boot-specific starters, safe SQL, pagination, plugins, and real-database testing.
- Restored framework-agnostic database backup and recovery guidance in the reorganized Spring Boot reference map.
- Added Maven Surefire guidance for test discovery, focused test runs, and separating fast tests from Docker-backed integration tests.
- Added optional JUnit 5 tree console reporting through Surefire.
- Added runnable Spring Boot samples covering PostgreSQL, Redis caching, OAuth2 resource-server security, OpenAPI, Jackson, ArchUnit, and Vue integration.
- Added Spring SSO guidance for exercising OIDC client and resource-server flows with Dex or Rose Authorization Server.
- Added the Spring Cloud Developer skill with focused references for release-train compatibility and support, Gateway, Config, service discovery, and resilience.
- Rechecked Auth0 and Keycloak provider guidance against current vendor documentation and clarified token audiences, claims, and mapper configuration.
- Added five authoring/review prompts and five SDD skills with their planning and review references.

### Changed

- Pinned reusable GitHub workflows.
- Updated SkillsJars metadata and repository links, and expanded `.gitignore` for common build, IDE, secret, and runtime files.
- Expanded Spring Boot guidance for Boot 4 and 3.5 compatibility, configuration, persistence, APIs, testing, security, caching, messaging, operations, and local Docker Compose workflows.
- Expanded `java-developer` through Java 27, including version-specific API and preview guidance, migration notes, and verification limits.

### Removed

- Removed the standalone Angular and NgRx skill packages.
- Removed the legacy `demo-task-manager` project; maintained runnable Spring Boot examples under `samples/`.
- Removed the standalone CSS, TypeScript, and frontend-Spring contract skill packages; remaining frontend guidance lives in the Spring Boot and CI/CD references.
