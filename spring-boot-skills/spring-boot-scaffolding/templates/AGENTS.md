# Spring Boot Project Guidance

## Stack

- Spring Boot 4.x; use the selected line from the repository `versions.json`
- Java 21 by default; preserve the project's selected Java version
- Keep the generated Maven or Gradle wrapper as the build entry point
- New default stack: Spring MVC, PostgreSQL, MyBatis-Plus, Flyway, Redis,
  ArchUnit, and Taikai
- Use Jackson 3 and Boot 4 modular starters
- Add Security, AI, and telemetry integrations only when the project requires them

## Commands

- Maven verification: `./mvnw verify`
- Gradle verification: `./gradlew check bootJar`
- Run focused tests with the selected wrapper and existing project conventions

## Engineering rules

- Inspect neighboring code and dependency management before choosing an API.
- Keep controllers as HTTP adapters and business rules in application services.
- Use request and response DTOs at HTTP boundaries.
- Apply Flyway migrations for schema changes; never edit applied migrations.
- Preserve existing API, error, security, pagination, and JSON contracts.
- Keep API documentation disabled in production unless explicitly required.

## Completion

Run the relevant tests and the selected wrapper's verification lifecycle. Report
the exact commands and any checks that could not run.
